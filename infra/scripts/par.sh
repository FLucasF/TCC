#!/usr/bin/env bash
# Roda o par SEM || COM do mesmo modelo AO MESMO TEMPO.
#
# Simultâneo de propósito: rodar um agora e outro daqui a meia hora mistura
# carga de servidor e fila com o efeito do harness.
#
# Uso:  infra/scripts/par.sh <prefixo> <modelo>
#       infra/scripts/par.sh MED-01-HAIKU claude-haiku-4-5
#
# Gera runs/<prefixo>-SEM e runs/<prefixo>-COM, com o log de cada uma ao lado.

set -uo pipefail

[ $# -eq 2 ] || { echo "uso: $0 <prefixo> <modelo>" >&2; exit 2; }
PREFIXO="$1"; MODELO="$2"
RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd -P)"
EXEC="$RAIZ/infra/scripts/executar.sh"

for c in SEM COM; do
    [ -e "$RAIZ/runs/$PREFIXO-$c" ] && { echo "runs/$PREFIXO-$c já existe" >&2; exit 2; }
done

printf '\033[36m=== par %s  %s  (SEM || COM) ===\033[0m\n' "$PREFIXO" "$MODELO"
T0="$(date +%s)"

"$EXEC" "$PREFIXO-SEM" "$MODELO" SEM > "$RAIZ/runs/logs/$PREFIXO-SEM.log" 2>&1 &
PID_SEM=$!
"$EXEC" "$PREFIXO-COM" "$MODELO" COM > "$RAIZ/runs/logs/$PREFIXO-COM.log" 2>&1 &
PID_COM=$!

wait "$PID_SEM"; RC_SEM=$?
wait "$PID_COM"; RC_COM=$?

printf 'par terminou em %ss  |  SEM=%s  COM=%s\n' "$(( $(date +%s) - T0 ))" "$RC_SEM" "$RC_COM"
for c in SEM COM; do
    tail -n 3 "$RAIZ/runs/logs/$PREFIXO-$c.log" | sed "s/^/  [$c] /"
done
[ "$RC_SEM" -eq 0 ] && [ "$RC_COM" -eq 0 ]
