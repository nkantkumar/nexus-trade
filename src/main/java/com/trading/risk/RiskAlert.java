package com.trading.risk;

package com.trading.risk.model;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Risk Alert - Represents a risk violation or warning that requires attention
 */
public class RiskAlert {
    private final String alertId;
    private final String accountId;
    private final RiskCheckType checkType;
    private final RiskAlertSeverity severity;
    private final String message;
    private final String details;
    private final long timestamp;
    private RiskAlertStatus status;
    private String acknowledgedBy;
    private Long acknowledgedAt;
    private String resolvedBy;
    private Long resolvedAt;
    private String resolution;
    private int escalationCount;
    private Long lastEscalationAt;

    // Additional context
    private final Map<String, Object> contextData;
    private OrderRequest relatedOrder;
    private Position relatedPosition;

    // Thresholds that triggered the alert
    private double currentValue;
    private double thresholdValue;

    public RiskAlert(String accountId, RiskCheckType checkType,
                     RiskAlertSeverity severity, String message) {
        this(accountId, checkType, severity, message, null);
    }

    public RiskAlert(String accountId, RiskCheckType checkType,
                     RiskAlertSeverity severity, String message, String details) {
        this.alertId = generateAlertId();
        this.accountId = accountId;
        this.checkType = checkType;
        this.severity = severity;
        this.message = message;
        this.details = details;
        this.timestamp = System.currentTimeMillis();
        this.status = RiskAlertStatus.ACTIVE;
        this.escalationCount = 0;
        this.contextData = new ConcurrentHashMap<>();
    }

    private String generateAlertId() {
        return "ALERT_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
    }

    // Getters
    public String getAlertId() { return alertId; }
    public String getAccountId() { return accountId; }
    public RiskCheckType getCheckType() { return checkType; }
    public RiskAlertSeverity getSeverity() { return severity; }
    public String getMessage() { return message; }
    public String getDetails() { return details; }
    public long getTimestamp() { return timestamp; }
    public RiskAlertStatus getStatus() { return status; }
    public String getAcknowledgedBy() { return acknowledgedBy; }
    public Long getAcknowledgedAt() { return acknowledgedAt; }
    public String getResolvedBy() { return resolvedBy; }
    public Long getResolvedAt() { return resolvedAt; }
    public String getResolution() { return resolution; }
    public int getEscalationCount() { return escalationCount; }
    public Long getLastEscalationAt() { return lastEscalationAt; }
    public OrderRequest getRelatedOrder() { return relatedOrder; }
    public Position getRelatedPosition() { return relatedPosition; }
    public double getCurrentValue() { return currentValue; }
    public double getThresholdValue() { return thresholdValue; }

    // Setters for additional context
    public void setRelatedOrder(OrderRequest order) { this.relatedOrder = order; }
    public void setRelatedPosition(Position position) { this.relatedPosition = position; }
    public void setCurrentValue(double currentValue) { this.currentValue = currentValue; }
    public void setThresholdValue(double thresholdValue) { this.thresholdValue = thresholdValue; }

    public void setContextData(String key, Object value) {
        contextData.put(key, value);
    }

    public Object getContextData(String key) {
        return contextData.get(key);
    }

    public Map<String, Object> getAllContextData() {
        return new ConcurrentHashMap<>(contextData);
    }

    // Alert lifecycle methods
    public void acknowledge(String acknowledgedBy) {
        this.status = RiskAlertStatus.ACKNOWLEDGED;
        this.acknowledgedBy = acknowledgedBy;
        this.acknowledgedAt = System.currentTimeMillis();
    }

    public void resolve(String resolvedBy, String resolution) {
        this.status = RiskAlertStatus.RESOLVED;
        this.resolvedBy = resolvedBy;
        this.resolution = resolution;
        this.resolvedAt = System.currentTimeMillis();
    }

    public void escalate() {
        this.escalationCount++;
        this.lastEscalationAt = System.currentTimeMillis();

        // Auto-escalate severity if too many escalations
        if (escalationCount >= 3 && severity == RiskAlertSeverity.MEDIUM) {
            // Can't change severity directly as it's final, but we can track
            contextData.put("auto_escalated", true);
        }
    }

    public boolean isResolved() {
        return status == RiskAlertStatus.RESOLVED;
    }

    public boolean isActive() {
        return status == RiskAlertStatus.ACTIVE;
    }

    public boolean isAcknowledged() {
        return status == RiskAlertStatus.ACKNOWLEDGED;
    }

    // Time-based helpers
    public long getAgeInMilliseconds() {
        return System.currentTimeMillis() - timestamp;
    }

    public long getAgeInSeconds() {
        return getAgeInMilliseconds() / 1000;
    }

    public String getFormattedTimestamp() {
        LocalDateTime dateTime = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(timestamp),
                ZoneId.systemDefault()
        );
        return dateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    public String getFormattedAge() {
        long seconds = getAgeInSeconds();
        if (seconds < 60) return seconds + " seconds";
        long minutes = seconds / 60;
        if (minutes < 60) return minutes + " minutes";
        long hours = minutes / 60;
        return hours + " hours";
    }

    // Severity-based helpers
    public boolean isCritical() {
        return severity == RiskAlertSeverity.CRITICAL;
    }

    public boolean isHigh() {
        return severity == RiskAlertSeverity.HIGH || severity == RiskAlertSeverity.CRITICAL;
    }

    // For alert correlation
    public boolean isSimilarTo(RiskAlert other) {
        return this.accountId.equals(other.accountId) &&
                this.checkType == other.checkType &&
                Math.abs(this.timestamp - other.timestamp) < 60000; // Within 1 minute
    }

    @Override
    public String toString() {
        return String.format("RiskAlert{id=%s, account=%s, type=%s, severity=%s, status=%s, message='%s', age=%s}",
                alertId, accountId, checkType, severity, status,
                message.length() > 50 ? message.substring(0, 47) + "..." : message,
                getFormattedAge());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RiskAlert that = (RiskAlert) o;
        return alertId.equals(that.alertId);
    }

    @Override
    public int hashCode() {
        return alertId.hashCode();
    }
}

/**
 * Risk Alert Severity Levels
 */
public enum RiskAlertSeverity {
    LOW(1, "Low"),
    MEDIUM(2, "Medium"),
    HIGH(3, "High"),
    CRITICAL(4, "Critical");

    private final int level;
    private final String displayName;

    RiskAlertSeverity(int level, String displayName) {
        this.level = level;
        this.displayName = displayName;
    }

    public int getLevel() { return level; }
    public String getDisplayName() { return displayName; }

    public boolean isGreaterThan(RiskAlertSeverity other) {
        return this.level > other.level;
    }

    public boolean isLessThan(RiskAlertSeverity other) {
        return this.level < other.level;
    }
}

/**
 * Risk Alert Status
 */
public enum RiskAlertStatus {
    ACTIVE("Active", "Alert has been triggered and not yet addressed"),
    ACKNOWLEDGED("Acknowledged", "Alert has been seen but not resolved"),
    RESOLVED("Resolved", "Alert has been resolved"),
    ESCALATED("Escalated", "Alert has been escalated to higher authority"),
    IGNORED("Ignored", "Alert has been marked as false positive");

    private final String displayName;
    private final String description;

    RiskAlertStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
}

/**
 * Risk Alert Manager - Manages active alerts and alert lifecycle
 */
class RiskAlertManager {
    private final Map<String, RiskAlert> activeAlerts;
    private final Map<String, List<RiskAlert>> alertHistory;
    private final List<RiskAlertListener> listeners;
    private final int maxHistorySize;

    public RiskAlertManager() {
        this(10000); // Keep last 10,000 alerts in history
    }

    public RiskAlertManager(int maxHistorySize) {
        this.activeAlerts = new ConcurrentHashMap<>();
        this.alertHistory = new ConcurrentHashMap<>();
        this.listeners = new CopyOnWriteArrayList<>();
        this.maxHistorySize = maxHistorySize;
    }

    /**
     * Create and dispatch a new alert
     */
    public RiskAlert createAlert(String accountId, RiskCheckType checkType,
                                 RiskAlertSeverity severity, String message) {
        RiskAlert alert = new RiskAlert(accountId, checkType, severity, message);
        return dispatchAlert(alert);
    }

    public RiskAlert createAlert(String accountId, RiskCheckType checkType,
                                 RiskAlertSeverity severity, String message, String details) {
        RiskAlert alert = new RiskAlert(accountId, checkType, severity, message, details);
        return dispatchAlert(alert);
    }

    /**
     * Dispatch alert to all listeners and store it
     */
    private RiskAlert dispatchAlert(RiskAlert alert) {
        // Check if similar alert already exists
        RiskAlert existing = findSimilarActiveAlert(alert);
        if (existing != null) {
            // Suppress duplicate alert, but escalate existing one
            existing.escalate();
            return existing;
        }

        activeAlerts.put(alert.getAlertId(), alert);

        // Notify listeners
        for (RiskAlertListener listener : listeners) {
            try {
                listener.onAlert(alert);
            } catch (Exception e) {
                System.err.println("Error notifying alert listener: " + e.getMessage());
            }
        }

        return alert;
    }

    /**
     * Acknowledge an alert
     */
    public boolean acknowledgeAlert(String alertId, String acknowledgedBy) {
        RiskAlert alert = activeAlerts.get(alertId);
        if (alert != null && alert.isActive()) {
            alert.acknowledge(acknowledgedBy);
            return true;
        }
        return false;
    }

    /**
     * Resolve an alert
     */
    public boolean resolveAlert(String alertId, String resolvedBy, String resolution) {
        RiskAlert alert = activeAlerts.remove(alertId);
        if (alert != null) {
            alert.resolve(resolvedBy, resolution);
            addToHistory(alert);
            return true;
        }
        return false;
    }

    /**
     * Escalate an alert
     */
    public boolean escalateAlert(String alertId) {
        RiskAlert alert = activeAlerts.get(alertId);
        if (alert != null) {
            alert.escalate();

            // Notify listeners about escalation
            for (RiskAlertListener listener : listeners) {
                try {
                    listener.onAlertEscalated(alert);
                } catch (Exception e) {
                    System.err.println("Error notifying escalation listener: " + e.getMessage());
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Get all active alerts
     */
    public List<RiskAlert> getActiveAlerts() {
        return new ArrayList<>(activeAlerts.values());
    }

    /**
     * Get active alerts for a specific account
     */
    public List<RiskAlert> getActiveAlertsForAccount(String accountId) {
        return activeAlerts.values().stream()
                .filter(alert -> alert.getAccountId().equals(accountId))
                .collect(Collectors.toList());
    }

    /**
     * Get active alerts by severity
     */
    public List<RiskAlert> getActiveAlertsBySeverity(RiskAlertSeverity severity) {
        return activeAlerts.values().stream()
                .filter(alert -> alert.getSeverity() == severity)
                .collect(Collectors.toList());
    }

    /**
     * Get alert history for an account
     */
    public List<RiskAlert> getAlertHistory(String accountId) {
        return alertHistory.getOrDefault(accountId, new ArrayList<>());
    }

    /**
     * Get total number of active alerts
     */
    public int getActiveAlertCount() {
        return activeAlerts.size();
    }

    /**
     * Get count of active alerts by severity
     */
    public Map<RiskAlertSeverity, Integer> getActiveAlertCountBySeverity() {
        Map<RiskAlertSeverity, Integer> counts = new HashMap<>();
        for (RiskAlertSeverity severity : RiskAlertSeverity.values()) {
            counts.put(severity, 0);
        }

        for (RiskAlert alert : activeAlerts.values()) {
            counts.merge(alert.getSeverity(), 1, Integer::sum);
        }

        return counts;
    }

    /**
     * Clean old resolved alerts from history
     */
    public void cleanHistory(long olderThanMs) {
        long cutoff = System.currentTimeMillis() - olderThanMs;

        for (Map.Entry<String, List<RiskAlert>> entry : alertHistory.entrySet()) {
            List<RiskAlert> history = entry.getValue();
            history.removeIf(alert -> alert.getResolvedAt() != null &&
                    alert.getResolvedAt() < cutoff);

            // Trim to max history size
            while (history.size() > maxHistorySize) {
                history.remove(0);
            }
        }
    }

    /**
     * Clear all resolved alerts from active map (maintenance)
     */
    public void clearResolvedAlerts() {
        activeAlerts.entrySet().removeIf(entry -> entry.getValue().isResolved());
    }

    private RiskAlert findSimilarActiveAlert(RiskAlert newAlert) {
        for (RiskAlert existing : activeAlerts.values()) {
            if (existing.isSimilarTo(newAlert) && !existing.isResolved()) {
                return existing;
            }
        }
        return null;
    }

    private void addToHistory(RiskAlert alert) {
        alertHistory.computeIfAbsent(alert.getAccountId(), k -> new ArrayList<>())
                .add(alert);

        // Trim history if needed
        List<RiskAlert> history = alertHistory.get(alert.getAccountId());
        while (history.size() > maxHistorySize) {
            history.remove(0);
        }
    }

    public void addListener(RiskAlertListener listener) {
        listeners.add(listener);
    }

    public void removeListener(RiskAlertListener listener) {
        listeners.remove(listener);
    }
}

/**
 * Risk Alert Listener Interface
 */
interface RiskAlertListener {
    void onAlert(RiskAlert alert);

    default void onAlertEscalated(RiskAlert alert) {
        // Default implementation - can be overridden
        System.err.println("Alert escalated: " + alert.getAlertId());
    }

    default void onAlertResolved(RiskAlert alert) {
        // Default implementation
        System.out.println("Alert resolved: " + alert.getAlertId());
    }
}

/**
 * Alert Action Handler - Defines actions to take when alerts are triggered
 */
class AlertActionHandler implements RiskAlertListener {
    private final RiskEngine riskEngine;
    private final OrderManager orderManager;
    private final NotificationService notificationService;

    public AlertActionHandler(RiskEngine riskEngine, OrderManager orderManager,
                              NotificationService notificationService) {
        this.riskEngine = riskEngine;
        this.orderManager = orderManager;
        this.notificationService = notificationService;
    }

    @Override
    public void onAlert(RiskAlert alert) {
        // Take automatic actions based on severity
        switch (alert.getSeverity()) {
            case CRITICAL:
                handleCriticalAlert(alert);
                break;
            case HIGH:
                handleHighAlert(alert);
                break;
            case MEDIUM:
                handleMediumAlert(alert);
                break;
            case LOW:
                handleLowAlert(alert);
                break;
        }
    }

    private void handleCriticalAlert(RiskAlert alert) {
        // 1. Disable trading for the account
        riskEngine.disableTrading(alert.getAccountId());

        // 2. Cancel all open orders for the account
        orderManager.cancelAllOrders(alert.getAccountId());

        // 3. Send immediate notification
        notificationService.sendUrgentAlert(alert);

        // 4. Log to audit system
        logCriticalEvent(alert);

        // 5. If margin-related, trigger liquidation
        if (alert.getCheckType() == RiskCheckType.MARGIN) {
            riskEngine.forceLiquidation(alert.getAccountId());
        }
    }

    private void handleHighAlert(RiskAlert alert) {
        // 1. Prevent new orders
        riskEngine.setTradingRestricted(alert.getAccountId(), true);

        // 2. Send notification
        notificationService.sendAlert(alert);

        // 3. If position limit, cancel orders for that symbol
        if (alert.getCheckType() == RiskCheckType.POSITION_LIMIT) {
            OrderRequest relatedOrder = alert.getRelatedOrder();
            if (relatedOrder != null) {
                orderManager.cancelOrdersForSymbol(alert.getAccountId(), relatedOrder.getSymbol());
            }
        }
    }

    private void handleMediumAlert(RiskAlert alert) {
        // 1. Send notification to risk desk
        notificationService.sendAlert(alert);

        // 2. Log for monitoring
        logMediumEvent(alert);

        // 3. Increase monitoring frequency for the account
        riskEngine.increaseMonitoringFrequency(alert.getAccountId());
    }

    private void handleLowAlert(RiskAlert alert) {
        // 1. Log only
        logLowEvent(alert);

        // 2. Send summary notification (batched)
        notificationService.batchAlert(alert);
    }

    private void logCriticalEvent(RiskAlert alert) {
        // Log to critical events system
        System.err.printf("CRITICAL ALERT: Account=%s, Type=%s, Message=%s%n",
                alert.getAccountId(), alert.getCheckType(), alert.getMessage());
    }

    private void logMediumEvent(RiskAlert alert) {
        System.out.printf("MEDIUM ALERT: Account=%s, Type=%s, Message=%s%n",
                alert.getAccountId(), alert.getCheckType(), alert.getMessage());
    }

    private void logLowEvent(RiskAlert alert) {
        // Debug level logging
        // System.out.printf("Low alert: %s%n", alert.getMessage());
    }

    @Override
    public void onAlertEscalated(RiskAlert alert) {
        // Alert was escalated - take stronger action
        if (alert.getEscalationCount() >= 3) {
            handleCriticalAlert(alert);
        } else if (alert.getEscalationCount() >= 2) {
            handleHighAlert(alert);
        }
    }
}

// Placeholder interfaces for dependencies
interface OrderManager {
    void cancelAllOrders(String accountId);
    void cancelOrdersForSymbol(String accountId, String symbol);
}

interface NotificationService {
    void sendUrgentAlert(RiskAlert alert);
    void sendAlert(RiskAlert alert);
    void batchAlert(RiskAlert alert);
}