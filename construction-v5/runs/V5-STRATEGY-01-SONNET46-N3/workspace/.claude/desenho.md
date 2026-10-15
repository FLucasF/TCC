# Desenho — checkout/resumo

## O que muda de caso para caso

| Dimensão | O que varia |
|---|---|
| Entrega | fórmula de custo, prazo, restrições (ex.: peso máximo) |
| Cupom | condição de aplicabilidade, fórmula do desconto |
| Nível do clube | se isenta frete, taxa de crédito, se dá brinde |
| Forma de pagamento | se aceita parcelas, limite do pedido, fórmula do total final |
| Região | taxa de seguro (só a porcentagem muda) |

## O que é igual em todos os casos

- Sequência de cálculo: subtotal → desconto cupom → frete → seguro → total base → ajuste pagamento
- Arredondamento HALF_EVEN para 2 casas em cada etapa
- Validações na ordem exata do contrato, retornando o primeiro erro
- Crédito e brinde calculados sobre o subtotal bruto (sem desconto, sem frete)

## Estrutura escolhida

Para cada dimensão que varia:
- **Interface** com o contrato do caso (ex.: `ModalidadeEntrega`, `Cupom`, `NivelClube`, `FormaPagamento`)
- **Uma classe por caso** com o comportamento daquele caso isolado
- **Registry (`@Component`)** que expõe `Optional<T> buscar(String codigo)`, eliminando if-else na escolha do caso

`Regiao` é um enum (conjunto fixo sem comportamento complexo); a taxa é um campo da constante.

O `CheckoutService` orquestra: valida, busca cada caso via registry, executa os cálculos na ordem definida, monta a resposta.

## Por que não outra abordagem

- Enum com métodos abstratos: boa para conjuntos fixos e pequenos. Entrega e cupom mudam com frequência (enunciado explícito) → classe separada é mais fácil de adicionar sem tocar nas existentes.
- Não foi criada estrutura para variação imaginada (ex.: desconto em cascata, múltiplos cupons) — apenas o que o enunciado descreve.
