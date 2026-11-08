"""The reviews still missing in one study, a few at a time, each through evaluation/quality/review_runs.py: a finished
run without a review.md gets one review, with the same reviewer, model and effort as all the others. Prints one line per
review; checking the subscription's quota between batches is up to whoever runs it. While the file
TCC_V1/infra/stop-rounds exists no new review starts and the ones already running finish, so nothing is cut in the
middle.

Usage (from anywhere):
    python TCC_V1/infra/review_rounds.py <construction|maintenance> <workers>
    python TCC_V1/infra/review_rounds.py maintenance 6
"""

import subprocess
import sys
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path

TCC_V1 = Path(__file__).resolve().parents[1]
QUALITY = TCC_V1 / "evaluation" / "quality"
STOP_FILE = TCC_V1 / "infra" / "stop-rounds"


def review(study: str, run_folder: Path) -> str:
    label = f"{study} {run_folder.parent.name} {run_folder.name}"
    if STOP_FILE.exists():
        return f"{label}: not started (stop file)"
    done = subprocess.run([sys.executable, str(QUALITY / "review_runs.py"), "review", str(run_folder), "--resume"],
                          cwd=TCC_V1, capture_output=True, text=True, encoding="utf-8", errors="replace", check=False)
    if not (QUALITY / "results" / study / run_folder.parent.name / run_folder.name / "review.md").is_file():
        return f"{label}: NO REVIEW (exit {done.returncode}) {done.stderr.strip()[-300:]}"
    return done.stdout.strip() or f"{label}: reviewed"


def main() -> None:
    if len(sys.argv) != 3:
        sys.exit(__doc__)
    study, workers = sys.argv[1], int(sys.argv[2])
    runs = sorted(meta.parent for meta in (TCC_V1 / study / "runs").glob("*/*/meta.json"))
    missing = [run for run in runs
               if not (QUALITY / "results" / study / run.parent.name / run.name / "review.md").is_file()]
    with ThreadPoolExecutor(max_workers=workers) as pool:
        for line in pool.map(lambda run: review(study, run), missing):
            print(line, flush=True)
    print(f"batch done: {study}, {len(missing)} missing at the start", flush=True)


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8")  # type: ignore[union-attr]
    main()
