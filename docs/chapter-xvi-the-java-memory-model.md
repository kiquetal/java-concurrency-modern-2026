### Chapter XVI: The Java Memory Model

##### Platform Memory Model

In a shared-memory multiprocessor architecture,
each processor has its own cache that is 
periodically synchronized with the main memory.


The JMM (Java Memory Model) defines the rules for how threads interact through memory,
Defines a partial ordering called "happens-before" that guarantees visibility and ordering of memory operations across threads.
Example: If Thread A writes to a variable and then Thread B reads that variable, the JMM ensures that if Thread A's write happens-before Thread B's read, then Thread B will see the updated value.
If there is no happens-before relationship, then the behavior is undefined, and Thread B may see a stale value or even a completely different value.

![happens-before visibility](happens-before-visibility.png)

#### Piggybacking on Synchronization

"Piggybacking" means exploiting an **existing** happens-before relationship to make other, unrelated writes visible — without adding extra synchronization for them.

The key insight: happens-before is **transitive**. If A happens-before B, and B happens-before C, then A happens-before C. This means that *all* writes performed by a thread before a synchronization point become visible to the thread that observes that synchronization point — not just the write to the synchronized variable itself.

**Example: `FutureTask` piggybacking**

`FutureTask` stores its result in a non-volatile field. How is the result safely published to the thread that calls `get()`? It piggybacks on the happens-before established by writing to an internal `volatile` state field:

```
Thread A (runs the task):
  1. result = computeExpensiveValue()    // write to non-volatile field
  2. state = COMPLETED                   // write to volatile field  ← synchronization point

Thread B (calls get()):
  3. read state == COMPLETED             // read volatile field      ← synchronization point
  4. return result                       // read non-volatile field
```

Step 2 happens-before step 3 (volatile write → volatile read rule).
Step 1 happens-before step 2 (program order within Thread A).
By transitivity: step 1 happens-before step 3, so step 4 sees the correct `result`.

Thread A "piggybacks" the visibility of `result` on the `volatile` write to `state`. No extra lock or volatile needed for `result` itself.

**Why it matters:**
- It's a performance optimization — avoids redundant synchronization.
- It's how many `java.util.concurrent` classes work internally (`FutureTask`, `CountDownLatch`, `Semaphore`).
- It's **fragile** — if you refactor and accidentally remove the synchronization point, the piggybacked writes silently become invisible. The code compiles, passes tests, and fails under load.

**The happens-before rules that enable piggybacking:**
1. **Program order rule** — within a single thread, each action happens-before the next action.
2. **Volatile variable rule** — a write to a `volatile` field happens-before every subsequent read of that field.
3. **Monitor lock rule** — an unlock on a monitor happens-before every subsequent lock on that same monitor.
4. **Transitivity** — if A happens-before B, and B happens-before C, then A happens-before C.

Piggybacking combines rules 1 + 4 with either rule 2 or 3: the program order rule chains all prior writes to the synchronization point, and transitivity carries that visibility across to the reading thread.

> **Rule of thumb:** Don't piggyback in application code unless you have a very good reason. Use explicit synchronization (`volatile`, `synchronized`, `Lock`) to make your intent clear. Piggybacking is a tool for library authors who need every last drop of performance and can prove correctness.

See in action: `src/main/java/dev/concurrency/memorymodel/VolatilePiggybackDemo.java`


