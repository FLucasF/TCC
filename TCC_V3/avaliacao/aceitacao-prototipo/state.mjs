// Teste de aceitacao do enunciado do State (experimento/prompt/state.md),
// caixa-preta, pela API. Casos = os 8 exemplos conferidos + textos + erros.
const BASE = process.env.BASE;
let ok = 0, falhas = [];
const req = async (metodo, caminho, corpo) => {
  const r = await fetch(BASE + caminho, {
    method: metodo,
    headers: corpo ? { "Content-Type": "application/json" } : {},
    body: corpo ? JSON.stringify(corpo) : undefined,
  });
  let j = null; try { j = await r.json(); } catch {}
  return { status: r.status, j };
};
const confere = (nome, cond, det) => { if (cond) ok++; else falhas.push(`${nome}: ${det}`); };
const num = (a, b) => typeof a === "number" && Math.abs(a - b) < 0.001;
const TEXTO = {
  AGUARDANDO_PAGAMENTO: "Aguardando pagamento", PAGO: "Pagamento confirmado",
  EM_SEPARACAO: "Separando seus produtos", ENVIADO: "A caminho", ENTREGUE: "Entregue",
  CANCELADO: "Cancelado", DEVOLVIDO: "Devolvido",
};
async function fluxo(nome, prod, frete, acoes) {
  const c = await req("POST", "/pedidos", { valorProdutos: prod, frete });
  confere(`${nome} criar`, c.status === 201, `status ${c.status}`);
  if (!c.j || c.j.id == null) { falhas.push(`${nome}: sem id na criacao`); return null; }
  confere(`${nome} situacao inicial`, c.j.situacao === "AGUARDANDO_PAGAMENTO", c.j.situacao);
  let ult = c;
  for (const a of acoes) {
    ult = await req("POST", `/pedidos/${c.j.id}/acoes`, { acao: a });
    if (ult.status !== 200) { falhas.push(`${nome} ${a}: status ${ult.status} ${JSON.stringify(ult.j)}`); return { id: c.j.id, falhou: true }; }
    confere(`${nome} texto apos ${a}`, ult.j.descricao === TEXTO[ult.j.situacao], `${ult.j.situacao} -> "${ult.j.descricao}"`);
  }
  return { id: c.j.id, p: ult.j };
}
function espera(nome, p, e) {
  if (!p) return;
  for (const [k, v] of Object.entries(e)) {
    const got = p[k];
    const bate = typeof v === "number" ? num(got, v) : JSON.stringify(got) === JSON.stringify(v);
    confere(`${nome} ${k}`, bate, `esperado ${JSON.stringify(v)}, veio ${JSON.stringify(got)}`);
  }
}
try {
  let r;
  r = await fluxo("Ex1", 200.00, 20.00, ["PAGAR", "SEPARAR", "CANCELAR"]);
  espera("Ex1", r?.p, { situacao: "CANCELADO", valorTotal: 220, valorReembolsado: 205, estoqueDevolvido: true, coletaAgendada: false,
    historico: ["AGUARDANDO_PAGAMENTO", "PAGO", "EM_SEPARACAO", "CANCELADO"] });
  r = await fluxo("Ex2", 150.00, 12.50, ["PAGAR", "SEPARAR", "ENVIAR", "ENTREGAR", "DEVOLVER"]);
  espera("Ex2", r?.p, { situacao: "DEVOLVIDO", valorTotal: 162.5, valorReembolsado: 150, estoqueDevolvido: false, coletaAgendada: true });
  r = await fluxo("Ex3", 80.00, 0.00, ["CANCELAR"]);
  espera("Ex3", r?.p, { situacao: "CANCELADO", valorReembolsado: 0, estoqueDevolvido: false, coletaAgendada: false });
  r = await fluxo("Ex4", 250.00, 25.00, ["PAGAR", "CANCELAR"]);
  espera("Ex4", r?.p, { situacao: "CANCELADO", valorReembolsado: 275, estoqueDevolvido: false });
  r = await fluxo("Ex5", 10.00, 3.00, ["PAGAR", "SEPARAR", "CANCELAR"]);
  espera("Ex5", r?.p, { situacao: "CANCELADO", valorTotal: 13, valorReembolsado: 0, estoqueDevolvido: true });
  // Ex6: ENVIAR sem separar
  r = await fluxo("Ex6", 99.90, 15.00, ["PAGAR"]);
  if (r?.id != null) {
    const e = await req("POST", `/pedidos/${r.id}/acoes`, { acao: "ENVIAR" });
    confere("Ex6 status", e.status === 409, `status ${e.status}`);
    confere("Ex6 erro", e.j?.erro === "ACAO_NAO_PERMITIDA", JSON.stringify(e.j));
    const g = await req("GET", `/pedidos/${r.id}`);
    confere("Ex6 continua PAGO", g.j?.situacao === "PAGO", g.j?.situacao);
  }
  // Ex7: ENVIADO + CANCELAR
  r = await fluxo("Ex7", 50.00, 10.00, ["PAGAR", "SEPARAR", "ENVIAR"]);
  if (r?.id != null && !r.falhou) {
    const e = await req("POST", `/pedidos/${r.id}/acoes`, { acao: "CANCELAR" });
    confere("Ex7 status", e.status === 409, `status ${e.status}`);
    confere("Ex7 erro", e.j?.erro === "ACAO_NAO_PERMITIDA", JSON.stringify(e.j));
    const g = await req("GET", `/pedidos/${r.id}`);
    confere("Ex7 continua ENVIADO", g.j?.situacao === "ENVIADO", g.j?.situacao);
  }
  // Ex8: acao inexistente
  r = await fluxo("Ex8", 30.00, 5.00, []);
  if (r?.id != null) {
    const e = await req("POST", `/pedidos/${r.id}/acoes`, { acao: "TROCAR" });
    confere("Ex8 status", e.status === 400, `status ${e.status}`);
    confere("Ex8 erro", e.j?.erro === "ACAO_INVALIDA", JSON.stringify(e.j));
  }
  // Erros a mais: pedido inexistente, criacao invalida, precedencia (inexistente + acao invalida)
  let e = await req("GET", `/pedidos/nao-existe-999`);
  confere("GET inexistente", e.status === 404 && e.j?.erro === "PEDIDO_NAO_ENCONTRADO", `${e.status} ${JSON.stringify(e.j)}`);
  e = await req("POST", `/pedidos/nao-existe-999/acoes`, { acao: "TROCAR" });
  confere("precedencia: inexistente antes de acao invalida", e.status === 404 && e.j?.erro === "PEDIDO_NAO_ENCONTRADO", `${e.status} ${JSON.stringify(e.j)}`);
  e = await req("POST", "/pedidos", { valorProdutos: 0, frete: 10 });
  confere("criar com produtos zero", e.status === 400 && e.j?.erro === "PEDIDO_INVALIDO", `${e.status} ${JSON.stringify(e.j)}`);
  e = await req("POST", "/pedidos", { valorProdutos: 10, frete: -1 });
  confere("criar com frete negativo", e.status === 400 && e.j?.erro === "PEDIDO_INVALIDO", `${e.status} ${JSON.stringify(e.j)}`);
} catch (err) {
  falhas.push("excecao no teste: " + err.message);
}
console.log(`RESULTADO: ${ok} de ${ok + falhas.length} verificacoes passaram`);
for (const f of falhas) console.log("  FALHA " + f);
process.exit(falhas.length ? 1 : 0);
