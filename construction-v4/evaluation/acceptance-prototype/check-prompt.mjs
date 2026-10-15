// Confere os numeros escritos no enunciado do Strategy (os 5 exemplos e a resposta de
// exemplo do anexo) contra a calculadora de referencia. Pega erro de copia entre os dois:
// os exemplos 1 a 4 e a resposta do anexo do enunciado do V4 sairam da calculadora.
// Nao confere a calculadora: isso e a conferencia humana do gabarito (README).
//
// Uso: node evaluation/acceptance-prototype/check-prompt.mjs [enunciado.md]
// Sai com 0 se todos os numeros batem.
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { calcular } from "./ref-strategy.mjs";

const aqui = path.dirname(fileURLToPath(import.meta.url));
const arquivo = path.resolve(process.argv[2] ?? path.join(aqui, "../../experiment/prompt/prompt.md"));
const txt = fs.readFileSync(arquivo, "utf8").replace(/\r\n/g, "\n");

// as entradas dos exemplos, como o enunciado as descreve (as mesmas do strategy.mjs)
const cam = { nome: "Camiseta", precoUnitario: 79.90, quantidade: 2, pesoKg: 0.30 };
const ten = { nome: "Tenis", precoUnitario: 249.90, quantidade: 1, pesoKg: 1.20 };
const fone = { nome: "Fone", precoUnitario: 199.90, quantidade: 2, pesoKg: 0.25 };
const meia = { nome: "Meia", precoUnitario: 19.90, quantidade: 7, pesoKg: 0.10 };
const entradas = {
  1: { itens: [cam, ten], modalidadeEntrega: "EXPRESSA", cupom: "BEMVINDO10", formaPagamento: "PIX", nivelClube: "BRONZE", regiao: "NORTE" },
  2: { itens: [cam, ten], modalidadeEntrega: "ECONOMICA", formaPagamento: "CARTAO", parcelas: 6, nivelClube: "PRATA", regiao: "CENTRO_OESTE" },
  3: { itens: [fone], modalidadeEntrega: "MOTOBOY", cupom: "MENOS50", formaPagamento: "BOLETO", nivelClube: "BRONZE", regiao: "NORDESTE" },
  4: { itens: [meia, cam], modalidadeEntrega: "RETIRADA_LOJA", cupom: "LEVE3PAGUE2", formaPagamento: "CARTAO", parcelas: 3, nivelClube: "PRATA", regiao: "SUL" },
  5: { itens: [cam, ten], modalidadeEntrega: "EXPRESSA", formaPagamento: "PIX", nivelClube: "OURO", regiao: "SUDESTE" },
};

let conferidos = 0, erros = 0;
const confere = (onde, campo, escrito, calc) => {
  conferidos++;
  if (escrito !== calc) { erros++; console.log(`DIVERGE ${onde} ${campo}: escrito ${escrito}, calculadora ${calc}`); }
};
const centavos = (s) => (s === undefined ? undefined : Math.round(Number(s.replace("−", "-").replace(",", ".")) * 100));

// os exemplos: a linha "→ ..." logo depois de "**Exemplo N**"
const exemplos = [...txt.matchAll(/\*\*Exemplo (\d)\*\*:[^\n]*\n→ ([^\n]*)/g)];
if (exemplos.length !== 5) { console.log(`esperava 5 exemplos, achei ${exemplos.length}`); process.exit(1); }
for (const [, n, linha] of exemplos) {
  const r = calcular(entradas[n]), pega = (re) => linha.match(re)?.[1], onde = `Exemplo ${n}`;
  confere(onde, "subtotal", centavos(pega(/subtotal ([\d,]+)/)), r.subtotalProdutos);
  confere(onde, "cupom", centavos(pega(/cupom ([\d,]+)/)), r.descontoCupom);
  confere(onde, "frete", centavos(pega(/frete ([\d,]+)/)), r.frete);
  confere(onde, "prazo", Number(pega(/prazo (\d+)/)), r.prazoEntregaDias);
  confere(onde, "seguro", centavos(pega(/seguro ([\d,]+)/)), r.seguro);
  confere(onde, "ajuste", centavos(pega(/ajuste ([−\d,]+)/)), r.ajustePagamento);
  confere(onde, "total final", centavos(pega(/total final ([\d,]+)/)), r.totalFinal);
  confere(onde, "parcelas", Number(pega(/(\d+)× de/)), r.parcelas);
  confere(onde, "parcela", centavos(pega(/× de ([\d,]+)/)), r.valorParcela);
  confere(onde, "credito", centavos(pega(/crédito ([\d,]+)/)), r.creditoProximaCompra);
  confere(onde, "brinde", pega(/brinde (sim|não)/) === "sim", r.brinde);
}

// o anexo: a requisicao e a resposta de exemplo. Desde 07/10 elas sao tabelas (campo, o que
// e, exemplo); nos enunciados anteriores eram dois blocos JSON, ainda lidos aqui.
const blocos = [...txt.matchAll(/```json\n([\s\S]*?)```/g)].map((m) => JSON.parse(m[1]));
let req, resp;
if (blocos.length >= 2) {
  [req, resp] = blocos;
} else {
  // a requisicao de exemplo, como a tabela "O que o site envia" a descreve
  req = { itens: [{ nome: "Camiseta", precoUnitario: 79.90, quantidade: 2, pesoKg: 0.30 }, { nome: "Tenis", precoUnitario: 249.90, quantidade: 1, pesoKg: 1.20 }],
          modalidadeEntrega: "EXPRESSA", cupom: "BEMVINDO10", formaPagamento: "PIX", parcelas: 1, nivelClube: "OURO", regiao: "SUDESTE" };
  const secao = (titulo) => { const i = txt.indexOf(titulo); if (i < 0) return ""; const j = txt.indexOf("\n###", i + titulo.length); return txt.slice(i, j < 0 ? undefined : j); };
  const exemplo = (s, campo) => s.match(new RegExp("^\\| `" + campo + "` \\|.*\\| ([^|]+) \\|$", "m"))?.[1].trim().replaceAll("`", "");
  const envia = secao("### O que o site envia");
  confere("anexo", "exemplo dos itens", exemplo(envia, "itens"), "Camiseta, 79.90, 2, 0.30 e Tênis, 249.90, 1, 1.20");
  for (const k of ["modalidadeEntrega", "cupom", "formaPagamento", "nivelClube", "regiao"]) confere("anexo", `exemplo de ${k}`, exemplo(envia, k), req[k]);
  confere("anexo", "exemplo de parcelas", Number(exemplo(envia, "parcelas")), req.parcelas);
  const devolve = secao("### O que o serviço devolve");
  resp = {};
  for (const k of ["subtotalProdutos", "descontoCupom", "frete", "prazoEntregaDias", "seguro", "ajustePagamento", "totalFinal", "parcelas", "valorParcela", "creditoProximaCompra", "brinde"]) {
    const v = exemplo(devolve, k);
    resp[k] = v === "true" ? true : v === "false" ? false : v === undefined ? undefined : Number(v);
  }
}
const r = calcular(req);
for (const [k, v] of Object.entries(resp)) {
  const inteiro = k === "prazoEntregaDias" || k === "parcelas";
  confere("anexo", k, typeof v === "number" && !inteiro ? Math.round(v * 100) : v, r[k]);
}

console.log(`${path.relative(process.cwd(), arquivo)}: ${conferidos - erros} de ${conferidos} numeros batem com a calculadora`);
process.exit(erros ? 1 : 0);
