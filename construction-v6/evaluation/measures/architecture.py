"""The architecture of the agent's service: three rules that do not depend on taste, checked by ArchUnit on the agent's
compiled classes (the rules are in evaluation/reference/.../architecture/ArchitectureRules.java):

  packages_in_cycles      how many of the application's packages are caught in some dependency cycle (the main
                          number: ArchUnit reports every path as a cycle, so one pair a <-> b can become many);
  package_cycles          the cycles as ArchUnit counts them (every path);
  controller_dependents   classes outside the web layer that depend on a controller;
  incoming_web_users      classes outside the web layer that use the API of the incoming web (Spring MVC's
                          annotations and servlet types, ResponseEntity): the business code knows it is called over
                          HTTP. The outgoing web (RestClient, to call the TransLog) is not counted.

Usage (from TCC_V6):  python evaluation/measures/architecture.py <runs-folder> <output.csv>

The agent's workspace is never touched: a copy is compiled in the pinned image (agent_project.py), and the rules read
the classes of that build.
"""

import csv
import json
import subprocess
import sys
import tempfile
from dataclasses import asdict, dataclass
from typing import Any
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
sys.path.insert(0, str(HERE.parent / "reference"))

from agent_project import find_project_folder, run_maven  # noqa: E402
from run_suite import compile_the_suite  # noqa: E402  (the reference project: the rules live there)


@dataclass
class ArchitectureResult:
    run: str
    situation: str = ""
    classes: int = 0
    packages_in_cycles: int = 0
    package_cycles: int = 0
    controller_dependents: int = 0
    incoming_web_users: int = 0
    examples: str = ""


def compiled_classes(project: Path, replaced_files: dict[str, str] | None = None) -> dict[str, bytes] | None:
    """The .class files of a build of the project's copy (path inside target/classes -> bytes), or None if it does
    not compile."""
    maven = run_maven(project, "-q -DskipTests compile", "target/classes", replaced_files)
    if maven.exit_code != 0:
        return None
    return {name.removeprefix("target/classes/"): data for name, data in maven.files.items() if name.endswith(".class")}


@dataclass
class RulesOutput:
    """What ArchitectureRules.java wrote, read and checked field by field (the JSON comes from another process)."""

    classes: int
    packages_in_cycles: int
    package_cycles: int
    controller_dependents: int
    incoming_web_users: int
    examples: list[str]

    @staticmethod
    def from_json(raw: Any) -> "RulesOutput":
        def number(key: str) -> int:
            value = raw.get(key) if isinstance(raw, dict) else None
            if not isinstance(value, int):
                raise ValueError(f"the rules' output has no number '{key}': {raw!r:.200}")
            return value

        def texts(key: str) -> list[str]:
            value = raw.get(key)
            if not isinstance(value, list) or not all(isinstance(item, str) for item in value):
                raise ValueError(f"the rules' output has no list of texts '{key}'")
            return value

        return RulesOutput(number("classes"), number("packagesInCycles"), number("packageCycles"),
                           number("controllerDependents"),
                           number("incomingWebUsers"),
                           texts("packageCycleExamples") + texts("controllerDependentExamples")
                           + texts("incomingWebUserExamples"))


def check_rules(class_files: dict[str, bytes], classpath: str) -> RulesOutput:
    with tempfile.TemporaryDirectory(prefix="architecture-") as temporary:
        classes_folder = Path(temporary) / "classes"
        for name, data in class_files.items():
            (classes_folder / name).parent.mkdir(parents=True, exist_ok=True)
            (classes_folder / name).write_bytes(data)
        result_file = Path(temporary) / "result.json"
        command = ["java", "-cp", classpath, "reference.architecture.ArchitectureRules",
                   "--classes", str(classes_folder), "--out", str(result_file)]
        finished = subprocess.run(command, capture_output=True, text=True, encoding="utf-8", errors="replace",
                                  check=False)
        if finished.returncode != 0:
            raise RuntimeError(f"the architecture rules failed: {finished.stderr[-1500:]}")
        return RulesOutput.from_json(json.loads(result_file.read_text(encoding="utf-8")))


def measure(run_folder: Path, classpath: str, replaced_files: dict[str, str] | None = None) -> ArchitectureResult:
    result = ArchitectureResult(run_folder.name)
    project = find_project_folder(run_folder / "workspace")
    if project is None:
        result.situation = "no-project"
        return result
    class_files = compiled_classes(project, replaced_files)
    if class_files is None:
        result.situation = "not-compiled"
        return result
    rules = check_rules(class_files, classpath)
    result.situation = "ok"
    result.classes = rules.classes
    result.packages_in_cycles = rules.packages_in_cycles
    result.package_cycles = rules.package_cycles
    result.controller_dependents = rules.controller_dependents
    result.incoming_web_users = rules.incoming_web_users
    result.examples = " | ".join(rules.examples)
    return result


def main() -> None:
    if len(sys.argv) != 3:
        sys.exit("usage: python architecture.py <runs-folder> <output.csv>")
    runs_folder, output_csv = Path(sys.argv[1]), Path(sys.argv[2])
    if output_csv.exists():
        sys.exit(f"{output_csv} already exists: a result is never written over another")
    classpath = compile_the_suite()
    results = []
    for run_folder in sorted(folder for folder in runs_folder.iterdir() if (folder / "workspace").is_dir()):
        result = measure(run_folder, classpath)
        results.append(result)
        print(f"{result.run}: {result.situation}, {result.classes} classes, {result.packages_in_cycles} packages in "
              f"{result.package_cycles} cycles, "
              f"{result.controller_dependents} controller dependents, {result.incoming_web_users} incoming-web users",
              flush=True)
    with output_csv.open("w", encoding="utf-8", newline="") as file:
        writer = csv.DictWriter(file, fieldnames=list(ArchitectureResult.__dataclass_fields__), lineterminator="\n")
        writer.writeheader()
        writer.writerows(asdict(result) for result in results)
    print(f"{len(results)} runs -> {output_csv}")


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8")  # type: ignore[union-attr]
    main()
