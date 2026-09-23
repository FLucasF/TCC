// Prepara os pacotes para a avaliação às cegas da §14.6.
//
// Uso:
//   node avaliacao/ferramentas/anonimizar.mjs <run_id> [run_id ...] [--semente N]
//   node avaliacao/ferramentas/anonimizar.mjs --todas-do-lote LOTE
//
// Para cada execução, produz avaliacao/pacotes/<CODIGO>/ com o código-fonte e
// nada mais. Fora ficam: CLAUDE.md, .claude/, target/, o .git do agente,
// meta.json, a transcrição e o log de build — tudo que diria ao avaliador em
// que braço aquele pacote estava.
//
// As datas de modificação são normalizadas: arquivo do braço COM nasce depois
// do harness ser copiado, e isso é rastro.
//
// O mapa código -> run vai para avaliacao/mapa-anonimizacao.csv, que está no
// .gitignore de propósito. Ele só deve ser versionado DEPOIS que as notas
// estiverem congeladas.

import { readdirSync, existsSync, mkdirSync, copyFileSync, writeFileSync, readFileSync, rmSync, utimesSync, statSync } from "node:fs";
import { join, dirname, relative, sep } from "node:path";

const ARGS = process.argv.slice(2);
const semente = Number(ARGS[ARGS.indexOf("--semente") + 1]) || 20260920;
const runs = ARGS.filter((a, i) => !a.startsWith("--") && ARGS[i - 1] !== "--semente");
if (!runs.length) {
  console.error("uso: node avaliacao/ferramentas/anonimizar.mjs <run_id> [run_id ...] [--semente N]");
  process.exit(2);
}

const PACOTES = join("avaliacao", "pacotes");
const MAPA = join("avaliacao", "mapa-anonimizacao.csv");
const DATA_FIXA = new Date("2026-01-01T00:00:00Z");

// Fora do pacote. Qualquer um destes revela a condição.
const FORA_DIR = new Set(["target", ".claude", ".git", "node_modules", ".mvn"]);
const FORA_ARQ = new Set(["CLAUDE.md", "meta.json", "claude-output.jsonl", "stderr.txt", "build.txt"]);

// Gerador determinístico, para o embaralhamento ser reproduzível a partir da
// semente registrada. Não precisa ser bom, precisa ser o mesmo sempre.
function rng(s) {
  let x = s >>> 0;
  return () => { x ^= x << 13; x >>>= 0; x ^= x >> 17; x ^= x << 5; x >>>= 0; return x / 0x100000000; };
}
const aleatorio = rng(semente);
const ALFABETO = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // sem I, O, 0 e 1
const codigo = () => Array.from({ length: 4 }, () => ALFABETO[Math.floor(aleatorio() * ALFABETO.length)]).join("");

function arquivos(dir, base = dir, acc = []) {
  for (const e of readdirSync(dir, { withFileTypes: true })) {
    if (e.isDirectory()) { if (!FORA_DIR.has(e.name)) arquivos(join(dir, e.name), base, acc); }
    else if (!FORA_ARQ.has(e.name)) acc.push(relative(base, join(dir, e.name)));
  }
  return acc;
}

// Rastro que o próprio modelo deixou: comentário citando a orientação recebida.
// §14.2 manda REGISTRAR, não remover — é resultado do modelo. O avaliador
// precisa saber quantos pacotes vazam, e quais.
const PISTAS = [/CLAUDE\.md/i, /\bharness\b/i, /orienta[cç][õo]es de projeto/i, /conforme (a )?instru[cç]/i, /\bskill\b/i];

// Embaralha a ordem antes de atribuir código: assim o código não denuncia a
// ordem em que as execuções rodaram.
const ordem = [...runs];
for (let i = ordem.length - 1; i > 0; i--) {
  const j = Math.floor(aleatorio() * (i + 1));
  [ordem[i], ordem[j]] = [ordem[j], ordem[i]];
}

mkdirSync(PACOTES, { recursive: true });
const usados = new Set();
const linhasMapa = [];
const vazamentos = [];

for (const run of ordem) {
  const ws = join("runs", run, "workspace");
  if (!existsSync(ws)) { console.error(`  ! ${run}: sem workspace, pulado`); continue; }

  let cod;
  do { cod = codigo(); } while (usados.has(cod));
  usados.add(cod);

  const destino = join(PACOTES, cod);
  if (existsSync(destino)) rmSync(destino, { recursive: true, force: true });

  const lista = arquivos(ws);
  for (const rel of lista) {
    const de = join(ws, rel), para = join(destino, rel);
    mkdirSync(dirname(para), { recursive: true });
    copyFileSync(de, para);
    utimesSync(para, DATA_FIXA, DATA_FIXA);
  }
  // Normaliza também as pastas, do mais fundo para o mais raso.
  const dirs = [...new Set(lista.map((r) => dirname(r)).filter((d) => d !== "."))]
    .sort((a, b) => b.split(sep).length - a.split(sep).length);
  for (const d of dirs) utimesSync(join(destino, d), DATA_FIXA, DATA_FIXA);
  utimesSync(destino, DATA_FIXA, DATA_FIXA);

  const pistas = [];
  for (const rel of lista.filter((r) => /\.(java|xml|properties|yml|yaml|md)$/i.test(r))) {
    const texto = readFileSync(join(destino, rel), "utf8");
    for (const p of PISTAS) if (p.test(texto)) pistas.push(`${rel}: ${p}`);
  }
  if (pistas.length) vazamentos.push({ cod, run, pistas });

  linhasMapa.push({ cod, run, arquivos: lista.length, pistas: pistas.length });
  console.log(`  ${cod}  <-  ${run.padEnd(26)} ${String(lista.length).padStart(3)} arquivos${pistas.length ? `  ** ${pistas.length} pista(s) de condicao **` : ""}`);
}

linhasMapa.sort((a, b) => a.cod.localeCompare(b.cod));
writeFileSync(MAPA,
  "codigo_cego,run_id,arquivos,pistas_de_condicao,semente\n" +
  linhasMapa.map((l) => `${l.cod},${l.run},${l.arquivos},${l.pistas},${semente}`).join("\n") + "\n");

// Planilha da avaliação, uma linha por pacote x ponto, em ordem de código.
//
// Até 21/09/2026 saíam QUATRO planilhas daqui: `notas-autor.csv`,
// `notas-professor.csv` e `consenso.csv`, com os seis critérios da rubrica, mais
// esta. A rubrica foi removida (§14.4) e com ela o segundo avaliador e o kappa,
// então sobra uma só. `C5_confirmado` também saiu: era referência a critério da
// rubrica. No lugar entrou `forma`, que é o desfecho secundário descritivo da
// §14.4, anotado à mão ao abrir o pacote.
const CAB_EXTENSAO = "codigo_cego,ponto,extensao,passou_nos_casos,arquivos_criados,arquivos_alterados,linhas_alteradas,forma,observacoes";
const EXT = { P1: "DRONE", P2: "DEZOFF", P3: "CARTEIRA_DIGITAL" };
const linhasExtensao = linhasMapa.flatMap((l) => ["P1", "P2", "P3"].map((p) => `${l.cod},${p},${EXT[p]},,,,,,`));

for (const [arq, cab, linhas] of [
  ["notas-extensao.csv", CAB_EXTENSAO, linhasExtensao],
]) {
  const caminho = join("avaliacao", arq);
  if (existsSync(caminho) && readFileSync(caminho, "utf8").split("\n").some((l, i) => i > 0 && l.split(",").slice(2).join("").trim())) {
    console.log(`\n  ! ${arq} já tem nota preenchida. NÃO foi sobrescrito.`);
    continue;
  }
  writeFileSync(caminho, cab + "\n" + linhas.join("\n") + "\n");
}

console.log(`\n${linhasMapa.length} pacotes em ${PACOTES}/`);
console.log(`mapa: ${MAPA}  (semente ${semente})`);

if (vazamentos.length) {
  console.log(`\n${vazamentos.length} pacote(s) com pista da condicao no proprio codigo:`);
  for (const v of vazamentos) {
    console.log(`  ${v.cod} (${v.run})`);
    for (const p of v.pistas.slice(0, 5)) console.log(`      ${p}`);
  }
  console.log("\n  §14.2: pista no codigo e RESULTADO DO MODELO e nao se remove.");
  console.log("  O avaliador anota que viu, e o numero entra nas ameacas a validade.");
}

console.log(`
PROXIMO PASSO, e ele e manual:
  1. Mover ${MAPA} para fora desta pasta, ou pelo menos nao abrir.
  2. Aplicar as tres extensoes em cada pacote, preenchendo
     avaliacao/notas-extensao.csv. O procedimento esta na §14.5.
  3. git add + commit, congelando a planilha.
  4. So entao reabrir o mapa.`);
