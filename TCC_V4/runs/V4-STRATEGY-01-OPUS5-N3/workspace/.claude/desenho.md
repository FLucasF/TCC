# Desenho — serviço de resumo do checkout

## O que muda de caso para caso

O enunciado descreve cinco eixos de variação, e só eles:

1. **Modalidade de entrega** — muda o cálculo do frete, o prazo e a
   disponibilidade (motoboy só até 5 kg). O enunciado avisa que entra opção
   nova quase toda semana.
2. **Cupom** — muda a condição de aplicabilidade e a conta do desconto. Cada
   cupom olha coisas diferentes: MENOS50 olha o subtotal, FRETEGRATIS olha o
   frete, LEVE3PAGUE2 olha item por item.
3. **Nível do clube** — muda o percentual de crédito, o frete que o cliente
   paga e se vai brinde. O enunciado avisa que vão criar mais níveis.
4. **Forma de pagamento** — muda o ajuste sobre o total, quais parcelamentos
   são permitidos e a disponibilidade (boleto só até R$ 1.000,00).
5. **Região** — muda **apenas o percentual** do seguro. O enunciado é
   explícito: "é só a porcentagem que muda, a conta é a mesma em todas".

## O que é igual em todos os casos

- A ordem do cálculo (produtos → cupom → frete → seguro → total → pagamento).
- O arredondamento: centavos, meio para o par, em cada etapa.
- A fórmula do percentual, usada pelo seguro e pelo crédito do clube:
  percentual × subtotal, arredondado (`Dinheiro.percentual`). Cada caso só
  informa o seu número.
- A ordem de verificação dos erros e o formato `{ "erro": "CODIGO" }`.
- O formato da requisição e da resposta.

## Estrutura escolhida

- Os quatro eixos cujo **comportamento** varia viram uma interface por eixo,
  com uma implementação por caso, e cada caso guarda sua própria regra:
  `ModalidadeEntrega`, `Cupom`, `NivelClube`, `FormaPagamento`.
- A escolha do caso é uma busca em um `Catalogo<T>` (mapa código → instância),
  nunca uma cadeia de `if`/`switch`. Acrescentar uma transportadora, um cupom
  ou um nível é criar uma classe e registrá-la no catálogo.
- A **região** não tem comportamento próprio, só um número: fica um `enum`
  `Regiao` com a alíquota como campo. Criar interface aqui seria estrutura para
  variação que o enunciado não descreve.
- `CalculoResumoService` concentra o que é igual: a sequência do cálculo, as
  validações na ordem pedida e o arredondamento.

## Assinaturas, decididas pelo caso mais exigente

- `Cupom.desconto(ContextoCupom)` recebe um contexto com itens, subtotal e
  frete, porque juntos os três cupons precisam dos três dados.
- `ModalidadeEntrega.frete(Pedido)` e `atende(Pedido)` recebem o pedido
  inteiro (peso e subtotal), não só o peso.
- `NivelClube.frete(ModalidadeEntrega, Pedido)` em vez de uma bandeira
  "isenta frete": a isenção do OURO é comportamento do nível, então mora no
  próprio OURO, e o serviço não decide nada — só pergunta.
- `FormaPagamento.liquidar(total, parcelas)` devolve `Liquidacao(totalFinal,
  valorParcela)`, porque no cartão com juros o total final deriva da parcela,
  e no Pix/boleto a parcela deriva do total.

## Decisões em pontos que o enunciado não fecha

- **FRETEGRATIS para um cliente OURO**: o frete já sai zerado por causa do
  nível, e o cupom devolve "o valor do frete", ou seja, zero. O cupom é
  calculado depois do frete justamente para poder enxergá-lo.
- **LEVE3PAGUE2 sem nenhum item com 3 unidades**: o cupom vale e o desconto é
  zero. O enunciado não lista condição para esse cupom, então não há motivo
  para recusar o pedido.

## Por quê

Cada regra de negócio fica num arquivo só dela, legível isolada, e a entrada
de um caso novo não mexe em nenhum caso existente nem no serviço.
