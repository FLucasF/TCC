# Desenho — serviço de resumo do checkout

## O que varia de caso para caso

- **Modalidade de entrega**: cada opção (ECONOMICA, EXPRESSA, RETIRADA_LOJA,
  MOTOBOY) tem sua própria conta de frete, seu prazo e sua própria condição de
  disponibilidade (ex.: motoboy só até 5kg). O enunciado avisa que isso cresce
  quase toda semana. → cada modalidade é uma implementação de uma interface
  `ModalidadeEntrega`, escolhida por um mapa (código → implementação), sem
  if/switch por código.

- **Cupom**: cada cupom tem sua própria condição de aplicabilidade e sua
  própria conta de desconto (percentual sobre produtos, valor fixo com piso,
  equivalente ao frete, ou "a cada 3 leva 1"). → cada cupom é uma
  implementação de `Cupom`, escolhida por um mapa (código → implementação).

- **Nível do clube**: cada nível tem seu próprio crédito, sua própria regra
  de frete grátis e sua própria regra de brinde. O enunciado avisa que vêm
  mais níveis. → cada nível é uma implementação de `BeneficioClube`, escolhida
  por um mapa (nível → implementação).

- **Forma de pagamento**: cada forma tem seu próprio ajuste sobre o total,
  sua própria regra de parcelamento permitido e sua própria condição de
  disponibilidade (ex.: boleto até R$1000). → cada forma é uma implementação
  de `FormaPagamento`, escolhida por um mapa (código → implementação).

## O que é igual em todos os casos

- **Região**: só a porcentagem do seguro muda; a conta (percentual sobre o
  subtotal de produtos, arredondado) é sempre a mesma. → não é estratégia,
  é só um enum com o percentual de cada região.

- Ordem de cálculo do resumo (produtos → cupom → frete → seguro → total →
  ajuste de pagamento) e o arredondamento "meio para o par" em cada etapa são
  fixos, independente do caso. → ficam no serviço central `ResumoService`,
  que chama as estratégias nos pontos certos.

- Validação de pedido (itens) é igual sempre → fica no próprio serviço,
  antes de qualquer estratégia.

## Estrutura

```
model/        Item, NivelClube, Regiao (enum com percentual), CodigoErro
entrega/      ModalidadeEntrega (interface) + 4 implementações + registro
cupom/        Cupom (interface) + 4 implementações + registro
clube/        BeneficioClube (interface) + 3 implementações + registro
pagamento/    FormaPagamento (interface) + 3 implementações + registro
web/          ResumoController, dtos de request/resposta/erro
service/      ResumoService (orquestra, igual para todo caso) + Arredondamento
exception/    CheckoutException(CodigoErro)
```

Cada estratégia conhece seu próprio código (ex. "MOTOBOY", "OURO") e se
registra num `Map<String, X>` via Spring (`Map<String, ModalidadeEntrega>`
injetado automaticamente a partir do nome do bean = código). O serviço central
só faz `mapa.get(codigo)`; nunca um if/else ou switch por código de caso.

A assinatura das interfaces foi pensada olhando o caso mais exigente de cada
família (ex.: `FormaPagamento` recebe total e parcelas e devolve total final +
ajuste, porque cartão parcelado precisa dos dois; `ModalidadeEntrega` recebe
peso e devolve frete + prazo e tem um método de disponibilidade separado,
porque motoboy precisa recusar acima de 5kg sem deixar de informar frete/prazo
para quem pode usá-lo).
