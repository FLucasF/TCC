"""What every generator of a reference solution uses: a fresh git copy of the base (outside the repository, in the
system's temporary folder), exact text replacements that fail loudly when the base changed, new files, a build with
the solution's own tests, and the patch saved next to this folder (evaluation/solutions/<name>.patch).
"""

import os
import shutil
import stat
import subprocess
import tempfile
from collections.abc import Callable
from pathlib import Path

SOLUTIONS = Path(__file__).resolve().parents[1]
BASE = SOLUTIONS.parents[1] / "experiment" / "base" / "hotel"
WORK_FOLDER = Path(tempfile.gettempdir()) / "maintenance-v1-solutions"
MAVEN = shutil.which("mvn") or "mvn"
GIT_SETTINGS = ["-c", "user.name=solutions", "-c", "user.email=solutions@local", "-c", "core.autocrlf=false"]

SERVICE = "src/main/java/hotel/reservations/ReservationService.java"
CALCULATOR = "src/main/java/hotel/reservations/PriceCalculator.java"
CALCULATOR_TEST = "src/test/java/hotel/reservations/PriceCalculatorTest.java"
SERVICE_TEST = "src/test/java/hotel/reservations/ReservationServiceTest.java"
REPORTS_TEST = "src/test/java/hotel/reports/ReportsTest.java"
HTTP_TEST = "src/test/java/hotel/web/HttpApiTest.java"
HOTEL = "src/main/java/hotel/Hotel.java"
HTTP_API = "src/main/java/hotel/web/HttpApi.java"
EMAIL_SENDER = "src/main/java/hotel/messages/EmailSender.java"
SENT_MESSAGES = "src/main/java/hotel/messages/SentMessages.java"
MESSAGE = "src/main/java/hotel/messages/Message.java"
RECEIPT = "src/main/java/hotel/documents/ReservationReceipt.java"
OCCUPANCY = "src/main/java/hotel/reports/OccupancyReport.java"
REVENUE = "src/main/java/hotel/reports/RevenueReport.java"


def remove_read_only(function: Callable[[str], object], path: str, _: BaseException) -> None:
    """git keeps its objects read-only, and Windows refuses to delete them until that is lifted."""
    Path(path).chmod(stat.S_IWRITE)
    function(path)


def git(arguments: list[str], folder: Path) -> bytes:
    return subprocess.run(["git", *GIT_SETTINGS, *arguments], cwd=folder, check=True, capture_output=True).stdout


class Solution:
    def __init__(self, name: str) -> None:
        self.name = name
        self.root = WORK_FOLDER / name
        if self.root.exists():
            shutil.rmtree(self.root, onexc=remove_read_only)
        shutil.copytree(BASE, self.root, ignore=shutil.ignore_patterns("target"))
        git(["init", "-q"], self.root)
        git(["add", "-A"], self.root)
        git(["commit", "-qm", "base"], self.root)

    def replace(self, relative_path: str, old: str, new: str, count: int = 1) -> None:
        path = self.root / relative_path
        text = path.read_text(encoding="utf-8")
        found = text.count(old)
        if found != count:
            raise SystemExit(f"{self.name} {relative_path}: {old[:70]!r} found {found} times, expected {count} "
                             "(did the base change?)")
        path.write_text(text.replace(old, new), encoding="utf-8", newline="\n")

    def write(self, relative_path: str, content: str) -> None:
        path = self.root / relative_path
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(content, encoding="utf-8", newline="\n")

    def build_and_save(self) -> None:
        verify = subprocess.run([MAVEN, "-B", "-q", "verify"], cwd=self.root, capture_output=True, text=True,
                                encoding="utf-8", errors="replace", check=False, env=os.environ.copy())
        if verify.returncode != 0:
            lines = [line for line in verify.stdout.splitlines()
                     if "ERROR" in line or "FAIL" in line or "expected" in line]
            raise SystemExit(f"{self.name}: mvn verify failed\n" + "\n".join(lines[:40]))
        shutil.rmtree(self.root / "target")
        git(["add", "-A"], self.root)
        patch = git(["diff", "--cached"], self.root)
        (SOLUTIONS / f"{self.name}.patch").write_bytes(patch)
        print(f"{self.name}: builds, its tests pass, patch saved ({patch.count(b'+++ ')} files)")
