package dev.concurrency.sharingobjects;

import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class TryLockRetryExample {

    private static final ReentrantLock inventoryLock = new ReentrantLock();
    private static final ReentrantLock ledgerLock = new ReentrantLock();
    private static final int MAX_RETRIES = 3;

    public static boolean placeOrder(String item) throws InterruptedException {
        Random random = new Random();
        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            if (inventoryLock.tryLock(100, TimeUnit.MILLISECONDS)) {
                try {
                    if (ledgerLock.tryLock(100, TimeUnit.MILLISECONDS)) {
                        try {
                            System.out.println(Thread.currentThread().getName()
                                    + " placed order for " + item);
                            return true;
                        } finally {
                            ledgerLock.unlock();
                        }
                    }
                } finally {
                    inventoryLock.unlock(); // release first lock if second failed
                }
            }
            int backoff = random.nextInt(50);
            System.out.println(Thread.currentThread().getName()
                    + " failed attempt " + (attempt + 1) + ", backing off " + backoff + "ms");
            Thread.sleep(backoff);
        }
        System.out.println(Thread.currentThread().getName()
                + " gave up after " + MAX_RETRIES + " attempts");
        return false;
    }

    // Acquires locks in OPPOSITE order to force contention with placeOrder
    public static boolean cancelOrder(String orderId) throws InterruptedException {
        Random random = new Random();
        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            if (ledgerLock.tryLock(100, TimeUnit.MILLISECONDS)) {
                try {
                    if (inventoryLock.tryLock(100, TimeUnit.MILLISECONDS)) {
                        try {
                            System.out.println(Thread.currentThread().getName()
                                    + " cancelled order " + orderId);
                            return true;
                        } finally {
                            System.out.println(Thread.currentThread().getName()
                                    + " released inventory lock");
                            inventoryLock.unlock();
                        }
                    }
                } finally {
                    System.out.println(Thread.currentThread().getName()
                            + " released ledger lock");
                    ledgerLock.unlock();
                }
            }
            int backoff = random.nextInt(50);
            System.out.println(Thread.currentThread().getName()
                    + " failed attempt " + (attempt + 1) + ", backing off " + backoff + "ms");
            Thread.sleep(backoff);
        }
        System.out.println(Thread.currentThread().getName()
                + " gave up after " + MAX_RETRIES + " attempts");
        return false;
    }

    public static void main(String[] args) {
        // placeOrder locks: inventory -> ledger
        // cancelOrder locks: ledger -> inventory  (opposite order!)
        // With synchronized this would DEADLOCK. With tryLock, threads back off and retry.

        Thread t1 = new Thread(() -> {
            try { placeOrder("Laptop"); } catch (InterruptedException e) {

                System.out.println("Interrupted while trying to place order");
                Thread.currentThread().interrupt(); }
        }, "Thread-Checkout");

        Thread t2 = new Thread(() -> {
            try { cancelOrder("ORD-42"); } catch (InterruptedException e) {
                System.out.println("Interrupted while trying to cancel order");
                Thread.currentThread().interrupt(); }
        }, "Thread-Cancel");

        t1.start();
        t2.start();
    }
}
