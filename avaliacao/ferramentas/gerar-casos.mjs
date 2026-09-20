// Gera os arquivos de caso da suíte escondida, calculando os valores esperados
// com aritmética decimal EXATA (BigInt em micros, 1e-6).
//
// Uso:  node avaliacao/ferramentas/gerar-casos.mjs
//
// Ponto flutuante não serve aqui: vários destes casos existem justamente para
// cair em empate de arredondamento, e 0,1125 não é representável em binário. A
// fórmula Price é a única exceção, por ser irracional, e ali há uma trava que
// recusa o caso se o resultado cair perto de um empate.
//
// O gerador SE CONFERE antes de escrever: reproduz os quatro exemplos do
// enunciado e os casos E5 e E6 do gabarito, e aborta sem escrever nada se algum
// divergir. Sem isso, um erro aqui viraria dezenas de valores esperados errados,
// e a suíte reprovaria implementações corretas em silêncio.
//
// Os arquivos gerados NÃO devem ser editados à mão. Mudou regra do enunciado,
// muda aqui e regera.

import { writeFileSync, mkdirSync } from "node:fs";
import { join } from "node:path";

const M = 1_000_000n; // micros
const mic = (x) => {
  const [i, f = ""] = String(x).split(".");
  const frac = (f + "000000").slice(0, 6);
  const sinal = i.startsWith("-") ? -1n : 1n;
  return sinal * (BigInt(i.replace("-", "")) * M + BigInt(frac));
};
const mul = (a, b) => (a * b) / M;

// Arredonda micros para centavos, meio-para-o-par, e devolve micros.
function cent(v) {
  const sinal = v < 0n ? -1n : 1n;
  const a = v < 0n ? -v : v;
  const passo = M / 100n; // 10000 micros = 1 centavo
  const q = a / passo, r = a % passo;
  let c = q;
  if (r * 2n > passo) c = q + 1n;
  else if (r * 2n === passo) c = q % 2n === 0n ? q : q + 1n;
  return sinal * c * passo;
}
const num = (v) => Number(v) / 1e6;
const empate = (v) => {
  const a = v < 0n ? -v : v;
  const passo = M / 100n;
  return (a % passo) * 2n === passo;
};

const FRETE = {
  ECONOMICA: (p) => mic(12) + mul(mic(2), p),
  EXPRESSA: (p) => mic(25) + mul(mic(4.5), p),
  RETIRADA_LOJA: () => 0n,
  MOTOBOY: () => mic(18),
};
const PRAZO = { ECONOMICA: 7, EXPRESSA: 2, RETIRADA_LOJA: 1, MOTOBOY: 0 };
const CUPONS = ["BEMVINDO10", "MENOS50", "FRETEGRATIS", "LEVE3PAGUE2"];
const PAGAMENTOS = ["PIX", "CARTAO", "BOLETO"];

let empatesVistos = 0;

function calcular(req) {
  const itens = req.itens ?? [];
  // 1 · PEDIDO_INVALIDO
  if (!itens.length) return { erro: "PEDIDO_INVALIDO" };
  for (const i of itens) {
    if (i.precoUnitario == null || i.quantidade == null || i.pesoKg == null) return { erro: "PEDIDO_INVALIDO" };
    if (i.precoUnitario <= 0 || i.quantidade <= 0 || i.pesoKg <= 0) return { erro: "PEDIDO_INVALIDO" };
  }
  // 2 · MODALIDADE_INVALIDA
  if (!req.modalidadeEntrega || !FRETE[req.modalidadeEntrega]) return { erro: "MODALIDADE_INVALIDA" };

  const sub = cent(itens.reduce((a, i) => a + mul(mic(i.precoUnitario), mic(i.quantidade)), 0n));
  const peso = itens.reduce((a, i) => a + mul(mic(i.pesoKg), mic(i.quantidade)), 0n); // sem arredondar

  // 3 · MODALIDADE_INDISPONIVEL
  if (req.modalidadeEntrega === "MOTOBOY" && peso > mic(5)) return { erro: "MODALIDADE_INDISPONIVEL" };

  const freteBruto = FRETE[req.modalidadeEntrega](peso);
  if (empate(freteBruto)) empatesVistos++;
  const frete = cent(freteBruto);

  // 4 e 5 · cupom
  let desconto = 0n;
  const cup = req.cupom ?? null;
  if (cup !== null) {
    if (!CUPONS.includes(cup)) return { erro: "CUPOM_INVALIDO" };
    if (cup === "BEMVINDO10") {
      const bruto = mul(sub, mic(0.1));
      if (empate(bruto)) empatesVistos++;
      desconto = cent(bruto);
    }
    if (cup === "MENOS50") {
      if (sub < mic(300)) return { erro: "CUPOM_NAO_APLICAVEL" };
      desconto = mic(50);
    }
    if (cup === "FRETEGRATIS") desconto = frete;
    if (cup === "LEVE3PAGUE2")
      desconto = cent(itens.reduce((a, i) => a + mul(mic(Math.floor(i.quantidade / 3)), mic(i.precoUnitario)), 0n));
  }

  const total = cent(sub - desconto + frete);

  // 6 · FORMA_PAGAMENTO_INVALIDA
  const pag = req.formaPagamento;
  if (!pag || !PAGAMENTOS.includes(pag)) return { erro: "FORMA_PAGAMENTO_INVALIDA" };
  const parcelas = req.parcelas ?? 1;

  // 7 · PARCELAMENTO_INVALIDO
  if (pag === "PIX" || pag === "BOLETO") { if (parcelas !== 1) return { erro: "PARCELAMENTO_INVALIDO" }; }
  else if (parcelas < 1 || parcelas > 12) return { erro: "PARCELAMENTO_INVALIDO" };

  // 8 · FORMA_PAGAMENTO_INDISPONIVEL
  if (pag === "BOLETO" && total > mic(1000)) return { erro: "FORMA_PAGAMENTO_INDISPONIVEL" };

  let ajuste, final, valorParcela;
  if (pag === "PIX") {
    const bruto = mul(total, mic(0.05));
    if (empate(bruto)) empatesVistos++;
    ajuste = -cent(bruto);
    final = cent(total + ajuste);
    valorParcela = final;
  } else if (pag === "BOLETO") {
    ajuste = mic(3.49);
    final = cent(total + ajuste);
    valorParcela = final;
  } else if (parcelas <= 3) {
    ajuste = 0n;
    final = total;
    const bruto = (total * M) / mic(parcelas);
    if (empate(bruto)) empatesVistos++;
    valorParcela = cent(bruto);
  } else {
    // Price. Único trecho em ponto flutuante, com trava contra empate.
    const t = num(total), i = 0.0199;
    const p = (t * i) / (1 - Math.pow(1 + i, -parcelas));
    const cents = p * 100;
    if (Math.abs(cents - Math.floor(cents) - 0.5) < 1e-4)
      throw new Error(`parcela perto de empate em Price: ${p}. Escolha outros valores.`);
    valorParcela = cent(mic(p.toFixed(6)));
    final = cent(valorParcela * mic(parcelas) / M);
    ajuste = cent(final - total);
  }

  return {
    subtotalProdutos: num(sub), descontoCupom: num(desconto), frete: num(frete),
    prazoEntregaDias: PRAZO[req.modalidadeEntrega], ajustePagamento: num(ajuste),
    totalFinal: num(final), parcelas, valorParcela: num(valorParcela),
  };
}

function caso(id, descricao, requisicao) {
  const r = calcular(requisicao);
  if (r.erro) return { id, descricao, requisicao, status_esperado: 400, esperado: { erro: r.erro } };
  return { id, descricao, requisicao, esperado: r };
}

const it = (p, q, kg, nome = "Item") => ({ nome, precoUnitario: p, quantidade: q, pesoKg: kg });

// ------------------------------------------------------------------ entrega
const ENTREGA = [
  caso("ent-economica", "ECONOMICA: 12,00 + 2,00/kg, prazo 7",
    { itens: [it(79.9, 2, 0.3, "Camiseta")], modalidadeEntrega: "ECONOMICA", formaPagamento: "PIX" }),
  caso("ent-expressa", "EXPRESSA: 25,00 + 4,50/kg, prazo 2",
    { itens: [it(79.9, 2, 0.3, "Camiseta")], modalidadeEntrega: "EXPRESSA", formaPagamento: "PIX" }),
  caso("ent-retirada", "RETIRADA_LOJA: gratis, prazo 1",
    { itens: [it(79.9, 2, 0.3, "Camiseta")], modalidadeEntrega: "RETIRADA_LOJA", formaPagamento: "PIX" }),
  caso("ent-motoboy", "MOTOBOY: 18,00 fixo, prazo 0",
    { itens: [it(79.9, 2, 0.3, "Camiseta")], modalidadeEntrega: "MOTOBOY", formaPagamento: "PIX" }),
  caso("ent-motoboy-5kg", "MOTOBOY com 5,00 kg: ate inclui o limite",
    { itens: [it(100, 1, 5.0, "Halter")], modalidadeEntrega: "MOTOBOY", formaPagamento: "PIX" }),
  caso("ent-motoboy-5kg01", "MOTOBOY com 5,01 kg: acima do limite",
    { itens: [it(100, 1, 5.01, "Halter")], modalidadeEntrega: "MOTOBOY", formaPagamento: "PIX" }),
  caso("ent-motoboy-soma-5kg", "MOTOBOY com 5,00 kg somados de varios itens",
    { itens: [it(50, 2, 1.25, "Peso"), it(30, 5, 0.5, "Anilha")], modalidadeEntrega: "MOTOBOY", formaPagamento: "PIX" }),
  caso("ent-peso-decimal", "peso somado NAO arredonda: 3 x 0,333 kg = 0,999 kg",
    { itens: [it(20, 3, 0.333, "Meia")], modalidadeEntrega: "ECONOMICA", formaPagamento: "PIX" }),
  caso("ent-inexistente", "modalidade que nao existe",
    { itens: [it(79.9, 2, 0.3, "Camiseta")], modalidadeEntrega: "SUPERSONICA", formaPagamento: "PIX" }),
];

// ------------------------------------------------------------------ cupons
const CUP = [
  caso("cup-sem", "sem cupom: desconto 0,00",
    { itens: [it(79.9, 2, 0.3, "Camiseta")], modalidadeEntrega: "RETIRADA_LOJA", formaPagamento: "PIX" }),
  caso("cup-bemvindo10", "BEMVINDO10: 10% sobre os produtos",
    { itens: [it(79.9, 2, 0.3, "Camiseta")], modalidadeEntrega: "RETIRADA_LOJA", cupom: "BEMVINDO10", formaPagamento: "PIX" }),
  caso("cup-menos50-29999", "MENOS50 com produtos 299,99: abaixo do minimo",
    { itens: [it(299.99, 1, 0.5, "Tenis")], modalidadeEntrega: "RETIRADA_LOJA", cupom: "MENOS50", formaPagamento: "PIX" }),
  caso("cup-menos50-30000", "MENOS50 com produtos 300,00: a partir de inclui",
    { itens: [it(300, 1, 0.5, "Tenis")], modalidadeEntrega: "RETIRADA_LOJA", cupom: "MENOS50", formaPagamento: "PIX" }),
  caso("cup-fretegratis-economica", "FRETEGRATIS: frete aparece, desconto igual a ele",
    { itens: [it(79.9, 2, 0.3, "Camiseta")], modalidadeEntrega: "ECONOMICA", cupom: "FRETEGRATIS", formaPagamento: "PIX" }),
  caso("cup-fretegratis-motoboy", "FRETEGRATIS com MOTOBOY",
    { itens: [it(79.9, 2, 0.3, "Camiseta")], modalidadeEntrega: "MOTOBOY", cupom: "FRETEGRATIS", formaPagamento: "PIX" }),
  caso("cup-fretegratis-retirada", "FRETEGRATIS com frete ja zero: desconto 0,00",
    { itens: [it(79.9, 2, 0.3, "Camiseta")], modalidadeEntrega: "RETIRADA_LOJA", cupom: "FRETEGRATIS", formaPagamento: "PIX" }),
  caso("cup-leve3-2un", "LEVE3PAGUE2 com 2 unidades: nada de graca",
    { itens: [it(19.9, 2, 0.1, "Meia")], modalidadeEntrega: "RETIRADA_LOJA", cupom: "LEVE3PAGUE2", formaPagamento: "PIX" }),
  caso("cup-leve3-3un", "LEVE3PAGUE2 com 3 unidades: 1 de graca",
    { itens: [it(19.9, 3, 0.1, "Meia")], modalidadeEntrega: "RETIRADA_LOJA", cupom: "LEVE3PAGUE2", formaPagamento: "PIX" }),
  caso("cup-leve3-6un", "LEVE3PAGUE2 com 6 unidades: 2 de graca",
    { itens: [it(19.9, 6, 0.1, "Meia")], modalidadeEntrega: "RETIRADA_LOJA", cupom: "LEVE3PAGUE2", formaPagamento: "PIX" }),
  caso("cup-leve3-7un", "LEVE3PAGUE2 com 7 unidades: ainda 2 de graca",
    { itens: [it(19.9, 7, 0.1, "Meia")], modalidadeEntrega: "RETIRADA_LOJA", cupom: "LEVE3PAGUE2", formaPagamento: "PIX" }),
  caso("cup-leve3-misto", "LEVE3PAGUE2 por item: 3 de um e 2 de outro, so o primeiro conta",
    { itens: [it(19.9, 3, 0.1, "Meia"), it(79.9, 2, 0.3, "Camiseta")], modalidadeEntrega: "RETIRADA_LOJA", cupom: "LEVE3PAGUE2", formaPagamento: "PIX" }),
  caso("cup-minusculo", "codigo em minusculas nao existe",
    { itens: [it(79.9, 2, 0.3, "Camiseta")], modalidadeEntrega: "RETIRADA_LOJA", cupom: "bemvindo10", formaPagamento: "PIX" }),
  caso("cup-inexistente", "cupom que nao existe",
    { itens: [it(79.9, 2, 0.3, "Camiseta")], modalidadeEntrega: "RETIRADA_LOJA", cupom: "NATAL2026", formaPagamento: "PIX" }),
];

// ------------------------------------------------------------------ pagamento
const PAG = [
  caso("pag-cartao-1x", "CARTAO em 1x: sem juros",
    { itens: [it(100, 2, 0.3, "Tenis")], modalidadeEntrega: "RETIRADA_LOJA", formaPagamento: "CARTAO", parcelas: 1 }),
  caso("pag-cartao-3x", "CARTAO em 3x: ainda sem juros",
    { itens: [it(100, 2, 0.3, "Tenis")], modalidadeEntrega: "RETIRADA_LOJA", formaPagamento: "CARTAO", parcelas: 3 }),
  caso("pag-cartao-4x", "CARTAO em 4x: primeira faixa com juros",
    { itens: [it(100, 2, 0.3, "Tenis")], modalidadeEntrega: "RETIRADA_LOJA", formaPagamento: "CARTAO", parcelas: 4 }),
  caso("pag-cartao-12x", "CARTAO em 12x: teto com juros",
    { itens: [it(100, 2, 0.3, "Tenis")], modalidadeEntrega: "RETIRADA_LOJA", formaPagamento: "CARTAO", parcelas: 12 }),
  caso("pag-cartao-13x", "CARTAO em 13x: acima do teto",
    { itens: [it(100, 2, 0.3, "Tenis")], modalidadeEntrega: "RETIRADA_LOJA", formaPagamento: "CARTAO", parcelas: 13 }),
  caso("pag-cartao-0x", "CARTAO em 0x",
    { itens: [it(100, 2, 0.3, "Tenis")], modalidadeEntrega: "RETIRADA_LOJA", formaPagamento: "CARTAO", parcelas: 0 }),
  caso("pag-boleto-1000", "BOLETO com total exatamente 1.000,00: passa de exclui o limite",
    { itens: [it(1000, 1, 0.5, "Notebook")], modalidadeEntrega: "RETIRADA_LOJA", formaPagamento: "BOLETO" }),
  caso("pag-boleto-100001", "BOLETO com total 1.000,01: passa do limite",
    { itens: [it(1000.01, 1, 0.5, "Notebook")], modalidadeEntrega: "RETIRADA_LOJA", formaPagamento: "BOLETO" }),
  caso("pag-pix-2x", "PIX com 2 parcelas: so a vista",
    { itens: [it(100, 1, 0.3, "Tenis")], modalidadeEntrega: "RETIRADA_LOJA", formaPagamento: "PIX", parcelas: 2 }),
  caso("pag-boleto-2x", "BOLETO com 2 parcelas: so a vista",
    { itens: [it(100, 1, 0.3, "Tenis")], modalidadeEntrega: "RETIRADA_LOJA", formaPagamento: "BOLETO", parcelas: 2 }),
  caso("pag-inexistente", "forma de pagamento que nao existe",
    { itens: [it(100, 1, 0.3, "Tenis")], modalidadeEntrega: "RETIRADA_LOJA", formaPagamento: "CRIPTO" }),
];

// ------------------------------------------------------------------ arredondamento
const ARR = [
  caso("arr-pix-20485", "PIX com 5% de 409,70 = 20,485: meio-para-o-par da 20,48, HALF_UP daria 20,49",
    { itens: [it(79.9, 2, 0.3, "Camiseta"), it(249.9, 1, 1.2, "Tenis")], modalidadeEntrega: "EXPRESSA", cupom: "FRETEGRATIS", formaPagamento: "PIX" }),
  caso("arr-percentual", "BEMVINDO10 com 10% de 12,25 = 1,225: empate no desconto",
    { itens: [it(12.25, 1, 0.1, "Chaveiro")], modalidadeEntrega: "RETIRADA_LOJA", cupom: "BEMVINDO10", formaPagamento: "BOLETO" }),
  caso("arr-frete-peso", "ECONOMICA com 0,1125 kg: frete 12,225, empate no frete",
    { itens: [it(100, 1, 0.1125, "Pluma")], modalidadeEntrega: "ECONOMICA", formaPagamento: "BOLETO" }),
  caso("arr-parcela", "CARTAO em 2x com total 100,05: parcela 50,025, empate na parcela",
    { itens: [it(100.05, 1, 0.1, "Livro")], modalidadeEntrega: "RETIRADA_LOJA", formaPagamento: "CARTAO", parcelas: 2 }),
];

// ------------------------------------------------------------------ opcionais e validacao
const OPC = [
  caso("opc-cupom-ausente", "cupom ausente do JSON",
    { itens: [it(79.9, 2, 0.3, "Camiseta")], modalidadeEntrega: "RETIRADA_LOJA", formaPagamento: "PIX" }),
  caso("opc-cupom-null", "cupom explicitamente null",
    { itens: [it(79.9, 2, 0.3, "Camiseta")], modalidadeEntrega: "RETIRADA_LOJA", cupom: null, formaPagamento: "PIX" }),
  caso("opc-parcelas-ausente", "parcelas ausente: considerar 1",
    { itens: [it(100, 2, 0.3, "Tenis")], modalidadeEntrega: "RETIRADA_LOJA", formaPagamento: "CARTAO" }),
  caso("opc-parcelas-1-pix", "PIX com parcelas 1 explicito: valido",
    { itens: [it(100, 2, 0.3, "Tenis")], modalidadeEntrega: "RETIRADA_LOJA", formaPagamento: "PIX", parcelas: 1 }),
  caso("val-carrinho-vazio", "carrinho vazio",
    { itens: [], modalidadeEntrega: "RETIRADA_LOJA", formaPagamento: "PIX" }),
  caso("val-preco-zero", "item com preco zero",
    { itens: [it(0, 1, 0.3, "Brinde")], modalidadeEntrega: "RETIRADA_LOJA", formaPagamento: "PIX" }),
  caso("val-quantidade-zero", "item com quantidade zero",
    { itens: [it(79.9, 0, 0.3, "Camiseta")], modalidadeEntrega: "RETIRADA_LOJA", formaPagamento: "PIX" }),
  caso("val-peso-negativo", "item com peso negativo",
    { itens: [it(79.9, 1, -0.3, "Camiseta")], modalidadeEntrega: "RETIRADA_LOJA", formaPagamento: "PIX" }),
  caso("val-modalidade-ausente", "modalidade ausente",
    { itens: [it(79.9, 1, 0.3, "Camiseta")], formaPagamento: "PIX" }),
  caso("val-pagamento-ausente", "forma de pagamento ausente",
    { itens: [it(79.9, 1, 0.3, "Camiseta")], modalidadeEntrega: "RETIRADA_LOJA" }),
];

// ------------------------------------------------------------- conferencia
// Referencia autoritativa: os quatro exemplos do enunciado e os dois casos
// reservados do gabarito. Se o gerador nao reproduzir estes, ele nao escreve.
const REFERENCIA = [
  ["Exemplo 1 do enunciado",
   { itens: [it(79.90, 2, 0.30, "Camiseta"), it(249.90, 1, 1.20, "Tenis")], modalidadeEntrega: "EXPRESSA", cupom: "BEMVINDO10", formaPagamento: "PIX", parcelas: 1 },
   { subtotalProdutos: 409.70, descontoCupom: 40.97, frete: 33.10, prazoEntregaDias: 2, ajustePagamento: -20.09, totalFinal: 381.74, parcelas: 1, valorParcela: 381.74 }],
  ["Exemplo 2 do enunciado",
   { itens: [it(79.90, 2, 0.30, "Camiseta"), it(249.90, 1, 1.20, "Tenis")], modalidadeEntrega: "ECONOMICA", formaPagamento: "CARTAO", parcelas: 6 },
   { subtotalProdutos: 409.70, descontoCupom: 0, frete: 15.60, prazoEntregaDias: 7, ajustePagamento: 30.10, totalFinal: 455.40, parcelas: 6, valorParcela: 75.90 }],
  ["Exemplo 3 do enunciado",
   { itens: [it(199.90, 2, 0.25, "Fone")], modalidadeEntrega: "MOTOBOY", cupom: "MENOS50", formaPagamento: "BOLETO" },
   { subtotalProdutos: 399.80, descontoCupom: 50, frete: 18, prazoEntregaDias: 0, ajustePagamento: 3.49, totalFinal: 371.29, parcelas: 1, valorParcela: 371.29 }],
  ["Exemplo 4 do enunciado",
   { itens: [it(19.90, 7, 0.10, "Meia"), it(79.90, 2, 0.30, "Camiseta")], modalidadeEntrega: "RETIRADA_LOJA", cupom: "LEVE3PAGUE2", formaPagamento: "CARTAO", parcelas: 3 },
   { subtotalProdutos: 299.10, descontoCupom: 39.80, frete: 0, prazoEntregaDias: 1, ajustePagamento: 0, totalFinal: 259.30, parcelas: 3, valorParcela: 86.43 }],
  ["E5 do gabarito",
   { itens: [it(59.90, 1, 0.50, "Livro")], modalidadeEntrega: "EXPRESSA", cupom: "FRETEGRATIS", formaPagamento: "PIX" },
   { subtotalProdutos: 59.90, descontoCupom: 27.25, frete: 27.25, prazoEntregaDias: 2, ajustePagamento: -3.00, totalFinal: 56.90, parcelas: 1, valorParcela: 56.90 }],
  ["E6 do gabarito",
   { itens: [it(199.90, 2, 0.25, "Fone")], modalidadeEntrega: "MOTOBOY", cupom: "MENOS50", formaPagamento: "CARTAO", parcelas: 12 },
   { subtotalProdutos: 399.80, descontoCupom: 50, frete: 18, prazoEntregaDias: 0, ajustePagamento: 49.32, totalFinal: 417.12, parcelas: 12, valorParcela: 34.76 }],
];

let divergiu = 0;
for (const [nome, req, esperado] of REFERENCIA) {
  const r = calcular(req);
  const dif = Object.entries(esperado).filter(([k, v]) => Math.abs(r[k] - v) > 0.004);
  if (dif.length) {
    divergiu++;
    console.error("  DIVERGE  " + nome + ": " + dif.map(([k, v]) => k + " esperado " + v + ", veio " + r[k]).join(" | "));
  } else console.log("  ok       " + nome);
}
if (divergiu) {
  console.error(String.fromCharCode(10) + divergiu + " caso(s) de referencia divergiram. NADA foi escrito.");
  process.exit(1);
}
console.log("  " + REFERENCIA.length + "/" + REFERENCIA.length + " casos de referencia conferem" + String.fromCharCode(10));

const DESTINO = "avaliacao/casos";
mkdirSync(DESTINO, { recursive: true });
const grupos = { "entrega.json": ENTREGA, "cupons.json": CUP, "pagamento.json": PAG, "arredondamento.json": ARR, "opcionais-validacao.json": OPC };
for (const [arq, casos] of Object.entries(grupos)) {
  writeFileSync(join(DESTINO, arq), JSON.stringify(casos, null, 2) + "\n");
  const erros = casos.filter((c) => c.status_esperado).length;
  console.log(`${arq.padEnd(26)} ${String(casos.length).padStart(2)} casos  (${erros} de erro)`);
}
console.log(`\nempates de arredondamento exercitados: ${empatesVistos}`);
