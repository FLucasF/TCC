// @ts-check
/**
 * Reads the manifest, maps changed paths to boundaries, runs the declared commands.
 *
 * Pure module: no dependency outside Node's standard library, no network. The hooks
 * in .claude/settings.json call it; tests call verify() directly.
 *
 * Node was chosen over Python because the frontend boundary and the jscpd check
 * already require it, so the executor adds no runtime the project was not paying
 * for. Types are JSDoc rather than TypeScript syntax: `tsc --checkJs` gives the same
 * checking, and a plain .mjs file imposes no minimum Node version.
 */

import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import process from "node:process";
import { spawnSync } from "node:child_process";
import { fileURLToPath } from "node:url";

export const HARNESS_ROOT = path.resolve(
  path.dirname(fileURLToPath(import.meta.url)),
  "..",
);
export const MANIFEST = path.join(HARNESS_ROOT, ".claude", "validation.json");
export const STATE = path.join(HARNESS_ROOT, "verify", ".state.json");
export const TRACE_SCHEMA = "harness.trace.v1";

/**
 * Mutable knobs. ES module bindings cannot be reassigned from outside, so the two
 * values tests need to redirect live here instead of as module constants.
 */
export const config = {
  trace: path.join(HARNESS_ROOT, "verify", ".trace.jsonl"),
  maxTraceRecords: 2000,
};

export const MAX_OUTPUT_LINES = 40;
export const COMMAND_TIMEOUT_S = 600;

// Command output is captured in full; the default 1 MB buffer silently truncates a
// Maven run. The cap only exists to stop a runaway process from exhausting memory.
const MAX_CAPTURE_BYTES = 64 * 1024 * 1024;

// Claude Code ends the turn after 8 consecutive Stop-hook blocks. Giving up first
// keeps the harness in control of its own failure instead of being overridden.
export const MAX_CONSECUTIVE_BLOCKS = 4;

const ERROR_MARKERS = ["error", "fail", "exception", "✗", "cannot", "expected"];

/** @typedef {"fast"|"full"} Phase */
/** @typedef {"UNMAPPED"|"NO_CHECKS"|"BLOCKED"|"PASS"|"FAIL"|"NOTE"} Outcome */

/**
 * @typedef {object} CommandSpec
 * @property {string} run
 * @property {string[]} [prerequisites]
 * @property {boolean} [filterToTouched]
 * @property {boolean} [reportOnly]
 */

/**
 * @typedef {object} Boundary
 * @property {string} id
 * @property {string[]} paths
 * @property {string} [workingDirectory]
 * @property {CommandSpec[]} [fast]
 * @property {CommandSpec[]} [commands]
 */

/**
 * @typedef {object} Manifest
 * @property {Boundary[]} boundaries
 * @property {string} [unmappedPathPolicy]
 */

/**
 * @typedef {object} CommandResult
 * @property {string} command
 * @property {Outcome} outcome
 * @property {string} output
 * @property {string[]} missing
 * @property {number|null} exitCode
 * @property {number|null} durationS
 */

/**
 * @typedef {object} BoundaryResult
 * @property {string} boundary
 * @property {Outcome} outcome
 * @property {CommandResult[]} commands
 * @property {string} note
 * @property {string[]} touched
 */

/**
 * @typedef {object} Result
 * @property {Phase} phase
 * @property {BoundaryResult[]} boundaries
 * @property {string[]} unmapped
 * @property {string[]} notes
 */

/**
 * @param {Result} result
 * @returns {boolean}
 */
export function failed(result) {
  return result.boundaries.some((b) => b.outcome === "FAIL");
}

// --------------------------------------------------------------- runtime guards
//
// The four places this module meets the outside world — the manifest on disk, the
// hook payload on stdin, the state file, and subprocess output — carry no types at
// runtime. Each is validated here rather than asserted with a cast, because a cast
// would only move the failure somewhere less obvious.

/**
 * @param {unknown} value
 * @param {string} source
 * @returns {Manifest}
 */
export function asManifest(value, source) {
  const fail = (/** @type {string} */ why) => {
    throw new TypeError(`manifesto inválido em ${source}: ${why}`);
  };
  if (typeof value !== "object" || value === null || Array.isArray(value)) {
    fail("a raiz não é um objeto");
  }
  const root = /** @type {Record<string, unknown>} */ (value);
  if (!Array.isArray(root.boundaries)) fail("`boundaries` não é uma lista");

  for (const raw of /** @type {unknown[]} */ (root.boundaries)) {
    if (typeof raw !== "object" || raw === null || Array.isArray(raw)) {
      fail("uma boundary não é um objeto");
    }
    const b = /** @type {Record<string, unknown>} */ (raw);
    if (typeof b.id !== "string" || !b.id) fail("boundary sem `id`");
    if (!Array.isArray(b.paths) || b.paths.some((p) => typeof p !== "string")) {
      fail(`boundary ${String(b.id)}: \`paths\` não é uma lista de strings`);
    }
    if (b.workingDirectory !== undefined && typeof b.workingDirectory !== "string") {
      fail(`boundary ${String(b.id)}: \`workingDirectory\` não é string`);
    }
    for (const key of /** @type {const} */ (["fast", "commands"])) {
      const specs = b[key];
      if (specs === undefined) continue;
      if (!Array.isArray(specs)) fail(`boundary ${String(b.id)}: \`${key}\` não é lista`);
      for (const spec of /** @type {unknown[]} */ (specs)) {
        if (typeof spec !== "object" || spec === null) {
          fail(`boundary ${String(b.id)}: comando em \`${key}\` não é objeto`);
        }
        const s = /** @type {Record<string, unknown>} */ (spec);
        if (typeof s.run !== "string" || !s.run) {
          fail(`boundary ${String(b.id)}: comando em \`${key}\` sem \`run\``);
        }
        if (s.prerequisites !== undefined
          && (!Array.isArray(s.prerequisites)
            || s.prerequisites.some((p) => typeof p !== "string"))) {
          fail(`boundary ${String(b.id)}: \`prerequisites\` não é lista de strings`);
        }
      }
    }
  }
  return /** @type {Manifest} */ (value);
}

/**
 * @param {string} [file]
 * @returns {Manifest}
 */
export function loadManifest(file = MANIFEST) {
  return asManifest(JSON.parse(fs.readFileSync(file, "utf-8")), file);
}

/**
 * Paths edited, as reported by the hook payload. Anything absent or of the wrong
 * shape is skipped rather than guessed at.
 *
 * @param {unknown} payload
 * @returns {string[]}
 */
export function pathsFromHook(payload) {
  if (typeof payload !== "object" || payload === null) return [];
  const input = /** @type {Record<string, unknown>} */ (payload).tool_input;
  if (typeof input !== "object" || input === null) return [];
  const toolInput = /** @type {Record<string, unknown>} */ (input);

  const found = [];
  for (const key of ["file_path", "notebook_path"]) {
    const value = toolInput[key];
    if (typeof value === "string" && value) found.push(value);
  }
  const edits = toolInput.edits;
  if (Array.isArray(edits)) {
    for (const edit of edits) {
      if (typeof edit === "object" && edit !== null) {
        const filePath = /** @type {Record<string, unknown>} */ (edit).file_path;
        if (typeof filePath === "string" && filePath) found.push(filePath);
      }
    }
  }
  return found;
}

// ------------------------------------------------------------- path and globbing

/**
 * Path relative to the harness root, forward slashes; null when outside it.
 *
 * Tool input arrives with backslashes on Windows; a guard written with forward
 * slashes silently matches nothing.
 *
 * Only absolute paths are resolved. Resolving a relative path would anchor it to
 * the process working directory, which is wherever the hook happened to be invoked
 * from — not the harness root.
 *
 * An absolute path outside the root is not the harness's business: a glob such as
 * "*.md" would otherwise match a note in ~/.claude and run this project's checks
 * on it.
 *
 * @param {string} raw
 * @returns {string|null}
 */
export function norm(raw) {
  let text;
  if (path.isAbsolute(raw) || /^[a-zA-Z]:[\\/]/.test(raw)) {
    const resolved = path.resolve(raw);
    const relative = path.relative(HARNESS_ROOT, resolved);
    if (!relative || relative.startsWith("..") || path.isAbsolute(relative)) return null;
    text = relative;
  } else {
    text = raw;
  }
  text = text.split(path.sep).join("/").split("\\").join("/");
  return text.startsWith("./") ? text.slice(2) : text;
}

const globCache = new Map();

/**
 * fnmatch semantics, deliberately case-sensitive on every platform.
 *
 * Python's fnmatch folds case on Windows and not elsewhere, which would make the
 * same manifest behave differently per operating system. A harness that claims to
 * be portable should not carry that.
 *
 * @param {string} pattern
 * @returns {RegExp}
 */
function globToRegExp(pattern) {
  const cached = globCache.get(pattern);
  if (cached) return cached;

  let out = "";
  for (let i = 0; i < pattern.length; i += 1) {
    const c = pattern[i];
    if (c === "*") {
      out += ".*";
    } else if (c === "?") {
      out += ".";
    } else if (c === "[") {
      const close = pattern.indexOf("]", i + 1);
      if (close === -1) {
        out += "\\[";
      } else {
        let body = pattern.slice(i + 1, close);
        i = close;
        let negate = "";
        if (body.startsWith("!") || body.startsWith("^")) {
          negate = "^";
          body = body.slice(1);
        }
        out += `[${negate}${body.replace(/\\/g, "\\\\").replace(/\]/g, "\\]")}]`;
      }
    } else {
      out += c.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
    }
  }

  const re = new RegExp(`^${out}$`);
  globCache.set(pattern, re);
  return re;
}

/**
 * @param {string} filePath
 * @param {string} pattern
 * @returns {boolean}
 */
export function matches(filePath, pattern) {
  if (globToRegExp(pattern).test(filePath)) return true;
  // "apps/backend/src/**" should match everything under it, which the glob alone
  // does not do for a trailing /**.
  if (pattern.endsWith("/**")) return filePath.startsWith(pattern.slice(0, -2));
  return false;
}

/**
 * @param {string|null} filePath
 * @param {Manifest} manifest
 * @returns {Boundary|null}
 */
export function boundaryFor(filePath, manifest) {
  if (filePath === null) return null;
  for (const boundary of manifest.boundaries) {
    if (boundary.paths.some((pat) => matches(filePath, pat))) return boundary;
  }
  return null;
}

// ------------------------------------------------------------------ subprocesses

/**
 * The `prerequisites` check: is this name runnable? Equivalent to shutil.which,
 * including PATHEXT handling, which is what makes `npx` and `mvn` resolve on
 * Windows where they are .cmd files rather than bare executables.
 *
 * @param {string} name
 * @returns {string|null}
 */
export function which(name) {
  const isWindows = process.platform === "win32";
  const exts = isWindows
    ? (process.env.PATHEXT || ".COM;.EXE;.BAT;.CMD")
      .split(";").map((e) => e.trim()).filter(Boolean)
    : [];
  const alreadyExt = isWindows
    && exts.some((e) => name.toLowerCase().endsWith(e.toLowerCase()));
  const suffixes = isWindows ? (alreadyExt ? [""] : ["", ...exts]) : [""];

  const runnable = (/** @type {string} */ candidate) => {
    try {
      if (!fs.statSync(candidate).isFile()) return false;
      if (!isWindows) fs.accessSync(candidate, fs.constants.X_OK);
      return true;
    } catch {
      return false;
    }
  };

  // A prerequisite carrying a separator is a path, not a PATH lookup, and it is
  // resolved against the harness root — the same anchor `paths` and
  // `workingDirectory` already use. Resolving it against the process working
  // directory would make the answer depend on where the hook happened to fire.
  if (name.includes("/") || name.includes("\\")) {
    const base = path.resolve(HARNESS_ROOT, name);
    for (const suffix of suffixes) {
      const candidate = base + suffix;
      if (runnable(candidate)) return candidate;
    }
    return null;
  }

  const dirs = (process.env.PATH || "").split(path.delimiter).filter(Boolean);
  for (const dir of dirs) {
    for (const suffix of suffixes) {
      const candidate = path.join(dir, name + suffix);
      if (runnable(candidate)) return candidate;
    }
  }
  return null;
}

/**
 * @param {string} text
 * @param {string[]|null} [touched]
 * @returns {string}
 */
export function truncate(text, touched = null) {
  let lines = text.split(/\r?\n/).filter((ln) => ln.trim());

  if (touched && touched.length) {
    const names = new Set(touched.map((t) => path.basename(t)));
    lines = lines.filter((ln) => [...names].some((n) => ln.includes(n)));
  }

  if (lines.length > MAX_OUTPUT_LINES) {
    const flagged = lines.filter(
      (ln) => ERROR_MARKERS.some((m) => ln.toLowerCase().includes(m)),
    );
    lines = (flagged.length ? flagged : lines).slice(-MAX_OUTPUT_LINES);
    lines.push(`... (saída truncada em ${MAX_OUTPUT_LINES} linhas)`);
  }

  return lines.join("\n");
}

/**
 * @param {CommandSpec} spec
 * @param {string} cwd
 * @param {string[]|null} touched
 * @returns {CommandResult}
 */
export function runCommand(spec, cwd, touched) {
  const command = spec.run;

  const missing = (spec.prerequisites || []).filter((b) => which(b) === null);
  if (missing.length) {
    return {
      command, outcome: "BLOCKED", output: "", missing,
      exitCode: null, durationS: null,
    };
  }

  const started = performance.now();
  const proc = spawnSync(command, {
    shell: true,
    cwd,
    encoding: "utf-8",
    timeout: COMMAND_TIMEOUT_S * 1000,
    maxBuffer: MAX_CAPTURE_BYTES,
    windowsHide: true,
  });
  const elapsed = (performance.now() - started) / 1000;

  if (proc.error) {
    const timedOut = /** @type {NodeJS.ErrnoException} */ (proc.error).code === "ETIMEDOUT";
    return {
      command,
      outcome: "FAIL",
      output: timedOut
        ? `timeout após ${COMMAND_TIMEOUT_S}s`
        : `não foi possível executar: ${proc.error.message}`,
      missing: [],
      exitCode: null,
      durationS: elapsed,
    };
  }

  const filterTo = spec.filterToTouched ? touched : null;
  const body = truncate(`${proc.stdout || ""}\n${proc.stderr || ""}`, filterTo);

  // Informational tools report findings without failing, so their exit code is not
  // a verdict. jscpd exits 0 with clones present; with a threshold set it exits
  // non-zero for any duplication at all, including clones unrelated to this edit.
  // Neither maps onto PASS/FAIL, so their output is a note instead.
  if (spec.reportOnly) {
    return {
      command,
      outcome: body.trim() ? "NOTE" : "PASS",
      output: body,
      missing: [],
      exitCode: proc.status,
      durationS: elapsed,
    };
  }

  if (proc.status === 0) {
    return {
      command, outcome: "PASS", output: "", missing: [],
      exitCode: 0, durationS: elapsed,
    };
  }

  return {
    command, outcome: "FAIL", output: body, missing: [],
    exitCode: proc.status, durationS: elapsed,
  };
}

// -------------------------------------------------------------- the verification

/**
 * @param {Boundary} boundary
 * @param {string[]} touched
 * @param {Phase} phase
 * @returns {BoundaryResult}
 */
function runBoundary(boundary, touched, phase) {
  const specs = (phase === "fast" ? boundary.fast : boundary.commands) || [];

  if (!specs.length) {
    const note = phase === "fast"
      ? "nenhuma verificação rápida declarada"
      : "boundary sem validação executável";
    return { boundary: boundary.id, outcome: "NO_CHECKS", commands: [], note, touched };
  }

  const cwd = path.resolve(HARNESS_ROOT, boundary.workingDirectory || ".");
  const runs = specs.map((spec) => runCommand(spec, cwd, touched));

  const verdicts = runs.filter((r) => r.outcome !== "NOTE");
  /** @type {Outcome} */
  let outcome = "PASS";
  if (verdicts.some((r) => r.outcome === "FAIL")) outcome = "FAIL";
  else if (verdicts.some((r) => r.outcome === "BLOCKED")) outcome = "BLOCKED";

  return { boundary: boundary.id, outcome, commands: runs, note: "", touched };
}

/**
 * @typedef {object} VerifyOptions
 * @property {Phase} [phase]
 * @property {Manifest|null} [manifest]
 * @property {string|null} [trace]
 * @property {(() => Date)|null} [now]
 */

/**
 * @param {string[]} paths
 * @param {VerifyOptions} [options]
 * @returns {Result}
 */
export function verify(paths, options = {}) {
  const { phase = "fast", manifest = null, trace = null, now = null } = options;
  const loaded = manifest || loadManifest();
  /** @type {Result} */
  const result = { phase, boundaries: [], unmapped: [], notes: [] };

  /** @type {Map<string, {boundary: Boundary, touched: string[]}>} */
  const grouped = new Map();
  for (const raw of paths) {
    const filePath = norm(raw);
    if (filePath === null) continue;
    const boundary = boundaryFor(filePath, loaded);
    if (boundary === null) {
      if ((loaded.unmappedPathPolicy || "report") === "report") {
        result.unmapped.push(filePath);
      }
      continue;
    }
    const entry = grouped.get(boundary.id) || { boundary, touched: [] };
    entry.touched.push(filePath);
    grouped.set(boundary.id, entry);
  }

  for (const { boundary, touched } of grouped.values()) {
    result.boundaries.push(runBoundary(boundary, touched, phase));
  }

  record(result, trace, now);
  return result;
}

/**
 * Run whole boundaries by id.
 *
 * The gate knows which boundaries are dirty, not which files changed. Turning an id
 * back into a path meant synthesising one from the glob, and the synthesised path
 * did not match its own pattern — so the gate passed without running anything,
 * silently.
 *
 * @param {string[]} ids
 * @param {VerifyOptions & {phase: Phase}} options
 * @returns {Result}
 */
export function verifyBoundaries(ids, options) {
  const { phase, manifest = null, trace = null, now = null } = options;
  const loaded = manifest || loadManifest();
  const byId = new Map(loaded.boundaries.map((b) => [b.id, b]));
  /** @type {Result} */
  const result = { phase, boundaries: [], unmapped: [], notes: [] };
  for (const id of ids) {
    const boundary = byId.get(id);
    if (boundary) result.boundaries.push(runBoundary(boundary, [], phase));
  }
  record(result, trace, now);
  return result;
}

// ---------------------------------------------------------------------- the trace

/**
 * @param {(() => Date)|null} now
 * @returns {string}
 */
function isoStamp(now) {
  const stamp = (now || (() => new Date()))();
  if (!(stamp instanceof Date) || Number.isNaN(stamp.getTime())) {
    throw new TypeError("o relógio do traço deve devolver um Date válido");
  }
  // Always UTC with a Z suffix. The Python version had to reject naive datetimes
  // here; a JavaScript Date is an instant and cannot be naive.
  return stamp.toISOString();
}

/**
 * @param {Result} result
 * @param {(() => Date)|null} now
 * @returns {Record<string, unknown>[]}
 */
export function traceRecords(result, now) {
  const ts = isoStamp(now);

  /**
   * @param {string|null} boundary
   * @param {string|null} command
   * @param {Outcome} outcome
   * @param {number|null} durationS
   * @param {number|null} exitCode
   * @param {string[]} touched
   * @returns {Record<string, unknown>}
   */
  const rec = (boundary, command, outcome, durationS, exitCode, touched) => ({
    schema: TRACE_SCHEMA,
    ts,
    boundary,
    phase: result.phase,
    command,
    outcome,
    duration_s: durationS,
    exit_code: exitCode,
    touched,
  });

  /** @type {Record<string, unknown>[]} */
  const records = result.unmapped.map(
    (p) => rec(null, null, "UNMAPPED", null, null, [p]),
  );

  for (const b of result.boundaries) {
    if (b.outcome === "NO_CHECKS") {
      records.push(rec(b.boundary, null, "NO_CHECKS", null, null, b.touched));
      continue;
    }
    for (const cmd of b.commands) {
      const entry = rec(
        b.boundary, cmd.command, cmd.outcome, cmd.durationS, cmd.exitCode, b.touched,
      );
      if (cmd.outcome === "FAIL") entry.output_head = cmd.output;
      records.push(entry);
    }
  }
  return records;
}

/**
 * @param {Result} result
 * @param {string|null} trace
 * @param {(() => Date)|null} now
 */
function record(result, trace, now) {
  const file = trace || config.trace;
  const records = traceRecords(result, now);
  if (!records.length) return;

  try {
    fs.mkdirSync(path.dirname(file), { recursive: true });
    const payload = `${records.map((r) => JSON.stringify(r)).join("\n")}\n`;
    fs.appendFileSync(file, payload, "utf-8");
  } catch (err) {
    // Memory is best-effort; the verification never changes because of it.
    result.notes.push(`traço não gravado em ${file}: ${describeError(err)}`);
    return;
  }

  try {
    trim(file);
  } catch (err) {
    result.notes.push(`traço gravado, mas não aparado em ${file}: ${describeError(err)}`);
  }
}

/**
 * @param {unknown} err
 * @returns {string}
 */
function describeError(err) {
  if (err instanceof Error) {
    const code = /** @type {NodeJS.ErrnoException} */ (err).code;
    return `${code || err.name}: ${err.message}`;
  }
  return String(err);
}

/** @param {string} file */
function trim(file) {
  const lines = fs.readFileSync(file, "utf-8").split("\n");
  const trailing = lines[lines.length - 1] === "" ? lines.pop() : null;
  if (lines.length <= config.maxTraceRecords) return;

  const kept = lines.slice(-config.maxTraceRecords);
  const tmp = `${file}.${process.pid}.tmp`;
  fs.writeFileSync(tmp, `${kept.join("\n")}${trailing === null ? "" : "\n"}`, "utf-8");
  fs.renameSync(tmp, file);
}

// ------------------------------------------------------------------- the report

/**
 * @param {Result} result
 * @returns {string}
 */
export function formatForModel(result) {
  if (!result.boundaries.length && !result.unmapped.length && !result.notes.length) {
    return "";
  }

  const out = [`[harness] verificação ${result.phase}`];

  for (const p of result.unmapped) {
    out.push(`  UNMAPPED: ${p} não cai em nenhuma boundary do manifesto`);
  }

  for (const b of result.boundaries) {
    if (b.outcome === "NO_CHECKS") {
      out.push(`  ${b.boundary}: NO_CHECKS — ${b.note}`);
      continue;
    }
    for (const cmd of b.commands) {
      const head = `  ${b.boundary}: ${cmd.outcome} · ${cmd.command}`;
      if (cmd.outcome === "BLOCKED") {
        out.push(`${head}\n    ausente: ${cmd.missing.join(", ")}`);
      } else if (cmd.outcome === "NOTE") {
        const body = cmd.output.split("\n").map((ln) => `    ${ln}`).join("\n");
        out.push(`  ${b.boundary}: NOTA · ${cmd.command}\n${body}`);
      } else if (cmd.outcome === "FAIL") {
        const body = cmd.output.split("\n").map((ln) => `    ${ln}`).join("\n");
        out.push(`${head}\n${body}`);
      } else {
        out.push(head);
      }
    }
  }

  for (const note of result.notes) out.push(`  NOTA · ${note}`);

  return out.join("\n");
}

// ------------------------------------------------------------------ the entrypoint

/**
 * @typedef {object} State
 * @property {string[]} dirty
 * @property {number} blocks
 */

/** @returns {State} */
function loadState() {
  try {
    const raw = JSON.parse(fs.readFileSync(STATE, "utf-8"));
    if (typeof raw !== "object" || raw === null) return { dirty: [], blocks: 0 };
    const parsed = /** @type {Record<string, unknown>} */ (raw);
    const dirty = Array.isArray(parsed.dirty)
      ? parsed.dirty.filter((d) => typeof d === "string")
      : [];
    const blocks = typeof parsed.blocks === "number" ? parsed.blocks : 0;
    return { dirty, blocks };
  } catch {
    return { dirty: [], blocks: 0 };
  }
}

/** @param {State} state */
function saveState(state) {
  fs.mkdirSync(path.dirname(STATE), { recursive: true });
  fs.writeFileSync(STATE, JSON.stringify(state), "utf-8");
}

/** @returns {unknown} */
function readStdin() {
  try {
    return JSON.parse(fs.readFileSync(0, "utf-8"));
  } catch {
    return null;
  }
}

/** @param {string} context */
function hookOutput(context) {
  process.stdout.write(JSON.stringify({
    hookSpecificOutput: {
      hookEventName: "PostToolUse",
      additionalContext: context,
    },
  }));
}

/**
 * @param {string[]} argv
 * @returns {number}
 */
export function main(argv) {
  if (argv.includes("--hook")) {
    const paths = pathsFromHook(readStdin());
    if (!paths.length) return 0;

    const result = verify(paths, { phase: "fast" });
    const state = loadState();
    state.dirty = [...new Set([
      ...state.dirty,
      ...result.boundaries.map((b) => b.boundary),
    ])].sort();
    saveState(state);

    const text = formatForModel(result);
    if (text) hookOutput(text);
    return 0;
  }

  if (argv.includes("--gate")) {
    const payload = readStdin();
    const stopHookActive = typeof payload === "object" && payload !== null
      && Boolean(/** @type {Record<string, unknown>} */ (payload).stop_hook_active);
    const state = loadState();

    if (stopHookActive || !state.dirty.length) return 0;

    if (state.blocks >= MAX_CONSECUTIVE_BLOCKS) {
      saveState({ dirty: state.dirty, blocks: 0 });
      process.stdout.write(JSON.stringify({
        systemMessage: "[harness] portão de conclusão desistiu após "
          + `${MAX_CONSECUTIVE_BLOCKS} bloqueios. Verificação não confirmada.`,
      }));
      return 0;
    }

    const result = verifyBoundaries(state.dirty, { phase: "full" });

    if (failed(result)) {
      saveState({ dirty: state.dirty, blocks: state.blocks + 1 });
      // Exit 2 rather than a JSON decision: three readings of the hooks reference
      // disagreed on the Stop payload shape (top-level `decision`,
      // `hookSpecificOutput.decision`, `hookSpecificOutput.permissionDecision`).
      // All three agreed that exit 2 blocks and stderr reaches the model.
      process.stderr.write(formatForModel(result));
      return 2;
    }

    saveState({ dirty: [], blocks: 0 });
    if (result.notes.length) {
      process.stdout.write(JSON.stringify({
        systemMessage: `[harness] ${result.notes.join("; ")}`,
      }));
    }
    return 0;
  }

  /** @type {Phase} */
  const phase = argv.includes("--phase") && argv.includes("full") ? "full" : "fast";
  const paths = argv.filter((a) => !a.startsWith("--") && a !== phase);
  process.stdout.write(
    `${formatForModel(verify(paths, { phase })) || "[harness] nada a verificar"}\n`,
  );
  return 0;
}

/** Exposed so tests can build temporary directories the same way the runner does. */
export function tmpdir() {
  return fs.mkdtempSync(path.join(os.tmpdir(), "harness-"));
}

if (import.meta.url === `file://${process.argv[1]}`
  || fileURLToPath(import.meta.url) === path.resolve(process.argv[1] || "")) {
  process.exitCode = main(process.argv.slice(2));
}
