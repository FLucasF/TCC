// Teste de aceitacao do enunciado do Strategy (experimento/prompt/prompt.md),
// caixa-preta, pela API. O esperado vem da calculadora de referencia, que
// reproduz os 5 exemplos conferidos do enunciado.
// A unidade e o CASO, como na hipotese C1: passa se todos os campos batem.
// Os campos que falharam aparecem nas linhas FALHA, como diagnostico.
import { calcular } from "./ref-strategy.mjs";
const BASE = process.env.BASE;
let passaram = 0; const falhas = [], observacoes = [];
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
  // fronteiras: cada "ate", "passa de" e "a partir de" do enunciado com o valor exato.
  // Entraram porque os mutantes M8, M10 e M11 passavam pela suite sem eles.
  ["fronteira: motoboy com 5 kg exatos", { itens: [{ nome: "Mochila", precoUnitario: 120.00, quantidade: 2, pesoKg: 2.50 }], modalidadeEntrega: "MOTOBOY", formaPagamento: "PIX", nivelClube: "BRONZE", regiao: "SUL" }],
  ["fronteira: OURO com produtos em 500,00 exatos", { itens: [{ nome: "Jaqueta", precoUnitario: 250.00, quantidade: 2, pesoKg: 1.00 }], modalidadeEntrega: "ECONOMICA", formaPagamento: "PIX", nivelClube: "OURO", regiao: "SUL" }],
  ["fronteira: MENOS50 com produtos em 300,00 exatos", { itens: [{ nome: "Vestido", precoUnitario: 150.00, quantidade: 2, pesoKg: 0.50 }], modalidadeEntrega: "ECONOMICA", cupom: "MENOS50", formaPagamento: "PIX", nivelClube: "BRONZE", regiao: "SUL" }],
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
// Observacoes, nao contam: pontos em que o proprio enunciado se contradiz.
try {
  // os exemplos 1-4 como estao escritos (sem clube e regiao)
  const ex1 = { itens: [cam, ten], modalidadeEntrega: "EXPRESSA", cupom: "BEMVINDO10", formaPagamento: "PIX" };
  let r = await post(ex1);
  observacoes.push("Ex1 literal: " + (r.status === 400 ? `erro ${r.j?.erro} (segue a regra do enunciado estendido)`
    : r.status === 200 && num(r.j?.totalFinal, calcular(ex1, { comImposto: false }).totalFinal) ? "total do exemplo 381,74 (segue o exemplo, trata clube/regiao como opcionais)"
    : `${r.status} ${JSON.stringify(r.j)?.slice(0, 120)}`));
  // o limite do boleto: o passo 5 define "total do pedido" com imposto; a regra do boleto, entre
  // parenteses, sem. Um pedido de 950 + 12% fica de um lado do limite em cada leitura.
  const casaco = { itens: [{ nome: "Casaco", precoUnitario: 950.00, quantidade: 1, pesoKg: 1.00 }], modalidadeEntrega: "RETIRADA_LOJA", formaPagamento: "BOLETO", nivelClube: "BRONZE", regiao: "SUDESTE" };
  r = await post(casaco);
  observacoes.push("boleto 950 + imposto: " + (r.status === 400 && r.j?.erro === "FORMA_PAGAMENTO_INDISPONIVEL" ? "recusa (le o total COM imposto, como o passo 5)"
    : r.status === 200 && num(r.j?.totalFinal, calcular(casaco).totalFinal) ? "aceita (le o total SEM imposto, como o parentese da regra do boleto)"
    : `${r.status} ${JSON.stringify(r.j)?.slice(0, 120)}`));
} catch (err) { observacoes.push("excecao nas observacoes: " + err.message); }
console.log(`RESULTADO: ${passaram} de ${casos.length} casos passaram`);
for (const f of falhas) console.log("  FALHA " + f);
for (const o of observacoes) console.log("  OBSERVACAO " + o);
process.exit(falhas.length ? 1 : 0);
