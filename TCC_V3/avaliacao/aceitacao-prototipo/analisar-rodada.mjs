// Resume uma rodada de teste a partir dos meta.json.
// Uso (da raiz do TCC_V3): node analisar-rodada.mjs <prefixo>
import { readFileSync, readdirSync, existsSync } from "node:fs";
import { join } from "node:path";

const prefixo = process.argv[2];
const runs = readdirSync("runs").filter((d) => d.startsWith(prefixo + "-")).sort();
const g = (o, p) => p.split(".").reduce((x, k) => (x == null ? x : x[k]), o);

console.log(`rodada ${prefixo}: ${runs.length} execucoes\n`);
const cab = ["run", "termino", "build", "versoes", "turnos", "tools", "in_total", "out", "api_s", "prompt", "harness", "isol"];
console.log(cab.join(" | "));
const linhas = [];
for (const r of runs) {
  const f = join("runs", r, "meta.json");
  if (!existsSync(f)) { console.log(`${r} | SEM meta.json`); continue; }
  const m = JSON.parse(readFileSync(f, "utf8"));
  const pre = (g(m, "environment.preflight") || []).join(" ");
  const isol = /~\/\.claude: \[\]/.test(pre) && /fora do workspace: \[\]/.test(pre) ? "ok" : "VER";
  const l = {
    run: r.replace(prefixo + "-", ""),
    termino: g(m, "outcome.termination"),
    build: g(m, "outcome.build_ok"),
    versoes: g(m, "foundation.versions_obeyed"),
    turnos: g(m, "outcome.turns"),
    tools: g(m, "outcome.tool_calls"),
    in_total: g(m, "tokens.input_total"),
    out: g(m, "tokens.output"),
    api_s: Math.round((g(m, "timing.duration_api_ms") || 0) / 1000),
    prompt: (g(m, "environment.prompt_hash") || "").slice(0, 8),
    harness: (g(m, "environment.harness_hash") || "-").slice(0, 8),
    isol,
    subagentes: g(m, "outcome.subagents_spawned"),
    negacoes: (g(m, "outcome.permission_denials") || []).length,
    modelos: JSON.stringify(g(m, "models_observed.messages")),
  };
  linhas.push(l);
  console.log(cab.map((k) => l[k]).join(" | "));
}
console.log("\nsubagentes / negacoes de permissao / modelos nas mensagens:");
for (const l of linhas) console.log(`  ${l.run}: ${l.subagentes} / ${l.negacoes} / ${l.modelos}`);

console.log("\npares simultaneos (HARNESS vs CONTROL, mesmo modelo):");
for (const mod of ["OPUS", "SONNET", "HAIKU"]) {
  const c = linhas.find((l) => l.run === `${mod}-CONTROL`), h = linhas.find((l) => l.run === `${mod}-HARNESS`);
  if (!c || !h) continue;
  const pct = (a, b) => (b ? `${(((a - b) / b) * 100).toFixed(0)}%` : "-");
  console.log(`  ${mod}: in_total ${pct(h.in_total, c.in_total)}, tempo de API ${pct(h.api_s, c.api_s)}, turnos ${c.turnos}->${h.turnos}`);
}
