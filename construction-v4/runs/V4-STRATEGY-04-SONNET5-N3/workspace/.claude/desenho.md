# Desenho — serviço de resumo de checkout

## O que varia de caso para caso

- **Modalidade de entrega**: fórmula de frete, prazo em dias e condição de
  disponibilidade (ex.: motoboy só até 5 kg) são diferentes por modalidade, e
  a lista de modalidades cresce com frequência (novas transportadoras).
- **Cupom**: fórmula de desconto e condição de aplicabilidade são diferentes
  por cupom (percentual, fixo com piso, frete grátis, leve-3-pague-2).
- **Nível do clube**: percentual de cashback, isenção de frete e regra de
  brinde são diferentes por nível, e a loja já avisa que vai criar mais
  níveis com seus próprios conjuntos de vantagens.
- **Forma de pagamento**: ajuste sobre o total (desconto, tarifa, juros),
  parcelamento permitido e disponibilidade (limite de valor do boleto) são
  diferentes por forma de pagamento.

## O que é igual em todos os casos

- **Região**: só a porcentagem do seguro muda; a conta (porcentagem sobre o
  subtotal de produtos, arredondada) é idêntica em todas as regiões. Não é
  comportamento variando, é só um número — fica um campo de enum, sem método.
- Arredondamento: toda etapa em dinheiro usa HALF_EVEN com 2 casas.
- Ordem de cálculo e ordem de validação dos erros são fixas e descritas no
  enunciado, iguais para qualquer pedido.

## Estrutura escolhida

Cada "caso que varia" vira um enum cujas constantes implementam os métodos do
próprio enum (um corpo por constante) — assim o comportamento de cada caso
mora só nele, e escolher entre eles é `enum.valueOf(...)` + despacho
polimórfico, nunca uma sequência de if/else ou switch por tipo:

- `ModalidadeEntrega { ECONOMICA, EXPRESSA, RETIRADA_LOJA, MOTOBOY }`
  - `BigDecimal frete(BigDecimal pesoKg)`
  - `int prazoDias()`
  - `boolean disponivel(BigDecimal pesoKg)`
- `Cupom { BEMVINDO10, MENOS50, FRETEGRATIS, LEVE3PAGUE2 }`
  - `boolean aplicavel(List<ItemRequest> itens, BigDecimal subtotalProdutos)`
  - `BigDecimal desconto(List<ItemRequest> itens, BigDecimal subtotalProdutos, BigDecimal frete)`
  (assinatura cobre o caso mais exigente, LEVE3PAGUE2, que precisa dos itens)
- `NivelClube { BRONZE, PRATA, OURO }`
  - `BigDecimal credito(BigDecimal subtotalProdutos)`
  - `boolean isentaFrete()`
  - `boolean temBrinde(BigDecimal subtotalProdutos)`
- `FormaPagamento { PIX, CARTAO, BOLETO }`
  - `boolean parcelasValidas(int parcelas)`
  - `boolean disponivel(BigDecimal totalPedido)`
  - `BigDecimal totalFinal(BigDecimal totalPedido, int parcelas)`
  - `BigDecimal parcela(BigDecimal totalPedido, BigDecimal totalFinal, int parcelas)`
- `Regiao { SUDESTE, SUL, CENTRO_OESTE, NORTE, NORDESTE }` com campo
  `percentualSeguro`, sem métodos (não há comportamento variando, só valor).

A validação de erros (10 situações, em ordem) é uma lista de verificações
executadas em sequência no `ResumoService`, cada uma devolvendo o primeiro
código de erro encontrado — é a própria natureza do requisito (checar "nesta
ordem"), não uma escolha de caso por condição.

`ResumoService` monta o cálculo chamando os métodos de cada enum na ordem do
enunciado (produtos → cupom → frete → seguro → total → pagamento) e devolve
o DTO de resposta. Erros de negócio são sinalizados por `CheckoutException`
com o código, traduzida pelo `GlobalExceptionHandler` para `{"erro":...}`.
