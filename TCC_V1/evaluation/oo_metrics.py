"""The object-oriented metrics of every TCC_V1 run, by the same CK as construction-v5 (version 0.7.1, checked by its
sha256): coupling (CBO), cohesion (LCOM), complexity per class (WMC), response (RFC), inheritance (DIT, NOC), and how
many interfaces, enums, records and abstract methods each project has. It only reads the source code: nothing is
built and no agent runs. Descriptive material for the text, outside the review.

Usage (from TCC_V1):
    python evaluation/oo_metrics.py [--workers N]

Output in evaluation/oo-metrics/: <study>/<scenario>/<run>/class.csv (one line per class, as CK writes it) and
oo-metrics.csv (one line per run). The summary follows construction-v5's aggregate-metrics.mjs: counts, means and
maximums over the named types (class, interface, enum, record); CK's "anonymous" types (anonymous classes and the
body of each enum constant with its own behaviour) go to a column of their own, and the WMC sum counts every type.
"""

import csv
import hashlib
import shutil
import subprocess
import sys
import tempfile
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path
from statistics import mean

TCC_V1 = Path(__file__).resolve().parents[1]
CK_JAR = (TCC_V1.parent / "construction-v5" / "evaluation" / "tools" / "ck"
          / "ck-0.7.1-8a1ef916-jar-with-dependencies.jar")
CK_SHA256 = "f3c0e5bc159189ebd213badf693ab792a31cbea749eb9f97b9185938c54a7baf"
OUTPUT = TCC_V1 / "evaluation" / "oo-metrics"
STUDIES = ("construction", "maintenance")
NAMED_TYPES = ("class", "interface", "enum", "record")
COLUMNS = ["study", "scenario", "arm", "model", "replica", "status", "ck_types", "ck_class", "ck_interface", "ck_enum",
           "ck_record", "ck_anonymous", "ck_abstract_methods", "ck_cbo_mean", "ck_cbo_max", "ck_wmc_sum",
           "ck_wmc_max", "ck_lcom_mean", "ck_rfc_mean", "ck_dit_max", "ck_noc_sum"]


def die(message: str) -> None:
    sys.exit(f"ERROR: {message}")


def source_folder(workspace: Path) -> Path | None:
    """src/main/java of the shallowest pom.xml, the same rule as the build and the review."""
    poms = sorted((p for p in workspace.rglob("pom.xml") if "target" not in p.parts), key=lambda p: len(p.parts))
    if not poms:
        return None
    folder = poms[0].parent / "src" / "main" / "java"
    return folder if folder.is_dir() else None


def number(text: str) -> float | None:
    try:
        return float(text)
    except ValueError:
        return None


def summarize(rows: list[dict[str, str]]) -> dict[str, str]:
    named = [row for row in rows if row["type"] in NAMED_TYPES]

    def values(key: str, source: list[dict[str, str]]) -> list[float]:
        return [v for v in (number(row[key]) for row in source) if v is not None]

    def fmt(value: float | None) -> str:
        return "" if value is None else f"{value:.2f}".rstrip("0").rstrip(".")

    summary = {"ck_types": str(len(named)), "ck_anonymous": str(sum(row["type"] == "anonymous" for row in rows))}
    for kind in NAMED_TYPES:
        summary[f"ck_{kind}"] = str(sum(row["type"] == kind for row in named))
    summary["ck_abstract_methods"] = fmt(sum(values("abstractMethodsQty", named)))
    summary["ck_cbo_mean"] = fmt(mean(values("cbo", named)) if named else None)
    summary["ck_cbo_max"] = fmt(max(values("cbo", named), default=None))
    summary["ck_wmc_sum"] = fmt(sum(values("wmc", rows)))
    summary["ck_wmc_max"] = fmt(max(values("wmc", named), default=None))
    summary["ck_lcom_mean"] = fmt(mean(values("lcom", named)) if named else None)
    summary["ck_rfc_mean"] = fmt(mean(values("rfc", named)) if named else None)
    summary["ck_dit_max"] = fmt(max(values("dit", named), default=None))
    summary["ck_noc_sum"] = fmt(sum(values("noc", named)))
    return summary


def measure(run_folder: Path) -> dict[str, str]:
    study = run_folder.parents[2].name
    scenario = run_folder.parent.name
    arm, rest = run_folder.name.split("-", 1)
    model, _, replica = rest.partition("-r")
    line = {"study": study, "scenario": scenario, "arm": arm, "model": model, "replica": replica or "1"}
    destination = OUTPUT / study / scenario / run_folder.name
    destination.mkdir(parents=True, exist_ok=True)
    source = source_folder(run_folder / "workspace")
    if source is None:
        return {**line, "status": "sem_codigo"}
    with tempfile.TemporaryDirectory() as scratch:
        done = subprocess.run(["java", "-jar", str(CK_JAR), str(source), "false", "0", "false", scratch + "/"],
                              capture_output=True, text=True, encoding="utf-8", errors="replace", check=False)
        produced = Path(scratch) / "class.csv"
        if done.returncode != 0 or not produced.is_file():
            return {**line, "status": "ck_falhou"}
        shutil.copyfile(produced, destination / "class.csv")
    with (destination / "class.csv").open(encoding="utf-8", newline="") as handle:
        rows = list(csv.DictReader(handle))
    return {**line, "status": "ok", **summarize(rows)}


def main() -> None:
    workers = int(sys.argv[sys.argv.index("--workers") + 1]) if "--workers" in sys.argv else 4
    if not CK_JAR.is_file():
        die(f"CK not found in {CK_JAR}")
    if hashlib.sha256(CK_JAR.read_bytes()).hexdigest() != CK_SHA256:
        die("the CK jar is not the one construction-v5 used (sha256 differs)")
    runs = sorted(meta.parent for study in STUDIES for meta in (TCC_V1 / study / "runs").glob("*/*/meta.json"))
    with ThreadPoolExecutor(max_workers=workers) as pool:
        lines = list(pool.map(measure, runs))
    with (OUTPUT / "oo-metrics.csv").open("w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=COLUMNS, lineterminator="\n")
        writer.writeheader()
        writer.writerows(lines)
    statuses = [line["status"] for line in lines]
    print(f"{len(lines)} runs: " + ", ".join(f"{s} {statuses.count(s)}" for s in sorted(set(statuses))))


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8")  # type: ignore[union-attr]
    main()
