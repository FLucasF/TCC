# Desenho — Resumo do Checkout

## O que muda de caso para caso

| Dimensão | O que varia | Frequência de mudança |
|---|---|---|
| **Modalidade de entrega** | Cálculo do frete, prazo, regra de disponibilidade | Alta (quase toda semana) |
| **Cupom** | Lógica de desconto, condição de aplicabilidade | Alta (marketing inventa promoções) |
| **Nível do clube** | % crédito, frete grátis, brinde | Média (novos níveis planejados) |
| **Forma de pagamento** | Ajuste sobre o total, regras de parcelamento, restrições | Baixa, mas comportamento distinto por caso |
| **Região (seguro)** | Apenas a porcentagem; fórmula idêntica | Baixa |

## O que é igual em todos os casos

- Fluxo de cálculo: subtotal → desconto cupom → frete → seguro → total pedido → ajuste pagamento → total final.
- Arredondamento: `HALF_EVEN` para 2 casas em cada etapa monetária.
- Fórmula do seguro: `porcentagem × subtotalProdutos`.
- Fórmula do crédito: `porcentagem × subtotalProdutos`.
- Ordem fixa de validação (10 passos).

## Estrutura escolhida

- **Entrega, Cupom, Clube, Pagamento**: interface + uma implementação por caso, registradas via Spring como `@Component`. O serviço recebe `List<X>` e monta `Map<código, X>` — sem `if/else` para escolher.
- **Região**: `enum` com a taxa de seguro — só a porcentagem muda, a fórmula é a mesma em todas, não justifica polimorfismo.
- **Serviço (`CheckoutService`)**: orquestra o fluxo fixo, delega cada parte ao objeto do caso correspondente.

## Por quê

As quatro primeiras dimensões têm comportamento distinto por caso e pelo menos uma (entrega) muda com frequência declarada.
Região muda apenas um valor escalar com fórmula idêntica — `enum` basta.
