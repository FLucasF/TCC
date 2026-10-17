"""The construction's correction, run like the maintenance's evaluator: for each execution and each dimension, a
blind work folder (a neutral code instead of the run's name), Claude Code inside the experiment's image with the
rubric as its prompt, and the written correction saved. The script judges nothing: it only prepares the folder,
starts the AI and keeps its answer. For the behavior dimension, a clean PostgreSQL 18 runs alongside, reachable
through DB_URL, DB_USER and DB_PASSWORD, as the statement says.

Usage (from construction-v6):
    python evaluation/judge/judge_with_agent.py <batch> <model> [effort] [--dimensions a,b] [--runs r1,r2]
                                                [--workers N] [--dry-run]
    python evaluation/judge/judge_with_agent.py pilots claude-opus-5-5 high --dimensions behavior --runs OPUS55-N0-03

<batch> is a folder under runs/ (pilots, or a batch of the study), or `planted`: the service with planted defects
(evaluation/judge/planted/), whose answers are known. The dimensions are architecture, tests,
database, behavior, patterns-strategy, patterns-adapter, patterns-state and patterns-factory (all of them when
--dimensions is left out). Output in analysis/<batch>/judge/<model>-<effort>/: one folder per code and dimension
(the prompt, the transcript and answer.md), key.json (code to run, written outside the containers) and summary.md
(the conclusion lines of every correction).
"""

import json
import random
import shutil
import subprocess
import sys
import tempfile
from concurrent.futures import ThreadPoolExecutor
from dataclasses import dataclass
from pathlib import Path
from typing import Any

V6 = Path(__file__).resolve().parents[2]
RUBRIC = V6 / "evaluation" / "judge" / "rubric"
ENV_FILE = V6.parent / "construction-v5" / ".env"        # the same subscription token as the runs (never printed)
IMAGE = "experimento-harness:v5"
DATABASE_IMAGE = "postgres:18.6-alpine@sha256:77f585114c32fbca283dc835b0596f4e52b51b4c6662d7810b2f4084f60a1873"
DATABASE_NAME, DATABASE_USER, DATABASE_PASSWORD = "shop", "postgres", "judge"   # a throwaway database, not a secret
CODE_SEED = 20261017
LINE_BREAK = "\n"

DIMENSIONS: dict[str, list[str]] = {
    "architecture": ["architecture.md"],
    "tests": ["tests.md"],
    "database": ["database.md"],
    "behavior": ["behavior.md"],
    "patterns-strategy": ["patterns/common.md", "patterns/strategy.md"],
    "patterns-adapter": ["patterns/common.md", "patterns/adapter.md"],
    "patterns-state": ["patterns/common.md", "patterns/state.md"],
    "patterns-factory": ["patterns/common.md", "patterns/factory.md"],
}

AGENT_COMMAND = """
    claude -p --model "$0" --effort "$1" --output-format stream-json --verbose \\
        --dangerously-skip-permissions --no-session-persistence < /experimento/prompt.md
"""


@dataclass(frozen=True)
class Job:
    code: str
    run_folder: Path
    dimension: str


def die(message: str) -> None:
    sys.exit(f"\033[31mERROR: {message}\033[0m")


def judge_prompt(dimension: str) -> str:
    parts = [(RUBRIC / "common.md").read_text(encoding="utf-8").strip()]
    parts += [(RUBRIC / name).read_text(encoding="utf-8").strip() for name in DIMENSIONS[dimension]]
    return (LINE_BREAK * 2 + "---" + LINE_BREAK * 2).join(parts) + LINE_BREAK


PLANTED = V6 / "evaluation" / "judge" / "planted"


def runs_root(batch: str) -> Path:
    return PLANTED / "runs" if batch == "planted" else V6 / "runs" / batch


def pit_text(batch: str, run_name: str, code: str) -> str:
    """The run's line of the batch's PIT table, with the run's name replaced by the code."""
    table = PLANTED / "pit.csv" if batch == "planted" else V6 / "analysis" / batch / "pit.csv"
    if not table.is_file():
        return "(no PIT numbers for this batch)" + LINE_BREAK
    header, *rows = table.read_text(encoding="utf-8").splitlines()
    for row in rows:
        if row.split(",", 1)[0] == run_name:
            return header + LINE_BREAK + code + "," + row.split(",", 1)[1] + LINE_BREAK
    return header + LINE_BREAK + "(no line for this execution)" + LINE_BREAK


def prepare_work_folder(job: Job, batch: str, work_folder: Path) -> None:
    """prompt.md, workspace/ (without builds and harness files), and the three materials."""
    work_folder.mkdir(parents=True)
    shutil.copyfile(job.run_folder / "prompt.md", work_folder / "prompt.md")
    shutil.copytree(job.run_folder / "workspace", work_folder / "workspace",
                    ignore=shutil.ignore_patterns("target", ".git", "CLAUDE.md", ".claude"))
    (work_folder / "architecture").mkdir()
    report = job.run_folder / "architecture" / "architecture-report.txt"
    if report.is_file():
        shutil.copyfile(report, work_folder / "architecture" / "architecture-report.txt")
    (work_folder / "pit.txt").write_text(pit_text(batch, job.run_folder.name, job.code), encoding="utf-8")
    (work_folder / "database").mkdir()
    for photo in (job.run_folder / "database" / "database-schema.json",
                  job.run_folder / "suite" / "database-schema.json"):
        if photo.is_file():
            shutil.copyfile(photo, work_folder / "database" / "database-schema.json")
            break


def start_database(name: str, network: str) -> None:
    subprocess.run(["docker", "network", "create", network], capture_output=True, check=False)
    subprocess.run(["docker", "run", "-d", "--name", name, "--network", network,
                    "-e", f"POSTGRES_PASSWORD={DATABASE_PASSWORD}", "-e", f"POSTGRES_DB={DATABASE_NAME}",
                    DATABASE_IMAGE], capture_output=True, check=True)
    for _ in range(60):
        ready = subprocess.run(["docker", "exec", name, "pg_isready", "-U", DATABASE_USER, "-d", DATABASE_NAME],
                               capture_output=True, check=False)
        if ready.returncode == 0:
            return
        subprocess.run(["docker", "exec", name, "sleep", "1"], capture_output=True, check=False)
    die(f"the database {name} did not start")


def stop_database(name: str, network: str) -> None:
    subprocess.run(["docker", "rm", "-f", name], capture_output=True, check=False)
    subprocess.run(["docker", "network", "rm", network], capture_output=True, check=False)


def final_answer(transcript: Path) -> tuple[str, float | None]:
    for line in reversed(transcript.read_text(encoding="utf-8", errors="replace").splitlines()):
        try:
            event = json.loads(line)
        except json.JSONDecodeError:
            continue
        if event.get("type") == "result":
            return str(event.get("result") or ""), event.get("total_cost_usd")
    return "", None


def conclusions(answer: str) -> list[str]:
    return [line.split(":", 1)[1].strip() for line in answer.splitlines() if line.strip().startswith("Conclusão:")]


def correct(job: Job, batch: str, output: Path, model: str, effort: str) -> dict[str, Any]:
    job_folder = output / job.code / job.dimension
    job_folder.mkdir(parents=True)
    prompt_file = job_folder / "judge-prompt.md"
    prompt_file.write_text(judge_prompt(job.dimension), encoding="utf-8", newline=LINE_BREAK)
    container = f"judge-{job.code}-{job.dimension}"
    with tempfile.TemporaryDirectory(prefix=f"judge-{job.code}-") as scratch:
        work_folder = Path(scratch) / "work"
        prepare_work_folder(job, batch, work_folder)
        command = ["docker", "run", "--rm", "--name", container, "--env-file", str(ENV_FILE),
                   "--mount", f"type=bind,source={work_folder},target=/workspace",
                   "--mount", f"type=bind,source={prompt_file},target=/experimento/prompt.md,readonly"]
        database, network = f"{container}-db", f"{container}-net"
        if job.dimension == "behavior":
            start_database(database, network)
            command += ["--network", network,
                        "-e", f"DB_URL=jdbc:postgresql://{database}:5432/{DATABASE_NAME}",
                        "-e", f"DB_USER={DATABASE_USER}", "-e", f"DB_PASSWORD={DATABASE_PASSWORD}"]
        command += [IMAGE, "bash", "-c", AGENT_COMMAND, model, effort]
        try:
            with (job_folder / "judge-output.jsonl").open("wb") as transcript, \
                    (job_folder / "stderr.txt").open("wb") as errors:
                exit_code = subprocess.run(command, stdout=transcript, stderr=errors, check=False).returncode
        finally:
            if job.dimension == "behavior":
                stop_database(database, network)
    answer, cost = final_answer(job_folder / "judge-output.jsonl")
    (job_folder / "answer.md").write_text(answer + LINE_BREAK, encoding="utf-8")
    found = conclusions(answer)
    print(f"{job.code} {job.dimension}: exit {exit_code}, {len(found)} conclusions", flush=True)
    return {"code": job.code, "dimension": job.dimension, "conclusions": found, "cost_usd": cost}


def main() -> None:
    arguments = sys.argv[1:]
    options: dict[str, str] = {}
    for flag in ("--dimensions", "--runs", "--workers"):
        if flag in arguments:
            position = arguments.index(flag)
            options[flag] = arguments[position + 1]
            del arguments[position:position + 2]
    dry_run = "--dry-run" in arguments
    arguments = [argument for argument in arguments if argument != "--dry-run"]
    if len(arguments) not in (2, 3):
        die("usage: python evaluation/judge/judge_with_agent.py <batch> <model> [effort] [--dimensions a,b] "
            "[--runs r1,r2] [--workers N] [--dry-run]")
    batch, model = arguments[0], arguments[1]
    effort = arguments[2] if len(arguments) == 3 else "high"
    dimensions = options["--dimensions"].split(",") if "--dimensions" in options else list(DIMENSIONS)
    unknown = [dimension for dimension in dimensions if dimension not in DIMENSIONS]
    if unknown:
        die(f"unknown dimensions: {unknown}")
    run_folders = sorted(folder for folder in runs_root(batch).iterdir() if (folder / "workspace").is_dir())
    if "--runs" in options:
        run_folders = [folder for folder in run_folders if folder.name in options["--runs"].split(",")]
    if not run_folders:
        die(f"no execution found in {runs_root(batch)}")
    random.Random(f"{CODE_SEED}-{batch}").shuffle(run_folders)
    codes = {folder: f"exec-{index:02d}" for index, folder in enumerate(run_folders, start=1)}
    jobs = [Job(codes[folder], folder, dimension) for folder in run_folders for dimension in dimensions]
    output = V6 / "analysis" / batch / "judge" / f"{model.removeprefix('claude-')}-{effort}"

    if dry_run:
        with tempfile.TemporaryDirectory() as scratch:
            for job in jobs:
                prepare_work_folder(job, batch, Path(scratch) / f"{job.code}-{job.dimension}")
        print(f"{len(jobs)} corrections ({len(run_folders)} executions x {len(dimensions)} dimensions); the work "
              f"folders build; output would go to {output.relative_to(V6)}")
        return
    if subprocess.run(["docker", "image", "inspect", IMAGE], capture_output=True, check=False).returncode != 0:
        die(f"image {IMAGE} not found (is Docker running?)")
    output.mkdir(parents=True, exist_ok=True)
    key_file = output / "key.json"
    key: dict[str, str] = json.loads(key_file.read_text(encoding="utf-8")) if key_file.is_file() else {}
    key.update({codes[folder]: folder.name for folder in run_folders})
    key_file.write_text(json.dumps(key, indent=2) + LINE_BREAK, encoding="utf-8")
    for job in jobs:
        if (output / job.code / job.dimension).exists():
            die(f"{job.code}/{job.dimension} was already corrected in {output.relative_to(V6)}")
    with ThreadPoolExecutor(max_workers=int(options.get("--workers", "1"))) as pool:
        results = list(pool.map(lambda job: correct(job, batch, output, model, effort), jobs))

    lines = [f"# A correção de runs/{batch}: {model}, effort {effort}", "",
             "| código | execução | dimensão | conclusões |", "|---|---|---|---|"]
    for result in sorted(results, key=lambda result: (key[result["code"]], result["dimension"])):
        lines.append(f"| {result['code']} | {key[result['code']]} | {result['dimension']} | "
                     f"{', '.join(result['conclusions']) or '(nenhuma)'} |")
    costs = [result["cost_usd"] for result in results if result["cost_usd"] is not None]
    lines += ["", f"Custo: US$ {sum(costs):.2f}."]
    summary = output / f"summary-{'-'.join(dimensions) if len(dimensions) < 3 else 'all'}.md"
    summary.write_text(LINE_BREAK.join(lines) + LINE_BREAK, encoding="utf-8")
    print(LINE_BREAK.join(lines))


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8")  # type: ignore[union-attr]
    main()
