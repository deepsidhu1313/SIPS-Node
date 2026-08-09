# SIPS security model

## Trust assumption

SIPS was built for a trusted laboratory LAN. Its design assumes every host that
can reach a node's ports is authorised to use it.

**Nothing in the code enforces that assumption.** Deploying a node on a network
where that does not hold gives every reachable host the ability to run arbitrary
code as the node's user. Treat the port range as equivalent to an unauthenticated
remote shell and firewall it accordingly.

## What is authenticated today

| Port | Server | Authentication |
|---|---|---|
| 13139 | API | API key plus permission bits |
| 13131 | Ping | Blacklist only |
| 13133 | Task | **None** |
| 13136 | Job | **None** |
| 13135 | File | **None** |
| 13132 | FileDownload | **None** |
| 13134 | TaskFinishListener | **None** |

API keys live in `etc/api.json` and are generated with:

```bash
java -jar SIPS-Node.jar --gen-api 192.168.1.12 7
```

Permission bits are POSIX-style: `4` read, `2` write, `1` execute, summed.
Evaluation lives in
[`AccessControl`](../src/main/java/in/co/s13/SIPS/tools/AccessControl.java).

## Blacklisting

```bash
java -jar SIPS-Node.jar --blacklist 2 192.168.1.30 rogue-node-uuid
```

Entries may be IP addresses, hostnames or node UUIDs, and are stored in
`etc/blacklist.json`. A blacklist match is **absolute**: it is checked before the
API key and cannot be waived by holding a valid key.

Note that a client's UUID is asserted by the caller and never proven, so UUID
blacklisting stops honest misconfiguration, not a determined attacker. IP-based
entries are the load-bearing ones.

## Open issues

These are known and unfixed. They are listed here so operators can judge
exposure rather than discover it.

### 1. The task server executes anything it is sent

`TaskHandler`'s `createprocess` command accepts Java source plus a manifest and
compiles and runs it. There is no key check and no blacklist check. Any host
able to reach port 13133 achieves remote code execution.

*Mitigation until fixed:* firewall 13131–13139 to known cluster members.

### 2. Custom schedulers are Java-deserialized

A job may name a scheduler that is loaded with `Util.deserialize()` from a file
under the job's own directory. Java deserialization of attacker-influenced data
is a gadget-chain sink.

*Mitigation:* only accept jobs from trusted submitters; prefer the built-in
schedulers, which are selected by name and never deserialized.

### 3. Path traversal in the file servers

`FileHandler` and `FileDownloadHandler` build paths by concatenating
network-supplied `PID` and `FILE` values:

```java
new File("data/" + pid + "/" + filenameToSend)
```

Neither value is normalised, so `../` escapes the job directory.

### 4. SQL built by string concatenation

Roughly fifty queries interpolate values directly, some of them
network-supplied. `SQLiteJDBC` has no prepared-statement support, which is the
prerequisite for fixing them.

### 5. API keys compared with `equalsIgnoreCase`

Not constant-time, so key comparison is theoretically timing-observable. Low
severity on a LAN, but worth changing when the comparison is next touched.

## Fixed

For reference, these were live and have been closed:

- **Blacklist bypass.** The check read
  `(!BLACKLIST.contains(ip) || !BLACKLIST.contains(uuid))`. Because the UUID is
  caller-supplied, a blacklisted host only had to invent an unlisted UUID to make
  the disjunction true. The blacklist is now absolute and covered by tests.
- **Negative permissions granted full access.** A malformed key yielding `-1`
  has every bit set, so all permission checks passed. Non-positive values now
  grant nothing.
- **Command injection in `deleteFile`.** The path was concatenated into a shell
  string and passed to `Runtime.exec(String)`; a crafted job token could append a
  second command. It now uses the platform-neutral file API and never invokes a
  shell.

## Reporting

Security issues should go to the maintainers privately rather than through
public issues.
