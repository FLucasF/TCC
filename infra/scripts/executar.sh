#!/usr/bin/env bash
# Executa UMA run do experimento dentro de um container descartável.
#
# Uso (Git Bash, a partir de qualquer pasta):
#   infra/scripts/executar.sh <run_id> <modelo> <SEM|COM>
#   infra/scripts/executar.sh FUMACA-01 claude-haiku-4-5 SEM
#
# Variáveis opcionais:
#   IMAGEM     padrão experimento-harness:v2
#   EFFORT     padrão high, conforme D8 do plano
#   ESQUELETO  padrão sim. "nao" começa com o workspace vazio
#   PROMPT_ARQ padrão experimento/prompt/prompt.md

set -uo pipefail

morrer() { printf '\033[31mERRO: %s\033[0m\n' "$*" >&2; exit 1; }

[ $# -eq 3 ] || morrer "uso: $0 <run_id> <modelo> <SEM|COM>"
RUN_ID="$1"; MODELO="$2"; COND="$3"

RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd -P)"
IMAGEM="${IMAGEM:-experimento-harness:v2}"
FERRAMENTAS_BLOQUEADAS="WebSearch,WebFetch,Agent,Task"
EFFORT="${EFFORT:-high}"
# ESQUELETO=nao começa com o workspace vazio. Nesse caso o enunciado também
# precisa mudar, porque o padrão diz que já existe um projeto na pasta.
ESQUELETO="${ESQUELETO:-sim}"

RUN_DIR="$RAIZ/runs/$RUN_ID"
WS="$RUN_DIR/workspace"
PROMPT="${PROMPT_ARQ:-$RAIZ/experimento/prompt/prompt.md}"
ENV_FILE="$RAIZ/.env"

# ------------------------------------------------------------------ preflight
case "$COND" in SEM|COM) ;; *) morrer "condição deve ser SEM ou COM" ;; esac
case "$MODELO" in claude-*-*) ;; *) morrer "use o ID completo do modelo, não alias: '$MODELO'" ;; esac
[ -e "$RUN_DIR" ] && morrer "$RUN_DIR já existe. Runs nunca são reaproveitadas."
[ -f "$PROMPT" ] || morrer "prompt não encontrado: $PROMPT"
[ -f "$ENV_FILE" ] || morrer "falta $ENV_FILE com CLAUDE_CODE_OAUTH_TOKEN=..."
grep -q '^CLAUDE_CODE_OAUTH_TOKEN=.\+' "$ENV_FILE" || morrer ".env sem CLAUDE_CODE_OAUTH_TOKEN"
if grep -Eq '^(ANTHROPIC_API_KEY|ANTHROPIC_AUTH_TOKEN|ANTHROPIC_BASE_URL|ANTHROPIC_MODEL)=' "$ENV_FILE"; then
    morrer ".env contém variável que troca cobrança, provedor ou modelo"
fi
docker image inspect "$IMAGEM" >/dev/null 2>&1 || morrer "imagem $IMAGEM não existe (rode o docker build)"
if [ "$COND" = "COM" ]; then
    [ -f "$RAIZ/experimento/harness/CLAUDE.md" ] || morrer "condição COM sem harness/CLAUDE.md"
fi

hash_arvore() { # hash estável de uma pasta: caminhos + conteúdo
    ( cd "$1" && find . -type f | LC_ALL=C sort | while read -r f; do printf '%s  ' "$f"; sha256sum "$f" | cut -d' ' -f1; done ) \
        | sha256sum | cut -d' ' -f1
}

# ------------------------------------------------------------------ workspace
mkdir -p "$RUN_DIR"
if [ "$ESQUELETO" = "sim" ]; then
    cp -r "$RAIZ/experimento/skeleton" "$WS"
    HASH_SKELETON="$(hash_arvore "$RAIZ/experimento/skeleton")"
    HASH_POM="$(sha256sum "$RAIZ/experimento/skeleton/pom.xml" | cut -d' ' -f1)"
else
    mkdir -p "$WS"
    HASH_SKELETON="vazio"
    HASH_POM=""
fi
HASH_HARNESS=""
if [ "$COND" = "COM" ]; then
    cp -r "$RAIZ/experimento/harness/." "$WS/"
    HASH_HARNESS="$(hash_arvore "$RAIZ/experimento/harness")"
fi
HASH_PROMPT="$(sha256sum "$PROMPT" | cut -d' ' -f1)"
# Dependencias declaradas no pom de partida, para o extrator comparar no fim.
DEPS_ANTES="[]"
if [ "$ESQUELETO" = "sim" ]; then
DEPS_ANTES="$(node -e 'const t=require("fs").readFileSync(process.argv[1],"utf8");
const b=t.match(/<dependencies>([\s\S]*?)<\/dependencies>/);
console.log(JSON.stringify(b?[...b[1].matchAll(/<groupId>\s*([^<]+?)\s*<\/groupId>\s*<artifactId>\s*([^<]+?)\s*<\/artifactId>/g)].map(m=>m[1]+":"+m[2]).sort():[]));' "$(cygpath -w "$RAIZ/experimento/skeleton/pom.xml")")"
fi
IMAGEM_ID="$(docker image inspect --format '{{.Id}}' "$IMAGEM")"

WS_WIN="$(cygpath -w "$WS")"
PROMPT_WIN="$(cygpath -w "$PROMPT")"
ENV_WIN="$(cygpath -w "$ENV_FILE")"
export MSYS_NO_PATHCONV=1

printf '\033[36m=== %s  %s  %s ===\033[0m\n' "$RUN_ID" "$MODELO" "$COND"

# ------------------------------------------------------------------ execução
INICIO="$(date -Iseconds)"; T0="$(date +%s)"

docker run --rm --name "exp-$RUN_ID" \
    --env-file "$ENV_WIN" \
    --mount "type=bind,source=$WS_WIN,target=/workspace" \
    --mount "type=bind,source=$PROMPT_WIN,target=/experimento/prompt.md,readonly" \
    "$IMAGEM" \
    bash -c '
        echo "[pre] claude $(claude --version)" >&2
        echo "[pre] conteudo de ~/.claude: [$(ls -A "$HOME/.claude" 2>/dev/null | tr "\n" " ")]" >&2
        echo "[pre] CLAUDE.md fora do workspace: [$(find / -name CLAUDE.md -not -path "/workspace/*" -not -path "/proc/*" 2>/dev/null | tr "\n" " ")]" >&2
        claude -p \
            --model "$0" \
            --effort "$2" \
            --output-format stream-json --verbose \
            --dangerously-skip-permissions \
            --disallowedTools "$1" \
            --no-session-persistence \
            < /experimento/prompt.md
    ' "$MODELO" "$FERRAMENTAS_BLOQUEADAS" "$EFFORT" \
    > "$RUN_DIR/claude-output.jsonl" 2> "$RUN_DIR/stderr.txt"
RC=$?

FIM="$(date -Iseconds)"; DURACAO=$(( $(date +%s) - T0 ))
echo "execução terminou: código $RC em ${DURACAO}s"

# ------------------------------------------------------------------ build pós-execução
# Container separado, sem token: o build é do avaliador, não do agente.
#
# O pom NÃO é procurado só na raiz. Sem esqueleto o agente escolhe onde põe o
# projeto — a MED-06-VAZIO-COM criou em `checkout-service/`, e um build que só
# olha `/workspace` marca como fracasso uma app que compila. Usa o pom mais
# raso, ignorando `target/`.
docker run --rm \
    --mount "type=bind,source=$WS_WIN,target=/workspace" \
    "$IMAGEM" bash -c '
        POM="$(find /workspace -name pom.xml -not -path "*/target/*" -printf "%d %p\n" 2>/dev/null | sort -n | head -1 | cut -d" " -f2-)"
        [ -n "$POM" ] || { echo "SEM POM em /workspace"; exit 66; }
        echo "pom encontrado: $POM"
        cd "$(dirname "$POM")" && mvn -B verify
    ' \
    > "$RUN_DIR/build.txt" 2>&1
BUILD_RC=$?
echo "build pós-execução: código $BUILD_RC"

# ------------------------------------------------------------------ meta.json
# node.exe é binário do Windows: recebe caminho do Windows, não o /j/... do Git Bash.
node "$(cygpath -w "$RAIZ/infra/scripts/extrair-meta.mjs")" "$(cygpath -w "$RUN_DIR")" \
    --run_id "$RUN_ID" --modelo "$MODELO" --condicao "$COND" \
    --inicio "$INICIO" --fim "$FIM" --duracao_s "$DURACAO" \
    --codigo_saida "$RC" --build_codigo "$BUILD_RC" \
    --ferramentas_bloqueadas "$FERRAMENTAS_BLOQUEADAS" --effort "$EFFORT" \
    --imagem "$IMAGEM" --imagem_id "$IMAGEM_ID" \
    --hash_prompt "$HASH_PROMPT" --hash_skeleton "$HASH_SKELETON" --hash_harness "$HASH_HARNESS" \
    --hash_pom "$HASH_POM" --deps_antes "$DEPS_ANTES" --esqueleto "$ESQUELETO"
