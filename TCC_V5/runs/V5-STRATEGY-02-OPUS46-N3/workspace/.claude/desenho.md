# Desenho — Resumo do Checkout

## O que muda de caso para caso

| Dimensão          | O que varia                                                    | Cresce? |
|-------------------|----------------------------------------------------------------|---------|
| Entrega           | Fórmula do frete, prazo, regra de disponibilidade              | Sim, toda semana |
| Cupom             | Cálculo do desconto e condição de aplicabilidade               | Sim, marketing cria novos |
| Forma de pagamento| Fórmula do ajuste (desconto/juros/tarifa) e restrições         | Possível |
| Nível do clube    | Conjunto de vantagens (cashback, frete grátis, brinde)         | Sim, novos níveis planejados |
| Região            | Só a porcentagem do seguro; a conta é sempre a mesma           | Não (dados, não comportamento) |

## O que é igual em todos os casos

- Pipeline de cálculo: subtotal → frete → cupom → seguro → total → pagamento.
- Arredondamento HALF_EVEN para 2 casas em cada etapa.
- Seguro = porcentagem × subtotal (sem desconto, sem frete).
- Crédito do clube = porcentagem × subtotal.
- Ordem de validação fixa (10 verificações).
- Estrutura do request/response JSON.

## Estrutura escolhida

- **Entrega, Cupom, Pagamento, Clube** → Strategy + Registry.
  Cada caso é um `@Component` com sua lógica. Um Registry (Map código→estratégia)
  resolve pelo nome, sem cadeia de if/else.
- **Região** → Enum com campo `taxaSeguro`. Só dado varia, comportamento não.
- **CheckoutService** orquestra o pipeline e as validações na ordem certa.

## Por quê

O enunciado descreve entrega e cupom como frequentemente mutáveis; clube como
planejando crescer. Strategy isola cada caso num lugar só e permite adicionar
novos sem tocar no código existente. Região é enum porque a especificação
diz explicitamente que só a porcentagem muda.
