#!/usr/bin/env bash
# metricas.sh
# Chama o metricas.py com um interpretador que de fato execute.
#
# Uso:  ./metricas.sh --base ~/eval [--build]

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"

# python3 no Windows costuma ser o stub da Microsoft Store: resolve no PATH
# e nao executa. Detecta rodando, nao localizando.
PY=""
for cand in python python3 py; do
    if command -v "$cand" >/dev/null 2>&1 && "$cand" -c 'import json, csv' >/dev/null 2>&1; then
        PY="$cand"; break
    fi
done

if [ -z "$PY" ]; then
    printf '\033[31m%s\033[0m\n' "ERRO: nenhum Python funcional no PATH." >&2
    exit 1
fi

exec "$PY" "$SCRIPT_DIR/metricas.py" "$@"
