// Compara duas leituras do mesmo conjunto de pacotes, pergunta por pergunta, e aplica
// a regra de saida pre-registrada.
//
// Uso:
//   node evaluation/tools/compare.mjs <leitura-a.csv> <leitura-b.csv> [--limite 0.9]
//   node evaluation/tools/compare.mjs leitura-lucas.csv semgrep-V4.csv        (a conferencia)
//   node evaluation/tools/compare.mjs leitura-lucas.csv releitura-lucas.csv   (a releitura)
//
// Cada CSV tem uma linha por pacote, identificada por "blind_code" (a planilha do
// Lucas) ou "pacote" (o CSV do Semgrep); linhas comecando por # sao ignoradas. So
// entram os pacotes que estao em A (a amostra lida). Perguntas comparadas: P4_localizacao,
// P4_selecao, P5_forma, P5_proporcao.
//
// Regra de saida (09/10): uma pergunta VALE para o lote inteiro se a concordancia for
// pelo menos o limite (0,9: 40 de 44 no V5; 18 de 20 no V4). Abaixo disso, ela vira DESCRITIVA. Celula vazia
// ou "indeterminado", de qualquer lado, conta como discordancia: na duvida, nao vale.
//
// COMO LER. Le as duas planilhas, casa as linhas pelo codigo do pacote e, em cada uma
// das 4 perguntas, conta em quantos pacotes as duas respostas sao iguais. Imprime uma
// tabela (pergunta, concordaram, decisao) e, embaixo, cada discordancia com a evidencia
// dos dois lados (o arquivo:linha), para abrir o codigo e ver quem tem razao. A conta
// oficial e a primeira: corrigir uma regra depois de ver a discordancia nao a refaz.

import { readFileSync } from "node:fs";

const PERGUNTAS = ["P4_localizacao", "P4_selecao", "P5_forma", "P5_proporcao"];

const args = process.argv.slice(2);
const iLimite = args.indexOf("--limite");
const limite = iLimite >= 0 ? Number(args[iLimite + 1]) : 0.9;
const [arqA, arqB] = args.filter((a, i, l) => !a.startsWith("--") && l[i - 1] !== "--limite");
if (!arqA || !arqB || !(limite > 0 && limite <= 1)) {
  console.error("uso: node compare.mjs <leitura-a.csv> <leitura-b.csv> [--limite 0.9]");
  process.exit(2);
}

// CSV com campos entre aspas (a evidencia tem virgulas).
function linhasCsv(texto) {
  const linhas = [];
  let campo = "", linha = [], aspas = false;
  for (let i = 0; i < texto.length; i++) {
    const c = texto[i];
    if (aspas) {
      if (c === '"' && texto[i + 1] === '"') { campo += '"'; i++; }
      else if (c === '"') aspas = false;
      else campo += c;
    } else if (c === '"') aspas = true;
    else if (c === ",") { linha.push(campo); campo = ""; }
    else if (c === "\n" || c === "\r") {
      if (c === "\r" && texto[i + 1] === "\n") i++;
      linha.push(campo); linhas.push(linha); linha = []; campo = "";
    } else campo += c;
  }
  if (campo || linha.length) { linha.push(campo); linhas.push(linha); }
  return linhas.filter((l) => l.length > 1 || l[0]);
}

function ler(arquivo) {
  const linhas = linhasCsv(readFileSync(arquivo, "utf8")).filter((l) => !l[0].startsWith("#"));
  const cab = linhas[0];
  const chave = cab.includes("blind_code") ? "blind_code" : "pacote";
  if (!cab.includes(chave)) throw new Error(`${arquivo}: sem coluna blind_code nem pacote`);
  const faltam = PERGUNTAS.filter((p) => !cab.includes(p));
  if (faltam.length) throw new Error(`${arquivo}: faltam as colunas ${faltam.join(", ")}`);
  const porPacote = new Map();
  for (const l of linhas.slice(1)) {
    const r = Object.fromEntries(cab.map((c, i) => [c, (l[i] ?? "").trim()]));
    porPacote.set(r[chave], r);
  }
  return porPacote;
}

const A = ler(arqA), B = ler(arqB);
const pacotes = [...A.keys()].sort();
const faltandoEmB = pacotes.filter((p) => !B.has(p));
if (faltandoEmB.length) {
  console.error(`ERRO: ${faltandoEmB.length} pacote(s) de A sem linha em B: ${faltandoEmB.join(" ")}`);
  process.exit(1);
}

const valido = (v) => v !== "" && v !== "indeterminado";
// Na evidencia do Semgrep ("p4-cond-calc=2(Arq.java:40) p5-..."), so os achados do
// ponto da pergunta; a evidencia escrita a mao passa inteira.
const doPonto = (ev = "", pergunta) => {
  const prefixo = pergunta.slice(0, 2).toLowerCase() + "-";
  const itens = ev.split(" ").filter((t) => /^p\d-/.test(t));
  return itens.length ? itens.filter((t) => t.startsWith(prefixo)).join(" ") || "(nenhum achado neste ponto)" : ev;
};
const minimo = Math.ceil(limite * pacotes.length);
console.log(`# Comparação: ${arqA}  ×  ${arqB}`);
console.log(`\n${pacotes.length} pacotes; a pergunta vale se concordarem em pelo menos ${minimo} (limite ${limite}).\n`);
console.log("| pergunta | concordaram | decisão |");
console.log("|---|---|---|");
const discordancias = [];
for (const p of PERGUNTAS) {
  let iguais = 0;
  for (const pac of pacotes) {
    const a = A.get(pac)[p], b = B.get(pac)[p];
    if (valido(a) && valido(b) && a === b) iguais++;
    else discordancias.push({ pac, p, a: a || "(vazio)", b: b || "(vazio)", evA: doPonto(A.get(pac).evidencia, p), evB: doPonto(B.get(pac).evidencia, p) });
  }
  console.log(`| ${p} | ${iguais} de ${pacotes.length} | ${iguais >= minimo ? "**vale** para o lote" : "vira **descritiva**"} |`);
}
if (discordancias.length) {
  console.log("\n## Discordâncias (para abrir o arquivo:linha e ver quem tem razão)\n");
  for (const d of discordancias) {
    console.log(`- **${d.pac}**, ${d.p}: A = \`${d.a}\`, B = \`${d.b}\``);
    if (d.evA) console.log(`  - evidência de A: ${d.evA.slice(0, 200)}`);
    if (d.evB) console.log(`  - evidência de B: ${d.evB.slice(0, 200)}`);
  }
  console.log("\nA regra não é corrigida para refazer esta conta: a concordância oficial é esta.");
} else {
  console.log("\nNenhuma discordância.");
}
