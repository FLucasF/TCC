# Desenho

## O que muda de caso para caso

- **Entrega** (modalidade): fórmula do frete, prazo, limite (motoboy ≤ 5 kg).
  Vai entrar modalidade nova toda semana.
- **Cupom**: condição de aplicabilidade e cálculo do desconto.
- **Nível do clube**: % de crédito, se zera frete, se manda brinde.
- **Pagamento**: regra de parcelas, se tem limite de total, cálculo do ajuste
  (desconto Pix, tarifa boleto, juros Price do cartão).
- **Região**: % do seguro (só o número muda; a conta é a mesma).

## O que é igual em todos os casos

- Ordem fixa das etapas: subtotal → cupom → frete → seguro → total do
  pedido → ajuste do pagamento → total final.
- Toda etapa em dinheiro arredonda para centavos com HALF_EVEN.
- A ordem das validações é fixa; devolve o primeiro erro.
- Seguro é sempre `% × subtotal`, arredondado — só o % varia.
- Crédito do clube é sempre `% × subtotal`, arredondado — só o % varia
  (Bronze = 0 %).

## Estrutura escolhida

- **Entrega, Cupom, Nivel, Pagamento**: interface por família + uma classe
  por caso. Cada caso guarda o próprio comportamento num lugar só. Um
  registro (`Entregas`, `Cupons`, `Niveis`, `Pagamentos`) mapeia nome →
  implementação. Entra modalidade/cupom/nível novo adicionando classe e
  registrando — nada de encadeamento de `if`.
- **Regiao**: enum com o percentual, porque só o número varia.
- **CalculadoraResumo**: orquestra a ordem fixa das etapas; delega às
  famílias o que varia. É o único lugar que decide a ordem.
- **Erro**: exceção `ErroPedido` carrega o código; o controller a converte
  em `{"erro": "..."}`.
- **Dinheiro**: helper único com o arredondamento HALF_EVEN, pra não
  espalhar `setScale` pelo código.

## Assinaturas

- `Entrega.frete(pesoKg) : BigDecimal`, `prazoDias() : int`,
  `disponivelPara(pesoKg) : boolean`.
- `Cupom.aplicavel(itens, subtotal) : boolean`,
  `desconto(itens, subtotal, frete) : BigDecimal` (FRETEGRATIS usa `frete`,
  LEVE3PAGUE2 usa `itens`, os outros usam só `subtotal`).
- `Nivel.credito(subtotal)`, `ajustarFrete(frete)`, `brinde(subtotal)`.
- `Pagamento.parcelasValidas(n)`, `disponivelPara(total)`,
  `calcular(total, parcelas) : Ajuste`.

A assinatura foi escolhida olhando o caso mais exigente (LEVE3PAGUE2 precisa
dos itens; FRETEGRATIS precisa do frete; cartão parcelado precisa do número
de parcelas).
