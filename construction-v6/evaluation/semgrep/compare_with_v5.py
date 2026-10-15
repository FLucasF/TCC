"""Validates the Strategy part of the V6 detector (P1 to P5) against V5's detector, on V5's packages.

The V6 detector is a Python rewrite of V5's, with the Portuguese names as synonyms. On V5's 220 packages its
P1 to P5 answers must be the same as V5's frozen CSV, except where the V6 régua changed on purpose (the cross-rule
homes, the nested classes, the translation to the carrier's code). Every difference is listed with both
evidences, so each one is either explained by one of those changes or is a porting bug.

Usage:  python evaluation/semgrep/compare_with_v5.py <v5-semgrep.csv> <v6-semgrep.csv>
"""

import csv
import sys
from pathlib import Path

# V5's values, in Portuguese, and the same value in V6
V5_TO_V6 = {
    "isolado": "isolated", "espalhado": "scattered", "indeterminado": "undetermined",
    "consulta": "lookup", "condicional-unica": "single-conditional",
    "condicional-no-calculo": "conditional-in-calculation",
    "enum-dados": "enum-data", "enum-abstrato": "enum-body", "classes": "classes", "mapa": "map", "switch": "switch",
    "outro": "other", "dados": "data", "condicional": "conditional", "estrutura": "structure",
}
# V5's column -> V6's column
COLUMNS = {
    **{f"P{n}_localizacao": f"P{n}_location" for n in range(1, 5)},
    **{f"P{n}_selecao": f"P{n}_selection" for n in range(1, 5)},
    "P5_forma": "P5_form", "P5_proporcao": "P5_proportion",
}


def read_rows(path: Path, key_column: str) -> dict[str, dict[str, str]]:
    """The CSV's rows by package, skipping the first line (the hashes)."""
    lines = path.read_text(encoding="utf-8").splitlines()[1:]
    return {row[key_column]: row for row in csv.DictReader(lines)}


def main() -> None:
    if len(sys.argv) != 3:
        sys.exit("usage: python compare_with_v5.py <v5-semgrep.csv> <v6-semgrep.csv>")
    v5_rows = read_rows(Path(sys.argv[1]), "pacote")
    v6_rows = read_rows(Path(sys.argv[2]), "package")
    if v5_rows.keys() != v6_rows.keys():
        sys.exit(f"different packages: only in V5 {sorted(v5_rows.keys() - v6_rows.keys())}, "
                 f"only in V6 {sorted(v6_rows.keys() - v5_rows.keys())}")

    answers_compared = 0
    differences_by_column: dict[str, int] = {}
    for package in sorted(v5_rows):
        v5, v6 = v5_rows[package], v6_rows[package]
        different = []
        for v5_column, v6_column in COLUMNS.items():
            answers_compared += 1
            if V5_TO_V6[v5[v5_column]] != v6[v6_column]:
                different.append(f"{v6_column}: V5 {V5_TO_V6[v5[v5_column]]}, V6 {v6[v6_column]}")
                differences_by_column[v6_column] = differences_by_column.get(v6_column, 0) + 1
        if different:
            print(f"{package}: " + "; ".join(different))
            print(f"    V5 evidence: {v5['evidencia']}")
            print(f"    V6 evidence: {v6['evidence']}")
    total_differences = sum(differences_by_column.values())
    print(f"\n{len(v5_rows)} packages, {answers_compared} answers: {answers_compared - total_differences} equal, "
          f"{total_differences} different {dict(sorted(differences_by_column.items()))}")


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8")  # type: ignore[union-attr]
    main()
