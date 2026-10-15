# Desenho: resumo de checkout

## O que varia de caso para caso (comportamento próprio)

- **Modalidade de entrega** (ECONOMICA, EXPRESSA, RETIRADA_LOJA, MOTOBOY):
  fórmula de frete, prazo em dias e condição de disponibilidade são
  diferentes em cada uma. Novas modalidades vão surgir com frequência.
  → `enum ModalidadeEntrega` com corpo por constante (frete, prazoDias,
  disponivel). Adicionar uma modalidade nova = adicionar uma constante nova,
  sem tocar em if/else existente.

- **Cupom** (BEMVINDO10, MENOS50, FRETEGRATIS, LEVE3PAGUE2): condição de
  aplicabilidade e cálculo do desconto são diferentes em cada um.
  → `enum Cupom` com corpo por constante (aplicavel, desconto).

- **Nível do clube** (BRONZE, PRATA, OURO): percentual de crédito, isenção
  de frete e brinde variam por nível.
  → `enum NivelClube` com corpo por constante (percentualCredito,
  freteGratis, temBrinde).

- **Forma de pagamento** (PIX, CARTAO, BOLETO): regra de parcelas válidas,
  disponibilidade e cálculo do ajuste/parcela são diferentes.
  → `enum FormaPagamento` com corpo por constante (parcelasValidas,
  disponivel, calcular).

## O que é igual em todos os casos (mesma conta, só o número muda)

- **Seguro por região**: a conta é idêntica (percentual × subtotal de
  produtos, arredondado); só o percentual muda por região. Não é
  comportamento, é dado.
  → `enum Regiao` guardando só o percentual (sem métodos de cálculo).

- Arredondamento: sempre `HALF_EVEN`, escala 2, numa única função utilitária
  usada por todo mundo.

## Estrutura dos pacotes

- `dominio`: `ItemPedido`, `ModalidadeEntrega`, `Cupom`, `NivelClube`,
  `FormaPagamento`, `Regiao`, `Dinheiro` (arredondamento).
- `api`: `ResumoCheckoutRequest`, `ItemRequest`, `ResumoCheckoutResponse`,
  `ErroResponse`, `CheckoutController`.
- `servico`: `CalculoCheckoutService` — orquestra a ordem fixa de cálculo
  (produtos → cupom → frete → seguro → total → pagamento) e as validações,
  na ordem exigida pelo enunciado. A ordem de validação e a sequência das
  etapas do cálculo são fixas pelo enunciado, não variam por caso — por
  isso ficam como uma sequência direta no serviço, e não como estratégia.
- `CalculoCheckoutException`: erro com código, tratado por
  `@ControllerAdvice` devolvendo `{"erro": "CODIGO"}` com HTTP 400.

## Pontos de atenção identificados nos exemplos

- Desconto do FRETEGRATIS é sempre igual ao frete já calculado (mesmo
  quando o frete já é zero por outro motivo, ex. OURO).
- Crédito do clube e seguro usam sempre o subtotal de produtos "cru" (sem
  desconto de cupom, sem frete).
- LEVE3PAGUE2: desconto = soma, por item, de `floor(quantidade/3) *
  precoUnitario`.
- Validação de disponibilidade de forma de pagamento (BOLETO > 1000) usa o
  total do pedido (antes do ajuste de pagamento).
