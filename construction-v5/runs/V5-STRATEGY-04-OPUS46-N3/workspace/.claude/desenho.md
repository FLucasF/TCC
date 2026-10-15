# Desenho — Resumo de compra

## O que muda de caso para caso

| Dimensão | O que varia | Frequência de mudança |
|---|---|---|
| Modalidade de entrega | Cálculo do frete, prazo, regra de disponibilidade | Alta (novas toda semana) |
| Cupom | Cálculo do desconto, condição de aplicabilidade | Alta (marketing cria novos) |
| Nível do clube | % cashback, frete grátis, brinde | Média (novos níveis planejados) |
| Forma de pagamento | Cálculo do ajuste, regra de parcelas, regra de disponibilidade | Baixa |
| Região (seguro) | Só a porcentagem; cálculo é idêntico | Baixa |

## O que é igual em todos os casos

- Ordem de cálculo: subtotal → frete → cupom → seguro → total → ajuste pagamento
- Fórmula do seguro: percentual × subtotal (só o percentual muda)
- Fórmula do crédito: percentual × subtotal
- Arredondamento: HALF_EVEN, escala 2, em cada etapa
- Ordem de validação: 10 verificações em sequência fixa
- Formato de entrada/saída (JSON, campos fixos)

## Estrutura escolhida

Cada dimensão que varia (entrega, cupom, clube, pagamento) é uma **interface**
com implementações por caso, registradas como componentes Spring. O serviço
recebe as listas via injeção e monta mapas código→implementação — sem
if/else para escolher entre casos.

Região é um **enum** com o percentual de seguro, porque só o percentual muda e
o cálculo é sempre o mesmo.

### Por que interfaces e não enum com campos

- Entrega e cupom mudam com alta frequência e cada caso tem lógica própria
  (fórmulas diferentes, regras de disponibilidade diferentes).
- Clube tem lógica por nível (brinde depende de threshold, frete grátis é booleano).
- Enum com campos serviria para variação de dados; aqui a variação é de comportamento.
