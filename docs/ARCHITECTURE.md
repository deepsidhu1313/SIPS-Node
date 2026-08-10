# SIPS architecture

How a job travels from annotated Java source to distributed execution and back.

## The idea

A user writes ordinary Java and marks the regions they want parallelised:

```java
SIPS sim = new SIPS("MatMul");

sim.simulateSection();          // runs once, locally, to capture runtime values
// ... build the input matrices ...
sim.saveValues("" + nra, "" + ncb, "" + nca);
sim.saveObject("a", 0, a);
sim.endSimulateSection();

a = (double[][]) sim.resolveObject("a", 0);   // fetched from the submitter

sim.parallelFor();              // the loop below is split across the cluster
for (int i = 0; i < nra; i++) {
    ...
}
sim.endParallelFor();
```

Nothing about the loop body changes. SIPS works out the loop's bounds, decides
how to split them, and runs a copy of the program on each node with a different
slice of the iteration space.

## How the bounds are discovered

This is the part that distinguishes SIPS from a job runner.

`SIPS-Run` parses the project with **JavaParser** and writes the resulting AST
into **SQLite tables** rather than keeping it in memory:

| Table | Holds |
|---|---|
| `SYNTAX` | Marker calls, including where each `parallelFor` begins and ends |
| `FORLOOP` | Loop init, comparison and update expressions, by line |
| `VARIABLES` | Declared types, by line |
| `VARDEC` | Variable initialisers |
| `BINARYEXP` | Binary expressions — the right-hand side gives a loop's limit |
| `SAVVAL` | Checkpoints linking source lines to captured runtime values |

Loop bounds are then recovered with SQL queries against that database. When a
bound is a literal (`i < 1000`) it is read directly. When it is a variable
(`i < nra`) the parsed value is not enough, so SIPS falls back to the
**simulation database**: the `simulateSection()` pass ran the program once and
recorded the actual runtime values via `saveValues()`.

That two-database lookup — parsed AST plus captured runtime state — is what lets
SIPS chunk loops whose extent is not known until run time.

## Job flow

```
SIPS-Run                          Master node                    Peer nodes
   |                                   |                              |
   |  1. parse AST -> .parsed/*.db     |                              |
   |  2. run simulation -> .simulated/ |                              |
   |  3. START_JOB (port 13136) -----> |                              |
   |                                   | 4. Job reads bounds from     |
   |                                   |    the two databases         |
   |                                   | 5. Scheduler splits the      |
   |                                   |    iteration space           |
   |                                   | 6. Distributor sends each    |
   |                                   |    chunk (port 13133) -----> |
   |                                   |                              | 7. write source,
   |                                   |                              |    generate build.xml,
   |                                   |                              |    run ant
   |                                   | <---- 8. stdout, timings ----|
   |                                   | <---- 9. results via file ---|
   |                                   |          server (13135)      |
```

## Ports

| Port | Server | Purpose |
|---|---|---|
| 13131 | Ping | Liveness probe used by peer discovery |
| 13132 | FileDownload | Serves cached files to peers |
| 13133 | Task | Receives chunks to execute |
| 13134 | TaskFinishListener | Completion notifications |
| 13135 | File | Serialized objects and results |
| 13136 | Job | Job submission and status |
| 13139 | API | Administration |

Only the API server authenticates. See [SECURITY.md](SECURITY.md).

## Peer discovery

Nodes find each other autonomously rather than through a registry:

1. `NetScanner` sweeps the configured subnets from `etc/networks.json` and the
   hosts in `etc/ips.json`.
2. `RangePinger` probes port 13131 on each candidate.
3. Responding nodes enter `LIVE_NODE_ADJ_DB`, keyed by node UUID.
4. `Dijkstra` over the resulting graph gives a distance between any two nodes,
   which schedulers use to prefer nearby peers.

Nodes advertise a benchmark record — CPU model, a SciMark 2.0 score, disk
throughput, memory — refreshed daily. Schedulers rank candidates on queue wait,
queue length, benchmark score and distance, in that order.

## Scheduling

`Scheduler` is a small interface in SIPS-lib:

```java
ArrayList<SIPSTask> schedule(map of nodes, map of tasks, settings);
ArrayList<ParallelForSENP> scheduleParallelFor(map of nodes, loop, settings);
```

A job names its scheduler in `manifest.json`. Built-in implementations:

| Scheduler | Strategy |
|---|---|
| `Chunk` | Equal-sized chunks, round-robin over the selected nodes |
| `Factoring` | Diminishing batches, half the remainder each round |
| `GSS` | Guided self-scheduling |
| `TSS` / `QSS` | Trapezoid and quadratic self-scheduling |
| `GA` | Genetic algorithm over the assignment space |
| `GATDS` | Genetic algorithm with task-dependency awareness |

A job may also supply its own scheduler as a serialized object. See
[SECURITY.md](SECURITY.md) for why that path needs care.

Two newer, smaller interfaces sit alongside it, each one method, because the
eight-method `Scheduler` is the reason nobody writes a new policy:

| Interface | Question it answers |
|---|---|
| `LoopPolicy` | how big is the next batch of one iteration space |
| `PlacementPolicy` | which of several ready tasks goes to which node |

Both can be compared against the classics offline — `Evaluator` and
`DagEvaluator` in SIPS-Schedulers, no cluster required.

## Execution on a node

`ParallelProcess` takes one of two paths, decided by the manifest's `TYPE`
field. It is declared rather than inferred from which other fields happen to be
set — a manifest naming no executor it meant gets a sentence back instead of a
surprise.

### Java chunks (`TYPE` absent or `java`)

1. `TaskHandler` accepts a `createprocess` command carrying source, manifest and
   chunk number.
2. `ParallelProcess` writes the source under `proc/<uuid>/<job>/<chunk>/`.
3. It generates an Ant `build.xml` targeting the running JDK, resolved by
   [`JavaTarget`](../src/main/java/in/co/s13/SIPS/tools/JavaTarget.java).
4. [`Platform`](../src/main/java/in/co/s13/SIPS/tools/Platform.java) builds the
   launch command for the host OS, and
   [`ExecutorScripts`](../src/main/java/in/co/s13/SIPS/executor/ExecutorScripts.java)
   generates the matching launcher script at startup.
5. Ant compiles and runs the chunk. stdout streams back to the master every
   `OUTPUTFREQUENCY` lines.
6. Results travel back as serialized objects through the file server, not as
   text.

### WebAssembly chunks (`TYPE` is `wasm`)

No Ant, no javac, no fork. The module arrives precompiled and runs inside the
node's own process through Chicory, a WebAssembly runtime written in pure Java —
a native runtime would reintroduce exactly the per-node toolchain requirement
that makes the Ant path painful.

1. The module and a `chunk.json` naming the iteration range travel in the
   per-chunk directory, the same channel that carries generated sources. Nothing
   in the distribution path changes.
2. `WasmRunner` parses the module once and instantiates it per chunk, so one
   chunk's output can never leak into another's.
3. The module reaches the outside world only through the six `sips` host
   functions — input, output, log, and the two early-exit calls. There is no file
   access, no network and no clock, which is why it is safe in-process. That list
   is the security model, and the runtime enforces it.
4. A result under 256 KB rides home inside the finish message; anything larger
   stays on disk for the file server.

Cost: single-digit milliseconds to parse, then well under a millisecond per
chunk, against hundreds of milliseconds for javac plus JVM startup. That is what
puts a floor under how small a chunk can usefully be — and removing the floor is
what lets a scheduler balance ragged work.

**One limitation, and it is not small:** WebAssembly has no interrupt, so a
module already running cannot be killed. It finishes or hits its timeout. See
[WASM_TASKS.md](../../SIPS-lib/docs/WASM_TASKS.md).

## Pipelines

A manifest declaring `STAGES` is a graph rather than a single loop, and takes a
second path from `JobHandler`: `StagedJob` instead of `Job`. Every manifest
without `STAGES` — which is every manifest written before stages existed — goes
through the original path unchanged.

```
JobHandler ──┬─ manifest has STAGES ──> StagedJob ──> JobRunner ──> DistributedStageRunner
             └─ otherwise ────────────> Job (single loop, as before)
```

`JobSequencer` releases a stage the moment *its own* dependencies finish, not at
a round boundary and not when a whole level is done. Chunk numbers are handed out
from one counter across the whole job, because the distribution table is keyed
`nodeUuid-chunkNumber` and two stages numbering from zero would collide.

See [TASK_GRAPHS.md](../../SIPS-lib/docs/TASK_GRAPHS.md).

## Node-local state

| Directory | Contents |
|---|---|
| `etc/` | `settings.json`, `api.json`, `ips.json`, `networks.json`, `blacklist.json`, `benchmarks.json` |
| `data/<job>/` | Job manifest, source, `.parsed/`, `.simulated/`, `.result/` |
| `proc/<uuid>/<job>/<chunk>/` | One execution sandbox per chunk |
| `cache/<uuid>/` | Files fetched from peers, checksummed with `.sha` siblings |
| `log/` | Per-service logs, rotated by `LogRotate` |

In-memory tables live in `GlobalValues`: `TASK_DB` (running chunks, keyed via
[`TaskKeys`](../src/main/java/in/co/s13/SIPS/datastructure/TaskKeys.java)),
`MASTER_DIST_DB` (distribution records), `RESULT_DB` (job results) and
`LIVE_NODE_ADJ_DB` (the peer graph).

## Known structural constraints

Worth knowing before extending the framework:

- **There is no result-merge primitive.** Chunks write their own outputs; the
  framework does not reduce or reassemble them. A pipeline can express *that* a
  merge stage runs last, but the merging itself is yours to write.
- **`Job` resolves bounds through eight near-identical per-primitive branches.**
  Adding a new bound type currently means adding a ninth. `StagedJob` sidesteps
  this — a stage carries explicit bounds — but the single-loop path still has it.
- **A pipeline cannot name a custom `PlacementPolicy`** the way a job names a
  scheduler. It gets the default.
- **`DistributedStageRunner` has not been run against a live cluster.** Its
  polling half is tested against a populated distribution table; the distributing
  half needs nodes.

Two constraints listed here previously are now fixed, noted in case older notes
still cite them: the task transport is byte-exact (`FilePayload` marks each
payload base64 or UTF-8, so images survive it), and two-dimensional tiling with
halo exists as `TileGrid` and `Tile` in SIPS-lib.
