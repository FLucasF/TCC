# Desenho: Serviço de Resumo de Checkout

## O que muda de caso para caso

1. **Modalidade de entrega**: cada uma tem custo, prazo e restrições próprias
2. **Cupom**: cada um tem regra de desconto e condições de aplicação diferentes
3. **Nível do clube**: cada um tem vantagens específicas (crédito, frete grátis, brinde)
4. **Forma de pagamento**: cada uma tem ajuste próprio (desconto PIX, tarifa boleto, juros cartão)

## O que é igual em todos os casos

1. Ordem do cálculo: produtos → cupom → frete → seguro → total → ajuste pagamento
2. Arredondamento bankers rounding em cada etapa
3. Cálculo do seguro (só muda a porcentagem por região, mas a conta é sempre: porcentagem sobre subtotal produtos)
4. Sequência de validações

## Estrutura escolhida

**Strategy** para cada dimensão que varia:

- `ModalidadeEntrega`: interface com `calcularFrete()`, `obterPrazo()`, `estaDisponivel()`
  - Implementações: Economica, Expressa, RetiradaLoja, Motoboy
  - Factory escolhe a implementação pelo código da modalidade

- `Cupom`: interface com `calcularDesconto()`, `podeAplicar()`
  - Implementações: BemVindo10, Menos50, FreteGratis, Leve3Pague2
  - Factory escolhe pelo código do cupom

- `NivelClube`: interface com `calcularCredito()`, `temFreteGratis()`, `ganhouBrinde()`
  - Implementações: Bronze, Prata, Ouro
  - Factory escolhe pelo código do nível

- `FormaPagamento`: interface com `calcularAjuste()`, `estaDisponivel()`
  - Implementações: Pix, Boleto, Cartao
  - Factory escolhe pelo código da forma

**Por quê**: cada nova modalidade/cupom/nível/forma entra como classe nova, sem alterar
condições existentes. O `CheckoutService` orquestra chamando as interfaces, sem conhecer
as implementações concretas.

## Observações

- Arredondamento: usar `RoundingMode.HALF_EVEN` do BigDecimal (bankers rounding) em cada etapa
- Seguro: não precisa de Strategy, é sempre o mesmo cálculo (porcentagem × subtotal)
- Validações: no início do `CheckoutService`, na ordem especificada
- Subtotal calculado uma vez e reutilizado para evitar recálculo
- Validação de parcelamento separada da validação de disponibilidade (boleto precisa do total)
