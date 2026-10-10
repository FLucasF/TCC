# Desenho — serviço de resumo do checkout

## O que muda de caso para caso

- **Entrega** (`ECONOMICA`, `EXPRESSA`, `RETIRADA_LOJA`, `MOTOBOY`, e novas que
  chegam quase toda semana): cada opção tem sua fórmula de custo, seu prazo e
  sua própria regra de disponibilidade (ex.: motoboy só até 5kg).
- **Cupom** (`BEMVINDO10`, `MENOS50`, `FRETEGRATIS`, `LEVE3PAGUE2`): cada um
  tem sua fórmula de desconto e sua própria condição de aplicabilidade.
- **Nível do clube** (`BRONZE`, `PRATA`, `OURO`, e outros em estudo): cada
  nível tem seu próprio crédito, isenção de frete e regra de brinde.
- **Forma de pagamento** (`PIX`, `CARTAO`, `BOLETO`): cada uma tem seu próprio
  ajuste sobre o total, suas parcelas permitidas e sua condição de
  disponibilidade.
- **Região**: só a porcentagem do seguro muda; a conta é sempre a mesma
  (porcentagem × subtotal de produtos). Isso é dado, não comportamento — vira
  uma tabela (`Map`), não uma estratégia.

## O que é igual em todos os casos

- Ordem do cálculo: subtotal → desconto do cupom → frete → seguro → total do
  pedido → ajuste de pagamento → total final.
- Arredondamento: toda etapa em dinheiro usa HALF_EVEN com 2 casas decimais.
- Peso do pedido: soma de peso × quantidade de cada item, sem arredondar.
- Crédito do clube e seguro são sempre sobre o subtotal de produtos (sem
  desconto, sem frete).
- Ordem fixa de validação dos erros (carrinho → clube → região → entrega →
  disponibilidade da entrega → cupom → aplicabilidade do cupom → pagamento →
  parcelas → disponibilidade do pagamento).

## Estrutura escolhida

- `OpcaoEntrega` (interface: `custo`, `prazoDias`, `disponivel`) com uma
  implementação por modalidade, registradas num `Map<String, OpcaoEntrega>`
  no `CatalogoEntregas`. Adicionar uma transportadora nova = nova classe +
  uma linha no mapa, sem tocar nas demais. Isso atende o requisito de que
  entregas novas chegam o tempo todo.
- `Cupom` (interface: `aplicavel`, `desconto`) com uma implementação por
  código, registradas num `Map<String, Cupom>` no `CatalogoCupons`.
- `BeneficioClube` (interface: `credito`, `frete`, `brinde`) com uma
  implementação por nível, registradas num `Map<String, BeneficioClube>` no
  `CatalogoClube`. Mesma lógica de extensão para os níveis futuros em estudo.
- `FormaPagamento` (interface: `ajuste` — devolve total final e parcela —,
  `parcelasPermitidas`, `disponivel`) com uma implementação por forma,
  registradas num `Map<String, FormaPagamento>` no `CatalogoPagamentos`.
- Seguro por região: `Map<String, BigDecimal>` simples (é dado, não
  comportamento).
- `ResumoCompraService` só orquestra: valida na ordem definida, busca a
  estratégia certa em cada mapa (nunca um `if/else` encadeado) e soma os
  valores na ordem do enunciado.
- Dinheiro sempre em `BigDecimal`, arredondado com `RoundingMode.HALF_EVEN`
  em cada etapa, nunca em `double`.
- Erros: uma exceção `PedidoInvalidoException(codigo)` tratada num
  `@RestControllerAdvice` que devolve `{"erro": codigo}` com 422/400.
