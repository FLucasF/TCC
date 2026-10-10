// Junta as metricas de um lote (CK e SonarQube) num CSV, uma linha por execucao.
//
// Uso:
//   node evaluation/tools/aggregate-metrics.mjs <prefixo>
//
// Le evaluation/metrics/<prefixo>/<run_id>/ (o que o metrics.sh gravou) e o
// meta.json de cada execucao, so para identificar modelo, braco e replica.
// Grava evaluation/metrics/<prefixo>/metricas.csv. So junta numeros: nenhuma
// leitura nem nota sai daqui.
//
// COMO LER. Para cada execucao: as colunas sonar_* vem do measures.json (o que o
// servidor do SonarQube devolveu); as colunas ck_* vem do class.csv do CK (uma linha
// por classe), resumidas em contagens, medias e maximos. A coluna "condition" diz
// CONTROL ou HARNESS, e a "nivel" diz N0 a N3 (tirada do fim do run_id).

import { readdirSync, readFileSync, existsSync, writeFileSync } from "node:fs";
import { join, dirname } from "node:path";
import { fileURLToPath } from "node:url";

const RAIZ = join(dirname(fileURLToPath(import.meta.url)), "..", "..");
const prefixo = process.argv[2];
if (!prefixo) { console.error("uso: node evaluation/tools/aggregate-metrics.mjs <prefixo>"); process.exit(1); }
const OUT = join(RAIZ, "evaluation", "metrics", prefixo);
if (!existsSync(OUT)) { console.error(`nao existe ${OUT}: rode antes o metrics.sh`); process.exit(1); }

// CSV do CK: os campos nao trazem virgula, mas o caminho do arquivo pode trazer
// aspas no Windows; este leitor respeita aspas.
function lerCsv(texto) {
  const linhas = texto.replace(/\r/g, "").split("\n").filter(Boolean);
  const campos = (l) => { const r = []; let c = "", q = false;
    for (const ch of l) { if (ch === '"') q = !q; else if (ch === "," && !q) { r.push(c); c = ""; } else c += ch; }
    r.push(c); return r; };
  const cab = campos(linhas[0]);
  return linhas.slice(1).map((l) => Object.fromEntries(campos(l).map((v, i) => [cab[i], v])));
}
const num = (v) => (v === undefined || v === "" || isNaN(Number(v)) ? null : Number(v));
const media = (a) => (a.length ? a.reduce((s, x) => s + x, 0) / a.length : null);
const fmt = (v) => (v === null || v === undefined ? "" : Number.isInteger(v) ? String(v) : v.toFixed(3));

const SONAR = ["ncloc", "classes", "functions", "complexity", "cognitive_complexity",
  "duplicated_lines_density", "duplicated_blocks", "code_smells", "sqale_index"];
const TIPOS = ["class", "interface", "enum", "record"];

const linhas = [];
for (const run of readdirSync(OUT, { withFileTypes: true }).filter((d) => d.isDirectory()).map((d) => d.name).sort()) {
  const dir = join(OUT, run);
  const meta = JSON.parse(readFileSync(join(RAIZ, "runs", run, "meta.json"), "utf8"));
  const status = existsSync(join(dir, "status")) ? readFileSync(join(dir, "status"), "utf8").trim() : "incompleto";
  // O nivel (N0 a N3) e o fim do run_id do V4 (V4-STRATEGY-01-HAIKU45-N2); vazio nos
  // lotes da bancada, que tinham so CONTROL e HARNESS (coluna nivel desde 09/10).
  const nivel = run.match(/-(N\d)$/)?.[1] ?? "";
  const l = { run_id: run, model: meta.model_requested, condition: meta.condition, nivel, replicate: meta.replicate ?? "", status };

  const sonarArq = join(dir, "sonar", "measures.json");
  const medidas = existsSync(sonarArq) ? JSON.parse(readFileSync(sonarArq, "utf8")).component?.measures ?? [] : [];
  for (const k of SONAR) l[`sonar_${k}`] = num(medidas.find((m) => m.metric === k)?.value);

  // O CK conta como "anonymous" o corpo de cada constante de enum com
  // comportamento proprio (e as classes anonimas). Contagens e medias usam so os
  // tipos com nome, o mesmo criterio do sonar_classes; as anonimas vao numa
  // coluna propria, porque medem um desenho (o enum com corpo por constante).
  // A soma de WMC usa tudo: a logica dentro das constantes tambem e codigo.
  const ckArq = join(dir, "ck", "class.csv");
  const todos = existsSync(ckArq) ? lerCsv(readFileSync(ckArq, "utf8")) : [];
  const cls = todos.filter((c) => TIPOS.includes(c.type));
  l.ck_tipos = todos.length ? cls.length : null;
  for (const t of TIPOS) l[`ck_${t}`] = todos.length ? cls.filter((c) => c.type === t).length : null;
  l.ck_anonymous = todos.length ? todos.filter((c) => c.type === "anonymous").length : null;
  const col = (k, lista = cls) => lista.map((c) => num(c[k])).filter((v) => v !== null);
  l.ck_cbo_media = media(col("cbo")); l.ck_cbo_max = col("cbo").length ? Math.max(...col("cbo")) : null;
  l.ck_wmc_soma = col("wmc", todos).length ? col("wmc", todos).reduce((s, x) => s + x, 0) : null;
  l.ck_wmc_max = col("wmc").length ? Math.max(...col("wmc")) : null;
  l.ck_lcom_media = media(col("lcom")); l.ck_rfc_media = media(col("rfc"));
  l.ck_dit_max = col("dit").length ? Math.max(...col("dit")) : null;
  linhas.push(l);
}

const colunas = Object.keys(linhas[0] ?? {});
const celula = (v) => (v === null || v === undefined ? "" : typeof v === "number" ? fmt(v) : String(v));
const csv = [colunas.join(","), ...linhas.map((l) => colunas.map((c) => celula(l[c])).join(","))].join("\n") + "\n";
writeFileSync(join(OUT, "metricas.csv"), csv);
console.log(`${linhas.length} execucoes -> evaluation/metrics/${prefixo}/metricas.csv (${colunas.length} colunas)`);
for (const l of linhas) console.log(`  ${l.run_id.padEnd(34)} ${l.status}`);
