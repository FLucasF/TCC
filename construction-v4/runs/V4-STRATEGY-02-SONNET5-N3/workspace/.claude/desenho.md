# Desenho: serviço de resumo de checkout

## O que muda de caso para caso

- **Modalidade de entrega** (ECONOMICA, EXPRESSA, RETIRADA_LOJA, MOTOBOY, e
  futuras): fórmula do frete, prazo em dias e condição de disponibilidade.
  Novas opções entram quase toda semana → cada modalidade é um `case` de um
  enum que implementa os mesmos métodos (`calcularFrete`, `prazoDias`,
  `disponivel`); adicionar modalidade é só adicionar uma constante, sem tocar
  nas outras nem em if/else.

- **Cupom** (BEMVINDO10, MENOS50, FRETEGRATIS, LEVE3PAGUE2): condição de
  aplicabilidade e fórmula de desconto. Mesma ideia: enum com os métodos
  `aplicavel` e `calcularDesconto`, um caso por constante.

- **Nível do clube** (BRONZE, PRATA, OURO, e futuros): percentual de crédito,
  isenção de frete, direito a brinde. Enum com `calcularCredito`,
  `isentaFrete`, `temBrinde`.

- **Forma de pagamento** (PIX, CARTAO, BOLETO): parcelamento permitido,
  disponibilidade por valor, e o ajuste (desconto/tarifa/juros) sobre o total.
  Enum com `parcelaValida`, `disponivel`, `calcular`.

## O que é igual em todos os casos

- Ordem fixa de cálculo (subtotal → desconto cupom → frete → seguro → total
  do pedido → ajuste de pagamento) e ordem fixa de validação com códigos de
  erro — isso é processo do serviço, não varia por caso, fica na classe de
  serviço (`CalculadoraResumoService`).
- Arredondamento "meio para o par" em cada etapa monetária — um único método
  utilitário (`Dinheiro.arredondar`), chamado pelos casos que precisam.
- Seguro por região: só a alíquota muda, a conta é a mesma sempre → não é
  comportamento variante, é dado. `Regiao` é um enum só com o percentual
  (sem métodos de cálculo próprios).

## Por que essa estrutura

Cada eixo de variação (entrega, cupom, clube, pagamento) tem seu próprio
enum-estratégia, resolvido por `fromCodigo(String)` (lookup, não if/else em
cadeia). O serviço injeta subtotal/frete/peso/itens no contexto necessário
para cada estratégia decidir por si (ex.: `CupomContexto` para o cupom
`FRETEGRATIS` saber o valor do frete). Região fica fora desse padrão porque o
enunciado diz explicitamente que não há variação de comportamento ali, só de
valor.
