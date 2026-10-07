// Teste de aceitacao do enunciado do Strategy (experiment/prompt/prompt.md),
// caixa-preta, pela API. O esperado vem da calculadora de referencia, que
// reproduz os 5 exemplos conferidos e a resposta de exemplo do anexo.
// A unidade e o CASO, como na hipotese da correcao: passa se todos os campos batem.
// Os campos que falharam aparecem nas linhas FALHA, como diagnostico.
// Escrita para o enunciado do V4, que corrigiu as contradicoes do da bancada (06/10)
// e trocou o imposto pelo seguro por regiao (07/10); por isso nao ha observacoes, so casos.
import { calcular } from "./ref-strategy.mjs";
const BASE = process.env.BASE;
let passaram = 0; const falhas = [];
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
  // os exemplos do enunciado, como estao escritos
  ["Ex1", { itens: [cam, ten], modalidadeEntrega: "EXPRESSA", cupom: "BEMVINDO10", formaPagamento: "PIX", nivelClube: "BRONZE", regiao: "NORTE" }],
  ["Ex2", { itens: [cam, ten], modalidadeEntrega: "ECONOMICA", formaPagamento: "CARTAO", parcelas: 6, nivelClube: "PRATA", regiao: "CENTRO_OESTE" }],
  ["Ex3", { itens: [fone], modalidadeEntrega: "MOTOBOY", cupom: "MENOS50", formaPagamento: "BOLETO", nivelClube: "BRONZE", regiao: "NORDESTE" }],
  ["Ex4", { itens: [meia, cam], modalidadeEntrega: "RETIRADA_LOJA", cupom: "LEVE3PAGUE2", formaPagamento: "CARTAO", parcelas: 3, nivelClube: "PRATA", regiao: "SUL" }],
  ["Ex5", { itens: [cam, ten], modalidadeEntrega: "EXPRESSA", formaPagamento: "PIX", nivelClube: "OURO", regiao: "SUDESTE" }],
  ["anexo: a resposta de exemplo", { itens: [cam, ten], modalidadeEntrega: "EXPRESSA", cupom: "BEMVINDO10", formaPagamento: "PIX", parcelas: 1, nivelClube: "OURO", regiao: "SUDESTE" }],
  ["OURO + FRETEGRATIS + cartao 10x", { itens: [bota], modalidadeEntrega: "EXPRESSA", cupom: "FRETEGRATIS", formaPagamento: "CARTAO", parcelas: 10, nivelClube: "OURO", regiao: "SUL" }],
  ["FRETEGRATIS sem OURO", { itens: [cam], modalidadeEntrega: "ECONOMICA", cupom: "FRETEGRATIS", formaPagamento: "PIX", nivelClube: "PRATA", regiao: "SUDESTE" }],
  ["erro: motoboy acima de 5 kg", { itens: [bota, bota], modalidadeEntrega: "MOTOBOY", formaPagamento: "PIX", nivelClube: "BRONZE", regiao: "SUL" }],
  ["erro: MENOS50 abaixo de 300", { itens: [cam], modalidadeEntrega: "ECONOMICA", cupom: "MENOS50", formaPagamento: "PIX", nivelClube: "BRONZE", regiao: "SUL" }],
  ["erro: boleto acima de 1000", { itens: [bota, bota], modalidadeEntrega: "ECONOMICA", formaPagamento: "BOLETO", nivelClube: "BRONZE", regiao: "SUL" }],
  ["erro: pix parcelado", { itens: [cam], modalidadeEntrega: "ECONOMICA", formaPagamento: "PIX", parcelas: 2, nivelClube: "BRONZE", regiao: "SUL" }],
  // o limite do boleto e sobre o total do pedido, com o seguro: 990 + 2,5% passa de 1000
  ["boleto: o seguro entra no limite", { itens: [{ nome: "Casaco", precoUnitario: 990.00, quantidade: 1, pesoKg: 1.00 }], modalidadeEntrega: "RETIRADA_LOJA", formaPagamento: "BOLETO", nivelClube: "BRONZE", regiao: "NORTE" }],
  ["precedencia: nivel antes de regiao", { itens: [cam], modalidadeEntrega: "ECONOMICA", formaPagamento: "PIX", nivelClube: "DIAMANTE", regiao: "MARTE" }],
  ["precedencia: regiao antes de modalidade", { itens: [cam], modalidadeEntrega: "DRONE", formaPagamento: "PIX", nivelClube: "BRONZE", regiao: "MARTE" }],
  ["precedencia: cupom antes de pagamento", { itens: [cam], modalidadeEntrega: "ECONOMICA", cupom: "XPTO", formaPagamento: "CHEQUE", nivelClube: "BRONZE", regiao: "SUL" }],
  ["precedencia: pedido invalido primeiro", { itens: [], modalidadeEntrega: "DRONE", formaPagamento: "CHEQUE", nivelClube: "X", regiao: "Y" }],
  // fronteiras: cada "ate", "passa de" e "a partir de" do enunciado com o valor exato.
  // Entraram porque os mutantes MUT8, MUT10, MUT11 e MUT17 passavam pela suite sem eles.
  ["fronteira: motoboy com 5 kg exatos", { itens: [{ nome: "Mochila", precoUnitario: 120.00, quantidade: 2, pesoKg: 2.50 }], modalidadeEntrega: "MOTOBOY", formaPagamento: "PIX", nivelClube: "BRONZE", regiao: "SUL" }],
  ["fronteira: OURO com produtos em 500,00 exatos", { itens: [{ nome: "Jaqueta", precoUnitario: 250.00, quantidade: 2, pesoKg: 1.00 }], modalidadeEntrega: "ECONOMICA", formaPagamento: "PIX", nivelClube: "OURO", regiao: "SUL" }],
  ["fronteira: MENOS50 com produtos em 300,00 exatos", { itens: [{ nome: "Vestido", precoUnitario: 150.00, quantidade: 2, pesoKg: 0.50 }], modalidadeEntrega: "ECONOMICA", cupom: "MENOS50", formaPagamento: "PIX", nivelClube: "BRONZE", regiao: "SUL" }],
  // 960,00 em produtos + 30,40 de frete + 9,60 de seguro = 1000,00 exatos: o boleto vale
  ["fronteira: boleto com 1000,00 exatos", { itens: [{ nome: "Casaco", precoUnitario: 480.00, quantidade: 2, pesoKg: 4.60 }], modalidadeEntrega: "ECONOMICA", formaPagamento: "BOLETO", nivelClube: "BRONZE", regiao: "SUDESTE" }],
];
const num = (a, b) => typeof a === "number" && Math.abs(a - b / 100) < 0.001;
for (const [nome, corpo] of casos) {
  const antes = falhas.length;
  try {
    const esp = calcular(corpo), r = await post(corpo);
    if (esp.erro) {
      if (!(r.status === 400 && r.j?.erro === esp.erro)) falhas.push(`${nome}: esperado 400 ${esp.erro}, veio ${r.status} ${JSON.stringify(r.j)}`);
    } else if (r.status !== 200) {
      falhas.push(`${nome}: esperado 200, veio ${r.status} ${JSON.stringify(r.j)}`);
    } else {
      for (const [k, v] of Object.entries(esp)) {
        const got = r.j?.[k];
        const inteiro = k === "prazoEntregaDias" || k === "parcelas";
        const bate = typeof v === "boolean" ? got === v : inteiro ? got === v : num(got, v);
        if (!bate) falhas.push(`${nome} ${k}: esperado ${inteiro || typeof v === "boolean" ? v : (v / 100).toFixed(2)}, veio ${JSON.stringify(got)}`);
      }
    }
  } catch (err) { falhas.push(`${nome}: excecao no teste: ${err.message}`); }
  if (falhas.length === antes) passaram++;
}
console.log(`RESULTADO: ${passaram} de ${casos.length} casos passaram`);
for (const f of falhas) console.log("  FALHA " + f);
process.exit(falhas.length ? 1 : 0);
