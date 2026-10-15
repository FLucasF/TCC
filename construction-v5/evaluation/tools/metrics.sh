#!/usr/bin/env bash
# Metricas automaticas do codigo de cada execucao de um lote: CK e SonarQube.
#
# Uso (Git Bash):
#   evaluation/tools/metrics.sh <prefixo>        (ex.: TESTE-STRATEGY-01)
#
# Mede o codigo de producao (src/main/java), ja compilado pelo build da bancada
# (target/classes, que o SonarQube precisa para analisar Java). Nao julga nada:
# so extrai numeros, e por isso roda direto em runs/, sem pacote anonimizado.
# Uma ferramenta nao sabe qual e o braco; a cegueira protege o julgamento humano.
#
# Pre-requisitos:
#   - Docker aberto, com o servidor tcc-sonarqube no ar (ver evaluation/tools/README.md)
#   - .env.sonar na raiz, com SONAR_TOKEN=... (fora do git; nunca e impresso)
#
# Saida: evaluation/metrics/<prefixo>/<run_id>/ (ck/ e sonar/) e, no fim,
# evaluation/metrics/<prefixo>/metricas.csv, uma linha por execucao.
# Recusa rodar se a pasta de saida ja existe, ou se alguma execucao ja foi
# analisada no SonarQube: uma analise nunca e refeita por cima de outra.
#
# COMO LER. Duas ferramentas, as duas so contam, sem julgar:
#   - o CK (um .jar) le o codigo-fonte e mede cada classe: acoplamento (CBO),
#     complexidade somada dos metodos (WMC), coesao (LCOM), heranca (DIT)...
#   - o SonarQube (um servidor no Docker) recebe o codigo e as classes compiladas por
#     um "scanner" e devolve linhas de codigo, complexidade ciclomatica e COGNITIVA (a
#     hipotese da qualidade), duplicacao e code smells.
# Tres partes: (1) preflight: confere que as versoes sao as travadas (o hash do .jar
# do CK, a imagem do SonarQube e do scanner) e que nenhuma execucao foi analisada
# antes; (2) por execucao: acha o projeto, roda o CK, roda o scanner e espera o
# servidor devolver as medidas; (3) no fim, o aggregate-metrics.mjs junta tudo no CSV.
# O SonarQube fica DESLIGADO durante as execucoes do V4 e ligado so para esta etapa.

set -uo pipefail
morrer() { printf '\033[31mERRO: %s\033[0m\n' "$*" >&2; exit 1; }

[ $# -eq 1 ] || morrer "uso: $0 <prefixo>"
PREFIXO="$1"
case "$PREFIXO" in ""|*[!A-Za-z0-9-]*) morrer "prefixo invalido: '$PREFIXO'" ;; esac

RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd -P)"
OUT="$RAIZ/evaluation/metrics/$PREFIXO"
ENV_SONAR="$RAIZ/.env.sonar"

# As versoes travadas. Mudar qualquer uma destas e um instrumento novo.
CK_JAR="$RAIZ/evaluation/tools/ck/ck-0.7.1-8a1ef916-jar-with-dependencies.jar"
CK_SHA256="f3c0e5bc159189ebd213badf693ab792a31cbea749eb9f97b9185938c54a7baf"
SONAR_IMAGE_ID="sha256:c0f1160bccfa435db4168c2d7df69af3c3ea7bfeb87395613014048fb33a68c1"
SCANNER="sonarsource/sonar-scanner-cli@sha256:a3f4215076706c95a17a68c19322ee916e40a3acd081a8c1a1e839e0194afa57"
SONAR_URL="http://localhost:9000"
REDE="tcc-sonar-net"
METRICAS="ncloc,classes,functions,complexity,cognitive_complexity,duplicated_lines_density,duplicated_blocks,code_smells,sqale_index"

# ------------------------------------------------------------------ preflight
[ -f "$CK_JAR" ] || morrer "falta o CK em $CK_JAR"
[ "$(sha256sum "$CK_JAR" | cut -d' ' -f1)" = "$CK_SHA256" ] || morrer "o .jar do CK nao e o travado (sha256 diferente)"
[ -f "$ENV_SONAR" ] && grep -q '^SONAR_TOKEN=.\+' "$ENV_SONAR" || morrer "falta $ENV_SONAR com SONAR_TOKEN=..."
[ "$(docker inspect --format '{{.Image}}' tcc-sonarqube 2>/dev/null)" = "$SONAR_IMAGE_ID" ] \
    || morrer "o container tcc-sonarqube nao existe ou nao usa a imagem travada"
[ "$(docker inspect --format '{{.State.Running}}' tcc-sonarqube)" = "true" ] || morrer "tcc-sonarqube esta parado (docker start tcc-sonarqube)"
curl -s "$SONAR_URL/api/system/status" | grep -q '"status":"UP"' || morrer "o SonarQube nao esta pronto em $SONAR_URL"
docker image inspect "$SCANNER" >/dev/null 2>&1 || morrer "falta a imagem travada do scanner"
docker network inspect "$REDE" >/dev/null 2>&1 || docker network create "$REDE" >/dev/null
docker network inspect "$REDE" --format '{{range .Containers}}{{.Name}} {{end}}' | grep -qw tcc-sonarqube \
    || docker network connect "$REDE" tcc-sonarqube

RUNS="$(cd "$RAIZ/runs" && ls -d "$PREFIXO"-* 2>/dev/null | LC_ALL=C sort)"
[ -n "$RUNS" ] || morrer "nenhuma execucao runs/$PREFIXO-*"
[ -e "$OUT" ] && morrer "$OUT ja existe: apague a pasta para refazer"

# O token so vive nesta variavel; nunca e impresso.
TOKEN="$(sed -n 's/^SONAR_TOKEN=//p' "$ENV_SONAR" | tr -d '\r\n ')"
for run in $RUNS; do
    code="$(curl -s -o /dev/null -w '%{http_code}' -u "$TOKEN:" "$SONAR_URL/api/components/show?component=$run")"
    [ "$code" = "404" ] || morrer "$run ja foi analisada no SonarQube (HTTP $code): nao se refaz por cima"
done

mkdir -p "$OUT"
export MSYS_NO_PATHCONV=1
printf '\033[36m=== metricas %s: %s execucoes ===\033[0m\n' "$PREFIXO" "$(echo "$RUNS" | wc -l)"

# ------------------------------------------------------------------ por execucao
for run in $RUNS; do
    dir="$OUT/$run"; mkdir -p "$dir/ck" "$dir/sonar"
    # O pom mais raso, como no build da bancada: o agente escolhe onde poe o projeto.
    pom="$(find "$RAIZ/runs/$run/workspace" -name pom.xml -not -path '*/target/*' -printf '%d %p\n' 2>/dev/null | sort -n | head -1 | cut -d' ' -f2-)"
    if [ -z "$pom" ]; then echo "sem_pom" > "$dir/status"; echo "  $run: sem pom"; continue; fi
    proj="$(dirname "$pom")"
    if [ ! -d "$proj/src/main/java" ]; then echo "sem_src_main_java" > "$dir/status"; echo "  $run: sem src/main/java"; continue; fi

    java -jar "$(cygpath -w "$CK_JAR")" "$(cygpath -w "$proj/src/main/java")" false 0 false "$(cygpath -w "$dir/ck")/" \
        > "$dir/ck/ck.log" 2>&1 || { echo "ck_falhou" > "$dir/status"; echo "  $run: CK falhou"; continue; }

    if [ ! -d "$proj/target/classes" ]; then echo "sem_classes" > "$dir/status"; echo "  $run: CK ok, sem classes compiladas para o SonarQube"; continue; fi
    docker run --rm --network "$REDE" --env-file "$(cygpath -w "$ENV_SONAR")" -e SONAR_HOST_URL=http://tcc-sonarqube:9000 \
        --mount "type=bind,source=$(cygpath -w "$proj"),target=/usr/src,readonly" \
        -v tcc-sonar-scannercache:/opt/sonar-scanner/.sonar/cache \
        "$SCANNER" \
        -Dsonar.projectKey="$run" -Dsonar.sources=src/main/java -Dsonar.java.binaries=target/classes \
        -Dsonar.java.source=21 -Dsonar.scm.disabled=true -Dsonar.working.directory=/tmp/scannerwork \
        > "$dir/sonar/scanner.log" 2>&1 || { echo "sonar_falhou" > "$dir/status"; echo "  $run: SonarQube falhou"; continue; }

    # O servidor processa o relatorio depois do scanner sair: espera as medidas.
    ok=""
    for i in $(seq 1 40); do
        curl -s -u "$TOKEN:" "$SONAR_URL/api/measures/component?component=$run&metricKeys=$METRICAS" > "$dir/sonar/measures.json"
        grep -q '"measures":\[{' "$dir/sonar/measures.json" && { ok=1; break; }
        sleep 3
    done
    [ -n "$ok" ] || { echo "sonar_sem_medidas" > "$dir/status"; echo "  $run: SonarQube nao devolveu medidas"; continue; }
    echo "ok" > "$dir/status"; echo "  $run: ok"
done

node "$(cygpath -w "$RAIZ/evaluation/tools/aggregate-metrics.mjs")" "$PREFIXO"
