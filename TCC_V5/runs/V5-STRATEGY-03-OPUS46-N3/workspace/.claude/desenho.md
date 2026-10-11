# Desenho — Resumo do Checkout

## O que muda de caso para caso

- **Modalidade de entrega**: cada uma tem sua fórmula de frete, prazo e regra de disponibilidade.
- **Cupom**: cada cupom tem lógica própria de desconto e condição de aplicabilidade.
- **Nível do clube**: cada nível tem seu conjunto de vantagens (crédito, frete grátis, brinde).
- **Forma de pagamento**: cada uma tem seu ajuste (desconto, tarifa, juros) e suas restrições.

## O que é igual em todos os casos

- A ordem de cálculo: subtotal → frete → cupom → seguro → total → ajuste pagamento.
- O arredondamento: HALF_EVEN para 2 casas em cada etapa.
- O seguro: sempre porcentagem sobre subtotal bruto; só a taxa muda por região (não o comportamento) → `enum Regiao` com a taxa.
- A validação: ordem fixa de checagem, retornando o primeiro erro.

## Estrutura escolhida

Cada dimensão que varia (entrega, cupom, clube, pagamento) é uma interface com uma implementação por caso, registrada como bean Spring. O serviço recebe as listas de implementações e monta mapas código→implementação para despacho direto, sem cadeia de ifs.

Regiões usam enum simples com a taxa de seguro, já que só a porcentagem muda e a conta é idêntica.
