// Teste de aceitacao do enunciado do Strategy (experimento/prompt/prompt.md),
// caixa-preta, pela API. O esperado vem da calculadora de referencia, que
// reproduz os 5 exemplos conferidos do enunciado.
import { calcular } from "./ref-strategy.mjs";
const BASE = process.env.BASE;
let ok = 0; const falhas = [], observacoes = [];
const post = async (corpo) => {
  const r = await fetch(BASE + "/checkout/resumo", { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(corpo) });
  let j = null; try { j = await r.json(); } catch {}
  return { status: r.status, j };
};
const cam = { nome: "Camiseta", precoUnitario: 79.90, quantidade: 2, pesoKg: 0.30 };
const ten = { nome: "Tenis", precoUnitario: 249.90, quantidade: 1, pesoKg: 1.20 };
const fone = { nome: "Fone", precoUnitario: 199.90, quantidade: 2, pesoKg: 0.25 };
const meia = { nome: "Meia", precoUnitario: 19.90, quantidade: 7, pesoKg: 0.10 };
const bota = { nome: "Bota", precoUnitario: 349.90, quantidade: 2, pesoKg: 2.10 };
const casos = [
  ["Ex5 literal", { itens: [cam, ten], modalidadeEntrega: "EXPRESSA", formaPagamento: "PIX", nivelClube: "OURO", regiao: "SUDESTE" }],
  ["Ex1 + BRONZE/NORTE", { itens: [cam, ten], modalidadeEntrega: "EXPRESSA", cupom: "BEMVINDO10", formaPagamento: "PIX", nivelClube: "BRONZE", regiao: "NORTE" }],
  ["Ex2 + PRATA/SUL", { itens: [cam, ten], modalidadeEntrega: "ECONOMICA", formaPagamento: "CARTAO", parcelas: 6, nivelClube: "PRATA", regiao: "SUL" }],
  ["Ex3 + BRONZE/NORDESTE", { itens: [fone], modalidadeEntrega: "MOTOBOY", cupom: "MENOS50", formaPagamento: "BOLETO", nivelClube: "BRONZE", regiao: "NORDESTE" }],
  ["Ex4 + PRATA/CENTRO_OESTE", { itens: [meia, cam], modalidadeEntrega: "RETIRADA_LOJA", cupom: "LEVE3PAGUE2", formaPagamento: "CARTAO", parcelas: 3, nivelClube: "PRATA", regiao: "CENTRO_OESTE" }],
  ["OURO + FRETEGRATIS + cartao 10x", { itens: [bota], modalidadeEntrega: "EXPRESSA", cupom: "FRETEGRATIS", formaPagamento: "CARTAO", parcelas: 10, nivelClube: "OURO", regiao: "SUL" }],
  ["FRETEGRATIS sem OURO", { itens: [cam], modalidadeEntrega: "ECONOMICA", cupom: "FRETEGRATIS", formaPagamento: "PIX", nivelClube: "PRATA", regiao: "SUDESTE" }],
  ["erro: motoboy acima de 5 kg", { itens: [bota, bota], modalidadeEntrega: "MOTOBOY", formaPagamento: "PIX", nivelClube: "BRONZE", regiao: "SUL" }],
  ["erro: MENOS50 abaixo de 300", { itens: [cam], modalidadeEntrega: "ECONOMICA", cupom: "MENOS50", formaPagamento: "PIX", nivelClube: "BRONZE", regiao: "SUL" }],
  ["erro: boleto acima de 1000", { itens: [bota, bota], modalidadeEntrega: "ECONOMICA", formaPagamento: "BOLETO", nivelClube: "BRONZE", regiao: "SUL" }],
  ["erro: pix parcelado", { itens: [cam], modalidadeEntrega: "ECONOMICA", formaPagamento: "PIX", parcelas: 2, nivelClube: "BRONZE", regiao: "SUL" }],
  ["precedencia: nivel antes de regiao", { itens: [cam], modalidadeEntrega: "ECONOMICA", formaPagamento: "PIX", nivelClube: "DIAMANTE", regiao: "MARTE" }],
  ["precedencia: regiao antes de modalidade", { itens: [cam], modalidadeEntrega: "DRONE", formaPagamento: "PIX", nivelClube: "BRONZE", regiao: "MARTE" }],
  ["precedencia: cupom antes de pagamento", { itens: [cam], modalidadeEntrega: "ECONOMICA", cupom: "XPTO", formaPagamento: "CHEQUE", nivelClube: "BRONZE", regiao: "SUL" }],
  ["precedencia: pedido invalido primeiro", { itens: [], modalidadeEntrega: "DRONE", formaPagamento: "CHEQUE", nivelClube: "X", regiao: "Y" }],
];
const num = (a, b) => typeof a === "number" && Math.abs(a - b / 100) < 0.001;
try {
  for (const [nome, corpo] of casos) {
    const esp = calcular(corpo), r = await post(corpo);
    if (esp.erro) {
      const bate = r.status === 400 && r.j?.erro === esp.erro;
      if (bate) ok++; else falhas.push(`${nome}: esperado 400 ${esp.erro}, veio ${r.status} ${JSON.stringify(r.j)}`);
      continue;
    }
    if (r.status !== 200) { falhas.push(`${nome}: esperado 200, veio ${r.status} ${JSON.stringify(r.j)}`); continue; }
    for (const [k, v] of Object.entries(esp)) {
      const got = r.j?.[k];
      const inteiro = k === "prazoEntregaDias" || k === "parcelas";
      const bate = typeof v === "boolean" ? got === v : inteiro ? got === v : num(got, v);
      if (bate) ok++; else falhas.push(`${nome} ${k}: esperado ${inteiro || typeof v === "boolean" ? v : (v / 100).toFixed(2)}, veio ${JSON.stringify(got)}`);
    }
  }
  // Observacao, nao conta: os exemplos 1-4 como estao escritos (sem clube e regiao)
  for (const [nome, corpo] of [["Ex1 literal", { itens: [cam, ten], modalidadeEntrega: "EXPRESSA", cupom: "BEMVINDO10", formaPagamento: "PIX" }]]) {
    const r = await post(corpo);
    const piloto = calcular(corpo, { comImposto: false });
    const como = r.status === 400 ? `erro ${r.j?.erro} (segue a regra do enunciado estendido)`
      : r.status === 200 && num(r.j?.totalFinal, piloto.totalFinal) ? "total do exemplo 381,74 (segue o exemplo, trata clube/regiao como opcionais)"
      : `${r.status} ${JSON.stringify(r.j)?.slice(0, 120)}`;
    observacoes.push(`${nome}: ${como}`);
  }
} catch (err) { falhas.push("excecao no teste: " + err.message); }
console.log(`RESULTADO: ${ok} de ${ok + falhas.length} verificacoes passaram`);
for (const f of falhas) console.log("  FALHA " + f);
for (const o of observacoes) console.log("  OBSERVACAO " + o);
process.exit(falhas.length ? 1 : 0);
