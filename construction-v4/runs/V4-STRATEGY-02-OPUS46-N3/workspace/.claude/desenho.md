# Desenho — Resumo de compra

## O que muda de caso para caso

- **Entrega**: cada modalidade tem cálculo de frete, prazo e regras de disponibilidade próprios. Novas modalidades surgem toda semana.
- **Cupom**: cada cupom tem lógica de desconto e condições de aplicabilidade próprias. Marketing cria novos com frequência.
- **Nível do clube**: cada nível tem benefícios diferentes (crédito, frete grátis, brinde). Novos níveis planejados.
- **Forma de pagamento**: cada forma tem ajuste próprio sobre o total (desconto, taxa, juros) e restrições.
- **Região**: só a taxa do seguro muda; a fórmula é a mesma em todas.

## O que é igual

- A ordem do cálculo: subtotal → frete → cupom → seguro → total → ajuste pagamento (frete antes do cupom porque FRETEGRATIS precisa do valor do frete).
- A fórmula do seguro (taxa × subtotal).
- O arredondamento HALF_EVEN em toda etapa monetária.
- A sequência de validação (10 erros em ordem fixa).
- O formato de entrada/saída.

## Estrutura escolhida

- **Interface + implementações Spring (@Component)** para entrega, cupom, clube e pagamento. Cada caso vive numa classe própria, registrada automaticamente num mapa por código. Para adicionar um caso novo, basta criar a classe — nenhum switch/if-else central precisa mudar.
- **Enum com taxa** para região, já que só o valor muda e a fórmula é fixa.
- **CheckoutService** orquestra a validação e o cálculo na ordem certa, delegando cada parte para a implementação correspondente.
