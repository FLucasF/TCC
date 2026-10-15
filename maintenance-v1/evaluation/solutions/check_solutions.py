"""The discrimination check of one scenario (the "discrimination power" of Akita's benchmark): every reference
solution of the scenario must pass the whole suite, and the untouched base must fail the scenario's acceptance.
Passing the suite says nothing about the design: the pattern solution and the naive one both pass, and telling
them apart is the evaluator's job.

Usage (from maintenance-v1):  python evaluation/solutions/check_solutions.py <scenario> <output-folder>
For example:          python evaluation/solutions/check_solutions.py 01 solutions-out

The solutions are patches over the base, named <scenario>-<kind>.patch (01-pattern.patch, 01-naive.patch). Each one
is applied to a clean copy of the base in <output-folder>/<kind>/solution, and the suite runs on it
(its output in <output-folder>/<kind>/suite).
"""

import json
import shutil
import subprocess
import sys
from pathlib import Path

MAINTENANCE_ROOT = Path(__file__).resolve().parents[2]
BASE = MAINTENANCE_ROOT / "experiment" / "base" / "hotel"
SOLUTIONS = MAINTENANCE_ROOT / "evaluation" / "solutions"
RUN_SUITE = MAINTENANCE_ROOT / "evaluation" / "suite" / "run_suite.py"


def apply_solution(patch: Path, project: Path) -> None:
    if project.exists():
        shutil.rmtree(project)
    shutil.copytree(BASE, project, ignore=shutil.ignore_patterns("target"))
    applied = subprocess.run(["git", "apply", "--whitespace=nowarn", str(patch)], cwd=project,
                             capture_output=True, text=True, check=False)
    if applied.returncode != 0:
        sys.exit(f"ERROR: {patch.name} does not apply to the base:\n{applied.stderr}")


def run_suite(project: Path, scenario: str, output_folder: Path) -> dict[str, list[dict[str, object]]]:
    subprocess.run([sys.executable, str(RUN_SUITE), str(project), scenario, str(output_folder)],
                   capture_output=True, check=False)
    result = json.loads((output_folder / "suite-result.json").read_text(encoding="utf-8"))
    groups: dict[str, list[dict[str, object]]] = result.get("groups", {})
    return groups


def failures(groups: dict[str, list[dict[str, object]]], group_name: str) -> list[str]:
    return [str(outcome["check"]) for outcome in groups.get(group_name, []) if not outcome["passed"]]


def main() -> None:
    if len(sys.argv) != 3:
        sys.exit(__doc__)
    scenario, output_folder = sys.argv[1], Path(sys.argv[2]).resolve()
    patches = sorted(SOLUTIONS.glob(f"{scenario}-*.patch"))
    if not patches:
        sys.exit(f"ERROR: no solution for scenario {scenario} in {SOLUTIONS}")

    all_as_expected = True
    base_groups = run_suite(BASE, scenario, output_folder / "base")
    acceptance_count = len(base_groups.get("acceptance", []))
    base_ok = not failures(base_groups, "regression") and len(failures(base_groups, "acceptance")) == acceptance_count
    all_as_expected &= base_ok and acceptance_count > 0
    print(f"base: regression {'passes' if not failures(base_groups, 'regression') else 'FAILS'}, "
          f"acceptance fails {len(failures(base_groups, 'acceptance'))} of {acceptance_count} "
          f"({'as expected' if base_ok else 'NOT as expected: the base must fail every acceptance check'})")

    for patch in patches:
        kind = patch.stem.split("-", 1)[1]
        project = output_folder / kind / "solution"
        apply_solution(patch, project)
        groups = run_suite(project, scenario, output_folder / kind / "suite")
        failed = failures(groups, "regression") + failures(groups, "acceptance")
        built = bool(groups)
        passed = built and not failed
        all_as_expected &= passed
        detail = "the service did not build" if not built else (", ".join(failed) if failed else "")
        print(f"{kind}: {'passes the whole suite' if passed else 'FAILS: ' + detail}")

    print("as expected" if all_as_expected else "NOT as expected")
    sys.exit(0 if all_as_expected else 1)


if __name__ == "__main__":
    main()
