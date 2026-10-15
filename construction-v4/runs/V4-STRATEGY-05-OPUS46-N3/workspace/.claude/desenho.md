# Desenho — Resumo de Checkout

## O que muda de caso para caso

| Dimensão | O que varia | Descrito como crescente? |
|---|---|---|
| Modalidade de entrega | Custo, prazo, regra de disponibilidade | Sim — "quase toda semana entra opção nova" |
| Cupom | Lógica de desconto e condição de aplicabilidade | Sim — "marketing adora inventar promoção" |
| Nível do clube | % crédito, frete grátis, limiar de brinde | Sim — "estudando criar mais níveis" |
| Forma de pagamento | Ajuste sobre o total, regra de parcelas, disponibilidade | Cada forma tem comportamento próprio |
| Região | Apenas a taxa de seguro (cálculo idêntico em todas) | Não |

## O que é igual em todos os casos

1. Fluxo de cálculo: subtotal → frete (com isenção do clube) → desconto do cupom → seguro → total do pedido → ajuste do pagamento → total final.
2. Arredondamento: HALF_EVEN para 2 casas em cada valor monetário.
3. Ordem de validação (erros 1–10) antes/durante o cálculo.
4. Estrutura da resposta JSON.

## Estrutura escolhida

- **Interface + classes separadas** para entrega, cupom, clube e pagamento.
  Cada caso implementa a interface com `codigo()` + métodos de cálculo/validação.
  O serviço monta um `Map<String, T>` para cada grupo → despacho por chave, sem cadeia de `if`.
- **Enum** para região (só a taxa muda; cálculo idêntico).
- Classe utilitária `Moeda` para arredondamento centralizado.
- As estratégias são registradas como `@Component`; o serviço recebe `List<T>` via injeção e constrói os mapas.
