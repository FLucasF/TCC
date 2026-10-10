// Le o JSON do Semgrep (uma subpasta = um pacote) e responde as perguntas da
// regua enxuta: localizacao e selecao nos pontos positivos (P1 a P4), forma e
// proporcao no controle negativo (P5).
//
// Uso:  node evaluation/tools/semgrep/classificar.mjs <saida-do-semgrep.json> <raiz-dos-pacotes> [raiz-no-json]
// Saida: CSV no stdout, uma linha por pacote (subpasta direta da raiz).
// [raiz-no-json] e como a raiz aparece nos caminhos do JSON, quando o Semgrep
// rodou num container (ex.: /pacotes); por padrao, a propria raiz.
//
// Versao 2 (09/10, revisao com o corpus adversarial, ver README). Erro do Semgrep num
// arquivo nao para mais o lote: vai para a coluna "avisos" do pacote. Um trecho que ele
// nao conseguiu ler (PartialParsing, ex.: um recurso novo do Java) deixa as respostas e
// avisa; qualquer outro erro deixa o pacote todo "indeterminado".
//
// COMO LER. O Semgrep devolve uma lista de LINHAS marcadas, cada uma com o nome da
// regra (ex.: "p4-cond-calc" na linha 58 do CheckoutService.java). Este script agrupa
// as linhas por pacote e por regra, e decide, ponto a ponto:
//
//   P1 a P4 (positivo()): primeiro TIRA o que a regua manda nao contar: as condicoes de
//   recusa (cond-validacao e cond-recusa) e as de fabrica (cond-fabrica) saem da conta;
//   o "registro" (a classe Ouro dizendo que atende "OURO") sai; as listas de validos
//   (um bloco que cita todos os casos so para conferir) saem. Depois decide:
//     - localizacao: sobrou alguma linha que nomeia um caso fora da casa dele (conta,
//       instanceof, rotulo de switch, conjunto parcial ou recusa)? espalhado. Senao, isolado.
//     - selecao: sobrou alguma que nomeia E calcula? condicional-no-calculo. Senao,
//       houve fabrica? condicional-unica. Senao, consulta.
//     - alarme: se o pacote inteiro nao cita nenhum nome do ponto, o agente usou outros
//       nomes (GOLD), e a resposta e "indeterminado", nunca um "isolado" falso.
//
//   P5 (negativo()): olha a forma (enum com dados, enum com corpo, classes, mapa,
//   switch) e a proporcao: estrutura (uma classe, um corpo ou uma funcao por regiao),
//   condicional (um switch/if que devolve o numero) ou dados (enum, mapa, tabela,
//   configuracao). Objetos criados por regiao: uma classe so para todas e dado; uma
//   classe por regiao e estrutura.
//
// A "evidencia" de cada pacote mostra so o que CONTOU, com a primeira linha de cada
// regra: e o que o Lucas confere quando discorda do Semgrep.

import { readFileSync, readdirSync } from "node:fs";
import { PONTOS, REGIOES_CLASSE, nomes, casoDe } from "./pontos.mjs";

const [jsonPath, raiz, raizNoJson = raiz] = process.argv.slice(2);
if (!jsonPath || !raiz) {
  console.error("uso: node classificar.mjs <saida.json> <raiz-dos-pacotes> [raiz-no-json]");
  process.exit(2);
}
const out = JSON.parse(readFileSync(jsonPath, "utf8"));

// O caminho de cada achado comeca pela pasta do pacote, relativa a raiz.
const prefixo = raizNoJson.replace(/\\/g, "/").replace(/\/?$/, "/");
const pacoteDe = (p) => p.replace(/\\/g, "/").replace(prefixo, "").split("/")[0];
const curto = (p) => p.replace(/\\/g, "/").replace(/^.*\/java\//, "");

const achados = {};
const textos = {};    // "pacote|arquivo:linha" -> texto da linha
const classesP5 = {}; // pacote -> Map(classe -> [onde]) dos objetos por regiao
for (const r of out.results) {
  const pac = pacoteDe(r.path);
  const regra = r.check_id.split(".").pop();
  const onde = `${curto(r.path)}:${r.start.line}`;
  ((achados[pac] ??= {})[regra] ??= []).push(onde);
  textos[`${pac}|${onde}`] = r.extra?.lines ?? "";
  if (regra === "p5-objeto") {
    const t = r.extra?.metavars?.$T?.abstract_content;
    if (t) { const m = (classesP5[pac] ??= new Map()); m.set(t, [...(m.get(t) ?? []), onde]); }
  }
}

// Erros do Semgrep, por pacote.
const avisos = {}, quebrados = new Set();
for (const e of out.errors ?? []) {
  const tipo = Array.isArray(e.type) ? e.type[0] : e.type;
  const caminho = e.path ?? "";
  const pac = caminho ? pacoteDe(caminho) : "(lote)";
  const linha = e.spans?.[0]?.start?.line ?? "?";
  (avisos[pac] ??= []).push(`${tipo} ${curto(caminho)}:${linha}`);
  if (tipo !== "PartialParsing") quebrados.add(pac);
}
if (avisos["(lote)"]) {
  console.error(`o Semgrep relatou erro sem arquivo: ${avisos["(lote)"].join("; ")}`);
  process.exit(1);
}

const lista = (h, id) => h[id] ?? [];
const conta = (h, id) => lista(h, id).length;
const texto = (pac, onde) => textos[`${pac}|${onde}`] ?? "";
// Cita TODOS os casos do ponto: cada caso por qualquer um dos nomes dele (OURO ou GOLD).
const temNome = (t, n) => new RegExp(`\\b${n}\\b`).test(t);
const citaTodos = (t, ponto) => ponto.casos.every((c) => nomes(ponto).filter((n) => casoDe(ponto, n) === c).some((n) => temNome(t, n)));

// Regua 2.1: o nome "numa lista de validos" nao conta. Uma lista de validos e um bloco
// de achados em linhas seguidas que cita TODOS os casos do ponto e passa no teste do
// tipo (comparacoes ligadas por && ou ||; ou rotulos de switch sem conta).
function semListasDeValidos(pac, achadosDoPonto, ponto, eLista) {
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
      const t = [...new Set(bloco.map((i) => texto(pac, i.onde)))].join("\n");
      if (citaTodos(t, ponto) && eLista(t)) bloco.forEach((i) => fora.add(i.onde));
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
// Comparacoes: ligadas por && ou ||, sem else, sem ternario, sem lambda.
const listaDeComparacoes = (t) => /&&|\|\|/.test(t) && !/\belse\b|\?|->/.test(t);
// Rotulos: "case A, B, C -> {}" ou "case A: case B: case C: break;", sem nada calculado.
const listaDeRotulos = (t) => t.split("\n").every((l) =>
  /^\s*(?:case\s+[^:>-]+(?:->\s*\{\s*\}?|:)\s*)+(?:break\s*;\s*)?$/.test(l));

// O registro: a unidade do caso diz se atende o codigo ("return "OURO".equals(c);" dentro
// de Ouro.java ou NivelOuro.java). E o nome usado como chave, dentro da propria casa.
function eRegistro(pac, onde, ponto) {
  const t = texto(pac, onde).trim();
  // a linha e o return de uma comparacao so (o metodo pode estar todo na mesma linha)
  if (!/(?:^|[{;]\s*)return\s+[^;&|?{}]*(?:equals|==)[^;&|?{}]*;\s*\}?$/.test(t)) return false;
  const arquivo = onde.slice(onde.lastIndexOf("/") + 1);
  return nomes(ponto).some((n) => temNome(t, n)) && ponto.classes.split("|").some((k) => arquivo.includes(k));
}

function positivo(pac, h, ponto) {
  const { id: p } = ponto;
  // Recusar: com throw (cond-validacao) ou devolvendo um erro (cond-recusa).
  const recusas = [...new Set([...lista(h, `${p}-cond-validacao`), ...lista(h, `${p}-cond-recusa`)])];
  const naoConta = new Set([...recusas, ...lista(h, `${p}-cond-fabrica`)]);
  const condCalc = semListasDeValidos(pac, lista(h, `${p}-cond-calc`), ponto, listaDeComparacoes)
    .filter((o) => !naoConta.has(o) && !eRegistro(pac, o, ponto));
  const condValidacao = semListasDeValidos(pac, recusas, ponto, listaDeComparacoes);
  const fabrica = [...new Set([...lista(h, `${p}-switch-fabrica`), ...lista(h, `${p}-cond-fabrica`)])];
  const rotulos = semListasDeValidos(pac, lista(h, `${p}-switch-label`).filter((l) => !fabrica.includes(l)), ponto, listaDeRotulos);
  const conjuntos = lista(h, `${p}-conjunto`).filter((o) => !citaTodos(texto(pac, o), ponto));
  // instanceof dentro de um if que recusa e recusa, nao conta (defeito achado na regressao de 09/10).
  const instRecusa = new Set(lista(h, `${p}-instanceof-recusa`));
  h[`${p}-instanceof`] = lista(h, `${p}-instanceof`).filter((o) => !instRecusa.has(o));
  h[`${p}-instanceof-recusa`] = [...instRecusa];
  // A evidencia mostra o que foi CONTADO.
  h[`${p}-cond-calc`] = condCalc;
  h[`${p}-cond-validacao`] = condValidacao;
  h[`${p}-switch-label`] = rotulos;
  h[`${p}-conjunto`] = conjuntos;
  h[`${p}-fabrica`] = fabrica;
  delete h[`${p}-switch-fabrica`]; delete h[`${p}-cond-fabrica`]; delete h[`${p}-cond-recusa`];
  const calculo = condCalc.length + conta(h, `${p}-instanceof`) + rotulos.length + conjuntos.length;
  const validacao = condValidacao.length + instRecusa.size;
  // Alarme: nenhum nome de caso no pacote inteiro quer dizer que o agente chamou os
  // casos de outro jeito (ex.: TIER_A, nem o enunciado nem o ingles); as regras nao os
  // veriam, e "isolado" seria falso.
  if (conta(h, `${p}-nome`) === 0) return ["indeterminado", "indeterminado"];
  const localizacao = calculo + validacao > 0 ? "espalhado" : "isolado";
  const selecao = calculo > 0 ? "condicional-no-calculo" : fabrica.length > 0 ? "condicional-unica" : "consulta";
  return [localizacao, selecao];
}

function negativo(pac, h) {
  // O mesmo alarme; as taxas num arquivo de configuracao tambem contam como nome.
  if (conta(h, "p5-nome") + conta(h, "p5-config") === 0) return ["outro", "indeterminado"];
  // Objetos por regiao: uma classe so (um objeto de valor, uma tabela) e dado; uma
  // classe por regiao (ou com o nome da regiao) e estrutura.
  const classes = classesP5[pac] ?? new Map();
  const comNome = [...classes.keys()].some((c) => new RegExp(REGIOES_CLASSE).test(c));
  const objEstrutura = classes.size >= 2 || comNome;
  const objDados = classes.size === 1 && !comNome;
  const objNoMapa = [...classes.values()].flat().some((o) => /Map\.|\bput\s*\(|\bentry\s*\(/.test(texto(pac, o)));
  const traducao = new Set(lista(h, "p5-traducao"));
  const rotulos = lista(h, "p5-switch-label").filter((o) => !traducao.has(o));
  h["p5-switch-label"] = rotulos;
  h["p5-objeto"] = [...classes.entries()].map(([c, ls]) => `${c}@${ls[0]}`);
  const estrutura = conta(h, "p5-enum-corpo") + conta(h, "p5-classe-regiao")
    + conta(h, "p5-switch-fabrica") + conta(h, "p5-mapa-funcao") + (objEstrutura ? 1 : 0);
  const condicional = rotulos.length - conta(h, "p5-switch-fabrica");
  const forma = conta(h, "p5-enum-corpo") ? "enum-abstrato"
    : conta(h, "p5-classe-regiao") ? "classes"
    : conta(h, "p5-mapa") + conta(h, "p5-mapa-funcao") + (objNoMapa ? 1 : 0) ? "mapa"
    : condicional + conta(h, "p5-switch-fabrica") > 0 ? "switch"
    : conta(h, "p5-enum-dados") ? "enum-dados"
    : "outro";
  const proporcao = estrutura > 0 ? "estrutura"
    : condicional > 0 ? "condicional"
    : conta(h, "p5-enum-dados") + conta(h, "p5-mapa") + conta(h, "p5-config") + (objDados ? 1 : 0) ? "dados"
    : "indeterminado";
  return [forma, proporcao];
}

const pacotes = readdirSync(raiz, { withFileTypes: true }).filter((d) => d.isDirectory()).map((d) => d.name).sort();
console.log([
  "pacote",
  "P1_localizacao", "P1_selecao", "P2_localizacao", "P2_selecao",
  "P3_localizacao", "P3_selecao", "P4_localizacao", "P4_selecao",
  "P5_forma", "P5_proporcao", "evidencia", "avisos",
].join(","));
for (const pac of pacotes) {
  const h = achados[pac] ?? {};
  let respostas = [...PONTOS.flatMap((ponto) => positivo(pac, h, ponto)), ...negativo(pac, h)];
  if (quebrados.has(pac)) respostas = respostas.map((_, i) => (i === 8 ? "outro" : "indeterminado"));
  // Depois das respostas: positivo() e negativo() deixam em h so os achados que contaram.
  const evidencia = Object.entries(h).filter(([id, ls]) => ls.length && !id.endsWith("-nome") && id !== "p5-traducao")
    .map(([id, ls]) => `${id}=${ls.length}(${ls[0]})`).join(" ");
  console.log([pac, ...respostas, `"${evidencia}"`, `"${(avisos[pac] ?? []).join("; ")}"`].join(","));
}
