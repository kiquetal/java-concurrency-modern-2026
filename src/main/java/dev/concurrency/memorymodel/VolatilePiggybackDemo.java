package dev.concurrency.memorymodel;

/**
 * Demonstrates volatile piggybacking.
 *
 * `result` is NOT volatile, but its write becomes visible to Thread B
 * because it piggybacks on the volatile write/read of `ready`.
 *
 * Timeline:
 *   Thread A: result = 42  →  ready = true   (volatile write flushes BOTH)
 *   Thread B: read ready == true              (volatile read refreshes BOTH)
 *             read result == 42               (visible thanks to piggybacking)
 *
 * Without `volatile` on `ready`, Thread B could see ready=true but result=0.
 */
public class VolatilePiggybackDemo {

    private static int result = 0;           // NOT volatile
    private static volatile boolean ready = false; // volatile — the synchronization point

    public static void main(String[] args) throws InterruptedException {
        Thread writer = new Thread(() -> {
            result = 42;    // step 1: non-volatile write
            ready = true;   // step 2: volatile write — flushes result too
        }, "Writer");

        Thread reader = new Thread(() -> {
            // Spin with a bounded check to avoid spinning forever.
            // In real code, use CountDownLatch or similar — this spin
            // exists only to demonstrate the volatile piggybacking effect.
            for (int i = 0; i < 1_000_000; i++) {
                if (ready) { // step 3: volatile read — refreshes result too
                    // step 4: guaranteed to see 42, thanks to piggybacking
                    System.out.println("result = " + result);
                    return;
                }
                else  {
                 System.out.println("Not ready yet...");
                         }
            }
            System.out.println("Timed out waiting for writer");
        }, "Reader");

        // we start the `reader` first because I want to know how the volatile read behaves when the writer hasn't written yet. If we started the writer first, the reader might see ready=true immediately and we wouldn't see the "Not ready yet..." messages.
        reader.start();
        writer.start();
        reader.join();
        writer.join();
    }
}
