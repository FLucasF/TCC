"""Builds a copy of an agent's hotel project and runs it: one fresh service per check, so no check sees another's
reservations. The agent's folder is never touched: Maven runs on a copy.

The HTTP boundary is where the typing stops: the service's JSON is parsed into plain Python values (Any), and each
check says what it expects to find there. Money comes back as Decimal, never as float.
"""

import json
import os
import shutil
import socket
import subprocess
import time
import urllib.error
import urllib.request
from collections.abc import Generator
from contextlib import contextmanager
from dataclasses import dataclass
from decimal import Decimal
from pathlib import Path
from typing import Any

MAVEN = shutil.which("mvn") or "mvn"
JAVA = shutil.which("java") or "java"
SERVICE_START_TIMEOUT_SECONDS = 30
SERVICE_STOP_TIMEOUT_SECONDS = 10
HTTP_TIMEOUT_SECONDS = 10
# environment variables a scenario may read; never inherited from the machine running the suite
SCENARIO_ENVIRONMENT_VARIABLES = ("HOTEL_TEST_MODE",)


@dataclass(frozen=True)
class HttpResponse:
    status: int
    body: str
    content_type: str

    def json(self) -> Any:
        return json.loads(self.body, parse_float=Decimal)


class HotelApi:
    def __init__(self, base_url: str) -> None:
        self.base_url = base_url

    def get(self, path: str) -> HttpResponse:
        return self._send(urllib.request.Request(self.base_url + path, method="GET"))

    def post(self, path: str, json_body: object | None = None, raw_body: str | None = None) -> HttpResponse:
        if raw_body is not None:
            data = raw_body.encode("utf-8")
        elif json_body is not None:
            data = json.dumps(json_body).encode("utf-8")
        else:
            data = b""
        request = urllib.request.Request(self.base_url + path, data=data, method="POST",
                                         headers={"Content-Type": "application/json"})
        return self._send(request)

    @staticmethod
    def _send(request: urllib.request.Request) -> HttpResponse:
        try:
            with urllib.request.urlopen(request, timeout=HTTP_TIMEOUT_SECONDS) as response:
                return HttpResponse(response.status, response.read().decode("utf-8"),
                                    response.headers.get("Content-Type", ""))
        except urllib.error.HTTPError as error_response:
            return HttpResponse(error_response.code, error_response.read().decode("utf-8"),
                                error_response.headers.get("Content-Type", ""))


@dataclass(frozen=True)
class BuildResult:
    jar: Path | None
    agent_tests_passed: bool


def find_service_jar(target_folder: Path) -> Path | None:
    """The runnable jar Maven built: the biggest one, so the shaded jar wins over Maven's "original-" leftover."""
    jars = [jar for jar in target_folder.glob("*.jar") if not jar.name.startswith("original-")]
    return max(jars, key=lambda jar: jar.stat().st_size) if jars else None


def build_agent_copy(agent_project: Path, work_folder: Path, build_log: Path) -> BuildResult:
    """Copies the project, runs the agent's own `mvn verify`, and, if its tests fail, packages without them, so the
    suite can still judge the service."""
    project_copy = work_folder / "project"
    if project_copy.resolve() == agent_project.resolve():
        raise ValueError(f"the output folder would overwrite the project itself: {agent_project}")
    if project_copy.exists():
        shutil.rmtree(project_copy)
    shutil.copytree(agent_project, project_copy, ignore=shutil.ignore_patterns("target", ".git"))
    with build_log.open("w", encoding="utf-8") as log:
        log.write("$ mvn -B verify\n")
        log.flush()
        verify = subprocess.run([MAVEN, "-B", "verify"], cwd=project_copy, stdout=log, stderr=subprocess.STDOUT,
                                check=False)
        agent_tests_passed = verify.returncode == 0
        if not agent_tests_passed:
            log.write("\n$ mvn -B package -DskipTests\n")
            log.flush()
            subprocess.run([MAVEN, "-B", "package", "-DskipTests"], cwd=project_copy, stdout=log,
                           stderr=subprocess.STDOUT, check=False)
    return BuildResult(find_service_jar(project_copy / "target"), agent_tests_passed)


def find_free_port() -> int:
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as probe:
        probe.bind(("127.0.0.1", 0))
        port: int = probe.getsockname()[1]
        return port


class HotelLauncher:
    """Starts the service from the built jar. Every `running()` is a new process with empty memory."""

    def __init__(self, jar: Path, service_log: Path) -> None:
        self.jar = jar
        self.service_log = service_log
        self.service_log.write_text("", encoding="utf-8")

    @contextmanager
    def running(self, scenario_environment: dict[str, str] | None = None) -> Generator[HotelApi]:
        port = find_free_port()
        environment = {name: value for name, value in os.environ.items()
                       if name not in SCENARIO_ENVIRONMENT_VARIABLES}
        environment.update(scenario_environment or {})
        environment["PORT"] = str(port)
        api = HotelApi(f"http://127.0.0.1:{port}")
        with self.service_log.open("a", encoding="utf-8") as log:
            log.write(f"\n===== service on port {port}, extra environment {scenario_environment or {}}\n")
            log.flush()
            process = subprocess.Popen([JAVA, "-jar", str(self.jar)], env=environment, stdout=log,
                                       stderr=subprocess.STDOUT)
            try:
                self._wait_until_up(api, process)
                yield api
            finally:
                process.terminate()
                try:
                    process.wait(SERVICE_STOP_TIMEOUT_SECONDS)
                except subprocess.TimeoutExpired:
                    process.kill()
                    process.wait()

    @staticmethod
    def _wait_until_up(api: HotelApi, process: subprocess.Popen[bytes]) -> None:
        deadline = time.monotonic() + SERVICE_START_TIMEOUT_SECONDS
        while time.monotonic() < deadline:
            if process.poll() is not None:
                raise RuntimeError(f"the service exited during start-up (code {process.returncode})")
            try:
                if api.get("/rooms").status == 200:
                    return
            except OSError:
                pass
            time.sleep(0.2)
        raise RuntimeError(f"the service did not answer GET /rooms within {SERVICE_START_TIMEOUT_SECONDS}s")
