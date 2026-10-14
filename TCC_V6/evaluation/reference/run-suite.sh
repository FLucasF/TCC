#!/usr/bin/env bash
# Runs the full suite against one agent's service: a clean PostgreSQL in Docker, the agent's own jar
# (the one its `mvn verify` built), and the suite (reference.suite.Suite), which starts and restarts
# the service by itself. Everything the suite needs to know about the agent is its workspace.
#
# Usage (Git Bash, from TCC_V6):  evaluation/reference/run-suite.sh <agent-workspace> <output-dir>
#   evaluation/reference/run-suite.sh runs/pilots/OPUS55-N0-03/workspace runs/pilots/OPUS55-N0-03/suite
#
# Output: <output-dir>/suite-report.txt (what is printed), suite-result.json (every check) and
# service.log (the agent's service output, both runs: before and after the restart).
set -uo pipefail
die() { printf '\033[31mERROR: %s\033[0m\n' "$*" >&2; exit 1; }
[ $# -eq 2 ] || die "usage: $0 <agent-workspace> <output-dir>"
WORKSPACE="$1"; OUT="$2"
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"
DATABASE=suite-postgres
DB_PORT=55432

# the agent's jar: the shallowest target/*.jar that is not the ".original" Spring Boot leaves behind
JAR="$(find "$WORKSPACE" -path '*/target/*.jar' -not -name '*.original' -printf '%d %p\n' 2>/dev/null | sort -n | head -1 | cut -d' ' -f2-)"
[ -n "$JAR" ] || die "no jar under $WORKSPACE (did the build run?)"
mkdir -p "$OUT"
: > "$OUT/service.log"

# the suite itself: compile it and ask Maven for the classpath (Jackson)
( cd "$HERE" && mvn -B -q compile dependency:build-classpath -Dmdep.outputFile=classpath.txt >/dev/null ) \
    || die "the suite did not compile"
CLASSPATH="$(cygpath -w "$HERE/target/classes");$(cat "$HERE/classpath.txt")"

# a clean database, removed at the end whatever happens
docker rm -f "$DATABASE" >/dev/null 2>&1
docker run -d --name "$DATABASE" -e POSTGRES_PASSWORD=suite -e POSTGRES_DB=shop -p "$DB_PORT:5432" postgres:16-alpine >/dev/null \
    || die "could not start PostgreSQL"
trap 'docker rm -f "$DATABASE" >/dev/null 2>&1' EXIT
until docker exec "$DATABASE" psql -U postgres -d shop -c 'select 1' >/dev/null 2>&1; do sleep 1; done

echo "suite: $JAR"
DB_URL="jdbc:postgresql://localhost:$DB_PORT/shop" DB_USER=postgres DB_PASSWORD=suite \
    java -cp "$CLASSPATH" reference.suite.Suite \
        --jar "$(cygpath -w "$JAR")" \
        --out "$(cygpath -w "$OUT/suite-result.json")" \
        --log "$(cygpath -w "$OUT/service.log")" \
    | tee "$OUT/suite-report.txt"
