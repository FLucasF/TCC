#!/usr/bin/env bash
# A pilot of the v2 prompt: ONE run, the same way as V5's run-one.sh (same image, same isolation,
# same token), but writing to runs/pilots/, outside TCC_V5 (the V5 batch is frozen).
#
# Usage (Git Bash, from TCC_V6):  infra/scripts/pilot.sh <id> <model> [effort]
#   infra/scripts/pilot.sh SONNET55-N0-01 claude-sonnet-5-5 medium
#
# Differences from run-one.sh, on purpose:
#   - only the arm without harness (N0): the workspace starts empty;
#   - the prompt is a COPY of experiment/prompt/prompt.md (the copy and its hash stay in the run
#     folder). Before running, it checks that no internal note was left and that the examples match
#     the reference calculator;
#   - the build runs without a database and without the TransLog, as the prompt says (with network,
#     so Maven can download the dependencies, as in V5).
set -uo pipefail
die() { printf '\033[31mERROR: %s\033[0m\n' "$*" >&2; exit 1; }
[ $# -ge 2 ] || die "usage: $0 <id> <model> [effort]"
ID="$1"; MODEL="$2"; EFFORT="${3:-medium}"
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"
V6="$(cd "$HERE/../.." && pwd -P)"
DRAFT="$V6/experiment/prompt/prompt.md"
REF="$V6/evaluation/reference"
ENV_FILE="/j/TCC/TCC_V5/.env"                       # the same subscription token as V5 (never printed)
IMAGE="experimento-harness:v5"
RUN="$V6/runs/pilots/$ID"; WS="$RUN/workspace"

case "$MODEL" in claude-*-*) ;; *) die "use the full model ID: '$MODEL'" ;; esac
[ -e "$RUN" ] && die "$RUN already exists: runs are never reused"
[ -f "$DRAFT" ] || die "experiment/prompt/prompt.md not found"
grep -q '^CLAUDE_CODE_OAUTH_TOKEN=.\+' "$ENV_FILE" || die ".env has no CLAUDE_CODE_OAUTH_TOKEN"
grep -Eq '^(ANTHROPIC_API_KEY|ANTHROPIC_AUTH_TOKEN|ANTHROPIC_BASE_URL|ANTHROPIC_MODEL)=' "$ENV_FILE" \
    && die ".env has a variable that changes the billing, the provider or the model"
docker image inspect "$IMAGE" >/dev/null 2>&1 || die "image $IMAGE does not exist"

# the pilot's prompt: the whole draft, with no internal note and with the examples checked
grep -q "A PREENCHER" "$DRAFT" && die "an internal note was left in the prompt"
( cd "$REF" && mvn -B -q compile >/dev/null 2>&1 && java -cp target/classes reference.PromptExamples --check "$(cygpath -w "$DRAFT")" ) \
    || die "the prompt's examples do not match the reference calculator"
mkdir -p "$WS"
cp "$DRAFT" "$RUN/prompt.md"
PROMPT_HASH="$(sha256sum "$RUN/prompt.md" | cut -d' ' -f1)"

export MSYS_NO_PATHCONV=1
WS_WIN="$(cygpath -w "$WS")"; PROMPT_WIN="$(cygpath -w "$RUN/prompt.md")"; ENV_WIN="$(cygpath -w "$ENV_FILE")"
printf '\033[36m=== %s  %s  effort %s ===\033[0m\n' "$ID" "$MODEL" "$EFFORT"
START="$(date -Iseconds)"; T0="$(date +%s)"
docker run --rm --name "pilot-$ID" \
    --env-file "$ENV_WIN" \
    --mount "type=bind,source=$WS_WIN,target=/workspace" \
    --mount "type=bind,source=$PROMPT_WIN,target=/experimento/prompt.md,readonly" \
    "$IMAGE" \
    bash -c '
        echo "[pre] claude $(claude --version)" >&2
        echo "[pre] contents of ~/.claude: [$(ls -A "$HOME/.claude" 2>/dev/null | tr "\n" " ")]" >&2
        echo "[pre] CLAUDE.md outside the workspace: [$(find / -name CLAUDE.md -not -path "/workspace/*" -not -path "/proc/*" 2>/dev/null | tr "\n" " ")]" >&2
        claude -p --model "$0" --effort "$1" --output-format stream-json --verbose \
            --dangerously-skip-permissions --no-session-persistence < /experimento/prompt.md
    ' "$MODEL" "$EFFORT" > "$RUN/claude-output.jsonl" 2> "$RUN/stderr.txt"
RC=$?; END="$(date -Iseconds)"; DUR=$(( $(date +%s) - T0 ))
echo "run finished: exit code $RC in ${DUR}s"

# the build, in a separate container and without the token (no database and no TransLog)
docker run --rm --mount "type=bind,source=$WS_WIN,target=/workspace" "$IMAGE" bash -c '
    POM="$(find /workspace -name pom.xml -not -path "*/target/*" -printf "%d %p\n" 2>/dev/null | sort -n | head -1 | cut -d" " -f2-)"
    [ -n "$POM" ] || { echo "NO POM in /workspace"; exit 66; }
    echo "pom found: $POM"; cd "$(dirname "$POM")" && mvn -B verify
' > "$RUN/build.txt" 2>&1
BRC=$?
echo "build: exit code $BRC"

# a simple summary, read from the transcript (Claude Code's "result" event)
PYTHONIOENCODING=utf-8 python - "$(cygpath -w "$RUN")" "$ID" "$MODEL" "$EFFORT" "$START" "$END" "$DUR" "$RC" "$BRC" "$PROMPT_HASH" <<'EOF'
import json, sys, os
run, run_id, model, effort, start, end, seconds, rc, brc, prompt_hash = sys.argv[1:]
result, models, tools = None, set(), {}
for line in open(os.path.join(run, "claude-output.jsonl"), encoding="utf8", errors="replace"):
    try: event = json.loads(line)
    except Exception: continue
    if event.get("type") == "system" and event.get("subtype") == "init": models.add(event.get("model"))
    if event.get("type") == "assistant":
        for content in event.get("message", {}).get("content", []):
            if content.get("type") == "tool_use": tools[content["name"]] = tools.get(content["name"], 0) + 1
    if event.get("type") == "result": result = event
result = result or {}
usage = result.get("usage", {})
meta = {"id": run_id, "requested_model": model, "answering_models": sorted(m for m in models if m), "effort": effort,
        "start": start, "end": end, "seconds": int(seconds), "run_exit_code": int(rc), "build_exit_code": int(brc),
        "prompt_sha256": prompt_hash, "turns": result.get("num_turns"), "cost_usd": result.get("total_cost_usd"),
        "tokens": {k: usage.get(k) for k in ("input_tokens", "cache_creation_input_tokens", "cache_read_input_tokens", "output_tokens")},
        "tools": tools, "final_answer": (result.get("result") or "")[:200]}
json.dump(meta, open(os.path.join(run, "meta.json"), "w", encoding="utf8"), ensure_ascii=False, indent=2)
print(json.dumps({k: meta[k] for k in ("seconds", "turns", "cost_usd", "build_exit_code", "final_answer")}, ensure_ascii=False))
EOF
