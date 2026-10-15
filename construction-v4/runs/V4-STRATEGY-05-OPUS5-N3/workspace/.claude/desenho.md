# Desenho — serviço de resumo do checkout

## O que muda de caso para caso

| Eixo | O que varia | Casos de hoje |
|---|---|---|
| Modalidade de entrega | cálculo do frete, prazo, se atende o pedido | ECONOMICA, EXPRESSA, RETIRADA_LOJA, MOTOBOY |
| Cupom | cálculo do desconto e condição de uso | BEMVINDO10, MENOS50, FRETEGRATIS, LEVE3PAGUE2 |
| Nível do clube | % de crédito, frete grátis, brinde | BRONZE, PRATA, OURO |
| Forma de pagamento | ajuste no total, parcelas permitidas, se atende o pedido | PIX, CARTAO, BOLETO |
| Região | **só a porcentagem** do seguro | SUDESTE, SUL, CENTRO_OESTE, NORTE, NORDESTE |

## O que é igual em todos os casos

- A ordem do cálculo (produtos → cupom → frete → seguro → total → pagamento).
- A ordem das validações (1 a 10) e o formato da resposta/erro.
- O arredondamento: centavos, HALF_EVEN, em cada etapa.
- A conta do seguro e a do crédito do clube: porcentagem sobre o subtotal.
- O peso do pedido (soma de peso × quantidade, sem arredondar).

## Estrutura escolhida

Os quatro eixos em que **o comportamento** varia viram uma interface cada, com
uma implementação por caso (comportamento de cada caso mora só nele):

- `ModalidadeEntrega`: `atende(pesoKg)`, `frete(pesoKg)`, `prazoDias()`
- `Cupom`: `aplicavel(ctx)`, `desconto(ctx)` — `ContextoCupom(itens, subtotal, frete)`
  é a assinatura que atende o caso mais exigente (LEVE3PAGUE2 precisa dos itens,
  FRETEGRATIS do frete, MENOS50 do subtotal)
- `NivelClube`: `credito(subtotal)`, `frete(freteCalculado)`, `brinde(subtotal)`
- `FormaPagamento`: `aceitaParcelas(n)`, `atende(total)`, `cobrar(total, n)`
  devolvendo `(totalFinal, valorParcela)` — atende cartão com e sem juros, Pix e boleto

A escolha do caso é **lookup num registro** (`Map<String, T>` imutável por eixo),
nunca cadeia de condições; código ausente/desconhecido devolve o erro do eixo.
Adicionar uma transportadora, cupom ou nível novo = uma classe nova + uma linha
no registro, sem tocar no resto.

A região varia **só no número**, então é um `enum` com a alíquota (`Regiao`) e a
conta do seguro fica num lugar só — nada de estrutura extra para ela.

`ResumoCheckoutService` orquestra: valida na ordem, chama cada eixo e monta o
resumo. Erros são `ErroCheckout` (enum de códigos) lançados em
`CheckoutException` e traduzidos para `{"erro": "CODIGO"}` pelo handler.

`Dinheiro` concentra o arredondamento (HALF_EVEN, 2 casas); como todo valor do
resumo sai de `arredonda`, o JSON já sai com 2 casas pelo `scale` do `BigDecimal`. O DTO de entrada recebe os eixos como `String` e os números como
wrappers, para distinguir "ausente/desconhecido" de valor válido.

## Decisoes que o enunciado nao fixa

- O frete que o cupom ve e o frete depois do clube, entao OURO + FRETEGRATIS da
  desconto 0,00 (o frete ja estava zerado; nao se desconta duas vezes).
- LEVE3PAGUE2 com nenhum item em trio e aplicavel, com desconto 0,00: o
  enunciado nao manda recusar.
- Pedido recusado responde HTTP 400 com `{"erro": "CODIGO"}`; corpo ilegivel
  (JSON quebrado) tambem cai em `PEDIDO_INVALIDO`, para o site receber sempre o
  mesmo formato.
