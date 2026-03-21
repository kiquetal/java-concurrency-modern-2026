### Chapter 3- Shared Objects

#### Notes

- Locking is not just about mutual exclusion; it is also about memory visibility.
To ensure that all threads see the most up-to-date values of shared mutable variables, the reading and
writing threads must synchronize on a common lock.

#### Volatile

A weaker form of synchronization, think they behave like SynchronizedInteger


Locking can guarantee both visibility and atomicity; volatile variables can only guarantee
visibility.

---

### 3.2 Publication and Escape

**Publishing** an object means making it available to code outside its current scope — returning it from a method, storing it in a public field, passing it to another class.

**Escape** is when an object is published unintentionally, or before it's ready.

#### Escaping internal mutable state

Returning a direct reference to a private field lets callers modify your internals:

```java
public class UserProfile {
    private List<String> roles = new ArrayList<>();

    public UserProfile() {
        roles.add("USER");
    }

    public List<String> getRoles() {
        return roles;  // the private list has escaped
    }
}
```

Any caller can now do `profile.getRoles().add("ADMIN")` — the `private` keyword is meaningless. Fix: return `List.copyOf(roles)`.

#### The `this` reference escaping during construction

If `this` is published before the constructor finishes, other threads can see a half-built object.

**Unsafe — `this` escapes via inner class:**

```java
public class ThisEscape {
    public ThisEscape(EventSource source) {
        source.registerListener(new EventListener() {
            public void onEvent(Event e) {
                doSomething(e);  // inner class holds hidden ref to ThisEscape.this
            }
        });
    }
}
```

The anonymous `EventListener` captures `ThisEscape.this`. When `registerListener()` runs, the enclosing `ThisEscape` instance is published — but the constructor hasn't finished yet. Another thread could fire an event and call `doSomething()` on an incomplete object.

**Safe — factory method separates construction from publication:**

```java
public class SafeListener {
    private final EventListener listener;

    private SafeListener() {
        listener = new EventListener() {
            public void onEvent(Event e) {
                doSomething(e);
            }
        };
    }

    public static SafeListener newInstance(EventSource source) {
        SafeListener safe = new SafeListener();  // fully constructed
        source.registerListener(safe.listener);  // THEN published
        return safe;
    }
}
```

The timeline:
1. `new SafeListener()` — constructor runs to completion, all fields set. The listener exists but nobody outside knows about it.
2. `source.registerListener(safe.listener)` — now the listener is shared. But `SafeListener` is already complete, so any thread that fires an event sees a fully built object.
3. `return safe` — caller gets the reference. Safe because the object is already complete.

The inner class still holds a hidden `this` reference — that hasn't changed. What changed is *when* that reference becomes reachable by other threads: after construction, not during.

The constructor is `private` so nobody can bypass the factory.

#### The rule

> Don't let `this` leak out of the constructor — don't pass it to external code, don't register it, don't start threads with it, don't publish inner classes that capture it.

A constructor doing *internal* work (setting fields, creating internal collections) is fine. The danger is publishing `this` to the outside world before the constructor returns.

#### Where this shows up in practice

- **GUI frameworks** (Swing, JavaFX, Android): wiring listeners in constructors
- **Spring**: calling `bus.register(this)` in a constructor instead of using `@PostConstruct`
- **Starting threads**: `new Thread(this::process).start()` inside a constructor
- **Observer/pub-sub**: `feed.subscribe(this)` in a constructor

These bugs are silent — the code compiles, passes tests, and works 99.9% of the time. Under load, another thread sees a default value (`0`, `null`) instead of the initialized one, causing a crash that's nearly impossible to reproduce.

### ThreadLocal

A `ThreadLocal` variable provides thread-local storage: each thread has its own independent copy of the variable. This is useful for maintaining per-thread state without synchronization.

```java
public class ThreadLocalExample {
    private static final ThreadLocal<Integer> threadLocalCount = ThreadLocal.withInitial(() ->
        0
    );

    public static void increment() {
        threadLocalCount.set(threadLocalCount.get() + 1);
    }

    public static int getCount() {
        return threadLocalCount.get();
    }
}
```
In this example, each thread has its own `threadLocalCount`. When one thread calls `increment()`, it only affects that thread's count. No synchronization is needed because threads don't share the variable.

See in action: `src/main/java/dev/concurrency/sharingobjects/ThreadLocalDemo.java`

#### Real-world use cases

1. **Database connections / transactions (Spring `@Transactional`)** — Spring stores the current `Connection` in a `ThreadLocal` so every repository call within a `@Transactional` method uses the same connection without passing it explicitly.

2. **User context in web applications** — Each HTTP request runs on its own thread. A servlet filter stores the authenticated user in a `ThreadLocal` at the start, and any service layer code can access it without needing the `HttpServletRequest`:
    ```java
    public class UserContext {
        private static final ThreadLocal<String> currentUser = new ThreadLocal<>();
        public static void set(String userId) { currentUser.set(userId); }
        public static String get() { return currentUser.get(); }
        public static void clear() { currentUser.remove(); }
    }
    ```

3. **MDC in logging (SLF4J / Logback)** — `MDC.put("requestId", "abc-123")` stores the request ID in a `ThreadLocal<Map>`. Every log line in that thread automatically includes it without passing it through every method.

4. **Non-thread-safe objects** — `SimpleDateFormat` is not thread-safe. Before Java 8's `DateTimeFormatter`, the common fix was wrapping it in a `ThreadLocal` so each thread gets its own instance.

#### ThreadLocal and virtual threads — a warning

`ThreadLocal` works well with platform threads (hundreds of threads = hundreds of copies). With virtual threads (potentially millions), each one gets its own copy — that's a memory problem. This is why Java introduced **Scoped Values** as the virtual-thread-friendly replacement. See Phase III notes on Scoped Values (Ch. 5).


#### Immutability

An immutable object is one whose state cannot change
after construction. Immutable objects are thread-safe by design, because their state cannot be modified after they are created. This means that multiple threads can safely share references to the same immutable object without any synchronization.

Immutable objects are always thread-safe, but thread-safe objects are not always immutable. For example, a `Vector` is thread-safe because its methods are synchronized, but it is not immutable because its state can change after construction.

```java
public final class ImmutableUser {
    private final String name;
    private final List<String> roles;

    public ImmutableUser(String name, List<String> roles) {
        this.name = name;
        this.roles = List.copyOf(roles); // Make a defensive copy
    }

    public String getName() { return name; }
    public List<String> getRoles() { return roles; } // Returns an unmodifiable list
}
```

See in action: `src/main/java/dev/concurrency/sharingobjects/ImmutableUser.java`

#### Sharing objects safely

- **Thread-confined**: A thread-confined object is only accessed by a single thread. No synchronization is needed because no other thread can see it.
  ```java
  public void processItems() {
      // confinedList is strictly thread-confined
      List<String> confinedList = new ArrayList<>();
      confinedList.add("Item 1");
  }
  ```
  See in action: `src/main/java/dev/concurrency/sharingobjects/ThreadConfinedExample.java`

- **Shared read-only**: If an object is shared between threads but never modified after construction, it is thread-safe. This is often achieved by making the object immutable.
  ```java
  public class Config {
      // Shared read-only object
      public static final Map<String, String> SETTINGS = Map.of("timeout", "5000");
  }
  ```
  See in action: `src/main/java/dev/concurrency/sharingobjects/SharedReadOnlyExample.java`

- **Shared thread-safe**: If an object is shared between threads and can be modified, it must be designed to be thread-safe. This typically involves using synchronization, volatile variables, or concurrent data structures to ensure that all threads see a consistent view of the object's state.
  ```java
  public class ActiveUsers {
      // Shared thread-safe concurrent collection
      private final ConcurrentHashMap<String, String> users = new ConcurrentHashMap<>();

      public void login(String user) {
          users.put(user, "Active");
      }
  }
  ```
  See in action: `src/main/java/dev/concurrency/sharingobjects/SharedThreadSafeExample.java`

- **Guarded**: A guarded object can be accessed only with a specific lock held. The lock protects the object's state from concurrent access, ensuring thread safety.
  ```java
  public class ConnectionPool {
      private final Lock lock = new ReentrantLock();
      // @GuardedBy("lock")
      private int availableConnections = 10;

      public void acquire() {
          lock.lock();
          try {
              availableConnections--;
          } finally {
              lock.unlock();
          }
      }
  }
  ```
  See in action: `src/main/java/dev/concurrency/sharingobjects/GuardedObjectExample.java`


### 3.3 Lock Hazards: Deadlock and Livelock

Using explicit locks (like `ReentrantLock`) or intrinsic locks (`synchronized`) is powerful, but introduces liveness hazards: the system might stop making progress.

#### Deadlock
A deadlock occurs when two or more threads are blocked forever, waiting for each other to release locks.

**Scenario:**
- Thread A acquires Lock 1.
- Thread B acquires Lock 2.
- Thread A tries to acquire Lock 2 (blocks, because B holds it).
- Thread B tries to acquire Lock 1 (blocks, because A holds it).
Neither thread can proceed. They are stuck indefinitely.

```java
// Deadlock example: Two threads trying to transfer money between two accounts.
public void transfer(Account from, Account to, double amount) {
    synchronized (from) {
        synchronized (to) {
            from.debit(amount);
            to.credit(amount);
        }
    }
}

// If Thread A calls transfer(acc1, acc2, 100) and Thread B calls transfer(acc2, acc1, 50) at the same time:
// 1. Thread A locks acc1.
// 2. Thread B locks acc2.
// 3. Thread A tries to lock acc2 (blocks).
// 4. Thread B tries to lock acc1 (blocks).
// DEADLOCK!
```

**How to avoid Deadlock:**
Always acquire locks in a consistent, global order. If you need to lock two accounts, lock the one with the smaller ID first.

```java
public void transferSafe(Account from, Account to, double amount) {
    // Determine a global lock order (e.g., by unique ID)
    Account firstLock = from.getId() < to.getId() ? from : to;
    Account secondLock = from.getId() < to.getId() ? to : from;

    synchronized (firstLock) {
        synchronized (secondLock) {
            from.debit(amount);
            to.credit(amount);
        }
    }
}
```
This guarantees that whether Thread A transfers `acc1` to `acc2` and Thread B transfers `acc2` to `acc1`, *both* threads will lock `acc1` first. The first thread to reach `synchronized(firstLock)` proceeds, while the second thread waits. Deadlock is mathematically impossible.

**Why does this work? (Timeline comparison)**
*Assume Account 1 has ID=100 and Account 2 has ID=200.*

**Before (The Bug):**
- Thread A (Acc1 -> Acc2) locks Acc1.
- Thread B (Acc2 -> Acc1) locks Acc2.
- Thread A tries to lock Acc2 -> blocked by Thread B.
- Thread B tries to lock Acc1 -> blocked by Thread A. (Deadlock)

**After (The Fix):**
- Thread A (Acc1 -> Acc2) determines first lock is Acc1 (ID 100 < 200).
- Thread B (Acc2 -> Acc1) determines first lock is Acc1 (ID 100 < 200).
- Thread A locks Acc1.
- Thread B tries to lock Acc1 -> blocked by Thread A. (Thread B safely waits, no deadlock)
- Thread A locks Acc2, does the transfer, and releases both locks.
- Thread B wakes up, locks Acc1, locks Acc2, does the transfer, and releases both locks.

#### Livelock
A livelock occurs when threads are not blocked, but they continuously change their state in response to each other without making any real progress.

**Analogy:** Two people meet in a narrow hallway. Person A steps to their right to let B pass. Person B steps to their left to let A pass. They are now blocking each other again. They keep stepping side-to-side indefinitely. They are "active" but no progress is made. In code, this often happens when threads use `tryLock()` and back off, but their backoff logic forces them into an endless retry loop in lockstep.

```java
// Livelock example: Two threads trying to politely let the other go first.
public void workerAction(Worker otherWorker) {
    while (this.isActive) {
        if (otherWorker.isActive) {
            // Be polite, sleep and let the other finish
            try {
                Thread.sleep(10);
                continue; // Restart the loop, backoff
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        // Perform work...
        this.isActive = false;
    }
}
// If Worker A and Worker B are both active, they will both see the other is active,
// both sleep for 10ms, wake up, see the other is still active, and sleep again indefinitely.
```

**How to avoid Livelock:**
Introduce **randomness** or **asymmetry** into the retry logic so threads break out of sync.

```java
public void workerAction(Worker otherWorker) {
    Random random = new Random();
    while (this.isActive) {
        if (otherWorker.isActive) {
            try {
                // Sleep for a RANDOM duration to desynchronize their loops
                Thread.sleep(random.nextInt(50)); 
                continue;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        // Perform work...
        this.isActive = false;
    }
}
```
By backing off for a random amount of time (e.g., 0 to 50ms), one thread will wake up before the other, realize the other is still sleeping, and complete its work. The endless "hallway dance" is broken.

#### Rules and Tips to Avoid Lock Hazards

> **Rule #1: Enforce a Global Lock Order.**
> If all threads always acquire locks in the exact same order (e.g., always Lock 1 then Lock 2), deadlocks involving those locks are mathematically impossible. If you need to lock two objects, order them by a unique property (like a database ID or `System.identityHashCode`).

- **Use Timed Locks (`tryLock`):** Instead of blocking forever with `.lock()`, use `.tryLock(timeout, TimeUnit)`. If the lock isn't acquired in time, the thread can back off, release its current locks, wait a random amount of time (to avoid livelock), and retry.
- **Keep Lock Blocks Small:** Hold locks for the shortest possible time. Only include the code that actually accesses the shared state. Do not perform expensive operations (like I/O or network calls) while holding a lock.
- **Never Call Alien Methods While Holding a Lock:** An "alien method" is a method whose implementation you don't control (e.g., an overridden method or a listener callback). Calling it while holding a lock is dangerous because the alien method might try to acquire another lock, violating your lock ordering and causing a deadlock.