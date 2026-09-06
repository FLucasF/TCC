"""Reads the manifest, maps changed paths to boundaries, runs the declared commands.

Pure module: no SDK dependency, no network. The hooks in .claude/settings.json call
it; tests call verify() directly.
"""

from __future__ import annotations

import fnmatch
import json
import os
import shutil
import subprocess
import sys
import time
from dataclasses import dataclass, field
from datetime import datetime, timezone
from pathlib import Path
from typing import Callable, Literal

HARNESS_ROOT = Path(__file__).resolve().parent.parent
MANIFEST = HARNESS_ROOT / ".claude" / "validation.json"
STATE = HARNESS_ROOT / "verify" / ".state.json"
TRACE = HARNESS_ROOT / "verify" / ".trace.jsonl"
TRACE_SCHEMA = "harness.trace.v1"

Phase = Literal["fast", "full"]
Outcome = Literal["UNMAPPED", "NO_CHECKS", "BLOCKED", "PASS", "FAIL", "NOTE"]

MAX_OUTPUT_LINES = 40
COMMAND_TIMEOUT_S = 600
MAX_TRACE_RECORDS = 2000

# Claude Code ends the turn after 8 consecutive Stop-hook blocks. Giving up first
# keeps the harness in control of its own failure instead of being overridden.
MAX_CONSECUTIVE_BLOCKS = 4

_ERROR_MARKERS = ("error", "fail", "exception", "✗", "cannot", "expected")


@dataclass
class CommandResult:
    command: str
    outcome: Outcome
    output: str = ""
    missing: list[str] = field(default_factory=list)
    exit_code: int | None = None
    duration_s: float | None = None


@dataclass
class BoundaryResult:
    boundary: str
    outcome: Outcome
    commands: list[CommandResult] = field(default_factory=list)
    note: str = ""
    touched: list[str] = field(default_factory=list)


@dataclass
class Result:
    phase: Phase
    boundaries: list[BoundaryResult] = field(default_factory=list)
    unmapped: list[str] = field(default_factory=list)
    notes: list[str] = field(default_factory=list)

    @property
    def failed(self) -> bool:
        return any(b.outcome == "FAIL" for b in self.boundaries)


def load_manifest(path: Path = MANIFEST) -> dict:
    return json.loads(path.read_text(encoding="utf-8"))


def norm(raw: str) -> str | None:
    """Path relative to the harness root, forward slashes; None when outside it.

    Tool input arrives with backslashes on Windows; a guard written with forward
    slashes silently matches nothing.

    Only absolute paths are resolved. Path.resolve() on a relative path anchors it
    to the process working directory, which is wherever the hook happened to be
    invoked from — not the harness root.

    An absolute path outside the root is not the harness's business: a glob such
    as "*.md" would otherwise match a note in ~/.claude and run this project's
    checks on it.
    """
    p = Path(raw)
    if p.is_absolute():
        try:
            p = p.resolve().relative_to(HARNESS_ROOT)
        except ValueError:
            return None
        except OSError:
            return p.as_posix()
    text = p.as_posix()
    return text[2:] if text.startswith("./") else text


def matches(path: str, pattern: str) -> bool:
    if fnmatch.fnmatch(path, pattern):
        return True
    # "apps/backend/src/**" should match everything under it, which fnmatch alone
    # does not do for a trailing /**.
    if pattern.endswith("/**"):
        return path.startswith(pattern[:-2])
    return False


def boundary_for(path: str, manifest: dict) -> dict | None:
    for boundary in manifest["boundaries"]:
        if any(matches(path, pat) for pat in boundary["paths"]):
            return boundary
    return None


def truncate(text: str, touched: list[str] | None = None) -> str:
    lines = [ln for ln in text.splitlines() if ln.strip()]

    if touched:
        names = {Path(t).name for t in touched}
        kept = [ln for ln in lines if any(n in ln for n in names)]
        lines = kept if kept else []

    if len(lines) > MAX_OUTPUT_LINES:
        flagged = [ln for ln in lines if any(m in ln.lower() for m in _ERROR_MARKERS)]
        lines = (flagged or lines)[-MAX_OUTPUT_LINES:]
        lines.append(f"... (saída truncada em {MAX_OUTPUT_LINES} linhas)")

    return "\n".join(lines)


def run_command(spec: dict, cwd: Path, touched: list[str] | None) -> CommandResult:
    command = spec["run"]

    missing = [b for b in spec.get("prerequisites", []) if shutil.which(b) is None]
    if missing:
        return CommandResult(command, "BLOCKED", missing=missing)

    started = time.perf_counter()
    try:
        proc = subprocess.run(
            command,
            shell=True,
            cwd=cwd,
            capture_output=True,
            text=True,
            encoding="utf-8",
            errors="replace",
            timeout=COMMAND_TIMEOUT_S,
        )
    except subprocess.TimeoutExpired:
        return CommandResult(
            command, "FAIL",
            output=f"timeout após {COMMAND_TIMEOUT_S}s",
            duration_s=time.perf_counter() - started,
        )
    elapsed = time.perf_counter() - started

    filter_to = touched if spec.get("filterToTouched") else None
    body = truncate((proc.stdout or "") + "\n" + (proc.stderr or ""), filter_to)

    # Informational tools report findings without failing, so their exit code is not
    # a verdict. jscpd exits 0 with clones present; with a threshold set it exits
    # non-zero for any duplication at all, including clones unrelated to this edit.
    # Neither maps onto PASS/FAIL, so their output is a note instead.
    if spec.get("reportOnly"):
        return CommandResult(
            command, "NOTE" if body.strip() else "PASS", output=body,
            exit_code=proc.returncode, duration_s=elapsed,
        )

    if proc.returncode == 0:
        return CommandResult(command, "PASS", exit_code=0, duration_s=elapsed)

    return CommandResult(
        command, "FAIL", output=body, exit_code=proc.returncode, duration_s=elapsed
    )


def verify(
    paths: list[str],
    phase: Phase = "fast",
    manifest: dict | None = None,
    *,
    trace: Path | None = None,
    now: Callable[[], datetime] | None = None,
) -> Result:
    manifest = manifest or load_manifest()
    result = Result(phase=phase)

    grouped: dict[str, tuple[dict, list[str]]] = {}
    for raw in paths:
        path = norm(raw)
        if path is None:
            continue
        boundary = boundary_for(path, manifest)
        if boundary is None:
            if manifest.get("unmappedPathPolicy", "report") == "report":
                result.unmapped.append(path)
            continue
        grouped.setdefault(boundary["id"], (boundary, []))[1].append(path)

    for boundary, touched in grouped.values():
        result.boundaries.append(_run_boundary(boundary, touched, phase))

    _record(result, trace, now)
    return result


def _run_boundary(boundary: dict, touched: list[str], phase: Phase) -> BoundaryResult:
    specs = boundary.get("fast" if phase == "fast" else "commands", [])

    if not specs:
        note = (
            "nenhuma verificação rápida declarada"
            if phase == "fast"
            else "boundary sem validação executável"
        )
        return BoundaryResult(boundary["id"], "NO_CHECKS", note=note, touched=touched)

    cwd = (HARNESS_ROOT / boundary.get("workingDirectory", ".")).resolve()
    runs = [run_command(spec, cwd, touched) for spec in specs]

    verdicts = [r for r in runs if r.outcome != "NOTE"]
    if any(r.outcome == "FAIL" for r in verdicts):
        outcome: Outcome = "FAIL"
    elif any(r.outcome == "BLOCKED" for r in verdicts):
        outcome = "BLOCKED"
    else:
        outcome = "PASS"

    return BoundaryResult(boundary["id"], outcome, commands=runs, touched=touched)


def verify_boundaries(
    ids: list[str],
    phase: Phase,
    manifest: dict | None = None,
    *,
    trace: Path | None = None,
    now: Callable[[], datetime] | None = None,
) -> Result:
    """Run whole boundaries by id.

    The gate knows which boundaries are dirty, not which files changed. Turning an
    id back into a path meant synthesising one from the glob, and the synthesised
    path did not match its own pattern — so the gate passed without running
    anything, silently.
    """
    manifest = manifest or load_manifest()
    by_id = {b["id"]: b for b in manifest["boundaries"]}
    result = Result(phase=phase)
    for bid in ids:
        if bid in by_id:
            result.boundaries.append(_run_boundary(by_id[bid], [], phase))
    _record(result, trace, now)
    return result


def _utcnow() -> datetime:
    return datetime.now(timezone.utc)


def _record(result: Result, trace: Path | None, now: Callable[[], datetime] | None) -> None:
    trace = trace or TRACE
    records = _trace_records(result, now or _utcnow)
    if not records:
        return

    try:
        trace.parent.mkdir(parents=True, exist_ok=True)
        with open(trace, "a", encoding="utf-8", newline="\n") as f:
            for entry in records:
                f.write(json.dumps(entry, ensure_ascii=False) + "\n")
                f.flush()
    except Exception as exc:  # memória é best-effort; a verificação nunca muda por causa dela
        result.notes.append(f"traço não gravado em {trace}: {type(exc).__name__}: {exc}")
        return

    try:
        _trim(trace)
    except OSError as exc:
        result.notes.append(
            f"traço gravado, mas não aparado em {trace}: {type(exc).__name__}: {exc}"
        )


def _trace_records(result: Result, now: Callable[[], datetime]) -> list[dict]:
    stamp = now()
    if stamp.tzinfo is None:
        raise ValueError("o relógio do traço deve devolver um datetime com timezone")
    ts = stamp.isoformat()

    def rec(boundary: str | None, command: str | None, outcome: Outcome,
            duration_s: float | None, exit_code: int | None, touched: list[str]) -> dict:
        return {
            "schema": TRACE_SCHEMA,
            "ts": ts,
            "boundary": boundary,
            "phase": result.phase,
            "command": command,
            "outcome": outcome,
            "duration_s": duration_s,
            "exit_code": exit_code,
            "touched": touched,
        }

    records = [rec(None, None, "UNMAPPED", None, None, [path]) for path in result.unmapped]
    for b in result.boundaries:
        if b.outcome == "NO_CHECKS":
            records.append(rec(b.boundary, None, "NO_CHECKS", None, None, b.touched))
            continue
        for cmd in b.commands:
            entry = rec(b.boundary, cmd.command, cmd.outcome,
                        cmd.duration_s, cmd.exit_code, b.touched)
            if cmd.outcome == "FAIL":
                entry["output_head"] = cmd.output
            records.append(entry)
    return records


def _trim(trace: Path) -> None:
    with open(trace, encoding="utf-8", newline="\n") as f:
        lines = f.readlines()
    if len(lines) <= MAX_TRACE_RECORDS:
        return
    tmp = trace.with_name(f"{trace.name}.{os.getpid()}.tmp")
    with open(tmp, "w", encoding="utf-8", newline="\n") as f:
        f.writelines(lines[-MAX_TRACE_RECORDS:])
    tmp.replace(trace)


def format_for_model(result: Result) -> str:
    if not result.boundaries and not result.unmapped and not result.notes:
        return ""

    out = [f"[harness] verificação {result.phase}"]

    for path in result.unmapped:
        out.append(f"  UNMAPPED: {path} não cai em nenhuma boundary do manifesto")

    for b in result.boundaries:
        if b.outcome == "NO_CHECKS":
            out.append(f"  {b.boundary}: NO_CHECKS — {b.note}")
            continue
        for cmd in b.commands:
            head = f"  {b.boundary}: {cmd.outcome} · {cmd.command}"
            if cmd.outcome == "BLOCKED":
                out.append(f"{head}\n    ausente: {', '.join(cmd.missing)}")
            elif cmd.outcome == "NOTE":
                body = "\n".join(f"    {ln}" for ln in cmd.output.splitlines())
                out.append(f"  {b.boundary}: NOTA · {cmd.command}\n{body}")
            elif cmd.outcome == "FAIL":
                body = "\n".join(f"    {ln}" for ln in cmd.output.splitlines())
                out.append(f"{head}\n{body}")
            else:
                out.append(head)

    for note in result.notes:
        out.append(f"  NOTA · {note}")

    return "\n".join(out)


def _load_state() -> dict:
    try:
        return json.loads(STATE.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError):
        return {"dirty": [], "blocks": 0}


def _save_state(state: dict) -> None:
    STATE.parent.mkdir(parents=True, exist_ok=True)
    STATE.write_text(json.dumps(state), encoding="utf-8")


def _paths_from_hook(payload: dict) -> list[str]:
    tool_input = payload.get("tool_input") or {}
    found = [tool_input[k] for k in ("file_path", "notebook_path") if tool_input.get(k)]
    for edit in tool_input.get("edits") or []:
        if isinstance(edit, dict) and edit.get("file_path"):
            found.append(edit["file_path"])
    return found


def _hook_output(context: str) -> None:
    print(json.dumps({
        "hookSpecificOutput": {
            "hookEventName": "PostToolUse",
            "additionalContext": context,
        }
    }))


def main(argv: list[str]) -> int:
    if "--hook" in argv:
        payload = json.load(sys.stdin)
        paths = _paths_from_hook(payload)
        if not paths:
            return 0

        result = verify(paths, phase="fast")
        state = _load_state()
        state["dirty"] = sorted({*state.get("dirty", []), *(b.boundary for b in result.boundaries)})
        _save_state(state)

        text = format_for_model(result)
        if text:
            _hook_output(text)
        return 0

    if "--gate" in argv:
        payload = json.load(sys.stdin)
        state = _load_state()

        if payload.get("stop_hook_active") or not state.get("dirty"):
            return 0

        if state.get("blocks", 0) >= MAX_CONSECUTIVE_BLOCKS:
            state["blocks"] = 0
            _save_state(state)
            print(json.dumps({"systemMessage":
                "[harness] portão de conclusão desistiu após "
                f"{MAX_CONSECUTIVE_BLOCKS} bloqueios. Verificação não confirmada."}))
            return 0

        result = verify_boundaries(state["dirty"], phase="full")

        if result.failed:
            state["blocks"] = state.get("blocks", 0) + 1
            _save_state(state)
            # Exit 2 rather than a JSON decision: three readings of the hooks
            # reference disagreed on the Stop payload shape (top-level `decision`,
            # `hookSpecificOutput.decision`, `hookSpecificOutput.permissionDecision`).
            # All three agreed that exit 2 blocks and stderr reaches the model.
            print(format_for_model(result), file=sys.stderr)
            return 2

        _save_state({"dirty": [], "blocks": 0})
        if result.notes:
            print(json.dumps({"systemMessage": "[harness] " + "; ".join(result.notes)}))
        return 0

    phase: Phase = "full" if "--phase" in argv and "full" in argv else "fast"
    paths = [a for a in argv if not a.startswith("--") and a != phase]
    print(format_for_model(verify(paths, phase=phase)) or "[harness] nada a verificar")
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
