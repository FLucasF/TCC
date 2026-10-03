// Mutantes da calculadora de referencia (ref-strategy.mjs): cada um planta UM erro.
// Servem para provar que a suite (strategy.mjs) REPROVA codigo errado, e nao so
// que aprova o certo. Quem roda: validar-mutantes.mjs.
//
// Criterio para entrar: um mutante por regra do enunciado que tem armadilha.
// Erro grosseiro, que qualquer caso pega (aliquota trocada, tarifa esquecida),
// nao entra: nao diz nada sobre a suite.
//
// Cada troca [de, para] precisa aparecer exatamente uma vez no ref-strategy.mjs.
export const MUTANTES = [
  { id: "M1", regra: "FRETEGRATIS: o frete aparece normalmente e o desconto fica igual ao frete", origem: "erro observado: Haiku HARNESS, TESTE-STRATEGY-01",
    trocas: [['if (cupom === "FRETEGRATIS") desconto = frete;', 'if (cupom === "FRETEGRATIS") { frete = 0; desconto = 0; }']] },
  { id: "M2", regra: "FRETEGRATIS vale para qualquer nivel do clube", origem: "erro observado: Haiku CONTROL, TESTE-STRATEGY-01",
    trocas: [['if (cupom === "FRETEGRATIS") desconto = frete;', 'if (cupom === "FRETEGRATIS") desconto = 0;']] },
  { id: "M3", regra: "arredondamento meio-para-o-par em cada etapa", origem: "armadilha do enunciado",
    trocas: [["if (r * 2n > den || (r * 2n === den && q % 2n === 1n)) q += 1n;", "if (r * 2n >= den) q += 1n;"],
             ["return f % 2 === 0 ? f : f + 1;", "return f + 1;"]] },
  { id: "M4", regra: "imposto sobre os produtos ja com o desconto do cupom", origem: "armadilha do enunciado (o anexo erra isso)",
    trocas: [["pct(subtotal - desconto, PCT_REGIAO[p.regiao])", "pct(subtotal, PCT_REGIAO[p.regiao])"]] },
  { id: "M5", regra: "precedencia dos erros: regiao antes de modalidade", origem: "erro observado: Haiku HARNESS, TESTE-STRATEGY-01",
    trocas: [['    if (!(p.regiao in PCT_REGIAO)) return { erro: "REGIAO_INVALIDA" };\n', ""],
             ["  const peso = itens", '  if (comImposto && !(p.regiao in PCT_REGIAO)) return { erro: "REGIAO_INVALIDA" };\n  const peso = itens']] },
  { id: "M6", regra: "cartao: ate 3x sem juros", origem: "fronteira",
    trocas: [["if (n <= 3)", "if (n < 3)"]] },
  // M7 (limite do boleto contando o imposto) SAIU em 03/10: o enunciado define "total do
  // pedido" com imposto no passo 5 e sem na regra do boleto, entao essa e uma leitura
  // valida, nao um erro. O caso virou observacao no strategy.mjs. Os ids nao mudam.
  { id: "M8", regra: "motoboy leva pedidos de ate 5 kg", origem: "fronteira",
    trocas: [["peso > 5", "peso >= 5"]] },
  { id: "M9", regra: "LEVE3PAGUE2: a cada 3 unidades de um mesmo item, uma sai de graca", origem: "armadilha do enunciado",
    trocas: [["desconto = itens.reduce((s, i) => s + centavos(Math.floor(i.quantidade / 3) * i.precoUnitario), 0);",
              "{ const u = itens.flatMap((i) => Array(i.quantidade).fill(i.precoUnitario)).sort((a, b) => a - b); desconto = u.slice(0, Math.floor(u.length / 3)).reduce((s, x) => s + centavos(x), 0); }"]] },
  { id: "M10", regra: "brinde do OURO se os produtos passarem de R$ 500", origem: "fronteira",
    trocas: [["subtotal > 50000", "subtotal >= 50000"]] },
  { id: "M11", regra: "MENOS50 so para compras a partir de R$ 300 em produtos", origem: "fronteira",
    trocas: [["subtotal < 30000", "subtotal <= 30000"]] },
  { id: "M12", regra: "credito do clube sobre os produtos sem desconto e sem frete", origem: "armadilha do enunciado",
    trocas: [['p.nivelClube === "PRATA" ? pct(subtotal, 200) : p.nivelClube === "OURO" ? pct(subtotal, 500) : 0',
              'p.nivelClube === "PRATA" ? pct(subtotal - desconto, 200) : p.nivelClube === "OURO" ? pct(subtotal - desconto, 500) : 0']] },
  { id: "M13", regra: "peso do pedido sem arredondar", origem: "armadilha do enunciado",
    trocas: [["const peso = itens.reduce((s, i) => s + i.pesoKg * i.quantidade, 0);",
              "const peso = Math.ceil(itens.reduce((s, i) => s + i.pesoKg * i.quantidade, 0));"]] },
  { id: "M14", regra: "cartao com juros: parcela arredondada, e o final e parcela x numero de parcelas", origem: "armadilha do enunciado",
    trocas: [["totalFinal = parcela * n;", "totalFinal = centavos((totalPedido / 100) * i / (1 - Math.pow(1 + i, -n)) * n);"]] },
  { id: "M15", regra: "Pix: 5% de desconto no total do pedido", origem: "armadilha do enunciado",
    trocas: [["totalFinal = totalPedido - pct(totalPedido, 500);", "totalFinal = totalPedido - pct(subtotal, 500);"]] },
  { id: "M16", regra: "colisao OURO + FRETEGRATIS: o OURO zera o frete, e o desconto do cupom fica igual a esse frete zerado", origem: "colisao citada na hipotese C3",
    trocas: [['  if (comImposto && p.nivelClube === "OURO") frete = 0;', '  const freteOriginal = frete;\n  if (comImposto && p.nivelClube === "OURO") frete = 0;'],
             ['if (cupom === "FRETEGRATIS") desconto = frete;', 'if (cupom === "FRETEGRATIS") desconto = freteOriginal;']] },
];
