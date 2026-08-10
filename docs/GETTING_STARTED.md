# Getting started

From nothing to a job running across two machines. About fifteen minutes.

This page assumes you have never seen SIPS. Every command is copy-pasteable and
every step says what you should see, so you can tell where you are if something
goes wrong.

## What SIPS does, in one example

You write ordinary Java. You mark one loop. It runs across every machine you
have.

```java
SIPS sim = new SIPS("MatMul");

sim.parallelFor();
for (int i = 0; i < 1000; i++) {
    // ... your existing loop body, unchanged ...
}
sim.endParallelFor();
```

There is no task class to write, no `Callable` to implement, no futures to
join. The loop above is the same loop you would have written for one machine.

## Before you start

Two things, on **every** machine that will run work:

| | Check with | If missing |
|---|---|---|
| **JDK 21+** | `java -version` | Install a JDK 21 or newer |
| **Apache Ant** | `ant -version` | `brew install ant` / `apt install ant` |

**Ant is not optional.** Each chunk is compiled and run through a generated Ant
build. A node without Ant accepts work and then fails every chunk — see
[Troubleshooting](#troubleshooting), because this is the single most common
first-run failure.

## 1. Build

```bash
git clone https://github.com/deepsidhu1313/SIPS-Node.git
git clone https://github.com/deepsidhu1313/SIPS-lib.git
git clone https://github.com/deepsidhu1313/SIPS-Run.git
git clone https://github.com/deepsidhu1313/SIPS-Schedulers.git
git clone https://github.com/deepsidhu1313/common-json.git
git clone https://github.com/deepsidhu1313/common-sqlitejdbc.git

curl -sLo pom.xml https://raw.githubusercontent.com/deepsidhu1313/SIPS-Node/master/tools/aggregator-pom.xml
mvn install
```

**You should see** `BUILD SUCCESS` and seven modules listed. If Maven cannot
resolve `in.co.s13:*`, you built the modules out of order — the aggregator
handles that, so use it rather than building each by hand.

## 2. Start a node

```bash
cd SIPS-Node
java -jar target/SIPS-Node-1.1.0-jar-with-dependencies.jar
```

**You should see** the node print its detected hardware, then a line per server
as each starts — ping, file, task, job, API.

Leave it running. That terminal is your first node.

> **Before you put this on a shared network, read
> [SECURITY.md](SECURITY.md).** The task server accepts and runs code with no
> authentication. On a laptop or a lab LAN behind a firewall this is fine. On
> anything else it is not.

## 3. Issue yourself an API key

In a second terminal, in the same directory:

```bash
java -jar target/SIPS-Node-1.1.0-jar-with-dependencies.jar --gen-api 127.0.0.1 7
```

**You should see** a JSON block containing a `key`. Copy it — you need it in the
next step.

The `7` is permissions: `4` read + `2` write + `1` execute. Use `4` for a
monitoring-only client.

## 4. Run the sample

```bash
git clone https://github.com/deepsidhu1313/SIPS-samples.git
cd SIPS-samples/MatMul
```

Open `manifest.json` and paste your key into `API-KEY`. Then:

```bash
java -jar ../../SIPS-Run/target/SIPS-Run-1.1.0-jar-with-dependencies.jar .
```

**You should see** the node terminal print that it received chunks, and the job
finish. Results land in `data/`.

That is a distributed job. On one machine it used one node; the next step adds
more.

## 5. Add a second machine

On the second machine, build and start a node exactly as above. Then tell the
first machine where to look:

```bash
java -jar target/SIPS-Node-1.1.0-jar-with-dependencies.jar --add-ip 1 192.168.1.42
```

or sweep the whole subnet:

```bash
java -jar target/SIPS-Node-1.1.0-jar-with-dependencies.jar --add-network 1 192.168.1.0
```

Restart the node. **You should see** the second machine appear in its live-node
table within a scan cycle. Run the sample again — the chunks now spread across
both.

Raise `MaxNodes` in `manifest.json` if you want more than the default four.

## Choosing a scheduler

`manifest.json` names one:

```json
"SCHEDULER": { "Name": "in.co.s13.sips.schedulers.Chunk", "MaxNodes": "4" }
```

This choice matters more than people expect. On a loop where every iteration
costs the same, all seven policies perform identically. On a loop where cost
varies — which is most real problems — the gap is large:

| Policy | Irregular workload, 8 nodes |
|---|---|
| `QSS` | 93% efficiency |
| `Factoring` | 90% |
| `GSS` | 89% |
| `Chunk` (default) | **79%** |

That is a fifth of your cluster, lost to the default. Measure yours rather than
guess — see [Choosing a scheduler](#choosing-a-scheduler-for-your-workload)
below.

| Policy | Use when |
|---|---|
| `Chunk` | Iterations cost roughly the same |
| `GSS`, `Factoring`, `TSS`, `QSS` | Cost varies between iterations |
| `GA`, `GATDS` | Tasks have dependencies between them |
| `DeviceAware` | Nodes differ in hardware and the work is GPU-suited |

### Choosing a scheduler for your workload

You do not need a cluster to compare them. Describe your workload's cost
distribution and evaluate every policy offline:

```java
Workload work = Workload.skewed("my-job", 64, 5, 500);   // chunks, min, max cost
for (Evaluation result : Evaluator.compare(work, 8)) {
    System.out.println(result);
}
```

Prints each policy's makespan, speedup, efficiency and load imbalance, best
first.

## Control flow inside the loop

`continue` works, unchanged — it is a local decision needing no coordination.

`break` is different, and currently **not supported**: `sim.breakLoop()` exists
but is not wired end to end. More importantly, `break` cannot mean in a
distributed loop what it means sequentially. [PARALLEL_LOOPS.md](PARALLEL_LOOPS.md)
explains why and what will replace it.

## Troubleshooting

Ordered by how often each actually happens.

### The job is accepted but every chunk fails

**Ant is not on `PATH`** on the executing node. This is the most common
first-run problem by a wide margin.

```bash
ant -version        # on the node, not the submitter
```

Check `log/tasks.log` on that node for the compiler output.

### No other nodes are found

- Nodes started with `--mode 1` or `--mode 2` **do not answer discovery
  probes**. Add them explicitly with `--add-ip`.
- Port `13131` must be reachable. A host firewall will silently prevent
  discovery.
- Check neither side is blacklisted: `--blacklist` with no arguments lists it.
- Discovery uses a subnet sweep and **cannot cross NAT**. Machines on different
  networks will not find each other.

### "You are not allowed" from the API

The key does not match, or the client's identity is not in `etc/api.json`. Keys
are issued per IP, hostname or UUID — one issued for `127.0.0.1` will not work
from another machine. Issue another.

### Chunks fail to compile on some nodes but not others

The generated build targets the JDK running that node. A node on an older JDK
than your source level will fail. Keep JDK versions aligned across the cluster.

### Everything is slow, and one node finishes long after the others

Load imbalance — you are probably using `Chunk` on a workload whose iterations
differ in cost. See [Choosing a scheduler](#choosing-a-scheduler) above.

### It worked once, then stopped

```bash
java -jar SIPS-Node.jar --clean         # clears data/ and proc/
java -jar SIPS-Node.jar --clean-cache   # clears cache/
```

## What to read next

| | |
|---|---|
| [ARCHITECTURE.md](ARCHITECTURE.md) | How a job flows through the system, and how loop bounds are recovered |
| [OPERATIONS.md](OPERATIONS.md) | Configuration, tuning, shared storage |
| [SECURITY.md](SECURITY.md) | Trust model and open issues — read before any shared network |
| [PARALLEL_LOOPS.md](PARALLEL_LOOPS.md) | `break`, `continue`, and early exit |
| [ACCELERATORS.md](../../SIPS-lib/docs/ACCELERATORS.md) | GPU kernels and image tiling |

## Known rough edges

Stated plainly, so you find them here rather than the hard way:

- **The task server has no authentication.** Firewall ports 13131–13139.
- **A running WebAssembly chunk cannot be interrupted.** It finishes or hits its
  timeout; only queued chunks are cancelled. Java chunks are killable.
- **Nothing here has been run against a live cluster** since the pipeline,
  WebAssembly and cluster-call work landed.
- **The IDE and mobile client have not been run against a live cluster** since
  their last rebuild.
- **`saveArrayElement`, `updateArrayElement` and `resolveArrayElement` do
  nothing** — no node handler exists. They are marked deprecated.

`break` and Windows paths used to be on this list. `sim.breakAll()` and
`sim.breakAfter()` are wired end to end as of 1.2.0, and every path in the
framework is built with `SipsPaths` as of 1.2.3 — with a test in each repository
that fails the build if one is glued together again.
