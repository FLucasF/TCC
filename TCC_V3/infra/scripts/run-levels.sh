#!/usr/bin/env bash
# Roda os quatro niveis da escada (N0 a N3) do mesmo modelo, TODOS ao mesmo
# tempo: um quarteto simultaneo, no lugar do par simultaneo do rodada.sh.
#
# Uso:  infra/scripts/run-levels.sh <prefixo> <replicate> <OPUS|SONNET|HAIKU|TODOS>
#       infra/scripts/run-levels.sh V4-STRATEGY-01 1 HAIKU     (4 execucoes)
#       infra/scripts/run-levels.sh V4-STRATEGY-01 1 TODOS     (12 execucoes)
#
# Os niveis (ver experiment/harnesses/README.md):
#   N0 = braco CONTROL, workspace vazio
#   N1, N2, N3 = braco HARNESS, com HARNESS=N1, N2 ou N3
# O run_id termina no nivel: <prefixo>-<MODELO>-N0 ... N3. No meta.json, o nivel
# tambem se le pelo harness_hash.
#
# SIMULTANEO DE PROPOSITO, pelo mesmo motivo do rodada.sh: os quatro niveis de um
# modelo rodam no mesmo instante, para horario, fila e carga de servidor serem os
# mesmos, e a analise compara os niveis em pares (N1 x N0, N2 x N1, N3 x N2).
# Com TODOS, os tres modelos tambem rodam juntos (12 containers); um modelo por
# vez e o mais seguro para a cota da assinatura, e os quartetos de modelos
# diferentes nao precisam ser simultaneos entre si.
#
# Nao muda o run-one.sh: chama ele quatro vezes, como o rodada.sh chama duas.
# Confere TUDO antes de lancar QUALQUER execucao: um quarteto com um nivel a
# menos nao serve para a comparacao, e e melhor falhar sem gastar cota.
#
# Chamava-se rodada-niveis.sh ate 07/10; o nome e as variaveis foram para o
# ingles, sem mudar a logica.

set -uo pipefail

case $# in 3) ;; *) { echo "uso: $0 <prefixo> <replicate> <OPUS|SONNET|HAIKU|TODOS>" >&2; exit 2; } ;; esac
PREFIX="$1"; REPLICATE="$2"; WHICH="$3"
case "$PREFIX" in ""|*[!A-Za-z0-9-]*) { echo "prefixo invalido: '$PREFIX'" >&2; exit 2; } ;; esac
case "$REPLICATE" in [1-9]|[1-9][0-9]) ;; *) { echo "replicate deve ser inteiro positivo: '$REPLICATE'" >&2; exit 2; } ;; esac

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd -P)"
RUN_ONE="$ROOT/infra/scripts/run-one.sh"
EFFORT="${EFFORT:-medium}"

# Os mesmos IDs completos do rodada.sh. O apelido a esquerda entra no run_id.
case "$WHICH" in
    OPUS)   MODELS="OPUS:claude-opus-5" ;;
    SONNET) MODELS="SONNET:claude-sonnet-5" ;;
    HAIKU)  MODELS="HAIKU:claude-haiku-4-5" ;;
    TODOS)  MODELS="OPUS:claude-opus-5 SONNET:claude-sonnet-5 HAIKU:claude-haiku-4-5" ;;
    *) { echo "modelo deve ser OPUS, SONNET, HAIKU ou TODOS: '$WHICH'" >&2; exit 2; } ;;
esac
LEVELS="N0 N1 N2 N3"

# ------------------------------------------------------------------ preflight
for level in N1 N2 N3; do
    dir="$ROOT/experiment/harnesses/$level"
    [ -d "$dir" ] || { echo "nivel $level nao encontrado: $dir" >&2; exit 2; }
    [ -f "$dir/CLAUDE.md" ] || compgen -G "$dir/.claude/skills/*/SKILL.md" >/dev/null \
        || { echo "nivel $level vazio: o run-one.sh o recusaria" >&2; exit 2; }
done
for m in $MODELS; do
    for level in $LEVELS; do
        id="$PREFIX-${m%%:*}-$level"
        [ -e "$ROOT/runs/$id" ] && { echo "runs/$id ja existe" >&2; exit 2; }
    done
done
mkdir -p "$ROOT/runs/logs"

RUN_COUNT=$(( $(echo $MODELS | wc -w) * 4 ))
printf '\033[36m=== quarteto %s  |  replica %s  |  effort=%s  |  %s execucoes em paralelo ===\033[0m\n' \
    "$PREFIX" "$REPLICATE" "$EFFORT" "$RUN_COUNT"
START="$(date +%s)"
PIDS=""; IDS=""

for m in $MODELS; do
    alias="${m%%:*}"; model="${m#*:}"
    for level in $LEVELS; do
        id="$PREFIX-$alias-$level"
        if [ "$level" = "N0" ]; then
            EFFORT="$EFFORT" NETWORK="${NETWORK:-}" "$RUN_ONE" "$id" "$model" CONTROL "$REPLICATE" \
                > "$ROOT/runs/logs/$id.log" 2>&1 &
        else
            HARNESS="$level" EFFORT="$EFFORT" NETWORK="${NETWORK:-}" "$RUN_ONE" "$id" "$model" HARNESS "$REPLICATE" \
                > "$ROOT/runs/logs/$id.log" 2>&1 &
        fi
        PIDS="$PIDS $!"; IDS="$IDS $id"
        echo "  lancada: $id"
    done
done

echo "aguardando..."
FAILURES=0
set -- $IDS
for p in $PIDS; do
    wait "$p"; rc=$?
    printf '  %-34s codigo=%s\n' "$1" "$rc"
    [ "$rc" -eq 0 ] || FAILURES=$((FAILURES + 1))
    shift
done

printf '\nquarteto terminou em %ss  |  falhas: %s\n' "$(( $(date +%s) - START ))" "$FAILURES"
for id in $IDS; do
    [ -f "$ROOT/runs/$id/meta.json" ] && printf '  %-34s %s\n' "$id" "$(tail -n 1 "$ROOT/runs/logs/$id.log")"
done
