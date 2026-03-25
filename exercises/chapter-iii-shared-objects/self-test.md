# Self-Test: Chapter III — Shared Objects

---

### 🧪 Publication and Escape

**Q1 (Conceptual):** Why is publishing `this` from a constructor dangerous, even if the object "looks" fully initialized by the time another thread uses it?

<details>
<summary>Answer</summary>

The JMM makes no guarantees about visibility of constructor writes to other threads unless a happens-before edge exists. Even if the constructor has finished by wall-clock time, without synchronization the other thread may see default field values (`0`, `null`, `false`). The object reference was published before the happens-before edge was established — so the other thread can see a half-constructed object. The factory method pattern fixes this by separating construction (private) from publication (after construction completes).

</details>

**Q2 (Spot the Bug):** What's wrong with this Spring component?

```java
@Component
public class EventProcessor {
    private final EventBus bus;
    private final List<String> cache = new ArrayList<>();

    public EventProcessor(EventBus bus) {
        this.bus = bus;
        cache.add("default");
        bus.register(this);  // publish this to the bus
    }

    public void onEvent(String event) {
        cache.add(event);
    }
}
```

<details>
<summary>Answer</summary>

`this` escapes during construction via `bus.register(this)`. If the `EventBus` fires an event on another thread before the constructor returns, `onEvent()` runs on a half-constructed `EventProcessor`. Even though `cache` is initialized above the `register()` call in source code, the JMM allows reordering — another thread might see `cache` as `null`. Fix: move `bus.register(this)` to a `@PostConstruct` method, or use a factory method that constructs first, then registers.

</details>

**Q3 (What Happens If):** In the `ImmutableUser` example, what happens if you change `List.copyOf(roles)` to just `roles` in the constructor?

<details>
<summary>Answer</summary>

The `ImmutableUser` is no longer truly immutable. The caller retains a reference to the original list and can mutate it: `roles.add("ADMIN")` would modify the list inside the "immutable" object. Other threads reading `getRoles()` would see the mutation — and since `ArrayList` is not thread-safe, concurrent reads and writes could cause `ConcurrentModificationException` or corrupted state. `List.copyOf()` creates a defensive copy that is both independent and unmodifiable.

</details>

---

### 🧪 ThreadLocal

**Q4 (Conceptual):** Why is `ThreadLocal` a problem with virtual threads, even though it works perfectly with platform threads?

<details>
<summary>Answer</summary>

Platform threads are expensive to create, so applications typically have hundreds at most — each `ThreadLocal` copy is a small overhead. Virtual threads are cheap and designed for millions of concurrent tasks. Each virtual thread gets its own `ThreadLocal` copy, so a million virtual threads means a million copies of every `ThreadLocal` variable. This can exhaust memory. Additionally, `ThreadLocal` requires explicit `remove()` to avoid leaks, which is error-prone at scale. `ScopedValue` solves both problems: it's immutable (no cleanup needed) and automatically inherited by child virtual threads without copying.

</details>

**Q5 (Spot the Bug):** What's wrong with this web filter?

```java
public class AuthFilter implements Filter {
    private static final ThreadLocal<String> currentUser = new ThreadLocal<>();

    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) {
        currentUser.set(((HttpServletRequest) req).getHeader("X-User"));
        chain.doFilter(req, res);
    }

    public static String getCurrentUser() {
        return currentUser.get();
    }
}
```

<details>
<summary>Answer</summary>

Missing `currentUser.remove()` after `chain.doFilter()`. In a thread pool (like a servlet container), the thread is reused for the next request. If the next request doesn't set the header, `getCurrentUser()` returns the previous request's user — a security vulnerability. Fix: add a `finally` block that calls `currentUser.remove()` after `chain.doFilter()`.

</details>

---

### 🧪 Deadlock and Livelock

**Q6 (Conceptual):** Global lock ordering breaks which Coffman condition, and why does that make deadlock impossible?

<details>
<summary>Answer</summary>

It breaks condition #4: circular wait. If all threads acquire locks in the same global order (e.g., always Lock A before Lock B), then Thread 1 holding A and waiting for B can never be blocked by Thread 2 holding B and waiting for A — because Thread 2 would also need to acquire A first, and would be blocked waiting for Thread 1 to release it. The wait graph can never form a cycle, so deadlock is mathematically impossible.

</details>

**Q7 (Spot the Bug):** What's wrong with this livelock "fix"?

```java
public void transfer(Account from, Account to, int amount) {
    while (true) {
        if (from.lock.tryLock()) {
            try {
                if (to.lock.tryLock()) {
                    try {
                        from.debit(amount);
                        to.credit(amount);
                        return;
                    } finally { to.lock.unlock(); }
                }
            } finally { from.lock.unlock(); }
        }
        Thread.sleep(10);  // fixed backoff
    }
}
```

<details>
<summary>Answer</summary>

The backoff is a fixed duration (`10ms`), not random. If two threads call `transfer(acc1, acc2)` and `transfer(acc2, acc1)` at the same time, they'll both fail, both sleep for exactly 10ms, both wake up at the same time, and both fail again — forever. This is a livelock. Fix: use `Thread.sleep(random.nextInt(50))` so the threads desynchronize and one can proceed while the other is still sleeping.

</details>

**Q8 (What Happens If):** In the `TryLockRetryExample`, what happens if you remove the `finally { inventoryLock.unlock(); }` block that runs when the second lock fails?

<details>
<summary>Answer</summary>

If the second lock (`ledgerLock`) fails to acquire, the first lock (`inventoryLock`) is never released. The thread retries the loop still holding `inventoryLock`. Now every other thread trying to acquire `inventoryLock` will also fail their `tryLock`, and after `MAX_RETRIES` they all give up. You've turned a livelock-prevention mechanism into a resource leak that causes all operations to fail. The `finally` block is critical — it ensures the first lock is always released when the second lock can't be acquired.

</details>
