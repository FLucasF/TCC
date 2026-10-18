# Padrões: Strategy

## Visão geral

O agente não usou o Strategy em nenhum dos quatro pontos que pedem o padrão. Os enums de entrega, de pagamento e de nível do clube só dão nome aos casos, e o cupom é um `String`. Toda a variação mora em cadeias de `if` dentro de um único serviço, o `CheckoutService`, que nomeia os casos no próprio cálculo.

Os problemas mais sérios, do mais grave para o menos grave:

1. **O nível do clube (P4).** As vantagens do `GOLD` ficam espalhadas por três pontos do serviço: o crédito, o frete e o brinde.
2. **A entrega (P1).** Ela é decidida em dois blocos condicionais que se sobrepõem. O prazo da retirada está escrito duas vezes, e a tradução para o código da TransLog fica no serviço, não no cliente da transportadora.
3. **O cupom (P2) e o pagamento (P3).** Os dois são `if` sobre os casos, e as recusas de cada caso ficam no meio da conta ou na validação geral.

Das regras cruzadas, só a do `GOLD` sem frete (R1) existe no código, e no lugar do remendo: no cálculo geral, sem a exceção do motoboy. As outras três não foram escritas. O FREESHIPPING é sempre recusado, o Pix dá desconto mesmo com cupom e o WELCOME10 não consulta o histórico.

O ponto positivo está no controle negativo (P5). O seguro por região foi resolvido com um `switch` que só devolve números, sem estrutura por região.

As linhas citadas sem pasta ficam em `src/main/java/com/loja/orderservice/`.

## Item a item

**P1-local, a entrega: onde mora o código de cada caso?** Os casos não têm casa: `ShippingMethod` é um enum só com os nomes (`domain/enums/ShippingMethod.java:3-8`). O código de cada entrega está espalhado pelo serviço:
- **O motoboy:** o limite de 5 kg e a tarifa de R$ 18,00 ficam numa cadeia de `if` (`service/CheckoutService.java:265-269`, com as constantes em `:24-25` e `:32`).
- **A retirada:** ela aparece duas vezes. Uma no bloco de `calculateSummary` (`service/CheckoutService.java:50`, `:56`, `:61-62`). Outra em `calculateShipping` (`:270-271`), um ramo que nunca é alcançado porque `calculateSummary` desvia o `STORE_PICKUP` antes.
- **A econômica e a expressa:** ficam juntas num ramo (`:272-294`). A tradução `ECONOMY ? "STD" : "EXP"` está no serviço (`:273`), e não do lado da TransLog (`integration/TransLogClient.java:24` recebe o `service` já traduzido). Por isso ela conta aqui como código da entrega fora da casa.
- **Os dias a mais do Norte e do Nordeste:** são somados com `if` sobre a região dentro do ramo das duas entregas da TransLog (`:288-292`).

Nenhum caso tem um lugar só dele.
Conclusão: omissão

**P1-selecao, a entrega: como o caso é escolhido?** O `shippingMethod` chega como enum (`dto/CheckoutSummaryRequest.java:16`), mas a conta é escolhida por condicionais que nomeiam os casos e calculam ali mesmo: `if (method == ShippingMethod.COURIER) … else if (method == STORE_PICKUP) … else if (method == ECONOMY || method == EXPRESS)` (`service/CheckoutService.java:265-295`). Há ainda o bloco anterior em `calculateSummary` (`:50-63`), que decide entre retirada e não retirada antes de chamar o cálculo. É condicional no cálculo, nos dois lugares.
Conclusão: omissão

**P2-local, o cupom: onde mora o código de cada caso?** O cupom é um `String` (`dto/CheckoutSummaryRequest.java:17`), sem casa. A lista de válidos em `isValidCoupon` (`service/CheckoutService.java:205-208`) não conta, porque só confere se o código existe. O código específico de cada cupom fica no serviço:
- **WELCOME10:** `:220-221`.
- **MINUS50:** o piso de R$ 300,00 e a recusa `COUPON_NOT_APPLICABLE` dentro do ramo (`:222-226`).
- **FREESHIPPING:** um ramo que sempre recusa o cupom (`:227-228`).
- **BUY3PAY2:** o método `calculateBuy3Pay2Discount`, privado do serviço (`:241-258`).

O teto de 30% (`:233-236`) é genérico e não nomeia nenhum caso.
Conclusão: omissão

**P2-selecao, o cupom: como o caso é escolhido?** Por uma cadeia `if ("WELCOME10".equals(coupon)) … else if ("MINUS50"…) … else if ("FREESHIPPING"…) … else if ("BUY3PAY2"…)` que faz a conta de cada um ali mesmo (`service/CheckoutService.java:220-231`). É condicional no cálculo.
Conclusão: omissão

**P3-local, a forma de pagamento: onde mora o código de cada caso?** `PaymentMethod` só tem os nomes (`domain/enums/PaymentMethod.java:3-7`). O código de cada forma está fora de qualquer casa:
- **O ajuste no cálculo:** o desconto do Pix (`service/CheckoutService.java:343-344`), a tarifa do boleto (`:345-346`) e a tabela Price do cartão, com a taxa `0.0199` escrita direto no ramo (`:347-360`).
- **A regra de parcelas:** fica na validação geral, com `if (… == PaymentMethod.PIX || … == PaymentMethod.BANK_SLIP)` (`:148-152`). É uma validação que nomeia os casos, fora da casa.

A recusa do boleto acima de R$ 1.000,00 (`PAYMENT_METHOD_UNAVAILABLE`) não existe no código: a busca pelo código de erro não acha nada.
Conclusão: omissão

**P3-selecao, a forma de pagamento: como o caso é escolhido?** Por `if (method == PaymentMethod.PIX) … else if (method == BANK_SLIP) … else if (method == CREDIT_CARD)`, que calcula em cada ramo (`service/CheckoutService.java:343-362`). É condicional no cálculo.
Conclusão: omissão

**P4-local, o nível do clube: onde mora o código de cada caso?** `ClubTier` só tem os nomes (`domain/enums/ClubTier.java:3-7`). Busquei os níveis e também o que eles fazem (crédito, frete, brinde), e as vantagens do `GOLD` estão em três lugares do serviço:
- **O crédito:** `calculateNextPurchaseCredit` dá 0%, 2% e 5% com `if` sobre `BRONZE`, `SILVER` e `GOLD` (`service/CheckoutService.java:370-380`).
- **O frete grátis:** fica em `calculateSummary`, com `request.getClubTier() != ClubTier.GOLD` e `== ClubTier.GOLD` (`:50-60`).
- **O brinde:** `request.getClubTier() == ClubTier.GOLD && productsSubtotal > 500.00` (`:80-81`).

Um nível novo mexeria nesses três pontos.
Conclusão: omissão

**P4-selecao, o nível do clube: como o caso é escolhido?** O `clubTier` chega como enum (`dto/CheckoutSummaryRequest.java:20`), e cada vantagem é decidida por uma comparação que nomeia o nível e faz a conta ali (`service/CheckoutService.java:50-60`, `:80`, `:371-377`). É condicional no cálculo, em três lugares.
Conclusão: omissão

**R1, o GOLD não paga frete, exceto no motoboy.** A regra está no cálculo geral do resumo, que é o lugar do remendo: `if (shippingMethod != STORE_PICKUP && clubTier != GOLD) { … cobra … } else if (shippingMethod != STORE_PICKUP && clubTier == GOLD) { … só o prazo … }` (`service/CheckoutService.java:50-60`). A exceção do motoboy não foi escrita: o ramo do `GOLD` zera o frete em qualquer entrega, inclusive `COURIER`. Não há nível nem política de frete que possa guardar a regra.
Conclusão: omissão

**R2, o FREESHIPPING não vale na retirada.** A regra cruzada não existe no código. Nenhuma linha relaciona `FREESHIPPING` com `STORE_PICKUP` (a busca pelos dois nomes só acha as linhas citadas em P1 e P2). O cupom é recusado sempre, em qualquer entrega: `else if ("FREESHIPPING".equals(coupon)) { throw … "COUPON_NOT_APPLICABLE" … }` (`service/CheckoutService.java:227-228`). Essa recusa nomeia o cupom no serviço e já pesa contra o P2-local, mas não é a regra da retirada. Como não há regra escrita, não há lugar para julgar (veja as Dúvidas).
Conclusão: não se aplica

**R3, o desconto do Pix só sem cupom.** A regra não existe no código. O ramo do Pix dá 5% sem olhar o cupom: `calculatePaymentAdjustment` nem recebe o cupom (`service/CheckoutService.java:340-344`, chamado em `:70-71`). Não há lugar para julgar.
Conclusão: não se aplica

**R4, o WELCOME10 só na primeira compra.** A regra não existe no código. `calculateCouponDiscount` recebe o `cpf` (`service/CheckoutService.java:213`), mas não o usa: o ramo do WELCOME10 só aplica os 10% (`:220-221`). O `OrderRepository` não tem consulta pelo histórico do CPF (`repository/OrderRepository.java:9-11`), e nem o controller nem o `OrderService` conferem o histórico. Não há lugar para julgar.
Conclusão: não se aplica

**P5-forma, a região: que forma a variação tem?** `Region` é um enum só com os nomes (`domain/enums/Region.java:3-9`). A porcentagem e o limite do seguro vêm de um `switch` que só devolve números (`service/CheckoutService.java:317-329`), seguido de uma conta única para todas as regiões (`:331-335`). Os dias a mais do Norte e do Nordeste são constantes (`:29-30`) somadas por um `if` sobre a região (`:288-292`). A forma, portanto, é um `switch` (e um `if`) que devolve números.
Conclusão: contenção

**P5-proporcao, a região: a resposta tem o tamanho do problema?** Tem. Não há classe por região, nem constante com corpo, nem objeto com comportamento por região: só números, numa conta única. A régua aceita o `switch`, mas lembra que é condicional, não dado. Uma tabela no próprio enum (a porcentagem, o limite e os dias em cada constante) tiraria o `switch` e o `if`. O `default -> percentage = BigDecimal.ZERO` (`:328`) nunca é alcançado, mas não é estrutura.
Conclusão: contenção

**E, estrutura sem necessidade fora dos pontos.** Não encontrei nenhuma. As únicas interfaces do projeto são os repositórios do Spring Data (`repository/*.java`), que são o mecanismo de acesso ao banco. O `TransLogClient` é uma classe concreta, sem interface sobrando (`integration/TransLogClient.java:18`). `ShippingInfo` é só um par de valores, privado (`service/CheckoutService.java:382-390`). Os builders vêm do Lombok, não de código escrito à mão. O método `findByIdAndCustomerCpf` não é usado (`repository/OrderRepository.java:10`), mas é código morto, não estrutura.
Conclusão: contenção

## O que mudaria primeiro

1. **Dar casa a cada nível do clube (P4).** Por exemplo, um `ClubTier` com a porcentagem de crédito e com métodos como `shippingCost(método, frete)` e `gift(subtotal)` em cada constante, ou uma interface com uma implementação por nível. Isso junta num lugar só as três vantagens que hoje estão espalhadas (`:50-60`, `:80`, `:371-377`). Também dá à exceção do motoboy (R1) o lugar certo, dentro do `GOLD`, em vez de mais um `&&` no cálculo geral. É o ponto em que o enunciado avisa que os níveis vão crescer.
2. **Dar casa a cada entrega (P1) e a cada cupom (P2).** Por exemplo, uma interface `ShippingOption` com uma implementação por entrega, achada por um mapa a partir do código. A tradução `STD`/`EXP` iria para o lado da TransLog. Isso acaba com o prazo da retirada escrito duas vezes e com a dupla de condicionais (`:50-63` e `:265-295`). O mesmo vale para os cupons: com cada um na sua classe e o contexto do pedido (a entrega, o histórico do CPF), a regra do FREESHIPPING na retirada (R2) e a da primeira compra (R4) teriam onde morar.

## Dúvidas

- **R2, R3 e R4 com `não se aplica`.** As três regras estão no `prompt.md` desta execução, mas não existem no código. A régua pergunta onde o agente pôs cada regra. Como ele não pôs em lugar nenhum, não há remendo nem lugar certo para julgar. Escolhi `não se aplica`, seguindo a orientação para o que não existe no código: julgar o desenho pelo que existe e deixar a falta para a dimensão do comportamento. O efeito no desenho já aparece em P2 e P3, que são omissão. Uma leitura mais dura daria `omissão`, com o argumento de que o desenho não tem onde guardar a regra.
- **A tradução para a TransLog.** A régua diz que a tradução `EXPRESS -> "EXP"` não espalha a entrega quando está do lado da TransLog. Aqui ela está no `CheckoutService` (`:273`), por isso a contei como código da entrega fora da casa, em P1-local.
