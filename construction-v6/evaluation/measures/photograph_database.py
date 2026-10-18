"""The photo of the database, without the suite: a clean PostgreSQL, the agent's service started once against it (the
service creates its schema when it starts), and the schema saved as database-schema.json (tables, columns,
constraints, indexes), with the same queries the suite used. It is material for the AI that corrects the database;
it judges nothing.

Usage (from construction-v6):
    python evaluation/measures/photograph_database.py <agent-workspace> <output-folder>
    python evaluation/measures/photograph_database.py runs/pilots/OPUS55-N0-03/workspace \
        runs/pilots/OPUS55-N0-03/database

The workspace must be built (the jar under target/). If the service does not start, the photo comes out empty and
service.log says why.
"""

import os
import subprocess
import sys
import time
import urllib.error
import urllib.request
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "reference"))

from run_suite_against_service import (  # noqa: E402
    DATABASE_NAME,
    DATABASE_PASSWORD,
    DATABASE_PORT,
    DATABASE_USER,
    find_agent_jar,
    remove_database,
    save_database_schema,
    start_clean_database,
)

SERVICE_PORT = 18080
START_TIMEOUT_SECONDS = 120


def wait_until_answering(service: subprocess.Popen[bytes]) -> bool:
    """True when the service answers any HTTP request (even a 404); False if it died or the time ran out."""
    deadline = time.monotonic() + START_TIMEOUT_SECONDS
    while time.monotonic() < deadline:
        if service.poll() is not None:
            return False
        try:
            urllib.request.urlopen(f"http://localhost:{SERVICE_PORT}/products/CAM-001", timeout=2)
            return True
        except urllib.error.HTTPError:
            return True
        except OSError:
            time.sleep(1)
    return False


def photograph(workspace: Path, output_folder: Path) -> bool:
    jar = find_agent_jar(workspace)
    if jar is None:
        sys.exit(f"ERROR: no jar under {workspace} (did the build run?)")
    output_folder.mkdir(parents=True, exist_ok=True)
    start_clean_database()
    try:
        # the port goes in PORT (what the prompt asks, Javalin) and in SERVER_PORT (a Spring Boot service of the older
        # batches)
        environment = {**os.environ, "DB_URL": f"jdbc:postgresql://localhost:{DATABASE_PORT}/{DATABASE_NAME}",
                       "DB_USER": DATABASE_USER, "DB_PASSWORD": DATABASE_PASSWORD,
                       "TRANSLOG_URL": "http://localhost:9",
                       "PORT": str(SERVICE_PORT), "SERVER_PORT": str(SERVICE_PORT)}
        with (output_folder / "service.log").open("wb") as log:
            service = subprocess.Popen(["java", "-jar", str(jar)], env=environment, stdout=log,
                                       stderr=subprocess.STDOUT)
            try:
                started = wait_until_answering(service)
                save_database_schema(output_folder)
            finally:
                service.terminate()
                try:
                    service.wait(15)
                except subprocess.TimeoutExpired:
                    service.kill()
        return started
    finally:
        remove_database()


def main() -> None:
    if len(sys.argv) != 3:
        sys.exit(__doc__)
    started = photograph(Path(sys.argv[1]).resolve(), Path(sys.argv[2]).resolve())
    print("photo saved" if started else "the service did not start: the photo may be empty (see service.log)")


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8")  # type: ignore[union-attr]
    main()
