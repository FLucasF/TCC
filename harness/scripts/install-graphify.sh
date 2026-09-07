#!/bin/sh
# Installs the code graph tool the impact-analysis skill uses.
#
# Installs only. It does not generate a graph, does not write to the project and
# does not configure Claude Code. Generating the graph is `graphify update <path>
# --no-cluster`, and the skill says when that is needed.
#
# POSIX sh: Linux, macOS, and Windows under Git Bash.

set -eu

VERSION="0.9.56"

# Probe each candidate by running it, not by finding it. On Windows, PATH carries an
# App Execution Alias named python3 that `command -v` resolves happily: it prints a
# Microsoft Store advert and exits 49, so name resolution alone picks an interpreter
# that cannot run anything.
PY=""
for candidate in python3 python py; do
    if command -v "$candidate" >/dev/null 2>&1 &&
       [ "$("$candidate" -c 'print(1)' 2>/dev/null)" = "1" ]; then
        PY="$candidate"
        break
    fi
done

if [ -z "$PY" ]; then
    echo "error: no working python3, python or py on PATH. graphify is a Python package." >&2
    exit 1
fi

# Do not add the [sql] extra. Measured on this project: it parses CREATE TABLE and
# not ALTER TABLE ... RENAME TO, so a schema with a rename migration yields table
# nodes under names that no longer exist — 29 of 29 obsolete here — and it doubles
# rebuild time. Nothing links those nodes to the entities anyway: the mapping lives
# in @Table(name = "..."), a string in an annotation, which the extractor ignores.
if ! "$PY" -m pip install --quiet "graphifyy==$VERSION"; then
    echo "" >&2
    echo "error: pip could not install graphifyy==$VERSION." >&2
    echo "If it reported an externally-managed environment (PEP 668, common on" >&2
    echo "Debian and Ubuntu), install into a virtualenv or use pipx:" >&2
    echo "    pipx install 'graphifyy==$VERSION'" >&2
    exit 1
fi

if ! command -v graphify >/dev/null 2>&1; then
    echo "error: graphifyy $VERSION installed but 'graphify' is not on PATH." >&2
    echo "Add the Python scripts directory to PATH:" >&2
    echo "    $("$PY" -c 'import sysconfig; print(sysconfig.get_path("scripts"))')" >&2
    exit 1
fi

echo "graphify $VERSION ready at $(command -v graphify)"
