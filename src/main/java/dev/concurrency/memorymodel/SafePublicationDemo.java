package dev.concurrency.memorymodel;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

/**
 * Demonstrates the four safe publication idioms (JCiP §3.5 / JMM Ch.16).
 *
 * Without safe publication, a reader thread can see a non-null reference
 * pointing to a half-constructed object — fields still at default values.
 *
 * Unsafe timeline (what we're preventing):
 *   Thread A: holder = new Holder(42)
 *     → JVM may reorder: assign reference BEFORE constructor finishes
 *   Thread B: reads holder (non-null) → reads holder.value → sees 0 (!)
 *
 * The four idioms that prevent this, each using a different happens-before edge:
 *   1. Static initializer    — class-load happens-before any use
 *   2. Volatile field         — volatile write happens-before volatile read
 *   3. Thread-safe collection — ConcurrentHashMap's internal synchronization
 *   4. Synchronized block     — unlock happens-before subsequent lock
 *
 * IMPORTANT: This demo uses CountDownLatch (not join()) to coordinate threads.
 * join() itself is a happens-before edge, which would mask the idiom being shown.
 * The latch only signals "the writer stored something" — the SAFE PUBLICATION
 * guarantee comes from the idiom itself, not from the latch.
 */
public class SafePublicationDemo {

    record Config(String name, int version) {}

    // --- Idiom 1: Static initializer ---
    static final Config VIA_STATIC = new Config("static-init", 1);

    // --- Idiom 2: Volatile field ---
    private static volatile Config viaVolatile;

    // --- Idiom 3: Thread-safe collection ---
    private static final Map<String, Config> registry = new ConcurrentHashMap<>();

    // --- Idiom 4: Synchronized block ---
    private static Config viaGuarded;
    private static final Object lock = new Object();

    public static void main(String[] args) throws InterruptedException {

        // Idiom 1 — safe at class-load time, no coordination needed
        Thread.ofPlatform().name("Reader-Static").start(() ->
            System.out.println("[static]     " + VIA_STATIC)
        ).join();

        // Idiom 2 — volatile: the reader spins until it sees a non-null reference.
        // When it does, the volatile read guarantees it sees the COMPLETE object.
        Thread.ofPlatform().name("Writer-Volatile").start(() ->
            viaVolatile = new Config("volatile", 2)
        );
        Thread.ofPlatform().name("Reader-Volatile").start(() -> {
            while (viaVolatile == null) { Thread.onSpinWait(); } // spin until published
            // volatile read of viaVolatile happens-before reading its fields
            System.out.println("[volatile]   " + viaVolatile);
        }).join();

        // Idiom 3 — thread-safe collection: ConcurrentHashMap.get() sees
        // everything that was visible to the thread that called put().
        var mapLatch = new CountDownLatch(1);
        Thread.ofPlatform().name("Writer-Map").start(() -> {
            registry.put("cfg", new Config("concurrent-map", 3));
            mapLatch.countDown(); // signal: "I stored it" (not the publication mechanism)
        });
        Thread.ofPlatform().name("Reader-Map").start(() -> {
            try { mapLatch.await(); } catch (InterruptedException e) { return; }
            // safe publication comes from ConcurrentHashMap's internal synchronization
            System.out.println("[map]        " + registry.get("cfg"));
        }).join();

        // Idiom 4 — guarded by lock: unlock happens-before the next lock
        // on the same monitor, so the reader sees the complete object.
        var guardLatch = new CountDownLatch(1);
        Thread.ofPlatform().name("Writer-Guarded").start(() -> {
            synchronized (lock) {
                viaGuarded = new Config("guarded", 4);
            }
            guardLatch.countDown();
        });
        Thread.ofPlatform().name("Reader-Guarded").start(() -> {
            try { guardLatch.await(); } catch (InterruptedException e) { return; }
            synchronized (lock) {
                // lock acquisition sees everything before the prior unlock
                System.out.println("[guarded]    " + viaGuarded);
            }
        }).join();
    }
}
