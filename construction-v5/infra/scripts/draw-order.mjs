// Sorteia a ordem dos quartetos do V4 (decisao de 08/10, OBJETIVO secao 2): cada rodada
// tem uma replica de cada modelo, e a ordem dos modelos dentro de cada rodada e sorteada.
// As rodadas vao em sequencia (a 1, depois a 2...), para um corte no meio do lote deixar
// replicas inteiras, e nao um modelo com 5 replicas e outro com nenhuma.
//
// Uso:  node infra/scripts/draw-order.mjs <desenho.json> <saida.csv> --seed N
//       node infra/scripts/draw-order.mjs experiment/desenho-v4.json experiment/ordem-v4.csv --seed 20261009
//
// A saida tem uma linha por quarteto, com o comando que o roda. Nada e sobrescrito: se a
// saida existe, o script recusa (sortear de novo ate gostar da ordem e o que isso impede).
// A semente vai no DECISOES.md; com ela, qualquer um refaz o mesmo sorteio.

import { readFileSync, writeFileSync, existsSync } from "node:fs";

const args = process.argv.slice(2);
const iSeed = args.indexOf("--seed");
const seed = iSeed >= 0 ? Number(args[iSeed + 1]) : NaN;
const [arqDesenho, saida] = args.filter((a, i, l) => !a.startsWith("--") && l[i - 1] !== "--seed");
const morrer = (m) => { console.error(`ERRO: ${m}`); process.exit(1); };
if (!arqDesenho || !saida) morrer("uso: draw-order.mjs <desenho.json> <saida.csv> --seed N");
if (!Number.isInteger(seed)) morrer("informe --seed N (inteiro), e registre a semente");
if (existsSync(saida)) morrer(`${saida} ja existe; nada foi sobrescrito`);

const D = JSON.parse(readFileSync(arqDesenho, "utf8"));
if (!D.modelos) morrer("o desenho ainda nao tem modelos (saem do mapa)");

// O mesmo gerador do evaluation/tools/sample.mjs (xorshift32), dividido por 2^32 para
// nunca dar 1.
function rng(s) {
  let x = s >>> 0 || 1;
  return () => { x ^= x << 13; x >>>= 0; x ^= x >> 17; x ^= x << 5; x >>>= 0; return x / 4294967296; };
}
const aleatorio = rng(seed);
const embaralha = (lista) => {
  const l = lista.slice();
  for (let i = l.length - 1; i > 0; i--) { const j = Math.floor(aleatorio() * (i + 1)); [l[i], l[j]] = [l[j], l[i]]; }
  return l;
};

// Ordem fixa antes do sorteio: o resultado so depende da semente.
const apelidos = Object.keys(D.modelos).sort();
// O run-levels.sh le o experiment/desenho-v5.json por padrao (ate o V4, o -v4); outro desenho (o
// exploratorio) vai no comando, para o quarteto nao rodar com os modelos errados.
const PADRAO = "experiment/desenho-v5.json";
const env = arqDesenho.replace(/\\/g, "/") === PADRAO ? "" : `DESENHO=${arqDesenho.replace(/\\/g, "/")} `;
const linhas = [];
let posicao = 0;
for (let r = 1; r <= D.replicas; r++) {
  const prefixo = `${D.prefixo}-${String(r).padStart(2, "0")}`;
  for (const a of embaralha(apelidos)) {
    posicao++;
    linhas.push([posicao, r, prefixo, a, D.modelos[a], `${env}infra/scripts/run-levels.sh ${prefixo} ${r} ${a}`]);
  }
}
writeFileSync(saida,
  `# ordem dos quartetos do ${D.prefixo}, sorteada por infra/scripts/draw-order.mjs com a semente ${seed}\n` +
  "posicao,rodada,prefixo,apelido,modelo,comando\n" + linhas.map((l) => l.join(",")).join("\n") + "\n");
console.log(`${linhas.length} quartetos (${D.replicas} rodadas x ${apelidos.length} modelos), semente ${seed} -> ${saida}`);
