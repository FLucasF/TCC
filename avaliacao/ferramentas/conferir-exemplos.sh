#!/usr/bin/env bash
# Sobe a aplicação de uma run e confere os casos campo a campo.
#
# Padrão: os quatro exemplos conferidos do enunciado. Outro conjunto de casos
# entra por `CASOS=`, com o mesmo formato — ver `avaliacao/casos/`.
#
# Nada desta pasta entra no container do agente: o `.dockerignore` é lista
# branca. Aqui os arquivos são montados só na hora de avaliar.
#
# Uso:  avaliacao/ferramentas/conferir-exemplos.sh <run_id> [run_id ...]
#       CASOS=avaliacao/casos/outros.json avaliacao/ferramentas/conferir-exemplos.sh <run_id>
#
# Sai com o total de casos que falharam somando todas as runs.

set -uo pipefail
[ $# -ge 1 ] || { echo "uso: $0 <run_id> [run_id ...]" >&2; exit 2; }

RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd -P)"
IMAGEM="${IMAGEM:-experimento-harness:v3}"
# CASOS aceita arquivo OU pasta. Com pasta, todos os .json de dentro rodam
# contra a mesma subida da aplicação — seis grupos num boot, não seis boots.
CASOS="${CASOS:-$RAIZ/avaliacao/casos/exemplos-enunciado.json}"
[ -e "$CASOS" ] || { echo "casos não encontrados: $CASOS" >&2; exit 2; }
# Absoluto, sempre. `cygpath -w` sobre caminho relativo devolve caminho
# relativo, e o Docker recusa com "is not a valid Windows path" — o que sai como
# código 125 e, até 20/09/2026, era contado como "125 casos com erro".
CASOS="$(cd "$(dirname "$CASOS")" && pwd -P)/$(basename "$CASOS")"
if [ -d "$CASOS" ]; then ALVO_CASOS="/casos"; else ALVO_CASOS="/casos.json"; fi
COMPARADOR="$RAIZ/avaliacao/ferramentas/comparar.mjs"
export MSYS_NO_PATHCONV=1

printf 'casos: %s\n\n' "$(basename "$CASOS")"
TOTAL_FALHAS=0
NAO_SUBIRAM=0
ERROS_DOCKER=0

for RUN_ID in "$@"; do
    WS="$RAIZ/runs/$RUN_ID/workspace"
    printf '\033[36m=== %s ===\033[0m\n' "$RUN_ID"
    [ -d "$WS" ] || { echo "  workspace não encontrado"; TOTAL_FALHAS=$((TOTAL_FALHAS + 1)); continue; }

    docker run --rm --name "conf-$RUN_ID" \
        --mount "type=bind,source=$(cygpath -w "$WS"),target=/ws,readonly" \
        --mount "type=bind,source=$(cygpath -w "$CASOS"),target=$ALVO_CASOS,readonly" \
        --mount "type=bind,source=$(cygpath -w "$COMPARADOR"),target=/comparar.mjs,readonly" \
        "$IMAGEM" bash -c '
# A run arquivada entra somente leitura e é copiada: avaliar não pode alterar o
# que se quer preservar. O `target/` da execução original é descartado, senão
# ele bloqueia a recompilação (aconteceu na FUMACA-01).
cp -r /ws /tmp/app
# O projeto pode não estar na raiz: sem esqueleto o agente escolhe onde põe.
POM="$(find /tmp/app -name pom.xml -not -path "*/target/*" | awk -F/ "{print NF, \$0}" | sort -n | head -1 | cut -d" " -f2-)"
[ -n "$POM" ] || { echo "sem pom no workspace"; exit 66; }
cd "$(dirname "$POM")" && rm -rf target
mvn -q spring-boot:run > /tmp/app.log 2>&1 &
if [ -d /casos ]; then node /comparar.mjs /casos/*.json; else node /comparar.mjs /casos.json; fi
CODIGO=$?
pkill -9 -f java 2>/dev/null || true
exit $CODIGO
' 2>"$RAIZ/runs/logs/conferir-$RUN_ID.err" | sed 's/^/  /'

    # Nem todo codigo de saida e contagem de casos. 66 e "a aplicacao nao subiu";
    # 125, 126 e 127 sao erro do proprio docker. Somar qualquer um deles como
    # numero de casos publica um numero inventado — aconteceu com o 66 em
    # 19/09/2026 e com o 125 em 20/09.
    FALHAS=${PIPESTATUS[0]}
    case "$FALHAS" in
        66) NAO_SUBIRAM=$((NAO_SUBIRAM + 1)) ;;
        125|126|127)
            echo "  ERRO do docker (codigo $FALHAS). Veja runs/logs/conferir-$RUN_ID.err"
            ERROS_DOCKER=$((ERROS_DOCKER + 1)) ;;
        *) TOTAL_FALHAS=$((TOTAL_FALHAS + FALHAS)) ;;
    esac
    echo
done

printf 'casos com erro: %s  |  apps que não subiram: %s\n' "$TOTAL_FALHAS" "$NAO_SUBIRAM"
exit $((TOTAL_FALHAS + NAO_SUBIRAM))
