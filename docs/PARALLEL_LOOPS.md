# Control flow in a parallel loop

What `break` and `continue` mean once a loop is spread across machines, and
what SIPS gives you instead of the promise `break` cannot keep.

## Short answer

| | Works today | Needs coordination |
|---|---|---|
| `continue` | **Yes** — plain Java `continue` | None |
| `sim.breakAll(index, value)` | **Yes** — stop everything, carry an answer home | Yes |
| `sim.breakAfter(index, why)` | **Yes** — nothing past this index is wanted | Yes |
| `sim.breakLoop()` | **Deprecated** — never finished; use one of the two above | — |

## `continue` is free

Nothing is needed. Write it:

```java
sim.parallelFor();
for (int i = 0; i < records; i++) {
    if (skip(i)) {
        continue;          // correct as-is
    }
    process(i);
}
sim.endParallelFor();
```

Skipping an iteration is a purely local decision. The node executing that chunk
moves to its next index; no other node needs to know, and nothing about the
distribution changes. There is no API for this because none is required.

## `break` is not free, and cannot be

Sequential `break` promises: *no later iteration runs.*

A distributed loop cannot promise that. By the time node A decides to break at
index 40, node B may already have finished index 900 — it was handed that chunk
at the start. Enforcing the sequential promise would mean committing iterations
strictly in order, which discards the parallelism that justified distributing
the loop.

Everyone who has met this problem resolved it the same way, and renamed the
operation to stop implying a guarantee they could not keep:

| Framework | What it does |
|---|---|
| **OpenMP** | Forbids branching out of a parallel region entirely. Added `#pragma omp cancel` in 4.0 — cooperative, best-effort. |
| **Java streams** | `findFirst` (ordered, expensive) vs `findAny` (unordered, cheap) |
| **Rayon** | `find_first` vs `find_any` |
| **Intel TBB** | No break; `task_group::cancel()` |

SIPS follows the same precedent, and
[`EarlyExit`](../../SIPS-lib/src/main/java/in/co/s13/sips/lib/loop/EarlyExit.java)
states the guarantees explicitly:

**Guaranteed** — no *further* chunks are handed out; the decision is visible to
every node that checks after it is set; exactly one reason is recorded even if
several nodes cancel at once.

**Not guaranteed** — running iterations are not interrupted; iterations with a
later index may already have completed and their side effects stand; there is
no "lowest index wins" ordering.

## Where this actually matters

Cancellation is not a nicety. These are problems where finishing the loop after
the answer is known is the difference between seconds and hours.

### Search — the canonical case

```java
sim.parallelFor();
for (long candidate = 0; candidate < keyspace; candidate++) {
    if (matches(candidate)) {
        sim.breakAll(candidate, found(candidate));   // 8 nodes stop; skip the other 99%
    }
}
sim.endParallelFor();
```

Brute-force key recovery, preimage search, SAT solving. The answer is usually
found early in *someone's* range, and the remaining work is pure waste. Without
cancellation a 1-in-1000 hit still costs the full keyspace.

**`breakAll` is exactly right here.** Any match will do; which node found it and
whether a lower index also matched are irrelevant — and the value travels home
with the stop, so nothing else has to go looking for it.

### Branch and bound

```java
if (cost < bestKnownBound) {
    sim.breakAll(index, solution);   // everything worse is now unreachable
}
```

Optimisation, scheduling, TSP. A new bound prunes whole subtrees on *other*
nodes. Not cancelling means every node explores branches already known to be
dead.

### Anomaly and intrusion detection

Scanning a petabyte of logs for a signature. One hit is enough to raise the
alarm and start a different workflow; the remaining scan is wasted spend.

### Early stopping in a parameter sweep

A hyperparameter search where one configuration reaches the target metric. The
rest of the grid no longer needs evaluating — and each evaluation may be hours
of GPU time.

An iterative solver is the `breakAfter` case instead: iterations up to
convergence are the answer, and only the ones past it are waste.

### Where cancellation is the *wrong* tool

If correctness depends on stopping before a specific iteration — a loop with
ordered side effects, or one where iteration *n+1* must not run if *n* matched —
cancellation will not give you that, and no distributed loop can cheaply. Either
keep that loop sequential, or collect all matches and take the minimum index
afterwards.

## The two operations, and which to reach for

They answer different questions, and conflating them is how you end up with a
wrong answer rather than a slow one.

### `breakAll(index, value)` — search

*"I found it. Nobody needs to keep looking."*

Every chunk is cancelled, including ones that had not started. The finder
carries a value home, and the first report wins.

```java
sim.parallelFor();
for (long i = 0; i < keyspace; i++) {
    if (matches(i)) {
        sim.breakAll(i, describe(i));   // stop the cluster, bring the answer
    }
}
sim.endParallelFor();
```

It is **find-any, not find-first.** Node B's match at index 3 loses to node A's
at index 10 if A reported first. If you need the lowest matching index, collect
every match and take the minimum — do not use `breakAll`.

### `breakAfter(index, why)` — a prefix

*"Nothing past index N is wanted."*

Chunks entirely beyond the boundary are cancelled. Chunks before it must still
finish, because their results are part of the answer — and a chunk that
*straddles* the boundary still runs, because cancelling it would silently
truncate the result. The tighter of two boundaries wins.

```java
sim.parallelFor();
for (long i = 0; i < maxIterations; i++) {
    double error = refine(i);
    if (error < tolerance) {
        sim.breakAfter(i, "converged");   // keep 0..i, drop the rest
    }
}
sim.endParallelFor();
```

A `breakAll` arriving after a `breakAfter` overrides it: a definite answer makes
even the prefix unnecessary.

## How it is wired

Three layers, all built.

**1. The AST pass recognises the markers.** SIPS-Run records `breakAll` and
`breakAfter` into the `SYNTAX` table alongside the nine markers it already knew.
The match is exact rather than a substring — `"endParallelFor".contains("parallelFor")`
is true, and that class of bug is why.

**2. The node handles the command.** `TaskHandler` records the exit into
`GlobalValues.EARLY_EXIT`, keyed by job token, and kills chunks that
`shouldRunChunk` says are no longer wanted.

**3. Chunks observe it.** A chunk cancelled while still queued never starts. A
chunk already running is killed through `Process.destroy()` — except on the
WebAssembly path, where it cannot be (see below).

The bug that made `breakLoop` undeliverable for years is also fixed: the client
was sending its body under `"body"` while every handler read `"Body"`.

## WebAssembly chunks

A WASM module can call early exit itself, through the host interface:

```wat
(import "sips" "break_all"   (func (param i64 i32 i32)))   ;; index, ptr, len
(import "sips" "break_after" (func (param i64)))           ;; index
```

These reach the same `EarlyExit` state the Java API does, so a WASM search chunk
stops the cluster exactly as `sim.breakAll()` does. The value comes home as raw
bytes — only the module knows what its answer means.

**One difference, and it is not small:** WebAssembly has no interrupt, so a
module already running cannot be stopped. It finishes or hits its timeout.
Queued chunks are still cancelled. Size chunks accordingly if early exit is what
you are relying on. See [WASM_TASKS.md](../../SIPS-lib/docs/WASM_TASKS.md).

## Reporting it honestly

`EarlyExit` records when the stop was set, and the job report should show how
many iterations completed after it. On any real cluster that number is
non-zero. Surfacing it is the point: a user who assumed sequential `break`
needs to see that the assumption did not hold, rather than discovering it later
through a wrong result.
