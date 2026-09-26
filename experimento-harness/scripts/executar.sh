#!/usr/bin/env bash
# Executa UMA run do experimento dentro de um container descartável.
#
# Uso (Git Bash, a partir de qualquer pasta):
#   scripts/executar.sh <run_id> <modelo> <SEM|COM>
#   scripts/executar.sh FUMACA-01 claude-haiku-4-5 SEM
#
# Variáveis opcionais: IMAGEM, MAX_TURNOS, TEMPO_MAX (segundos)

set -uo pipefail

morrer() { printf '\033[31mERRO: %s\033[0m\n' "$*" >&2; exit 1; }

[ $# -eq 3 ] || morrer "uso: $0 <run_id> <modelo> <SEM|COM>"
RUN_ID="$1"; MODELO="$2"; COND="$3"

RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd -P)"
IMAGEM="${IMAGEM:-experimento-harness:v1}"
MAX_TURNOS="${MAX_TURNOS:-200}"
TEMPO_MAX="${TEMPO_MAX:-3600}"
FERRAMENTAS_BLOQUEADAS="WebSearch,WebFetch,Agent,Task"

RUN_DIR="$RAIZ/runs/$RUN_ID"
WS="$RUN_DIR/workspace"
PROMPT="$RAIZ/prompt/prompt.md"
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
    [ -f "$RAIZ/harness/CLAUDE.md" ] || morrer "condição COM sem harness/CLAUDE.md"
fi

hash_arvore() { # hash estável de uma pasta: caminhos + conteúdo
    ( cd "$1" && find . -type f | LC_ALL=C sort | while read -r f; do printf '%s  ' "$f"; sha256sum "$f" | cut -d' ' -f1; done ) \
        | sha256sum | cut -d' ' -f1
}

# ------------------------------------------------------------------ workspace
mkdir -p "$RUN_DIR"
cp -r "$RAIZ/skeleton" "$WS"
HASH_HARNESS=""
if [ "$COND" = "COM" ]; then
    cp -r "$RAIZ/harness/." "$WS/"
    HASH_HARNESS="$(hash_arvore "$RAIZ/harness")"
fi
HASH_PROMPT="$(sha256sum "$PROMPT" | cut -d' ' -f1)"
HASH_SKELETON="$(hash_arvore "$RAIZ/skeleton")"
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
        timeout "$0" claude -p \
            --model "$1" \
            --effort high \
            --output-format stream-json --verbose \
            --max-turns "$2" \
            --dangerously-skip-permissions \
            --disallowedTools "$3" \
            --no-session-persistence \
            < /experimento/prompt.md
    ' "$TEMPO_MAX" "$MODELO" "$MAX_TURNOS" "$FERRAMENTAS_BLOQUEADAS" \
    > "$RUN_DIR/claude-output.jsonl" 2> "$RUN_DIR/stderr.txt"
RC=$?

FIM="$(date -Iseconds)"; DURACAO=$(( $(date +%s) - T0 ))
echo "execução terminou: código $RC em ${DURACAO}s"

# ------------------------------------------------------------------ build pós-execução
# Container separado: só compila o que o modelo deixou, sem internet do Maven.
docker run --rm \
    --mount "type=bind,source=$WS_WIN,target=/workspace" \
    "$IMAGEM" bash -c 'cd /workspace && mvn -o -B verify' \
    > "$RUN_DIR/build.txt" 2>&1
BUILD_RC=$?
echo "build pós-execução: código $BUILD_RC"

# ------------------------------------------------------------------ meta.json
# node.exe é binário do Windows: recebe caminho do Windows, não o /j/... do Git Bash.
node "$(cygpath -w "$RAIZ/scripts/extrair-meta.mjs")" "$(cygpath -w "$RUN_DIR")" \
    --run_id "$RUN_ID" --modelo "$MODELO" --condicao "$COND" \
    --inicio "$INICIO" --fim "$FIM" --duracao_s "$DURACAO" \
    --codigo_saida "$RC" --build_codigo "$BUILD_RC" \
    --max_turnos "$MAX_TURNOS" --tempo_maximo_s "$TEMPO_MAX" \
    --ferramentas_bloqueadas "$FERRAMENTAS_BLOQUEADAS" \
    --imagem "$IMAGEM" --imagem_id "$IMAGEM_ID" \
    --hash_prompt "$HASH_PROMPT" --hash_skeleton "$HASH_SKELETON" --hash_harness "$HASH_HARNESS"
