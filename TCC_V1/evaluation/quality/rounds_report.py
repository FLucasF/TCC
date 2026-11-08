"""The five rounds of TCC_V1 side by side, from what the reviewer and the CK wrote: the pattern class round by round,
how often the first round repeats, the good runs per dimension, the overengineering and the object-oriented metrics.
Reads only (results/, oo-metrics/oo-metrics.csv and the runs' meta.json); the text goes to stdout, in Portuguese, like
the other reports.

Usage (from TCC_V1):
    python evaluation/quality/rounds_report.py > ../docs/rodadas-cinco-patterns.md
"""

import csv
import json
import sys
from collections import Counter, defaultdict
from dataclasses import dataclass
from pathlib import Path

QUALITY = Path(__file__).resolve().parent
TCC_V1 = QUALITY.parents[1]
RESULTS = QUALITY / "results"
CK_FILE = TCC_V1 / "evaluation" / "oo-metrics" / "oo-metrics.csv"

STUDIES = {"construction": "Construir", "maintenance": "Manter"}
SCENARIOS = {"01-strategy": "01 Strategy", "03-observer": "03 Observer", "05-factory-method": "05 Factory Method",
             "07-decorator": "07 Decorator", "09-template-method": "09 Template Method"}
MODELS = {"haiku-4-5": "Haiku 4.5", "sonnet-4-5": "Sonnet 4.5", "sonnet-5-5": "Sonnet 5.5"}
ARMS = ("N0", "N1", "N2")
ROUNDS = (1, 2, 3, 4, 5)
SYMBOLS = {"acerto": "✅", "alternativa equivalente": "🔷", "malfeito": "⚠️", "omissão": "❌"}
CLASSES = tuple(SYMBOLS)
RIGHT = ("acerto", "alternativa equivalente")
DIMENSIONS = {"design": "design do resto do código", "architecture": "arquitetura", "tests": "testes",
              "database": "banco", "behavior": "comportamento"}
CK_COLUMNS = {"ck_types": "tipos com nome", "ck_interface": "interfaces", "ck_abstract_methods": "métodos abstratos",
              "ck_wmc_max": "WMC da maior classe", "ck_cbo_mean": "CBO médio"}


@dataclass(frozen=True)
class Run:
    study: str
    scenario: str
    model: str
    arm: str
    round: int
    grades: dict[str, str]
    cost_usd: float
    ck: dict[str, float] | None

    @property
    def pattern_class(self) -> str:
        return self.grades["design_class"]

    @property
    def right(self) -> bool:
        return self.pattern_class in RIGHT


def read_ck() -> dict[tuple[str, str, str, str, int], dict[str, float]]:
    measures = {}
    with CK_FILE.open(encoding="utf-8", newline="") as file:
        for row in csv.DictReader(file):
            if row["status"] != "ok":
                continue
            values = {name: float(row[name]) for name in CK_COLUMNS}
            measures[(row["study"], row["scenario"], row["arm"], row["model"], int(row["replica"]))] = values
    return measures


def read_runs() -> list[Run]:
    ck = read_ck()
    runs = []
    for study in STUDIES:
        for scenario in SCENARIOS:
            for model in MODELS:
                for arm in ARMS:
                    for number in ROUNDS:
                        name = f"{arm}-{model}" + (f"-r{number}" if number > 1 else "")
                        meta = TCC_V1 / study / "runs" / scenario / name / "meta.json"
                        grades = RESULTS / study / scenario / name / "review.json"
                        runs.append(Run(study, scenario, model, arm, number,
                                        json.loads(grades.read_text(encoding="utf-8")),
                                        float(json.loads(meta.read_text(encoding="utf-8")).get("cost_usd") or 0),
                                        ck.get((study, scenario, arm, model, number))))
    return runs


def table(header: list[str], rows: list[list[str]]) -> list[str]:
    return ["| " + " | ".join(header) + " |", "|" + "---|" * len(header),
            *("| " + " | ".join(row) + " |" for row in rows), ""]


def comma(value: float, digits: int) -> str:
    return f"{value:.{digits}f}".replace(".", ",")


def right_count(runs: list[Run]) -> str:
    return f"{sum(run.right for run in runs)} de {len(runs)}"


def round_by_round(runs: list[Run]) -> list[str]:
    lines = []
    for study, study_name in STUDIES.items():
        rows = []
        for scenario, scenario_name in SCENARIOS.items():
            for model, model_name in MODELS.items():
                for arm in ARMS:
                    cell = sorted((run for run in runs if (run.study, run.scenario, run.model, run.arm)
                                   == (study, scenario, model, arm)), key=lambda run: run.round)
                    repeats = sum(run.right == cell[0].right for run in cell[1:])
                    rows.append([scenario_name, model_name, arm, " ".join(SYMBOLS[run.pattern_class] for run in cell),
                                 right_count(cell), f"{repeats} de 4"])
        lines += [f"### {study_name}", "",
                  *table(["cenário", "modelo", "braço", "rodadas 1 a 5", "acertos", "a rodada 1 se repete"], rows)]
    return lines


def pattern_totals(runs: list[Run]) -> list[str]:
    rows = []
    for scenario, scenario_name in SCENARIOS.items():
        for study, study_name in STUDIES.items():
            rows.append([scenario_name, study_name.lower(),
                         *(right_count([run for run in runs if (run.scenario, run.study, run.arm)
                                        == (scenario, study, arm)]) for arm in ARMS)])
    for study, study_name in STUDIES.items():
        rows.append(["todos", study_name.lower(),
                     *(right_count([run for run in runs if (run.study, run.arm) == (study, arm)]) for arm in ARMS)])
    return ["### O pattern certo por cenário e braço", "", *table(["pattern", "tarefa", *ARMS], rows)]


def model_totals(runs: list[Run]) -> list[str]:
    rows = []
    for model, model_name in MODELS.items():
        for study, study_name in STUDIES.items():
            rows.append([model_name, study_name.lower(),
                         *(right_count([run for run in runs if (run.model, run.study, run.arm)
                                        == (model, study, arm)]) for arm in ARMS)])
    return ["### O pattern certo por modelo e braço", "", *table(["modelo", "tarefa", *ARMS], rows)]


def stability(runs: list[Run]) -> list[str]:
    cells: dict[tuple[str, str, str, str], list[Run]] = defaultdict(list)
    for run in runs:
        cells[(run.study, run.scenario, run.model, run.arm)].append(run)
    repeats = 0
    unanimous = 0
    arm_repeats: Counter[str] = Counter()
    model_repeats: Counter[str] = Counter()
    for (_, _, model, arm), cell in cells.items():
        cell.sort(key=lambda run: run.round)
        same = sum(run.right == cell[0].right for run in cell[1:])
        repeats += same
        arm_repeats[arm] += same
        model_repeats[model] += same
        unanimous += len({run.right for run in cell}) == 1
    new_rounds = len(runs) - len(cells)
    return ["### Estabilidade", "",
            f"A rodada 1 (pattern certo ou não) se repete em {repeats} de {new_rounds} rodadas novas "
            f"({comma(100 * repeats / new_rounds, 0)}%).",
            f"Combinações com as 5 rodadas iguais (todas certas ou todas erradas): {unanimous} de {len(cells)}.",
            "Repetições por braço, de 120: " + ", ".join(f"{arm} {arm_repeats[arm]}" for arm in ARMS) + ".",
            "Repetições por modelo, de 120: " + ", ".join(f"{MODELS[model]} {model_repeats[model]}"
                                                          for model in MODELS) + ".", ""]


def good_runs(runs: list[Run], title: str) -> list[str]:
    def row(name: str, is_good: str | None) -> list[str]:
        values = []
        for study in STUDIES:
            for arm in ARMS:
                cell = [run for run in runs if (run.study, run.arm) == (study, arm)]
                values.append(str(sum(run.right if is_good is None else run.grades[is_good] == "bom" for run in cell)))
        return [name, *values]
    size = len([run for run in runs if (run.study, run.arm) == ("construction", "N0")])
    return [f"### {title} ({size} por braço; no pattern, acerto ou alternativa)", "",
            *table(["dimensão", "Construir N0", "N1", "N2", "Manter N0", "N1", "N2"],
                   [row("pattern", None), *(row(name, key) for key, name in DIMENSIONS.items())])]


def overengineering(runs: list[Run]) -> list[str]:
    rows = []
    for study, study_name in STUDIES.items():
        for arm in ARMS:
            cell = [run for run in runs if (run.study, run.arm) == (study, arm)]
            exaggerated = [run for run in cell if run.grades["design_overengineering"] == "exagero"]
            per_model = Counter(MODELS[run.model] for run in exaggerated)
            rows.append([study_name, arm, f"{len(exaggerated)} de {len(cell)}",
                         ", ".join(f"{name} {count}" for name, count in sorted(per_model.items())) or "nenhum"])
    return ["### O exagero", "", *table(["tarefa", "braço", "execuções com exagero", "por modelo"], rows)]


def ck_row(runs: list[Run]) -> list[str]:
    measured = [run.ck for run in runs if run.ck is not None]
    return [comma(sum(values[column] for values in measured) / len(measured), 2) if measured else "-"
            for column in CK_COLUMNS]


def ck_by_arm(runs: list[Run], title: str) -> list[str]:
    columns = [ck_row([run for run in runs if (run.study, run.arm) == (study, arm)])
               for study in STUDIES for arm in ARMS]
    rows = [[name, *(column[index] for column in columns)] for index, name in enumerate(CK_COLUMNS.values())]
    return [f"### {title}", "", *table(["medida", "Construir N0", "N1", "N2", "Manter N0", "N1", "N2"], rows)]


def ck_by_class(runs: list[Run]) -> list[str]:
    rows = []
    for study, study_name in STUDIES.items():
        for pattern_class in CLASSES:
            cell = [run for run in runs if (run.study, run.pattern_class) == (study, pattern_class)]
            rows.append([study_name.lower(), pattern_class, str(sum(run.ck is not None for run in cell)),
                         *ck_row(cell)])
    return ["### O CK pela classe do pattern (só as execuções com código)", "",
            *table(["tarefa", "classe", "execuções", *CK_COLUMNS.values()], rows)]


def costs(runs: list[Run]) -> list[str]:
    rows = [[MODELS[model], *(comma(sum(run.cost_usd for run in runs if (run.model, run.study) == (model, study)), 2)
                              for study in STUDIES)] for model in MODELS]
    return ["### O custo das execuções (US$)", "", *table(["modelo", "construir", "manter"], rows)]


def main() -> None:
    runs = read_runs()
    pilot = [run for run in runs if run.round == 1]
    lines = ["# As cinco rodadas dos cinco patterns", "",
             f"{len(runs)} execuções: duas tarefas, cinco cenários, três modelos, três braços e cinco rodadas. Gerado "
             "por `TCC_V1/evaluation/quality/rounds_report.py`. Símbolos: ✅ acerto, 🔷 alternativa equivalente, "
             "⚠️ malfeito, ❌ omissão; os acertos contam ✅ e 🔷.", "",
             *round_by_round(runs), *pattern_totals(runs), *model_totals(runs), *stability(runs),
             *good_runs(runs, "Execuções boas nas cinco rodadas"), *good_runs(pilot, "Execuções boas só na rodada 1"),
             *overengineering(runs), *ck_by_arm(runs, "O CK por braço, nas cinco rodadas (médias)"),
             *ck_by_arm(pilot, "O CK por braço, só na rodada 1 (médias)"), *ck_by_class(runs), *costs(runs)]
    print("\n".join(lines).rstrip())


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8")  # type: ignore[union-attr]
    main()
