// A nota de 0 a 100 de cada execucao, como RESUMO (decisao de 09/10). As hipoteses
// sao lidas nas medidas separadas; a nota so as junta para o leitor.
//
// Uso:
//   node evaluation/tools/nota.mjs <acceptance.csv> <semgrep.csv> [mapa.csv]
//
// <acceptance.csv> e o CSV do lote que o infra/scripts/acceptance.sh gera (com as
// colunas contas e recusas, da suite de 09/10). <semgrep.csv> e a saida do
// evaluation/tools/semgrep/detect.sh. Se o Semgrep rodou nos pacotes cegos, passe o
// mapa de anonimizacao para ligar cada codigo a sua execucao.
//
// Pesos A (a nota principal): contas 30 (12 casos), recusas 20 (9 casos, a recusa com o
// codigo certo e status de sucesso vale 0,5, ja contada pela suite), padrao 50 (P1 a P4,
// 10 por ponto: 10 com a localizacao e a selecao certas, 5 com uma, 0 com nenhuma; P5
// sem exagero, 10). Variantes publicadas ao lado: B (40, 20, 40) e C (35, 25, 40).
// Trava: nao compilou ou nao subiu (no_pom, build_failed, app_did_not_start), nota 0.
// "indeterminado" no Semgrep conta como errado, e a linha sai marcada.

import { readFileSync } from "node:fs";

const PESOS = { A: [30, 20, 50], B: [40, 20, 40], C: [35, 25, 40] };
const CONTAS = 12, RECUSAS = 9, PADRAO = 50;
const TRAVA = new Set(["no_pom", "build_failed", "app_did_not_start"]);

const [arqAcc, arqSem, arqMapa] = process.argv.slice(2);
if (!arqAcc || !arqSem) {
  console.error("uso: node nota.mjs <acceptance.csv> <semgrep.csv> [mapa.csv]");
  process.exit(2);
}

// CSV simples: as colunas usadas aqui nao tem virgula; a evidencia (entre aspas) e
// cortada antes de separar.
function ler(arquivo) {
  const linhas = readFileSync(arquivo, "utf8").trim().split(/\r?\n/).filter((l) => !l.startsWith("#"));
  const cab = linhas[0].split(",");
  return linhas.slice(1).map((l) => {
    const v = l.replace(/,"[^"]*"$/, "").split(",");
    return Object.fromEntries(cab.map((c, i) => [c, v[i] ?? ""]));
  });
}

const acc = ler(arqAcc);
const sem = new Map(ler(arqSem).map((r) => [r.pacote, r]));
const execucaoDe = new Map(arqMapa ? ler(arqMapa).map((r) => [r.run_id, r.blind_code]) : []);

function padrao(s) {
  const pontos = [], marcas = [];
  for (const p of ["P1", "P2", "P3", "P4"]) {
    const loc = s[`${p}_localizacao`], sel = s[`${p}_selecao`];
    if (loc === "indeterminado" || sel === "indeterminado") marcas.push(`${p} indeterminado`);
    const certos = (loc === "isolado") + (sel === "consulta" || sel === "condicional-unica");
    pontos.push(certos === 2 ? 10 : certos === 1 ? 5 : 0);
  }
  if (s.P5_proporcao === "indeterminado") marcas.push("P5 indeterminado");
  pontos.push(s.P5_proporcao === "dados" || s.P5_proporcao === "condicional" ? 10 : 0);
  return { pontos, total: pontos.reduce((a, b) => a + b, 0), marcas };
}

const fmt = (x) => x.toFixed(1);
console.log("run_id,status,contas,recusas,P1,P2,P3,P4,P5,nota_A,nota_B,nota_C,marcas");
let erros = 0;
for (const r of acc) {
  const chave = execucaoDe.get(r.run_id) ?? r.run_id;
  const s = sem.get(chave);
  if (!s) { console.error(`ERRO: ${r.run_id} sem linha no Semgrep (chave ${chave})`); erros++; continue; }
  const pd = padrao(s);
  const travado = TRAVA.has(r.status);
  if (!travado && (r.contas === "" || r.recusas === "")) {
    console.error(`ERRO: ${r.run_id} sem contas/recusas: medido com a suite anterior a 09/10`);
    erros++; continue;
  }
  const contas = travado ? 0 : Number(r.contas), recusas = travado ? 0 : Number(r.recusas);
  const notas = Object.values(PESOS).map(([wc, wr, wp]) =>
    travado ? 0 : contas / CONTAS * wc + recusas / RECUSAS * wr + pd.total / PADRAO * wp);
  const marcas = [...(travado ? [`trava: ${r.status}`] : []), ...pd.marcas].join("; ");
  console.log([r.run_id, r.status, contas, recusas, ...pd.pontos, ...notas.map(fmt), marcas].join(","));
}
process.exit(erros ? 1 : 0);
