// Testa o testador. Sobe uma aplicação de mentira com defeito conhecido e
// confere se o `comparar.mjs` acusa exatamente o que deveria acusar.
//
// Uso:  node avaliacao/ferramentas/autoteste.mjs
//
// Por que existe: em 19/09/2026 apareceram cinco defeitos nas ferramentas de
// medição, três achados por acaso, e em 20/09 mais um — a ferramenta não sabia
// expressar caso de erro. Ferramenta de medição sem teste próprio reporta
// número errado em silêncio, e o número errado vira resultado do TCC.
//
// Não substitui a app de referência em Docker: esta roda só o comparador, sem
// Maven, Spring nem container. Cobre a camada onde os defeitos apareceram.

import { createServer } from "node:http";
import { spawn } from "node:child_process";
import { writeFileSync, mkdtempSync } from "node:fs";
import { join, dirname } from "node:path";
import { tmpdir } from "node:os";
import { fileURLToPath } from "node:url";

const AQUI = dirname(fileURLToPath(import.meta.url));
const COMPARAR = join(AQUI, "comparar.mjs");

// Dois casos, um de cada natureza. Valores conferidos à mão:
// Camiseta 100,00 x 1 (0,50 kg), ECONOMICA -> frete 12 + 2x0,50 = 13,00;
// total do pedido 113,00; PIX 5% = 5,65; total final 107,35.
const CASOS = [
  {
    id: "auto-sucesso",
    descricao: "ECONOMICA + PIX",
    requisicao: {
      itens: [{ nome: "Camiseta", precoUnitario: 100.0, quantidade: 1, pesoKg: 0.5 }],
      modalidadeEntrega: "ECONOMICA",
      formaPagamento: "PIX",
    },
    esperado: {
      subtotalProdutos: 100.0, descontoCupom: 0, frete: 13.0, prazoEntregaDias: 7,
      ajustePagamento: -5.65, totalFinal: 107.35, parcelas: 1, valorParcela: 107.35,
    },
  },
  {
    id: "auto-erro",
    descricao: "carrinho vazio",
    requisicao: { itens: [], modalidadeEntrega: "ECONOMICA", formaPagamento: "PIX" },
    status_esperado: 400,
    esperado: { erro: "PEDIDO_INVALIDO" },
  },
];

const OK = {
  subtotalProdutos: 100.0, descontoCupom: 0, frete: 13.0, prazoEntregaDias: 7,
  ajustePagamento: -5.65, totalFinal: 107.35, parcelas: 1, valorParcela: 107.35,
};

// Cada variante é uma app com um defeito conhecido, e o número de casos que o
// comparador tem obrigação de reprovar.
const VARIANTES = [
  {
    nome: "correta",
    esperaFalhas: 0,
    porque: "app certa: nenhum caso pode falhar",
    responder: (req) => (req.itens?.length ? { status: 200, corpo: OK }
                                           : { status: 400, corpo: { erro: "PEDIDO_INVALIDO" } }),
  },
  {
    nome: "erro-devolvido-como-200",
    esperaFalhas: 1,
    porque: "corpo de erro certo, mas HTTP 200: tem que reprovar pelo status",
    responder: (req) => (req.itens?.length ? { status: 200, corpo: OK }
                                           : { status: 200, corpo: { erro: "PEDIDO_INVALIDO" } }),
  },
  {
    nome: "codigo-de-erro-trocado",
    esperaFalhas: 1,
    porque: "HTTP 400 certo, código errado: tem que reprovar pelo campo",
    responder: (req) => (req.itens?.length ? { status: 200, corpo: OK }
                                           : { status: 400, corpo: { erro: "MODALIDADE_INVALIDA" } }),
  },
  {
    nome: "total-certo-frete-errado",
    esperaFalhas: 1,
    porque: "totalFinal certo e frete zerado: é a regra de conferir campo a campo",
    responder: (req) => (req.itens?.length ? { status: 200, corpo: { ...OK, frete: 0 } }
                                           : { status: 400, corpo: { erro: "PEDIDO_INVALIDO" } }),
  },
  {
    nome: "erro-com-corpo-vazio",
    esperaFalhas: 1,
    porque: "status certo e corpo vazio: não pode passar por falta de campo",
    responder: (req) => (req.itens?.length ? { status: 200, corpo: OK }
                                           : { status: 400, corpo: null }),
  },
];

let variante = VARIANTES[0];

const servidor = createServer((req, res) => {
  let dados = "";
  req.on("data", (c) => (dados += c));
  req.on("end", () => {
    let corpoReq = {};
    try { corpoReq = JSON.parse(dados || "{}"); } catch { corpoReq = {}; }
    const r = variante.responder(corpoReq);
    res.writeHead(r.status, { "Content-Type": "application/json" });
    res.end(r.corpo === null ? "" : JSON.stringify(r.corpo));
  });
});

await new Promise((r) => servidor.listen(0, "127.0.0.1", r));
const porta = servidor.address().port;

const dir = mkdtempSync(join(tmpdir(), "autoteste-"));
const arquivoCasos = join(dir, "casos.json");
writeFileSync(arquivoCasos, JSON.stringify(CASOS, null, 2));

console.log(`autoteste do comparar.mjs  (porta ${porta})\n`);

// `spawn`, nunca `spawnSync`: o servidor de mentira vive neste mesmo processo,
// e a versão síncrona trava o event loop — o filho ficaria os 180s do `subiu()`
// esperando uma resposta que este processo não tem como emitir.
const rodarComparador = () =>
  new Promise((resolve) => {
    const filho = spawn(process.execPath, [COMPARAR, arquivoCasos], {
      env: { ...process.env, URL_ALVO: `http://127.0.0.1:${porta}/checkout/resumo` },
    });
    let saida = "";
    filho.stdout.on("data", (c) => (saida += c));
    filho.stderr.on("data", (c) => (saida += c));
    filho.on("close", (codigo) => resolve({ codigo, saida }));
  });

let reprovou = 0;
for (const v of VARIANTES) {
  variante = v;
  const { codigo: falhas, saida } = await rodarComparador();
  const bate = falhas === v.esperaFalhas;
  if (!bate) reprovou++;
  console.log(`  ${bate ? "ok  " : "RUIM"}  ${v.nome.padEnd(26)} falhas=${falhas} esperado=${v.esperaFalhas}`);
  console.log(`        ${v.porque}`);
  if (!bate) console.log(saida.replace(/^/gm, "        | "));
}

servidor.close();
console.log(`\n${VARIANTES.length - reprovou}/${VARIANTES.length} variantes com o comportamento esperado`);
process.exit(reprovou);
