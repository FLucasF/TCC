# Padrões: Strategy

## Visão geral

O agente montou o serviço já no formato Strategy nos quatro pontos que pedem o padrão. A entrega, o cupom e a forma de pagamento são interfaces (`ShippingMethod`, `Coupon`, `PaymentMethod`) com implementações em `record`. Os casos são instâncias registradas por código em `Rules.standard` (`src/main/java/shop/domain/Rules.java:10-35`) e são achados por um `Registry` que é um mapa de código para objeto (`src/main/java/shop/domain/Registry.java:18-20`). O nível do clube é um `record` só de dados com um método para todos os níveis (`src/main/java/shop/domain/ClubTier.java:6-15`). A região é um enum só de números, que é a resposta simples que o controle negativo pede.

A busca pelos códigos dos casos fora de `Rules.java` e de `Region.java` não acha nada: o cálculo em `CheckoutService` nunca nomeia um caso. As quatro regras cruzadas foram resolvidas com propriedades genéricas ou com contexto passado ao caso, sem `if` sobre casos concretos.

Não há problema sério, só ressalvas menores, em ordem de peso:
- a combinação "nível dá frete grátis × entrega aceita esse benefício" é feita por uma linha genérica no serviço, e não dentro do nível;
- a assinatura de `PaymentMethod.settle` carrega um `boolean couponUsed` que só o Pix usa;
- o `Coupon.Context` tem um campo `cpf` que nenhum cupom lê.

## Item a item

**P1-local, a entrega: onde mora o código de cada caso.** Há duas implementações:
- `ShippingMethod.Carrier` (`src/main/java/shop/domain/ShippingMethod.java:44-56`), usada por `ECONOMY` e `EXPRESS`, que diferem só no código de serviço `"STD"`/`"EXP"` (`Rules.java:16-17`);
- `ShippingMethod.Fixed` (`ShippingMethod.java:32-41`), usada por `STORE_PICKUP` e `COURIER`, que diferem só nos dados: preço, prazo, limite de peso e duas flags (`Rules.java:18-19`).

As regras de cada caso estão nessas casas:
- o limite de 5 kg do motoboy é o `maxWeightKg` testado em `ShippingMethod.java:36-38`;
- os dias a mais do Norte e do Nordeste só entram na transportadora, perguntando à região sem nomeá-la (`request.region().extraDispatchDays()`, `ShippingMethod.java:54`);
- a escolha entre `SHIPPING_METHOD_UNAVAILABLE` e `CARRIER_UNAVAILABLE` também fica nas implementações (`ShippingMethod.java:37`, `:50`, `:52`).

A tradução `"STD"`/`"EXP"` vai como dado para o lado da transportadora, o que não espalha a entrega. No `CheckoutService`, a única linha que toca a entrega fora da casa é `shipping.coveredByTierFreeShipping()` (`src/main/java/shop/app/CheckoutService.java:68`). Ela pergunta a qualquer entrega e não nomeia nenhuma.
Conclusão: acerto

**P1-selecao, a entrega: como o caso é escolhido.** Por consulta: `rules.shipping().find(text(body.path("shippingMethod")))` (`CheckoutService.java:61-62`) busca no mapa do `Registry`, e o cálculo chama `shipping.quote(...)` (`CheckoutService.java:67`) sem saber qual entrega é.
Conclusão: acerto

**P2-local, o cupom: onde mora o código de cada caso.** Cada cupom tem a sua classe aninhada em `Coupon.java`:
- `PercentOfProducts` para o WELCOME10 (`src/main/java/shop/domain/Coupon.java:21-29`);
- `FixedOffProducts` para o MINUS50 (`:31-39`);
- `FreeShipping` para o FREESHIPPING (`:41-49`);
- `EveryNthFree` para o BUY3PAY2 (`:52-62`).

A condição e a conta de cada um estão na sua casa, inclusive a recusa `COUPON_NOT_APPLICABLE` (`Coupon.java:25`, `:35`, `:45`). O teto de 30% fica no serviço (`CheckoutService.java:80-81`), mas vale para todos os cupons e não cita nenhum. Os nomes só aparecem como chave de registro (`Rules.java:21-24`). Um detalhe sem efeito no desenho: o campo `cpf` do `Coupon.Context` (`Coupon.java:8`) não é lido por nenhum cupom.
Conclusão: acerto

**P2-selecao, o cupom: como o caso é escolhido.** Por consulta: `rules.coupons().find(...)` (`CheckoutService.java:76-77`), e depois `coupon.discount(context)` (`CheckoutService.java:81`). O `if (couponUsed)` (`CheckoutService.java:75`) só vê se veio um cupom e não nomeia nenhum caso.
Conclusão: acerto

**P3-local, a forma de pagamento: onde mora o código de cada caso.** Há três implementações: `Pix` (`src/main/java/shop/domain/PaymentMethod.java:20-32`), `BankSlip` (`:34-52`) e `CreditCard` (`:55-73`). Cada uma decide:
- as suas parcelas, em `acceptsInstallments` (`:22`, `:36`, `:58`);
- o seu ajuste, em `settle`: o desconto do Pix, a tarifa do boleto, a tabela Price do cartão;
- a sua recusa: o limite de R$ 1.000,00 só existe em `BankSlip.checkAvailable` (`:41-45`), em cima de um `default` vazio da interface (`:15-16`).

O serviço só pergunta em termos genéricos: `payment.acceptsInstallments` (`CheckoutService.java:145`), `payment.checkAvailable` (`:90`) e `payment.settle` (`:91`). O código de erro `INVALID_INSTALLMENTS` é único e não depende do caso. A busca por `PIX`, `CREDIT_CARD` e `BANK_SLIP` só acha `Rules.java:26-28`.
Conclusão: acerto

**P3-selecao, a forma de pagamento: como o caso é escolhido.** Por consulta: `rules.payments().find(...)` (`CheckoutService.java:85-86`).
Conclusão: acerto

**P4-local, o nível do clube: onde mora o código de cada caso.** `ClubTier` é um `record` com `creditRate`, `freeShipping` e `giftAbove`. Há um método `credit` e um método `gift` para todos os níveis (`ClubTier.java:8-14`), e os três níveis são instâncias só de dados (`Rules.java:12-14`). Os níveis diferem só nesses números e flags, e a régua conta isso como acerto. Busquei `credit`, `shipping` e `gift`:
- o serviço usa `tier.freeShipping()` (`CheckoutService.java:68`), `tier.credit(subtotal)` (`:95`) e `tier.gift(subtotal)` (`:96`);
- o saldo é lançado a partir de `summary().nextPurchaseCredit()` (`src/main/java/shop/app/CreditEffect.java:11`, `:16`).

Nenhuma dessas linhas nomeia um nível. Uma ressalva: um nível novo com uma vantagem de outro tipo (por exemplo, frete com desconto parcial) obrigaria a mudar o `record` e a linha 68 do serviço. Hoje, porém, os casos cabem nos dados.
Conclusão: acerto

**P4-selecao, o nível do clube: como o caso é escolhido.** Por consulta: `rules.tiers().find(text(body.path("clubTier")))` (`CheckoutService.java:54-55`).
Conclusão: acerto

**R1, o GOLD sem frete, exceto no motoboy.** Nenhuma linha escreve `GOLD` ou `COURIER` fora do registro. A regra é feita de dois dados:
- o GOLD tem `freeShipping = true` (`Rules.java:14`);
- o motoboy tem `coveredByTierFreeShipping = false` (`Rules.java:19`, flag declarada em `ShippingMethod.java:27-29` com `default true`).

Uma linha genérica do serviço junta os dois: `tier.freeShipping() && shipping.coveredByTierFreeShipping() ? Money.ZERO : ...` (`CheckoutService.java:68-69`). Um nível ou uma entrega nova não muda essa linha, então ela não é o remendo. A ressalva é que a decisão fica no checkout, e não numa política dentro do nível (algo como `tier.shippingCost(method, quote)`), como a régua sugere. O efeito na extensão é pequeno.
Conclusão: acerto

**R2, o FREESHIPPING não vale na retirada.** A regra mora no cupom: `if (!c.shipping().chargesFreight()) throw notApplicable()` (`Coupon.java:44-46`). Ela nem cita `STORE_PICKUP`: pergunta à entrega se ela cobra frete, e a retirada responde `false` pelo dado em `Rules.java:18`. Não há nada no serviço.
Conclusão: acerto

**R3, o desconto do Pix só sem cupom.** A regra mora no Pix, que recebe o contexto: `couponUsed ? Money.ZERO : Money.round(total.multiply(discountRate))` (`PaymentMethod.java:28`). O serviço só calcula `couponUsed` de forma genérica, pela presença do campo (`CheckoutService.java:73`), e passa esse valor a qualquer forma de pagamento (`:91`). A ressalva é que o `boolean couponUsed` faz parte da assinatura de `settle` de todas as formas (`PaymentMethod.java:18`), embora só o Pix o use. Um objeto de contexto seria mais estável, mas a regra não escapou da casa.
Conclusão: acerto

**R4, o WELCOME10 só na primeira compra.** A checagem está no cupom: `if (firstPurchaseOnly && !c.firstPurchase().getAsBoolean()) throw notApplicable()` (`Coupon.java:24-26`). O serviço só entrega ao contexto um fornecedor preguiçoso da consulta, `() -> !db.tx(d -> d.hasCompletedPurchase(cpf))` (`CheckoutService.java:79`). Esse fornecedor não cita o WELCOME10 e só roda se o cupom pedir. Não há checagem no controller nem no serviço.
Conclusão: acerto

**P5-forma, a região: que forma a variação tem.** Um enum que só carrega números, com construtor e sem corpo por constante: a porcentagem, o limite (ou `null`) e os dias a mais (`src/main/java/shop/domain/Region.java:8-22`). Há um método `insurance` para todas as regiões (`Region.java:24-27`), um acessor `extraDispatchDays` (`:29-31`) e um `parse` por laço genérico sobre `values()` (`:33-40`).
Conclusão: contenção

**P5-proporcao, a região: a resposta tem o tamanho do problema.** Sim. A variação é dado puro: não há classe por região nem constante com corpo, e nenhum `switch`. Quem usa a região só pergunta: `region.insurance(subtotal)` (`CheckoutService.java:88`) e `request.region().extraDispatchDays()` (`ShippingMethod.java:54`).
Conclusão: contenção

**E, estrutura sem necessidade fora dos pontos.** Olhei a estrutura perto do Strategy:
- `Registry<T>` (`Registry.java`) é um mapa genérico usado pelos quatro pontos que pedem consulta, então é necessário;
- `Rules` (`Rules.java`) só reúne os registros e o teto do cupom;
- os `record`s de valor (`ShippingMethod.Request`/`Quote`, `PaymentMethod.Settlement`, `Coupon.Context`) são parâmetros e resultados das estratégias, não camadas.

`CarrierGateway` tem uma implementação de produção só (`TransLogClient`), mas o enunciado pede a troca de transportadora ("Pode ser que um dia a gente troque de transportadora"), e os testes a substituem (`src/test/java/shop/TestApp.java:51`). Isso é assunto do Adapter, não estrutura sem necessidade. Também tiro `PaidOrderEffect` e `OrderLifecycle`, que respondem a pontos de outros patterns. O mais perto de sobra são duas coisas pequenas que não chegam a ser estrutura: o `ShippingMethod.Quote` duplica a forma de `CarrierGateway.Quote`, e o campo `cpf` do `Coupon.Context` está morto.
Conclusão: contenção

## O que mudaria primeiro

1. **Levar para o nível a decisão do frete grátis.** Hoje a linha `CheckoutService.java:68-69` junta duas flags. Com um método no nível que recebe a entrega e a cotação (ou numa política de frete), um nível novo com outro tipo de vantagem no frete mudaria só o nível, que é o crescimento que o enunciado anuncia ("cada um vai ter seu conjunto de vantagens").
2. **Trocar o `boolean couponUsed` de `PaymentMethod.settle` por um contexto da compra.** Hoje uma regra que é só do Pix aparece na assinatura de todas as formas de pagamento (`PaymentMethod.java:18`). Um objeto de contexto deixa a próxima regra cruzada de pagamento entrar sem mudar a interface.

## Dúvidas

- **A casa de `STORE_PICKUP` e `COURIER`, e de `ECONOMY` e `EXPRESS`.** Cada par divide uma classe (`Fixed` e `Carrier`), e o que é só de cada caso é a linha de dados em `Rules.java`. Pela definição, a casa é uma unidade só do caso. Tratei esses pares como o `enum` só de dados, que a régua aceita como acerto quando os casos diferem só nos dados. É o que acontece aqui: o código das duas classes é genérico e as diferenças são números, flags e o código de serviço.
- **O nível do clube como `record` e não como interface.** Também apliquei a regra do "só dados, um método para todos". Uma leitura mais exigente da frase "cada um vai ter seu conjunto de vantagens" pediria uma interface, mas a régua aceita a forma de dados quando os casos de hoje cabem nela.
