# Self-Test: Chapter XVI — The Java Memory Model

---

### 🧪 Piggybacking on Synchronization

**Q1 (Conceptual):** Why does `FutureTask` store its result in a non-volatile field, yet the thread calling `get()` always sees the correct value? Which happens-before rules make this work, and in what order?

<details>
<summary>Answer</summary>

`FutureTask` piggybacks on a `volatile` state field. The chain is: (1) program order rule — the result write happens-before the volatile write to `state` within the same thread, (2) volatile variable rule — the volatile write to `state` happens-before the volatile read of `state` by the calling thread, (3) transitivity — therefore the result write happens-before the calling thread reads it. The result field doesn't need to be volatile because the volatile state field carries its visibility.

</details>

**Q2 (Spot the Bug):** What's wrong with this code?

```java
public class TaskResult {
    private int result;
    private boolean done;  // not volatile

    public void complete(int value) {
        result = value;
        done = true;
    }

    public int get() {
        while (!done) { Thread.onSpinWait(); }
        return result;
    }
}
```

<details>
<summary>Answer</summary>

`done` is not `volatile`, so the reader thread may never see `done = true` (the compiler or CPU can cache it). Even if it does see `done = true`, there's no happens-before edge, so `result` might still be `0`. Fix: make `done` volatile. The volatile read/write will piggyback the visibility of `result`.

</details>

**Q3 (What Happens If):** In `VolatilePiggybackDemo`, what happens if you remove `volatile` from the `ready` field?

<details>
<summary>Answer</summary>

Two possible failures: (1) the reader thread may spin forever because it never sees `ready = true` — the JIT compiler can hoist the read out of the loop since `ready` is not volatile, turning it into an infinite loop. (2) Even if the reader does see `ready = true` by luck, there's no happens-before edge, so it could read `result = 0` instead of `42`. The piggybacking effect is completely gone without `volatile`.

</details>

---

### 🧪 Safe Publication

**Q4 (Conceptual):** Why is a `static final` field safely published but a `private static` field assigned in a `static` method is not? Both use the keyword `static` — what's the actual difference?

<details>
<summary>Answer</summary>

A `static final` field is initialized during class loading, which the JVM synchronizes internally — it holds a lock so only one thread initializes the class, and all other threads are blocked until it completes. This creates a happens-before edge. A `static` method is just a regular method call with no synchronization — the keyword `static` only means "no `this` pointer." Two threads can call it simultaneously with no happens-before between them.

</details>

**Q5 (Spot the Bug):** What's wrong with this singleton?

```java
public class ConnectionPool {
    private static ConnectionPool instance;
    private final List<Connection> pool;

    private ConnectionPool() {
        pool = new ArrayList<>();
        for (int i = 0; i < 10; i++) pool.add(createConnection());
    }

    public static ConnectionPool getInstance() {
        if (instance == null) {
            synchronized (ConnectionPool.class) {
                if (instance == null) {
                    instance = new ConnectionPool();
                }
            }
        }
        return instance;
    }
}
```

<details>
<summary>Answer</summary>

Classic broken double-checked locking. The `instance` field is not `volatile`. Thread B can read `instance != null` outside the synchronized block (the first `if` check) and use it — but without a happens-before edge, it may see a half-constructed object where `pool` is `null` or partially filled. Fix: add `volatile` to `instance`. The volatile read in the outer `if` will establish the happens-before edge.

</details>

**Q6 (What Happens If):** In `SafePublicationDemo`, what would happen if `viaVolatile` was a plain (non-volatile) field and the reader still used a spin-wait loop?

<details>
<summary>Answer</summary>

Two failures: (1) the spin loop `while (viaVolatile == null)` may never terminate — the JIT can cache the field read since it's not volatile, turning it into an infinite loop. (2) Even if the reader escapes the loop, there's no happens-before edge, so it could see a `Config` reference where `name` is `null` and `version` is `0` — a half-constructed object. The volatile keyword is what makes both the reference AND the object's fields visible.

</details>

---

### 🧪 `join()` and Happens-Before

**Q7 (Conceptual):** If you start 4 threads and then call `join()` on all 4 from main, does `join()` create any ordering between the 4 threads themselves?

<details>
<summary>Answer</summary>

No. `join()` creates a happens-before from each thread to main, but NOT between the threads. The 4 threads run concurrently with no ordering between them — they can execute in any order, interleave arbitrarily, and finish in any order. `join()` only tells main: "wait until this thread finishes, then you can see what it did." The threads are invisible to each other through `join()`.

</details>

**Q8 (Spot the Bug):** A developer writes this and claims the volatile is unnecessary because `join()` handles visibility. Are they right?

```java
private static volatile Config config;

Thread writer = new Thread(() -> config = new Config("test", 1));
writer.start();
writer.join();
// reader runs on main thread after join
System.out.println(config);
```

<details>
<summary>Answer</summary>

They're right — in this specific case. `join()` is a full happens-before edge: all of the writer's actions (including the `config` assignment and the `Config` constructor) happen-before `join()` returns to main. Since main reads `config` directly after `join()`, the volatile is redundant here. However, if another thread (not main) were reading `config` without going through `join()`, the volatile would be necessary. The volatile is only redundant because the reader is the same thread that called `join()`.

</details>
