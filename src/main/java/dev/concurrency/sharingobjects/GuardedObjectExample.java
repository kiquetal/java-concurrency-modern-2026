package dev.concurrency.sharingobjects;

public class GuardedObjectExample {
    // The state is guarded by the intrinsic lock of the current object ('this')
    private int availableConnections;

    public GuardedObjectExample(int totalConnections) {
        this.availableConnections = totalConnections;
    }

    // Using 'synchronized' ensures that only one thread can execute this method at a time,
    // guarding the 'availableConnections' variable from concurrent access.
    public synchronized boolean acquire() {
        if (availableConnections > 0) {
            availableConnections--;
            System.out.println(Thread.currentThread().getName() + " acquired connection. Remaining: " + availableConnections);
            return true;
        }
        System.out.println(Thread.currentThread().getName() + " failed to acquire connection.");
        return false;
    }

    public synchronized void release() {
        availableConnections++;
        System.out.println(Thread.currentThread().getName() + " released connection. Remaining: " + availableConnections);
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
