# Desenho — Serviço de Resumo de Checkout

## O que muda de caso para caso

| Dimensão | Varia em quê | Frequência de mudança |
|---|---|---|
| Modalidade de entrega | Cálculo do frete, prazo, regra de disponibilidade | Alta (quase toda semana) |
| Cupom | Cálculo do desconto, condição de aplicabilidade | Alta (marketing inventa) |
| Nível do clube | % crédito, frete grátis, brinde | Média (estudando novos níveis) |
| Forma de pagamento | Ajuste sobre o total, regras de parcelamento e disponibilidade | Baixa, mas comportamento distinto por caso |
| Região (seguro) | Só a porcentagem muda; fórmula é a mesma | Baixa |

## O que é igual em todos os casos

- Ordem do cálculo: subtotal → cupom → frete → seguro → total → pagamento.
- Arredondamento: HALF_EVEN para 2 casas em cada etapa.
- Seguro: sempre `subtotal × taxa da região`, sem desconto e sem frete.
- Crédito do clube: sempre sobre o subtotal bruto.
- Validação: ordem fixa de checagem, retorna o primeiro erro.

## Estrutura escolhida

- **Entrega, Cupom, Nível do Clube, Forma de Pagamento**: cada caso é uma
  classe própria que implementa uma interface do domínio e é registrada como
  `@Component`. O serviço recebe todas as implementações via injeção e monta um
  mapa `código → implementação`. Escolher entre os casos é um `Map.get(código)`,
  sem sequência de `if/else`.
- **Região**: enum simples com a taxa de seguro, pois só a porcentagem varia e a
  fórmula é idêntica.
- **CheckoutService**: orquestra o cálculo na ordem fixa, delegando às
  estratégias.

## Por quê

As dimensões que o enunciado descreve como variando com frequência (entrega,
cupom, clube) ganham extensibilidade por adição de classe. As que não variam
(região/seguro) ficam num enum direto. Nenhuma variação imaginária foi
adicionada.
