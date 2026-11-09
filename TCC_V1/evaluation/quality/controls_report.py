"""The negative controls of the maintenance study: how many runs kept the change small (contenção) and how many built a
structure nobody asked for (exagero), per arm, scenario and model, next to the scenarios that ask for the pattern. Reads
only results/maintenance and the runs' meta.json; the text goes to stdout, in Portuguese.

Usage (from TCC_V1):
    python evaluation/quality/controls_report.py > ../docs/controles-negativos.md
"""

import json
import sys
from dataclasses import dataclass
from pathlib import Path

QUALITY = Path(__file__).resolve().parent
RESULTS = QUALITY / "results" / "maintenance"
RUNS = QUALITY.parents[1] / "maintenance" / "runs"
ARMS = ("N0", "N1", "N2")
MODELS = {"haiku-4-5": "Haiku 4.5", "sonnet-4-5": "Sonnet 4.5", "sonnet-5-5": "Sonnet 5.5"}
SCENARIOS = {"02-strategy-control": "02 temporada (Strategy)", "04-observer-control": "04 e-mail (Observer)",
             "06-factory-method-control": "06 crianças (Factory Method)",
             "08-decorator-control": "08 assinatura (Decorator)",
             "10-template-method-control": "10 coluna (Template Method)"}
POSITIVE = ("01-strategy", "03-observer", "05-factory-method", "07-decorator", "09-template-method")
RIGHT_IN_POSITIVE = ("acerto", "alternativa equivalente")


@dataclass(frozen=True)
class Run:
    scenario: str
    arm: str
    model: str
    design_class: str
    behavior: str
    build: int


def read_runs(scenarios: list[str]) -> list[Run]:
    runs = []
    for scenario in scenarios:
        for grades_file in sorted((RESULTS / scenario).glob("*/review.json")):
            name = grades_file.parent.name
            grades = json.loads(grades_file.read_text(encoding="utf-8"))
            meta = json.loads((RUNS / scenario / name / "meta.json").read_text(encoding="utf-8"))
            arm, model = name.split("-", 1)
            runs.append(Run(scenario, arm, model.split("-r")[0], str(grades.get("design_class")),
                            str(grades.get("behavior")), int(meta["build_exit_code"])))
    return runs


def table(header: list[str], rows: list[list[str]]) -> list[str]:
    return ["| " + " | ".join(header) + " |", "|" + "---|" * len(header),
            *("| " + " | ".join(row) + " |" for row in rows), ""]


def contained(runs: list[Run]) -> str:
    return f"{sum(run.design_class == 'contenção' for run in runs)} de {len(runs)}"


def main() -> None:
    controls = read_runs(list(SCENARIOS))
    positive = read_runs(list(POSITIVE))
    lines = ["# Os controles negativos da manutenção", "",
             "Gerado por `TCC_V1/evaluation/quality/controls_report.py`. Em cada controle, a resposta certa é a "
             "mudança pequena (`contenção`); `exagero` é criar uma estrutura que ninguém pediu.", "",
             f"Execuções revisadas: {len(controls)}.", "", "## Contenção por nível", ""]
    lines += table(["", *ARMS], [
        ["contenção nos controles", *(contained([r for r in controls if r.arm == arm]) for arm in ARMS)],
        ["*pattern* certo nos cenários que pedem",
         *(f"{sum(r.design_class in RIGHT_IN_POSITIVE for r in positive if r.arm == arm)} de "
           f"{sum(r.arm == arm for r in positive)}" for arm in ARMS)],
        ["comportamento `bom` nos controles",
         *(f"{sum(r.behavior == 'bom' for r in controls if r.arm == arm)} de {sum(r.arm == arm for r in controls)}"
           for arm in ARMS)],
        ["`mvn verify` passa nos controles",
         *(f"{sum(r.build == 0 for r in controls if r.arm == arm)} de {sum(r.arm == arm for r in controls)}"
           for arm in ARMS)]])
    lines += ["## Contenção por cenário e nível", ""]
    lines += table(["cenário", *ARMS], [
        [title, *(contained([r for r in controls if r.scenario == scenario and r.arm == arm]) for arm in ARMS)]
        for scenario, title in SCENARIOS.items()])
    lines += ["## Contenção por modelo e nível", ""]
    lines += table(["modelo", *ARMS], [
        [title, *(contained([r for r in controls if r.model == model and r.arm == arm]) for arm in ARMS)]
        for model, title in MODELS.items()])
    exaggerated = [r for r in controls if r.design_class != "contenção"]
    lines += ["## As execuções com exagero", ""]
    lines += table(["cenário", "nível", "modelo", "classe"], [
        [SCENARIOS[r.scenario], r.arm, MODELS[r.model], r.design_class] for r in exaggerated]) if exaggerated \
        else ["Nenhuma.", ""]
    print("\n".join(lines))


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8")  # type: ignore[union-attr]
    main()
