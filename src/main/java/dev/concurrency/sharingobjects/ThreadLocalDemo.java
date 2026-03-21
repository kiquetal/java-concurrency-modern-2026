package dev.concurrency.sharingobjects;

public class ThreadLocalDemo
{
    private static ThreadLocal<Integer> threadLocal = ThreadLocal.withInitial(() -> 0);

    public static void main(String[] args)
    {
        Runnable task = () -> {
            for (int i = 0; i < 5; i++) {
                int value = threadLocal.get();
                System.out.println(Thread.currentThread().getName() + " - Value: " + value);
                threadLocal.set(value + 1);
                System.out.println(Thread.currentThread().getName() + " - Updated Value: " + threadLocal.get());
            }
        };

        Thread thread1 = new Thread(task, "Thread-1");
        Thread thread2 = new Thread(task, "Thread-2");

        thread1.start();
        thread2.start();

        try {
            thread1.join();
            thread2.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Thread interrupted while waiting for completion");
        }

        System.out.println("Final value for Thread-1: " + threadLocal.get());
        System.out.println("Final value for Thread-2: " + threadLocal.get());
    }
}
