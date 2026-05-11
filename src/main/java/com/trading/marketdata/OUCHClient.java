package com.trading.marketdata;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.DatagramChannel;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Minimal OUCH-style binary client stub: connects over UDP, tracks ref numbers, and forwards fills via
 * {@link VenueEventListener}. Message layout is illustrative — align with your venue’s binary spec for production.
 */
public final class OUCHClient {

    private final VenueEventListener listener;
    private final String host;
    private final int port;
    private final String username;
    private final String password;
    private DatagramChannel channel;
    private final AtomicLong userRefNumGenerator = new AtomicLong(1);
    private final ConcurrentHashMap<Long, String> userRefToOrderId = new ConcurrentHashMap<>();
    private volatile boolean connected;

    public OUCHClient(VenueEventListener listener, String host, int port, String username, String password) {
        this.listener = listener;
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
    }

    public void connect() throws Exception {
        channel = DatagramChannel.open();
        channel.socket().setReuseAddress(true);
        channel.connect(new InetSocketAddress(host, port));
        Thread receiver = new Thread(this::receiveLoop, "ouch-receiver");
        receiver.setDaemon(true);
        receiver.start();
        connected = true;
    }

    public void disconnect() {
        connected = false;
        if (channel != null) {
            try {
                channel.close();
            } catch (Exception ignored) {
            }
            channel = null;
        }
    }

    public boolean isConnected() {
        return connected && channel != null && channel.isConnected();
    }

    public long sendEnterOrder(String clientOrderId, String symbol, char side, long quantity, double price) {
        if (channel == null || !channel.isConnected()) {
            return -1;
        }
        long userRef = userRefNumGenerator.getAndIncrement();
        userRefToOrderId.put(userRef, clientOrderId);

        int bodyLen = 2 + 1 + 4 + 4 + 8 + 8 + 1 + 1;
        ByteBuffer buf = ByteBuffer.allocate(bodyLen);
        buf.order(ByteOrder.BIG_ENDIAN);
        buf.putShort((short) (bodyLen - 2));
        buf.put((byte) 'O');
        buf.putInt((int) userRef);
        buf.putInt((int) quantity);
        buf.putLong(Math.round(price * 10_000));
        byte[] sym = new byte[8];
        byte[] s = symbol.getBytes();
        System.arraycopy(s, 0, sym, 0, Math.min(8, s.length));
        buf.put(sym);
        buf.put((byte) side);
        buf.put((byte) '0');
        buf.flip();
        try {
            channel.write(buf);
        } catch (Exception e) {
            userRefToOrderId.remove(userRef);
            return -1;
        }
        return userRef;
    }

    public void sendCancelOrder(long userRefNum, long quantity) {
        if (channel == null || !channel.isConnected()) {
            return;
        }
        ByteBuffer buf = ByteBuffer.allocate(11);
        buf.order(ByteOrder.BIG_ENDIAN);
        buf.putShort((short) 9);
        buf.put((byte) 'X');
        buf.putInt((int) userRefNum);
        buf.putInt((int) quantity);
        buf.flip();
        try {
            channel.write(buf);
        } catch (Exception ignored) {
        }
    }

    public void sendReplaceOrder(long userRefNum, long newQuantity, double newPrice) {
        if (channel == null || !channel.isConnected()) {
            return;
        }
        ByteBuffer buf = ByteBuffer.allocate(25);
        buf.order(ByteOrder.BIG_ENDIAN);
        buf.putShort((short) 23);
        buf.put((byte) 'U');
        buf.putInt((int) userRefNum);
        buf.putInt((int) newQuantity);
        buf.putLong(Math.round(newPrice * 10_000));
        buf.flip();
        try {
            channel.write(buf);
        } catch (Exception ignored) {
        }
    }

    public void sendMarketDataSubscription(String symbol) {
        // OUCH is order entry; market data subscription is venue-specific — no-op stub.
    }

    private void receiveLoop() {
        ByteBuffer buf = ByteBuffer.allocate(4096);
        buf.order(ByteOrder.BIG_ENDIAN);
        while (connected && !Thread.currentThread().isInterrupted()) {
            try {
                buf.clear();
                int read = channel.read(buf);
                if (read <= 0) {
                    Thread.sleep(50);
                    continue;
                }
                buf.flip();
                if (buf.remaining() < 3) {
                    continue;
                }
                short len = buf.getShort();
                byte type = buf.get();
                if (type == 'E' && buf.remaining() >= 21) {
                    long ref = buf.getInt() & 0xFFFFFFFFL;
                    long qty = buf.getInt() & 0xFFFFFFFFL;
                    long pxLong = buf.getLong();
                    long match = buf.getLong();
                    double px = pxLong / 10_000.0;
                    String oid = userRefToOrderId.getOrDefault(ref, String.valueOf(ref));
                    listener.onTradeExecution(oid, qty, px, match, System.nanoTime());
                }
            } catch (Exception e) {
                if (!connected) {
                    break;
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }
}
