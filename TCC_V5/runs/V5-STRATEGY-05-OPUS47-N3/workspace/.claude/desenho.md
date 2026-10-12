# Desenho — resumo de checkout

## O que muda de caso para caso
- **Modalidade de entrega** (ECONOMICA, EXPRESSA, RETIRADA_LOJA, MOTOBOY):
  cada uma tem fórmula de custo, prazo e limite de peso próprios. O enunciado
  diz que entram opções novas quase toda semana.
- **Cupom** (BEMVINDO10, MENOS50, FRETEGRATIS, LEVE3PAGUE2): cada um tem sua
  condição de aplicabilidade e seu cálculo de desconto.
- **Nível do clube** (BRONZE, PRATA, OURO): cada um define se paga frete,
  quanto de crédito gera e se dá brinde. O enunciado diz que novos níveis
  virão.
- **Forma de pagamento** (PIX, CARTAO, BOLETO): cada uma define parcelas
  válidas, se atende o pedido, e como calcula ajuste/valor final/parcela.

## O que é igual em todos os casos
- Ordem do cálculo: produtos → desconto → frete → seguro → total pedido → ajuste.
- Arredondamento meio-para-par a cada etapa que produz dinheiro.
- Fórmula do seguro: percentual × subtotal; só o percentual muda por região.
- Ordem fixa de validação e seus códigos de erro.

## Estrutura escolhida
- Cada família que varia por caso é um `enum` com métodos abstratos: cada
  constante carrega seu próprio comportamento num só lugar; a escolha entre
  casos é `Enum.valueOf`, não cadeia de condições. Facilita acrescentar
  variantes (entrega, nível) só adicionando constante.
- `Regiao` é enum com um campo `percentual`, porque o enunciado diz que
  "é só a porcentagem que muda, a conta é a mesma".
- `CalculadoraResumo` orquestra a sequência de validações e cálculos. A
  ordem dos erros é inerente à especificação, então vive aqui.
- `Dinheiro.cent(x)` centraliza o arredondamento HALF_EVEN para 2 casas.
- Erros de cálculo viajam como `ErroCalculo(codigo)`; o controller traduz
  em `{ "erro": codigo }`.

## Assinaturas
- `Modalidade.custo(peso) / prazoDias() / atende(peso)`
- `Cupom.aplicavel(itens, subtotal, frete) / desconto(itens, subtotal, frete)`
  (frete entra porque FRETEGRATIS depende dele)
- `NivelClube.credito(subtotal) / pagaFrete() / brinde(subtotal)`
- `FormaPagamento.parcelasValidas(n) / atende(total) / calcular(total, n)`
  retornando `(ajuste, totalFinal, valorParcela)`.
- `Regiao.seguro(subtotal)`.
