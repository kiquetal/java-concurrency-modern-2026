### Chapter 3- Shared Objects

#### Notes

- Locking is not just about mutual exclusionl it is also about memory visibility.
To ensure that all threads see the most up-to-date values of shared mutable varibles, the reading and
writing threads must synchronize on a common lock.

#### Volatile

A weaker form of synchronization, think they behave like SyncrhonizedInteger


Locking can guarantee both visibility and atomicity; volatile variables can only guarantee
visibility..

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
