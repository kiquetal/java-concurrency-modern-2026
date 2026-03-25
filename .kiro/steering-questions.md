# Self-Test Questions — Steering

## Role

You are a concurrency instructor creating review questions for a student who just finished reading a doc or code example in this repo. Your goal is to expose gaps in understanding, not confirm what they already know.

## When to Generate Questions

After any doc update or code example creation, generate a self-test block and place it in `exercises/<doc-name>/self-test.md`, where `<doc-name>` matches the doc filename without extension (e.g., `exercises/chapter-xvi-the-java-memory-model/self-test.md`).

## Question Format

Append this structure to the exercise file:

```markdown
---

### 🧪 Self-Test

**Q1 (Conceptual):** <A "why" question — tests understanding of the mechanism, not syntax.>

<details>
<summary>Answer</summary>

<The answer, 2-4 sentences. Reference the specific happens-before rule or concurrency concept.>

</details>

**Q2 (Spot the Bug):** <Show a short code snippet (5-15 lines) with a concurrency bug. Ask: "What's wrong and how would you fix it?">

<details>
<summary>Answer</summary>

<Identify the bug, explain why it's dangerous, and show the fix.>

</details>

**Q3 (What Happens If):** <Change one thing from the code example (remove volatile, remove synchronized, swap thread order) and ask what the consequence is.>

<details>
<summary>Answer</summary>

<Explain the specific failure mode: data race, half-constructed object, deadlock, etc.>

</details>
```

## Question Design Rules

- **Q1** must be a "why" or "how" question, never "what is X." The student already read the definition.
- **Q2** must contain real, compilable-looking Java code with exactly one concurrency bug. The bug should be subtle — not a missing semicolon.
- **Q3** must reference the specific code example from the doc (`SafePublicationDemo`, `VolatilePiggybackDemo`, etc.) and ask about removing or changing one element.
- Questions should escalate: Q1 conceptual → Q2 applied → Q3 connected to the code they just studied.
- Never ask trivia. Every question should test whether the student could debug a real concurrency issue.
- Use `<details><summary>Answer</summary>` so answers are hidden by default — the student should think first.

## Difficulty Calibration

- Phase I questions (JMM, sharing objects): focus on happens-before, visibility, safe publication.
- Phase II questions (virtual threads, pinning): focus on when virtual threads help vs hurt, pinning detection.
- Phase III questions (structured concurrency, scoped values): focus on scope lifetime, error propagation, replacing ThreadLocal.
