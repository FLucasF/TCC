"""A pilot of the v2 prompt: ONE run, the same way as V5's run-one.sh (same image, same isolation, same token), but
writing to runs/pilots/, outside TCC_V5 (the V5 batch is frozen).

Usage (from TCC_V6):  python infra/scripts/pilot.py <id> <model> [effort] [--dry-run]
                      python infra/scripts/pilot.py SONNET55-N0-04 claude-sonnet-5-5 medium

Differences from run-one.sh, on purpose:
  - only the arm without harness (N0): the workspace starts empty;
  - the prompt is a COPY of experiment/prompt/prompt.md (the copy and its hash stay in the run folder). Before
    running, it checks that no internal note was left and that the examples match the reference calculator;
  - the build runs without a database and without the TransLog, as the prompt says (with network, so Maven can
    download the dependencies, as in V5).

Output in runs/pilots/<id>/: prompt.md, claude-output.jsonl, stderr.txt, build.txt and meta.json.
The subscription token is read by Docker from V5's .env file (--env-file); this script never reads its value.
"""

import hashlib
import json
import re
import shutil
import subprocess
import sys
import time
from datetime import datetime
from pathlib import Path
from typing import Any

V6 = Path(__file__).resolve().parents[2]
PROMPT = V6 / "experiment" / "prompt" / "prompt.md"
REFERENCE = V6 / "evaluation" / "reference"
PILOTS = V6 / "runs" / "pilots"
ENV_FILE = V6.parent / "TCC_V5" / ".env"          # the same subscription token as V5 (never printed)
IMAGE = "experimento-harness:v5"

# what runs inside the container: the agent, reading the prompt from stdin
AGENT_COMMAND = """
    echo "[pre] claude $(claude --version)" >&2
    echo "[pre] contents of ~/.claude: [$(ls -A "$HOME/.claude" 2>/dev/null | tr "\\n" " ")]" >&2
    echo "[pre] CLAUDE.md outside the workspace: [$(find / -name CLAUDE.md -not -path "/workspace/*" -not -path "/proc/*" 2>/dev/null | tr "\\n" " ")]" >&2
    claude -p --model "$0" --effort "$1" --output-format stream-json --verbose \\
        --dangerously-skip-permissions --no-session-persistence < /experimento/prompt.md
"""
# the build, in a separate container without the token: the shallowest pom.xml, then mvn verify
BUILD_COMMAND = """
    POM="$(find /workspace -name pom.xml -not -path "*/target/*" -printf "%d %p\\n" 2>/dev/null | sort -n | head -1 | cut -d" " -f2-)"
    [ -n "$POM" ] || { echo "NO POM in /workspace"; exit 66; }
    echo "pom found: $POM"; cd "$(dirname "$POM")" && mvn -B verify
"""
# variables that would change the billing, the provider or the model
FORBIDDEN_IN_ENV = re.compile(r"^(ANTHROPIC_API_KEY|ANTHROPIC_AUTH_TOKEN|ANTHROPIC_BASE_URL|ANTHROPIC_MODEL)=", re.MULTILINE)


def die(message: str) -> None:
    sys.exit(f"\033[31mERROR: {message}\033[0m")


def check_before_running(run_folder: Path, model: str) -> None:
    if not re.fullmatch(r"claude-[\w.-]+-[\w.-]+", model):
        die(f"use the full model ID: '{model}'")
    if run_folder.exists():
        die(f"{run_folder} already exists: runs are never reused")
    if not PROMPT.is_file():
        die(f"{PROMPT} not found")
    # the .env is only checked for which variables it sets; no value is kept or printed
    env_text = ENV_FILE.read_text(encoding="utf-8")
    if not re.search(r"^CLAUDE_CODE_OAUTH_TOKEN=.+", env_text, re.MULTILINE):
        die(".env has no CLAUDE_CODE_OAUTH_TOKEN")
    if FORBIDDEN_IN_ENV.search(env_text):
        die(".env has a variable that changes the billing, the provider or the model")
    if subprocess.run(["docker", "image", "inspect", IMAGE], capture_output=True, check=False).returncode != 0:
        die(f"image {IMAGE} does not exist")
    if "A PREENCHER" in PROMPT.read_text(encoding="utf-8"):
        die("an internal note was left in the prompt")
    maven = shutil.which("mvn") or "mvn"
    compiled = subprocess.run([maven, "-B", "-q", "compile"], cwd=REFERENCE, capture_output=True, check=False)
    checked = subprocess.run(["java", "-cp", "target/classes", "reference.PromptExamples", "--check", str(PROMPT)],
                             cwd=REFERENCE, capture_output=True, text=True, encoding="utf-8", check=False)
    if compiled.returncode != 0 or checked.returncode != 0:
        die("the prompt's examples do not match the reference calculator")
    print(checked.stdout.strip())


def agent_docker_command(run_folder: Path, workspace: Path, model: str, effort: str) -> list[str]:
    return [
        "docker", "run", "--rm", "--name", f"pilot-{run_folder.name}",
        "--env-file", str(ENV_FILE),
        "--mount", f"type=bind,source={workspace},target=/workspace",
        "--mount", f"type=bind,source={run_folder / 'prompt.md'},target=/experimento/prompt.md,readonly",
        IMAGE, "bash", "-c", AGENT_COMMAND, model, effort,
    ]


def build_docker_command(workspace: Path) -> list[str]:
    return ["docker", "run", "--rm", "--mount", f"type=bind,source={workspace},target=/workspace",
            IMAGE, "bash", "-c", BUILD_COMMAND]


def run_agent(run_folder: Path, workspace: Path, model: str, effort: str) -> int:
    command = agent_docker_command(run_folder, workspace, model, effort)
    with (run_folder / "claude-output.jsonl").open("wb") as output, (run_folder / "stderr.txt").open("wb") as errors:
        return subprocess.run(command, stdout=output, stderr=errors, check=False).returncode


def run_build(run_folder: Path, workspace: Path) -> int:
    command = build_docker_command(workspace)
    with (run_folder / "build.txt").open("wb") as output:
        return subprocess.run(command, stdout=output, stderr=subprocess.STDOUT, check=False).returncode


def summarize(run_folder: Path, details: dict[str, Any]) -> dict[str, Any]:
    """A simple summary, read from the transcript (Claude Code's "result" event)."""
    result: dict[str, Any] = {}
    answering_models: set[str] = set()
    tools_used: dict[str, int] = {}
    transcript = (run_folder / "claude-output.jsonl").read_text(encoding="utf-8", errors="replace")
    for line in transcript.splitlines():
        try:
            event = json.loads(line)
        except json.JSONDecodeError:
            continue
        if event.get("type") == "system" and event.get("subtype") == "init" and event.get("model"):
            answering_models.add(event["model"])
        if event.get("type") == "assistant":
            for content in event.get("message", {}).get("content", []):
                if content.get("type") == "tool_use":
                    tools_used[content["name"]] = tools_used.get(content["name"], 0) + 1
        if event.get("type") == "result":
            result = event
    usage = result.get("usage", {})
    token_kinds = ("input_tokens", "cache_creation_input_tokens", "cache_read_input_tokens", "output_tokens")
    return {
        **details,
        "answering_models": sorted(answering_models),
        "turns": result.get("num_turns"),
        "cost_usd": result.get("total_cost_usd"),
        "tokens": {kind: usage.get(kind) for kind in token_kinds},
        "tools": tools_used,
        "final_answer": (result.get("result") or "")[:200],
    }


def main() -> None:
    arguments = sys.argv[1:]
    dry_run = "--dry-run" in arguments            # does every check and shows the commands, runs nothing
    arguments = [argument for argument in arguments if argument != "--dry-run"]
    if len(arguments) not in (2, 3):
        die("usage: python infra/scripts/pilot.py <id> <model> [effort] [--dry-run]")
    run_id, model = arguments[0], arguments[1]
    effort = arguments[2] if len(arguments) == 3 else "medium"
    run_folder = PILOTS / run_id
    workspace = run_folder / "workspace"

    check_before_running(run_folder, model)
    if dry_run:
        print("checks passed; it would run:")
        for command in (agent_docker_command(run_folder, workspace, model, effort), build_docker_command(workspace)):
            print("  " + " ".join(part if " " not in part else "<bash script>" for part in command))
        return
    workspace.mkdir(parents=True)
    shutil.copyfile(PROMPT, run_folder / "prompt.md")
    prompt_sha256 = hashlib.sha256((run_folder / "prompt.md").read_bytes()).hexdigest()

    print(f"\033[36m=== {run_id}  {model}  effort {effort} ===\033[0m", flush=True)
    start, started_at = datetime.now().astimezone(), time.monotonic()
    run_exit_code = run_agent(run_folder, workspace, model, effort)
    end, seconds = datetime.now().astimezone(), int(time.monotonic() - started_at)
    print(f"run finished: exit code {run_exit_code} in {seconds}s", flush=True)

    build_exit_code = run_build(run_folder, workspace)
    print(f"build: exit code {build_exit_code}", flush=True)

    meta = summarize(run_folder, {
        "id": run_id, "requested_model": model, "effort": effort,
        "start": start.isoformat(timespec="seconds"), "end": end.isoformat(timespec="seconds"), "seconds": seconds,
        "run_exit_code": run_exit_code, "build_exit_code": build_exit_code, "prompt_sha256": prompt_sha256,
    })
    (run_folder / "meta.json").write_text(json.dumps(meta, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps({key: meta[key] for key in ("seconds", "turns", "cost_usd", "build_exit_code", "final_answer")},
                     ensure_ascii=False))


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8")  # type: ignore[union-attr]
    main()
