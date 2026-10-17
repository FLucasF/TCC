"""The agent's project, and Maven run on a copy of it inside the pinned image. Shared by the measures
(measure_test_quality.py).

The project is the one of the shallowest pom.xml in the workspace (outside target/ and .git/), the same rule as the
build and the suite. The measures never touch the agent's workspace: they work on a copy, which
goes into the container as a tar through stdin and whose results come back as a tar through stdout (a bind mount of
a Windows folder makes the container read each file across the Docker VM boundary, which is very slow).

Maven runs in the same image as the agent's build (same JDK, same Maven), as root (this is the measuring container,
not the agent's), with its downloads kept in a named Docker volume, so each measure does not download Spring again.
"""

import io
import subprocess
import tarfile
from dataclasses import dataclass
from pathlib import Path

IMAGE = "experimento-harness:v5"
MAVEN_CACHE_VOLUME = "v6-maven-cache"
SKIPPED_FOLDERS = {"target", ".git"}


def find_project_folder(workspace: Path) -> Path | None:
    """The folder of the shallowest pom.xml (a tie is broken by the path, like the build's sort)."""
    poms = [pom for pom in workspace.rglob("pom.xml") if not SKIPPED_FOLDERS & set(pom.relative_to(workspace).parts)]
    if not poms:
        return None
    poms.sort(key=lambda pom: (len(pom.relative_to(workspace).parts), pom.relative_to(workspace).as_posix()))
    return poms[0].parent


def project_as_tar(project: Path, replaced_files: dict[str, str]) -> bytes:
    """The project without target/ and .git/, as a tar; `replaced_files` (path inside the project -> new text)
    changes files of the copy only (an extra plugin in the pom, a test without assertions...)."""
    buffer = io.BytesIO()
    with tarfile.open(fileobj=buffer, mode="w") as archive:
        for source in sorted(project.rglob("*")):
            path_inside = source.relative_to(project).as_posix()
            if SKIPPED_FOLDERS & set(source.relative_to(project).parts) or not source.is_file():
                continue
            if path_inside in replaced_files:
                continue
            archive.add(source, arcname=f"project/{path_inside}", recursive=False)
        for path_inside, text in replaced_files.items():
            data = text.encode("utf-8")
            info = tarfile.TarInfo(f"project/{path_inside}")
            info.size = len(data)
            archive.addfile(info, io.BytesIO(data))
    return buffer.getvalue()


@dataclass
class MavenRun:
    exit_code: int
    log: str                          # Maven's whole output
    files: dict[str, bytes]           # the files asked for, by path inside the project


def run_maven(project: Path, maven_arguments: str, wanted_folder: str, replaced_files: dict[str, str] | None = None,
              timeout_seconds: int = 1800) -> MavenRun:
    """Runs `mvn <maven_arguments>` on a copy of the project inside the image, and brings back Maven's log and the
    files under `wanted_folder` (e.g. target/pit-reports)."""
    inside_the_container = (
        "mkdir /work && tar -x -C /work && cd /work/project && "
        f"mvn -B {maven_arguments} > /work/maven.log 2>&1; echo $? > /work/exit-code; "
        f"cd /work && tar -c maven.log exit-code $(test -e project/{wanted_folder} && echo project/{wanted_folder})"
    )
    command = ["docker", "run", "--rm", "-i", "--user", "root", "-e", "HOME=/root",
               "-v", f"{MAVEN_CACHE_VOLUME}:/root/.m2", "--entrypoint", "bash", IMAGE, "-c", inside_the_container]
    finished = subprocess.run(command, input=project_as_tar(project, replaced_files or {}), capture_output=True,
                              timeout=timeout_seconds, check=False)
    if finished.returncode != 0 or not finished.stdout:
        raise RuntimeError(f"the measuring container failed (exit {finished.returncode}): "
                           f"{finished.stderr.decode('utf-8', 'replace')[-1500:]}")
    files: dict[str, bytes] = {}
    with tarfile.open(fileobj=io.BytesIO(finished.stdout)) as archive:
        for member in archive.getmembers():
            extracted = archive.extractfile(member) if member.isfile() else None
            if extracted is not None:
                files[member.name] = extracted.read()
    log = files.pop("maven.log", b"").decode("utf-8", "replace")
    exit_code = int(files.pop("exit-code", b"1").decode().strip() or 1)
    inside_project = {name.removeprefix("project/"): data for name, data in files.items()}
    return MavenRun(exit_code, log, inside_project)
