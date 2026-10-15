// A prova de que o hipoteses.mjs aplica as regras do secao 4.1 do OBJETIVO como estao
// escritas (09/10). Monta um lote de mentira (5 modelos x 5 replicas x 4 niveis) com os
// resultados PLANEJADOS para cada hipotese dar um veredito conhecido, e depois muda uma
// coisa por caso: cada caso tem de dar o veredito que a regra manda.
//
// Uso:  node evaluation/tools/hipoteses-teste.mjs     (sai com 1 se algum caso falhar)

import { mkdtempSync, mkdirSync, writeFileSync, readFileSync, rmSync } from "node:fs";
import { join } from "node:path";
import { tmpdir } from "node:os";
import { spawnSync } from "node:child_process";
import { fileURLToPath } from "node:url";

const SCRIPT = join(fileURLToPath(new URL(".", import.meta.url)), "hipoteses.mjs");
const MOD = ["A", "B", "C", "D", "E"], NIV = ["N0", "N1", "N2", "N3"], R = 5;
const id = (r, m, n) => `T-0${r}-${m}-${n}`;

// O cenario base. Cada medida e uma funcao (replica, modelo, nivel) -> valor.
//   P4: A e B erram tudo no N0; no N1, A acerta as 5 e B acerta 3. C, D e E no teto.
//   P5: exagero so no N1, em 3 replicas de A e 2 de B (5 pares, todos subindo).
//   suite: 21 em tudo. Complexidade: cai no N1 para A, B e C; igual em D e E.
//   tokens: sobem no N1 em 18 pares; tempo: 17; turnos: 15.
const base = () => ({
  p4: (r, m, n) => (["C", "D", "E"].includes(m) ? 1 : n === "N0" ? 0 : m === "A" ? 1 : r <= 3 ? 1 : 0),
  p5: (r, m, n) => (n === "N1" && ((m === "A" && r <= 3) || (m === "B" && r <= 2)) ? 1 : 0),
  pontos: () => 21,
  cognitiva: (r, m, n) => (n !== "N0" && ["A", "B", "C"].includes(m) ? 30 : 40),
  sobe: { tokens: 18, tempo: 17, turnos: 15 },
  conferencia: null, exploratorio: false, indeterminado: null,
});
// Quantos pares (contando pela ordem modelo, replica) sobem no N1; o resto desce.
const subindo = (qtos, r, m) => MOD.indexOf(m) * R + (r - 1) < qtos;

function montar(raiz, c) {
  const sem = ["pacote,P1_localizacao,P1_selecao,P2_localizacao,P2_selecao,P3_localizacao,P3_selecao,P4_localizacao,P4_selecao,P5_forma,P5_proporcao,evidencia,avisos"];
  const acc = ["run_id,status,passed,total,suite_hash,reference_hash,prompt_hash,prompt_check,contas,recusas,pontos"];
  const met = ["run_id,model,condition,nivel,replicate,status,sonar_classes,sonar_cognitive_complexity,sonar_duplicated_lines_density,sonar_code_smells,ck_cbo_media,ck_lcom_media"];
  const res = ["run_id,input_total,duration_api_ms,turns"];
  for (let r = 1; r <= R; r++) for (const m of MOD) for (const n of NIV) {
    const i = id(r, m, n);
    const p4 = c.p4(r, m, n) ? "isolado,consulta" : "espalhado,condicional-no-calculo";
    const p4final = c.indeterminado === i ? "indeterminado,indeterminado" : p4;
    sem.push(`${i},isolado,consulta,isolado,consulta,isolado,consulta,${p4final},enum-dados,${c.p5(r, m, n) ? "estrutura" : "dados"},"",""`);
    const pts = c.pontos(r, m, n);
    acc.push(`${i},${pts === 21 ? "all_passed" : "some_failed"},${pts},21,x,x,x,confere,${Math.min(12, pts)},${Math.max(0, pts - 12)},${pts}`);
    met.push(`${i},modelo-${m},${n === "N0" ? "CONTROL" : "HARNESS"},${n},${r},ok,10,${c.cognitiva(r, m, n)},0,5,1.5,2.0`);
    const sob = (k) => (n === "N0" ? 100 : subindo(c.sobe[k], r, m) ? 200 : 50);
    res.push(`${i},${sob("tokens")},${sob("tempo")},${sob("turnos")}`);
    mkdirSync(join(raiz, "runs", i), { recursive: true });
    writeFileSync(join(raiz, "runs", i, "acceptance.txt"), "status: ok\n---\n");
  }
  writeFileSync(join(raiz, "semgrep.csv"), sem.join("\n") + "\n");
  writeFileSync(join(raiz, "aceitacao.csv"), acc.join("\n") + "\n");
  writeFileSync(join(raiz, "metricas.csv"), met.join("\n") + "\n");
  writeFileSync(join(raiz, "resultados.csv"), res.join("\n") + "\n");
  writeFileSync(join(raiz, "desenho.json"), JSON.stringify({ prefixo: "T", replicas: R,
    niveis: { N0: {}, N1: {}, N2: {}, N3: {} }, modelos: Object.fromEntries(MOD.map((m) => [m, `modelo-${m}`])) }));
  if (c.conferencia) writeFileSync(join(raiz, "conferencia.md"), c.conferencia);
}

function rodar(c) {
  const raiz = mkdtempSync(join(tmpdir(), "hip-"));
  try {
    montar(raiz, c);
    const a = ["desenho.json", "--semgrep", "semgrep.csv", "--aceitacao", "aceitacao.csv", "--metricas", "metricas.csv",
      "--resultados", "resultados.csv", "--json", "saida.json", "--raiz", raiz];
    if (c.conferencia) a.push("--conferencia", "conferencia.md");
    if (c.exploratorio) a.push("--exploratorio");
    const p = spawnSync(process.execPath, [SCRIPT, ...a], { encoding: "utf8" });
    if (p.status !== 0) return { erro: p.stderr };
    const j = JSON.parse(readFileSync(join(raiz, "saida.json"), "utf8"));
    return { v: Object.fromEntries(j.resumo.map((x) => [x.nome, x.veredito])), md: p.stdout };
  } finally { rmSync(raiz, { recursive: true, force: true }); }
}

// [descricao, mudanca no cenario, { hipotese: veredito esperado }]
const CASOS = [
  ["o cenario base", () => {}, {
    "Desenho: isola cada caso no P4": "apoiada",
    "Exagero: aplica onde nao pede": "apoiada (altera)",
    "Correcao: suite inteira": "apoiada",
    "Qualidade: menos complexidade": "apoiada",
    "Modelo: efeito maior no mais fraco": "apoiada",
    "Custo: tokens de entrada": "apoiada (altera)",
    "Custo: tempo de API": "inconclusiva",
    "Custo: processo de trabalho (turnos)": "contrariada",
    "Modelo: no teto, nao piora (desenho)": "apoiada",
  }],
  ["um modelo no teto piora em 1 par: o desenho nao pode ser apoiado", (c) => { const f = c.p4; c.p4 = (r, m, n) => (m === "C" && n === "N1" && r === 1 ? 0 : f(r, m, n)); },
    { "Desenho: isola cada caso no P4": "inconclusiva" }],
  ["A e B (4 de 5 no N0, fora do teto) erram tudo no N1: contrariada", (c) => { c.p4 = (r, m, n) => (["C", "D", "E"].includes(m) ? 1 : n === "N0" ? (r <= 4 ? 1 : 0) : 0); },
    { "Desenho: isola cada caso no P4": "contrariada" }],
  ["todos no teto: sem espaco para efeito", (c) => { c.p4 = () => 1; },
    { "Desenho: isola cada caso no P4": "sem espaco para efeito (todos no teto)" }],
  ["a suite cai em 3 pares: saldo 3, ainda nao-inferior", (c) => { c.pontos = (r, m, n) => (n === "N1" && m === "A" && r <= 3 ? 20 : 21); },
    { "Correcao: suite inteira": "apoiada" }],
  ["a suite cai em 4 pares: saldo 4, contrariada", (c) => { c.pontos = (r, m, n) => (n === "N1" && m === "A" && r <= 4 ? 20 : 21); },
    { "Correcao: suite inteira": "contrariada" }],
  ["exagero em so 2 pares: contrariada", (c) => { c.p5 = (r, m, n) => (n === "N1" && m === "A" && r <= 2 ? 1 : 0); },
    { "Exagero: aplica onde nao pede": "contrariada" }],
  ["exagero dividido (4 sobem, 2 descem): contrariada", (c) => { c.p5 = (r, m, n) => (m === "A" && ((n === "N1" && r <= 4) || (n === "N0" && r === 5)) ? 1 : (m === "B" && n === "N0" && r === 1 ? 1 : 0)); },
    { "Exagero: aplica onde nao pede": "contrariada" }],
  ["exagero 6 a 1: inconclusiva (precisa de 6 de 7)", (c) => { c.p5 = (r, m, n) => ((m === "A" && n === "N1") || (m === "B" && n === "N1" && r === 1) || (m === "C" && n === "N0" && r === 1) ? 1 : 0); },
    { "Exagero: aplica onde nao pede": "inconclusiva" }],
  ["tokens com 17: inconclusiva", (c) => { c.sobe.tokens = 17; }, { "Custo: tokens de entrada": "inconclusiva" }],
  ["tokens: 10 sobem e 15 descem (lado maior 15): contrariada", (c) => { c.sobe.tokens = 10; }, { "Custo: tokens de entrada": "contrariada" }],
  ["o mais forte fora do teto tem o maior saldo: modelo contrariada", (c) => {
    c.p4 = (r, m, n) => (["D", "E"].includes(m) ? 1 : m === "C" ? (n === "N0" && r === 1 ? 0 : 1) : 0); },
    { "Modelo: efeito maior no mais fraco": "contrariada" }],
  ["dois fracos empatados no maior saldo: modelo inconclusiva", (c) => {
    c.p4 = (r, m, n) => (["C", "D", "E"].includes(m) ? 1 : n === "N0" ? 0 : r <= 3 ? 1 : 0); },
    { "Modelo: efeito maior no mais fraco": "inconclusiva (empate no maior saldo)" }],
  ["a conferencia reprova o P4: desenho e modelo descritivos", (c) => { c.conferencia = "| P4_localizacao | 16 de 20 | vira **descritiva** |\n"; },
    { "Desenho: isola cada caso no P4": "descritiva (a conferencia reprovou P4_localizacao)", "Modelo: efeito maior no mais fraco": "descritiva (a conferencia reprovou P4_localizacao)", "Correcao: suite inteira": "apoiada" }],
  ["exploratorio: nenhum veredito", (c) => { c.exploratorio = true; },
    { "Desenho: isola cada caso no P4": "exploratorio (sem veredito)", "Correcao: suite inteira": "exploratorio (sem veredito)" }],
  ["nenhum par com dado (a suite sem pontos): sem dado, nunca 'apoiada'", (c) => { c.pontos = () => ""; },
    { "Correcao: suite inteira": "sem dado", "Processo: corrige mais": "sem dado" }],
  ["um indeterminado no P4: o par sai da conta, o veredito nao muda", (c) => { c.indeterminado = id(1, "A", "N1"); },
    { "Desenho: isola cada caso no P4": "apoiada" }],
];

let falhas = 0;
for (const [desc, f, esperado] of CASOS) {
  const c = base(); f(c);
  const r = rodar(c);
  const erros = r.erro ? [`o script falhou: ${r.erro}`] : Object.entries(esperado).filter(([h, v]) => r.v[h] !== v).map(([h, v]) => `${h}: esperado "${v}", veio "${r.v[h]}"`);
  if (!r.erro && desc.startsWith("um indeterminado") && !/\| A \| 4 \| 0 \| 0 \| 1 \|/.test(r.md)) erros.push("o par com indeterminado nao aparece como 'sem dado' na tabela do A");
  if (erros.length) falhas++;
  console.log(`${erros.length ? "FALHA" : "ok   "} ${desc}${erros.length ? "\n        " + erros.join("\n        ") : ""}`);
}
console.log(`\n${CASOS.length - falhas} de ${CASOS.length} casos como a regra manda.`);
process.exit(falhas ? 1 : 0);
