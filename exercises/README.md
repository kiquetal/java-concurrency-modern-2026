# Exercises — Java Concurrency Self-Tests

Self-test questions organized by chapter. Each exercise uses collapsible answers — try to reason through the question before revealing the solution.

## Structure

### [Chapter III — Shared Objects](chapter-iii-shared-objects/self-test.md)

8 questions covering three topic areas:

- **Publication and Escape (Q1–Q3)** — Why `this` escaping a constructor is dangerous, spotting unsafe publication in a Spring component, and understanding defensive copies for immutability.
- **ThreadLocal (Q4–Q5)** — Why `ThreadLocal` breaks down with virtual threads, and a missing `remove()` that causes a security vulnerability in a web filter.
- **Deadlock and Livelock (Q6–Q8)** — How global lock ordering prevents deadlock, why fixed backoff causes livelock, and what happens when you forget to release a lock on retry failure.

### [Chapter XVI — The Java Memory Model](chapter-xvi-the-java-memory-model/self-test.md)

9 questions covering four topic areas:

- **Piggybacking on Synchronization (Q1–Q3)** — How `FutureTask` safely publishes a non-volatile result, spotting a missing `volatile` on a done flag, and what breaks when you remove `volatile` from a piggyback demo.
- **Safe Publication (Q4–Q6)** — The difference between `static final` fields and `static` methods, broken double-checked locking without `volatile`, and what happens when a spin-wait reads a non-volatile reference.
- **Happens-Before Reasoning (Q9)** — Tracing a full happens-before chain through volatile piggybacking to prove visibility.
- **`join()` and Happens-Before (Q7–Q8)** — Whether `join()` orders threads relative to each other, and when `volatile` is redundant because `join()` already provides the visibility guarantee.

## Question Types

| Type | What it tests |
|---|---|
| **Conceptual** | Understanding the "why" behind a concurrency rule |
| **Spot the Bug** | Finding a concurrency defect in realistic code |
| **What Happens If** | Predicting behavior when a safety mechanism is removed |
| **Trace the Chain** | Walking through happens-before edges step by step |
