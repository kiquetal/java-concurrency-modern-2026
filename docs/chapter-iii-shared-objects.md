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
      // @GuardedBy("this")
      private int availableConnections = 10;

      public synchronized void acquire() {
          availableConnections--;
      }
  }
  ```
  See in action: `src/main/java/dev/concurrency/sharingobjects/GuardedObjectExample.java`
