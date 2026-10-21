"""The quality reviewer of TCC_V1: only for the evaluation, never part of the experiment. After the runs, each project
is copied to a work folder of its own and reviewed by Claude Code running the `quality-reviewer` agent, with one skill
per column (review-architecture, review-design, review-tests, review-database, review-behavior), in the reviewer's
image (tcc-v1-reviewer:1), with a PostgreSQL of its own. The skills carry the measuring scripts; the reviewer runs
them and judges. At the end, this script checks that every result exists and runs the missing scripts itself,
recording that the reviewer skipped them.

Usage (from TCC_V1):
    python evaluation/quality/review_runs.py review <run folder>... [options]
    python evaluation/quality/review_runs.py review maintenance/runs/01-strategy/N0-sonnet-5-5
    python evaluation/quality/review_runs.py review-all <construction|maintenance> [--scenario NN] [--arm NX] [options]
    python evaluation/quality/review_runs.py calibrate [<case>...] [options]
    python evaluation/quality/review_runs.py summary

Options: --model claude-opus-5-5 (default), --effort high (default), --workers N (default 1), --resume (review
again only the ones without a review.md), --dry-run (assemble the work folders and show the commands, run nothing).

Output, one folder per review, with review.md (the reviewer's text), review.json (the grades read from it),
measures/ (what the scripts wrote), the transcript and meta.json:
    evaluation/quality/results/<study>/<scenario>-<pattern>/<arm>-<model>/      (the runs)
    evaluation/quality/calibration/<model>-<effort>/<study>/<case>/               (the calibration)
`summary` writes evaluation/quality/results/summary.md (one line per run, and the counts per study, arm and column);
`calibrate` writes calibration/<model>-<effort>/summary.md (expected and given, per case and column).

The calibration cases are patches over the maintenance base, outside TCC_V1 (with the answer key, cases.json):
J:/TCC/maintenance-v1/evaluation/solutions-postgres/. In construction, a case is the base with the patch applied, as
a project built from zero; in maintenance, the base, the base with the patch applied and the patch.

Isolation: the reviewer's container sees only its work folder: the text the agent received, the project (without the
agent's CLAUDE.md, .claude/, .git and target/), task.md, the agent definition and the skills. Never the run's name,
the model, the arm, or the calibration's answer key. The subscription token comes from construction-v5's .env
(--env-file), never read here.
"""

import importlib.util
import json
import random
import re
import shutil
import subprocess
import sys
import tempfile
from collections.abc import Callable
from concurrent.futures import ThreadPoolExecutor
from dataclasses import dataclass, field
from datetime import datetime
from pathlib import Path
from typing import Any

QUALITY = Path(__file__).resolve().parent
TCC_V1 = QUALITY.parents[1]
REPOSITORY_ROOT = TCC_V1.parent
sys.path.insert(0, str(TCC_V1 / "infra"))

import run_database  # noqa: E402  (TCC_V1/infra: one PostgreSQL per review)

CONSTRUCTION = TCC_V1 / "construction"
MAINTENANCE = TCC_V1 / "maintenance"
BASE = MAINTENANCE / "experiment" / "base" / "hotel"
CALIBRATION_CASES = REPOSITORY_ROOT / "maintenance-v1" / "evaluation" / "solutions-postgres"
RESULTS = QUALITY / "results"
CALIBRATION = QUALITY / "calibration"
ENV_FILE = REPOSITORY_ROOT / "construction-v5" / ".env"   # the same subscription token as the runs (never printed)
IMAGE = "tcc-v1-reviewer:1"
LINE_BREAK = "\n"

STUDIES = ("construction", "maintenance")
STUDY_NAMES = {"construction": "construção", "maintenance": "manutenção"}
PATTERN_BY_SCENARIO = {"01": "strategy", "03": "observer", "05": "factory-method", "07": "decorator",
                       "09": "template-method"}
PATTERN_NAMES = {"strategy": "Strategy", "observer": "Observer", "factory-method": "Factory Method",
                 "decorator": "Decorator", "template-method": "Template Method"}
# the scripts each skill carries; project.sh (lib/) goes next to them
SCRIPTED_SKILLS = ("review-architecture", "review-tests", "review-database", "review-behavior")
# the review.md sections, and the grade lines read from each
COLUMNS = {"arquitetura": "architecture", "design de baixo nível": "design", "testes": "tests", "banco": "database",
           "comportamento": "behavior"}
GRADES = ("bom", "com problemas", "ruim")
CLASSES = ("acerto", "alternativa equivalente", "malfeito", "omissão")
OVERENGINEERING = ("contenção", "exagero")

REVIEWER_COMMAND = """
    echo "[pre] claude $(claude --version)" >&2
    echo "Faça a revisão descrita em task.md." | claude -p --agent quality-reviewer --model "$0" --effort "$1" \\
        --output-format stream-json --verbose --dangerously-skip-permissions --no-session-persistence
"""


@dataclass(frozen=True)
class Review:
    """One project to review: a run of the studies, or a calibration case (a patch over the base)."""
    study: str
    scenario: str
    output: Path
    container: str
    run_folder: Path | None = None
    patch: Path | None = None
    expected: dict[str, str] = field(default_factory=dict)

    @property
    def pattern(self) -> str:
        return PATTERN_BY_SCENARIO[self.scenario]

    @property
    def project_folder(self) -> str:
        return "projeto" if self.study == "construction" else "final"


def die(message: str) -> None:
    sys.exit(f"\033[31mERROR: {message}\033[0m")


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8").strip()


def short(model: str, effort: str) -> str:
    return f"{model.removeprefix('claude-')}-{effort}"


# ---------------------------------------------------------------------------------------------------------- the texts

def construction_request(scenario: str) -> str:
    """The exact text a construction run of the scenario receives."""
    spec = importlib.util.spec_from_file_location(
        "construction_run_one_scenario", CONSTRUCTION / "infra" / "scripts" / "run_one_scenario.py")
    assert spec is not None and spec.loader is not None
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return str(module.prompt_text(scenario))


def maintenance_request(scenario: str) -> str:
    """The exact text a maintenance run of the scenario receives: the frame with the scenario's request inside."""
    scenarios = MAINTENANCE / "experiment" / "scenarios"
    request = read(min(scenarios.glob(f"{scenario}-*.md")))
    return (scenarios / "frame.md").read_text(encoding="utf-8").replace("{{PEDIDO}}", request)


def task_text(review: Review) -> str:
    project = review.project_folder
    lines = [
        "# A revisão",
        "",
        f"- **Pesquisa:** {STUDY_NAMES[review.study]}.",
        f"- **Cenário:** {review.scenario}; o texto pede, sem dizer o nome, o {PATTERN_NAMES[review.pattern]} "
        f"(a referência em `.claude/skills/review-design/references/{review.pattern}.md`).",
    ]
    if review.study == "construction":
        lines += [
            "- **O texto que o agente recebeu:** `enunciado.md`.",
            "- **O projeto:** `projeto/`, construído do zero pelo agente.",
            "- **Os scripts:** rode cada um com `projeto` (por exemplo, "
            "`bash .claude/skills/review-tests/measure_tests.sh projeto`).",
        ]
    else:
        lines += [
            "- **O texto que o agente recebeu:** `pedido.md`.",
            "- **O projeto:** `base/` (antes da mudança), `final/` (como o agente o deixou) e `changes.patch` (a "
            "diferença, sem os arquivos de configuração do agente).",
            "- **Os scripts:** rode cada um com `final`; o da arquitetura e o dos testes, também com `base`, para "
            "comparar.",
        ]
    lines += [
        f"- **O roteiro do comportamento:** `.claude/skills/review-behavior/check_{review.scenario}.sh {project}`.",
        "",
        "Escreva a revisão em `review.md`, no formato do agente `quality-reviewer`.",
    ]
    return LINE_BREAK.join(lines) + LINE_BREAK


# ---------------------------------------------------------------------------------------------- the work folder

def agent_files() -> Callable[[Any, list[str]], set[str]]:
    """What never goes to the reviewer from a project: the build, the agent's git and its harness (which would tell
    the arm, and whose CLAUDE.md Claude Code would load)."""
    return shutil.ignore_patterns("target", ".git", "CLAUDE.md", ".claude")


def apply_patch(patch: Path, project: Path) -> None:
    shutil.copytree(BASE, project, ignore=shutil.ignore_patterns("target"))
    applied = subprocess.run(["git", "apply", "--whitespace=nowarn", str(patch)], cwd=project, capture_output=True,
                             text=True, encoding="utf-8", check=False)
    if applied.returncode != 0:
        die(f"{patch.name} does not apply to the base: {applied.stderr}")


def assemble(review: Review, work: Path) -> None:
    """The reviewer's work folder: the agent, the skills (with project.sh next to the scripts), task.md, the text the
    agent received and the project."""
    (work / ".claude" / "agents").mkdir(parents=True)
    shutil.copyfile(QUALITY / "agent" / "quality-reviewer.md", work / ".claude" / "agents" / "quality-reviewer.md")
    shutil.copytree(QUALITY / "skills", work / ".claude" / "skills")
    for skill in SCRIPTED_SKILLS:
        shutil.copyfile(QUALITY / "lib" / "project.sh", work / ".claude" / "skills" / skill / "project.sh")
    (work / "measures").mkdir()
    (work / "task.md").write_text(task_text(review), encoding="utf-8", newline=LINE_BREAK)
    if review.study == "construction":
        if review.run_folder is not None:
            shutil.copyfile(review.run_folder / "prompt.md", work / "enunciado.md")
            shutil.copytree(review.run_folder / "workspace", work / "projeto", ignore=agent_files())
        else:
            assert review.patch is not None
            (work / "enunciado.md").write_text(construction_request(review.scenario), encoding="utf-8",
                                               newline=LINE_BREAK)
            apply_patch(review.patch, work / "projeto")
        return
    shutil.copytree(BASE, work / "base", ignore=shutil.ignore_patterns("target"))
    if review.run_folder is not None:
        shutil.copyfile(review.run_folder / "prompt.md", work / "pedido.md")
        shutil.copytree(review.run_folder / "workspace", work / "final", ignore=agent_files())
        shutil.copyfile(review.run_folder / "changes.patch", work / "changes.patch")
    else:
        assert review.patch is not None
        (work / "pedido.md").write_text(maintenance_request(review.scenario), encoding="utf-8", newline=LINE_BREAK)
        apply_patch(review.patch, work / "final")
        shutil.copyfile(review.patch, work / "changes.patch")


def expected_measures(review: Review) -> dict[str, tuple[str, str, str]]:
    """The result files the review must leave in measures/, and the skill, script and folder that write each one."""
    project = review.project_folder
    measures = {
        "dependencies.md": ("review-architecture", "measure_dependencies.sh", project),
        "tests.md": ("review-tests", "measure_tests.sh", project),
        "database.md": ("review-database", "measure_database.sh", project),
        "behavior.md": ("review-behavior", f"check_{review.scenario}.sh", project),
    }
    if review.study == "maintenance":
        measures["dependencies-base.md"] = ("review-architecture", "measure_dependencies.sh", "base")
        measures["tests-base.md"] = ("review-tests", "measure_tests.sh", "base")
    return measures


# -------------------------------------------------------------------------------------------------- the containers

def mount(work: Path) -> list[str]:
    return ["--mount", f"type=bind,source={work},target=/review"]


def reviewer_command(review: Review, work: Path, model: str, effort: str) -> list[str]:
    return ["docker", "run", "--rm", "--name", review.container, "--env-file", str(ENV_FILE),
            *run_database.review_arguments(review.container), *mount(work), "-w", "/review",
            IMAGE, "bash", "-c", REVIEWER_COMMAND, model, effort]


def script_command(review: Review, work: Path, skill: str, script: str, folder: str) -> list[str]:
    """A measuring script run by the pipeline itself, in a container without the token."""
    return ["docker", "run", "--rm", "--name", f"{review.container}-script",
            *run_database.review_arguments(review.container), *mount(work), "-w", "/review", IMAGE,
            "bash", f".claude/skills/{skill}/{script}", folder]


# ---------------------------------------------------------------------------------------------------- the answer

def section_lines(text: str) -> dict[str, list[str]]:
    """The lines of each "## " section of review.md, by the section's name in lowercase."""
    sections: dict[str, list[str]] = {}
    current = ""
    for line in text.splitlines():
        if line.startswith("## "):
            current = line[3:].strip().lower()
            sections[current] = []
        elif current:
            sections[current].append(line)
    return sections


def grade_line(lines: list[str], label: str, allowed: tuple[str, ...]) -> str | None:
    for line in lines:
        match = re.fullmatch(rf"\s*\*\*{label}:\*\*\s*`?([^`]+?)`?\s*\.?\s*", line)
        if match:
            word = match.group(1).strip().lower()
            return word if word in allowed else f"?{word}"
    return None


def read_grades(review_file: Path) -> dict[str, str | None]:
    """The grades in review.md: one per column, and the class and the overengineering in the design."""
    if not review_file.is_file():
        return {}
    sections = section_lines(review_file.read_text(encoding="utf-8", errors="replace"))
    grades: dict[str, str | None] = {}
    for title, column in COLUMNS.items():
        lines = sections.get(title, [])
        grades[column] = grade_line(lines, "Nota", GRADES)
        if column == "design":
            grades["design_class"] = grade_line(lines, "Classe", CLASSES)
            grades["design_overengineering"] = grade_line(lines, "Exagero", OVERENGINEERING)
    return grades


def transcript_summary(transcript: Path) -> dict[str, Any]:
    """The turns, the cost, and the skills the reviewer opened (with the Skill tool, or reading the SKILL.md)."""
    result: dict[str, Any] = {}
    skills: set[str] = set()
    for line in transcript.read_text(encoding="utf-8", errors="replace").splitlines():
        try:
            event = json.loads(line)
        except json.JSONDecodeError:
            continue
        if event.get("type") == "assistant":
            for content in event.get("message", {}).get("content", []):
                if content.get("type") != "tool_use":
                    continue
                tool_input = json.dumps(content.get("input", {}), ensure_ascii=False)
                if content.get("name") == "Skill":
                    skills.add(str(content.get("input", {}).get("skill", "")))
                skills.update(re.findall(r"skills/(review-[a-z]+)/SKILL\.md", tool_input))
        if event.get("type") == "result":
            result = event
    return {"turns": result.get("num_turns"), "cost_usd": result.get("total_cost_usd"),
            "skills_opened": sorted(skills), "final_answer": str(result.get("result") or "")[:200]}


# ------------------------------------------------------------------------------------------------------ one review

def run_review(review: Review, model: str, effort: str) -> dict[str, Any]:
    review.output.mkdir(parents=True)
    started = datetime.now().astimezone()
    with tempfile.TemporaryDirectory(prefix="quality-review-") as scratch:
        work = Path(scratch) / "review"
        work.mkdir()
        assemble(review, work)
        try:
            run_database.start(review.container)
        except RuntimeError as failure:
            return {"review": review.output.name, "error": str(failure)}
        try:
            with (review.output / "reviewer-output.jsonl").open("wb") as output, \
                    (review.output / "stderr.txt").open("wb") as errors:
                exit_code = subprocess.run(reviewer_command(review, work, model, effort), stdout=output,
                                           stderr=errors, check=False).returncode
            skipped = []
            for result_file, (skill, script, folder) in expected_measures(review).items():
                if (work / "measures" / result_file).is_file():
                    continue
                skipped.append(result_file)
                with (review.output / "pipeline-scripts.txt").open("ab") as log:
                    subprocess.run(script_command(review, work, skill, script, folder), stdout=log,
                                   stderr=subprocess.STDOUT, check=False)
        finally:
            run_database.remove(review.container)
        for name in ("review.md", "task.md"):
            if (work / name).is_file():
                shutil.copyfile(work / name, review.output / name)
        shutil.copytree(work / "measures", review.output / "measures")
    grades = read_grades(review.output / "review.md")
    (review.output / "review.json").write_text(json.dumps(grades, ensure_ascii=False, indent=2) + LINE_BREAK,
                                               encoding="utf-8")
    meta = {
        "study": review.study, "scenario": review.scenario, "pattern": review.pattern,
        "run": str(review.run_folder.relative_to(TCC_V1)) if review.run_folder else None,
        "case": review.patch.name if review.patch else None,
        "model": model, "effort": effort, "image": IMAGE, "database_image": run_database.IMAGE,
        "start": started.isoformat(timespec="seconds"),
        "end": datetime.now().astimezone().isoformat(timespec="seconds"),
        "exit_code": exit_code, "review_written": (review.output / "review.md").is_file(),
        "scripts_skipped_by_reviewer": skipped, "grades": grades,
        **transcript_summary(review.output / "reviewer-output.jsonl"),
    }
    (review.output / "meta.json").write_text(json.dumps(meta, ensure_ascii=False, indent=2) + LINE_BREAK,
                                             encoding="utf-8")
    print(f"{review.output.relative_to(QUALITY)}: exit {exit_code}, grades {grades}, skipped {skipped}, "
          f"US$ {meta['cost_usd']}", flush=True)
    return meta


def check_can_run(model: str) -> None:
    if not re.fullmatch(r"claude-[\w.-]+-[\w.-]+", model):
        die(f"use the full model ID: '{model}'")
    if subprocess.run(["docker", "image", "inspect", IMAGE], capture_output=True, check=False).returncode != 0:
        die(f"image {IMAGE} not found: docker build -t {IMAGE} {QUALITY / 'docker'}")
    if not ENV_FILE.is_file():
        die(f"{ENV_FILE} not found")


def run_all(reviews: list[Review], model: str, effort: str, workers: int, dry_run: bool,
            resume: bool) -> list[dict[str, Any]]:
    if dry_run:
        with tempfile.TemporaryDirectory() as scratch:
            for index, review in enumerate(reviews):
                work = Path(scratch) / str(index)
                work.mkdir()
                assemble(review, work)
                print(f"{review.output.relative_to(QUALITY)}: assembles ({review.study} {review.scenario})")
            if reviews:
                print("  " + " ".join(part if " " not in part and "\n" not in part else "<script>"
                                      for part in reviewer_command(reviews[0], Path(scratch) / "0", model, effort)))
        return []
    check_can_run(model)
    pending = []
    for review in reviews:
        if (review.output / "review.md").is_file() and resume:
            continue
        if review.output.exists():
            if not resume:
                die(f"{review.output} already exists: reviews are never reused (--resume redoes only the missing)")
            shutil.rmtree(review.output)
        pending.append(review)
    with ThreadPoolExecutor(max_workers=workers) as pool:
        return list(pool.map(lambda review: run_review(review, model, effort), pending))


# ---------------------------------------------------------------------------------------------- the runs and cases

def run_reviews(run_folders: list[Path], model: str, effort: str) -> list[Review]:
    reviews = []
    for folder in run_folders:
        folder = folder if folder.is_absolute() else (TCC_V1 / folder)
        folder = folder.resolve()
        if not (folder / "meta.json").is_file():
            die(f"{folder} is not a finished run (no meta.json)")
        study = folder.parents[2].name
        if study not in STUDIES:
            die(f"{folder} is not under construction/runs or maintenance/runs")
        scenario = folder.parent.name[:2]
        output = RESULTS / study / folder.parent.name / folder.name
        container = f"tcc-v1-review-{study[:5]}-{scenario}-{folder.name}-{short(model, effort)}"
        reviews.append(Review(study, scenario, output, container, run_folder=folder))
    return reviews


def all_runs(study: str, scenario: str | None, arm: str | None) -> list[Path]:
    runs = TCC_V1 / study / "runs"
    return sorted(folder.parent for folder in runs.glob("*/*/meta.json")
                  if (scenario is None or folder.parent.parent.name.startswith(scenario))
                  and (arm is None or folder.parent.name.startswith(f"{arm}-")))


def calibration_reviews(names: list[str], model: str, effort: str) -> list[Review]:
    cases_file = CALIBRATION_CASES / "cases.json"
    if not cases_file.is_file():
        die(f"{cases_file} not found")
    cases = json.loads(cases_file.read_text(encoding="utf-8"))
    reviews = []
    for case in cases:
        if names and case["name"] not in names:
            continue
        for study in case["studies"]:
            output = CALIBRATION / short(model, effort) / study / case["name"]
            container = f"tcc-v1-calibration-{study[:5]}-{case['name']}-{short(model, effort)}"
            # the expected grades, the same in both studies, or one set per study
            expected = case["expected"].get(study, case["expected"])
            reviews.append(Review(study, case["scenario"], output, container,
                                  patch=CALIBRATION_CASES / case["patch"], expected=expected))
    # a shuffled order, so the reviewer's position in a batch says nothing about the case
    random.Random(20261021).shuffle(reviews)
    return reviews


def calibration_summary(reviews: list[Review], model: str, effort: str) -> None:
    lines = [f"# A calibração do revisor de qualidade: {model}, effort {effort}", "",
             "| pesquisa | caso | coluna | esperado | revisor | confere |", "|---|---|---|---|---|---|"]
    right = total = 0
    costs = []
    for review in sorted(reviews, key=lambda review: (review.study, review.output.name)):
        meta_file = review.output / "meta.json"
        meta = json.loads(meta_file.read_text(encoding="utf-8")) if meta_file.is_file() else {}
        if meta.get("cost_usd") is not None:
            costs.append(meta["cost_usd"])
        grades = meta.get("grades", {})
        for column, expected in review.expected.items():
            given = grades.get(column)
            total += 1
            right += given == expected
            lines.append(f"| {review.study} | {review.output.name} | {column} | {expected} | {given} | "
                         f"{'sim' if given == expected else 'NÃO'} |")
    lines += ["", f"**{right} de {total}** conferem. Custo: US$ {sum(costs):.2f}."]
    text = LINE_BREAK.join(lines) + LINE_BREAK
    (CALIBRATION / short(model, effort) / "summary.md").write_text(text, encoding="utf-8")
    print(text)


def summary() -> None:
    """One line per reviewed run, and how many got `bom` per study, arm and column."""
    rows = []
    for meta_file in sorted(RESULTS.glob("*/*/*/meta.json")):
        meta = json.loads(meta_file.read_text(encoding="utf-8"))
        arm, model = meta_file.parent.name.split("-", 1)
        rows.append((meta["study"], meta["scenario"], arm, model, meta.get("grades", {}),
                     meta.get("scripts_skipped_by_reviewer", [])))
    columns = ("architecture", "design_class", "design", "tests", "database", "behavior")
    lines = ["# O verificador de qualidade nas execuções", "",
             "| pesquisa | cenário | braço | modelo | arquitetura | pattern | design | testes | banco | "
             "comportamento | scripts pulados |", "|---|---|---|---|---|---|---|---|---|---|---|"]
    for study, scenario, arm, model, grades, skipped in rows:
        lines.append(f"| {STUDY_NAMES[study]} | {scenario} | {arm} | {model} | "
                     + " | ".join(str(grades.get(column)) for column in columns)
                     + f" | {', '.join(skipped) or '-'} |")
    lines += ["", "## Quantos `bom` (e, no pattern, acerto + alternativa), por pesquisa, braço e coluna", "",
              "| pesquisa | braço | execuções | arquitetura | pattern | design | testes | banco | comportamento |",
              "|---|---|---|---|---|---|---|---|---|"]
    for study in STUDIES:
        for arm in sorted({row[2] for row in rows if row[0] == study}):
            group = [row[4] for row in rows if row[0] == study and row[2] == arm]
            counts = []
            for column in columns:
                good = ("acerto", "alternativa equivalente") if column == "design_class" else ("bom",)
                counts.append(str(sum(grades.get(column) in good for grades in group)))
            lines.append(f"| {STUDY_NAMES[study]} | {arm} | {len(group)} | " + " | ".join(counts) + " |")
    text = LINE_BREAK.join(lines) + LINE_BREAK
    RESULTS.mkdir(parents=True, exist_ok=True)
    (RESULTS / "summary.md").write_text(text, encoding="utf-8")
    print(text)


def option(arguments: list[str], name: str, default: str) -> str:
    if name in arguments:
        position = arguments.index(name)
        value = arguments[position + 1]
        del arguments[position:position + 2]
        return value
    return default


def main() -> None:
    arguments = sys.argv[1:]
    dry_run = "--dry-run" in arguments
    resume = "--resume" in arguments
    arguments = [argument for argument in arguments if argument not in ("--dry-run", "--resume")]
    model = option(arguments, "--model", "claude-opus-5-5")
    effort = option(arguments, "--effort", "high")
    workers = int(option(arguments, "--workers", "1"))
    scenario = option(arguments, "--scenario", "") or None
    arm = option(arguments, "--arm", "") or None
    if not arguments:
        die(__doc__ or "")
    command, rest = arguments[0], arguments[1:]
    if command == "review" and rest:
        run_all(run_reviews([Path(folder) for folder in rest], model, effort), model, effort, workers, dry_run,
                resume)
    elif command == "review-all" and len(rest) == 1 and rest[0] in STUDIES:
        folders = all_runs(rest[0], scenario, arm)
        if not folders:
            die(f"no finished run in {rest[0]}/runs")
        run_all(run_reviews(folders, model, effort), model, effort, workers, dry_run, resume)
    elif command == "calibrate":
        reviews = calibration_reviews(rest, model, effort)
        if not reviews:
            die("no calibration case matches")
        run_all(reviews, model, effort, workers, dry_run, resume)
        if not dry_run:
            calibration_summary(reviews, model, effort)
    elif command == "summary" and not rest:
        summary()
    else:
        die(__doc__ or "")


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8")  # type: ignore[union-attr]
    main()
