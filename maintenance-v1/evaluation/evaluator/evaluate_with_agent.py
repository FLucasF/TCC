"""The evaluator agent: Claude Code, inside the experiment's image, reads one change (the request, the base, the
project after the change and the diff) and answers with a class of the answer key, as JSON. The prompt is
common.md followed by the pattern's prompt.md. The evaluator's container sees only its work folder: never the
answer key, the reference solutions, the run's name or the model that made the change. Each pattern has its folder
here (strategy/, observer/, factory-method/, decorator/, template-method/), with its prompt and its calibrations.

Usage (from maintenance-v1):
    python evaluation/evaluator/evaluate_with_agent.py calibrate <model> [effort] [--workers N] [--dry-run]
    python evaluation/evaluator/evaluate_with_agent.py calibrate claude-opus-5-5 high --workers 3
    python evaluation/evaluator/evaluate_with_agent.py summarize claude-opus-5-5 high   (reads the saved answers again)

`calibrate` evaluates the 20 reference solutions (evaluation/solutions/), whose class is known, under codes drawn
at random (cal-01 to cal-20). Output in each pattern's folder, in calibration/<model>-<effort>/: one folder per code
(the evaluator's prompt, transcript and answer), key.json (code to solution, written outside the containers) and
summary.md (expected class against the evaluator's); and the whole picture in calibration-<model>-<effort>.md here.
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

MAINTENANCE_ROOT = Path(__file__).resolve().parents[2]
REPOSITORY_ROOT = MAINTENANCE_ROOT.parent
EVALUATOR = MAINTENANCE_ROOT / "evaluation" / "evaluator"
SOLUTIONS = MAINTENANCE_ROOT / "evaluation" / "solutions"
BASE = MAINTENANCE_ROOT / "experiment" / "base" / "hotel"
SCENARIOS = MAINTENANCE_ROOT / "experiment" / "scenarios"
ENV_FILE = REPOSITORY_ROOT / "construction-v5" / ".env"   # the same subscription token as the runs (never printed)
IMAGE = "experimento-harness:v5"
CODE_SEED = 20261016
LINE_BREAK = "\n"

PATTERN_BY_SCENARIO = {
    "01": "strategy", "02": "strategy",
    "03": "observer", "04": "observer",
    "05": "factory-method", "06": "factory-method",
    "07": "decorator", "08": "decorator",
    "09": "template-method", "10": "template-method",
}
PATTERNS = sorted(set(PATTERN_BY_SCENARIO.values()))
EXPECTED_CLASS_BY_KIND = {"pattern": "acerto", "naive": "omissao", "contained": "contencao",
                          "overengineered": "exagero"}

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


def die(message: str) -> None:
    sys.exit(f"\033[31mERROR: {message}\033[0m")


def request_text(scenario: str) -> str:
    """The exact text the agent of a run receives: the frame with the scenario's request inside."""
    request = sorted(SCENARIOS.glob(f"{scenario}-*.md"))[0].read_text(encoding="utf-8").strip()
    return (SCENARIOS / "frame.md").read_text(encoding="utf-8").replace("{{PEDIDO}}", request)


def evaluator_prompt(scenario: str) -> str:
    common = (EVALUATOR / "common.md").read_text(encoding="utf-8").strip()
    pattern = (EVALUATOR / PATTERN_BY_SCENARIO[scenario] / "prompt.md").read_text(encoding="utf-8").strip()
    return common + LINE_BREAK * 2 + "---" + LINE_BREAK * 2 + pattern + LINE_BREAK


def prepare_work_folder(case: Case, work_folder: Path) -> None:
    """pedido.md, base/, final/ (the base with the solution applied) and changes.patch."""
    work_folder.mkdir(parents=True)
    (work_folder / "pedido.md").write_text(request_text(case.scenario), encoding="utf-8", newline=LINE_BREAK)
    shutil.copytree(BASE, work_folder / "base", ignore=shutil.ignore_patterns("target"))
    shutil.copytree(BASE, work_folder / "final", ignore=shutil.ignore_patterns("target"))
    applied = subprocess.run(["git", "apply", "--whitespace=nowarn", str(case.solution)], cwd=work_folder / "final",
                             capture_output=True, text=True, check=False)
    if applied.returncode != 0:
        die(f"{case.solution.name} does not apply to the base: {applied.stderr}")
    shutil.copyfile(case.solution, work_folder / "changes.patch")


def evaluator_docker_command(case: Case, work_folder: Path, prompt_file: Path, model: str,
                             effort: str) -> list[str]:
    return [
        "docker", "run", "--rm", "--name", f"evaluator-{case.code}",
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


def parse_answer(text: str) -> dict[str, Any] | None:
    """The LAST valid JSON object in the answer: the evaluator sometimes answers, then corrects itself with a second
    object. Fenced blocks (```json) first; if there are none, any object in the text."""
    for block in reversed(re.findall(r"```(?:json)?\s*(\{.*?\})\s*```", text, re.DOTALL)):
        try:
            answer = json.loads(block)
        except json.JSONDecodeError:
            continue
        if isinstance(answer, dict):
            return answer
    decoder = json.JSONDecoder()
    found: dict[str, Any] | None = None
    for start in (position for position, character in enumerate(text) if character == "{"):
        try:
            candidate, _ = decoder.raw_decode(text[start:])
        except json.JSONDecodeError:
            continue
        if isinstance(candidate, dict) and "classe" in candidate:
            found = candidate
    return found


def read_answer(case_folder: Path) -> tuple[str | None, float | None]:
    """Reads the evaluator's transcript again, rewrites answer.json, and returns the class and the cost."""
    text, cost = final_answer(case_folder / "evaluator-output.jsonl")
    answer = parse_answer(text)
    (case_folder / "answer.json").write_text(json.dumps(answer if answer is not None else {"unparsed": text},
                                                        ensure_ascii=False, indent=2) + LINE_BREAK, encoding="utf-8")
    given = answer.get("classe") if answer else None
    return (given if isinstance(given, str) else None), cost


def calibration_folder(pattern: str, model: str, effort: str) -> Path:
    return EVALUATOR / pattern / "calibration" / f"{model.removeprefix('claude-')}-{effort}"


def evaluate(case: Case, model: str, effort: str) -> dict[str, Any]:
    case_folder = calibration_folder(case.pattern, model, effort) / case.code
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
    patches = sorted(SOLUTIONS.glob("[0-9][0-9]-*.patch"))
    if len(patches) != 20:
        die(f"expected 20 reference solutions in {SOLUTIONS}, found {len(patches)}")
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


def calibrate(model: str, effort: str, workers: int, dry_run: bool) -> None:
    if not re.fullmatch(r"claude-[\w.-]+-[\w.-]+", model):
        die(f"use the full model ID: '{model}'")
    cases = calibration_cases()
    if dry_run:
        with tempfile.TemporaryDirectory() as scratch:
            for case in cases:
                prepare_work_folder(case, Path(scratch) / case.code)
        for pattern in PATTERNS:
            codes = ", ".join(case.code for case in cases if case.pattern == pattern)
            print(f"{pattern}: {codes} -> {calibration_folder(pattern, model, effort).relative_to(MAINTENANCE_ROOT)}")
        print("the 20 work folders build")
        return
    if subprocess.run(["docker", "image", "inspect", IMAGE], capture_output=True, check=False).returncode != 0:
        die(f"image {IMAGE} not found (is Docker running?)")
    if not ENV_FILE.is_file():
        die(f"{ENV_FILE} not found")
    for pattern in PATTERNS:
        folder = calibration_folder(pattern, model, effort)
        if folder.exists():
            die(f"{folder} already exists: calibrations are never reused")
        folder.mkdir(parents=True)
        key = {case.code: case.solution.name for case in cases if case.pattern == pattern}
        (folder / "key.json").write_text(json.dumps(key, indent=2) + LINE_BREAK, encoding="utf-8")
    with ThreadPoolExecutor(max_workers=workers) as pool:
        results = list(pool.map(lambda case: evaluate(case, model, effort), cases))
    write_summaries(cases, results, model, effort)


def summarize_again(model: str, effort: str) -> None:
    """Reads the 20 saved answers again (after a fix in how answers are read), without running any evaluation."""
    cases = calibration_cases()
    results = []
    for case in cases:
        given, cost = read_answer(calibration_folder(case.pattern, model, effort) / case.code)
        results.append({"code": case.code, "exit_code": None, "class": given, "cost_usd": cost})
    write_summaries(cases, results, model, effort)


def main() -> None:
    arguments = sys.argv[1:]
    if arguments[:1] == ["summarize"] and len(arguments) in (2, 3):
        summarize_again(arguments[1], arguments[2] if len(arguments) == 3 else "high")
        return
    dry_run = "--dry-run" in arguments
    workers = 1
    if "--workers" in arguments:
        position = arguments.index("--workers")
        workers = int(arguments[position + 1])
        del arguments[position:position + 2]
    arguments = [argument for argument in arguments if argument != "--dry-run"]
    if len(arguments) not in (2, 3) or arguments[0] != "calibrate":
        die("usage: python evaluation/evaluator/evaluate_with_agent.py calibrate <model> [effort] [--workers N]")
    calibrate(arguments[1], arguments[2] if len(arguments) == 3 else "high", workers, dry_run)


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8")  # type: ignore[union-attr]
    main()
