# Desenho — serviço de resumo do checkout

## O que muda de caso para caso

1. **Modalidade de entrega**: a fórmula do frete, o prazo e a restrição de
   disponibilidade (motoboy até 5 kg). Entram opções novas quase toda semana.
2. **Cupom**: a condição de aplicabilidade e a fórmula do desconto (percentual
   sobre produtos, valor fixo com mínimo, valor do frete, unidades grátis por
   item).
3. **Nível do clube**: percentual de crédito, isenção de frete e brinde.
   Novos níveis estão previstos.
4. **Forma de pagamento**: parcelas permitidas, disponibilidade (boleto até
   R$ 1.000) e como o total vira valor final + valor da parcela.
5. **Região**: apenas o percentual do seguro — o enunciado diz explicitamente
   que a conta é a mesma.

## O que é igual em todos os casos

- A ordem do cálculo: produtos → cupom → frete → seguro → total → pagamento.
- O arredondamento: centavos, meio-para-o-par (`HALF_EVEN`), no resultado de
  cada etapa, onde ele nasce.
- A conta do seguro e a do crédito do clube: percentual sobre o subtotal de
  produtos (sem desconto, sem frete).
- A ordem de verificação dos erros e o formato `{ "erro": "CODIGO" }`.

## Estrutura escolhida

Cada dimensão que varia é um **enum-estratégia**: o enum é o catálogo de casos
(usado também para traduzir o texto que vem do site) e o comportamento de cada
caso mora no corpo da própria constante. Assim, acrescentar uma transportadora,
um cupom ou um nível do clube é acrescentar uma constante, sem mexer em nada que
já existe, e em nenhum ponto se escolhe o caso com cadeia de condições.

- `ModalidadeEntrega` → `frete(pesoKg)`, `prazoDias()`, `atende(pesoKg)`.
  Assinatura decidida pelo caso mais exigente: as duas opções que cobram por kg
  e o motoboy, cuja disponibilidade depende do peso. O frete é método de cada
  constante (e não um campo com uma conta comum, como na região) porque o
  enunciado diz que cada transportadora tem o *seu jeito de cobrar* — fixo +
  por kg é o jeito de duas delas, não a regra da loja.
- `Cupom` → `aplicavel(ctx)`, `desconto(ctx)`, com `ContextoCupom` carregando
  itens + subtotal + frete, porque `LEVE3PAGUE2` precisa dos itens e
  `FRETEGRATIS` precisa do frete. Só `MENOS50` tem condição: é a única
  promoção que o enunciado descreve com exigência. Desconto zerado não é
  recusa — `FRETEGRATIS` numa retirada na loja e `LEVE3PAGUE2` sem nenhum trio
  valem, com desconto 0,00.
- `NivelClube` → `credito(subtotal)` (percentual é campo, a conta é comum),
  `freteDevido(frete)` e `brinde(subtotal)`.
- `FormaPagamento` → `permiteParcelas(n)`, `atende(total)`,
  `cobrar(total, n)` devolvendo `ValorCobrado(totalFinal, valorParcela)`.
  Assinatura pelo caso mais exigente (cartão: depende de total e de parcelas).
- `Regiao` → só o percentual como campo, uma única conta herdada.

`Dinheiro` concentra o arredondamento e os percentuais, e cada valor é
arredondado uma única vez, por quem o calcula — inclusive a linha do carrinho
(`Item.total()`), para que a soma dos produtos seja a soma de valores já em
centavos.

## A ordem no serviço

`CheckoutService` é a parte igual a todos os casos: valida na ordem definida,
pede a cada estratégia o seu pedaço e monta o resumo. Os erros são lançados
como `CheckoutException` com o código, e um `@RestControllerAdvice` os traduz
para o JSON de erro — a ordem de verificação fica num lugar só, legível de cima
para baixo.

Uma inversão necessária: o **frete é calculado antes do desconto do cupom**,
porque `FRETEGRATIS` desconta justamente o valor do frete que vai aparecer no
resumo (já zerado, no caso do OURO). O resultado é o mesmo da ordem do
enunciado, que só descreve como as parcelas entram no total.

O serviço fica no pacote `web`, junto do controller e dos DTOs: ele é a camada
que lê os dados do site e orquestra o domínio. Os campos que podem vir com
valor inexistente (`nivelClube`, `regiao`, `modalidadeEntrega`, `cupom`,
`formaPagamento`) são recebidos como texto no DTO e traduzidos pelo serviço,
para que um valor desconhecido vire o código de erro certo em vez de erro de
desserialização. Por isso `ItemRequest` (tudo pode ser nulo) e `Item` (já
validado) são registros separados.
