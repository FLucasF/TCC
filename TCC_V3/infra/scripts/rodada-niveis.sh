#!/usr/bin/env bash
# Roda os quatro niveis da escada (N0 a N3) do mesmo modelo, TODOS ao mesmo
# tempo: um quarteto simultaneo, no lugar do par simultaneo do rodada.sh.
#
# Uso:  infra/scripts/rodada-niveis.sh <prefixo> <replicate> <OPUS|SONNET|HAIKU|TODOS>
#       infra/scripts/rodada-niveis.sh V4-STRATEGY-01 1 HAIKU     (4 execucoes)
#       infra/scripts/rodada-niveis.sh V4-STRATEGY-01 1 TODOS     (12 execucoes)
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
# Nao muda o executar.sh: chama ele quatro vezes, como o rodada.sh chama duas.
# Confere TUDO antes de lancar QUALQUER execucao: um quarteto com um nivel a
# menos nao serve para a comparacao, e e melhor falhar sem gastar cota.

set -uo pipefail

case $# in 3) ;; *) { echo "uso: $0 <prefixo> <replicate> <OPUS|SONNET|HAIKU|TODOS>" >&2; exit 2; } ;; esac
PREFIXO="$1"; REPLICATE="$2"; QUAIS="$3"
case "$PREFIXO" in ""|*[!A-Za-z0-9-]*) { echo "prefixo invalido: '$PREFIXO'" >&2; exit 2; } ;; esac
case "$REPLICATE" in [1-9]|[1-9][0-9]) ;; *) { echo "replicate deve ser inteiro positivo: '$REPLICATE'" >&2; exit 2; } ;; esac

RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd -P)"
EXEC="$RAIZ/infra/scripts/executar.sh"
EFFORT="${EFFORT:-medium}"

# Os mesmos IDs completos do rodada.sh. O apelido a esquerda entra no run_id.
case "$QUAIS" in
    OPUS)   MODELOS="OPUS:claude-opus-5" ;;
    SONNET) MODELOS="SONNET:claude-sonnet-5" ;;
    HAIKU)  MODELOS="HAIKU:claude-haiku-4-5" ;;
    TODOS)  MODELOS="OPUS:claude-opus-5 SONNET:claude-sonnet-5 HAIKU:claude-haiku-4-5" ;;
    *) { echo "modelo deve ser OPUS, SONNET, HAIKU ou TODOS: '$QUAIS'" >&2; exit 2; } ;;
esac
NIVEIS="N0 N1 N2 N3"

# ------------------------------------------------------------------ preflight
for n in N1 N2 N3; do
    d="$RAIZ/experiment/harnesses/$n"
    [ -d "$d" ] || { echo "nivel $n nao encontrado: $d" >&2; exit 2; }
    [ -f "$d/CLAUDE.md" ] || compgen -G "$d/.claude/skills/*/SKILL.md" >/dev/null \
        || { echo "nivel $n vazio: o executar.sh o recusaria" >&2; exit 2; }
done
for m in $MODELOS; do
    for n in $NIVEIS; do
        id="$PREFIXO-${m%%:*}-$n"
        [ -e "$RAIZ/runs/$id" ] && { echo "runs/$id ja existe" >&2; exit 2; }
    done
done
mkdir -p "$RAIZ/runs/logs"

N_EXEC=$(( $(echo $MODELOS | wc -w) * 4 ))
printf '\033[36m=== quarteto %s  |  replica %s  |  effort=%s  |  %s execucoes em paralelo ===\033[0m\n' \
    "$PREFIXO" "$REPLICATE" "$EFFORT" "$N_EXEC"
T0="$(date +%s)"
PIDS=""; IDS=""

for m in $MODELOS; do
    apelido="${m%%:*}"; modelo="${m#*:}"
    for n in $NIVEIS; do
        id="$PREFIXO-$apelido-$n"
        if [ "$n" = "N0" ]; then
            EFFORT="$EFFORT" NETWORK="${NETWORK:-}" "$EXEC" "$id" "$modelo" CONTROL "$REPLICATE" \
                > "$RAIZ/runs/logs/$id.log" 2>&1 &
        else
            HARNESS="$n" EFFORT="$EFFORT" NETWORK="${NETWORK:-}" "$EXEC" "$id" "$modelo" HARNESS "$REPLICATE" \
                > "$RAIZ/runs/logs/$id.log" 2>&1 &
        fi
        PIDS="$PIDS $!"; IDS="$IDS $id"
        echo "  lancada: $id"
    done
done

echo "aguardando..."
FALHAS=0
set -- $IDS
for p in $PIDS; do
    wait "$p"; rc=$?
    printf '  %-34s codigo=%s\n' "$1" "$rc"
    [ "$rc" -eq 0 ] || FALHAS=$((FALHAS + 1))
    shift
done

printf '\nquarteto terminou em %ss  |  falhas: %s\n' "$(( $(date +%s) - T0 ))" "$FALHAS"
for id in $IDS; do
    [ -f "$RAIZ/runs/$id/meta.json" ] && printf '  %-34s %s\n' "$id" "$(tail -n 1 "$RAIZ/runs/logs/$id.log")"
done
