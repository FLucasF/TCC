"""The quality of the agent's own tests, by mutation testing (PIT): small bugs are planted in the agent's code, one at a
time, and its tests run against each one. A mutant the tests catch is "killed"; one they let through "survived".

Usage (from TCC_V6):  python evaluation/measures/measure_test_quality.py <runs-folder> <output.csv>
                          [--without-assertions]
                      python evaluation/measures/measure_test_quality.py runs/pilots pit-pilots.csv

<runs-folder> has one folder per run, each with its workspace/. The agent's workspace is never touched: PIT runs on a
copy, in the pinned image (maven_on_agent_copy.py). --without-assertions is the validation of this measure (the
"discrimination power" of Akita's benchmark): the same tests, with every assertion removed, run the same code but
check nothing, so they must kill far fewer mutants. A measure that does not drop there measures coverage, not tests.

The columns: tests (@Test and @ParameterizedTest methods the agent wrote), mutants, killed, survived, no_coverage (the
mutants no test even runs), mutation_score = killed / mutants, test_strength = killed / mutants the tests run (the
quality of the tests where they look), line_coverage, and the situation (ok, no-tests, tests-fail, no-mutants,
not-compiled, failed).
"""

import csv
import re
import sys
import time
import xml.etree.ElementTree as ElementTree
from dataclasses import asdict, dataclass, field
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

from maven_on_agent_copy import find_project_folder, run_maven  # noqa: E402

PIT_VERSION = "1.30.1"
PIT_JUNIT5_PLUGIN_VERSION = "1.2.3"     # 1.17.x does not run on JUnit 6 / Spring Boot 4 (found on 2026-10-14)
PIT_PLUGIN = f"""
      <plugin>
        <groupId>org.pitest</groupId>
        <artifactId>pitest-maven</artifactId>
        <version>{PIT_VERSION}</version>
        <dependencies>
          <dependency>
            <groupId>org.pitest</groupId>
            <artifactId>pitest-junit5-plugin</artifactId>
            <version>{PIT_JUNIT5_PLUGIN_VERSION}</version>
          </dependency>
        </dependencies>
        <configuration>
          <outputFormats><param>XML</param></outputFormats>
          <timestampedReports>false</timestampedReports>
          <threads>4</threads>
        </configuration>
      </plugin>
"""
# a killed mutant: the tests failed, crashed or hung on it
KILLED_STATUSES = {"KILLED", "TIMED_OUT", "MEMORY_ERROR", "RUN_ERROR"}


@dataclass
class PitResult:
    run: str
    situation: str = ""
    tests: int = 0
    mutants: int = 0
    killed: int = 0
    survived: int = 0
    no_coverage: int = 0
    mutation_score: str = ""
    test_strength: str = ""
    line_coverage: str = ""
    seconds: int = 0
    notes: list[str] = field(default_factory=list)


# ------------------------------------------------------------------ preparing the copy

def with_pit_plugin(pom_text: str, skip_failing_tests: bool = False) -> str:
    """The pom with PIT's plugin added (inside <build><plugins>, creating them if needed). skipFailingTests has no
    command-line property in pitest-maven (a -D is silently ignored, found on 2026-10-14): it only works here."""
    plugin = PIT_PLUGIN
    if skip_failing_tests:
        with_skip = "<threads>4</threads>\n          <skipFailingTests>true</skipFailingTests>"
        plugin = plugin.replace("<threads>4</threads>", with_skip)
    if "</plugins>" in pom_text:
        return pom_text.replace("</plugins>", plugin + "    </plugins>", 1)
    if "</build>" in pom_text:
        return pom_text.replace("</build>", "  <plugins>" + plugin + "    </plugins>\n  </build>", 1)
    new_build = "  <build>\n    <plugins>" + plugin + "    </plugins>\n  </build>\n</project>"
    return pom_text.replace("</project>", new_build, 1)


def common_package(java_files: list[Path]) -> str:
    """The deepest package that contains every class of the files (com.shop.orders)."""
    packages = []
    for java_file in java_files:
        source = java_file.read_text(encoding="utf-8", errors="replace")
        match = re.search(r"^\s*package\s+([\w.]+)\s*;", source, re.MULTILINE)
        if match:
            packages.append(match.group(1).split("."))
    if not packages:
        return ""
    common = packages[0]
    for parts in packages[1:]:
        while common and parts[:len(common)] != common:
            common = common[:-1]
    return ".".join(common)


def count_tests(test_files: list[Path]) -> int:
    return sum(len(re.findall(r"@(?:Test|ParameterizedTest|RepeatedTest|TestFactory)\b",
                              file.read_text(encoding="utf-8", errors="replace"))) for file in test_files)


# ------------------------------------------------------------------ the validation: the same tests, without assertions

ASSERTION_STATEMENT = re.compile(r"(?<![\w.])(?:(?:org\.junit\.jupiter\.api\.)?Assertions\.|Assert\.|Mockito\.)?"
                                 r"(?:assert\w*|verify|verifyNoInteractions|verifyNoMoreInteractions)\s*\(")
EXPECTATION_CALL = re.compile(r"\.\s*andExpect(?:All)?\s*\(")


def matching_parenthesis(source: str, opening: int) -> int:
    """The position of the ")" that closes the "(" at `opening`, skipping text inside quotes."""
    depth, position = 0, opening
    while position < len(source):
        char = source[position]
        if char in "\"'":
            position += 1
            while position < len(source) and source[position] != char:
                position += 2 if source[position] == "\\" else 1
        elif char == "(":
            depth += 1
        elif char == ")":
            depth -= 1
            if depth == 0:
                return position
        position += 1
    return len(source) - 1


def blank_out(source: str, start: int, end: int) -> str:
    """Replaces source[start:end] with spaces, keeping the line breaks (so line numbers do not move)."""
    return source[:start] + re.sub(r"[^\n]", " ", source[start:end]) + source[end:]


# what is left of a chain whose calls were all removed ("result ;"): not a Java statement, so it goes too
LEFTOVER_NAME = re.compile(r"(?<=[;{}])(\s*)(?!(?:return|break|continue)\b)[A-Za-z_]\w*(\s*);")


def without_assertions(source: str) -> str:
    """The test file with every assertion removed: assert...(...); and verify(...)...; statements, and the
    .andExpect(...) of MockMvc chains. The tests still run the agent's code, but check nothing."""
    while match := EXPECTATION_CALL.search(source):
        source = blank_out(source, match.start(), matching_parenthesis(source, match.end() - 1) + 1)
    position = 0
    while match := ASSERTION_STATEMENT.search(source, position):
        statement_end = source.find(";", matching_parenthesis(source, match.end() - 1))
        if statement_end < 0:
            break
        statement_start = max(source.rfind(delimiter, 0, match.start()) for delimiter in ";{}") + 1
        before_the_call = source[statement_start:match.start()].strip()
        if not before_the_call:
            start = match.start()                       # the statement is the assertion
        elif before_the_call.endswith("="):
            start = statement_start                     # its value is used (var e = assertThrows(...);): all of it goes
        else:
            position = match.end()                      # a declaration (private void assertError(...)): it stays
            continue
        source = blank_out(source, start, statement_end + 1)
        position = start + 1
    return LEFTOVER_NAME.sub(lambda leftover: re.sub(r"[^\n]", " ", leftover.group(0)), source)


# ------------------------------------------------------------------ running and reading PIT

def read_mutations(xml_bytes: bytes, result: PitResult) -> None:
    root = ElementTree.fromstring(xml_bytes)
    statuses = [mutation.get("status", "") for mutation in root.iter("mutation")]
    result.mutants = len(statuses)
    result.killed = sum(status in KILLED_STATUSES for status in statuses)
    result.no_coverage = statuses.count("NO_COVERAGE")
    result.survived = statuses.count("SURVIVED")
    if result.mutants:
        result.mutation_score = f"{result.killed / result.mutants:.3f}"
        covered = result.mutants - result.no_coverage
        result.test_strength = f"{result.killed / covered:.3f}" if covered else "0.000"


def measure(run_folder: Path, assertions: bool = True) -> PitResult:
    result = PitResult(run_folder.name)
    project = find_project_folder(run_folder / "workspace")
    if project is None:
        result.situation = "no-project"
        return result
    main_files = sorted((project / "src" / "main" / "java").rglob("*.java"))
    test_files = sorted((project / "src" / "test" / "java").rglob("*.java"))
    result.tests = count_tests(test_files)
    if result.tests == 0:
        result.situation, result.mutation_score, result.test_strength = "no-tests", "0.000", "0.000"
        return result

    # Without its assertThrows, a test whose code throws on purpose now fails before any mutant: in the validation
    # mode PIT skips those tests instead of refusing the whole run.
    pom_text = (project / "pom.xml").read_text(encoding="utf-8")
    replaced = {"pom.xml": with_pit_plugin(pom_text, skip_failing_tests=not assertions)}
    if not assertions:
        for test_file in test_files:
            test_source = test_file.read_text(encoding="utf-8")
            replaced[test_file.relative_to(project).as_posix()] = without_assertions(test_source)
    target_classes, target_tests = common_package(main_files), common_package(test_files)
    arguments = (f"-q test-compile org.pitest:pitest-maven:{PIT_VERSION}:mutationCoverage "
                 f"-DtargetClasses={target_classes}.* -DtargetTests={target_tests or target_classes}.*")
    started = time.monotonic()
    maven = run_maven(project, arguments, "target/pit-reports", replaced)
    result.seconds = round(time.monotonic() - started)

    coverage = re.search(r"Line Coverage[^:]*: \d+/\d+ \((\d+)%\)", maven.log)
    result.line_coverage = f"{int(coverage.group(1)) / 100:.2f}" if coverage else ""
    mutations_xml = maven.files.get("target/pit-reports/mutations.xml")
    if maven.exit_code == 0 and mutations_xml:
        result.situation = "ok"
        read_mutations(mutations_xml, result)
    elif "COMPILATION ERROR" in maven.log:
        result.situation = "not-compiled"
    elif "did not pass without mutation" in maven.log:
        result.situation = "tests-fail"
    elif "No mutations found" in maven.log:
        result.situation = "no-mutants"
    else:
        result.situation = "failed"
    if result.situation != "ok":
        result.notes.append(maven.log.strip().splitlines()[-1][:200] if maven.log.strip() else "no Maven output")
    return result


def main() -> None:
    arguments = sys.argv[1:]
    assertions = "--without-assertions" not in arguments
    arguments = [argument for argument in arguments if argument != "--without-assertions"]
    if len(arguments) != 2:
        sys.exit("usage: python measure_test_quality.py <runs-folder> <output.csv> [--without-assertions]")
    runs_folder, output_csv = Path(arguments[0]), Path(arguments[1])
    if output_csv.exists():
        sys.exit(f"{output_csv} already exists: a result is never written over another")
    results = []
    for run_folder in sorted(folder for folder in runs_folder.iterdir() if (folder / "workspace").is_dir()):
        result = measure(run_folder, assertions)
        results.append(result)
        print(f"{result.run}: {result.situation}, {result.tests} tests, score {result.mutation_score or '-'}, "
              f"strength {result.test_strength or '-'}, coverage {result.line_coverage or '-'} ({result.seconds}s)",
              flush=True)
    with output_csv.open("w", encoding="utf-8", newline="") as file:
        columns = [name for name in PitResult.__dataclass_fields__]
        writer = csv.DictWriter(file, fieldnames=columns, lineterminator="\n")
        writer.writeheader()
        for result in results:
            row = asdict(result)
            row["notes"] = "; ".join(result.notes)
            writer.writerow(row)
    print(f"{len(results)} runs -> {output_csv}")


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8")  # type: ignore[union-attr]
    main()
