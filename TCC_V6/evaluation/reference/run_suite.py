"""Runs the full suite against one agent's service: a clean PostgreSQL in Docker, the agent's own jar (the one its
`mvn verify` built), and the suite (reference.suite.Suite), which starts and restarts the service by itself.
Everything the suite needs to know about the agent is its workspace.

Usage (from TCC_V6):  python evaluation/reference/run_suite.py <agent-workspace> <output-folder>
For example:          python evaluation/reference/run_suite.py runs/pilots/OPUS55-N0-03/workspace suite-out

Output: <output-folder>/suite-report.txt (what is printed), suite-result.json (every check), and service.log and
service-b.log (the output of each machine, before and after the restart). The service runs on two machines with the
same database, as the prompt says; --machines 1 exists only to validate the suite.
"""

import os
import shutil
import subprocess
import sys
import time
from pathlib import Path

REFERENCE = Path(__file__).resolve().parent
DATABASE_CONTAINER = "suite-postgres"
DATABASE_PORT = 55432
DATABASE_IMAGE = "postgres:16-alpine"
# a throwaway database for the suite: these are test values, not a secret
DATABASE_NAME, DATABASE_USER, DATABASE_PASSWORD = "shop", "postgres", "suite"
DATABASE_START_TIMEOUT_SECONDS = 60


def find_agent_jar(workspace: Path) -> Path | None:
    """The shallowest target/*.jar that is not the ".original" Spring Boot leaves behind."""
    jars = [jar for jar in workspace.rglob("*.jar")
            if jar.parent.name == "target" and not jar.name.endswith(".original")]
    jars.sort(key=lambda jar: (len(jar.relative_to(workspace).parts), jar.as_posix()))
    return jars[0] if jars else None


def compile_the_suite() -> str:
    """Compiles the suite and asks Maven for its classpath (Jackson); returns the full classpath."""
    maven = shutil.which("mvn") or "mvn"
    command = [maven, "-B", "-q", "compile", "dependency:build-classpath", "-Dmdep.outputFile=classpath.txt"]
    if subprocess.run(command, cwd=REFERENCE, capture_output=True, check=False).returncode != 0:
        sys.exit("ERROR: the suite did not compile")
    dependencies = (REFERENCE / "classpath.txt").read_text(encoding="utf-8").strip()
    return os.pathsep.join([str(REFERENCE / "target" / "classes"), dependencies])


def start_clean_database() -> None:
    subprocess.run(["docker", "rm", "-f", DATABASE_CONTAINER], capture_output=True, check=False)
    started = subprocess.run(
        ["docker", "run", "-d", "--name", DATABASE_CONTAINER, "-e", f"POSTGRES_PASSWORD={DATABASE_PASSWORD}",
         "-e", f"POSTGRES_DB={DATABASE_NAME}", "-p", f"{DATABASE_PORT}:5432", DATABASE_IMAGE],
        capture_output=True, check=False)
    if started.returncode != 0:
        sys.exit("ERROR: could not start PostgreSQL")
    deadline = time.monotonic() + DATABASE_START_TIMEOUT_SECONDS
    while time.monotonic() < deadline:
        ready = subprocess.run(["docker", "exec", DATABASE_CONTAINER, "psql", "-U", DATABASE_USER, "-d", DATABASE_NAME,
                                "-c", "select 1"], capture_output=True, check=False)
        if ready.returncode == 0:
            return
        time.sleep(1)
    sys.exit(f"ERROR: PostgreSQL did not answer within {DATABASE_START_TIMEOUT_SECONDS}s")


def remove_database() -> None:
    subprocess.run(["docker", "rm", "-f", DATABASE_CONTAINER], capture_output=True, check=False)


def run_suite(workspace: Path, output_folder: Path, echo: bool = True, machines: int = 2) -> int:
    """Runs the suite against the workspace's service on `machines` machines (two, as the prompt says; one only
    to validate the suite); returns the suite's exit code."""
    jar = find_agent_jar(workspace)
    if jar is None:
        sys.exit(f"ERROR: no jar under {workspace} (did the build run?)")
    output_folder.mkdir(parents=True, exist_ok=True)
    for old_log in output_folder.glob("service*.log"):          # service.log, service-b.log: one per machine
        old_log.write_text("", encoding="utf-8")
    classpath = compile_the_suite()

    start_clean_database()
    try:
        if echo:
            print(f"suite: {jar}", flush=True)
        environment = {**os.environ, "DB_URL": f"jdbc:postgresql://localhost:{DATABASE_PORT}/{DATABASE_NAME}",
                       "DB_USER": DATABASE_USER, "DB_PASSWORD": DATABASE_PASSWORD}
        command = ["java", "-cp", classpath, "reference.suite.Suite", "--jar", str(jar),
                   "--machines", str(machines), "--out", str(output_folder / "suite-result.json"),
                   "--log", str(output_folder / "service.log")]
        with subprocess.Popen(command, env=environment, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                              text=True, encoding="utf-8", errors="replace") as suite, \
                (output_folder / "suite-report.txt").open("w", encoding="utf-8", newline="\n") as report:
            assert suite.stdout is not None
            for line in suite.stdout:                 # like tee: on the screen and in the report
                report.write(line)
                if echo:
                    try:
                        print(line, end="", flush=True)
                    except OSError:                   # the screen was closed (| head): the report goes on
                        echo = False
        return suite.returncode
    finally:
        remove_database()                             # removed at the end whatever happens


def main() -> None:
    arguments = sys.argv[1:]
    machines = 2
    if "--machines" in arguments:
        position = arguments.index("--machines")
        machines = int(arguments[position + 1])
        del arguments[position:position + 2]
    if len(arguments) != 2:
        sys.exit("usage: python run_suite.py <agent-workspace> <output-folder> [--machines 2]")
    sys.exit(run_suite(Path(arguments[0]).resolve(), Path(arguments[1]).resolve(), machines=machines))


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8")  # type: ignore[union-attr]
    main()
