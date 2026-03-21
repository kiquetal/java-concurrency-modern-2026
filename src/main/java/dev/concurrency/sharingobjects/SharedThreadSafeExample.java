package dev.concurrency.sharingobjects;

import java.util.concurrent.ConcurrentHashMap;

public class SharedThreadSafeExample {
    // A shared data structure that manages its own internal synchronization.
    // ConcurrentHashMap allows multiple threads to read and write safely.
    private final ConcurrentHashMap<String, String> activeUsers = new ConcurrentHashMap<>();

    public void login(String userId) {
        activeUsers.put(userId, "Active");
        System.out.println(userId + " logged in.");
    }

    public void logout(String userId) {
        activeUsers.remove(userId);
        System.out.println(userId + " logged out.");
    }

    public boolean isActive(String userId) {
        return activeUsers.containsKey(userId);
    }

    public static void main(String[] args) throws InterruptedException {
        SharedThreadSafeExample system = new SharedThreadSafeExample();

        Thread t1 = new Thread(() -> system.login("UserA"));
        Thread t2 = new Thread(() -> system.login("UserB"));
        Thread t3 = new Thread(() -> {
            system.logout("UserA");
            System.out.println("Is UserB active? " + system.isActive("UserB"));
        });

        t1.start();
        t2.start();
        
        t1.join();
        t2.join();
        t3.start();
    }
}
