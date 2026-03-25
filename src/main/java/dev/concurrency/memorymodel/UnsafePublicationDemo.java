package dev.concurrency.memorymodel;

/**
 * Demonstrates UNSAFE vs SAFE lazy singleton publication.
 *
 * The broken version has two bugs:
 *   1. Race condition — two threads see instance == null, both create an instance.
 *   2. Half-constructed object — Thread B can see a non-null reference where
 *      the constructor hasn't finished yet (JVM may reorder: assign reference
 *      BEFORE running the constructor body).
 *
 * Unsafe timeline:
 *   Thread A: sees instance == null → enters new Config() → JVM assigns reference
 *             → constructor STILL RUNNING (setting fields)
 *   Thread B: sees instance != null → uses it → reads default values (0, null) 💥
 *
 * Three correct fixes shown below, each using a different happens-before edge.
 */
public class UnsafePublicationDemo {

    // ========================
    // ❌ BROKEN — do NOT use
    // ========================
    static class BrokenSingleton {
        private static BrokenSingleton instance;
        private final int value;

        private BrokenSingleton() {
            value = 42; // this write may not be visible to other threads!
        }

        public static BrokenSingleton getInstance() {
            if (instance == null) {          // Thread A and B both read null
                instance = new BrokenSingleton(); // both create an instance
            }                                // OR: B sees non-null but value == 0
            return instance;
        }
    }

    // ========================
    // ✅ Fix 1: volatile field
    // ========================
    static class VolatileSingleton {
        private static volatile VolatileSingleton instance;
        private final int value;

        private VolatileSingleton() { value = 42; }

        public static VolatileSingleton getInstance() {
            if (instance == null) {
                synchronized (VolatileSingleton.class) {
                    if (instance == null) { // double-checked locking — safe with volatile
                        instance = new VolatileSingleton();
                    }
                }
            }
            return instance;
        }
    }

    // ========================
    // ✅ Fix 2: holder idiom (lazy, lock-free)
    // ========================
    static class HolderSingleton {
        private final int value;

        private HolderSingleton() { value = 42; }

        // Inner class is not loaded until getInstance() is called.
        // Class loading is synchronized by the JVM — happens-before guaranteed.
        private static class Holder {
            static final HolderSingleton INSTANCE = new HolderSingleton();
        }

        public static HolderSingleton getInstance() {
            return Holder.INSTANCE;
        }
    }

    // ========================
    // ✅ Fix 3: enum singleton (simplest)
    // ========================
    enum EnumSingleton {
        INSTANCE;
        private final int value = 42;
        public int getValue() { return value; }
    }

    public static void main(String[] args) throws InterruptedException {
        // Race the broken singleton — run multiple times to see inconsistency
        System.out.println("--- Broken singleton (race condition) ---");
        for (int i = 0; i < 5; i++) {
            // Reset the static field via reflection for demo purposes
            try {
                var field = BrokenSingleton.class.getDeclaredField("instance");
                field.setAccessible(true);
                field.set(null, null);
            } catch (Exception e) { throw new RuntimeException(e); }

            var results = new BrokenSingleton[2];
            Thread t1 = Thread.ofPlatform().name("T1").start(() -> results[0] = BrokenSingleton.getInstance());
            Thread t2 = Thread.ofPlatform().name("T2").start(() -> results[1] = BrokenSingleton.getInstance());
            t1.join(); t2.join();
            System.out.println("  same instance? " + (results[0] == results[1])
                + "  (identity: " + System.identityHashCode(results[0])
                + " vs " + System.identityHashCode(results[1]) + ")");
        }

        // Safe versions — always consistent
        System.out.println("--- Volatile singleton ---");
        System.out.println("  value = " + VolatileSingleton.getInstance().value);

        System.out.println("--- Holder idiom ---");
        System.out.println("  value = " + HolderSingleton.getInstance().value);

        System.out.println("--- Enum singleton ---");
        System.out.println("  value = " + EnumSingleton.INSTANCE.getValue());
    }
}
