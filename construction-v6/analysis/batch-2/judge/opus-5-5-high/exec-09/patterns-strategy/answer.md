Já li o enunciado e todo o código principal, e fiz a busca pelos nomes de cada caso. Segue a correção.

# Padrões: Strategy

## Visão geral

O agente não usou Strategy em nenhum dos quatro pontos que pedem o padrão. A entrega, o cupom, a forma de pagamento e o nível do clube são enums que só servem de rótulo (`src/main/java/com/loja/models/Enums.java:4-18`). Às vezes nem isso: o código compara o texto da requisição direto (`req.coupon.equals("WELCOME10")`). Todo o comportamento de cada caso fica numa classe só, `src/main/java/com/loja/services/CheckoutService.java`, em cadeias de `if`/`else if` que nomeiam os casos dentro do cálculo. O problema mais sério vem disso: várias regras de cada caso estão escritas **duas vezes**, uma na validação e outra no cálculo. O limite de 5 kg do motoboy está nas linhas 127 e 351, as condições do WELCOME10 nas linhas 171-175 e 284-288, o FREESHIPPING na retirada nas linhas 181 e 300. Um caso novo mexe em três ou quatro métodos. As quatro regras cruzadas caíram todas no serviço, como remendos. O lado bom é a contenção. O projeto não criou nenhuma estrutura desnecessária, e a região, que é o controle negativo, foi resolvida como números num `switch`, sem uma classe por região.

## Item a item

**P1-local, a entrega: onde mora o código de cada caso.** Nenhuma entrega tem casa própria: `ShippingMethod` é um enum sem corpo (`src/main/java/com/loja/models/Enums.java:12-14`). O código específico de cada caso está espalhado pelo `CheckoutService`:
- o COURIER aparece com o limite de 5 kg na validação (`src/main/java/com/loja/services/CheckoutService.java:121-130`) e de novo no cálculo, com o preço de R$ 18,00 e o prazo 0 (`:350-355`);
- o STORE_PICKUP aparece com frete 0 e prazo 1 (`:346-348`);
- o ECONOMY e o EXPRESS chamam a TransLog na validação (`:132-153`) e de novo no cálculo (`:357-385`), cada vez com a tradução `method == ECONOMY ? "STD" : "EXP"` (`:144`, `:362`).

Não existe um lado TransLog separado (não há adaptador), então essa tradução fica no meio do cálculo e não conta como trabalho de adaptador. Os casos também aparecem nas regras dos outros pontos (`:181`, `:225-226`, `:300`). Cada linha listada faz algo só para uma entrega.
Conclusão: omissão

**P1-selecao, a entrega: como o caso é escolhido.** O `Enums.ShippingMethod.valueOf(req.shippingMethod)` (`src/main/java/com/loja/services/CheckoutService.java:338`) só converte o texto num rótulo. Quem decide e faz a conta são os `if (method == ...)` em `calculateShipping` (`:346`, `:350`, `:362`) e em `validateShippingMethod` (`:121`, `:132`). É uma condicional no cálculo.
Conclusão: omissão

**P2-local, o cupom: onde mora o código de cada caso.** O cupom não existe nem como enum, é só texto comparado no serviço. As condições de cada cupom estão em `validateCouponApplicability` (`src/main/java/com/loja/services/CheckoutService.java:171-184`). A conta de cada um está em `calculateCouponDiscount` (`:284-318`), que repete as mesmas condições: WELCOME10 em `:285-288`, MINUS50 em `:293-295`, FREESHIPPING em `:300-302`. A lista de válidos em `isCouponValid` (`:455-457`) só confere se o código existe e não conta. O teto de 30% (`:210-213`) é genérico e também não conta. Todo o resto é código de cada cupom fora de uma casa que não existe.
Conclusão: omissão

**P2-selecao, o cupom: como o caso é escolhido.** Uma cadeia de `if (coupon.equals("..."))` que calcula e devolve o desconto ali mesmo (`src/main/java/com/loja/services/CheckoutService.java:284-320`). É uma condicional no cálculo.
Conclusão: omissão

**P3-local, a forma de pagamento: onde mora o código de cada caso.** `PaymentMethod` é um enum sem corpo (`src/main/java/com/loja/models/Enums.java:16-18`). O código de cada forma está espalhado:
- o desconto do Pix fica em `src/main/java/com/loja/services/CheckoutService.java:240-243`;
- a tarifa do boleto está copiada duas vezes (`:244-246` e `:248-250`), porque o `if` do cupom envolve os dois casos;
- os juros do cartão ficam em `:254-258`;
- a regra de parcelas do Pix e do boleto fica em `isInstallmentsValid` (`:449-450`);
- a recusa do boleto acima de R$ 1.000 fica em `validatePaymentMethod` (`:190-194`).
Conclusão: omissão

**P3-selecao, a forma de pagamento: como o caso é escolhido.** `if (req.paymentMethod.equals("PIX"))`, `equals("BANK_SLIP")` e `equals("CREDIT_CARD")` fazem a conta dentro de `calculateSummary` (`src/main/java/com/loja/services/CheckoutService.java:239-262`). É uma condicional no cálculo. O `valueOf` da linha `:91` só valida.
Conclusão: omissão

**P4-local, o nível do clube: onde mora o código de cada caso.** `ClubTier` é um enum sem corpo e sem dados (`src/main/java/com/loja/models/Enums.java:4-6`). As porcentagens nem moram no enum. O crédito do SILVER e do GOLD fica em `src/main/java/com/loja/services/CheckoutService.java:265-270`, o brinde do GOLD em `:272` e o frete grátis do GOLD em `:224-228`, todos no serviço. A busca por `credit` e `gift` no resto do projeto só acha persistência (`OrderService`, `Database`), nenhuma regra.
Conclusão: omissão

**P4-selecao, o nível do clube: como o caso é escolhido.** `if (tier == SILVER) ... else if (tier == GOLD)` e `tier == GOLD && subtotal > 500` fazem a conta em `calculateSummary` (`src/main/java/com/loja/services/CheckoutService.java:224`, `:266-272`). É uma condicional no cálculo.
Conclusão: omissão

**R1, o GOLD não paga frete, exceto no motoboy.** A regra está em `calculateSummary` como `if (ClubTier.valueOf(req.clubTier) == GOLD && !req.shippingMethod.equals("COURIER") && !req.shippingMethod.equals("STORE_PICKUP")) shippingCost = 0` (`src/main/java/com/loja/services/CheckoutService.java:224-228`). É exatamente o remendo descrito na régua: um `if` no cálculo geral que nomeia o nível e a entrega. Ele ainda cita o STORE_PICKUP, que já tem frete 0. Como não existe estrutura que a regra pudesse contornar, classifico como omissão, e não como malfeito.
Conclusão: omissão

**R2, o FREESHIPPING não vale na retirada.** A regra aparece duas vezes no serviço: `if (req.shippingMethod.equals("STORE_PICKUP"))` na validação (`src/main/java/com/loja/services/CheckoutService.java:180-183`) e de novo no cálculo do desconto (`:299-302`). Não há casa do cupom onde ela pudesse morar. É o remendo, num terceiro lugar.
Conclusão: omissão

**R3, o desconto do Pix só sem cupom.** `if (req.coupon == null || req.coupon.isEmpty()) { if (req.paymentMethod.equals("PIX")) ... }` em `calculateSummary` (`src/main/java/com/loja/services/CheckoutService.java:239-243`). É o remendo no serviço. O efeito colateral aparece logo abaixo: a tarifa do boleto teve de ser copiada no `else` (`:248-250`).
Conclusão: omissão

**R4, o WELCOME10 só na primeira compra.** A consulta ao histórico (`hasCompletedOrder`, com o SQL em `src/main/java/com/loja/services/CheckoutService.java:323-335`) é chamada no serviço, na validação (`:171-175`) e de novo no cálculo (`:284-288`). Não há um cupom que receba o contexto e faça a consulta. É o remendo no serviço.
Conclusão: omissão

**P5-forma, a região.** A porcentagem e o limite do seguro saem de um `switch (region)` que só atribui dois números por região (`src/main/java/com/loja/services/CheckoutService.java:425-436`), seguido de uma conta única para todas (`:438-441`). Os dias a mais do Norte e do Nordeste saem de um `if/else if` que só atribui o número (`:378-383`). O enum `Region` não tem corpo (`src/main/java/com/loja/models/Enums.java:8-10`). A forma é um `switch` (e um `if`) que devolve números.
Conclusão: contenção

**P5-proporcao, a região.** Não há classe por região, nem constante com corpo, nem objeto com comportamento próprio. A variação ficou como números e a conta é uma só. Pela régua, isso é aceitável sem ser a forma ideal: é uma condicional, e não dado. Um enum com a porcentagem, o limite e os dias a mais deixaria tudo num lugar só. Ainda assim é do tamanho do problema.
Conclusão: contenção

**E, estrutura sem necessidade fora dos pontos.** Não há nenhuma: zero `interface`, `abstract`, `implements` ou `extends` em `src/main/java`. Os serviços são classes com métodos estáticos chamados direto por `src/main/java/com/loja/api/Routes.java`. As classes aninhadas `ValidationError`, `ShippingInfo` e `TranslogResponse` (`src/main/java/com/loja/services/CheckoutService.java:460-484`) só carregam dados. Também não há builder nem fábrica.
Conclusão: contenção

## O que mudaria primeiro

1. **Criar uma estratégia para o cupom e outra para a entrega**, cada uma com a sua validação e a sua conta. Por exemplo, uma interface `Coupon` com um método que valida o contexto e outro que calcula o desconto, e uma interface `ShippingOption` com a cotação, o prazo e a disponibilidade, escolhidas por um `Map` a partir do código. As regras R2 e R4 iriam para dentro do FREESHIPPING e do WELCOME10. São justamente os dois pontos em que o enunciado avisa que o crescimento é toda semana, e hoje cada caso novo exige mexer em três ou quatro métodos e repetir a mesma condição na validação e no cálculo.
2. **Dar corpo aos enums `ClubTier` e `PaymentMethod`**: uma porcentagem de crédito, um `freeShipping(method)` e um `gift(subtotal)` por nível, e um `adjust(total, context)` com as parcelas permitidas por forma de pagamento. Isso tiraria o R1 e o R3 do `calculateSummary` e eliminaria a tarifa do boleto copiada duas vezes.

## Dúvidas

- **Conclusão dos remendos sem estrutura.** A régua define malfeito como "a estrutura existe, mas algum caso escapa". Aqui não existe estrutura nenhuma, e as regras cruzadas estão no serviço junto com todo o resto. Por isso usei omissão nos itens R1 a R4, e não malfeito.
- **Tradução `ECONOMY -> "STD"`.** A régua só a desconta quando está do lado da TransLog. Neste projeto não há esse lado: a chamada HTTP e a tradução estão no próprio `CheckoutService` (`:144`, `:362`, `:388-418`). Contei a tradução como código específico da entrega fora de casa. Isso não muda a conclusão do P1, que seria omissão de qualquer forma.
