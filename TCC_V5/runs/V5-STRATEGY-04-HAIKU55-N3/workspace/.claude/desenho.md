# Desenho: serviço de resumo de checkout

## O que varia caso a caso
- **Modalidade de entrega**: custo do frete, prazo, se atende o peso do pedido.
- **Cupom**: se é aplicável ao pedido e como calcula o desconto.
- **Nível do clube**: crédito, se paga frete, se ganha brinde.
- **Forma de pagamento**: quais parcelamentos aceita, se está disponível para o total, e o ajuste sobre o total.

## O que é igual em todos os casos
- Ordem de cálculo: subtotal, cupom, frete, seguro, total, pagamento.
- Arredondamento "meio para o par" em cada etapa.
- Ordem de validação e códigos de erro.
- Formato de entrada e saída JSON.
- Percentual do seguro por região: é só dado, não comportamento.

## Estrutura
- Cada variação é um `enum` com corpo por constante (uma constante por caso). O comportamento de cada caso fica no próprio caso. A escolha é feita por busca pelo código (`Optional`), sem sequência de `if`.
- Métodos com comportamento padrão (ex.: `disponivel` = true, `aplicavel` = true) ficam no enum; só os casos que diferem sobrescrevem.
- `CalculadoraResumo` (serviço) tem a sequência fixa: validações na ordem da tabela de erros, depois o cálculo.
- `Pedido` e `ItemPedido` guardam os dados e calculam subtotal e peso.
- Assinaturas escolhidas para o caso mais exigente:
  - `Cupom.aplicavel(Pedido)` e `Cupom.desconto(Pedido, frete)`: FRETEGRATIS precisa do frete e LEVE3PAGUE2 precisa dos itens.
  - `NivelClube.frete(valor)`: o nível decide o frete cobrado.
  - `FormaPagamento.calcular(total, parcelas)` devolve `Pagamento`.
- Entrada do controller recebe códigos como `String`, para que código desconhecido vire o erro de negócio correto, e não erro de desserialização.

## Premissas (não estavam no enunciado)
- Erros respondem HTTP 400 com corpo `{ "erro": "CODIGO" }`.
- `cupom` vazio ou ausente = sem cupom; `parcelas` ausente = 1.
- Cartão: a divisão "até 3x sem juros" fica dentro do caso CARTAO (é a regra de parcelas, não uma escolha entre formas de pagamento).
- Limite do boleto (R$ 1.000,00) e limite do motoboy (5 kg) são inclusivos.
- Brinde: produtos estritamente acima de R$ 500,00.
- Cartão: até 3x sem juros; de 4x a 12x com 1,99% a.m. pela tabela Price.
