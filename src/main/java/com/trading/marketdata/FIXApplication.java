package com.trading.marketdata;

import quickfix.Application;
import quickfix.ConfigError;
import quickfix.DefaultMessageFactory;
import quickfix.DoNotSend;
import quickfix.FieldNotFound;
import quickfix.FileLogFactory;
import quickfix.FileStoreFactory;
import quickfix.Initiator;
import quickfix.LogFactory;
import quickfix.Message;
import quickfix.MessageFactory;
import quickfix.MessageStoreFactory;
import quickfix.Session;
import quickfix.SessionID;
import quickfix.SessionNotFound;
import quickfix.SessionSettings;
import quickfix.SocketInitiator;
import quickfix.field.ClOrdID;
import quickfix.field.ExecType;
import quickfix.field.HandlInst;
import quickfix.field.MDEntryPx;
import quickfix.field.MDEntrySize;
import quickfix.field.MDEntryType;
import quickfix.field.MsgType;
import quickfix.field.NoMDEntries;
import quickfix.field.OrdType;
import quickfix.field.OrderQty;
import quickfix.field.OrigClOrdID;
import quickfix.field.Price;
import quickfix.field.Side;
import quickfix.field.Symbol;
import quickfix.field.TestReqID;
import quickfix.field.Text;
import quickfix.field.TimeInForce;
import quickfix.field.TransactTime;
import quickfix.fix44.ExecutionReport;
import quickfix.fix44.MarketDataSnapshotFullRefresh;
import quickfix.fix44.NewOrderSingle;
import quickfix.fix44.OrderCancelReject;
import quickfix.fix44.OrderCancelRequest;
import quickfix.fix44.TestRequest;
import quickfix.fix44.TradeCaptureReport;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FIXApplication implements Application {

    private final VenueEventListener listener;
    private final SessionSettings settings;
    private final MessageStoreFactory storeFactory;
    private final LogFactory logFactory;
    private final Map<String, String> clOrdIdToInternalOrderId = new ConcurrentHashMap<>();

    private volatile SessionID sessionId;
    private volatile Initiator initiator;

    public FIXApplication(VenueEventListener listener, SessionSettings settings) {
        this.listener = listener;
        this.settings = settings;
        this.storeFactory = new FileStoreFactory(settings);
        this.logFactory = new FileLogFactory(settings);
    }

    public void start() throws ConfigError {
        MessageFactory messageFactory = new DefaultMessageFactory();
        this.initiator = new SocketInitiator(this, storeFactory, settings, logFactory, messageFactory);
        this.initiator.start();
    }

    public void stop() {
        if (initiator != null) {
            initiator.stop();
        }
    }

    @Override
    public void onCreate(SessionID sessionId) {
        this.sessionId = sessionId;
    }

    @Override
    public void onLogon(SessionID sessionId) {
        this.sessionId = sessionId;
        try {
            Session.sendToTarget(new TestRequest(new TestReqID("LOGON_TEST")), sessionId);
        } catch (SessionNotFound e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void onLogout(SessionID sessionId) {}

    @Override
    public void toAdmin(Message message, SessionID sessionId) {}

    @Override
    public void fromAdmin(Message message, SessionID sessionId) throws FieldNotFound {}

    @Override
    public void toApp(Message message, SessionID sessionId) throws DoNotSend {}

    @Override
    public void fromApp(Message message, SessionID sessionId) throws FieldNotFound {
        String msgType = getMsgType(message);
       /* switch (msgType) {
            case "8" -> handleExecutionReport(new ExecutionReport(message));
            case "W" -> handleMarketData(new MarketDataSnapshotFullRefresh(message));
            case "AE" -> handleTradeReport(new TradeCaptureReport(message));
            case "9" -> handleCancelReject(new OrderCancelReject(message));
            default -> {
                // ignore
            }
        }*/
    }

    public void sendNewOrder(InternalOrder order) {
        SessionID sid = requireSession();
        char fixSide = order.getSide() == VenueSide.BUY ? Side.BUY : Side.SELL;
        NewOrderSingle message =
                new NewOrderSingle(
                        new ClOrdID(order.getOrderId()),
                        new Side(fixSide),
                        new TransactTime(),
                        new OrdType(OrdType.LIMIT));
        message.set(new Symbol(order.getSymbol()));
        message.set(new OrderQty(order.getQuantity()));
        message.set(new Price(order.getPrice()));
        message.set(new TimeInForce(TimeInForce.DAY));
        message.set(new HandlInst('1'));
        message.set(new Text("nexus-trade"));
        sendToTarget(message, sid);
        clOrdIdToInternalOrderId.put(order.getOrderId(), order.getOrderId());
    }

    public void sendCancelRequest(String originalOrderId, long quantity) {
        SessionID sid = requireSession();
        OrderCancelRequest cancel =
                new OrderCancelRequest(
                        new OrigClOrdID(originalOrderId),
                        new ClOrdID("CXL_" + originalOrderId + "_" + System.nanoTime()),
                        new Side(Side.BUY),
                        new TransactTime());
        cancel.set(new OrderQty(quantity));
        sendToTarget(cancel, sid);
    }

    /** Stub: wire {@link quickfix.fix44.OrderCancelReplaceRequest} per venue. */
    public void sendReplaceRequest(String orderId, long newQuantity, double newPrice) {
        // no-op compile-safe default
    }

    /** Stub: wire {@link quickfix.fix44.MarketDataRequest} repeating groups per venue. */
    public void sendMarketDataRequest(String symbol) {
        // no-op compile-safe default
    }

    private void handleExecutionReport(ExecutionReport report) throws FieldNotFound {
        String clOrdId = report.getClOrdID().getValue();
        char execType = report.getExecType().getValue();
        double cumQty = report.getCumQty().getValue();
        double avgPx = report.isSetAvgPx() ? report.getAvgPx().getValue() : 0.0;
        String internal = clOrdIdToInternalOrderId.getOrDefault(clOrdId, clOrdId);

        if (execType == ExecType.FILL || execType == ExecType.PARTIAL_FILL) {
            listener.onExecutionFill(internal, (long) cumQty, avgPx);
        } else if (execType == ExecType.REJECTED) {
            String reason = report.isSetText() ? report.getText().getValue() : "rejected";
            listener.onExecutionReject(internal, reason);
        } else if (execType == ExecType.CANCELED) {
            listener.onExecutionCancelled(internal);
        }
    }

    private void handleMarketData(MarketDataSnapshotFullRefresh md) throws FieldNotFound {
        NoMDEntries noEntries = new NoMDEntries();
        md.get(noEntries);
        int n = noEntries.getValue();
        for (int i = 1; i <= n; i++) {
            MarketDataSnapshotFullRefresh.NoMDEntries group = new MarketDataSnapshotFullRefresh.NoMDEntries();
            md.getGroup(i, group);
            MDEntryType t = new MDEntryType();
            MDEntryPx px = new MDEntryPx();
            MDEntrySize sz = new MDEntrySize();
            group.get(t);
            group.get(px);
            long size = 0L;
            if (group.isSetField(sz)) {
                group.get(sz);
                size = (long) sz.getValue();
            }
            listener.onMarketDataEntry(t.getValue(), px.getValue(), size);
        }
    }

    private void handleTradeReport(TradeCaptureReport report) throws FieldNotFound {
        if (report.isSetLastPx() && report.isSetLastQty()) {
            listener.onMarketDataEntry(
                    MDEntryType.TRADE, report.getLastPx().getValue(), (long) report.getLastQty().getValue());
        }
    }

    private void handleCancelReject(OrderCancelReject reject) throws FieldNotFound {
        String clOrdId = reject.isSetClOrdID() ? reject.getClOrdID().getValue() : "";
        String reason = reject.isSetText() ? reject.getText().getValue() : "cancel reject";
        String internal = clOrdIdToInternalOrderId.getOrDefault(clOrdId, clOrdId);
        listener.onExecutionReject(internal, reason);
    }

    private SessionID requireSession() {
        SessionID sid = this.sessionId;
        if (sid == null) {
            throw new IllegalStateException("No active FIX session");
        }
        return sid;
    }

    private static void sendToTarget(Message message, SessionID sid) {
        try {
            Session.sendToTarget(message, sid);
        } catch (SessionNotFound e) {
            throw new IllegalStateException(e);
        }
    }

    private static String getMsgType(Message message) {
        try {
            MsgType msgType = new MsgType();
            message.getHeader().getField(msgType);
            return msgType.getValue();
        } catch (FieldNotFound e) {
            return "Unknown";
        }
    }
}
