# Desenho — serviço de resumo do checkout

## O que muda de caso para caso

- **Modalidade de entrega**: cada uma tem seu jeito de cobrar, seu prazo e suas
  limitações (motoboy até 5 kg). O enunciado diz que entram opções novas quase
  toda semana.
- **Cupom**: cada código tem sua própria regra de desconto e sua própria
  condição de uso (percentual, valor fixo com piso, frete grátis, leve 3 pague 2).
- **Nível do clube**: cada nível tem seu conjunto de vantagens (crédito, frete,
  brinde) e o enunciado fala em criar mais níveis.
- **Forma de pagamento**: cada uma tem seu ajuste sobre o total (desconto Pix,
  tarifa do boleto, juros/parcelamento do cartão), seu parcelamento permitido e
  sua própria indisponibilidade (boleto acima de R$ 1.000,00).

## O que é igual em todos os casos

- A ordem do resumo: produtos → desconto → frete → seguro → total → ajuste de
  pagamento. No cálculo o frete é **apurado antes** do desconto, porque
  FRETEGRATIS depende dele; a ordem do resumo não muda.
- O arredondamento: meio-para-o-par, 2 casas, em cada etapa (`Dinheiro`).
- A conta de percentual sobre produtos é a mesma em todos os usos (seguro,
  crédito do clube, cupom percentual, desconto Pix): `Dinheiro.percentual`. Só a
  taxa muda, e a taxa pertence a quem a usa.
- O seguro: muda **só a alíquota** por região. Por isso região é um `enum` com a
  alíquota, e não cinco classes.

## Estrutura escolhida

- Quatro famílias polimórficas, uma interface por eixo que varia:
  `ModalidadeEntrega`, `Cupom`, `NivelClube`, `FormaPagamento`. Cada caso é uma
  classe própria (um bean), e o comportamento dele mora só lá.
- A escolha do caso é um **`Catalogo<T>` (mapa código → implementação)**,
  montado a partir dos beans; não há cadeia de `if`/`switch` por código. Código
  ausente ou desconhecido vira o erro próprio do eixo; dois casos com o mesmo
  código param a aplicação na subida.
- Sem cupom é tratado por um objeto nulo (`SemCupom`), para o cálculo não ter
  ramo especial.
- Assinaturas decididas olhando o caso mais exigente:
  - `ModalidadeEntrega`: precisa do peso (econômica/expressa) → `frete(peso)`,
    `atende(peso)`, `prazoDias()`.
  - `Cupom`: o mais exigente é FRETEGRATIS (precisa do frete) e LEVE3PAGUE2
    (precisa dos itens) → recebe um `ContextoCupom(itens, subtotal, frete)`.
  - `FormaPagamento`: o cartão precisa de total + parcelas e devolve total final
    **e** valor da parcela → `calcular(total, parcelas)` → `ResultadoPagamento`.
  - `NivelClube`: `credito(subtotal)`, `temBrinde(subtotal)` e
    `frete(freteDaModalidade)` — o nível **transforma** o frete, para "OURO não
    paga frete" morar dentro de `ClubeOuro` em vez de virar um `if` no
    orquestrador. O padrão zera o frete de ninguém.
- `CalculadoraDeResumo` só orquestra a ordem e as conferências (na ordem pedida,
  cada uma lançando `PedidoRecusadoException` com seu código); nenhuma regra de
  caso particular mora nela.

## Decisões de leitura do enunciado

- LEVE3PAGUE2 conta por linha do carrinho ("um mesmo item do carrinho").
- Código de cupom é comparado exatamente como escrito (maiúsculo); minúscula é
  `CUPOM_INVALIDO`.
- Brinde do OURO exige produtos **acima** de R$ 500,00; MENOS50 vale **a partir
  de** R$ 300,00; boleto vale até R$ 1.000,00 **inclusive**.
