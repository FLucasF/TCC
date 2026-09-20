#!/usr/bin/env bash
# Roda uma rodada inteira: os três modelos, nas duas condições, TODOS ao mesmo
# tempo. Seis execuções em paralelo, seis containers.
#
# Uso:  infra/scripts/rodada.sh <prefixo> [repeticao]
#       EFFORT=high infra/scripts/rodada.sh MED-01
#
# Simultâneo de propósito: horário, carga de servidor e fila ficam iguais para
# todas as seis. Em compensação, a duração de relógio fica contaminada pela
# disputa de CPU da máquina — `duracao_api_ms` é a medida limpa.

set -uo pipefail

case $# in 1|2) ;; *) { echo "uso: $0 <prefixo> [repeticao]" >&2; exit 2; } ;; esac
PREFIXO="$1"; REPETICAO="${2:-}"
RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd -P)"
EXEC="$RAIZ/infra/scripts/executar.sh"
EFFORT="${EFFORT:-medium}"

MODELOS="OPUS:claude-opus-5 SONNET:claude-sonnet-5 HAIKU:claude-haiku-4-5"

for m in $MODELOS; do
    for c in SEM COM; do
        id="$PREFIXO-${m%%:*}-$c"
        [ -e "$RAIZ/runs/$id" ] && { echo "runs/$id já existe" >&2; exit 2; }
    done
done

printf '\033[36m=== rodada %s  |  effort=%s  |  6 execuções em paralelo ===\033[0m\n' "$PREFIXO" "$EFFORT"
T0="$(date +%s)"
PIDS=""; IDS=""

for m in $MODELOS; do
    apelido="${m%%:*}"; modelo="${m#*:}"
    for c in SEM COM; do
        id="$PREFIXO-$apelido-$c"
        EFFORT="$EFFORT" "$EXEC" "$id" "$modelo" "$c" ${REPETICAO:+"$REPETICAO"} > "$RAIZ/runs/logs/$id.log" 2>&1 &
        PIDS="$PIDS $!"; IDS="$IDS $id"
        echo "  lançada: $id"
    done
done

echo "aguardando as seis..."
FALHAS=0
set -- $IDS
for p in $PIDS; do
    wait "$p"; rc=$?
    printf '  %-24s codigo=%s\n' "$1" "$rc"
    [ "$rc" -eq 0 ] || FALHAS=$((FALHAS + 1))
    shift
done

printf '\nrodada terminou em %ss  |  falhas: %s\n' "$(( $(date +%s) - T0 ))" "$FALHAS"
for id in $IDS; do
    [ -f "$RAIZ/runs/$id/meta.json" ] && printf '  %-24s %s\n' "$id" "$(tail -n 1 "$RAIZ/runs/logs/$id.log")"
done
exit "$FALHAS"
