"""Reads the design of each package with Semgrep and answers the régua's questions (P1 to P7 and F).

Usage:  python evaluation/semgrep/detect.py <packages-root> <output.csv>
        python evaluation/semgrep/detect.py runs/pilots analysis/semgrep-pilots.csv

<packages-root> has one subfolder per package (a run's workspace, or a blind copy). Deterministic and without AI:
the same package always gets the same answer. It refuses to run if rules.yml is not what rules.py writes, or if
the output already exists (a result is never written over another).

HOW TO READ. The files of this folder, in the order they come in:
  points.py      the names of the cases of each point (GOLD, NORTH, PAID...), the only table;
  rules.py       writes rules.yml from it (rules.yml is never edited by hand);
  clean_copy.py  makes the copy without comments that Semgrep reads;
  rules.yml      what Semgrep looks for: each rule marks a LINE of code ("an if that names GOLD");
  classify.py    reads the marked lines and answers the régua's questions per package.
This script only chains them: checks the rules, makes the clean copy, runs Semgrep in a container pinned by its
hash, classifies, and writes the CSV with the hashes in its first line.
"""

import csv
import hashlib
import io
import json
import subprocess
import sys
import tarfile
import tempfile
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))

import classify  # noqa: E402  (the sibling modules, after the path above)
import clean_copy  # noqa: E402
import rules  # noqa: E402

# The pinned version (the same as V5). Changing the image is a new instrument.
SEMGREP_IMAGE = "semgrep/semgrep@sha256:30e6afa99ebd8e7b4115d4904898108eb4bf77025819e9263f09cf14e6f6e549"


def short_hash(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()[:16]


def files_as_tar(clean_packages: Path) -> bytes:
    """The clean copy and the rules in one tar, to go into the container through stdin. A bind mount of a Windows
    folder makes Semgrep read each file across the Docker VM boundary: on V5's 220 packages it ran at 4% CPU for
    half an hour (2026-10-14). Inside the container the files are on the container's own disk."""
    buffer = io.BytesIO()
    with tarfile.open(fileobj=buffer, mode="w") as archive:
        archive.add(clean_packages, arcname="packages")
        archive.add(rules.RULES_FILE, arcname="rules/rules.yml")
    return buffer.getvalue()


def run_semgrep(clean_packages: Path, json_output: Path) -> None:
    inside_the_container = ("mkdir /work && tar -x -C /work && semgrep --config /work/rules/rules.yml --json "
                            "--metrics=off --quiet --no-git-ignore /work/packages")
    command = ["docker", "run", "--rm", "-i", "--entrypoint", "sh", SEMGREP_IMAGE, "-c", inside_the_container]
    finished = subprocess.run(command, input=files_as_tar(clean_packages), capture_output=True, check=False)
    output = finished.stdout.decode("utf-8", errors="replace")
    if not output.strip():
        sys.exit(f"Semgrep failed (exit {finished.returncode}):\n{finished.stderr.decode('utf-8', 'replace')[-2000:]}")
    json_output.write_text(output, encoding="utf-8")


def main() -> None:
    if len(sys.argv) != 3:
        sys.exit("usage: python detect.py <packages-root> <output.csv>")
    packages_root, output_csv = Path(sys.argv[1]).resolve(), Path(sys.argv[2])
    if not packages_root.is_dir():
        sys.exit(f"folder not found: {packages_root}")
    if output_csv.exists():
        sys.exit(f"{output_csv} already exists: a result is never written over another")
    if rules.RULES_FILE.read_text(encoding="utf-8") != rules.render():
        sys.exit("rules.yml differs from what rules.py writes: run python rules.py first")

    with tempfile.TemporaryDirectory(prefix="semgrep-") as temporary:
        clean_packages = Path(temporary) / "clean"
        files_copied = clean_copy.clean_copy(packages_root, clean_packages)
        json_output = Path(temporary) / "semgrep.json"
        run_semgrep(clean_packages, json_output)
        rows = classify.classify(json.loads(json_output.read_text(encoding="utf-8")), clean_packages, "/work/packages")

    output_csv.parent.mkdir(parents=True, exist_ok=True)
    with output_csv.open("w", encoding="utf-8", newline="") as file:
        file.write(f"# rules {short_hash(rules.RULES_FILE)}  classifier {short_hash(HERE / 'classify.py')}  "
                   f"clean-copy {short_hash(HERE / 'clean_copy.py')}  image {SEMGREP_IMAGE.rsplit(':', 1)[1][:16]}\n")
        writer = csv.writer(file, lineterminator="\n")
        writer.writerow(classify.HEADER)
        writer.writerows(rows)
    print(f"{len(rows)} packages ({files_copied} files) -> {output_csv}")


if __name__ == "__main__":
    main()
