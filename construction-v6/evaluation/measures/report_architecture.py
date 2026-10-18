"""The architecture report of each run: the facts about the agent's packages that reading the code gets wrong (the
dependencies between packages, the cycles, the business classes that know the web), for the AI's correction. Nothing
here is a score: what a cycle means is the corrector's call. The facts are read by ArchUnit from the agent's compiled
classes (evaluation/reference/.../architecture/ArchitectureReport.java, which explains each one).

Usage (from construction-v6):  python evaluation/measures/report_architecture.py <runs-folder>
                               python evaluation/measures/report_architecture.py runs/pilots

For each run, it writes <run>/architecture/architecture.json (everything) and architecture-report.txt (the same, to
read). A run that already has the folder is skipped: a report is never written over another.

The agent's workspace is never touched: a copy is compiled in the pinned image (maven_on_agent_copy.py), and the
report reads the classes of that build.
"""

import json
import subprocess
import sys
import tempfile
from dataclasses import asdict, dataclass
from pathlib import Path
from typing import Any

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
sys.path.insert(0, str(HERE.parent / "reference"))

from maven_on_agent_copy import find_project_folder, run_maven  # noqa: E402
from run_suite_against_service import compile_the_suite  # noqa: E402  (the reference project: the report lives there)

REPORT_FOLDER = "architecture"


@dataclass
class PackageDependency:
    source: str                       # "from" in the JSON
    target: str                       # "to"
    class_dependencies: list[str]     # "a.X -> b.Y"


@dataclass
class Cycle:
    packages: list[str]
    dependencies: list[PackageDependency]


@dataclass
class ClassUses:
    class_name: str                   # "class" in the JSON
    uses: list[str]


@dataclass
class ArchitectureFacts:
    """What ArchitectureReport.java wrote, read and checked field by field (the JSON comes from another process)."""

    root_package: str
    classes: int
    root_package_classes: list[str]
    packages: dict[str, list[str]]
    dependencies: list[PackageDependency]
    cycles: list[Cycle]
    controller_dependents: list[ClassUses]
    incoming_web_users: list[ClassUses]
    javalin_users: list[ClassUses]    # every class that uses Javalin, the web layer included (none in older reports)

    @staticmethod
    def from_json(raw: Any) -> "ArchitectureFacts":
        def value(container: Any, key: str, kind: type) -> Any:
            found = container.get(key) if isinstance(container, dict) else None
            if not isinstance(found, kind):
                raise ValueError(f"the report's output has no {kind.__name__} '{key}': {container!r:.200}")
            return found

        def texts(container: Any, key: str) -> list[str]:
            found = value(container, key, list)
            if not all(isinstance(item, str) for item in found):
                raise ValueError(f"the report's output has a non-text item in '{key}'")
            return [str(item) for item in found]

        def dependency(item: Any) -> PackageDependency:
            return PackageDependency(value(item, "from", str), value(item, "to", str),
                                     texts(item, "classDependencies"))

        def class_uses(item: Any) -> ClassUses:
            return ClassUses(value(item, "class", str), texts(item, "uses"))

        return ArchitectureFacts(
            root_package=value(raw, "rootPackage", str),
            classes=value(raw, "classes", int),
            root_package_classes=texts(raw, "rootPackageClasses"),
            packages={value(item, "name", str): texts(item, "classes") for item in value(raw, "packages", list)},
            dependencies=[dependency(item) for item in value(raw, "dependencies", list)],
            cycles=[Cycle(texts(item, "packages"), [dependency(edge) for edge in value(item, "dependencies", list)])
                    for item in value(raw, "cycles", list)],
            controller_dependents=[class_uses(item) for item in value(raw, "controllerDependents", list)],
            incoming_web_users=[class_uses(item) for item in value(raw, "incomingWebUsers", list)],
            javalin_users=[class_uses(item)
                           for item in (raw.get("javalinUsers") or [] if isinstance(raw, dict) else [])])


@dataclass
class ArchitectureReport:
    run: str
    situation: str = ""               # ok, no-project, not-compiled
    facts: ArchitectureFacts | None = None


def compiled_classes(project: Path, replaced_files: dict[str, str] | None = None) -> dict[str, bytes] | None:
    """The .class files of a build of the project's copy (path inside target/classes -> bytes), or None if it does
    not compile."""
    maven = run_maven(project, "-q -DskipTests compile", "target/classes", replaced_files)
    if maven.exit_code != 0:
        return None
    return {name.removeprefix("target/classes/"): data for name, data in maven.files.items() if name.endswith(".class")}


def read_facts(class_files: dict[str, bytes], classpath: str) -> ArchitectureFacts:
    with tempfile.TemporaryDirectory(prefix="architecture-") as temporary:
        classes_folder = Path(temporary) / "classes"
        for name, data in class_files.items():
            (classes_folder / name).parent.mkdir(parents=True, exist_ok=True)
            (classes_folder / name).write_bytes(data)
        result_file = Path(temporary) / "result.json"
        command = ["java", "-cp", classpath, "reference.architecture.ArchitectureReport",
                   "--classes", str(classes_folder), "--out", str(result_file)]
        finished = subprocess.run(command, capture_output=True, text=True, encoding="utf-8", errors="replace",
                                  check=False)
        if finished.returncode != 0:
            raise RuntimeError(f"the architecture report failed: {finished.stderr[-1500:]}")
        return ArchitectureFacts.from_json(json.loads(result_file.read_text(encoding="utf-8")))


def describe(run_folder: Path, classpath: str, replaced_files: dict[str, str] | None = None) -> ArchitectureReport:
    report = ArchitectureReport(run_folder.name)
    project = find_project_folder(run_folder / "workspace")
    if project is None:
        report.situation = "no-project"
        return report
    class_files = compiled_classes(project, replaced_files)
    if class_files is None:
        report.situation = "not-compiled"
        return report
    report.situation = "ok"
    report.facts = read_facts(class_files, classpath)
    return report


def as_text(report: ArchitectureReport) -> str:
    """The report to read: the same facts as the JSON, laid out by question."""
    lines = [f"Architecture of {report.run}: {report.situation}"]
    facts = report.facts
    if facts is None:
        return lines[0] + "\n"
    lines += [f"Root package {facts.root_package or '(none)'}, {facts.classes} classes; "
              "names below are relative to it.",
              "These are facts for the reading: what they mean is the corrector's call.", ""]

    lines.append(f"Packages ({len(facts.packages)}):")
    width = max((len(name) for name in facts.packages), default=0)
    lines += [f"  {name:<{width}}  {len(classes)} classes" for name, classes in facts.packages.items()]
    if facts.root_package_classes:
        lines.append(f"In the root package itself, outside every package: {', '.join(facts.root_package_classes)}")
    lines.append("")

    lines.append(f"Dependencies between packages ({len(facts.dependencies)}):")
    lines += [f"  {edge.source} -> {edge.target}  ({len(edge.class_dependencies)} class dependencies)"
              for edge in facts.dependencies] or ["  none"]
    lines.append("")

    in_cycles = sum(len(cycle.packages) for cycle in facts.cycles)
    lines.append(f"Cycles: {len(facts.cycles)} group(s), {in_cycles} of {len(facts.packages)} packages caught in one")
    for number, cycle in enumerate(facts.cycles, start=1):
        lines.append(f"  group {number}: {', '.join(cycle.packages)}")
        for edge in cycle.dependencies:
            lines.append(f"    {edge.source} -> {edge.target}")
            lines += [f"      {dependency}" for dependency in edge.class_dependencies]
    lines.append("")

    for title, entries in (("Classes outside the web layer that depend on a controller", facts.controller_dependents),
                           ("Classes outside the web layer that use the incoming web", facts.incoming_web_users)):
        lines.append(f"{title}: {len(entries) or 'none'}")
        lines += [f"  {entry.class_name} uses {', '.join(entry.uses)}" for entry in entries]
    if facts.javalin_users:
        lines.append(f"Classes that use Javalin ({len(facts.javalin_users)}; Javalin has no annotation that marks "
                     "the web layer, so the web layer is in this list too: telling it apart from the business classes "
                     "is the corrector's call):")
        lines += [f"  {entry.class_name} uses {', '.join(entry.uses)}" for entry in facts.javalin_users]
    return "\n".join(lines) + "\n"


def as_json(report: ArchitectureReport) -> str:
    return json.dumps(asdict(report), indent=2, ensure_ascii=False) + "\n"


def write(report: ArchitectureReport, folder: Path) -> None:
    folder.mkdir(parents=True)
    (folder / "architecture.json").write_text(as_json(report), encoding="utf-8", newline="\n")
    (folder / "architecture-report.txt").write_text(as_text(report), encoding="utf-8", newline="\n")


def main() -> None:
    if len(sys.argv) != 2:
        sys.exit("usage: python report_architecture.py <runs-folder>")
    runs_folder = Path(sys.argv[1])
    classpath = compile_the_suite()
    for run_folder in sorted(folder for folder in runs_folder.iterdir() if (folder / "workspace").is_dir()):
        if (run_folder / REPORT_FOLDER).exists():
            print(f"{run_folder.name}: already has {REPORT_FOLDER}/, kept (remove it to redo the report)", flush=True)
            continue
        report = describe(run_folder, classpath)
        write(report, run_folder / REPORT_FOLDER)
        summary = report.situation
        if report.facts is not None:
            facts = report.facts
            summary += (f", {len(facts.packages)} packages, {sum(len(cycle.packages) for cycle in facts.cycles)} in "
                        f"{len(facts.cycles)} cycle group(s), {len(facts.controller_dependents)} controller "
                        f"dependents, {len(facts.incoming_web_users)} incoming-web users")
        print(f"{run_folder.name}: {summary}", flush=True)


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8")  # type: ignore[union-attr]
    main()
