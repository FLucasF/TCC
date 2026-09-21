// Produz as tabelas da §15 a partir do que o agregador e a avaliação deixaram.
//
// Uso:
//   node infra/scripts/analisar.mjs                    # tudo que existir
//   node infra/scripts/analisar.mjs --prefixo LOTE     # só o lote
//   node infra/scripts/analisar.mjs --incluir-invalidas
//
// Lê, e ignora em silêncio o que ainda não existe:
//   analise/resultados.csv      custo, tempo, fundacao   (do agregar.mjs)
//   analise/funcional.csv       % aprovados por grupo    (do conferir-exemplos.sh)
//   avaliacao/consenso.csv      rubrica, ou notas-autor.csv se o consenso não existir
//
// SEM p-valor, de propósito. Com n=3 por braço existem 20 arranjos possíveis
// num Mann-Whitney, então o menor p bicaudal alcançável é 0,10: p<0,05 é
// impossível por construção, e publicar o número só induziria a ler um "não
// significativo" que vem do desenho e não dos dados. A §2.4 do plano já diz que
// o estudo é exploratório e descritivo.
//
// No lugar do p, a medida de efeito é de pares: em quantos pares SEM/COM do
// mesmo modelo e repetição o braço COM supera o SEM.

import { existsSync, readFileSync } from "node:fs";
import { join } from "node:path";

const args = process.argv.slice(2);
const opt = (n, p) => { const i = args.indexOf(`--${n}`); return i >= 0 ? args[i + 1] : p; };
const prefixo = opt("prefixo", null);
const incluirInvalidas = args.includes("--incluir-invalidas");

function csv(caminho) {
  if (!existsSync(caminho)) return null;
  const linhas = readFileSync(caminho, "utf8").trim().split(/\r?\n/);
  if (linhas.length < 2) return [];
  // Parser pequeno, mas que respeita aspas: `starters` e `motivo_proposta`
  // podem conter virgula.
  const campos = (l) => {
    const out = []; let cur = "", aspas = false;
    for (let i = 0; i < l.length; i++) {
      const c = l[i];
      if (aspas) { if (c === '"' && l[i + 1] === '"') { cur += '"'; i++; } else if (c === '"') aspas = false; else cur += c; }
      else if (c === '"') aspas = true;
      else if (c === ",") { out.push(cur); cur = ""; }
      else cur += c;
    }
    out.push(cur);
    return out;
  };
  const cab = campos(linhas[0]);
  return linhas.slice(1).map((l) => Object.fromEntries(campos(l).map((v, i) => [cab[i], v])));
}

const num = (v) => (v === "" || v === undefined || v === null ? null : Number(v));
const apelido = (m) => ({ "claude-opus-5": "Opus 5", "claude-sonnet-5": "Sonnet 5", "claude-haiku-4-5": "Haiku 4.5" }[m] ?? m);
const par = (runId) => runId.replace(/-(COM|SEM)$/, "");

let runs = csv(join("analise", "resultados.csv"));
if (!runs) { console.error("falta analise/resultados.csv — rode antes: node infra/scripts/agregar.mjs"); process.exit(1); }
if (prefixo) runs = runs.filter((r) => r.run_id.startsWith(prefixo));

const semValida = runs.filter((r) => r.valida === "");
const excluidas = runs.filter((r) => r.valida === "false");
if (!incluirInvalidas) runs = runs.filter((r) => r.valida !== "false");

if (!runs.length) { console.error("nenhuma execucao apos os filtros"); process.exit(1); }

const funcional = csv(join("analise", "funcional.csv")) ?? [];
// Prefere o consenso, mas so quando ele TEM nota. O anonimizar.mjs cria os
// quatro CSV vazios de uma vez, entao um `??` escolheria o consenso vazio e
// esconderia as notas do autor. Defeito achado no teste sintetico do
// cruzamento, em 21/09/2026.
const comNota = (linhas) => (linhas ?? []).filter((n) => n.C1 !== "" && n.C1 !== undefined);
const notasPreenchidas = [csv(join("avaliacao", "consenso.csv")), csv(join("avaliacao", "notas-autor.csv"))]
  .map(comNota)
  .find((a) => a.length) ?? [];

// O mapa liga codigo cego -> run, e so entao da para cruzar as notas com
// modelo e condicao. Ele esta no .gitignore de proposito: o autor e um dos
// dois avaliadores, e versiona-lo antes das notas fecharem seria deixar o
// gabarito da cegueira aberto. Sem ele, as tabelas por celula nao saem, e o
// script diz por que em vez de quebrar.
const mapa = csv(join("avaliacao", "mapa-anonimizacao.csv"));
const runPorCodigo = new Map((mapa ?? []).map((m) => [m.codigo_cego, m.run_id]));
const dadosPorRun = new Map(runs.map((r) => [r.run_id, r]));

const modelos = [...new Set(runs.map((r) => r.modelo))];
const celulas = [];
for (const m of modelos) for (const c of ["SEM", "COM"]) celulas.push({ modelo: m, condicao: c, runs: runs.filter((r) => r.modelo === m && r.condicao === c) });

const linha = (v) => `| ${v.join(" | ")} |`;
const mediana = (a) => { if (!a.length) return null; const s = [...a].sort((x, y) => x - y); const i = Math.floor(s.length / 2); return s.length % 2 ? s[i] : (s[i - 1] + s[i]) / 2; };
const faixa = (a) => (a.length ? `${Math.min(...a)}–${Math.max(...a)}` : "");
const fmt = (v, casas = 0) => (v === null || Number.isNaN(v) ? "" : v.toLocaleString("pt-BR", { minimumFractionDigits: casas, maximumFractionDigits: casas }));

console.log(`# Análise${prefixo ? ` — ${prefixo}` : ""}\n`);
console.log(`${runs.length} execuções${excluidas.length ? `, ${excluidas.length} excluída(s) por \`valida: false\`` : ""}.`);
if (semValida.length) {
  console.log(`\n> [!warning] ${semValida.length} execução(ões) com \`valida\` vazia`);
  console.log("> Entraram na contagem, mas não passaram pelo julgamento humano da §13.3.");
  console.log("> A proposta do extrator está em `valida_proposta`.");
}

// ---------------------------------------------------------------- 15.2 custo
console.log(`\n## 15.2 · Custo\n`);
console.log(linha(["Modelo", "Condição", "n", "Entrada total (mediana)", "Saída", "Raciocínio", "Duração API (s)", "Turnos"]));
console.log(linha(Array(8).fill("---")));
for (const c of celulas) {
  if (!c.runs.length) continue;
  const col = (campo) => c.runs.map((r) => num(r[campo])).filter((v) => v !== null);
  console.log(linha([
    apelido(c.modelo), c.condicao, c.runs.length,
    fmt(mediana(col("entrada_total"))), fmt(mediana(col("saida"))), fmt(mediana(col("raciocinio"))),
    fmt(mediana(col("duracao_api_ms")) / 1000, 0), `${fmt(mediana(col("turnos")))} (${faixa(col("turnos"))})`,
  ]));
}
console.log("\nMediana, não média: com n pequeno uma execução fora da curva desloca a média inteira.");
console.log("`duracao_api_ms` e não `duracao_s`: as execuções rodam em paralelo e disputam CPU.");

// ------------------------------------------------- medida de efeito, por par
console.log(`\n## Medida de efeito: pares SEM/COM\n`);
console.log("Em quantos pares do mesmo modelo e repetição o braço `COM` supera o `SEM`.");
console.log("Sem p-valor: com n=3 por braço, o menor p bicaudal alcançável num Mann-Whitney");
console.log("é 0,10, então p<0,05 é impossível por construção. Ver §2.4 do plano.\n");

const pares = new Map();
for (const r of runs) {
  const k = par(r.run_id);
  if (!pares.has(k)) pares.set(k, {});
  pares.get(k)[r.condicao] = r;
}
const completos = [...pares.entries()].filter(([, p]) => p.COM && p.SEM);

const METRICAS = [
  ["entrada_total", "tokens de entrada", "menor é melhor"],
  ["saida", "tokens de saída", "menor é melhor"],
  ["duracao_api_ms", "duração da API", "menor é melhor"],
  ["turnos", "turnos", "menor é melhor"],
];
console.log(linha(["Métrica", "COM > SEM", "COM < SEM", "empate", "pares"]));
console.log(linha(Array(5).fill("---")));
for (const [campo, nome] of METRICAS) {
  let maior = 0, menor = 0, igual = 0;
  for (const [, p] of completos) {
    const a = num(p.COM[campo]), b = num(p.SEM[campo]);
    if (a === null || b === null) continue;
    if (a > b) maior++; else if (a < b) menor++; else igual++;
  }
  console.log(linha([nome, maior, menor, igual, maior + menor + igual]));
}
if (!completos.length) console.log("\n(nenhum par SEM/COM completo)");

// ---------------------------------------------------------- P7 e uso de web
console.log(`\n## Controles\n`);
console.log(linha(["Modelo", "Condição", "Obedeceu versões", "Build ok", "Chamadas de web (mediana)"]));
console.log(linha(Array(5).fill("---")));
for (const c of celulas) {
  if (!c.runs.length) continue;
  const aplic = c.runs.filter((r) => r.obedeceu_versoes !== "");
  const obed = aplic.filter((r) => r.obedeceu_versoes === "true").length;
  console.log(linha([
    apelido(c.modelo), c.condicao,
    aplic.length ? `${obed}/${aplic.length}` : "n/a",
    `${c.runs.filter((r) => r.build_ok === "true").length}/${c.runs.length}`,
    fmt(mediana(c.runs.map((r) => num(r.chamadas_web)).filter((v) => v !== null))),
  ]));
}

// ------------------------------------------------------------ 15.1c funcional
if (funcional.length) {
  const porRun = new Map();
  for (const f of funcional) {
    if (!porRun.has(f.run_id)) porRun.set(f.run_id, { total: 0, falhas: 0, grupos: {} });
    const p = porRun.get(f.run_id);
    p.total += Number(f.total); p.falhas += Number(f.falhas);
    p.grupos[f.grupo] = `${Number(f.total) - Number(f.falhas)}/${f.total}`;
  }
  const grupos = [...new Set(funcional.map((f) => f.grupo))].sort();
  console.log(`\n## 15.1c · Testes funcionais escondidos\n`);
  console.log(linha(["Execução", "Modelo", "Cond.", "Total", ...grupos]));
  console.log(linha(Array(4 + grupos.length).fill("---")));
  for (const r of runs) {
    const p = porRun.get(r.run_id);
    if (!p) continue;
    console.log(linha([r.run_id, apelido(r.modelo), r.condicao,
      `**${p.total - p.falhas}/${p.total}**`, ...grupos.map((g) => p.grupos[g] ?? "")]));
  }
} else {
  console.log(`\n## 15.1c · Testes funcionais escondidos\n`);
  console.log("Sem dados. Rode:");
  console.log("```bash\nCASOS=avaliacao/casos avaliacao/ferramentas/conferir-exemplos.sh <run_id> ...\n```");
}

// ------------------------------------------------------------- 15.1 rubrica
console.log(`\n## 15.1 · Strategy correto por ponto\n`);
if (!notasPreenchidas.length) {
  console.log("Sem notas preenchidas. O desfecho primário sai de `avaliacao/consenso.csv`,");
  console.log("ou de `notas-autor.csv` enquanto o consenso não existir. Ver `avaliacao/README.md`.");
} else if (!mapa) {
  console.log("Notas preenchidas, mas **sem o mapa de anonimização** — então não dá para");
  console.log("cruzar com modelo e condição. Isso é esperado: o mapa fica fora do repositório");
  console.log("até as notas fecharem. Traga `avaliacao/mapa-anonimizacao.csv` de volta depois");
  console.log("do commit de `notas-autor.csv` e rode outra vez.\n");
  const porPonto = {};
  for (const n of notasPreenchidas) {
    porPonto[n.ponto] ??= { correto: 0, parcial: 0, sem: 0, totais: [] };
    porPonto[n.ponto][n.classe] = (porPonto[n.ponto][n.classe] ?? 0) + 1;
    if (n.total !== "") porPonto[n.ponto].totais.push(Number(n.total));
  }
  console.log(linha(["Ponto", "correto", "parcial", "sem", "mediana 0–12", "n"]));
  console.log(linha(Array(6).fill("---")));
  for (const p of ["P1", "P2", "P3"]) {
    const d = porPonto[p];
    if (d) console.log(linha([p, d.correto ?? 0, d.parcial ?? 0, d.sem ?? 0, fmt(mediana(d.totais), 1), d.totais.length]));
  }
} else {
  // Junta nota (por codigo cego) com execucao (por run), pelo mapa.
  const cruzadas = [];
  const orfas = new Set();
  for (const n of notasPreenchidas) {
    const runId = runPorCodigo.get(n.codigo_cego);
    const dados = runId ? dadosPorRun.get(runId) : null;
    if (!dados) { orfas.add(n.codigo_cego); continue; }
    cruzadas.push({ ...n, run_id: runId, modelo: dados.modelo, condicao: dados.condicao });
  }
  if (orfas.size) console.log(`> ${orfas.size} código(s) cego(s) sem execução correspondente: ${[...orfas].join(", ")}\n`);

  const nota = (m, c, p) => cruzadas.filter((x) => x.modelo === m && x.condicao === c && x.ponto === p);
  const totais = (a) => a.map((n) => Number(n.total)).filter((v) => !Number.isNaN(v));

  console.log(linha(["Modelo", "Condição", "P1 Entrega", "P2 Cupons", "P3 Pagamento", "Pontos corretos (média 0–3)"]));
  console.log(linha(Array(6).fill("---")));
  for (const c of celulas) {
    const porP = ["P1", "P2", "P3"].map((p) => nota(c.modelo, c.condicao, p));
    if (!porP.some((x) => x.length)) continue;
    const corretos = porP.map((x) => `${x.filter((n) => n.classe === "correto").length}/${x.length}`);
    // Media por EXECUCAO: quantos dos tres pontos ficaram corretos em cada uma.
    const porExec = {};
    for (const x of porP.flat()) porExec[x.run_id] = (porExec[x.run_id] ?? 0) + (x.classe === "correto" ? 1 : 0);
    const v = Object.values(porExec);
    console.log(linha([apelido(c.modelo), c.condicao, ...corretos, v.length ? fmt(v.reduce((s, x) => s + x, 0) / v.length, 2) : ""]));
  }

  console.log(`\n### 15.1b · Rubrica por ponto (mediana e faixa, 0–12)\n`);
  console.log(linha(["Modelo", "Condição", "P1", "P2", "P3"]));
  console.log(linha(Array(5).fill("---")));
  for (const c of celulas) {
    const cols = ["P1", "P2", "P3"].map((p) => {
      const t = totais(nota(c.modelo, c.condicao, p));
      return t.length ? `${fmt(mediana(t), 1)} (${faixa(t)})` : "";
    });
    if (cols.every((x) => !x)) continue;
    console.log(linha([apelido(c.modelo), c.condicao, ...cols]));
  }

  console.log(`\n### 15.1d · Efeito do harness por dificuldade (COM − SEM, mediana 0–12)\n`);
  console.log(linha(["Modelo", "Δ P1", "Δ P2", "Δ P3"]));
  console.log(linha(Array(4).fill("---")));
  for (const m of modelos) {
    const d = ["P1", "P2", "P3"].map((p) => {
      const com = totais(nota(m, "COM", p)), sem = totais(nota(m, "SEM", p));
      if (!com.length || !sem.length) return "";
      const delta = mediana(com) - mediana(sem);
      return (delta > 0 ? "+" : "") + fmt(delta, 1);
    });
    if (d.every((x) => !x)) continue;
    console.log(linha([apelido(m), ...d]));
  }
  console.log("\nDiferença de medianas, não teste. A medida de efeito está na seção de pares.");
}

console.log(`\n---\n`);
console.log("Gerado por `infra/scripts/analisar.mjs`.");
