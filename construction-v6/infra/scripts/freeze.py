"""The freeze of V6, like V5's: the sha256 of every file that decides a result, in CONGELADO-V6.sha256 (the format of
`sha256sum`, so `sha256sum -c` checks it too). Frozen: every file of construction-v6 tracked by git, except what the
runs produce and what only describes (the README, the notes, the runs, the analysis, the judge's batches).

Usage (from construction-v6):
    python infra/scripts/freeze.py           writes CONGELADO-V6.sha256 (refused if it exists: a freeze is done once)
    python infra/scripts/freeze.py --check   compares every frozen file with its hash (no output: all identical)

Freeze only with everything committed (the files are listed by `git ls-files`, and a change not committed would be
frozen without a commit that holds it), and record the commit in the README.
"""

import hashlib
import subprocess
import sys
from pathlib import Path

V6 = Path(__file__).resolve().parents[2]
FREEZE_FILE = V6 / "CONGELADO-V6.sha256"
NOT_FROZEN = ("README.md", "NOTAS.md", "CONGELADO-V6.sha256", "runs/", "analysis/", "evaluation/judge/batches/")


def frozen_files() -> list[str]:
    listed = subprocess.run(["git", "ls-files", "-z"], cwd=V6, capture_output=True, check=True).stdout
    paths = [path for path in listed.decode("utf-8").split("\0") if path]
    return sorted(path for path in paths if not any(path == item or path.startswith(item) for item in NOT_FROZEN))


def digest(path: str) -> str:
    return hashlib.sha256((V6 / path).read_bytes()).hexdigest()


def freeze() -> None:
    if FREEZE_FILE.exists():
        sys.exit(f"{FREEZE_FILE.name} already exists: a change to what froze is a dated amendment, not a new freeze")
    changed = subprocess.run(["git", "status", "--porcelain", "."], cwd=V6, capture_output=True, text=True,
                             check=True).stdout.strip()
    if changed:
        sys.exit(f"commit everything first; not committed:\n{changed}")
    files = frozen_files()
    FREEZE_FILE.write_text("".join(f"{digest(path)} *{path}\n" for path in files), encoding="utf-8", newline="\n")
    print(f"{len(files)} files -> {FREEZE_FILE.name}")


def check() -> None:
    differences = []
    for line in FREEZE_FILE.read_text(encoding="utf-8").splitlines():
        expected, path = line.split(" *", 1)
        if not (V6 / path).is_file():
            differences.append(f"missing: {path}")
        elif digest(path) != expected:
            differences.append(f"changed: {path}")
    if differences:
        sys.exit("\n".join(differences))


if __name__ == "__main__":
    if sys.argv[1:] == ["--check"]:
        check()
    else:
        freeze()
