# SIPS-Node

[![build](https://github.com/deepsidhu1313/SIPS-Node/actions/workflows/build.yml/badge.svg)](https://github.com/deepsidhu1313/SIPS-Node/actions/workflows/build.yml)

The SIPS daemon. A node discovers its peers, advertises its hardware, accepts
chunks of work, compiles and runs them, and reports results back.

Every node is a peer: the same binary acts as submitter, scheduler and worker
depending on which servers it is started with.

## Build

```bash
./mvnw verify
```

Requires **JDK 21**. Runs on Windows, macOS, Linux and Solaris.

## Run

```bash
java -jar target/SIPS-Node-0.1-SNAPSHOT-jar-with-dependencies.jar --help
```

**Apache Ant must be on `PATH`** for Java chunks — they are compiled and
executed through a generated Ant build. WebAssembly chunks need nothing but the
JVM: the module arrives precompiled and runs in the node's own process.

## What a job can be

| Manifest says | What runs | Needs Ant |
|---|---|---|
| nothing extra | one marked `for` loop, split across nodes | yes |
| `"TYPE": "wasm"` | a precompiled WebAssembly module | no |
| `"STAGES": [...]` | a pipeline: parallel stages, single stages, dependencies | per stage |

A pipeline says its ordering once, so the cluster is not drained to idle between
steps the way separate submissions leave it.

## Upgrading

Run the new build. `Settings.init()` brings the databases and settings files up
to date at every startup, and `bin/` scripts are rewritten when the task server
starts — so there is no upgrade step to remember. See
[MIGRATIONS.md](../SIPS-lib/docs/MIGRATIONS.md).

Nodes can be upgraded one at a time. Each announces which wire protocol it
speaks on every ping, and work that an older node would accept and then fail is
scheduled onto the nodes that can run it — with the ones left out named in the
job log. See [PROTOCOL.md](../SIPS-lib/docs/PROTOCOL.md).

## Documentation

- **[Getting started](docs/GETTING_STARTED.md)** — zero to a distributed job in
  fifteen minutes, including the failure modes people actually hit
- [Parallel loops](docs/PARALLEL_LOOPS.md) — `break`, `continue`, early exit
- [WebAssembly chunks](../SIPS-lib/docs/WASM_TASKS.md) — the host interface and its limits
- [Task graphs](../SIPS-lib/docs/TASK_GRAPHS.md) — pipelines and placement policies
- [Migrations](../SIPS-lib/docs/MIGRATIONS.md) — upgrading an existing node
- [Wire protocol](../SIPS-lib/docs/PROTOCOL.md) — running a half-upgraded cluster
- [Architecture](docs/ARCHITECTURE.md) — job flow, ports, how loop bounds are recovered
- [Operations](docs/OPERATIONS.md) — configuration, tuning, troubleshooting
- [Security](docs/SECURITY.md) — trust model and known gaps

> **Before deploying:** only the API port authenticates. The task server will
> compile and run anything sent to it. Firewall ports 13131–13139 to known
> cluster members. See [docs/SECURITY.md](docs/SECURITY.md).

## Layout

| Package | Role |
|---|---|
| `executor` | Task execution, distribution, generated build files, pipelines |
| `executor.sockets` | The seven servers, and their handlers |
| `Scanner` | Peer discovery and liveness |
| `datastructure` | Node, task, result and graph types |
| `tools` | Platform detection, access control, logging, utilities |
| `benchmarks` | SciMark 2.0 based hardware scoring |
| `settings` | Global state and configuration loading |

## Contributing

Work is test-first. A bug fix starts with a failing test that reproduces it.

The classes in `tools` — `Platform`, `JavaTarget`, `AccessControl`, `CpuInfo` —
exist because the logic they hold was previously buried in socket handlers and
could not be tested. Prefer extending them over adding new branches inline.

```bash
./mvnw test
```
