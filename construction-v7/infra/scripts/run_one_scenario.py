"""Runs ONE execution of the construction study: an empty workspace with the arm's harness, the prompt of one scenario
(the inn's system described in full, with the scenario's part at the end), and Claude Code inside the experiment's
image, the same way as maintenance-v1's run_one_scenario.py.

Usage (from construction-v7):
    python infra/scripts/run_one_scenario.py <scenario> <arm> <model> [effort] [--dry-run]
    python infra/scripts/run_one_scenario.py 01 N0 claude-sonnet-5-5 medium

<scenario> is 01, 03, 05, 07 or 09 (the files in experiment/scenarios/; the five that ask for a pattern), <arm> is N0, N1 or N2 (experiment/harnesses/),
<model> is the full model ID. --dry-run does every check and shows the Docker commands without running anything.

The prompt is experiment/scenarios/frame.md with the scenario in {{PEDIDO}}; three passages of the frame
({{IDENTIFICACAO}}, {{CAMPOS_IDENTIFICACAO}}, {{EMAIL_CONFIRMACAO}}) come from defaults/, unless the scenario has its
own (<scenario-file-stem>.<PASSAGE>.md), because scenario 05 changes what the frame says there.

Output in runs/<scenario>-<pattern>/<arm>-<model>/ (for example runs/01-strategy/N0-sonnet-5-5/):
    workspace/          the project as the agent left it (the build's target/ is not versioned)
    agent-git.bundle    only when the agent ran `git init`: its history (the .git folder leaves the workspace)
    prompt.md           the exact text the agent received
    claude-output.jsonl the transcript; stderr.txt; build.txt (mvn verify, in a container without the token)
    meta.json           the run's identity (scenarios, harness and prompt hashes, model, effort) and its summary

Isolation: the agent's container sees only its workspace and the prompt, never the answer key, the reference
solutions or other runs. The subscription token is read by Docker from construction-v5's .env (--env-file); this
script only checks which variables the file sets and never reads a value.
"""

import hashlib
import json
import re
import shutil
import subprocess
import sys
import time
from collections.abc import Callable
from datetime import datetime
from pathlib import Path
from typing import Any

CONSTRUCTION_ROOT = Path(__file__).resolve().parents[2]
REPOSITORY_ROOT = CONSTRUCTION_ROOT.parent
SCENARIOS = CONSTRUCTION_ROOT / "experiment" / "scenarios"
FRAME = SCENARIOS / "frame.md"
HARNESSES = CONSTRUCTION_ROOT / "experiment" / "harnesses"
RUNS = CONSTRUCTION_ROOT / "runs"
ENV_FILE = REPOSITORY_ROOT / "construction-v5" / ".env"   # the same subscription token as the other studies (never printed)
IMAGE = "experimento-harness:v5"
ARMS = ("N0", "N1", "N2")
PASSAGES = ("IDENTIFICACAO", "CAMPOS_IDENTIFICACAO", "EMAIL_CONFIRMACAO")
SKILL_NAME = "gof-patterns"

# the scenario number, and the label of its run folder: the pattern it asks for (the negative controls of
# maintenance-v1, the even scenarios, were left out of construction-v7 on 2026-10-18)
SCENARIO_LABELS = {"01": "strategy", "03": "observer", "05": "factory-method", "07": "decorator",
                   "09": "template-method"}

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
FORBIDDEN_IN_ENV = re.compile(r"^(ANTHROPIC_API_KEY|ANTHROPIC_AUTH_TOKEN|ANTHROPIC_BASE_URL|ANTHROPIC_MODEL)=",
                              re.MULTILINE)


def die(message: str) -> None:
    sys.exit(f"\033[31mERROR: {message}\033[0m")


def git_output(arguments: list[str]) -> str:
    return subprocess.run(["git", *arguments], cwd=REPOSITORY_ROOT, capture_output=True, text=True,
                          encoding="utf-8", check=False).stdout.strip()


def committed_tree_hash(folder: Path) -> str:
    """The folder's tree hash in HEAD. The folder must have no uncommitted change: the hash is what says, later,
    what the run received."""
    relative = folder.relative_to(REPOSITORY_ROOT).as_posix()
    if git_output(["status", "--porcelain", "--", relative]):
        die(f"{relative} has uncommitted changes: commit it before running")
    tree_hash = git_output(["rev-parse", f"HEAD:{relative}"])
    if not tree_hash:
        die(f"{relative} is not in the last commit")
    return tree_hash


def model_short_name(model: str) -> str:
    return model.removeprefix("claude-")


def scenario_file(scenario: str) -> Path:
    matches = [path for path in SCENARIOS.glob(f"{scenario}-*.md") if "." not in path.stem]
    if len(matches) != 1:
        die(f"expected one file for scenario {scenario} in {SCENARIOS}, found {len(matches)}")
    return matches[0]


def prompt_text(scenario: str) -> str:
    """The frame, with the scenario's passages (its own, or the defaults) and the scenario at the end."""
    scenario_path = scenario_file(scenario)
    text = FRAME.read_text(encoding="utf-8")
    for passage in PASSAGES:
        own = SCENARIOS / f"{scenario_path.stem}.{passage}.md"
        source = own if own.is_file() else SCENARIOS / "defaults" / f"{passage}.md"
        text = text.replace("{{" + passage + "}}", source.read_text(encoding="utf-8").strip())
    text = text.replace("{{PEDIDO}}", scenario_path.read_text(encoding="utf-8").strip())
    left = re.findall(r"\{\{\w+\}\}", text)
    if left:
        die(f"the prompt of scenario {scenario} still has {left}")
    return text


def check_before_running(scenario: str, arm: str, model: str, run_folder: Path) -> None:
    if scenario not in SCENARIO_LABELS:
        die(f"unknown scenario '{scenario}': use one of {', '.join(SCENARIO_LABELS)}")
    if arm not in ARMS:
        die(f"unknown arm '{arm}': use one of {', '.join(ARMS)}")
    if not re.fullmatch(r"claude-[\w.-]+-[\w.-]+", model):
        die(f"use the full model ID: '{model}'")
    if run_folder.exists():
        die(f"{run_folder} already exists: runs are never reused")
    if not ENV_FILE.is_file():
        die(f"{ENV_FILE} not found")
    # the .env is only checked for which variables it sets; no value is kept or printed
    env_text = ENV_FILE.read_text(encoding="utf-8")
    if not re.search(r"^CLAUDE_CODE_OAUTH_TOKEN=.+", env_text, re.MULTILINE):
        die(".env has no CLAUDE_CODE_OAUTH_TOKEN")
    if FORBIDDEN_IN_ENV.search(env_text):
        die(".env has a variable that changes the billing, the provider or the model")
    if subprocess.run(["docker", "image", "inspect", IMAGE], capture_output=True, check=False).returncode != 0:
        die(f"image {IMAGE} not found (is Docker running?)")


def prepare_workspace(arm: str, workspace: Path) -> None:
    """An empty workspace, with the arm's harness files at its root (N0 gets nothing)."""
    workspace.mkdir(parents=True)
    if arm != "N0":
        shutil.copytree(HARNESSES / arm, workspace, dirs_exist_ok=True)


def agent_docker_command(run_name: str, run_folder: Path, workspace: Path, model: str, effort: str) -> list[str]:
    return [
        "docker", "run", "--rm", "--name", f"construction-v7-{run_name}",
        "--env-file", str(ENV_FILE),
        "--mount", f"type=bind,source={workspace},target=/workspace",
        "--mount", f"type=bind,source={run_folder / 'prompt.md'},target=/experimento/prompt.md,readonly",
        IMAGE, "bash", "-c", AGENT_COMMAND, model, effort,
    ]


def build_docker_command(workspace: Path) -> list[str]:
    return ["docker", "run", "--rm", "--mount", f"type=bind,source={workspace},target=/workspace",
            IMAGE, "bash", "-c", BUILD_COMMAND]


def run_logged(command: list[str], output_file: Path, errors_file: Path | None = None) -> int:
    with output_file.open("wb") as output:
        if errors_file is None:
            return subprocess.run(command, stdout=output, stderr=subprocess.STDOUT, check=False).returncode
        with errors_file.open("wb") as errors:
            return subprocess.run(command, stdout=output, stderr=errors, check=False).returncode


def move_agent_git_out(run_folder: Path) -> bool:
    """When the agent ran `git init` in its workspace, its history goes to <run>/agent-git.bundle and the .git folder
    leaves the workspace, so the study's repository versions the project's files (and not a nested repository).
    Returns whether the agent had a git of its own."""
    agent_git = run_folder / "workspace" / ".git"
    if not agent_git.is_dir():
        return False
    bundle = run_folder / "agent-git.bundle"
    bundled = subprocess.run(["git", "-C", str(run_folder / "workspace"), "bundle", "create", str(bundle), "--all"],
                             capture_output=True, check=False)
    if bundled.returncode != 0 or not bundle.is_file():
        die(f"could not bundle the agent's git in {agent_git}: {bundled.stderr.decode(errors='replace')}")
    shutil.rmtree(agent_git, onexc=remove_read_only)
    return True


def remove_read_only(function: Callable[[str], object], path: str, _error: BaseException) -> None:
    """Git keeps its object files read-only, and Windows refuses to delete those: make them writable, then retry."""
    Path(path).chmod(0o700)
    function(path)


def summarize(run_folder: Path, details: dict[str, Any]) -> dict[str, Any]:
    """A summary read from the transcript, including whether the agent ever opened the skill."""
    result: dict[str, Any] = {}
    answering_models: set[str] = set()
    tools_used: dict[str, int] = {}
    skill_reads: list[str] = []
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
                if content.get("type") != "tool_use":
                    continue
                tools_used[content["name"]] = tools_used.get(content["name"], 0) + 1
                tool_input = json.dumps(content.get("input", {}), ensure_ascii=False)
                if content["name"] == "Skill" or SKILL_NAME in tool_input:
                    skill_reads.append(f"{content['name']}: {tool_input[:160]}")
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
        "skill_opened": bool(skill_reads),
        "skill_reads": skill_reads,
        "final_answer": (result.get("result") or "")[:200],
    }


def main() -> None:
    arguments = sys.argv[1:]
    dry_run = "--dry-run" in arguments
    arguments = [argument for argument in arguments if argument != "--dry-run"]
    if len(arguments) not in (3, 4):
        die("usage: python infra/scripts/run_one_scenario.py <scenario> <arm> <model> [effort] [--dry-run]")
    scenario, arm, model = arguments[0], arguments[1], arguments[2]
    effort = arguments[3] if len(arguments) == 4 else "medium"
    run_name = f"{arm}-{model_short_name(model)}"
    run_folder = RUNS / f"{scenario}-{SCENARIO_LABELS.get(scenario, 'unknown')}" / run_name
    workspace = run_folder / "workspace"

    check_before_running(scenario, arm, model, run_folder)
    prompt = prompt_text(scenario)
    identity = {
        "scenario": scenario, "scenario_file": scenario_file(scenario).name, "arm": arm,
        "requested_model": model, "effort": effort, "image": IMAGE,
        "scenarios_tree": committed_tree_hash(SCENARIOS),
        "harness_tree": committed_tree_hash(HARNESSES / arm) if arm != "N0" else None,
        "prompt_sha256": hashlib.sha256(prompt.encode("utf-8")).hexdigest(),
    }
    if dry_run:
        print("checks passed; it would run", json.dumps(identity, ensure_ascii=False))
        print(f"  into {run_folder.relative_to(CONSTRUCTION_ROOT)} ({len(prompt.split())} words of prompt)")
        for command in (agent_docker_command(f"{scenario}-{run_name}", run_folder, workspace, model, effort),
                        build_docker_command(workspace)):
            print("  " + " ".join(part if " " not in part else "<bash script>" for part in command))
        return

    prepare_workspace(arm, workspace)
    (run_folder / "prompt.md").write_text(prompt, encoding="utf-8", newline="\n")

    print(f"\033[36m=== {scenario} {arm} {model} effort {effort} ===\033[0m", flush=True)
    start, started_at = datetime.now().astimezone(), time.monotonic()
    run_exit_code = run_logged(agent_docker_command(f"{scenario}-{run_name}", run_folder, workspace, model, effort),
                               run_folder / "claude-output.jsonl", run_folder / "stderr.txt")
    end, seconds = datetime.now().astimezone(), int(time.monotonic() - started_at)
    print(f"run finished: exit code {run_exit_code} in {seconds}s", flush=True)

    build_exit_code = run_logged(build_docker_command(workspace), run_folder / "build.txt")
    print(f"build: exit code {build_exit_code}", flush=True)

    meta = summarize(run_folder, {
        **identity, "start": start.isoformat(timespec="seconds"), "end": end.isoformat(timespec="seconds"),
        "seconds": seconds, "run_exit_code": run_exit_code, "build_exit_code": build_exit_code,
        "agent_git_bundled": move_agent_git_out(run_folder),
    })
    (run_folder / "meta.json").write_text(json.dumps(meta, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({key: meta[key] for key in ("seconds", "turns", "cost_usd", "build_exit_code", "skill_opened",
                                                 "final_answer")}, ensure_ascii=False))


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8")  # type: ignore[union-attr]
    main()
