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

**Apache Ant must be on `PATH`** — chunks are compiled and executed through a
generated Ant build.

## Documentation

- **[Getting started](docs/GETTING_STARTED.md)** — zero to a distributed job in
  fifteen minutes, including the failure modes people actually hit
- [Parallel loops](docs/PARALLEL_LOOPS.md) — `break`, `continue`, early exit
- [Architecture](docs/ARCHITECTURE.md) — job flow, ports, how loop bounds are recovered
- [Operations](docs/OPERATIONS.md) — configuration, tuning, troubleshooting
- [Security](docs/SECURITY.md) — trust model and known gaps

> **Before deploying:** only the API port authenticates. The task server will
> compile and run anything sent to it. Firewall ports 13131–13139 to known
> cluster members. See [docs/SECURITY.md](docs/SECURITY.md).

## Layout

| Package | Role |
|---|---|
| `executor` | Task execution, distribution, generated build files |
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
