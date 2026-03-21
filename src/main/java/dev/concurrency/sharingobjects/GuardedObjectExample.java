package dev.concurrency.sharingobjects;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class GuardedObjectExample {
    // The state is guarded by an explicit ReentrantLock
    private final Lock lock = new ReentrantLock();
    // @GuardedBy("lock")
    private int availableConnections;

    public GuardedObjectExample(int totalConnections) {
        this.availableConnections = totalConnections;
    }

    // Using explicit Lock ensures that we can protect the state from concurrent access,
    // offering more flexibility than intrinsic locks if needed later (e.g. tryLock).
    public boolean acquire() {
        lock.lock();
        try {
            if (availableConnections > 0) {
                availableConnections--;
                System.out.println(Thread.currentThread().getName() + " acquired connection. Remaining: " + availableConnections);
                return true;
            }
            System.out.println(Thread.currentThread().getName() + " failed to acquire connection.");
            return false;
        } finally {
            lock.unlock(); // Ensure lock is released even if an exception occurs
        }
    }

    public void release() {
        lock.lock();
        try {
            availableConnections++;
            System.out.println(Thread.currentThread().getName() + " released connection. Remaining: " + availableConnections);
        } finally {
            lock.unlock(); // Ensure lock is released even if an exception occurs
        }
    }

    public static void main(String[] args) {
        GuardedObjectExample pool = new GuardedObjectExample(2);

        Runnable task = () -> {
            if (pool.acquire()) {
                try {
                    Thread.sleep(100); // Simulate work
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    pool.release();
                }
            }
        };

        Thread t1 = new Thread(task, "Thread-1");
        Thread t2 = new Thread(task, "Thread-2");
        Thread t3 = new Thread(task, "Thread-3");

        t1.start();
        t2.start();
        t3.start();
    }
}
