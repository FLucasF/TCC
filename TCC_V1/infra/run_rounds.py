"""Repeated rounds of the study: for one study and one model, the given scenarios x N0/N1/N2 x the given rounds, a few
at a time, through each study's run_one_scenario.py. A run that already has a meta.json is skipped. Prints one line per
run and the total cost; checking the subscription's quota between batches is up to whoever runs it. While the file
TCC_V1/infra/stop-rounds exists no new run starts and the ones already running finish, so nothing is cut in the middle.

Usage (from anywhere):
    python TCC_V1/infra/run_rounds.py <construction|maintenance> <model> <workers> <scenarios> <rounds>
    python TCC_V1/infra/run_rounds.py maintenance claude-haiku-4-5 3 01,03,09 2,3,4,5
"""

import json
import subprocess
import sys
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path

TCC_V1 = Path(__file__).resolve().parents[1]
STOP_FILE = TCC_V1 / "infra" / "stop-rounds"
LABELS = {"01": "01-strategy", "03": "03-observer", "05": "05-factory-method", "07": "07-decorator",
          "09": "09-template-method"}
ARMS = ("N0", "N1", "N2")


def run(study: str, scenario: str, arm: str, model: str, replica: str) -> tuple[str, float]:
    folder = TCC_V1 / study / "runs" / LABELS[scenario] / f"{arm}-{model.removeprefix('claude-')}-r{replica}"
    if (folder / "meta.json").is_file():
        return f"{study} {scenario} {folder.name}: already done", 0.0
    if STOP_FILE.exists():
        return f"{study} {scenario} {folder.name}: not started (stop file)", 0.0
    script = TCC_V1 / study / "infra" / "scripts" / "run_one_scenario.py"
    done = subprocess.run([sys.executable, str(script), scenario, arm, model, "medium", "--replica", replica],
                          cwd=TCC_V1 / study, capture_output=True, text=True, encoding="utf-8", errors="replace",
                          check=False)
    if not (folder / "meta.json").is_file():
        return f"{study} {scenario} {folder.name}: NO META (exit {done.returncode}) {done.stderr.strip()[-300:]}", 0.0
    meta = json.loads((folder / "meta.json").read_text(encoding="utf-8"))
    cost = float(meta.get("cost_usd") or 0)
    return (f"{study} {scenario} {folder.name}: agent {meta['run_exit_code']} build {meta['build_exit_code']} "
            f"US$ {cost:.2f} {meta['seconds']}s"), cost


def main() -> None:
    if len(sys.argv) != 6:
        sys.exit(__doc__)
    study, model, workers = sys.argv[1], sys.argv[2], int(sys.argv[3])
    scenarios, rounds = sys.argv[4].split(","), sys.argv[5].split(",")
    jobs = [(scenario, arm, replica) for replica in rounds for scenario in scenarios for arm in ARMS]
    total = 0.0
    with ThreadPoolExecutor(max_workers=workers) as pool:
        for line, cost in pool.map(lambda job: run(study, job[0], job[1], model, job[2]), jobs):
            print(line, flush=True)
            total += cost
    print(f"batch done: {study} {model}, {len(jobs)} jobs, US$ {total:.2f}", flush=True)


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8")  # type: ignore[union-attr]
    main()
