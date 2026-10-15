# Desenho — serviço de resumo da compra

## O que varia de caso para caso (e cresce com o tempo)

- **Entrega**: fórmula de custo, prazo em dias e condição de disponibilidade
  mudam por modalidade (hoje 4, mas "quase toda semana entra uma nova").
  → interface `ModalidadeEntrega` (custo, prazoDias, disponivel), uma
  implementação por modalidade, cada uma informando seu próprio `codigo()`.
  O serviço monta um `Map<codigo, ModalidadeEntrega>` a partir da lista de
  beans do Spring — escolher a modalidade é uma busca no mapa, não uma
  cadeia de if/else.

- **Cupom**: condição de aplicabilidade e fórmula de desconto mudam por
  cupom (fixos por ora, mas cada um com regra própria: percentual, fixo com
  piso, frete grátis, N-por-M).
  → interface `Cupom` (codigo, aplicavel, desconto), uma implementação por
  cupom, resolvidas via `Map<codigo, Cupom>`.

- **Nível do clube**: percentual de crédito, isenção de frete e brinde
  mudam por nível (hoje 3, "estudando criar mais").
  → interface `NivelClube` (codigo, credito, freteGratis, brinde), uma
  implementação por nível, resolvida via `Map<codigo, NivelClube>`.

- **Forma de pagamento**: parcelas permitidas, disponibilidade e o ajuste
  sobre o total (desconto Pix, tarifa Boleto, juros Price no Cartão) mudam
  por forma de pagamento.
  → interface `FormaPagamento` (codigo, parcelasPermitidas, disponivel,
  calcular→valorFinal/valorParcela), uma implementação por forma, resolvida
  via `Map<codigo, FormaPagamento>`.

## O que é igual em todos os casos

- Ordem fixa do cálculo: subtotal → desconto cupom → frete → seguro →
  total do pedido → ajuste de pagamento → total final.
- Arredondamento "meio para o par" em cada etapa monetária (um único
  `Arredondamento.paraCentavos`).
- Seguro: é sempre `percentual(região) × subtotalProdutos`, só a região
  muda a alíquota — por isso região é só um `Map<regiao, BigDecimal>` de
  alíquotas, não merece interface própria (o enunciado não descreve a
  fórmula do seguro como variando por região, só a alíquota).
  (`AliquotaSeguro` — enum-like simples.)
- Validação: sequência fixa de 10 checagens, nessa ordem, cada uma delega
  para o mapa/estratégia correspondente para saber se existe/é aplicável.
  Isso é uma sequência de validações **determinada pelo enunciado**, não
  uma escolha de comportamento — por isso é código sequencial comum no
  serviço, não polimorfismo.

## Estrutura de pacotes

```
checkout/
  dto/           ItemRequest, ResumoRequest, ResumoResponse, ErroResponse
  erro/          PedidoInvalidoException (carrega o código) + handler
  comum/         Arredondamento
  entrega/       ModalidadeEntrega + 4 impls
  cupom/         Cupom + 4 impls
  clube/         NivelClube + 3 impls
  pagamento/     FormaPagamento + 3 impls
  seguro/        AliquotaSeguro
  ResumoService  orquestra a sequência fixa acima
  CheckoutController
```

Cada estratégia é um `@Component` com `codigo()`; o `ResumoService` recebe
`List<ModalidadeEntrega>`, `List<Cupom>`, `List<NivelClube>`,
`List<FormaPagamento>` do Spring e monta os mapas no construtor.
