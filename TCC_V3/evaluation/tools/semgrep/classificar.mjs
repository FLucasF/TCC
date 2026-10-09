// Le o JSON do Semgrep (uma subpasta = um pacote) e responde as perguntas da
// regua enxuta: localizacao e selecao nos pontos positivos (P1 a P4), forma e
// proporcao no controle negativo (P5).
//
// Uso:  node evaluation/tools/semgrep/classificar.mjs <saida-do-semgrep.json> <raiz-dos-pacotes> [raiz-no-json]
// Saida: CSV no stdout, uma linha por pacote (subpasta direta da raiz).
// [raiz-no-json] e como a raiz aparece nos caminhos do JSON, quando o Semgrep
// rodou num container (ex.: /pacotes); por padrao, a propria raiz.

import { readFileSync, readdirSync } from "node:fs";
import { PONTOS } from "./pontos.mjs";

const [jsonPath, raiz, raizNoJson = raiz] = process.argv.slice(2);
if (!jsonPath || !raiz) {
  console.error("uso: node classificar.mjs <saida.json> <raiz-dos-pacotes> [raiz-no-json]");
  process.exit(2);
}
const out = JSON.parse(readFileSync(jsonPath, "utf8"));
if (out.errors?.length) {
  console.error(`o Semgrep relatou ${out.errors.length} erro(s); confira antes de usar o resultado`);
  process.exit(1);
}

// O caminho de cada achado comeca pela pasta do pacote, relativa a raiz.
const prefixo = raizNoJson.replace(/\\/g, "/").replace(/\/?$/, "/");
const pacoteDe = (p) => p.replace(/\\/g, "/").replace(prefixo, "").split("/")[0];

const achados = {};
const textos = {}; // "pacote|arquivo:linha" -> texto da linha, para achar listas de validos
for (const r of out.results) {
  const pac = pacoteDe(r.path);
  const regra = r.check_id.split(".").pop();
  const onde = `${r.path.replace(/^.*\/java\//, "")}:${r.start.line}`;
  ((achados[pac] ??= {})[regra] ??= []).push(onde);
  textos[`${pac}|${onde}`] = r.extra?.lines ?? "";
}

const lista = (h, id) => h[id] ?? [];
const conta = (h, id) => lista(h, id).length;

// Regua 2.2: o nome "numa lista de validacao" nao conta. Uma lista de validos e
// um bloco de comparacoes em linhas seguidas, ligadas por && ou ||, sem else,
// que cita TODOS os casos do ponto (ex.: !c.equals("BEMVINDO10") && !c.equals(...)).
function semListasDeValidos(pac, achadosDoPonto, casos) {
  const porArquivo = {};
  for (const onde of achadosDoPonto) {
    const [arquivo, linha] = [onde.slice(0, onde.lastIndexOf(":")), Number(onde.slice(onde.lastIndexOf(":") + 1))];
    (porArquivo[arquivo] ??= []).push({ onde, linha });
  }
  const fora = new Set();
  for (const itens of Object.values(porArquivo)) {
    itens.sort((a, b) => a.linha - b.linha);
    let bloco = [];
    const fecha = () => {
      const texto = [...new Set(bloco.map((i) => textos[`${pac}|${i.onde}`]))].join("\n");
      const citaTodos = casos.every((c) => new RegExp(`\\b${c}\\b`).test(texto));
      if (citaTodos && /&&|\|\|/.test(texto) && !/\belse\b|\?|->/.test(texto)) bloco.forEach((i) => fora.add(i.onde));
      bloco = [];
    };
    for (const i of itens) {
      if (bloco.length && i.linha - bloco[bloco.length - 1].linha > 1) fecha();
      bloco.push(i);
    }
    fecha();
  }
  return achadosDoPonto.filter((o) => !fora.has(o));
}

function positivo(pac, h, { id: p, casos }) {
  const fabrica = new Set(lista(h, `${p}-switch-fabrica`));
  const rotulosQueCalculam = lista(h, `${p}-switch-label`).filter((l) => !fabrica.has(l));
  const condCalc = semListasDeValidos(pac, lista(h, `${p}-cond-calc`), casos);
  const condValidacao = semListasDeValidos(pac, lista(h, `${p}-cond-validacao`), casos);
  // A evidencia mostra o que foi CONTADO: sem as listas de validos, e sem os rotulos
  // de fabrica entre os rotulos que calculam.
  h[`${p}-cond-calc`] = condCalc;
  h[`${p}-cond-validacao`] = condValidacao;
  h[`${p}-switch-label`] = rotulosQueCalculam;
  const calculo = condCalc.length + conta(h, `${p}-instanceof`) + rotulosQueCalculam.length;
  const validacao = condValidacao.length;
  // Alarme: nenhum nome de caso no pacote inteiro quer dizer que o agente chamou os
  // casos de outro jeito (ex.: GOLD); as regras nao os veriam, e "isolado" seria falso.
  if (conta(h, `${p}-nome`) === 0) return ["indeterminado", "indeterminado"];
  const localizacao = calculo + validacao > 0 ? "espalhado" : "isolado";
  const selecao = calculo > 0 ? "condicional-no-calculo" : fabrica.size > 0 ? "condicional-unica" : "consulta";
  return [localizacao, selecao];
}

function negativo(h) {
  if (conta(h, "p5-nome") === 0) return ["outro", "indeterminado"]; // o mesmo alarme
  const estrutura = conta(h, "p5-enum-corpo") + conta(h, "p5-classe-regiao")
    + conta(h, "p5-switch-fabrica") + conta(h, "p5-mapa-objeto");
  const condicional = conta(h, "p5-switch-label") - conta(h, "p5-switch-fabrica");
  const forma = conta(h, "p5-enum-corpo") ? "enum-abstrato"
    : conta(h, "p5-classe-regiao") ? "classes"
    : conta(h, "p5-mapa") + conta(h, "p5-mapa-objeto") ? "mapa"
    : condicional + conta(h, "p5-switch-fabrica") > 0 ? "switch"
    : conta(h, "p5-enum-dados") ? "enum-dados"
    : "outro";
  const proporcao = estrutura > 0 ? "estrutura"
    : condicional > 0 ? "condicional"
    : conta(h, "p5-enum-dados") + conta(h, "p5-mapa") ? "dados"
    : "indeterminado";
  return [forma, proporcao];
}

const pacotes = readdirSync(raiz, { withFileTypes: true }).filter((d) => d.isDirectory()).map((d) => d.name).sort();
console.log([
  "pacote",
  "P1_localizacao", "P1_selecao", "P2_localizacao", "P2_selecao",
  "P3_localizacao", "P3_selecao", "P4_localizacao", "P4_selecao",
  "P5_forma", "P5_proporcao", "evidencia",
].join(","));
for (const pac of pacotes) {
  const h = achados[pac] ?? {};
  const respostas = [...PONTOS.flatMap((ponto) => positivo(pac, h, ponto)), ...negativo(h)];
  // Depois das respostas: positivo() deixa em h so os achados que contaram.
  const evidencia = Object.entries(h).filter(([id, ls]) => ls.length && !id.endsWith("-nome"))
    .map(([id, ls]) => `${id}=${ls.length}(${ls[0]})`).join(" ");
  console.log([pac, ...respostas, `"${evidencia}"`].join(","));
}
