package com.trading.risk;

import java.util.Calendar;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap; /**
 * Trading hours validator - prevents trading outside market hours
 */
public class TradingHoursValidator implements RiskValidator {
    private final Map<String, TradingSchedule> marketSchedules;

    public TradingHoursValidator() {
        this.marketSchedules = new ConcurrentHashMap<>();

        // Default US market hours (Eastern Time)
        TradingSchedule usEquities = new TradingSchedule(
                "9:30", "16:00",  // Regular hours
                "8:00", "20:00",   // Extended hours
                false              // No trading on weekends
        );
        marketSchedules.put("NYSE", usEquities);
        marketSchedules.put("NASDAQ", usEquities);
    }

    @Override
    public RiskCheckResult validate(OrderRequest order, RiskAccount account, RiskContext context) {
        String exchange = getExchangeForSymbol(order.getSymbol());
        TradingSchedule schedule = marketSchedules.get(exchange);

        if (schedule == null) {
            return RiskCheckResult.pass(RiskCheckType.TRADING_HOURS);
        }

        long now = System.currentTimeMillis();
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(now);

        // Check if weekend
        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        if (schedule.isNoTradingOnWeekends() &&
                (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY)) {
            return RiskCheckResult.fail(RiskCheckType.TRADING_HOURS,
                    "No trading on weekends");
        }

        // Check trading session
        TradingSession session = getTradingSession(now, schedule);

        if (session == TradingSession.CLOSED) {
            return RiskCheckResult.fail(RiskCheckType.TRADING_HOURS,
                    "Market is closed");
        }

        // Different order types have different restrictions
        if (order.getOrderType() == OrderType.MARKET) {
            if (session != TradingSession.REGULAR) {
                return RiskCheckResult.fail(RiskCheckType.TRADING_HOURS,
                        "Market orders only allowed during regular trading hours");
            }
        }

        if (order.getOrderType() == OrderType.STOP) {
            if (session != TradingSession.REGULAR) {
                return RiskCheckResult.fail(RiskCheckType.TRADING_HOURS,
                        "Stop orders only allowed during regular trading hours");
            }
        }

        // Check time-in-force restrictions
        if (order.getTimeInForce() == TimeInForce.AT_THE_OPEN) {
            if (!isPreMarketOpen(cal, schedule)) {
                return RiskCheckResult.fail(RiskCheckType.TRADING_HOURS,
                        "AT_THE_OPEN orders only accepted during pre-market");
            }
        }

        if (order.getTimeInForce() == TimeInForce.AT_THE_CLOSE) {
            if (!isPostMarketOpen(cal, schedule)) {
                return RiskCheckResult.fail(RiskCheckType.TRADING_HOURS,
                        "AT_THE_CLOSE orders only accepted during post-market");
            }
        }

        return RiskCheckResult.pass(RiskCheckType.TRADING_HOURS);
    }

    private TradingSession getTradingSession(long timestamp, TradingSchedule schedule) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestamp);

        String timeStr = String.format("%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE));

        if (isTimeBetween(timeStr, schedule.getRegularOpen(), schedule.getRegularClose())) {
            return TradingSession.REGULAR;
        } else if (isTimeBetween(timeStr, schedule.getPreOpen(), schedule.getRegularOpen())) {
            return TradingSession.PRE_MARKET;
        } else if (isTimeBetween(timeStr, schedule.getRegularClose(), schedule.getPostClose())) {
            return TradingSession.POST_MARKET;
        }

        return TradingSession.CLOSED;
    }

    private boolean isTimeBetween(String time, String start, String end) {
        return time.compareTo(start) >= 0 && time.compareTo(end) < 0;
    }

    private boolean isPreMarketOpen(Calendar cal, TradingSchedule schedule) {
        String timeStr = String.format("%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE));
        return isTimeBetween(timeStr, schedule.getPreOpen(), schedule.getRegularOpen());
    }

    private boolean isPostMarketOpen(Calendar cal, TradingSchedule schedule) {
        String timeStr = String.format("%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE));
        return isTimeBetween(timeStr, schedule.getRegularClose(), schedule.getPostClose());
    }

    private String getExchangeForSymbol(String symbol) {
        // Would map symbol to exchange
        return "NYSE";
    }

    @Override
    public int getPriority() { return 25; }
}
