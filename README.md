# Java Concurrency Modern 2026

Learning journey through modern Java concurrency, based on:

- **Java Concurrency in Practice** — Brian Goetz et al.
- **Modern Concurrency in Java** — A N M Bazlur Rahman
- **Java Structured Concurrency** — Anghel Leonard
- **Understanding Java Virtual Threads** — Fu Cheng

## Structure

```
docs/       → Markdown notes (migrating to Astro later)
src/        → Code examples and demos
exercises/  → Self-test questions per chapter
```

## 📚 Notes

| Chapter | Source | Doc |
| :--- | :--- | :--- |
| Ch. 3 — Sharing Objects | JCiP | [docs/chapter-iii-shared-objects.md](docs/chapter-iii-shared-objects.md) |
| Ch. 16 — The Java Memory Model | JCiP | [docs/chapter-xvi-the-java-memory-model.md](docs/chapter-xvi-the-java-memory-model.md) |

## 💻 Code Examples

### Sharing Objects (`dev.concurrency.sharingobjects`)

| File | Topic |
| :--- | :--- |
| [GuardedObjectExample](src/main/java/dev/concurrency/sharingobjects/GuardedObjectExample.java) | Guarded-by pattern |
| [ThreadLocalDemo](src/main/java/dev/concurrency/sharingobjects/ThreadLocalDemo.java) | ThreadLocal usage and cleanup |
| [SharedThreadSafeExample](src/main/java/dev/concurrency/sharingobjects/SharedThreadSafeExample.java) | Thread-safe shared state |
| [ImmutableUser](src/main/java/dev/concurrency/sharingobjects/ImmutableUser.java) | Immutability with defensive copies |
| [TryLockRetryExample](src/main/java/dev/concurrency/sharingobjects/TryLockRetryExample.java) | tryLock with random backoff |
| [ThreadConfinedExample](src/main/java/dev/concurrency/sharingobjects/ThreadConfinedExample.java) | Thread confinement |
| [SharedReadOnlyExample](src/main/java/dev/concurrency/sharingobjects/SharedReadOnlyExample.java) | Read-only shared data |

### Memory Model (`dev.concurrency.memorymodel`)

| File | Topic |
| :--- | :--- |
| [VolatilePiggybackDemo](src/main/java/dev/concurrency/memorymodel/VolatilePiggybackDemo.java) | Piggybacking visibility on volatile |
| [SafePublicationDemo](src/main/java/dev/concurrency/memorymodel/SafePublicationDemo.java) | 4 safe publication idioms |
| [UnsafePublicationDemo](src/main/java/dev/concurrency/memorymodel/UnsafePublicationDemo.java) | Broken singleton + 3 fixes |

## 🧪 Exercises

Self-test questions organized by chapter — see [exercises/README.md](exercises/README.md).

| Chapter | Questions | Topics |
| :--- | :--- | :--- |
| [Ch. III — Shared Objects](exercises/chapter-iii-shared-objects/self-test.md) | 8 | Publication/escape, ThreadLocal, deadlock/livelock |
| [Ch. XVI — Java Memory Model](exercises/chapter-xvi-the-java-memory-model/self-test.md) | 9 | Piggybacking, safe publication, happens-before, `join()` |

## 🗺️ Chronological Study Plan

### Phase I: Foundations (The Physics)
- [x] **Java Concurrency in Practice** (JCiP)
  - Ch. 3: Sharing Objects
  - Ch. 16: The Java Memory Model

### Phase II: The Mechanics (The Shift)
- [ ] **Modern Concurrency in Java** (Bazlur Rahman)
  - Ch. 4: Virtual Threads
  - Ch. 7: Carrier Thread Pinning
- [ ] **Understanding Java Virtual Threads** (Fu Cheng)
  - The Scheduler & Continuations

### Phase III: The Architecture (Patterns)
- [ ] **Java Structured Concurrency** (Anghel Leonard)
  - Ch. 2: StructuredTaskScope
  - Ch. 5: Scoped Values
