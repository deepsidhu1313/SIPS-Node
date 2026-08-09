# Running a SIPS node

## Requirements

- **JDK 21**
- **Apache Ant** on `PATH` — each chunk is compiled and run through a generated
  Ant build, so a node without Ant accepts work and then fails every task
- Windows, macOS, Linux or Solaris

## Starting a node

```bash
java -jar SIPS-Node.jar
```

Modes:

```bash
java -jar SIPS-Node.jar --mode 0   # default: all servers, including ping
java -jar SIPS-Node.jar --mode 1   # private: no ping server, not discoverable
java -jar SIPS-Node.jar --mode 2   # master only: no ping, no task server
```

A node in mode 1 or 2 will not answer discovery probes, so peers must be told
about it explicitly with `--add-ip`.

## First-run setup

```bash
# Identify this node
java -jar SIPS-Node.jar --generate-app-uuid

# Benchmark the hardware (otherwise done automatically, refreshed daily)
java -jar SIPS-Node.jar --benchmark

# Tell it where to look for peers
java -jar SIPS-Node.jar --add-network 2 192.168.1.0 10.10.100.0
java -jar SIPS-Node.jar --add-ip 1 compute-node.example.com

# Issue an API key: 4 read, 2 write, 1 execute, summed
java -jar SIPS-Node.jar --gen-api 192.168.1.12 7
```

## Tuning concurrency

| Flag | Controls |
|---|---|
| `--set-process-limit N` | Chunks executed in parallel. Set to the core count. |
| `--set-file-resolvers N` | Threads fetching data from peers |
| `--set-ping-handlers N` | Threads answering discovery probes |
| `--set-process-handlers N` | Threads accepting new chunk submissions |
| `--set-api-handlers N` | Threads serving the API |

## Configuration files

All under `etc/`, all JSON, all editable while the node is stopped.

| File | Contents |
|---|---|
| `settings.json` | Ports, limits, storage flags |
| `api.json` | API keys and permission bits |
| `ips.json` | Individual hosts to probe |
| `networks.json` | Subnets to sweep |
| `blacklist.json` | Barred IPs, hostnames and UUIDs |
| `benchmarks.json` | Cached hardware benchmark, refreshed daily |

### Shared storage

With `--shared-storage`, several nodes may share one installation directory.
Config files are then prefixed per host (`<hostname>-api.json`) unless the
corresponding `HAS_COMMON_*` flag is set, in which case a single `common-`
prefixed file is shared.

## Housekeeping

```bash
java -jar SIPS-Node.jar --clean         # drop data/ and proc/
java -jar SIPS-Node.jar --clean-cache   # drop cache/
```

Logs live in `log/`, one file per service, rotated automatically.

## Submitting work

A job is a directory holding `manifest.json`, a `src/` tree and an optional
`lib/`. See [SIPS-samples](../../SIPS-samples) for two worked examples.

```json
{
    "PROJECT": "MatMul",
    "MAIN": "MatMul",
    "ARGS": [],
    "JVMARGS": [],
    "LIB": ["SIPS-lib-0.2-SNAPSHOT-jar-with-dependencies.jar"],
    "ATTCH": [],
    "OUTPUTFREQUENCY": 100,
    "SCHEDULER": {
        "Name": "in.co.s13.sips.schedulers.Chunk",
        "MaxNodes": "4"
    },
    "MASTER": {
        "HOST": "127.0.0.1",
        "API-PORT": "13139",
        "JOB-PORT": "13136",
        "API-KEY": "<key issued by the master>"
    }
}
```

`API-KEY` is a credential. Keep real manifests out of version control, and treat
any key that has been committed as compromised.

Submit with:

```bash
java -jar SIPS-Run.jar /path/to/project
```

## Troubleshooting

**Tasks are accepted but never finish.** Check Ant is on `PATH` on the executing
node, and read `log/tasks.log`.

**No peers are discovered.** Nodes in mode 1 or 2 do not answer probes. Confirm
port 13131 is reachable and that neither side is blacklisted.

**A node reports "Unsupported operating system".** `Platform` did not recognise
`os.name`. Supported values cover Windows, macOS, Linux, the BSDs, AIX and
Solaris.

**Chunks fail to compile.** The generated `build.xml` targets the JDK running the
node. A node on an older JDK than the submitter's source level will fail; keep
JDK versions aligned across the cluster.
