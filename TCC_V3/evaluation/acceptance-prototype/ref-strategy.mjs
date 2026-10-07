// Calculadora de referencia do enunciado do Strategy (experiment/prompt/prompt.md).
// Dinheiro em centavos inteiros; arredondamento meio-para-o-par em cada etapa.
// O P5 e o seguro do envio por regiao (no enunciado da bancada, ate 06/10, era um imposto).
export const PCT_SEGURO = { SUDESTE: 100, SUL: 100, CENTRO_OESTE: 150, NORTE: 250, NORDESTE: 200 }; // em pontos-base

// (valorCentavos * pontosBase / 10000), arredondado meio-para-o-par
export function pct(c, bp) {
  const num = BigInt(c) * BigInt(bp), den = 10000n;
  let q = num / den, r = num % den;
  if (r * 2n > den || (r * 2n === den && q % 2n === 1n)) q += 1n;
  return Number(q);
}
// arredonda um numero real (em reais) para centavos, meio-para-o-par
export function centavos(x) {
  const v = x * 100, f = Math.floor(v), d = v - f;
  if (Math.abs(d - 0.5) < 1e-7) return f % 2 === 0 ? f : f + 1;
  return Math.round(v);
}

export function calcular(p) {
  const itens = p.itens || [];
  if (!itens.length || itens.some((i) => !(i.precoUnitario > 0) || !(i.quantidade > 0) || !(i.pesoKg > 0))) return { erro: "PEDIDO_INVALIDO" };
  if (!["BRONZE", "PRATA", "OURO"].includes(p.nivelClube)) return { erro: "NIVEL_CLUBE_INVALIDO" };
  if (!Object.hasOwn(PCT_SEGURO, p.regiao ?? "")) return { erro: "REGIAO_INVALIDA" };
  if (!["ECONOMICA", "EXPRESSA", "RETIRADA_LOJA", "MOTOBOY"].includes(p.modalidadeEntrega)) return { erro: "MODALIDADE_INVALIDA" };
  const peso = itens.reduce((s, i) => s + i.pesoKg * i.quantidade, 0);
  if (p.modalidadeEntrega === "MOTOBOY" && peso > 5) return { erro: "MODALIDADE_INDISPONIVEL" };
  const cupom = p.cupom ?? null;
  if (cupom !== null && !["BEMVINDO10", "MENOS50", "FRETEGRATIS", "LEVE3PAGUE2"].includes(cupom)) return { erro: "CUPOM_INVALIDO" };
  const subtotal = itens.reduce((s, i) => s + centavos(i.precoUnitario * i.quantidade), 0);
  if (cupom === "MENOS50" && subtotal < 30000) return { erro: "CUPOM_NAO_APLICAVEL" };
  if (!["PIX", "CARTAO", "BOLETO"].includes(p.formaPagamento)) return { erro: "FORMA_PAGAMENTO_INVALIDA" };
  const n = p.parcelas ?? 1;
  if (p.formaPagamento === "CARTAO" ? !(n >= 1 && n <= 12) : n !== 1) return { erro: "PARCELAMENTO_INVALIDO" };

  let frete = { ECONOMICA: 1200 + centavos(2 * peso), EXPRESSA: 2500 + centavos(4.5 * peso), RETIRADA_LOJA: 0, MOTOBOY: 1800 }[p.modalidadeEntrega];
  const prazo = { ECONOMICA: 7, EXPRESSA: 2, RETIRADA_LOJA: 1, MOTOBOY: 0 }[p.modalidadeEntrega];
  if (p.nivelClube === "OURO") frete = 0;
  let desconto = 0;
  if (cupom === "BEMVINDO10") desconto = pct(subtotal, 1000);
  if (cupom === "MENOS50") desconto = 5000;
  if (cupom === "FRETEGRATIS") desconto = frete;
  if (cupom === "LEVE3PAGUE2") desconto = itens.reduce((s, i) => s + centavos(Math.floor(i.quantidade / 3) * i.precoUnitario), 0);
  const seguro = pct(subtotal, PCT_SEGURO[p.regiao]);
  const totalPedido = subtotal - desconto + frete + seguro;
  if (p.formaPagamento === "BOLETO" && totalPedido > 100000) return { erro: "FORMA_PAGAMENTO_INDISPONIVEL" };

  let totalFinal, parcela;
  if (p.formaPagamento === "PIX") { totalFinal = totalPedido - pct(totalPedido, 500); parcela = totalFinal; }
  if (p.formaPagamento === "BOLETO") { totalFinal = totalPedido + 349; parcela = totalFinal; }
  if (p.formaPagamento === "CARTAO") {
    if (n <= 3) { totalFinal = totalPedido; parcela = centavos(totalPedido / 100 / n); }
    else { const i = 0.0199; parcela = centavos((totalPedido / 100) * i / (1 - Math.pow(1 + i, -n))); totalFinal = parcela * n; }
  }
  return {
    subtotalProdutos: subtotal, descontoCupom: desconto, frete, prazoEntregaDias: prazo, seguro,
    ajustePagamento: totalFinal - totalPedido, totalFinal, parcelas: n, valorParcela: parcela,
    creditoProximaCompra: p.nivelClube === "PRATA" ? pct(subtotal, 200) : p.nivelClube === "OURO" ? pct(subtotal, 500) : 0,
    brinde: p.nivelClube === "OURO" && subtotal > 50000,
  };
}
