# Desenho — serviço de resumo do checkout

## O que muda de caso para caso (o enunciado diz que varia)

- **Modalidade de entrega**: cada uma tem sua própria fórmula de frete, seu prazo
  e suas restrições ("quase toda semana entra uma opção nova").
- **Cupom**: cada código tem sua própria regra de desconto e sua própria condição
  de uso ("o pessoal do marketing adora inventar promoção").
- **Nível do clube**: cada nível tem seu próprio conjunto de vantagens — crédito,
  isenção de frete, brinde ("estamos estudando criar mais níveis").
- **Forma de pagamento**: cada uma tem seu ajuste sobre o total, suas parcelas
  permitidas e suas restrições.

## O que é igual em todos os casos

- A ordem do cálculo (produtos → cupom → frete → seguro → total → pagamento).
- O arredondamento: centavos, meio para o par, em cada etapa.
- O **seguro**: o enunciado diz explicitamente que "só a porcentagem muda, a conta
  é a mesma". Então região **não** é estratégia: é um enum que carrega a alíquota.
- O crédito do clube e o seguro incidem sempre sobre o subtotal de produtos.

## Estrutura escolhida

Uma interface por eixo que varia, uma implementação por caso, e um catálogo
(`Map<String, T>`) que resolve o código vindo do JSON. A escolha do caso é um
`get` no mapa, nunca uma cadeia de `if`/`switch` sobre o código.

- `entrega.ModalidadeEntrega` → `Economica`, `Expressa`, `RetiradaLoja`, `Motoboy`
- `cupom.Cupom` → `Bemvindo10`, `Menos50`, `FreteGratis`, `Leve3Pague2`
- `clube.NivelClube` → `Bronze`, `Prata`, `Ouro`
- `pagamento.FormaPagamento` → `Pix`, `Cartao`, `Boleto`
- `Regiao` (enum com alíquota), `Dinheiro` (arredondamento), `Pedido`/`Item`

Cada interface tem o par **atende/calcula**, porque o enunciado separa "não
existe" (código não está no catálogo, e aí o erro vem do próprio catálogo) de
"existe mas não atende o pedido" — `atende(...)`/`aplicavelA(...)`, que respondem
só sim ou não, porque cada eixo tem um único código de indisponibilidade.

`Carrinho` é a porta de entrada dos itens: valida o que o site mandou e devolve
um `Pedido`, para que o resto do cálculo nunca veja campo ausente ou negativo.

## Assinaturas decididas olhando todos os casos

- `ModalidadeEntrega.frete(Pedido)` e `atende(Pedido)` — o Motoboy precisa do
  peso do pedido para cobrar nada além do limite e para dizer que não atende;
  então as duas recebem o pedido. `prazoDias()` é fixo por modalidade.
- `Cupom.desconto(Pedido, BigDecimal frete)` — FRETEGRATIS exige o frete, e
  LEVE3PAGUE2 exige os itens individuais; então a assinatura mais exigente é
  pedido + frete. Por isso o **frete é calculado antes do desconto** internamente,
  mesmo que no resumo o cupom apareça antes.
- `FormaPagamento.cobrar(BigDecimal total, int parcelas)` → `(totalFinal,
  valorParcela)`; o ajuste é `totalFinal - total`, calculado fora, igual para todos.
- `NivelClube`: `creditoProximaCompra(subtotal)`, `freteCobrado(frete)`,
  `temBrinde(subtotal)`. O frete entra e sai do nível (em vez de um `isentaFrete()`
  lido de fora) para que a vantagem do OURO more dentro do OURO — e para que um
  nível futuro possa cobrar meio frete sem mexer no cálculo.

## Erros

`ErroPedido` (exceção com código) + validação na ordem exata do enunciado, num
único lugar (`CalculadoraResumo`), com os códigos de "não existe" vindos do
catálogo e os de "não atende" vindos do próprio caso. Os campos de enum chegam
como `String` no DTO justamente para distinguir ausente/inexistente do válido.
