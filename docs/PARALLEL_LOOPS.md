# Control flow in a parallel loop

What `break` and `continue` mean once a loop is spread across machines, which
of them SIPS supports today, and how the missing one should be built.

## Short answer

| | Works today | Needs coordination |
|---|---|---|
| `continue` | **Yes** — plain Java `continue` | None |
| `break` | **No** — `sim.breakLoop()` is not wired | Yes, and it cannot mean what it means sequentially |

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

SIPS should follow the same precedent, and
[`LoopCancellation`](../../SIPS-lib/src/main/java/in/co/s13/sips/lib/loop/LoopCancellation.java)
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
        found = candidate;
        sim.breakLoop();     // 8 nodes stop; do not search the other 99%
    }
}
sim.endParallelFor();
```

Brute-force key recovery, preimage search, SAT solving. The answer is usually
found early in *someone's* range, and the remaining work is pure waste. Without
cancellation a 1-in-1000 hit still costs the full keyspace.

**Cancellation semantics are exactly right here.** Any match will do; which node
found it and whether a lower index also matched are irrelevant.

### Branch and bound

```java
if (cost < bestKnownBound) {
    recordSolution(cost);
    sim.breakLoop();         // everything worse is now unreachable
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

### Where cancellation is the *wrong* tool

If correctness depends on stopping before a specific iteration — a loop with
ordered side effects, or one where iteration *n+1* must not run if *n* matched —
cancellation will not give you that, and no distributed loop can cheaply. Either
keep that loop sequential, or collect all matches and take the minimum index
afterwards.

## How it should be built

The transformation route is right; three pieces are missing, one per layer.

**1. The AST pass must recognise the marker.** SIPS-Run's pass currently
recognises nine (`parallelFor`, `endParallelFor`, `simulateSection`,
`endSimulateSection`, `saveValues`, `saveObject`, `resolveObject`, `defineTask`,
`endTask`). Add `breakLoop`, and rewrite a call inside a parallel region into
two statements — signal the master, then a real Java `break` so the node stops
its own chunk immediately:

```java
// sim.breakLoop();   becomes:
SipsRuntime.cancel(jobToken, "breakLoop at Search.java:42");
break;
```

**2. The node must handle the command.** `TaskHandler` has cases for
`createprocess`, `kill`, `printoutput` and six others but not `breakLoop`. It
needs one that sets `LoopCancellation` for the job and stops the executor
handing out queued chunks. Note the existing client sends its body under
`"body"` while every handler reads `"Body"` — that must be fixed or the handler
will never see it.

**3. Nodes must observe it.** Two paths, and both are wanted: the master pushes
cancellation to nodes holding chunks of that job, and a long-running chunk polls
`shouldContinue()` between iterations so it can stop mid-chunk rather than at
the next chunk boundary.

The scheduler side is already in place — cancellation only has to make
`shouldContinue()` false for the loop being scheduled.

## Reporting it honestly

`LoopCancellation` counts iterations that completed *after* cancellation was
set, and the job report should show it. On any real cluster that number is
non-zero. Surfacing it is the point: a user who assumed sequential `break`
needs to see that the assumption did not hold, rather than discovering it later
through a wrong result.
