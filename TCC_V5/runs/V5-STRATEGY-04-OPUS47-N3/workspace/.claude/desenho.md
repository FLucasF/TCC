# Desenho — serviço `/checkout/resumo`

## O que muda de caso para caso
- **Modalidade de entrega**: fórmula do frete, prazo em dias, se atende o peso do pedido.
- **Cupom**: regra de desconto e condição de aplicabilidade.
- **Nível de clube**: percentual do crédito, se há frete grátis, condição do brinde.
- **Forma de pagamento**: cálculo do ajuste, parcelas permitidas, condição de disponibilidade.
- **Região**: apenas o percentual do seguro.

## O que é igual em todos os casos
- Ordem das etapas de cálculo (subtotal → cupom → frete → seguro → total pedido → ajuste pagamento).
- Fórmula do seguro: percentual da região × subtotal, arredondado.
- Arredondamento: `BigDecimal` com `HALF_EVEN` em 2 casas, aplicado em cada etapa.
- Sequência fixa das 10 validações, cada uma devolvendo o primeiro código de erro encontrado.

## Estrutura escolhida
Cada família variável vira um `enum`, e cada constante guarda o comportamento daquele caso em métodos próprios (sobrescritos). A seleção é por `valueOf`, sem cascata de `if`/`switch` espalhada pelo serviço.

- `Entrega` (enum): `valorFrete(peso)`, `prazoDias()`, `atende(peso)`.
- `Cupom` (enum): `desconto(subtotal, frete, itens)`, `aplicavel(subtotal)`.
- `NivelClube` (enum): `credito(subtotal)`, `freteGratis()`, `brinde(subtotal)`.
- `FormaPagamento` (enum): `ajuste(totalPedido, parcelas)`, `parcelasValidas(parcelas)`, `atende(totalPedido)`.
- `Regiao` (enum): percentual próprio e método `seguro(subtotal)` que aplica a fórmula comum (percentual × subtotal, arredondado).

`CheckoutServico` só sequencia as validações e as etapas de cálculo, delegando cada decisão específica ao enum correspondente. Entrada e saída são DTOs separados do domínio; o controller mapeia erro de domínio (`ErroCheckout`) em `{ "erro": CODIGO }`.

Por quê: cada caso vive num lugar só (a constante do enum). Adicionar uma nova modalidade/cupom/nível/forma é incluir uma constante nova com os métodos próprios — o serviço não muda.
