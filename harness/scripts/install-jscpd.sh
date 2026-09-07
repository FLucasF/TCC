#!/bin/sh
# Installs the copy-paste detector the duplication-check skill uses.
#
# Installs only. It runs no check and writes nothing to the project. The skill
# gives the command and the calibrated flags.
#
# POSIX sh: Linux, macOS, and Windows under Git Bash.

set -eu

VERSION="5.1.2"

if ! command -v npm >/dev/null 2>&1; then
    echo "error: no npm on PATH. jscpd is an npm package." >&2
    exit 1
fi

if ! npm install -g "jscpd@$VERSION"; then
    echo "" >&2
    echo "error: npm could not install jscpd@$VERSION globally." >&2
    echo "If it reported EACCES, do not rerun with sudo — that leaves root-owned" >&2
    echo "files in your npm tree. Point the global prefix at your home directory:" >&2
    echo "    npm config set prefix ~/.npm-global" >&2
    echo "    export PATH=\"\$HOME/.npm-global/bin:\$PATH\"" >&2
    exit 1
fi

if ! command -v jscpd >/dev/null 2>&1; then
    echo "error: jscpd $VERSION installed but 'jscpd' is not on PATH." >&2
    echo "Check the npm global prefix and that its bin directory is on PATH:" >&2
    echo "    npm config get prefix" >&2
    exit 1
fi

echo "jscpd $VERSION ready at $(command -v jscpd)"
