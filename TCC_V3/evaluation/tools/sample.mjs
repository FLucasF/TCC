// Sorteia os pacotes que o Lucas le, e gera a planilha dele em branco.
//
// Uso:
//   node evaluation/tools/sample.mjs amostra   <mapa.csv> <pasta-de-saida> --seed N
//   node evaluation/tools/sample.mjs releitura <pasta-de-saida> --seed N [--quantos 5]
//
// "amostra": 1 pacote por modelo x nivel (no V4, 5 x 4 = 20), sorteado entre as
// replicas. Le o mapa de anonimizacao para saber o modelo e o nivel de cada codigo,
// mas so escreve CODIGOS, em ordem alfabetica: nada na saida diz o braco.
//   <saida>/amostra.csv             os codigos sorteados
//   <saida>/leitura-lucas.csv       a planilha em branco, uma linha por codigo
//
// "releitura": sorteia, ENTRE os codigos da amostra, os que o Lucas rele uma ou duas
// semanas depois. Fica num comando separado de proposito: quem sabe desde o comeco
// quais vai reler pode guardar as respostas deles.
//   <saida>/releitura.csv           os codigos
//   <saida>/releitura-lucas.csv     a planilha em branco da releitura
//
// O run_id do V4 termina em <replica>-<MODELO>-<NIVEL> (ex.: V4-STRATEGY-03-HAIKU45-N2).
// Nada e sobrescrito: se a saida ja existe, o script recusa.

import { readFileSync, writeFileSync, existsSync, mkdirSync } from "node:fs";
import { join } from "node:path";

const COLUNAS = [
  "leitor", "blind_code", "P4_localizacao", "P4_selecao", "P5_forma", "P5_proporcao",
  "evidencia", "duvida", "achei_que_sabia_nivel", "observacao",
];

const args = process.argv.slice(2);
const modo = args[0];
const iSeed = args.indexOf("--seed");
const seed = iSeed >= 0 ? Number(args[iSeed + 1]) : NaN;
const iQuantos = args.indexOf("--quantos");
const quantos = iQuantos >= 0 ? Number(args[iQuantos + 1]) : 5;
const posicionais = args.slice(1).filter((a, i, l) => !a.startsWith("--") && !["--seed", "--quantos"].includes(l[i - 1]));

const morrer = (m) => { console.error(`ERRO: ${m}`); process.exit(1); };
if (!["amostra", "releitura"].includes(modo)) morrer("modo deve ser 'amostra' ou 'releitura'");
if (!Number.isInteger(seed)) morrer("informe --seed N (inteiro), e registre a semente");

// O mesmo gerador do anonymize.mjs: reproduzivel a partir da semente.
function rng(s) {
  let x = s >>> 0 || 1;
  return () => { x ^= x << 13; x >>>= 0; x ^= x >> 17; x ^= x << 5; x >>>= 0; return x / 4294967296; }; // 2^32: nunca da 1
}
const aleatorio = rng(seed);
const sorteia = (lista) => lista[Math.floor(aleatorio() * lista.length)];
const embaralha = (lista) => {
  const l = lista.slice();
  for (let i = l.length - 1; i > 0; i--) { const j = Math.floor(aleatorio() * (i + 1)); [l[i], l[j]] = [l[j], l[i]]; }
  return l;
};
const planilha = (codigos) =>
  COLUNAS.join(",") + "\n" + codigos.map((c) => ["lucas", c, ...Array(COLUNAS.length - 2).fill("")].join(",")).join("\n") + "\n";
const escreve = (arquivo, texto) => {
  if (existsSync(arquivo)) morrer(`${arquivo} ja existe; nada foi sobrescrito`);
  writeFileSync(arquivo, texto);
};

if (modo === "amostra") {
  const [mapa, saida] = posicionais;
  if (!mapa || !saida) morrer("uso: sample.mjs amostra <mapa.csv> <pasta-de-saida> --seed N");
  const linhas = readFileSync(mapa, "utf8").trim().split(/\r?\n/).slice(1).map((l) => l.split(","));
  const grupos = new Map();
  for (const [codigo, run] of linhas) {
    const m = run.match(/-(\d+)-([A-Z0-9]+)-(N[0-9])$/);
    if (!m) morrer(`run_id fora do formato <replica>-<MODELO>-<NIVEL>: ${run}`);
    const chave = `${m[2]} ${m[3]}`;
    if (!grupos.has(chave)) grupos.set(chave, []);
    grupos.get(chave).push(codigo);
  }
  // Ordem fixa dos grupos e dos codigos antes do sorteio: o resultado so depende da semente.
  const escolhidos = [...grupos.keys()].sort().map((k) => sorteia(grupos.get(k).sort()));
  mkdirSync(saida, { recursive: true });
  const ordenados = escolhidos.slice().sort();
  escreve(join(saida, "amostra.csv"), "blind_code\n" + ordenados.join("\n") + "\n");
  escreve(join(saida, "leitura-lucas.csv"), planilha(ordenados));
  console.log(`${ordenados.length} pacotes sorteados (1 por modelo x nivel, ${grupos.size} grupos), semente ${seed}`);
  console.log(`  ${join(saida, "amostra.csv")}`);
  console.log(`  ${join(saida, "leitura-lucas.csv")}  <- a planilha em branco`);
} else {
  const [saida] = posicionais;
  if (!saida) morrer("uso: sample.mjs releitura <pasta-de-saida> --seed N [--quantos 5]");
  const amostra = readFileSync(join(saida, "amostra.csv"), "utf8").trim().split(/\r?\n/).slice(1);
  if (quantos > amostra.length) morrer(`a amostra tem ${amostra.length} pacotes; nao da para reler ${quantos}`);
  const escolhidos = embaralha(amostra.sort()).slice(0, quantos).sort();
  escreve(join(saida, "releitura.csv"), "blind_code\n" + escolhidos.join("\n") + "\n");
  escreve(join(saida, "releitura-lucas.csv"), planilha(escolhidos));
  console.log(`${escolhidos.length} pacotes para a releitura, semente ${seed}`);
  console.log(`  ${join(saida, "releitura-lucas.csv")}  <- a planilha em branco`);
}
