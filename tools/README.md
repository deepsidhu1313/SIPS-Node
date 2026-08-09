# Building every SIPS module at once

`aggregator-pom.xml` builds all six Maven modules in dependency order with a
single command. It is an aggregator, not a parent: the modules do not inherit
from it, so each repository still builds standalone.

```bash
mkdir sips && cd sips
for r in common-json common-sqlitejdbc SIPS-lib SIPS-Schedulers SIPS-Node SIPS-Run; do
  git clone --branch v1.1.0 https://github.com/deepsidhu1313/$r.git
done
curl -sLo pom.xml https://raw.githubusercontent.com/deepsidhu1313/SIPS-Node/v1.1.0/tools/aggregator-pom.xml
mvn install
```

Requires JDK 21. To build a single module instead, use its own `./mvnw verify`.

The module list in the aggregator is the authoritative build order — the
modules depend on each other and will not resolve if built out of sequence.
