# Desenho — Checkout Service

## O que muda de caso para caso

- **Modalidade de entrega**: cada uma tem cálculo de frete, prazo e regra de disponibilidade diferentes.
- **Cupom**: cada um tem regra de aplicabilidade e fórmula de desconto diferentes.
- **Forma de pagamento**: cada uma tem validação de parcelas, disponibilidade e cálculo do total diferentes.
- **Nível do clube**: crédito gerado, frete grátis e elegibilidade para brinde variam por nível.
- **Região**: taxa de seguro varia.

## O que é igual em todos os casos

- Fluxo de validação e cálculo do checkout (ordem fixa de 18 passos).
- Estrutura de erro (CheckoutException + ErroCheckout).
- Endpoint REST POST /checkout/resumo.
- Arredondamento HALF_EVEN com 2 casas.

## Estrutura escolhida

- **Interfaces** para os conceitos variáveis (ModalidadeEntrega, Cupom, FormaPagamento).
- **Enums com métodos abstratos** para NivelClube e Regiao (comportamento por valor, sem switch/if).
- **Registros (@Component com Map)** para lookup por string sem cadeia de condições.
- **CheckoutServico** orquestra o fluxo em ordem determinística, delegando para as abstrações.

Não há estrutura para variação imaginada: sem generics desnecessários, sem factory adicional, sem hierarquia além do especificado.
