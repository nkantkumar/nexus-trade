package com.trading.risk;


import java.util.*;
import java.util.concurrent.*;

/**
 * Manages credit lines, ratings, and credit-related operations
 */
public class CreditManager {
    private final Map<String, CreditAccount> creditAccounts;
    private final Map<String, CreditRating> creditRatings;
    private final Map<String, MarketMakerStatus> marketMakerStatus;
    private final ScheduledExecutorService creditMonitor;

    public CreditManager() {
        this.creditAccounts = new ConcurrentHashMap<>();
        this.creditRatings = new ConcurrentHashMap<>();
        this.marketMakerStatus = new ConcurrentHashMap<>();
        this.creditMonitor = Executors.newSingleThreadScheduledExecutor();

        // Start credit monitoring
        creditMonitor.scheduleAtFixedRate(this::monitorCreditUsage, 60, 60, TimeUnit.SECONDS);
    }

    /**
     * Get credit rating for an account
     */
    public CreditRating getCreditRating(String accountId) {
        return creditRatings.getOrDefault(accountId, CreditRating.FAIR);
    }

    /**
     * Set credit rating for an account
     */
    public void setCreditRating(String accountId, CreditRating rating) {
        creditRatings.put(accountId, rating);

        // Adjust credit limits based on rating
        CreditAccount account = creditAccounts.get(accountId);
        if (account != null) {
            adjustCreditLimitByRating(account, rating);
        }
    }

    /**
     * Check if market maker is in good standing
     */
    public boolean isMarketMakerInGoodStanding(String accountId) {
        MarketMakerStatus status = marketMakerStatus.get(accountId);
        if (status == null) return false;

        return status.isActive() &&
                status.getViolationCount() < 3 &&
                status.getObligationMet() > 0.95; // 95% obligation met
    }

    /**
     * Register a market maker account
     */
    public void registerMarketMaker(String accountId, double minimumObligation) {
        MarketMakerStatus status = new MarketMakerStatus(accountId, minimumObligation);
        marketMakerStatus.put(accountId, status);
    }

    /**
     * Update market maker performance
     */
    public void updateMarketMakerPerformance(String accountId, double quoteWidth,
                                             double quoteDepth, double uptime) {
        MarketMakerStatus status = marketMakerStatus.get(accountId);
        if (status != null) {
            status.updatePerformance(quoteWidth, quoteDepth, uptime);
        }
    }

    /**
     * Get available credit for an account
     */
    public double getAvailableCredit(String accountId) {
        CreditAccount account = creditAccounts.get(accountId);
        if (account == null) return 0;
        return account.getCreditLimit() - account.getUsedCredit();
    }

    /**
     * Reserve credit for an order
     */
    public boolean reserveCredit(String accountId, double amount) {
        CreditAccount account = creditAccounts.get(accountId);
        if (account == null) return false;

        synchronized (account) {
            if (account.getUsedCredit() + amount <= account.getCreditLimit()) {
                account.setUsedCredit(account.getUsedCredit() + amount);
                return true;
            }
        }
        return false;
    }

    /**
     * Release reserved credit
     */
    public void releaseCredit(String accountId, double amount) {
        CreditAccount account = creditAccounts.get(accountId);
        if (account != null) {
            synchronized (account) {
                account.setUsedCredit(Math.max(0, account.getUsedCredit() - amount));
            }
        }
    }

    /**
     * Create a new credit account
     */
    public void createCreditAccount(String accountId, double creditLimit, double marginRequirement) {
        CreditAccount account = new CreditAccount(accountId, creditLimit, marginRequirement);
        creditAccounts.put(accountId, account);
        creditRatings.put(accountId, CreditRating.FAIR);
    }

    private void adjustCreditLimitByRating(CreditAccount account, CreditRating rating) {
        double baseLimit = account.getBaseCreditLimit();
        switch (rating) {
            case EXCELLENT:
                account.setCreditLimit(baseLimit * 2.0);
                break;
            case GOOD:
                account.setCreditLimit(baseLimit * 1.5);
                break;
            case FAIR:
                account.setCreditLimit(baseLimit);
                break;
            case LIMITED:
                account.setCreditLimit(baseLimit * 0.5);
                break;
            case POOR:
                account.setCreditLimit(baseLimit * 0.25);
                break;
        }
    }

    private void monitorCreditUsage() {
        for (CreditAccount account : creditAccounts.values()) {
            double usage = account.getUsedCredit() / account.getCreditLimit();

            if (usage > 0.9) {
                // High credit usage - send alert
                System.err.printf("CREDIT ALERT: Account %s usage at %.1f%%%n",
                        account.getAccountId(), usage * 100);
            }

            if (usage > 1.0) {
                // Credit limit exceeded - escalate
                System.err.printf("CREDIT OVERDUE: Account %s exceeded limit by %.2f%n",
                        account.getAccountId(), account.getUsedCredit() - account.getCreditLimit());
            }
        }
    }

    public void shutdown() {
        creditMonitor.shutdown();
    }
}

/**
 * Credit account information
 */
class CreditAccount {
    private final String accountId;
    private final double baseCreditLimit;
    private volatile double creditLimit;
    private volatile double usedCredit;
    private volatile double marginRequirement;
    private volatile long lastUpdateTime;

    public CreditAccount(String accountId, double creditLimit, double marginRequirement) {
        this.accountId = accountId;
        this.baseCreditLimit = creditLimit;
        this.creditLimit = creditLimit;
        this.marginRequirement = marginRequirement;
        this.usedCredit = 0;
        this.lastUpdateTime = System.currentTimeMillis();
    }

    public String getAccountId() { return accountId; }
    public double getBaseCreditLimit() { return baseCreditLimit; }
    public double getCreditLimit() { return creditLimit; }
    public void setCreditLimit(double limit) { this.creditLimit = limit; }
    public double getUsedCredit() { return usedCredit; }
    public void setUsedCredit(double used) { this.usedCredit = used; }
    public double getMarginRequirement() { return marginRequirement; }
    public void setMarginRequirement(double requirement) { this.marginRequirement = requirement; }
    public long getLastUpdateTime() { return lastUpdateTime; }
}

/**
 * Market maker status
 */
class MarketMakerStatus {
    private final String accountId;
    private final double minimumObligation;
    private volatile boolean active;
    private volatile int violationCount;
    private volatile double obligationMet;
    private volatile double averageQuoteWidth;
    private volatile double averageQuoteDepth;
    private volatile double uptime;
    private final List<Long> violations;

    public MarketMakerStatus(String accountId, double minimumObligation) {
        this.accountId = accountId;
        this.minimumObligation = minimumObligation;
        this.active = true;
        this.violationCount = 0;
        this.obligationMet = 1.0;
        this.violations = new ArrayList<>();
    }

    public void updatePerformance(double quoteWidth, double quoteDepth, double uptime) {
        this.averageQuoteWidth = (this.averageQuoteWidth + quoteWidth) / 2;
        this.averageQuoteDepth = (this.averageQuoteDepth + quoteDepth) / 2;
        this.uptime = uptime;

        // Check obligations
        if (quoteWidth > 0.10) { // Quote width > 10 cents is violation
            addViolation();
        }
    }

    private void addViolation() {
        violationCount++;
        violations.add(System.currentTimeMillis());

        // Clean old violations (older than 24 hours)
        long cutoff = System.currentTimeMillis() - 86400000;
        violations.removeIf(v -> v < cutoff);

        if (violationCount > 5) {
            active = false; // Suspend market maker
        }
    }

    public boolean isActive() { return active; }
    public int getViolationCount() { return violationCount; }
    public double getObligationMet() { return obligationMet; }
}
