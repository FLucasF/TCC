# Desenho — Serviço de Resumo de Checkout

## O que muda de caso para caso

| Dimensão | O que varia | Frequência de mudança |
|---|---|---|
| **Modalidade de entrega** | Cálculo do frete, prazo, regra de disponibilidade | Alta (quase toda semana) |
| **Cupom** | Cálculo do desconto, condição de aplicabilidade | Alta (marketing inventa promoções) |
| **Nível do clube** | % crédito, frete grátis, brinde | Média (estudando novos níveis) |
| **Forma de pagamento** | Cálculo do ajuste, parcelas permitidas, disponibilidade | Baixa |
| **Região** | Só a porcentagem do seguro (a fórmula é igual) | Baixa |

## O que é igual em todos os casos

- Ordem de cálculo: subtotal → cupom → frete → seguro → total → pagamento.
- Ordem de validação (10 checagens em sequência fixa).
- Arredondamento: HALF_EVEN com 2 casas em cada etapa.
- Seguro: sempre `porcentagem × subtotal` (só a porcentagem varia por região).
- Crédito do clube: sempre `porcentagem × subtotal`.

## Estrutura escolhida

Cada dimensão que varia vira um **enum com métodos abstratos**. Cada constante
do enum encapsula o comportamento daquele caso. Não há `if/else` nem `switch`
para escolher entre os casos — o dispatch é polimórfico.

- `ModalidadeEntrega` — métodos: `calcularFrete(peso)`, `prazoDias()`, `disponivel(peso)`
- `Cupom` — métodos: `calcularDesconto(subtotal, itens, frete)`, `aplicavel(subtotal, itens)`
- `NivelClube` — métodos: `percentualCredito()`, `freteGratis()`, `temBrinde(subtotal)`
- `FormaPagamento` — métodos: `calcular(total, parcelas)`, `parcelamentoValido(parcelas)`, `disponivel(total)`
- `Regiao` — enum simples com campo `percentualSeguro` (fórmula única, só dado muda)

O `CheckoutService` orquestra: valida na ordem do spec, calcula cada parte
delegando ao enum, monta a resposta.

## Por quê

Enum com métodos abstratos: adicionar um caso novo é adicionar uma constante —
o compilador obriga a implementar todos os métodos. Sem `if/else` espalhado.
Região é enum simples porque só o dado muda, não o comportamento.
