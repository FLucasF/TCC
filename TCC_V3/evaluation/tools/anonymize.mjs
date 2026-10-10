// Prepara os pacotes para a avaliacao as cegas.
//
// Uso:
//   node evaluation/tools/anonymize.mjs <run_id> [run_id ...] [--seed N] [--padrao NOME] [--sem-comentarios]
//
// Com --sem-comentarios (a copia que o Lucas le no V4, decisao de 09/10): os
// comentarios do .java, do .xml e as linhas de comentario de .properties/.yml saem,
// e os .md (README e afins) nao entram. As LINHAS ficam no mesmo lugar (o comentario
// vira linha vazia), para o arquivo:linha da leitura bater com o do Semgrep, que roda
// no codigo original. Sem a opcao, o pacote sai byte a byte igual ao de antes.
//
// Com --padrao, pacotes e mapa vao para evaluation/<NOME>/, a pasta do padrao,
// onde ja mora o gabarito que os le. Se o mapa ja existir la, o script recusa:
// sobrescreve-lo apagaria a unica ligacao entre codigo cego e execucao. Sem
// --padrao, grava em evaluation/, como sempre gravou.
//
// Para cada execucao, produz evaluation/packages/<CODIGO>/ com o codigo-fonte e
// NADA MAIS. Fora ficam: CLAUDE.md, .claude/, target/, o .git do agente,
// meta.json, a transcricao e o log de build — tudo que diria a quem avalia em
// que braco aquele pacote estava.
//
// As datas de modificacao sao normalizadas. Arquivo do braco HARNESS nasce
// depois do harness ser copiado, e um `ls -la` entrega isso.
//
// O mapa codigo -> execucao vai para evaluation/mapa-anonimizacao.csv, que esta
// no .gitignore de proposito. Mova-o para fora desta pasta antes de avaliar.
//
// Este script NAO gera planilha de notas. A avaliacao ainda nao esta desenhada,
// e inventar colunas aqui seria decidir por ela. (Desde 09/10 a planilha e do
// sample.mjs, que le o mapa que este script grava.)
//
// COMO LER, e onde entra no V4. Roda uma vez, nas 100 execucoes do lote, com
// --padrao strategy --sem-comentarios:
//   1. embaralha as execucoes com a semente e da a cada uma um codigo de 4 letras;
//   2. copia o codigo de cada uma para evaluation/strategy/packages/<CODIGO>/, sem o
//      que entregaria o nivel (CLAUDE.md, .claude/, meta.json...) e sem comentarios;
//   3. iguala as datas dos arquivos (senao a data diria qual pacote e de qual nivel);
//   4. conta as "pistas" (o codigo citando CLAUDE.md, harness, skill) no original;
//   5. grava o mapa codigo -> execucao, que o Lucas NAO abre ate a leitura ser commitada.
// Depois: o Semgrep roda nas copias cegas (o CSV sai por codigo), o sample.mjs sorteia
// os 20 que o Lucas le, e o compare.mjs cruza as duas leituras pelo codigo.

import {
  readdirSync, existsSync, mkdirSync, copyFileSync, writeFileSync,
  readFileSync, utimesSync, statSync,
} from "node:fs";
import { join, dirname } from "node:path";
import { copiaSemComentarios } from "./sem-comentarios.mjs";

const ARGS = process.argv.slice(2);
const iSeed = ARGS.indexOf("--seed");
const seed = iSeed >= 0 ? Number(ARGS[iSeed + 1]) : 20260923;
const iPadrao = ARGS.indexOf("--padrao");
const padrao = iPadrao >= 0 ? ARGS[iPadrao + 1] : null;
const semComentarios = ARGS.includes("--sem-comentarios");
const runs = ARGS.filter((a, i) => !a.startsWith("--") && ARGS[i - 1] !== "--seed" && ARGS[i - 1] !== "--padrao");

if (!runs.length) {
  console.error("uso: node evaluation/tools/anonymize.mjs <run_id> [run_id ...] [--seed N] [--padrao NOME]");
  process.exit(2);
}
if (iPadrao >= 0 && !/^[a-z0-9-]+$/.test(padrao ?? "")) {
  console.error(`--padrao deve ser o nome de uma pasta de evaluation/ (a-z, 0-9, -): '${padrao ?? ""}'`);
  process.exit(2);
}

const BASE = padrao ? join("evaluation", padrao) : "evaluation";
const PACOTES = join(BASE, "packages");
const MAPA = join(BASE, "mapa-anonimizacao.csv");

if (padrao && existsSync(MAPA)) {
  console.error(`${MAPA} ja existe. Guarde-o fora da pasta antes; o script nao sobrescreve mapa.`);
  process.exit(1);
}
const DATA_FIXA = new Date("2026-01-01T00:00:00Z");

// Fora do pacote. Qualquer um destes revela a condicao ou e ruido.
const FORA_DIR = new Set(["target", ".claude", ".git", "node_modules", ".mvn"]);
const FORA_ARQ = new Set(["CLAUDE.md", "meta.json", "claude-output.jsonl", "stderr.txt", "build.txt"]);

// Sem --sem-comentarios, comentario citando o harness e RESULTADO DO MODELO e NAO se
// remove: registra-se, e o numero vai para as ameacas a validade. Com a opcao (V4),
// todos os comentarios saem da copia, mas a pista continua contada no ORIGINAL, para o
// mapa dizer quantos pacotes tinham pista antes da limpeza.
const PISTA = /CLAUDE\.md|harness|orienta[cç][oõ]es de projeto|\bskill\b/i;

// O removedor de comentarios mora em sem-comentarios.mjs (09/10), que o Semgrep tambem usa.

// Gerador deterministico, para o embaralhamento ser reproduzivel a partir da
// semente registrada. Nao precisa ser bom, precisa ser o mesmo sempre.
function rng(s) {
  let x = s >>> 0 || 1;
  return () => {
    x ^= x << 13; x >>>= 0;
    x ^= x >> 17;
    x ^= x << 5; x >>>= 0;
    return x / 0xffffffff;
  };
}

// Sem vogais, para nao formar palavra por acidente, e sem 0/O e 1/I.
const ALFABETO = "23456789BCDFGHJKLMNPQRSTVWXZ";

function varrer(dir, base = dir) {
  const achados = [];
  for (const i of readdirSync(dir, { withFileTypes: true })) {
    if (i.isDirectory()) {
      if (FORA_DIR.has(i.name)) continue;
      achados.push(...varrer(join(dir, i.name), base));
    } else {
      if (FORA_ARQ.has(i.name)) continue;
      achados.push(join(dir, i.name));
    }
  }
  return achados;
}

const aleatorio = rng(seed);

// Embaralha as execucoes ANTES de atribuir codigo: a ordem das pastas nao pode
// seguir a ordem em que elas rodaram.
const ordem = runs.slice();
for (let i = ordem.length - 1; i > 0; i--) {
  const j = Math.floor(aleatorio() * (i + 1));
  [ordem[i], ordem[j]] = [ordem[j], ordem[i]];
}

const usados = new Set();
const codigo = () => {
  let c;
  do { c = Array.from({ length: 4 }, () => ALFABETO[Math.floor(aleatorio() * ALFABETO.length)]).join(""); }
  while (usados.has(c));
  usados.add(c);
  return c;
};

mkdirSync(PACOTES, { recursive: true });

const linhasMapa = [];
const vazamentos = [];

for (const run of ordem) {
  const ws = join("runs", run, "workspace");
  if (!existsSync(ws)) { console.error(`  ! ${run}: workspace nao encontrado, pulado`); continue; }

  const cod = codigo();
  const destino = join(PACOTES, cod);
  if (existsSync(destino)) { console.error(`  ! ${destino} ja existe, pulado`); continue; }

  const lista = varrer(ws);
  const pistas = [];

  for (const origem of lista) {
    const relativo = origem.slice(ws.length + 1);
    const alvo = join(destino, relativo);
    // Com --sem-comentarios, o .md nao e copiado, mas a pista nele ainda e contada (abaixo).
    const copia = !(semComentarios && /\.md$/i.test(relativo));
    if (copia) {
      mkdirSync(dirname(alvo), { recursive: true });
      if (semComentarios) copiaSemComentarios(origem, alvo, relativo);
      else copyFileSync(origem, alvo);
      // Normaliza a data. Sem isto, o braco HARNESS tem arquivos mais novos.
      utimesSync(alvo, DATA_FIXA, DATA_FIXA);
    }

    if (/\.(java|md|xml|properties|ya?ml|txt)$/i.test(relativo)) {
      let texto = "";
      try { texto = readFileSync(origem, "utf8"); } catch { /* binario */ }
      texto.split(/\r?\n/).forEach((l, n) => {
        if (PISTA.test(l)) pistas.push(`${relativo}:${n + 1}: ${l.trim().slice(0, 90)}`);
      });
    }
  }

  // As PASTAS tambem, de baixo para cima — senao `src/` e as intermediarias
  // guardam a hora em que o anonimizador rodou. Isso nao vaza a condicao (todos
  // os pacotes sao gerados juntos), mas um `ls -la` mostrando datas diferentes
  // entre pacotes convida quem avalia a reparar em coisa que nao e o codigo.
  const pastas = new Set();
  for (const origem of lista) {
    let d = dirname(join(destino, origem.slice(ws.length + 1)));
    while (d.startsWith(destino)) { pastas.add(d); d = dirname(d); }
  }
  for (const d of [...pastas].sort((a, b) => b.length - a.length)) {
    try { utimesSync(d, DATA_FIXA, DATA_FIXA); } catch { /* alguns SO recusam */ }
  }

  linhasMapa.push({ cod, run, arquivos: lista.length, pistas: pistas.length });
  if (pistas.length) vazamentos.push({ cod, run, pistas });

  console.log(
    `  ${cod}  <-  ${run.padEnd(30)} ${String(lista.length).padStart(3)} arquivos` +
    (pistas.length ? `  ** ${pistas.length} pista(s) de condicao **` : "")
  );
}

linhasMapa.sort((a, b) => a.cod.localeCompare(b.cod));
writeFileSync(
  MAPA,
  "blind_code,run_id,files,condition_leaks,seed\n" +
  linhasMapa.map((l) => `${l.cod},${l.run},${l.arquivos},${l.pistas},${seed}`).join("\n") + "\n"
);

console.log(`\n${linhasMapa.length} pacotes em ${PACOTES}/`);
console.log(`mapa: ${MAPA}  (semente ${seed})`);

if (vazamentos.length) {
  console.log(`\n${vazamentos.length} pacote(s) com pista da condicao no proprio codigo:`);
  for (const v of vazamentos) {
    console.log(`  ${v.cod}`);
    for (const p of v.pistas.slice(0, 5)) console.log(`      ${p}`);
  }
  if (semComentarios) {
    console.log("\n  Os comentarios sairam da copia; o numero acima e o que havia no original.");
    console.log("  O que sobra (nomes de classe, por exemplo) quem avalia anota na planilha.");
  } else {
    console.log("\n  Pista no codigo e RESULTADO DO MODELO e nao se remove.");
    console.log("  Quem avalia anota que viu, e o numero entra nas ameacas a validade.");
  }
}

console.log(`
PROXIMO PASSO, e ele e manual:
  1. Mova ${MAPA} para fora desta pasta, ou pelo menos nao abra.
  2. Avalie os pacotes sem saber de que braco veio cada um.
  3. Congele o resultado da avaliacao com um commit.
  4. So entao reabra o mapa.`);
