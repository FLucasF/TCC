"""The evaluator agent of the construction study: Claude Code, inside the experiment's image, reads one project built
from zero (the prompt and the project) and answers with written feedback and one conclusion line per item (the
scenario's class, the collateral overengineering, the behavior), the same way as maintenance-v1's evaluator. The
prompt is common.md followed by the pattern's prompt.md. The evaluator's container sees only its work folder: never
the answer key, the reference solutions' names, the run's name or the model that built the project.

Usage (from construction-v7):
    python evaluation/evaluator/evaluate_with_agent.py calibrate <model> [effort] [--workers N] [--dry-run]
    python evaluation/evaluator/evaluate_with_agent.py calibrate claude-opus-5-5 high --workers 3
    python evaluation/evaluator/evaluate_with_agent.py evaluate-runs <scenario> <arm> <model> [effort] [--workers N]
    python evaluation/evaluator/evaluate_with_agent.py evaluate-runs 01 N0 claude-opus-5-5 high --workers 3

`calibrate` evaluates the 10 reference solutions of the five scenarios that ask for a pattern, whose class is known,
under codes drawn at random (cal-01 to cal-10). A reference solution is a complete project: maintenance-v1's base (the
inn's system without any pattern) with one of maintenance-v1's reference patches applied
(evaluation/solutions/NN-<kind>.patch there), which is exactly a project built from zero with the scenario done with
the pattern ("pattern", expected acerto) or without it ("naive", omissão). The negative controls (the even scenarios)
were left out of construction-v7 on 2026-10-18. Output in each pattern's folder, in calibration/<model>-<effort>/: one
folder per code, key.json and summary.md; and the whole picture in calibration-<model>-<effort>.md here.

`evaluate-runs` evaluates every run of one scenario and arm (runs/<scenario>-<label>/<arm>-*/), blind, under codes
drawn at random (run-01, run-02...). The evaluator gets the run's prompt and the run's project (without target/, .git
and the harness files). Output in the pattern's folder, in runs/<scenario>-<arm>/<model>-<effort>/.
"""

import json
import random
import re
import shutil
import subprocess
import sys
import tempfile
from concurrent.futures import ThreadPoolExecutor
from dataclasses import dataclass
from pathlib import Path
from typing import Any

CONSTRUCTION_ROOT = Path(__file__).resolve().parents[2]
REPOSITORY_ROOT = CONSTRUCTION_ROOT.parent
RUNS = CONSTRUCTION_ROOT / "runs"
EVALUATOR = CONSTRUCTION_ROOT / "evaluation" / "evaluator"
MAINTENANCE = REPOSITORY_ROOT / "maintenance-v1"
BASE = MAINTENANCE / "experiment" / "base" / "hotel"
SOLUTIONS = MAINTENANCE / "evaluation" / "solutions"
ENV_FILE = REPOSITORY_ROOT / "construction-v5" / ".env"   # the same subscription token as the runs (never printed)
IMAGE = "experimento-harness:v5"
CODE_SEED = 20261018
LINE_BREAK = "\n"

sys.path.insert(0, str(CONSTRUCTION_ROOT / "infra" / "scripts"))
from run_one_scenario import prompt_text  # noqa: E402  (the exact text a run of each scenario receives)

PATTERN_BY_SCENARIO = {"01": "strategy", "03": "observer", "05": "factory-method", "07": "decorator",
                       "09": "template-method"}
PATTERNS = sorted(set(PATTERN_BY_SCENARIO.values()))
EXPECTED_CLASS_BY_KIND = {"pattern": "acerto", "naive": "omissao"}

EVALUATOR_COMMAND = """
    claude -p --model "$0" --effort "$1" --output-format stream-json --verbose \\
        --dangerously-skip-permissions --no-session-persistence < /experimento/prompt.md
"""


@dataclass(frozen=True)
class Case:
    code: str
    scenario: str
    solution: Path

    @property
    def pattern(self) -> str:
        return PATTERN_BY_SCENARIO[self.scenario]

    @property
    def expected_class(self) -> str:
        return EXPECTED_CLASS_BY_KIND[self.solution.stem.split("-", 1)[1]]


@dataclass(frozen=True)
class RunCase:
    code: str
    scenario: str
    run_folder: Path

    @property
    def pattern(self) -> str:
        return PATTERN_BY_SCENARIO[self.scenario]


def die(message: str) -> None:
    sys.exit(f"\033[31mERROR: {message}\033[0m")


def evaluator_prompt(scenario: str) -> str:
    common = (EVALUATOR / "common.md").read_text(encoding="utf-8").strip()
    pattern = (EVALUATOR / PATTERN_BY_SCENARIO[scenario] / "prompt.md").read_text(encoding="utf-8").strip()
    return common + LINE_BREAK * 2 + "---" + LINE_BREAK * 2 + pattern + LINE_BREAK


def build_reference_project(case: Case, project: Path) -> None:
    """The base with the reference patch applied."""
    shutil.copytree(BASE, project, ignore=shutil.ignore_patterns("target"))
    applied = subprocess.run(["git", "apply", "--whitespace=nowarn", str(case.solution)], cwd=project,
                             capture_output=True, text=True, check=False)
    if applied.returncode != 0:
        die(f"{case.solution.name} does not apply to the base: {applied.stderr}")


def prepare_work_folder(case: Case | RunCase, work_folder: Path) -> None:
    """enunciado.md and projeto/ (the run's project, or the reference solution's)."""
    work_folder.mkdir(parents=True)
    if isinstance(case, RunCase):
        shutil.copyfile(case.run_folder / "prompt.md", work_folder / "enunciado.md")
        shutil.copytree(case.run_folder / "workspace", work_folder / "projeto",
                        ignore=shutil.ignore_patterns("target", ".git", "CLAUDE.md", ".claude"))
        return
    (work_folder / "enunciado.md").write_text(prompt_text(case.scenario), encoding="utf-8", newline=LINE_BREAK)
    build_reference_project(case, work_folder / "projeto")


def evaluator_docker_command(case: Case | RunCase, work_folder: Path, prompt_file: Path, model: str,
                             effort: str) -> list[str]:
    return [
        "docker", "run", "--rm", "--name", f"evaluator-v7-{case.scenario}-{case.code}",
        "--env-file", str(ENV_FILE),
        "--mount", f"type=bind,source={work_folder},target=/workspace",
        "--mount", f"type=bind,source={prompt_file},target=/experimento/prompt.md,readonly",
        IMAGE, "bash", "-c", EVALUATOR_COMMAND, model, effort,
    ]


def final_answer(transcript: Path) -> tuple[str, float | None]:
    """The text of Claude Code's "result" event, and its cost."""
    for line in reversed(transcript.read_text(encoding="utf-8", errors="replace").splitlines()):
        try:
            event = json.loads(line)
        except json.JSONDecodeError:
            continue
        if event.get("type") == "result":
            return str(event.get("result") or ""), event.get("total_cost_usd")
    return "", None


CLASS_CODE_BY_WORD = {"acerto": "acerto", "alternativa equivalente": "alternativa_equivalente",
                      "malfeito": "malfeito", "omissão": "omissao", "contenção": "contencao", "exagero": "exagero"}


def read_answer(case_folder: Path) -> tuple[str | None, float | None]:
    """Reads the evaluator's transcript and returns the class and the cost. answer.md keeps the text, answer.json the
    three conclusions (the scenario's class, the collateral overengineering, the behavior)."""
    text, cost = final_answer(case_folder / "evaluator-output.jsonl")
    found = [line.split(":", 1)[1].strip().lower() for line in text.splitlines()
             if line.strip().startswith("Conclusão:")]
    answer: dict[str, Any] = {"unparsed": text}
    if found:
        (case_folder / "answer.md").write_text(text + LINE_BREAK, encoding="utf-8")
        answer = {
            "classe": CLASS_CODE_BY_WORD.get(found[0]),
            "exagero_colateral": found[1] if len(found) > 1 else None,
            "comportamento": found[2] if len(found) > 2 else None,
            "conclusoes": found,
        }
    (case_folder / "answer.json").write_text(json.dumps(answer, ensure_ascii=False, indent=2) + LINE_BREAK,
                                             encoding="utf-8")
    given = answer.get("classe")
    return (given if isinstance(given, str) else None), cost


def calibration_folder(pattern: str, model: str, effort: str) -> Path:
    return EVALUATOR / pattern / "calibration" / f"{model.removeprefix('claude-')}-{effort}"


def evaluate(case: Case | RunCase, case_folder: Path, model: str, effort: str) -> dict[str, Any]:
    case_folder.mkdir(parents=True)
    prompt_file = case_folder / "evaluator-prompt.md"
    prompt_file.write_text(evaluator_prompt(case.scenario), encoding="utf-8", newline=LINE_BREAK)
    with tempfile.TemporaryDirectory(prefix=f"evaluation-{case.code}-") as scratch:
        work_folder = Path(scratch) / "work"
        prepare_work_folder(case, work_folder)
        command = evaluator_docker_command(case, work_folder, prompt_file, model, effort)
        with (case_folder / "evaluator-output.jsonl").open("wb") as output, \
                (case_folder / "stderr.txt").open("wb") as errors:
            exit_code = subprocess.run(command, stdout=output, stderr=errors, check=False).returncode
    given, cost = read_answer(case_folder)
    print(f"{case.code} ({case.pattern}): exit {exit_code}, class {given}", flush=True)
    return {"code": case.code, "exit_code": exit_code, "class": given, "cost_usd": cost}


def calibration_cases() -> list[Case]:
    patches = sorted(patch for patch in SOLUTIONS.glob("[0-9][0-9]-*.patch") if patch.name[:2] in PATTERN_BY_SCENARIO)
    if len(patches) != 2 * len(PATTERN_BY_SCENARIO):
        die(f"expected {2 * len(PATTERN_BY_SCENARIO)} reference solutions in {SOLUTIONS}, found {len(patches)}")
    random.Random(CODE_SEED).shuffle(patches)
    return [Case(f"cal-{index:02d}", patch.name[:2], patch) for index, patch in enumerate(patches, start=1)]


def summary_table(cases: list[Case], results_by_code: dict[str, dict[str, Any]]) -> tuple[list[str], int]:
    lines = ["| código | solução | esperado | avaliador | confere |", "|---|---|---|---|---|"]
    right = 0
    for case in sorted(cases, key=lambda case: case.solution.name):
        given = results_by_code[case.code]["class"]
        right += given == case.expected_class
        lines.append(f"| {case.code} | {case.solution.stem} | {case.expected_class} | {given} | "
                     f"{'sim' if given == case.expected_class else 'NÃO'} |")
    return lines, right


def write_summaries(cases: list[Case], results: list[dict[str, Any]], model: str, effort: str) -> None:
    results_by_code = {result["code"]: result for result in results}
    for pattern in PATTERNS:
        pattern_cases = [case for case in cases if case.pattern == pattern]
        table, right = summary_table(pattern_cases, results_by_code)
        lines = [f"# Calibração do avaliador de {pattern}: {model}, effort {effort}", "", *table, "",
                 f"**{right} de {len(pattern_cases)}** na classe esperada."]
        (calibration_folder(pattern, model, effort) / "summary.md").write_text(
            LINE_BREAK.join(lines) + LINE_BREAK, encoding="utf-8")
    table, right = summary_table(cases, results_by_code)
    costs = [result["cost_usd"] for result in results if result["cost_usd"] is not None]
    lines = [f"# Calibração do avaliador: {model}, effort {effort}", "", *table, "",
             f"**{right} de {len(cases)}** na classe esperada. Custo: US$ {sum(costs):.2f}."]
    text = LINE_BREAK.join(lines) + LINE_BREAK
    (EVALUATOR / f"calibration-{model.removeprefix('claude-')}-{effort}.md").write_text(text, encoding="utf-8")
    print(text)


def check_can_run(model: str) -> None:
    if not re.fullmatch(r"claude-[\w.-]+-[\w.-]+", model):
        die(f"use the full model ID: '{model}'")
    if subprocess.run(["docker", "image", "inspect", IMAGE], capture_output=True, check=False).returncode != 0:
        die(f"image {IMAGE} not found (is Docker running?)")
    if not ENV_FILE.is_file():
        die(f"{ENV_FILE} not found")


def calibrate(model: str, effort: str, workers: int, dry_run: bool) -> None:
    cases = calibration_cases()
    if dry_run:
        with tempfile.TemporaryDirectory() as scratch:
            for case in cases:
                prepare_work_folder(case, Path(scratch) / case.code)
        for pattern in PATTERNS:
            codes = ", ".join(case.code for case in cases if case.pattern == pattern)
            print(f"{pattern}: {codes} -> {calibration_folder(pattern, model, effort).relative_to(CONSTRUCTION_ROOT)}")
        print(f"the {len(cases)} work folders build")
        return
    check_can_run(model)
    for pattern in PATTERNS:
        folder = calibration_folder(pattern, model, effort)
        if folder.exists():
            die(f"{folder} already exists: calibrations are never reused")
        folder.mkdir(parents=True)
        key = {case.code: case.solution.name for case in cases if case.pattern == pattern}
        (folder / "key.json").write_text(json.dumps(key, indent=2) + LINE_BREAK, encoding="utf-8")
    with ThreadPoolExecutor(max_workers=workers) as pool:
        results = list(pool.map(
            lambda case: evaluate(case, calibration_folder(case.pattern, model, effort) / case.code, model, effort),
            cases))
    write_summaries(cases, results, model, effort)


def run_cases(scenario: str, arm: str) -> list[RunCase]:
    scenario_folders = sorted(RUNS.glob(f"{scenario}-*"))
    if len(scenario_folders) != 1:
        die(f"expected one folder for scenario {scenario} in {RUNS}, found {len(scenario_folders)}")
    run_folders = sorted(folder for folder in scenario_folders[0].glob(f"{arm}-*") if (folder / "meta.json").is_file())
    if not run_folders:
        die(f"no finished run of scenario {scenario}, arm {arm}")
    random.Random(f"{CODE_SEED}-{scenario}-{arm}").shuffle(run_folders)
    return [RunCase(f"run-{index:02d}", scenario, folder) for index, folder in enumerate(run_folders, start=1)]


def evaluate_runs(scenario: str, arm: str, model: str, effort: str, workers: int, dry_run: bool) -> None:
    cases = run_cases(scenario, arm)
    batch_folder = EVALUATOR / PATTERN_BY_SCENARIO[scenario] / "runs" / f"{scenario}-{arm}"
    output_folder = batch_folder / f"{model.removeprefix('claude-')}-{effort}"
    if dry_run:
        with tempfile.TemporaryDirectory() as scratch:
            for case in cases:
                prepare_work_folder(case, Path(scratch) / case.code)
        print(f"{len(cases)} work folders build; output would go to {output_folder.relative_to(CONSTRUCTION_ROOT)}")
        return
    check_can_run(model)
    if output_folder.exists():
        die(f"{output_folder} already exists: evaluations are never reused")
    output_folder.mkdir(parents=True)
    (output_folder / "key.json").write_text(json.dumps({case.code: case.run_folder.name for case in cases},
                                                       indent=2) + LINE_BREAK, encoding="utf-8")
    with ThreadPoolExecutor(max_workers=workers) as pool:
        results = list(pool.map(lambda case: evaluate(case, output_folder / case.code, model, effort), cases))
    by_code = {result["code"]: result for result in results}
    lines = [f"# O avaliador nas runs do cenário {scenario}, braço {arm}: {model}, effort {effort}", "",
             "| código | run | avaliador |", "|---|---|---|"]
    for case in sorted(cases, key=lambda case: case.run_folder.name):
        lines.append(f"| {case.code} | {case.run_folder.name} | {by_code[case.code]['class']} |")
    costs = [result["cost_usd"] for result in results if result["cost_usd"] is not None]
    lines += ["", f"Custo: US$ {sum(costs):.2f}."]
    text = LINE_BREAK.join(lines) + LINE_BREAK
    (output_folder / "summary.md").write_text(text, encoding="utf-8")
    print(text)


def main() -> None:
    arguments = sys.argv[1:]
    dry_run = "--dry-run" in arguments
    arguments = [argument for argument in arguments if argument != "--dry-run"]
    workers = 1
    if "--workers" in arguments:
        position = arguments.index("--workers")
        workers = int(arguments[position + 1])
        del arguments[position:position + 2]
    if arguments[:1] == ["calibrate"] and len(arguments) in (2, 3):
        calibrate(arguments[1], arguments[2] if len(arguments) == 3 else "high", workers, dry_run)
        return
    if arguments[:1] == ["evaluate-runs"] and len(arguments) in (4, 5):
        evaluate_runs(arguments[1], arguments[2], arguments[3], arguments[4] if len(arguments) == 5 else "high",
                      workers, dry_run)
        return
    die(__doc__ or "")


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8")  # type: ignore[union-attr]
    main()
