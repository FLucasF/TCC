"""The PostgreSQL of one execution, shared by the two studies' run_one_scenario.py: a Docker network of its own, a
clean database in it (reachable from the other containers of that network as `db`), the photo of its schema at the
end (`pg_dump --schema-only`), and everything removed afterwards.

The agent's container and the build's container join the network and receive DB_URL, DB_USER and DB_PASSWORD, the
variables the prompt (construction) and the base's README (maintenance) talk about. Nothing is published on the
host: only the containers of the execution reach the database. The password is a fixed, throwaway one; the database
lives only while the execution runs.
"""

import subprocess
import time
from pathlib import Path

IMAGE = "postgres:18.6-alpine@sha256:77f585114c32fbca283dc835b0596f4e52b51b4c6662d7810b2f4084f60a1873"
HOST_IN_NETWORK = "db"
NAME, USER, PASSWORD = "hotel", "hotel", "hotel"
START_TIMEOUT_SECONDS = 60
SCHEMA_FILE = "database-schema.sql"


def network_name(run_id: str) -> str:
    return f"{run_id}-net"


def container_name(run_id: str) -> str:
    return f"{run_id}-db"


def environment() -> dict[str, str]:
    """What the containers of the execution receive to reach the database."""
    return {"DB_URL": f"jdbc:postgresql://{HOST_IN_NETWORK}:5432/{NAME}", "DB_USER": USER, "DB_PASSWORD": PASSWORD}


def docker_arguments(run_id: str) -> list[str]:
    """The `docker run` arguments that put a container in the execution's network, with the database variables."""
    arguments = ["--network", network_name(run_id)]
    for key, value in environment().items():
        arguments += ["-e", f"{key}={value}"]
    return arguments


def review_arguments(run_id: str) -> list[str]:
    """For the quality review's containers: the same, plus the database as a superuser in PGHOST, PGPORT, PGUSER and
    PGPASSWORD, so the review scripts can create one empty database per measurement."""
    return [*docker_arguments(run_id), "-e", f"PGHOST={HOST_IN_NETWORK}", "-e", "PGPORT=5432", "-e", f"PGUSER={USER}",
            "-e", f"PGPASSWORD={PASSWORD}"]


def start_commands(run_id: str) -> list[list[str]]:
    return [
        ["docker", "network", "create", network_name(run_id)],
        ["docker", "run", "-d", "--name", container_name(run_id), "--network", network_name(run_id),
         "--network-alias", HOST_IN_NETWORK, "-e", f"POSTGRES_DB={NAME}", "-e", f"POSTGRES_USER={USER}",
         "-e", f"POSTGRES_PASSWORD={PASSWORD}", IMAGE],
    ]


def start(run_id: str) -> None:
    """A clean database in a network of its own; raises RuntimeError when it does not come up."""
    remove(run_id)
    for command in start_commands(run_id):
        started = subprocess.run(command, capture_output=True, text=True, encoding="utf-8", check=False)
        if started.returncode != 0:
            remove(run_id)
            raise RuntimeError(f"could not start the database ({' '.join(command[:3])}): {started.stderr.strip()}")
    deadline = time.monotonic() + START_TIMEOUT_SECONDS
    while time.monotonic() < deadline:
        # pg_isready answers before the image's first-start script ends; a real query does not
        ready = subprocess.run(["docker", "exec", container_name(run_id), "psql", "-U", USER, "-d", NAME, "-qtAc",
                                "SELECT 1"], capture_output=True, text=True, encoding="utf-8", check=False)
        if ready.returncode == 0 and ready.stdout.strip() == "1":
            return
        time.sleep(1)
    remove(run_id)
    raise RuntimeError(f"PostgreSQL did not answer within {START_TIMEOUT_SECONDS}s")


def photograph(run_id: str, output_folder: Path) -> bool:
    """Saves the schema as it is now (tables, columns, constraints, indexes; no data) in <output>/database-schema.sql.
    Returns whether pg_dump worked."""
    dump = subprocess.run(["docker", "exec", container_name(run_id), "pg_dump", "--schema-only", "--no-owner",
                           "--no-privileges", "-U", USER, NAME], capture_output=True, check=False)
    (output_folder / SCHEMA_FILE).write_bytes(dump.stdout if dump.returncode == 0 else dump.stderr)
    return dump.returncode == 0


def remove(run_id: str) -> None:
    subprocess.run(["docker", "rm", "-f", container_name(run_id)], capture_output=True, check=False)
    subprocess.run(["docker", "network", "rm", network_name(run_id)], capture_output=True, check=False)
