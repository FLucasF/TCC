"""The second verifier: which runs it reviews and how much it agrees with the official verdicts. The runs are always the
same: one round drawn for each combination of study, scenario, arm and model, with a fixed seed, then shuffled; the
first N are the sample. The draw looks at nothing but the run names, and the verifier never sees this list. The reviews
themselves go through review_runs.py --second-verifier, and they never replace the official verdicts.

Usage (from TCC_V1):
    python evaluation/quality/second_verifier.py sample <N>
    python evaluation/quality/review_runs.py review $(python evaluation/quality/second_verifier.py sample 3) \\
        --model claude-fable-5-1 --second-verifier --resume --workers 3
    python evaluation/quality/second_verifier.py agreement fable-5-1-high
"""

import json
import random
import sys
from collections import Counter
from pathlib import Path

QUALITY = Path(__file__).resolve().parent
TCC_V1 = QUALITY.parents[1]
RESULTS = QUALITY / "results"
SECOND_VERIFIER = QUALITY / "second-verifier"
SEED = 20261103
STUDIES = ("construction", "maintenance")
COLUMNS = {"design_class": "classe do pattern", "design_overengineering": "exagero", "design": "design",
           "architecture": "arquitetura", "tests": "testes", "database": "banco", "behavior": "comportamento"}


def sample() -> list[str]:
    draw = random.Random(SEED)
    chosen = []
    for study in STUDIES:
        for scenario in sorted(folder for folder in (TCC_V1 / study / "runs").iterdir() if folder.is_dir()):
            rounds: dict[str, list[str]] = {}
            for meta in sorted(scenario.glob("*/meta.json")):
                combination = meta.parent.name.rsplit("-r", 1)[0]
                rounds.setdefault(combination, []).append(meta.parent.name)
            for combination in sorted(rounds):
                chosen.append(f"{study}/runs/{scenario.name}/{draw.choice(rounds[combination])}")
    draw.shuffle(chosen)
    return chosen


def kappa(pairs: list[tuple[str, str]]) -> float | None:
    """Cohen's kappa; None when both verifiers gave a single grade, the same one (chance explains all of it)."""
    observed = sum(first == second for first, second in pairs) / len(pairs)
    first_counts = Counter(first for first, _ in pairs)
    second_counts = Counter(second for _, second in pairs)
    chance = sum(first_counts[grade] * second_counts[grade] for grade in first_counts) / len(pairs) ** 2
    return None if chance == 1 else (observed - chance) / (1 - chance)


def agreement(verifier: str) -> None:
    pairs: dict[str, list[tuple[str, str]]] = {column: [] for column in COLUMNS}
    differences = []
    for grades_file in sorted((SECOND_VERIFIER / verifier).glob("*/*/*/review.json")):
        run = grades_file.parent.relative_to(SECOND_VERIFIER / verifier)
        second = json.loads(grades_file.read_text(encoding="utf-8"))
        official = json.loads((RESULTS / run / "review.json").read_text(encoding="utf-8"))
        for column, pair in pairs.items():
            pair.append((official[column], second[column]))
            if official[column] != second[column]:
                differences.append(f"- {run.as_posix()}, {COLUMNS[column]}: oficial {official[column]}, "
                                   f"segundo {second[column]}")
    print(f"| coluna | iguais, em {len(pairs['design'])} | kappa |\n|---|---|---|")
    for column, pair in pairs.items():
        value = kappa(pair)
        print(f"| {COLUMNS[column]} | {sum(first == second for first, second in pair)} | "
              f"{'-' if value is None else f'{value:.2f}'} |")
    total = sum(len(pair) for pair in pairs.values())
    print(f"\n{total - len(differences)} notas iguais de {total}.\n")
    print("\n".join(differences))


def main() -> None:
    if len(sys.argv) == 3 and sys.argv[1] == "sample":
        print("\n".join(sample()[:int(sys.argv[2])]))
    elif len(sys.argv) == 3 and sys.argv[1] == "agreement":
        agreement(sys.argv[2])
    else:
        sys.exit(__doc__)


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8")  # type: ignore[union-attr]
    main()
