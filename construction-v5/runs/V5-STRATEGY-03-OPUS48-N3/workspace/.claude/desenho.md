# Desenho — serviço de resumo do checkout

## O que muda de caso para caso (cada um no seu lugar, sem cadeia de `if`)

- **Modalidade de entrega**: fórmula do frete, prazo e disponibilidade mudam por
  opção. Enunciado diz que entram opções novas quase toda semana → `enum
  ModalidadeEntrega` com métodos (`frete(peso)`, `prazoDias()`, `disponivel(peso)`),
  cada caso no seu bloco. Seleção por `valueOf`.
- **Cupom**: regra de desconto e condição de uso mudam por cupom. → `enum Cupom`
  com `desconto(ctx)` e `aplicavel(ctx)`. Contexto carrega o caso mais exigente
  (subtotal p/ MENOS50, frete p/ FRETEGRATIS, itens p/ LEVE3PAGUE2).
- **Forma de pagamento**: ajuste, parcelas permitidas e disponibilidade mudam. →
  `enum FormaPagamento` com `parcelasValidas(n)`, `disponivel(total)`,
  `calcula(total, n) -> (totalFinal, valorParcela)`.

## O que é igual (só o valor muda → dado, não comportamento)

- **Seguro**: "só a porcentagem muda, a conta é a mesma" → `enum Regiao` com um
  campo percentual. Conta única no serviço: `round(subtotal * pct)`.
- **Clube**: crédito é sempre `pct * subtotal`; frete grátis é um liga/desliga;
  brinde é "subtotal acima de um limite". → `enum NivelClube` com campos
  (`creditoPct`, `isentaFrete`, `limiteBrinde` nulo = nunca). Sem estratégia
  elaborada porque hoje só variam valores/flags.

## Assinatura pensada pelo caso mais exigente

- `Cupom.desconto(ContextoCupom)` onde o contexto tem subtotal, frete e itens.
- `FormaPagamento.calcula` devolve `ResultadoPagamento(totalFinal, valorParcela)`
  porque cartão parcelado precisa dos dois separados.

## Fluxo e ordem (erros devolvem o 1º problema, na ordem 1..10)

1 itens → 2 clube → 3 região → 4 modalidade → 5 modalidade disponível (peso) →
6 cupom existe → 7 cupom aplicável → 8 pagamento existe → 9 parcelas válidas →
calcula subtotal/frete(+clube)/desconto/seguro/totalPedido →
10 pagamento disponível (boleto ≤ 1000) → ajuste/parcela/crédito/brinde.

Frete é calculado já com o clube (OURO zera) antes do cupom, porque FRETEGRATIS
usa o frete que aparece no resumo.

Erro = `PedidoInvalidoException(codigo)` capturada no controller → `{erro}`.

## Dinheiro

`BigDecimal` em tudo (entrada já vem como número → BigDecimal, sem `double`),
arredondando a cada etapa com `HALF_EVEN` em 2 casas. Peso não arredonda. Juros
Price usa `Math.pow` (double) só no cálculo da parcela, que é então arredondada.
