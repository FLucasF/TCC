# What the review scripts share, sourced by them (the pipeline copies this file next to each skill's scripts): where
# the results go, the project's pom, the build, the jar that starts on its own, a database of its own, and the
# service started against it.
#
# The review container has the review's PostgreSQL in PGHOST, PGPORT, PGUSER and PGPASSWORD (a superuser), and the
# scripts create one database per measurement, so one never sees another's data.

# measures/ next to the project folder (/review/projeto -> /review/measures)
results_folder() {
    local folder
    folder="$(cd "$1/.." && pwd)/measures"
    mkdir -p "$folder"
    echo "$folder"
}

# the name of a result: $1, or $1-base when the measured folder is the maintenance base (to compare with the final)
result_name() {
    if [ "$(basename "$2")" = "base" ]; then echo "$1-base"; else echo "$1"; fi
}

# the folder of the shallowest pom.xml under $1 (the project root), or nothing
project_root() {
    local pom
    pom="$(find "$1" -name pom.xml -not -path '*/target/*' -printf '%d %p\n' 2>/dev/null | sort -n | head -1 \
        | cut -d' ' -f2-)"
    [ -n "$pom" ] && dirname "$pom"
}

# mvn package without the tests, quiet; the log in $2; returns Maven's exit code
build_package() {
    (cd "$1" && timeout 900 mvn -B -q -DskipTests package) > "$2" 2>&1
}

# the jar under $1 whose manifest has a Main-Class (the biggest, when there are several), or nothing
main_jar() {
    local jar scratch
    scratch="$(mktemp -d)"
    while read -r jar; do
        rm -rf "${scratch:?}"/*
        if (cd "$scratch" && jar xf "$jar" META-INF/MANIFEST.MF 2>/dev/null) \
                && grep -q '^Main-Class:' "$scratch/META-INF/MANIFEST.MF" 2>/dev/null; then
            rm -rf "$scratch"
            echo "$jar"
            return 0
        fi
    done < <(find "$1" -path '*/target/*.jar' -not -name 'original-*' -not -name '*-sources.jar' \
        -not -name '*-javadoc.jar' -not -name '*-tests.jar' -printf '%s %p\n' 2>/dev/null | sort -rn | cut -d' ' -f2-)
    rm -rf "$scratch"
    return 1
}

# drops and creates the database $1 (empty); prints its JDBC address
fresh_database() {
    psql -q -d postgres -c "DROP DATABASE IF EXISTS \"$1\" WITH (FORCE)" -c "CREATE DATABASE \"$1\"" >/dev/null \
        && echo "jdbc:postgresql://$PGHOST:$PGPORT/$1"
}

# starts the jar $1 against the JDBC address $2 on port $3, logging to $4; the rest of the arguments are extra
# variables (NAME=value). Waits until it answers any HTTP request; sets SERVICE_PID. Returns 1 if it does not start.
start_service() {
    local jar="$1" url="$2" port="$3" log="$4"
    shift 4
    env DB_URL="$url" DB_USER="$PGUSER" DB_PASSWORD="$PGPASSWORD" PORT="$port" "$@" java -jar "$jar" > "$log" 2>&1 &
    SERVICE_PID=$!
    local second
    for second in $(seq 1 120); do
        if ! kill -0 "$SERVICE_PID" 2>/dev/null; then
            return 1
        fi
        # curl without -f answers 0 for any HTTP status: the service is up
        if curl -s -o /dev/null --max-time 2 "http://localhost:$port/rooms"; then
            return 0
        fi
        sleep 1
    done
    stop_service
    return 1
}

stop_service() {
    if [ -n "${SERVICE_PID:-}" ]; then
        kill "$SERVICE_PID" 2>/dev/null
        wait "$SERVICE_PID" 2>/dev/null
        SERVICE_PID=""
    fi
}
