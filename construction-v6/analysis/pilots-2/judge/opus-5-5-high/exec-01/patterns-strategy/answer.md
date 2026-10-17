# Padrões: Strategy

## Visão geral

O agente não usou o Strategy em nenhum dos quatro pontos que pedem o padrão. Entrega, cupom, forma de pagamento e nível do clube são enums sem corpo, só rótulos (`entity/ShippingMethod.java`, `entity/ClubTier.java`, `entity/PaymentMethod.java`), ou texto solto, no caso do cupom. Todo o comportamento de cada caso está em cadeias de `if` que nomeiam os casos dentro de um único `PricingService`. Uma parte das regras de cada caso também está espalhada no `ValidationService`.

Em ordem de gravidade:

1. **A entrega e o cupom**, os dois pontos que o enunciado diz que vão crescer toda semana, são os mais espalhados. Uma entrega nova exige mexer no frete, no prazo por região e na tradução para a TransLog. Um cupom novo exige mexer no cálculo, na lista de válidos e na validação de aplicabilidade.
2. **As regras cruzadas** R2, R3 e R4 estão todas como `if` num serviço, fora de qualquer casa.
3. **A regra do frete grátis do GOLD (R1)** não existe no código.

Há um ponto positivo. Na região (o controle negativo), a resposta ficou simples, um `switch` que só devolve números, sem estrutura por região. E não há estrutura desnecessária perto do pattern.

## Item a item

**P1-local, a entrega.** Os casos não têm casa: `ShippingMethod` é um enum só com as quatro constantes (`src/main/java/com/loja/orderservice/entity/ShippingMethod.java:3-8`). Todo o código específico de cada caso está fora dele:
- o STORE_PICKUP, grátis e com 1 dia, em `src/main/java/com/loja/orderservice/service/PricingService.java:148-150`;
- o COURIER, com o limite de 5 kg, os R$ 18,00 e os 0 dias, em `PricingService.java:152-157`;
- a tradução ECONOMY/EXPRESS para `STD`/`EXP`, em `PricingService.java:160`. Ela não fica no lado da TransLog: está no cálculo do frete, e todo caso que não é ECONOMY vira `EXP`;
- os dias a mais do Norte e do Nordeste, só para ECONOMY e EXPRESS, em `PricingService.java:166-170`;
- a regra do FREESHIPPING que nomeia o STORE_PICKUP, em `src/main/java/com/loja/orderservice/service/ValidationService.java:190`.

Uma entrega nova mexe em pelo menos três desses lugares.
Conclusão: omissão

**P1-selecao, a entrega.** O caso é escolhido por `if (method == ShippingMethod.STORE_PICKUP)` e `if (method == ShippingMethod.COURIER)`. Os outros caem por exclusão na TransLog, e a conta é feita ali mesmo (`PricingService.java:148-172`). É uma condicional no cálculo.
Conclusão: omissão

**P2-local, o cupom.** O cupom nem chega a ser um tipo: é uma `String` comparada com literais. O código de cada caso fica fora de qualquer casa:
- os 10% do WELCOME10, em `PricingService.java:93-94`;
- os R$ 50,00 e o mínimo de R$ 300,00 do MINUS50, em `PricingService.java:95-98`;
- o desconto igual ao frete do FREESHIPPING, que recalcula o frete, em `PricingService.java:99-101`;
- o BUY3PAY2, em `PricingService.java:102-103`, com o método próprio em `PricingService.java:114-130`;
- a primeira compra do WELCOME10, em `ValidationService.java:182-187`;
- a retirada do FREESHIPPING, em `ValidationService.java:189-193`.

A lista de válidos em `ValidationService.java:172-175` não conta, porque só confere se o código existe. O teto de 30% em `PricingService.java:106-109` é genérico e também não conta. Um cupom novo mexe em três lugares, em dois arquivos.
Conclusão: omissão

**P2-selecao, o cupom.** É a cadeia `if ("WELCOME10".equals(coupon)) … else if ("BUY3PAY2"…)`, que nomeia os casos e faz a conta no mesmo método (`PricingService.java:93-104`). As condições de aplicabilidade são escolhidas da mesma forma, em `ValidationService.java:182-193`. É uma condicional no cálculo.
Conclusão: omissão

**P3-local, a forma de pagamento.** `PaymentMethod` é um enum sem corpo (`src/main/java/com/loja/orderservice/entity/PaymentMethod.java:3-7`). O código de cada caso está espalhado:
- o desconto do PIX, em `PricingService.java:247-250`;
- a tarifa do BANK_SLIP, em `PricingService.java:251-253`;
- os juros do CREDIT_CARD, em `PricingService.java:254-264`;
- a recusa do boleto acima de R$ 1.000,00, em `PricingService.java:220-226`;
- as parcelas permitidas para PIX/BANK_SLIP e para CREDIT_CARD, em `ValidationService.java:144-152`;
- um método vazio que nomeia o BANK_SLIP, em `ValidationService.java:155-159`.

As regras de cada forma ficam em três métodos de dois serviços.
Conclusão: omissão

**P3-selecao, a forma de pagamento.** É a cadeia `if (method == PaymentMethod.PIX && !hasCoupon) … else if (method == BANK_SLIP) … else if (method == CREDIT_CARD)`, que faz a conta ali (`PricingService.java:247-265`). É uma condicional no cálculo.
Conclusão: omissão

**P4-local, o nível do clube.** `ClubTier` é um enum sem corpo, sem nem os números de cada nível (`src/main/java/com/loja/orderservice/entity/ClubTier.java:3-7`). O código de cada caso está fora dele:
- o crédito de 2% do SILVER e de 5% do GOLD, em `PricingService.java:283-290`;
- o brinde do GOLD acima de R$ 500,00, em `PricingService.java:292-297`.

A vantagem do frete grátis do GOLD não existe no código: a busca por `GOLD` em `src/main/java` só acha `PricingService.java:286` e `:293`, e o cálculo do frete (`PricingService.java:142-179`) não recebe o nível. Julgo pelos casos que existem: as vantagens de cada nível estão em métodos genéricos que perguntam qual é o nível.
Conclusão: omissão

**P4-selecao, o nível do clube.** O nível é escolhido por `if (tier == ClubTier.SILVER) … else if (tier == ClubTier.GOLD)` no cálculo do crédito (`PricingService.java:284-288`) e por `if (tier == ClubTier.GOLD && …)` no brinde (`PricingService.java:293`). Nos dois casos a conta é feita ali. É uma condicional no cálculo.
Conclusão: omissão

**R1, o GOLD não paga frete, exceto no motoboy.** A regra não foi implementada. Não há nenhum ponto do código que zere o frete do GOLD: o cálculo do frete (`PricingService.java:142-179`) não lê `getClubTier()`, e as únicas citações a `GOLD` são o crédito e o brinde (`PricingService.java:286`, `:293`). Também não há remendo para julgar. Que o serviço não dá o frete grátis é assunto do comportamento.
Conclusão: não se aplica

**R2, o FREESHIPPING não vale na retirada.** A regra está em `if ("FREESHIPPING".equals(coupon)) { if (request.getShippingMethod() == ShippingMethod.STORE_PICKUP) throw … }`, no `ValidationService` (`ValidationService.java:189-193`). É um terceiro lugar, fora de qualquer casa do cupom, porque a casa nem existe. É o remendo, sem estrutura por baixo.
Conclusão: omissão

**R3, o desconto do Pix só sem cupom.** O serviço calcula `hasCoupon` (`PricingService.java:48`) e o cálculo testa `method == PaymentMethod.PIX && !hasCoupon` (`PricingService.java:247`). É exatamente o remendo previsto, no cálculo geral.
Conclusão: omissão

**R4, o WELCOME10 só na primeira compra.** A checagem do histórico está no `ValidationService`: `if ("WELCOME10".equals(coupon))` chama `orderRepository.findCompletedOrdersByCpf(...)` (`ValidationService.java:182-187`), com a consulta em `src/main/java/com/loja/orderservice/repository/OrderRepository.java:13-14`. A regra está num serviço, fora de qualquer casa do cupom.
Conclusão: omissão

**P5-forma, a região.** `Region` é um enum sem corpo (`src/main/java/com/loja/orderservice/entity/Region.java:3-9`). A porcentagem e o limite do seguro saem de um `switch (region)` que só atribui números (`PricingService.java:197-209`), e há um único cálculo genérico depois (`PricingService.java:211-215`). Os dias a mais saem de um `if/else if` sobre `Region.NORTH`/`Region.NORTHEAST` que só soma 3 ou 2 (`PricingService.java:166-170`). Esse `if` também nomeia ECONOMY/EXPRESS, o que já entrou no P1-local. A forma é um `switch`, mais uma condicional, que devolvem números.
Conclusão: contenção

**P5-proporcao, a região.** Não há classe por região, constante com corpo nem objeto com comportamento próprio: só números e uma conta para todas as regiões. A variação é condicional, não dado, como a régua admite. A resposta tem o tamanho do problema.
Conclusão: contenção

**E, estrutura sem necessidade fora dos pontos.** Não achei nada perto do pattern:
- `config/WebClientConfig.java` só registra o `RestTemplate` que o cliente da TransLog usa;
- `PricingUtil` é um conjunto de funções estáticas de arredondamento;
- `ShippingCostInfo` e `PaymentAdjustmentInfo` (`PricingService.java:132-140`, `:228-240`) só carregam resultados.

Não há interface com uma implementação só, camada de repasse nem fábrica. O que existe de sobra é código morto, não estrutura: o método vazio `validatePaymentMethodAvailability` (`ValidationService.java:155-159`) e o `ClubTier.valueOf(tier.toString())` redundante (`ValidationService.java:110-114`).
Conclusão: contenção

## O que mudaria primeiro

1. **Entrega e cupom como estratégias.** Uma interface `ShippingOption` (preço, prazo, disponibilidade) e uma interface `Coupon` (desconto, aplicabilidade a partir de um contexto com o pedido, a entrega e o histórico do cliente), com uma implementação por caso e escolhidas por consulta num `Map` de beans. São os dois pontos que o enunciado diz que crescem toda semana, e hoje cada caso novo mexe em dois ou três métodos de dois serviços. Isso também levaria as regras R2 e R4 do `ValidationService` para as casas do FREESHIPPING e do WELCOME10.
2. **Nível do clube e forma de pagamento com comportamento próprio.** Um enum `ClubTier` com as porcentagens e o frete grátis como dado (e a exceção do motoboy dentro do GOLD) daria o lugar natural para a regra R1, que hoje nem existe. Um `PaymentMethod` com o ajuste, as parcelas e a recusa por caso tiraria o `&& !hasCoupon` do cálculo (R3).

## Dúvidas

- **R1:** a regra do frete grátis do GOLD está no `prompt.md`, mas não existe no código. Segui a parte comum da régua para "um caso que não existe no código" e marquei `não se aplica`, porque não há onde julgar o remendo. A falta do frete grátis fica para a dimensão do comportamento.
- **Remendos sem estrutura (R2 a R4):** a régua define o malfeito como "a estrutura existe, mas algum caso escapa". Aqui não há estrutura nenhuma, então dei `omissão`, que descreve a variação escrita com `if` sobre os casos.
