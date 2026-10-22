"""The evaluator agent of TCC_V1, the same for both studies: Claude Code, inside the experiment's image, reads one
execution and answers with written feedback and one conclusion line per item (the pattern's class, the collateral
overengineering, the behavior). The criteria are the same in construction and maintenance; only the context changes
(what the evaluator receives, how to read it, how to test the behavior, and each pattern's starting point).

The prompt is common.md with the study's context (context/<study>/) in its slots, followed by the pattern's
criteria.md with the study's starting point (patterns/<pattern>/<study>.md) in its slot. The evaluator's container
sees only its work folder: never the answer key, the reference solutions' names, the run's name or the model.

Usage (from TCC_V1):
    python evaluation/evaluate_with_agent.py <study> calibrate <model> [effort] [--workers N] [--dry-run]
    python evaluation/evaluate_with_agent.py construction calibrate claude-opus-5-5 high --workers 3
    python evaluation/evaluate_with_agent.py <study> evaluate-runs <scenario> <arm> <model> [effort] [--workers N]
        [--resume] [--dry-run]
    python evaluation/evaluate_with_agent.py maintenance evaluate-runs 01 N0 claude-opus-5-5 high --workers 3

<study> is construction or maintenance. `calibrate` evaluates the 10 reference solutions of the five scenarios that
ask for a pattern (maintenance/evaluation/solutions/NN-pattern.patch, expected acerto, and NN-naive.patch, expected
omissão), under codes drawn at random (cal-01 to cal-10). In construction, a reference solution is the maintenance
base with the patch applied, given as a project built from zero; in maintenance, it is the base, the base with the
patch applied and the patch. `evaluate-runs` evaluates every run of one scenario and arm, blind, under codes drawn at
random (run-01, run-02...); --resume evaluates again only the codes without an answer.

Output in evaluation/<study>/<pattern>/calibration/<model>-<effort>/ and evaluation/<study>/<pattern>/runs/
<scenario>-<arm>/<model>-<effort>/: one folder per code (the evaluator's prompt, transcript and answer), key.json
(code to solution or run, written outside the containers) and summary.md; and the whole calibration in
evaluation/<study>/calibration-<model>-<effort>.md.
"""

import json
import random
import re
import shutil
import subprocess
import sys
import tempfile
from collections.abc import Callable
from concurrent.futures import ThreadPoolExecutor
from dataclasses import dataclass
from pathlib import Path
from typing import Any

TCC_V1 = Path(r"J:\TCC\TCC_V1")
REPOSITORY_ROOT = TCC_V1.parent
EVALUATION = Path(__file__).resolve().parent   # the restored, calibrated evaluator (reading-only run)
CONSTRUCTION = TCC_V1 / "construction"
MAINTENANCE = TCC_V1 / "maintenance"
BASE = MAINTENANCE / "experiment" / "base" / "hotel"
SOLUTIONS = MAINTENANCE / "evaluation" / "solutions"
ENV_FILE = REPOSITORY_ROOT / "construction-v5" / ".env"   # the same subscription token as the runs (never printed)
IMAGE = "experimento-harness:v5"
CODE_SEED = 20261019
LINE_BREAK = "\n"

STUDIES = ("construction", "maintenance")
PATTERN_BY_SCENARIO = {"01": "strategy", "03": "observer", "05": "factory-method", "07": "decorator",
                       "09": "template-method"}
PATTERNS = sorted(set(PATTERN_BY_SCENARIO.values()))
EXPECTED_CLASS_BY_KIND = {"pattern": "acerto", "naive": "omissao"}
CLASS_CODE_BY_WORD = {"acerto": "acerto", "alternativa equivalente": "alternativa_equivalente",
                      "malfeito": "malfeito", "omissão": "omissao"}
CONTEXT_SLOTS = {"INTRODUCAO": "introducao.md", "LEITURA": "leitura.md", "COMPORTAMENTO": "comportamento.md"}

EVALUATOR_COMMAND = """
    claude -p --model "$0" --effort "$1" --output-format stream-json --verbose \\
        --dangerously-skip-permissions --no-session-persistence < /experimento/prompt.md
"""


@dataclass(frozen=True)
class Case:
    """A reference solution: a patch of maintenance/evaluation/solutions, whose class is known."""
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
    """A run of one of the studies: runs/<scenario>-<label>/<arm>-<model>/."""
    code: str
    scenario: str
    run_folder: Path

    @property
    def pattern(self) -> str:
        return PATTERN_BY_SCENARIO[self.scenario]


def die(message: str) -> None:
    sys.exit(f"\033[31mERROR: {message}\033[0m")


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8").strip()


def evaluator_prompt(study: str, scenario: str) -> str:
    """common.md with the study's context, then the pattern's criteria with the study's starting point."""
    common = read(EVALUATION / "common.md")
    for slot, file_name in CONTEXT_SLOTS.items():
        common = common.replace("{{" + slot + "}}", read(EVALUATION / "context" / study / file_name))
    pattern_folder = EVALUATION / "patterns" / PATTERN_BY_SCENARIO[scenario]
    starting_point = read(pattern_folder / f"{study}.md")
    criteria = read(pattern_folder / "criteria.md").replace("{{PONTO_DE_PARTIDA}}", starting_point)
    prompt = common + LINE_BREAK * 2 + "---" + LINE_BREAK * 2 + criteria + LINE_BREAK
    left = re.findall(r"\{\{\w+\}\}", prompt)
    if left:
        die(f"the evaluator's prompt for {study} {scenario} still has {left}")
    return prompt


def construction_request(scenario: str) -> str:
    """The exact text a construction run of the scenario receives."""
    sys.path.insert(0, str(CONSTRUCTION / "infra" / "scripts"))
    from run_one_scenario import prompt_text  # only construction needs it
    return str(prompt_text(scenario))


def maintenance_request(scenario: str) -> str:
    """The exact text a maintenance run of the scenario receives: the frame with the scenario's request inside."""
    scenarios = MAINTENANCE / "experiment" / "scenarios"
    request = read(min(scenarios.glob(f"{scenario}-*.md")))
    return (scenarios / "frame.md").read_text(encoding="utf-8").replace("{{PEDIDO}}", request)


def apply_solution(case: Case, project: Path) -> None:
    """The base with the reference patch applied."""
    shutil.copytree(BASE, project, ignore=shutil.ignore_patterns("target"))
    applied = subprocess.run(["git", "apply", "--whitespace=nowarn", str(case.solution)], cwd=project,
                             capture_output=True, text=True, check=False)
    if applied.returncode != 0:
        die(f"{case.solution.name} does not apply to the base: {applied.stderr}")


def agent_files() -> Callable[[Any, list[str]], set[str]]:
    """What never goes to the evaluator from a run's workspace: the build, the agent's git and the harness."""
    return shutil.ignore_patterns("target", ".git", "CLAUDE.md", ".claude")


def prepare_construction_folder(case: Case | RunCase, work_folder: Path) -> None:
    """enunciado.md and projeto/ (the run's project, or the reference solution as a project built from zero)."""
    work_folder.mkdir(parents=True)
    if isinstance(case, RunCase):
        shutil.copyfile(case.run_folder / "prompt.md", work_folder / "enunciado.md")
        shutil.copytree(case.run_folder / "workspace", work_folder / "projeto", ignore=agent_files())
        return
    (work_folder / "enunciado.md").write_text(construction_request(case.scenario), encoding="utf-8",
                                              newline=LINE_BREAK)
    apply_solution(case, work_folder / "projeto")


def prepare_maintenance_folder(case: Case | RunCase, work_folder: Path) -> None:
    """pedido.md, base/, final/ (the run's final project, or the base with the solution applied) and changes.patch."""
    work_folder.mkdir(parents=True)
    shutil.copytree(BASE, work_folder / "base", ignore=shutil.ignore_patterns("target"))
    if isinstance(case, RunCase):
        shutil.copyfile(case.run_folder / "prompt.md", work_folder / "pedido.md")
        shutil.copytree(case.run_folder / "workspace", work_folder / "final", ignore=agent_files())
        shutil.copyfile(case.run_folder / "changes.patch", work_folder / "changes.patch")
        return
    (work_folder / "pedido.md").write_text(maintenance_request(case.scenario), encoding="utf-8", newline=LINE_BREAK)
    apply_solution(case, work_folder / "final")
    shutil.copyfile(case.solution, work_folder / "changes.patch")


def prepare_work_folder(study: str, case: Case | RunCase, work_folder: Path) -> None:
    if study == "construction":
        prepare_construction_folder(case, work_folder)
    else:
        prepare_maintenance_folder(case, work_folder)


def container_arm(case: Case | RunCase) -> str:
    """The arm of a run (N0, N1, N2...), or "cal" for a calibration case: only to keep container names apart."""
    return case.run_folder.name.split("-", 1)[0] if isinstance(case, RunCase) else "cal"


def evaluator_docker_command(study: str, case: Case | RunCase, work_folder: Path, prompt_file: Path, model: str,
                             effort: str) -> list[str]:
    return [
        # the study, the scenario and the arm are in the name: several batches can run at the same time
        "docker", "run", "--rm", "--name",
        f"tcc-v1-evaluator-{study}-{case.scenario}-{container_arm(case)}-{case.code}",
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


def read_answer(case_folder: Path) -> tuple[str | None, float | None]:
    """Reads the evaluator's transcript and returns the class and the cost. answer.md keeps the text, answer.json the
    three conclusions (the pattern's class, the collateral overengineering, the behavior)."""
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


def evaluate(study: str, case: Case | RunCase, case_folder: Path, model: str, effort: str) -> dict[str, Any]:
    case_folder.mkdir(parents=True)
    prompt_file = case_folder / "evaluator-prompt.md"
    prompt_file.write_text(evaluator_prompt(study, case.scenario), encoding="utf-8", newline=LINE_BREAK)
    with tempfile.TemporaryDirectory(prefix=f"evaluation-{case.code}-") as scratch:
        work_folder = Path(scratch) / "work"
        prepare_work_folder(study, case, work_folder)
        command = evaluator_docker_command(study, case, work_folder, prompt_file, model, effort)
        with (case_folder / "evaluator-output.jsonl").open("wb") as output, \
                (case_folder / "stderr.txt").open("wb") as errors:
            exit_code = subprocess.run(command, stdout=output, stderr=errors, check=False).returncode
    given, cost = read_answer(case_folder)
    print(f"{study} {case.code} ({case.pattern}): exit {exit_code}, class {given}", flush=True)
    return {"code": case.code, "exit_code": exit_code, "class": given, "cost_usd": cost}


def check_can_run(model: str) -> None:
    if not re.fullmatch(r"claude-[\w.-]+-[\w.-]+", model):
        die(f"use the full model ID: '{model}'")
    if subprocess.run(["docker", "image", "inspect", IMAGE], capture_output=True, check=False).returncode != 0:
        die(f"image {IMAGE} not found (is Docker running?)")
    if not ENV_FILE.is_file():
        die(f"{ENV_FILE} not found")


def short(model: str, effort: str) -> str:
    return f"{model.removeprefix('claude-')}-{effort}"


def calibration_folder(study: str, pattern: str, model: str, effort: str) -> Path:
    return EVALUATION / study / pattern / "calibration" / short(model, effort)


def calibration_cases() -> list[Case]:
    patches = sorted(patch for patch in SOLUTIONS.glob("[0-9][0-9]-*.patch")
                     if patch.name[:2] in PATTERN_BY_SCENARIO and patch.stem.split("-", 1)[1] in EXPECTED_CLASS_BY_KIND)
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


def write_summaries(study: str, cases: list[Case], results: list[dict[str, Any]], model: str, effort: str) -> None:
    results_by_code = {result["code"]: result for result in results}
    for pattern in PATTERNS:
        pattern_cases = [case for case in cases if case.pattern == pattern]
        table, right = summary_table(pattern_cases, results_by_code)
        lines = [f"# Calibração do avaliador de {pattern} ({study}): {model}, effort {effort}", "", *table, "",
                 f"**{right} de {len(pattern_cases)}** na classe esperada."]
        (calibration_folder(study, pattern, model, effort) / "summary.md").write_text(
            LINE_BREAK.join(lines) + LINE_BREAK, encoding="utf-8")
    table, right = summary_table(cases, results_by_code)
    costs = [result["cost_usd"] for result in results if result["cost_usd"] is not None]
    lines = [f"# Calibração do avaliador unificado ({study}): {model}, effort {effort}", "", *table, "",
             f"**{right} de {len(cases)}** na classe esperada. Custo: US$ {sum(costs):.2f}."]
    text = LINE_BREAK.join(lines) + LINE_BREAK
    (EVALUATION / study / f"calibration-{short(model, effort)}.md").write_text(text, encoding="utf-8")
    print(text)


def calibrate(study: str, model: str, effort: str, workers: int, dry_run: bool) -> None:
    cases = calibration_cases()
    if dry_run:
        with tempfile.TemporaryDirectory() as scratch:
            for case in cases:
                prepare_work_folder(study, case, Path(scratch) / case.code)
                evaluator_prompt(study, case.scenario)
        for pattern in PATTERNS:
            codes = ", ".join(case.code for case in cases if case.pattern == pattern)
            print(f"{pattern}: {codes} -> {calibration_folder(study, pattern, model, effort).relative_to(TCC_V1)}")
        print(f"the {len(cases)} work folders and prompts build")
        return
    check_can_run(model)
    for pattern in PATTERNS:
        folder = calibration_folder(study, pattern, model, effort)
        if folder.exists():
            die(f"{folder} already exists: calibrations are never reused")
        folder.mkdir(parents=True)
        key = {case.code: case.solution.name for case in cases if case.pattern == pattern}
        (folder / "key.json").write_text(json.dumps(key, indent=2) + LINE_BREAK, encoding="utf-8")
    with ThreadPoolExecutor(max_workers=workers) as pool:
        results = list(pool.map(
            lambda case: evaluate(study, case, calibration_folder(study, case.pattern, model, effort) / case.code,
                                  model, effort),
            cases))
    write_summaries(study, cases, results, model, effort)


def run_cases(study: str, scenario: str, arm: str) -> list[RunCase]:
    runs = TCC_V1 / study / "runs"
    scenario_folders = sorted(runs.glob(f"{scenario}-*"))
    if len(scenario_folders) != 1:
        die(f"expected one folder for scenario {scenario} in {runs}, found {len(scenario_folders)}")
    run_folders = sorted(folder for folder in scenario_folders[0].glob(f"{arm}-*") if (folder / "meta.json").is_file())
    if not run_folders:
        die(f"no finished run of scenario {scenario}, arm {arm} in {study}")
    random.Random(f"{CODE_SEED}-{study}-{scenario}-{arm}").shuffle(run_folders)
    return [RunCase(f"run-{index:02d}", scenario, folder) for index, folder in enumerate(run_folders, start=1)]


def evaluate_runs(study: str, scenario: str, arm: str, model: str, effort: str, workers: int, dry_run: bool,
                  resume: bool) -> None:
    if scenario not in PATTERN_BY_SCENARIO:
        die(f"scenario {scenario} does not ask for a pattern: use one of {', '.join(PATTERN_BY_SCENARIO)}")
    cases = run_cases(study, scenario, arm)
    output_folder = EVALUATION / study / PATTERN_BY_SCENARIO[scenario] / "runs" / f"{scenario}-{arm}" / short(model,
                                                                                                             effort)
    if dry_run:
        with tempfile.TemporaryDirectory() as scratch:
            for case in cases:
                prepare_work_folder(study, case, Path(scratch) / case.code)
        print(f"{len(cases)} work folders build; output would go to {output_folder.relative_to(TCC_V1)}")
        return
    check_can_run(model)
    key = {case.code: case.run_folder.name for case in cases}
    if output_folder.exists() and not resume:
        die(f"{output_folder} already exists: evaluations are never reused (--resume evaluates only the missing ones)")
    if resume:
        saved_key = json.loads((output_folder / "key.json").read_text(encoding="utf-8"))
        if saved_key != key:
            die(f"{output_folder}/key.json does not match the runs of {scenario}-{arm}: not resuming")
    else:
        output_folder.mkdir(parents=True)
        (output_folder / "key.json").write_text(json.dumps(key, indent=2) + LINE_BREAK, encoding="utf-8")
    # with --resume, an evaluation with an answer stays; one without (it failed) is done again from scratch
    done = {case.code for case in cases if (output_folder / case.code / "answer.md").is_file()} if resume else set()
    missing = [case for case in cases if case.code not in done]
    for case in missing:
        if (output_folder / case.code).exists():
            shutil.rmtree(output_folder / case.code)
    with ThreadPoolExecutor(max_workers=workers) as pool:
        results = list(pool.map(lambda case: evaluate(study, case, output_folder / case.code, model, effort), missing))
    for code in sorted(done):
        given, cost = read_answer(output_folder / code)
        results.append({"code": code, "exit_code": None, "class": given, "cost_usd": cost})
    by_code = {result["code"]: result for result in results}
    lines = [f"# O avaliador nas runs do cenário {scenario}, braço {arm} ({study}): {model}, effort {effort}", "",
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
    resume = "--resume" in arguments
    arguments = [argument for argument in arguments if argument not in ("--dry-run", "--resume")]
    workers = 1
    if "--workers" in arguments:
        position = arguments.index("--workers")
        workers = int(arguments[position + 1])
        del arguments[position:position + 2]
    if not arguments or arguments[0] not in STUDIES:
        die(__doc__ or "")
    study, command = arguments[0], arguments[1:]
    if command[:1] == ["calibrate"] and len(command) in (2, 3):
        calibrate(study, command[1], command[2] if len(command) == 3 else "high", workers, dry_run)
        return
    if command[:1] == ["evaluate-runs"] and len(command) in (4, 5):
        evaluate_runs(study, command[1], command[2], command[3], command[4] if len(command) == 5 else "high",
                      workers, dry_run, resume)
        return
    die(__doc__ or "")


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8")  # type: ignore[union-attr]
    main()
