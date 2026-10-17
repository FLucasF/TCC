"""Runs ONE execution of the construction study: an empty workspace with the arm's harness, the prompt, and Claude
Code inside the experiment's image, the same way as the maintenance study's run_one_scenario.py.

Usage (from construction-v6):
    python infra/scripts/run_one_execution.py <batch> <arm> <model> [effort] [--dry-run]
    python infra/scripts/run_one_execution.py pilots-2 N0 claude-haiku-4-5 medium

<batch> is the folder under runs/ (one per batch, so the judge can correct them together), <arm> is N0, N1 or N2
(N1 and N2 read experiment/harnesses/<arm>/, which must be committed), <model> is the full model ID. --dry-run does
every check and shows the Docker commands without running anything.

Output in runs/<batch>/<arm>-<model>/ (for example runs/pilots-2/N0-haiku-4-5/):
    workspace/          the project as the agent left it, with the harness files it received
    prompt.md           a copy of experiment/prompt/prompt.md, the exact text the agent received
    claude-output.jsonl the transcript; stderr.txt; build.txt (mvn verify, in a container without the token)
    meta.json           the run's identity (prompt and harness hashes, model, effort) and its summary

The material for the correction comes after the batch, from the measures (README of evaluation/measures/):
    python evaluation/measures/measure_test_quality.py runs/<batch> analysis/<batch>/pit.csv
    python evaluation/measures/report_architecture.py runs/<batch>
    python evaluation/measures/photograph_database.py runs/<batch>/<run>/workspace runs/<batch>/<run>/database

Isolation: the agent's container sees only its workspace and the prompt, never the reference, the judge's rubric or
other runs. The subscription token is read by Docker from construction-v5's .env (--env-file); this script only
checks which variables the file sets and never reads a value.
"""

import hashlib
import json
import shutil
import subprocess
import sys
import time
from datetime import datetime
from pathlib import Path
from typing import Any

sys.path.insert(0, str(Path(__file__).resolve().parent))

from run_pilot_without_harness import (  # noqa: E402  (the same image, commands and checks as the pilots)
    PROMPT,
    V6,
    build_docker_command,
    check_before_running,
    die,
    summarize,
)
from run_pilot_without_harness import agent_docker_command as pilot_agent_docker_command  # noqa: E402

REPOSITORY_ROOT = V6.parent
HARNESSES = V6 / "experiment" / "harnesses"
RUNS = V6 / "runs"
ARMS = ("N0", "N1", "N2")
SKILL_NAME = "gof-patterns"


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


def prepare_workspace(arm: str, workspace: Path) -> None:
    """An empty workspace, with the arm's harness files at its root (N0 gets nothing)."""
    workspace.mkdir(parents=True)
    if arm != "N0":
        shutil.copytree(HARNESSES / arm, workspace, dirs_exist_ok=True)


def agent_docker_command(container_name: str, run_folder: Path, workspace: Path, model: str,
                         effort: str) -> list[str]:
    command = pilot_agent_docker_command(run_folder, workspace, model, effort)
    command[command.index("--name") + 1] = container_name
    return command


def skill_reads(run_folder: Path) -> list[str]:
    """Every tool call that opened the skill (the Skill tool, or a read of its folder)."""
    reads: list[str] = []
    transcript = (run_folder / "claude-output.jsonl").read_text(encoding="utf-8", errors="replace")
    for line in transcript.splitlines():
        try:
            event = json.loads(line)
        except json.JSONDecodeError:
            continue
        if event.get("type") != "assistant":
            continue
        for content in event.get("message", {}).get("content", []):
            if content.get("type") != "tool_use":
                continue
            tool_input = json.dumps(content.get("input", {}), ensure_ascii=False)
            if content["name"] == "Skill" or SKILL_NAME in tool_input:
                reads.append(f"{content['name']}: {tool_input[:160]}")
    return reads


def main() -> None:
    arguments = sys.argv[1:]
    dry_run = "--dry-run" in arguments
    arguments = [argument for argument in arguments if argument != "--dry-run"]
    if len(arguments) not in (3, 4):
        die("usage: python infra/scripts/run_one_execution.py <batch> <arm> <model> [effort] [--dry-run]")
    batch, arm, model = arguments[0], arguments[1], arguments[2]
    effort = arguments[3] if len(arguments) == 4 else "medium"
    if arm not in ARMS:
        die(f"unknown arm '{arm}': use one of {', '.join(ARMS)}")
    if batch in ("pilots", "planted") or not batch.replace("-", "").isalnum():
        die(f"use a new batch name (letters, digits and hyphens; not 'pilots' or 'planted'): '{batch}'")
    run_name = f"{arm}-{model_short_name(model)}"
    run_folder = RUNS / batch / run_name
    workspace = run_folder / "workspace"
    container_name = f"construction-{batch}-{run_name}"

    check_before_running(run_folder, model)
    if arm != "N0" and not (HARNESSES / arm).is_dir():
        die(f"{HARNESSES / arm} not found: the arm's harness must exist before running it")
    identity: dict[str, Any] = {
        "batch": batch, "arm": arm, "requested_model": model, "effort": effort,
        "harness_tree": committed_tree_hash(HARNESSES / arm) if arm != "N0" else None,
    }
    if dry_run:
        print("checks passed; it would run", json.dumps(identity, ensure_ascii=False))
        print(f"  into {run_folder.relative_to(V6)}")
        for command in (agent_docker_command(container_name, run_folder, workspace, model, effort),
                        build_docker_command(workspace)):
            print("  " + " ".join(part if " " not in part else "<bash script>" for part in command))
        return

    prepare_workspace(arm, workspace)
    shutil.copyfile(PROMPT, run_folder / "prompt.md")
    identity["prompt_sha256"] = hashlib.sha256((run_folder / "prompt.md").read_bytes()).hexdigest()

    print(f"\033[36m=== {batch} {arm} {model} effort {effort} ===\033[0m", flush=True)
    start, started_at = datetime.now().astimezone(), time.monotonic()
    with (run_folder / "claude-output.jsonl").open("wb") as output, (run_folder / "stderr.txt").open("wb") as errors:
        run_exit_code = subprocess.run(agent_docker_command(container_name, run_folder, workspace, model, effort),
                                       stdout=output, stderr=errors, check=False).returncode
    end, seconds = datetime.now().astimezone(), int(time.monotonic() - started_at)
    print(f"run finished: exit code {run_exit_code} in {seconds}s", flush=True)

    with (run_folder / "build.txt").open("wb") as output:
        build_exit_code = subprocess.run(build_docker_command(workspace), stdout=output, stderr=subprocess.STDOUT,
                                         check=False).returncode
    print(f"build: exit code {build_exit_code}", flush=True)

    reads = skill_reads(run_folder)
    meta = summarize(run_folder, {
        **identity, "start": start.isoformat(timespec="seconds"), "end": end.isoformat(timespec="seconds"),
        "seconds": seconds, "run_exit_code": run_exit_code, "build_exit_code": build_exit_code,
        "skill_opened": bool(reads), "skill_reads": reads,
    })
    (run_folder / "meta.json").write_text(json.dumps(meta, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({key: meta[key] for key in ("seconds", "turns", "cost_usd", "build_exit_code", "skill_opened",
                                                 "final_answer")}, ensure_ascii=False))


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8")  # type: ignore[union-attr]
    main()
