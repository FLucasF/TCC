#!/usr/bin/env bash
# Roda uma rodada inteira: os tres modelos, nas duas condicoes, TODOS ao mesmo
# tempo. Seis execucoes em paralelo, seis containers.
#
# Uso:  infra/scripts/rodada.sh <prefixo> [replicate]
#       infra/scripts/rodada.sh SMOKE-01
#       infra/scripts/rodada.sh BATCH-01 1
#
# SIMULTANEO DE PROPOSITO, e este e o ponto mais importante do desenho.
#
# A analise compara PARES: cada execucao CONTROL contra a HARNESS que rodou no
# mesmo instante, no mesmo modelo. Isso cancela horario, fila e carga de
# servidor — que sao a maior fonte de variacao nao controlada. Rodar um agora e
# outro daqui a meia hora joga fora exatamente o que este script compra.
#
# Efeito colateral aceito: a duracao de RELOGIO fica contaminada pela disputa de
# CPU entre os seis containers. Por isso a medida limpa e `duration_api_ms`, e
# nao `duration_s`.
#
# O lote sao TRES rodadas:
#   rodada.sh BATCH-01 1
#   rodada.sh BATCH-02 2
#   rodada.sh BATCH-03 3

set -uo pipefail

case $# in 1|2) ;; *) { echo "uso: $0 <prefixo> [replicate]" >&2; exit 2; } ;; esac
PREFIXO="$1"; REPLICATE="${2:-}"
RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd -P)"
EXEC="$RAIZ/infra/scripts/executar.sh"
EFFORT="${EFFORT:-medium}"

# Os tres modelos do bloco, sempre por ID COMPLETO. O apelido a esquerda e o que
# entra no run_id.
#
# ATENCAO: pede-se `claude-haiku-4-5` e as mensagens voltam com
# `claude-haiku-4-5-20251001`. Alias e snapshot datado sao o MESMO modelo — ver
# a normalizacao em extrair-meta.mjs.
MODELOS="OPUS:claude-opus-5 SONNET:claude-sonnet-5 HAIKU:claude-haiku-4-5"

mkdir -p "$RAIZ/runs/logs"

# Confere TODOS antes de lancar QUALQUER um: melhor falhar sem gastar cota do
# que descobrir na quarta execucao que a quinta ja existia.
for m in $MODELOS; do
    for c in CONTROL HARNESS; do
        id="$PREFIXO-${m%%:*}-$c"
        [ -e "$RAIZ/runs/$id" ] && { echo "runs/$id ja existe" >&2; exit 2; }
    done
done

printf '\033[36m=== rodada %s  |  effort=%s  |  6 execucoes em paralelo ===\033[0m\n' "$PREFIXO" "$EFFORT"
T0="$(date +%s)"
PIDS=""; IDS=""

for m in $MODELOS; do
    apelido="${m%%:*}"; modelo="${m#*:}"
    for c in CONTROL HARNESS; do
        id="$PREFIXO-$apelido-$c"
        EFFORT="$EFFORT" NETWORK="${NETWORK:-}" "$EXEC" "$id" "$modelo" "$c" ${REPLICATE:+"$REPLICATE"} \
            > "$RAIZ/runs/logs/$id.log" 2>&1 &
        PIDS="$PIDS $!"; IDS="$IDS $id"
        echo "  lancada: $id"
    done
done

echo "aguardando as seis..."
FALHAS=0
set -- $IDS
for p in $PIDS; do
    wait "$p"; rc=$?
    printf '  %-32s codigo=%s\n' "$1" "$rc"
    [ "$rc" -eq 0 ] || FALHAS=$((FALHAS + 1))
    shift
done

printf '\nrodada terminou em %ss  |  falhas: %s\n' "$(( $(date +%s) - T0 ))" "$FALHAS"
for id in $IDS; do
    [ -f "$RAIZ/runs/$id/meta.json" ] && printf '  %-32s %s\n' "$id" "$(tail -n 1 "$RAIZ/runs/logs/$id.log")"
done
exit "$FALHAS"
