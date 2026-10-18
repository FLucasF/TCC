# Padrões: Strategy

## Visão geral

O agente montou o Strategy nos quatro pontos que pedem o padrão, e já desde o começo. Há uma interface por ponto (`ShippingStrategy`, `CouponStrategy`, `PaymentStrategy`, `ClubTierStrategy`) e uma classe `@Component` por caso. As classes são achadas por consulta, em mapas montados a partir dos beans injetados (`service/CheckoutService.java:42-49`). Nenhum `if` ou `switch` sobre entregas, cupons, pagamentos ou níveis aparece no serviço, no controller ou no cálculo.

Três das quatro regras cruzadas moram na casa do par:
- o motoboy dentro do `GoldClub`;
- a recusa da retirada dentro do `FreeShippingCoupon`;
- o "só sem cupom" dentro do `PixPayment`.

O problema mais sério está no WELCOME10. A definição de "primeira compra" (nenhum pedido `PAID`, `SHIPPED` ou `DELIVERED`) mora no `CheckoutService`, não no cupom, e a consulta ao banco roda para qualquer cupom. O cupom só lê um booleano pronto.

A região, que é o controle negativo, foi resolvida sem estrutura por região. Mas a resposta são dois `switch` que devolvem números, e eles estão em dois lugares diferentes, não numa tabela única. Fora dos pontos do enunciado, não achei estrutura sem necessidade perto deste pattern.

## Item a item

**P1-local, onde mora o código de cada entrega.** Cada entrega tem a sua casa:
- `STORE_PICKUP`: grátis, 1 dia (`service/shipping/StorePickupShipping.java:21-23`);
- `COURIER`: R$ 18,00, 0 dias e o limite de 5 kg em `isAvailable` (`service/shipping/CourierShipping.java:11,19-26`);
- `ECONOMY` e `EXPRESS`: herdam da classe-base `TransLogShipping`, que consulta a TransLog, converte centavos e horas e soma os dias a mais do Norte e do Nordeste (`service/shipping/TransLogShipping.java:36-56`). Cada subclasse só diz o seu código, `"STD"` ou `"EXP"` (`EconomyShipping.java:20-22`, `ExpressShipping.java:20-22`).

A tradução para o código da TransLog fica do lado da TransLog e não espalha a entrega. A classe-base é comum às duas entregas que dependem da TransLog e não nomeia nenhuma delas, então também não conta como código de um caso fora da casa.

A busca pelos nomes das entregas fora de `service/shipping/` só acha o próprio enum e o `COURIER` dentro do `GoldClub`, que é a regra cruzada R1. O `CheckoutService` só faz validações genéricas (região, CEP, entrega nula: `CheckoutService.java:183-198`) e aplica `clubStrategy.hasFreeShipping(...)` sem nomear caso (`:217-219`).

Uma observação que não muda a conclusão: `TransLogShipping.isAvailable` engole qualquer exceção e devolve `false` (`TransLogShipping.java:30-31`). Com isso, a TransLog fora do ar vira `SHIPPING_METHOD_UNAVAILABLE`, o `catch` de `CheckoutService.java:205-209` nunca é alcançado, e a TransLog é consultada duas vezes por resumo. Isso é coisa do comportamento e do adaptador, não da casa dos casos.

Conclusão: acerto

**P1-selecao, como a entrega é escolhida.** É por consulta: `shippingStrategies.get(request.shippingMethod())` (`CheckoutService.java:195`), com o mapa montado a partir de `ShippingStrategy::getMethod` (`:44-45`). Os `if` de `:191-198` e `:211-213` só recusam o pedido e não fazem conta.

Conclusão: acerto

**P2-local, onde mora o código de cada cupom.** A conta e a condição de cada cupom estão na casa dele:
- MINUS50: o mínimo de R$ 300 e os R$ 50 (`service/coupon/Minus50Coupon.java:17,22`);
- BUY3PAY2: a unidade grátis a cada 3 (`Buy3Pay2Coupon.java:23-35`);
- FREESHIPPING: o desconto igual ao frete (`FreeShippingCoupon.java:17,22`);
- WELCOME10: os 10% (`Welcome10Coupon.java:23`).

O teto de 30% fica no serviço (`CheckoutService.java:258-259`), mas vale para todos os cupons e não nomeia nenhum.

O que escapa é a regra do WELCOME10. `CheckoutService.java:239-242` calcula `isFirstPurchase` com `existsByCustomerCpfAndStatusIn(cpf, PAID, SHIPPED, DELIVERED)`, e o cupom só devolve `context.isFirstPurchase()` (`Welcome10Coupon.java:18`). A linha do serviço não cita o WELCOME10, mas faz uma coisa que só serve a ele: guarda a definição de "primeira compra" do enunciado, com quais situações contam e o cancelado de fora. Se essa definição mudar, muda o serviço e não o cupom. E a consulta roda para todo cupom informado, mesmo o MINUS50.

Conclusão: malfeito

**P2-selecao, como o cupom é escolhido.** É por consulta: `couponStrategies.get(request.coupon())` (`CheckoutService.java:234`), com o mapa montado a partir de `CouponStrategy::getCode` (`:42-43`). O `if (request.coupon() == null)` (`:230`) trata a ausência de cupom e não nomeia caso. O cupom inexistente só recusa (`:235-237`).

Conclusão: acerto

**P3-local, onde mora o código de cada forma de pagamento.** Cada forma de pagamento guarda o seu ajuste, as suas parcelas e a sua recusa:
- Pix: o desconto de 5% e o "só sem cupom" (`service/payment/PixPayment.java:22-36`);
- cartão: de 1 a 12 parcelas, sem juros até 3x, tabela Price de 4x em diante (`CreditCardPayment.java:12,25-55`);
- boleto: a tarifa de R$ 3,49, o limite de R$ 1.000 e só à vista (`BankSlipPayment.java:10-11,19-32`).

O serviço não nomeia nenhuma forma de pagamento. Ele chama `isValidInstallments`, `isAvailable` e `calculatePayment` e escolhe o código de erro pelo resultado do contrato (`CheckoutService.java:272-284`).

Conclusão: acerto

**P3-selecao, como a forma de pagamento é escolhida.** É por consulta: `paymentStrategies.get(request.paymentMethod())` (`CheckoutService.java:267`, com o mapa em `:46-47`). Os `if` de `:263-270` só recusam.

Conclusão: acerto

**P4-local, onde mora o código de cada nível.** Busquei os nomes dos níveis e também `credit`, `shipping` e `gift`.
- O crédito de 2% e de 5% está em `SilverClub.java:19` e `GoldClub.java:21`.
- O frete grátis do GOLD está em `GoldClub.java:25-27`.
- O brinde acima de R$ 500 está em `GoldClub.java:12,30-32`.
- O BRONZE devolve zero e `false` em tudo (`BronzeClub.java:17-29`).

Fora da casa, só aparecem chamadas genéricas ao contrato (`CheckoutService.java:81-82,217`) e o uso do crédito que já saiu no resumo (`service/OrderService.java:128,197`, que lê `summary.nextPurchaseCredit()` sem perguntar o nível).

Conclusão: acerto

**P4-selecao, como o nível é escolhido.** É por consulta: `clubStrategies.get(tier)` (`CheckoutService.java:173`, com o mapa em `:48-49`). Os `if` de `:170-176` só recusam com `INVALID_CLUB_TIER`.

Conclusão: acerto

**R1, o GOLD sem frete, exceto no motoboy.** A regra mora no nível: `return shippingMethod != ShippingMethod.COURIER` (`service/club/GoldClub.java:26`). Citar o `COURIER` na casa do GOLD é a casa do par. O serviço só pergunta `clubStrategy.hasFreeShipping(request.shippingMethod())` e zera o frete (`CheckoutService.java:217-219`), sem nomear nível nem entrega.

Conclusão: acerto

**R2, o FREESHIPPING não vale na retirada.** A regra mora no cupom: `isApplicable` devolve `context.shippingCost().isPositive()` (`service/coupon/FreeShippingCoupon.java:17`). Não há `if` sobre `STORE_PICKUP` no serviço. O agente pôs a regra como "não vale sem frete a cobrar", e não como "não vale na retirada", tomando a explicação do enunciado ("que já não tem frete") como a própria regra.

Essa leitura tem uma consequência. O frete chega ao cupom já zerado pelo GOLD (o frete é calculado em `CheckoutService.java:64-66` antes do cupom em `:68-70`). Assim, um GOLD com FREESHIPPING numa entrega da TransLog também é recusado. Se isso está certo ou errado é coisa da dimensão do comportamento. Para o desenho, a regra está na casa do cupom.

Conclusão: acerto

**R3, o desconto do Pix só sem cupom.** A regra mora no Pix, que recebe o contexto: `if (context.hasCoupon())` (`service/payment/PixPayment.java:28-30`). O serviço só monta o contexto genérico com `request.coupon() != null` (`CheckoutService.java:276-278`), sem nomear o Pix. Isso também respeita o "mesmo que o cupom tenha dado desconto zero", porque o teste é se o cupom veio, e não o valor do desconto.

Conclusão: acerto

**R4, o WELCOME10 só na primeira compra.** A decisão final está no cupom (`Welcome10Coupon.java:18`), mas a checagem do histórico está no serviço: `CheckoutService.java:239-242` faz a consulta e já embute quais situações contam como compra anterior. É uma forma mais branda do remendo, porque o serviço não nomeia o WELCOME10. Mesmo assim, a regra do caso ficou dividida entre a casa dele e o serviço, e o cupom não consulta os pedidos.

Conclusão: malfeito

**P5-forma, que forma a variação da região tem.** `Region` é um enum só de rótulos (`domain/Region.java:3-9`). Os números moram em dois `switch` que nomeiam as regiões:
- a porcentagem e o limite do seguro, num `switch` que devolve um `record InsuranceRate(percentage, maxAmount)` da mesma classe para todas as regiões (`service/InsuranceService.java:22-30,32`), com uma conta única (`:11-20`);
- os dias a mais, num segundo `switch` que devolve só o número (`NORTH -> 3`, `NORTHEAST -> 2`, `default -> 0`), dentro de `TransLogShipping.java:50-56`.

Não há classe por região nem constante com corpo.

Conclusão: contenção

**P5-proporcao, se a resposta tem o tamanho do problema.** Tem: nenhuma região ganhou comportamento próprio, e cada `switch` só devolve números (ou um objeto só de dados, o mesmo para todas), com uma conta só para todas. É a forma aceitável, mas é condicional e não dado. E os números de uma mesma região ficaram espalhados em dois arquivos de pacotes diferentes, então uma região nova muda dois `switch`. Um enum `Region` com os três números (porcentagem, limite e dias a mais) seria a tabela do tamanho do problema.

Conclusão: contenção

**E, estrutura sem necessidade fora dos pontos.** Perto deste pattern, não achei interface com uma implementação só, camada que só repassa, nem fábrica ou builder sem uso. Os registros de contexto (`ShippingContext`, `CouponContext`, `PaymentContext`) e de resultado servem às estratégias. A classe-base `TransLogShipping` tem duas subclasses reais. `Money` e `Weight` são objetos de valor usados em todo o cálculo. O `InsuranceService` é uma classe concreta só com o seguro, sem interface.

Conclusão: contenção

## O que mudaria primeiro

1. **Levar a regra do WELCOME10 para dentro do cupom.** O `Welcome10Coupon` receberia o repositório de pedidos (ou uma consulta por CPF) e decidiria ali o que conta como primeira compra. O `isFirstPurchase` sairia do `CouponContext` e do `CheckoutService.java:239-242`. Assim, a regra do caso ficaria inteira na casa dele, e o serviço deixaria de consultar o banco para todo cupom.
2. **Juntar os números da região num lugar só**, como dado no enum `Region` (porcentagem, limite, dias a mais) ou numa tabela. Isso tiraria os dois `switch` de `InsuranceService.java:22-30` e `TransLogShipping.java:50-56`. Não é exagero nem omissão do padrão, mas hoje uma região nova muda dois lugares.

## Dúvidas

- **R4 e P2-local.** A régua diz que uma função genérica que não cita o caso, mesmo que só um caso a use, não conta como código fora da casa. E diz que a checagem do histórico no serviço é o remendo. A linha `CheckoutService.java:239-242` não cita o WELCOME10, mas guarda a definição que só ele usa. Escolhi `malfeito`, porque ali mora uma regra de negócio do caso e não um mecanismo genérico da estrutura (como seria um método `default` ou uma classe-base). Uma leitura mais literal da exceção daria `acerto` nos dois itens.
- **R2.** A recusa por "frete não positivo" em vez de "entrega é retirada" foi julgada só pelo lugar onde mora, que é a casa do cupom. A diferença de resultado com o GOLD fica para a dimensão do comportamento.
