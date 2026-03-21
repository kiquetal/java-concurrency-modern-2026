package dev.concurrency.sharingobjects;

import java.util.Map;

public class SharedReadOnlyExample {
    // This map is shared across all threads but is entirely read-only.
    // Map.of creates an unmodifiable map, ensuring thread safety.
    public static final Map<String, String> SETTINGS = Map.of(
            "timeout", "5000",
            "retries", "3"
    );

    public static void main(String[] args) {
        Runnable task = () -> {
            // Any thread can safely read from the shared SETTINGS map
            String timeout = SETTINGS.get("timeout");
            System.out.println(Thread.currentThread().getName() + " timeout setting: " + timeout);
        };

        new Thread(task, "Worker-1").start();
        new Thread(task, "Worker-2").start();
    }
}
