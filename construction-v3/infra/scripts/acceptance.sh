#!/usr/bin/env bash
# Roda a suite de aceitacao do Strategy em todas as execucoes de um lote, uma por
# vez, e grava o resultado de cada uma ao lado do build.txt dela.
#
# Uso:  infra/scripts/acceptance.sh <prefixo>
#       infra/scripts/acceptance.sh V4-STRATEGY-01
#
# Para cada runs/<prefixo>-*/: sobe o servico que o agente escreveu num container
# da imagem da bancada, SEM token e sem variavel de ambiente nenhuma (o
# evaluation/acceptance-prototype/executor.sh), roda os casos do strategy.mjs e
# grava runs/<id>/acceptance.txt. No fim, refaz analysis/acceptance-<prefixo>.csv
# a partir dos acceptance.txt do lote: uma linha por execucao.
#
# Tres garantias:
# - NUNCA mede duas vezes: execucao que ja tem acceptance.txt e pulada. Medir de
#   novo ate dar o resultado esperado e o que isso impede.
# - Cada resultado grava o hash da suite e o do enunciado da execucao, e a
#   execucao so e medida se o enunciado dela for o atual (o da suite). Uma
#   execucao de outro enunciado (a bancada, com imposto) e recusada; para ensaio,
#   ALLOW_PROMPT_MISMATCH=1 mede assim mesmo e registra a diferenca.
# - "Nao compilou", "nao subiu" e "nao seguiu o contrato" sao situacoes proprias,
#   separadas de "errou casos".
#
# COMO LER. Tres partes:
#   1. preflight: confere o Docker, que a imagem e a da bancada (pelo ID), e calcula os
#      hashes da suite, da calculadora de referencia e do enunciado;
#   2. "uma por vez": para cada execucao ainda nao medida, sobe um container que
#      compila o projeto do agente, liga o servico e manda os 21 pedidos da suite
#      (tudo isso e o executor.sh); le a saida e decide o status pelo codigo de saida
#      (0 passou tudo, 1 errou casos, 3 sem pom, 4 nao compilou, 5 nao subiu);
#      grava runs/<id>/acceptance.txt com o cabecalho (hashes, status, pontos) e a
#      saida completa da suite embaixo;
#   3. "o CSV do lote": refaz o analysis/acceptance-<prefixo>.csv lendo os
#      acceptance.txt (os pontos por tipo vem daqui para a nota).
#
# Variaveis (para teste da propria ferramenta; o padrao e o do experimento):
#   RUNS_DIR   onde estao as execucoes      (padrao: runs/)
#   CSV_FILE   o CSV do lote                (padrao: analysis/acceptance-<prefixo>.csv)
#   ALLOW_PROMPT_MISMATCH=1   mede execucao de outro enunciado, marcando no resultado

set -uo pipefail

case $# in 1) ;; *) { echo "uso: $0 <prefixo>" >&2; exit 2; } ;; esac
PREFIX="$1"
case "$PREFIX" in ""|*[!A-Za-z0-9-]*) { echo "prefixo invalido: '$PREFIX'" >&2; exit 2; } ;; esac

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd -P)"
RUNS_DIR="${RUNS_DIR:-$ROOT/runs}"
CSV_FILE="${CSV_FILE:-$ROOT/analysis/acceptance-$PREFIX.csv}"
SUITE_DIR="$ROOT/evaluation/acceptance-prototype"
SUITE_FILE="strategy.mjs"
PROMPT_FILE="$ROOT/experiment/prompt/prompt.md"
IMAGE="experimento-harness:v5"
EXPECTED_IMAGE_ID="sha256:8e96815c94d1662d82fdfc0683a16ea3f5f9c322888268a11609c7bf77b6921b"

fail() { echo "ERRO: $*" >&2; exit 2; }
# caminho para o Docker: no Git Bash do Windows precisa virar C:\..., no Linux nao
host_path() { if command -v cygpath >/dev/null 2>&1; then cygpath -w "$1"; else printf '%s' "$1"; fi; }

# ------------------------------------------------------------------ preflight
command -v node >/dev/null 2>&1 || fail "node nao encontrado"
docker info >/dev/null 2>&1 || fail "Docker fechado ou inacessivel"
IMAGE_ID="$(docker image inspect --format '{{.Id}}' "$IMAGE" 2>/dev/null)" || fail "imagem $IMAGE nao existe"
[ "$IMAGE_ID" = "$EXPECTED_IMAGE_ID" ] || fail "imagem $IMAGE e $IMAGE_ID, esperado $EXPECTED_IMAGE_ID"
[ -f "$SUITE_DIR/$SUITE_FILE" ] && [ -f "$SUITE_DIR/executor.sh" ] || fail "suite ou executor ausente em $SUITE_DIR"
SUITE_HASH="$(sha256sum "$SUITE_DIR/$SUITE_FILE" | cut -c1-16)"
REF_HASH="$(sha256sum "$SUITE_DIR/ref-strategy.mjs" | cut -c1-16)"
PROMPT_HASH="$(sha256sum "$PROMPT_FILE" | cut -d' ' -f1)"

shopt -s nullglob
RUN_DIRS=("$RUNS_DIR/$PREFIX"-*/)
[ ${#RUN_DIRS[@]} -gt 0 ] || fail "nenhuma execucao em $RUNS_DIR/$PREFIX-*"
mkdir -p "$(dirname "$CSV_FILE")"

printf 'suite %s (%s), calculadora %s, enunciado %s\n' "$SUITE_FILE" "$SUITE_HASH" "$REF_HASH" "${PROMPT_HASH:0:16}"
printf '%s execucoes em %s\n\n' "${#RUN_DIRS[@]}" "$RUNS_DIR/$PREFIX-*"

# ------------------------------------------------------------------ uma por vez
for run_dir in "${RUN_DIRS[@]}"; do
    run_dir="${run_dir%/}"; run_id="$(basename "$run_dir")"
    out="$run_dir/acceptance.txt"

    if [ -f "$out" ]; then printf '  %-36s ja medido, pulado\n' "$run_id"; continue; fi
    if [ ! -d "$run_dir/workspace" ] || [ ! -f "$run_dir/meta.json" ]; then
        printf '  %-36s sem workspace ou meta.json, pulado\n' "$run_id"; continue
    fi

    run_prompt_hash="$(node -e 'try { console.log(require(process.argv[1]).environment.prompt_hash || "") } catch { console.log("") }' "$(host_path "$run_dir/meta.json")")"
    if [ "$run_prompt_hash" = "$PROMPT_HASH" ]; then prompt_check="confere"
    elif [ "${ALLOW_PROMPT_MISMATCH:-0}" = "1" ]; then prompt_check="DIFERE (medido com ALLOW_PROMPT_MISMATCH=1)"
    else printf '  %-36s enunciado %s nao e o da suite, pulado\n' "$run_id" "${run_prompt_hash:0:16}"; continue
    fi

    raw="$(MSYS_NO_PATHCONV=1 docker run --rm \
        --mount "type=bind,source=$(host_path "$run_dir/workspace"),target=/ws,readonly" \
        --mount "type=bind,source=$(host_path "$SUITE_DIR"),target=/aceitacao,readonly" \
        "$IMAGE" bash /aceitacao/executor.sh "$SUITE_FILE" 2>&1)"
    rc=$?

    passed=""; total=""
    result_line="$(printf '%s\n' "$raw" | grep -m1 '^RESULTADO:')"
    if [ -n "$result_line" ]; then
        passed="$(printf '%s' "$result_line" | sed -n 's/^RESULTADO: \([0-9]*\) de \([0-9]*\) casos.*/\1/p')"
        total="$(printf '%s' "$result_line" | sed -n 's/^RESULTADO: \([0-9]*\) de \([0-9]*\) casos.*/\2/p')"
    fi
    # Os pontos por tipo de caso (09/10): CONTAS, RECUSAS (com o 0,5) e PONTOS, "x de n".
    # Ficam vazios quando o servico nem chegou a responder (sem pom, build, nao subiu).
    points() { printf '%s\n' "$raw" | sed -n "s/^$1: \([0-9.]*\) de \([0-9]*\)\$/\1 de \2/p" | head -1; }
    contas="$(points CONTAS)"; recusas="$(points RECUSAS)"; pontos="$(points PONTOS)"
    case $rc in
        0) status="all_passed" ;;
        1) status="some_failed"
           # todos os casos devolveram 404 ou 405: o servico subiu, mas nao no caminho do
           # contrato (404) ou com outro metodo (405; o enunciado nao diz o verbo)
           not_found="$(printf '%s\n' "$raw" | grep -cE 'veio 40[45]')"
           [ "$passed" = "0" ] && [ -n "$total" ] && [ "$not_found" -ge "$total" ] && status="contract_not_followed" ;;
        3) status="no_pom" ;;
        4) status="build_failed" ;;
        5) status="app_did_not_start" ;;
        *) status="error_$rc" ;;
    esac

    {
        echo "run_id: $run_id"
        echo "measured_at: $(date -u +%Y-%m-%dT%H:%M:%SZ)"
        echo "suite: $SUITE_FILE $SUITE_HASH"
        echo "reference: ref-strategy.mjs $REF_HASH"
        echo "prompt: ${run_prompt_hash:0:16} $prompt_check"
        echo "image: $IMAGE_ID"
        echo "status: $status"
        echo "passed: $passed"
        echo "total: $total"
        echo "contas: $contas"
        echo "recusas: $recusas"
        echo "pontos: $pontos"
        echo "---"
        printf '%s\n' "$raw"
    } > "$out"
    printf '  %-36s %s%s\n' "$run_id" "$status" "${passed:+ ($passed de $total)}"
done

# ------------------------------------------------------------------ o CSV do lote
# Refeito a partir dos acceptance.txt, que sao a fonte: o CSV nunca diverge deles.
{
    echo "run_id,status,passed,total,suite_hash,reference_hash,prompt_hash,prompt_check,contas,recusas,pontos"
    for run_dir in "${RUN_DIRS[@]}"; do
        f="${run_dir%/}/acceptance.txt"; [ -f "$f" ] || continue
        field() { sed -n "s/^$1: //p" "$f" | head -1; }
        prompt_line="$(field prompt)"
        # contas/recusas/pontos so o valor (o total e fixo: 12, 9 e 21); vazio nas
        # medicoes anteriores a 09/10, que nao tinham essas linhas.
        printf '%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s\n' "$(field run_id)" "$(field status)" "$(field passed)" "$(field total)" \
            "$(field suite | cut -d' ' -f2)" "$(field reference | cut -d' ' -f2)" \
            "${prompt_line%% *}" "$(printf '%s' "${prompt_line#* }" | cut -d' ' -f1)" \
            "$(field contas | cut -d' ' -f1)" "$(field recusas | cut -d' ' -f1)" "$(field pontos | cut -d' ' -f1)"
    done
} > "$CSV_FILE"
printf '\nCSV: %s\n' "$CSV_FILE"
