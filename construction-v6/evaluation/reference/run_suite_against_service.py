"""Runs the full suite against one agent's service: a clean PostgreSQL in Docker, the agent's own jar (the one its
`mvn verify` built), and the suite (reference.suite.Suite), which starts and restarts the service by itself.
Everything the suite needs to know about the agent is its workspace.

Usage (from construction-v6):
                      python evaluation/reference/run_suite_against_service.py <agent-workspace> <output-folder>
For example:          python evaluation/reference/run_suite_against_service.py \
                          runs/pilots/OPUS55-N0-03/workspace suite-out

Output: <output-folder>/suite-report.txt (what is printed), suite-result.json (every check), database-schema.json
(the schema the service created, for the AI's reading of the database), and service.log and
service-b.log (the output of each machine, before and after the restart). The service runs on two machines with the
same database, as the prompt says; --machines 1 exists only to validate the suite.
"""

import json
import os
import shutil
import subprocess
import sys
import time
from pathlib import Path

REFERENCE = Path(__file__).resolve().parent
DATABASE_CONTAINER = "suite-postgres"
DATABASE_PORT = 55432
# the PostgreSQL the prompt asks for (18), pinned by digest so the tag moving does not change the suite (17/10/2026)
DATABASE_IMAGE = "postgres:18.6-alpine@sha256:77f585114c32fbca283dc835b0596f4e52b51b4c6662d7810b2f4084f60a1873"
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


# The schema the agent's service created, read from PostgreSQL's catalog (the system schemas left out). Each query
# returns one JSON array, so the output does not depend on psql's text formatting.
APPLICATION_SCHEMAS = "n.nspname NOT IN ('pg_catalog', 'information_schema') AND n.nspname NOT LIKE 'pg_toast%'"
SCHEMA_QUERIES = {
    "tables": f"""SELECT coalesce(json_agg(c.relname ORDER BY c.relname), '[]') FROM pg_class c
        JOIN pg_namespace n ON n.oid = c.relnamespace WHERE c.relkind IN ('r', 'p') AND {APPLICATION_SCHEMAS}""",
    # the type with its size (numeric(12,2), character varying(255)), whether it accepts null, and the default
    "columns": f"""SELECT coalesce(json_agg(json_build_object('table', c.relname, 'column', a.attname,
        'type', format_type(a.atttypid, a.atttypmod), 'nullable', NOT a.attnotnull,
        'default', pg_get_expr(d.adbin, d.adrelid)) ORDER BY c.relname, a.attnum), '[]')
        FROM pg_attribute a JOIN pg_class c ON c.oid = a.attrelid JOIN pg_namespace n ON n.oid = c.relnamespace
        LEFT JOIN pg_attrdef d ON d.adrelid = a.attrelid AND d.adnum = a.attnum
        WHERE c.relkind IN ('r', 'p') AND a.attnum > 0 AND NOT a.attisdropped AND {APPLICATION_SCHEMAS}""",
    # PostgreSQL 18 also lists each NOT NULL here (type 'n'): left out, the columns already say it ("nullable")
    "constraints": f"""SELECT coalesce(json_agg(json_build_object('table', c.conrelid::regclass::text,
        'name', c.conname, 'type', c.contype, 'definition', pg_get_constraintdef(c.oid))
        ORDER BY c.conrelid::regclass::text, c.conname),
        '[]') FROM pg_constraint c JOIN pg_namespace n ON n.oid = c.connamespace
        WHERE c.contype <> 'n' AND {APPLICATION_SCHEMAS}""",
    # every index, unique or not: a foreign key without an index is read from here
    "indexes": f"""SELECT coalesce(json_agg(json_build_object('table', t.relname, 'name', i.relname,
        'definition', pg_get_indexdef(i.oid), 'unique', x.indisunique) ORDER BY t.relname, i.relname), '[]')
        FROM pg_index x JOIN pg_class i ON i.oid = x.indexrelid JOIN pg_class t ON t.oid = x.indrelid
        JOIN pg_namespace n ON n.oid = t.relnamespace WHERE {APPLICATION_SCHEMAS}""",
}


def save_database_schema(output_folder: Path) -> None:
    """Saves the agent's schema (tables, columns, constraints, indexes) as database-schema.json, for the AI's
    reading of the database (how it is organized and what it enforces). Read after the suite, before the database is
    removed."""
    schema: dict[str, object] = {}
    for name, query in SCHEMA_QUERIES.items():
        answer = subprocess.run(["docker", "exec", DATABASE_CONTAINER, "psql", "-U", DATABASE_USER, "-d", DATABASE_NAME,
                                 "-At", "-c", " ".join(query.split())],
                                capture_output=True, text=True, encoding="utf-8", errors="replace", check=False)
        schema[name] = json.loads(answer.stdout) if answer.returncode == 0 and answer.stdout.strip() else None
    (output_folder / "database-schema.json").write_text(json.dumps(schema, indent=2, ensure_ascii=False),
                                                        encoding="utf-8")


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
        save_database_schema(output_folder)
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
        sys.exit("usage: python run_suite_against_service.py <agent-workspace> <output-folder> [--machines 2]")
    sys.exit(run_suite(Path(arguments[0]).resolve(), Path(arguments[1]).resolve(), machines=machines))


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8")  # type: ignore[union-attr]
    main()
