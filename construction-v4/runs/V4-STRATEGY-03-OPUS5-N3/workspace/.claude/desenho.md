# Desenho — serviço de resumo do checkout

## O que muda de caso para caso

| Eixo | O que varia | O enunciado diz que cresce? |
|---|---|---|
| Modalidade de entrega | fórmula do frete, prazo, restrição de disponibilidade | sim ("quase toda semana entra uma opção nova") |
| Cupom | fórmula do desconto, condição de aplicabilidade | sim (marketing inventa promoção) |
| Nível do clube | % de crédito, frete grátis, brinde | sim ("estudando criar mais níveis") |
| Forma de pagamento | ajuste sobre o total, parcelas permitidas, restrição de disponibilidade | não declarado, mas os 3 casos já têm regras bem diferentes |
| Região | **só a porcentagem** do seguro | não |

## O que é igual em todos os casos

- A sequência do cálculo: produtos → cupom → frete → seguro → total → pagamento.
- O arredondamento: sempre centavos, meio-para-o-par, a cada etapa.
- A fórmula do seguro (% sobre produtos) e a do crédito (% sobre produtos).
- A ordem de validação e o formato `{ "erro": "CODIGO" }`.

## Estrutura escolhida

- Um **tipo por caso** nos quatro eixos que variam em comportamento
  (`ModalidadeEntrega`, `Cupom`, `NivelClube`, `FormaPagamento`): interface +
  uma classe `@Component` por caso. Cada caso guarda sua regra inteira (custo,
  prazo, condição) num lugar só dele. Adicionar uma transportadora, um cupom ou
  um nível do clube = criar uma classe, sem tocar no resto.
- A escolha entre casos é **busca por código num registro** (`Map<String, T>`
  montado a partir da lista injetada pelo Spring), não cadeia de `if`/`switch`.
  O registro também é o que distingue "não existe" (erro de código inválido) de
  "existe mas não atende" (erro de indisponível).
- **Região é enum com um campo de taxa**, porque ali só muda o número, não o
  comportamento. Criar classe por região seria estrutura sem variação.
- `ResumoService` é o único lugar que conhece a sequência das etapas; ele não
  conhece nenhum caso concreto.
- Erros: `PedidoRecusadoException(codigo)` lançada na ordem da tabela por um
  único validador dentro do serviço; um `@RestControllerAdvice` traduz para JSON.
- `Dinheiro` concentra o arredondamento meio-para-o-par e as operações, para
  nenhuma etapa reinventar `setScale`.

## Assinaturas (decididas olhando o caso mais exigente de cada eixo)

- `ModalidadeEntrega`: `codigo()`, `atende(ContextoEntrega)`, `frete(ContextoEntrega)`,
  `prazoDias()` — contexto leva peso e subtotal porque o frete exigente (ECONOMICA)
  depende do peso e o motoboy depende do peso para a restrição.
- `Cupom`: `codigo()`, `aplicavel(ContextoCupom)`, `desconto(ContextoCupom)` —
  contexto leva itens (LEVE3PAGUE2), subtotal (BEMVINDO10/MENOS50) e frete
  (FRETEGRATIS), que é o caso mais exigente. Por isso o frete é calculado antes
  do desconto, mesmo o resumo exibindo o desconto antes.
- `NivelClube`: `codigo()`, `credito(subtotal)`, `frete(freteDaEntrega)`,
  `temBrinde(subtotal)` — o nivel devolve o frete que ele paga, em vez de expor uma
  flag para o servico decidir; assim a vantagem do OURO fica dentro do OURO.
- `FormaPagamento`: `codigo()`, `parcelasPermitidas(n)`, `atende(total)`,
  `cobrar(total, parcelas) -> Cobranca(totalFinal, valorParcela)` — caso mais
  exigente é CARTAO, que precisa das parcelas e devolve parcela e total juntos.

## Decisões de borda

- Campos de domínio chegam como texto no DTO (não como enum do Jackson) para
  que valor desconhecido vire o código de erro certo, e não erro de desserialização.
- `FRETEGRATIS` e `LEVE3PAGUE2` não têm condição declarada: são sempre aplicáveis
  e podem render desconto zero (frete já grátis, nenhum item com 3 unidades).
- Cupom em branco (`""`) conta como ausencia de cupom, igual a cupom nao enviado.
- Corpo JSON que nem da para ler (tipo trocado onde o campo e lista, JSON quebrado)
  vira `PEDIDO_INVALIDO`, por ser o codigo de "o pedido que chegou nao serve".
