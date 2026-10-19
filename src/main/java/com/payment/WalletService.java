package com.payment;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Idempotent, thread-safe money deduction.
 * Same requestId (idempotency key) is applied at most once, no matter how many
 * times or how concurrently it is retried.
 */
public class WalletService {

    public enum Status { PENDING, SUCCESS, INSUFFICIENT_FUNDS }

    public record Result(Status status, long balanceAfter) {}

    private static final class Account {
        final ReentrantLock lock = new ReentrantLock();
        long balance;                       // in cents; only touched while holding lock
        Account(long balance) { this.balance = balance; }
    }

    private static final class Txn {
        final String accountId;
        final long amount;
        final CountDownLatch done = new CountDownLatch(1);
        volatile Status status = Status.PENDING;
        volatile long balanceAfter;
        Txn(String accountId, long amount) { this.accountId = accountId; this.amount = amount; }
    }

    private final Map<String, Account> accounts = new ConcurrentHashMap<>();
    private final Map<String, Txn> txns = new ConcurrentHashMap<>();   // requestId -> outcome

    public void createAccount(String accountId, long balance) {
        accounts.put(accountId, new Account(balance));
    }

    public long balanceOf(String accountId) {
        Account a = accounts.get(accountId);
        a.lock.lock();
        try { return a.balance; } finally { a.lock.unlock(); }
    }

    public Result deduct(String accountId, String requestId, long amount) throws InterruptedException {
        if (amount <= 0) throw new IllegalArgumentException("amount must be positive");
        Account acct = accounts.get(accountId);
        if (acct == null) throw new IllegalArgumentException("unknown account");

        // STEP 1: atomically claim the requestId. Exactly one thread can win this.
        Txn mine = new Txn(accountId, amount);
        Txn existing = txns.putIfAbsent(requestId, mine);

        if (existing != null) {
            // Duplicate / retry: never deduct again. Wait for the original, return its outcome.
            if (!existing.accountId.equals(accountId) || existing.amount != amount) {
                throw new IllegalArgumentException("requestId reused with a different payload");
            }
            existing.done.await();
            return outcome(existing);
        }

        // STEP 2: we own the request. Check balance + deduct + record outcome under the account lock.
        boolean completed = false;
        try {
            acct.lock.lock();
            try {
                if (acct.balance >= amount) {
                    acct.balance -= amount;
                    mine.status = Status.SUCCESS;
                } else {
                    mine.status = Status.INSUFFICIENT_FUNDS;
                }
                mine.balanceAfter = acct.balance;
            } finally {
                acct.lock.unlock();
            }
            completed = true;
            return outcome(mine);
        } finally {
            // If we died before touching the balance, nothing was deducted, so free the key for a clean retry.
            if (!completed) txns.remove(requestId, mine);
            mine.done.countDown();          // wake any duplicates waiting on us
        }
    }

    private static Result outcome(Txn t) {
        if (t.status == Status.PENDING) {
            throw new IllegalStateException("original attempt failed before completing; please retry");
        }
        return new Result(t.status, t.balanceAfter);
    }

    // ---------------- demo ----------------
    public static void main(String[] args) throws Exception {
        WalletService wallet = new WalletService();
        wallet.createAccount("acc1", 1000);
        ExecutorService pool = Executors.newFixedThreadPool(16);

        // Test 1: the SAME request retried by 20 threads -> deducted exactly once
        List<Future<Result>> same = new ArrayList<>();
        for (int i = 0; i < 20; i++) same.add(pool.submit(() -> wallet.deduct("acc1", "req-1", 100)));
        for (Future<Result> f : same) f.get();
        System.out.println("After 20 retries of req-1: balance = " + wallet.balanceOf("acc1") + " (expect 900)");

        // Test 2: 50 DIFFERENT requests of 100 against 900 -> exactly 9 succeed, never negative
        AtomicInteger ok = new AtomicInteger();
        List<Future<?>> many = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            final String id = "req-new-" + i;
            many.add(pool.submit(() -> {
                Result r = wallet.deduct("acc1", id, 100);
                if (r.status() == Status.SUCCESS) ok.incrementAndGet();
                return null;
            }));
        }
        for (Future<?> f : many) f.get();
        System.out.println("Succeeded: " + ok.get() + " (expect 9), balance = " + wallet.balanceOf("acc1") + " (expect 0)");
        pool.shutdown();
    }
}
