#!/usr/bin/env bash
# Executa UMA run do experimento dentro de um container descartavel.
#
# Uso (Git Bash, a partir de qualquer pasta):
#   infra/scripts/executar.sh <run_id> <modelo> <CONTROL|HARNESS> [replicate]
#   infra/scripts/executar.sh SMOKE-01-OPUS-CONTROL claude-opus-5 CONTROL
#   infra/scripts/executar.sh BATCH-01-OPUS-HARNESS claude-opus-5 HARNESS 1
#
# Variaveis opcionais:
#   IMAGE        padrao experimento-harness:v3
#   EFFORT       padrao medium
#   PROMPT_FILE  padrao experiment/prompt/prompt.md
#   HARNESS      padrao only-claude. Nome de uma pasta de experiment/harnesses/,
#                usada so na condicao HARNESS (ex.: HARNESS=claude-and-skills)
#   NETWORK      rotulo da rede, gravado no meta.json (ex.: casa-wifi)
#
# NENHUMA restricao de ferramenta. O agente recebe tudo que o Claude Code
# oferece, inclusive subagente. Nao acrescente --disallowedTools, --allowedTools
# nem --tools: a pergunta do experimento e sobre o Claude Code como ele vem, e
# restringir ferramenta mede uma versao de laboratorio dele.
#
# O workspace nasce VAZIO. As versoes (Java 21, Spring Boot 4.1.1) sao PEDIDAS
# no enunciado, nao impostas pelo ambiente — desobedecer nao invalida a
# execucao, vira dado em `foundation.versions_obeyed`.

set -uo pipefail

morrer() { printf '\033[31mERRO: %s\033[0m\n' "$*" >&2; exit 1; }

case $# in 3|4) ;; *) morrer "uso: $0 <run_id> <modelo> <CONTROL|HARNESS> [replicate]" ;; esac
RUN_ID="$1"; MODEL="$2"; CONDITION="$3"
# A replica e obrigatoria no lote e vazia na fumaca. Com n=3 por celula, sem ela
# nao da para dizer qual das tres e cada run.
REPLICATE="${4:-}"
case "$REPLICATE" in ""|[1-9]|[1-9][0-9]) ;; *) morrer "replicate deve ser inteiro positivo: '$REPLICATE'" ;; esac

RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd -P)"
IMAGE="${IMAGE:-experimento-harness:v3}"
EFFORT="${EFFORT:-medium}"

RUN_DIR="$RAIZ/runs/$RUN_ID"
WS="$RUN_DIR/workspace"
PROMPT="${PROMPT_FILE:-$RAIZ/experiment/prompt/prompt.md}"
HARNESS="${HARNESS:-only-claude}"
HARNESS_DIR="$RAIZ/experiment/harnesses/$HARNESS"
ENV_FILE="$RAIZ/.env"

# ------------------------------------------------------------------ preflight
case "$CONDITION" in CONTROL|HARNESS) ;; *) morrer "condicao deve ser CONTROL ou HARNESS" ;; esac
# Alias resolve para um modelo que voce nao escolheu. Exige o ID completo.
case "$MODEL" in claude-*-*) ;; *) morrer "use o ID completo do modelo, nao alias: '$MODEL'" ;; esac
[ -e "$RUN_DIR" ] && morrer "$RUN_DIR ja existe. Runs nunca sao reaproveitadas."
[ -f "$PROMPT" ] || morrer "enunciado nao encontrado: $PROMPT"
[ -f "$ENV_FILE" ] || morrer "falta $ENV_FILE com CLAUDE_CODE_OAUTH_TOKEN=..."
grep -q '^CLAUDE_CODE_OAUTH_TOKEN=.\+' "$ENV_FILE" || morrer ".env sem CLAUDE_CODE_OAUTH_TOKEN"
# Qualquer uma destas troca cobranca, provedor ou modelo sem avisar, e o
# experimento passaria a medir outra coisa.
if grep -Eq '^(ANTHROPIC_API_KEY|ANTHROPIC_AUTH_TOKEN|ANTHROPIC_BASE_URL|ANTHROPIC_MODEL)=' "$ENV_FILE"; then
    morrer ".env contem variavel que troca cobranca, provedor ou modelo"
fi
docker image inspect "$IMAGE" >/dev/null 2>&1 || morrer "imagem $IMAGE nao existe (rode o docker build)"
if [ "$CONDITION" = "HARNESS" ]; then
    case "$HARNESS" in ""|*[!a-z0-9-]*) morrer "HARNESS deve ser o nome de uma pasta de experiment/harnesses/: '$HARNESS'" ;; esac
    [ -d "$HARNESS_DIR" ] || morrer "harness nao encontrado: $HARNESS_DIR"
    # Uma pasta de skills sem skill e uma versao ainda nao montada: rodar com ela
    # mediria outra coisa com o nome desta.
    if [ -d "$HARNESS_DIR/.claude/skills" ] && ! compgen -G "$HARNESS_DIR/.claude/skills/*/SKILL.md" >/dev/null; then
        morrer "harness $HARNESS tem .claude/skills/ sem nenhuma skill (<nome>/SKILL.md)"
    fi
    [ -f "$HARNESS_DIR/CLAUDE.md" ] || compgen -G "$HARNESS_DIR/.claude/skills/*/SKILL.md" >/dev/null \
        || morrer "harness $HARNESS vazio: precisa de CLAUDE.md ou de .claude/skills/<nome>/SKILL.md"
fi

# Hash estavel de uma pasta: caminhos + conteudo de cada arquivo. O .gitkeep so
# existe para o git guardar pasta vazia e nao chega ao agente, entao nao entra.
hash_arvore() {
    ( cd "$1" && find . -type f -not -name .gitkeep | LC_ALL=C sort | while read -r f; do printf '%s  ' "$f"; sha256sum "$f" | cut -d' ' -f1; done ) \
        | sha256sum | cut -d' ' -f1
}

# ------------------------------------------------------------------ workspace
mkdir -p "$RUN_DIR" "$WS" "$RAIZ/runs/logs"
HARNESS_HASH=""
if [ "$CONDITION" = "HARNESS" ]; then
    # Copia INDISCRIMINADA: qualquer arquivo que sobrar na pasta da versao entra
    # no workspace do agente e contamina o braco. Skills ficam em
    # .claude/skills/<nome>/SKILL.md, onde o Claude Code as acha em /workspace.
    cp -r "$HARNESS_DIR/." "$WS/"
    find "$WS" -name .gitkeep -type f -delete
    HARNESS_HASH="$(hash_arvore "$HARNESS_DIR")"
    echo "harness: $HARNESS ($HARNESS_HASH)"
fi
PROMPT_HASH="$(sha256sum "$PROMPT" | cut -d' ' -f1)"
IMAGE_ID="$(docker image inspect --format '{{.Id}}' "$IMAGE")"

WS_WIN="$(cygpath -w "$WS")"
PROMPT_WIN="$(cygpath -w "$PROMPT")"
ENV_WIN="$(cygpath -w "$ENV_FILE")"
export MSYS_NO_PATHCONV=1

printf '\033[36m=== %s  %s  %s ===\033[0m\n' "$RUN_ID" "$MODEL" "$CONDITION"

# ------------------------------------------------------------------ execucao
START="$(date -Iseconds)"; T0="$(date +%s)"

docker run --rm --name "exp-$RUN_ID" \
    --env-file "$ENV_WIN" \
    --mount "type=bind,source=$WS_WIN,target=/workspace" \
    --mount "type=bind,source=$PROMPT_WIN,target=/experimento/prompt.md,readonly" \
    "$IMAGE" \
    bash -c '
        echo "[pre] claude $(claude --version)" >&2
        echo "[pre] conteudo de ~/.claude: [$(ls -A "$HOME/.claude" 2>/dev/null | tr "\n" " ")]" >&2
        echo "[pre] CLAUDE.md fora do workspace: [$(find / -name CLAUDE.md -not -path "/workspace/*" -not -path "/proc/*" 2>/dev/null | tr "\n" " ")]" >&2
        claude -p \
            --model "$0" \
            --effort "$1" \
            --output-format stream-json --verbose \
            --dangerously-skip-permissions \
            --no-session-persistence \
            < /experimento/prompt.md
' "$MODEL" "$EFFORT" \
    > "$RUN_DIR/claude-output.jsonl" 2> "$RUN_DIR/stderr.txt"
RC=$?

END="$(date -Iseconds)"; DURATION=$(( $(date +%s) - T0 ))
echo "execucao terminou: codigo $RC em ${DURATION}s"

# ------------------------------------------------------------ build pos-execucao
# Container separado, SEM o token: o build e do avaliador, nao do agente. Se
# rodasse no mesmo container, o agente poderia ter mexido no ambiente.
#
# O pom NAO e procurado so na raiz: sem esqueleto o agente escolhe onde poe o
# projeto. Usa o pom mais raso, ignorando target/.
docker run --rm \
    --mount "type=bind,source=$WS_WIN,target=/workspace" \
    "$IMAGE" bash -c '
        POM="$(find /workspace -name pom.xml -not -path "*/target/*" -printf "%d %p\n" 2>/dev/null | sort -n | head -1 | cut -d" " -f2-)"
        [ -n "$POM" ] || { echo "SEM POM em /workspace"; exit 66; }
        echo "pom encontrado: $POM"
        cd "$(dirname "$POM")" && mvn -B verify
    ' \
    > "$RUN_DIR/build.txt" 2>&1
BUILD_RC=$?
echo "build pos-execucao: codigo $BUILD_RC"

# ------------------------------------------------------------------ meta.json
# node.exe e binario do Windows: recebe caminho do Windows, nao o /j/... do Git Bash.
node "$(cygpath -w "$RAIZ/infra/scripts/extrair-meta.mjs")" "$(cygpath -w "$RUN_DIR")" \
    --run_id "$RUN_ID" --model "$MODEL" --condition "$CONDITION" \
    --replicate "$REPLICATE" \
    --start "$START" --end "$END" --duration_s "$DURATION" \
    --exit_code "$RC" --build_code "$BUILD_RC" \
    --effort "$EFFORT" \
    --image "$IMAGE" --image_id "$IMAGE_ID" \
    --prompt_hash "$PROMPT_HASH" --harness_hash "$HARNESS_HASH" \
    --network "${NETWORK:-}"
