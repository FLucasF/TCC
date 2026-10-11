# Desenho — Resumo do Checkout

## O que muda de caso para caso

| Dimensão | O que varia | Frequência de mudança |
|---|---|---|
| Modalidade de entrega | Cálculo do frete, prazo, regra de disponibilidade | Alta (quase semanal) |
| Cupom | Cálculo do desconto, condição de aplicabilidade | Alta (marketing inventa sempre) |
| Nível do clube | % crédito, frete grátis, brinde | Média (novos níveis planejados) |
| Forma de pagamento | Ajuste (desconto/tarifa/juros), regras de parcelas, disponibilidade | Baixa |
| Região (seguro) | Apenas a taxa % — cálculo idêntico em todas | Baixa |

## O que é igual em todos os casos

- Ordem do cálculo: subtotal → frete (+ ajuste clube) → cupom → seguro → total → pagamento.
- Arredondamento HALF_EVEN a cada etapa.
- Validação na ordem fixa da tabela de erros.
- Seguro = subtotal × taxa da região (só a taxa muda, conta é a mesma → enum).

## Estrutura escolhida

- **Entrega, Cupom, Clube, Pagamento**: interface + uma implementação por caso, registradas como beans Spring e coletadas num Map<código, implementação> pelo serviço. Adicionar um caso novo = criar uma classe nova, sem mexer no serviço.
- **Região**: enum com a taxa, pois só a porcentagem varia e a conta é idêntica.
- **Serviço**: orquestra a sequência fixa de cálculo e validação; não contém lógica de nenhum caso específico.
