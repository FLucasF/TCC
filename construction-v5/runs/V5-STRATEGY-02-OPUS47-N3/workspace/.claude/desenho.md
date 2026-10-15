# Desenho do serviço de resumo da compra

## O que muda, o que é igual

Entre os casos de **modalidade de entrega**, **cupom**, **nível do clube** e
**forma de pagamento**, variam tanto a conta quanto a condição de
aplicabilidade/disponibilidade. Entre as **regiões**, só muda a porcentagem
do seguro — a conta é a mesma.

A sequência do cálculo (subtotal → desconto → frete → seguro → total →
ajuste do pagamento) é igual para todo pedido.

## Estrutura

- Cada família com comportamento variado é um `enum` com métodos abstratos.
  Cada caso implementa o seu comportamento no corpo do próprio valor; não
  há `if`/`switch` no serviço escolhendo entre casos.

  - `ModalidadeEntrega`: `frete(pesoKg)`, `prazoDias()`, `atende(pesoKg)`
  - `Cupom`: `aplicavel(ctx)`, `desconto(ctx)`
  - `NivelClube`: `credito(subtotal)`, `fretePago(freteBase)`,
    `temBrinde(subtotal)`
  - `FormaPagamento`: `parcelasValidas(n)`, `disponivel(total)`,
    `calcular(total, parcelas)`

- `Regiao` é `enum` com um campo `aliquotaSeguro`, porque só a porcentagem
  varia; a conta do seguro mora no serviço.

- `CheckoutService` orquestra: valida na ordem exigida, chama cada caso
  pelo nome e monta o resumo. Lookup por `valueOf` com tratamento de
  `IllegalArgumentException` para distinguir "não existe" de "existe mas
  não aplicável".

- `Dinheiro` concentra o arredondamento meio-para-par em centavos
  (`BigDecimal` com `HALF_EVEN`).

## Validação

Erros devolvidos exatamente na ordem fixa do enunciado:
itens → clube → região → modalidade existe → modalidade atende →
cupom existe → cupom aplicável → pagamento existe → parcelas válidas →
pagamento atende. Cada erro é uma `PedidoInvalidoException` com o código;
o controller devolve `{ "erro": "..." }`.

## Ordem interna do cálculo

Internamente, o frete é calculado antes do desconto porque `FRETEGRATIS`
depende do valor do frete. O frete apresentado no resumo é o frete após o
`NivelClube` zerar (OURO não paga), e `FRETEGRATIS` usa esse mesmo valor
exibido. Seguro, crédito e brinde são sobre o subtotal dos produtos (sem
desconto e sem frete), conforme o enunciado.
