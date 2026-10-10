#!/usr/bin/env bash
# Roda os quatro niveis da escada (N0 a N3) do mesmo modelo, TODOS ao mesmo
# tempo: um quarteto simultaneo, no lugar do par simultaneo do rodada.sh.
#
# Uso:  infra/scripts/run-levels.sh <prefixo> <replicate> <APELIDO>
#       infra/scripts/run-levels.sh V4-STRATEGY-01 1 HAIKU45     (4 execucoes)
#
# O APELIDO e uma chave de "modelos" do desenho (experiment/desenho-v4.json, ou o
# arquivo em DESENHO=...), e o ID completo do modelo e o effort saem de la: uma lista
# so, a mesma que o verify.mjs confere. Desde 09/10 (o mapa): HAIKU45, SONNET45,
# OPUS46, SONNET5 e OPUS5. Um quarteto por vez, na ordem de experiment/ordem-v4.csv.
# Se o prefixo for do lote do desenho, ele tem de ser <prefixo do lote>-<replica com
# 2 digitos> (V4-STRATEGY-03 com a replica 3): o verify.mjs so reconhece esse nome.
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
# Os quartetos de modelos diferentes nao precisam ser simultaneos entre si, e um
# modelo por vez e o mais seguro para a cota da assinatura. (Ate 09/10 havia a opcao
# TODOS, com tres modelos juntos; saiu com o desenho de 5 modelos.)
#
# Nao muda o run-one.sh: chama ele quatro vezes, como o rodada.sh chama duas.
# Confere TUDO antes de lancar QUALQUER execucao: um quarteto com um nivel a
# menos nao serve para a comparacao, e e melhor falhar sem gastar cota.
#
# Chamava-se rodada-niveis.sh ate 07/10; o nome e as variaveis foram para o
# ingles, sem mudar a logica.
#
# COMO LER (para quem le o V4 pela primeira vez). O script tem 4 partes:
#   1. confere os 3 argumentos (formato do prefixo e da replica);
#   2. le o desenho: troca o apelido (HAIKU45) pelo ID do modelo (claude-haiku-4-5)
#      e pega o effort; recusa o que nao bate com o desenho;
#   3. "preflight": confere que as pastas dos niveis existem e que nenhuma das 4
#      execucoes ja existe em runs/ (uma execucao nunca e refeita por cima);
#   4. lanca as 4 execucoes em paralelo (o "&" no fim da linha) e espera todas.
# Cada execucao e o run-one.sh, que sobe um container, roda o Claude Code com o
# enunciado e grava runs/<id>/ (meta.json, transcricao, workspace, build.txt).

set -uo pipefail

# ------------------------------------------------------------------ 1. argumentos
case $# in 3) ;; *) { echo "uso: $0 <prefixo> <replicate> <APELIDO do desenho>" >&2; exit 2; } ;; esac
PREFIX="$1"; REPLICATE="$2"; WHICH="$3"
# So letras, numeros e hifen: o prefixo vira nome de pasta e de container.
case "$PREFIX" in ""|*[!A-Za-z0-9-]*) { echo "prefixo invalido: '$PREFIX'" >&2; exit 2; } ;; esac
case "$REPLICATE" in [1-9]|[1-9][0-9]) ;; *) { echo "replicate deve ser inteiro positivo: '$REPLICATE'" >&2; exit 2; } ;; esac

# ------------------------------------------------------------------ 2. o desenho
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd -P)"
RUN_ONE="$ROOT/infra/scripts/run-one.sh"
DESENHO="${DESENHO:-$ROOT/experiment/desenho-v4.json}"
[ -f "$DESENHO" ] || { echo "desenho nao encontrado: $DESENHO" >&2; exit 2; }
# Absoluto: o arquivo de ordem traz DESENHO=experiment/..., relativo a raiz do TCC_V3.
DESENHO="$(cd "$(dirname "$DESENHO")" && pwd -P)/$(basename "$DESENHO")"
# caminho para o node: no Git Bash do Windows precisa virar C:\..., no Linux nao
host_path() { if command -v cygpath >/dev/null 2>&1; then cygpath -w "$1"; else printf '%s' "$1"; fi; }

# O ID completo e o effort vem do desenho; o apelido entra no run_id.
# O node le o JSON e imprime "<id> <effort>" (ex.: "claude-haiku-4-5 medium"); se o
# apelido nao existe, ou o prefixo nao bate com a replica, ele sai com erro e o
# script para aqui, antes de lancar qualquer coisa.
LIDO="$(node -e '
const d = JSON.parse(require("fs").readFileSync(process.argv[1], "utf8")); const [, , apelido, prefixo, replica] = process.argv;
const id = (d.modelos || {})[apelido];
if (!id) { console.error(`apelido deve ser um de: ${Object.keys(d.modelos || {}).join(", ") || "(o desenho ainda nao tem modelos)"}`); process.exit(1); }
if (prefixo.startsWith(d.prefixo + "-") && prefixo !== `${d.prefixo}-${String(replica).padStart(2, "0")}`) {
  console.error(`no lote ${d.prefixo}, a replica ${replica} roda com o prefixo ${d.prefixo}-${String(replica).padStart(2, "0")}, nao ${prefixo}`); process.exit(1);
}
console.log(id, d.effort, d.ensaio ? "ensaio" : prefixo.startsWith(d.prefixo + "-") ? "lote" : "fora");
' "$(host_path "$DESENHO")" "$WHICH" "$PREFIX" "$REPLICATE")" || exit 2
read -r MODEL_ID DESIGN_EFFORT NO_LOTE <<< "$LIDO"
# O effort de fora (EFFORT=...) so e aceito se for o do desenho.
EFFORT="${EFFORT:-$DESIGN_EFFORT}"
[ "$EFFORT" = "$DESIGN_EFFORT" ] || { echo "EFFORT=$EFFORT, o desenho pede $DESIGN_EFFORT" >&2; exit 2; }
# No lote do desenho, nada de enunciado ou imagem trocados por variavel de ambiente
# (o run-one.sh aceita PROMPT_FILE e IMAGE para os ensaios da bancada; esquecidos no
# terminal, mudariam o V4 sem aviso, e o verify.mjs so acusaria depois de gastar a cota).
# Um desenho de ensaio da bancada (com "ensaio": true) pode usar as duas.
if [ "$NO_LOTE" = "lote" ]; then
    for v in PROMPT_FILE IMAGE; do
        [ -n "${!v:-}" ] && { echo "$v esta definido ($v=${!v}); no lote do desenho, rode sem ele" >&2; exit 2; }
    done
fi
MODELS="$WHICH:$MODEL_ID"
LEVELS="N0 N1 N2 N3"

# ------------------------------------------------------------------ 3. preflight
# As pastas dos niveis N1 a N3 tem de existir e ter conteudo (o N0 nao tem pasta:
# e o workspace vazio).
for level in N1 N2 N3; do
    dir="$ROOT/experiment/harnesses/$level"
    [ -d "$dir" ] || { echo "nivel $level nao encontrado: $dir" >&2; exit 2; }
    [ -f "$dir/CLAUDE.md" ] || compgen -G "$dir/.claude/skills/*/SKILL.md" >/dev/null \
        || { echo "nivel $level vazio: o run-one.sh o recusaria" >&2; exit 2; }
done
# Nenhuma das 4 execucoes pode existir ainda: um quarteto e sempre inteiro e novo.
for m in $MODELS; do
    for level in $LEVELS; do
        id="$PREFIX-${m%%:*}-$level"
        [ -e "$ROOT/runs/$id" ] && { echo "runs/$id ja existe" >&2; exit 2; }
    done
done
mkdir -p "$ROOT/runs/logs"

# ------------------------------------------------------------------ 4. lancar e esperar
RUN_COUNT=$(( $(echo $MODELS | wc -w) * 4 ))
printf '\033[36m=== quarteto %s  |  replica %s  |  effort=%s  |  %s execucoes em paralelo ===\033[0m\n' \
    "$PREFIX" "$REPLICATE" "$EFFORT" "$RUN_COUNT"
START="$(date +%s)"
PIDS=""; IDS=""

# Cada execucao vai para o fundo ("&"), com a saida no runs/logs/<id>.log; o $! e o
# numero do processo, guardado para esperar por ele depois.
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

# Espera as 4, na ordem em que foram lancadas, e conta quantas sairam com erro.
echo "aguardando..."
FAILURES=0
set -- $IDS
for p in $PIDS; do
    wait "$p"; rc=$?
    printf '  %-34s codigo=%s\n' "$1" "$rc"
    [ "$rc" -eq 0 ] || FAILURES=$((FAILURES + 1))
    shift
done

# Resumo: a ultima linha do log de cada execucao (o run-one.sh imprime ali o termino,
# os turnos, os tokens e se compilou).
printf '\nquarteto terminou em %ss  |  falhas: %s\n' "$(( $(date +%s) - START ))" "$FAILURES"
for id in $IDS; do
    [ -f "$ROOT/runs/$id/meta.json" ] && printf '  %-34s %s\n' "$id" "$(tail -n 1 "$ROOT/runs/logs/$id.log")"
done
