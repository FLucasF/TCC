"""Runs the suite against one copy of the hotel: the regression (what the base already did keeps working) and the
acceptance of one scenario (what that change request asked for works).

Usage (from maintenance-v1):  python evaluation/suite/run_suite.py <project> <scenario> <output-folder>
For example:          python evaluation/suite/run_suite.py experiment/base/hotel 00 suite-out/base
                      python evaluation/suite/run_suite.py runs/<id>/workspace 01 runs/<id>/suite

<scenario> is the two-digit number of the change request (01 to 10), or 00 for the base itself (regression only).
<project> is never touched: Maven builds a copy inside <output-folder>.

Output, in <output-folder>: suite-result.json (every check, with the reason of each failure), suite-report.txt (what
is printed), build.log (the agent's `mvn verify`, and the packaging when its tests fail) and service.log.
"""

import json
import sys
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

import acceptance  # noqa: E402
from checks import Check, CheckFailed  # noqa: E402
from hotel_service import HotelLauncher, build_agent_copy  # noqa: E402
from regression import regression_checks  # noqa: E402

KNOWN_SCENARIOS = ["00", *(f"{number:02d}" for number in range(1, 11))]


def run_check(check: Check, hotel: HotelLauncher) -> dict[str, object]:
    started = time.monotonic()
    try:
        check.run(hotel)
        passed, reason = True, ""
    except CheckFailed as failure:
        passed, reason = False, str(failure)
    except Exception as error:  # a check that crashes (a missing JSON field, a service that died) is a failure too
        passed, reason = False, f"{type(error).__name__}: {error}"
    return {"check": check.name, "passed": passed, "reason": reason,
            "seconds": round(time.monotonic() - started, 1)}


def main() -> None:
    if len(sys.argv) != 4 or sys.argv[2] not in KNOWN_SCENARIOS:
        sys.exit(__doc__)
    project, scenario, output_folder = Path(sys.argv[1]).resolve(), sys.argv[2], Path(sys.argv[3]).resolve()
    output_folder.mkdir(parents=True, exist_ok=True)

    build = build_agent_copy(project, output_folder, output_folder / "build.log")
    result: dict[str, object] = {"project": str(project), "scenario": scenario,
                                 "agent_tests_passed": build.agent_tests_passed, "service_built": build.jar is not None}
    report_lines = [f"project: {project}", f"scenario: {scenario}",
                    f"agent's mvn verify: {'passed' if build.agent_tests_passed else 'FAILED'}"]
    if build.jar is None:
        report_lines.append("the service did not build: no check ran")
        result["groups"] = {}
    else:
        hotel = HotelLauncher(build.jar, output_folder / "service.log")
        groups = {"regression": regression_checks(scenario), "acceptance": acceptance.checks_for(scenario)}
        group_results: dict[str, list[dict[str, object]]] = {}
        for group_name, checks in groups.items():
            group_results[group_name] = [run_check(check, hotel) for check in checks]
            passed = sum(1 for outcome in group_results[group_name] if outcome["passed"])
            report_lines.append(f"\n{group_name}: {passed} of {len(checks)} passed")
            for outcome in group_results[group_name]:
                mark = "ok  " if outcome["passed"] else "FAIL"
                reason = f"  {outcome['reason']}" if outcome["reason"] else ""
                report_lines.append(f"  {mark} {outcome['check']}{reason}")
        result["groups"] = group_results

    (output_folder / "suite-result.json").write_text(json.dumps(result, indent=2, ensure_ascii=False) + "\n",
                                                     encoding="utf-8")
    report_text = "\n".join(report_lines) + "\n"
    (output_folder / "suite-report.txt").write_text(report_text, encoding="utf-8")
    print(report_text, end="")


if __name__ == "__main__":
    main()
