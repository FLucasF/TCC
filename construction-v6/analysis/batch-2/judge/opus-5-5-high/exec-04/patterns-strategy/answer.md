# Padrões: Strategy

## Visão geral

Nenhum dos quatro pontos que pedem o Strategy tem estrutura. A entrega, o cupom, a forma de pagamento e o nível do clube viraram enums que são só rótulos (`ShippingMethod`, `PaymentMethod`, `ClubTier`). O cupom nem isso: é um `String`. Toda a variação está em `switch` e `if` que nomeiam os casos dentro de `CheckoutService` e de `Validation`. Isso põe o código de cada caso em dois ou três lugares. Na entrega, por exemplo, a cotação na TransLog é escrita três vezes: no frete, no prazo e na validação. As quatro regras cruzadas (R1 a R4) ficaram no serviço ou na validação, como condicionais sobre os dois casos. O que está bom é o controle negativo: a região é um enum que carrega os números do seguro, com uma conta única para todas as regiões. Os dias a mais vêm de um `switch` que só devolve números. Não há estrutura sobrando em parte nenhuma do projeto. Em ordem de gravidade: (1) entrega e cupom, os dois pontos que o enunciado diz que vão crescer, foram escritos sem padrão e espalhados por vários arquivos; (2) o nível do clube espalha as vantagens do GOLD por três métodos; (3) a forma de pagamento repete o mesmo problema, com menos peso porque o enunciado não diz que ela vai crescer.

## Item a item

**P1-local, a entrega: onde mora o código de cada caso.** `ShippingMethod` é um enum sem corpo (`model/ShippingMethod.java:3-7`). Os casos não têm casa, então todo o código de cada caso está fora dela:
- o frete: `case STORE_PICKUP -> 0`, `case COURIER -> 18.00`, `case ECONOMY, EXPRESS ->` cotação (`service/CheckoutService.java:116-131`);
- o prazo: `case STORE_PICKUP -> 1`, `case COURIER -> 0`, a cotação de novo (`service/CheckoutService.java:136-151`);
- o limite de 5 kg do motoboy: `req.shippingMethod() == ShippingMethod.COURIER && totalWeightKg > 5` (`service/Validation.java:66`);
- a recusa da TransLog: `ECONOMY || EXPRESS` e uma terceira chamada à cotação (`service/Validation.java:70-79`).

A tradução para `"STD"`/`"EXP"` também está fora do lado da TransLog: aparece três vezes (`service/CheckoutService.java:122`, `:142`, `service/Validation.java:72`), e o `TransLogClient.quote` recebe o código já traduzido (`service/TransLogClient.java:23`). Uma entrega nova mexe em pelo menos quatro lugares, em dois arquivos.
Conclusão: omissão

**P1-selecao, a entrega: como o caso é escolhido.** O Jackson converte o `shippingMethod` na constante do enum (`model/CheckoutRequest.java:13`), mas a constante não carrega comportamento. Quem faz a conta são os `switch (req.shippingMethod())` dentro do próprio cálculo (`service/CheckoutService.java:116`, `:136`). É uma condicional no cálculo.
Conclusão: omissão

**P2-local, o cupom: onde mora o código de cada caso.** O cupom é um `String` (`model/CheckoutRequest.java:10`) e não tem casa. O código de cada um fica em dois `switch` diferentes:
- o desconto: `case "WELCOME10"` (10%), `"MINUS50"` (50,00), `"FREESHIPPING"` (o frete), `"BUY3PAY2"` (`service/CheckoutService.java:76-88`), mais o método só do BUY3PAY2 (`service/CheckoutService.java:94-106`);
- a condição de uso: o histórico do WELCOME10, o mínimo de R$ 300 do MINUS50 e a retirada do FREESHIPPING (`service/Validation.java:115-138`).

A lista de cupons válidos (`service/Validation.java:150-153`) não conta, porque só confere se o código existe. O teto de 30% (`service/CheckoutService.java:90-91`) também não conta, porque é genérico. Um cupom novo mexe em três lugares.
Conclusão: omissão

**P2-selecao, o cupom: como o caso é escolhido.** O caso é escolhido por `switch (req.coupon())` sobre o texto, dentro do método que calcula o desconto (`service/CheckoutService.java:76`). É uma condicional no cálculo.
Conclusão: omissão

**P3-local, a forma de pagamento: onde mora o código de cada caso.** `PaymentMethod` é um enum sem corpo (`model/PaymentMethod.java:3-6`). O código de cada caso está espalhado assim:
- o ajuste: o desconto do Pix, a tarifa de 3,49 do boleto e a tabela Price do cartão (`service/CheckoutService.java:179-201`);
- as parcelas permitidas: `PIX || BANK_SLIP` só 1x, `CREDIT_CARD` de 1 a 12 (`service/Validation.java:93-101`);
- a recusa do boleto acima de R$ 1.000: `method == PaymentMethod.BANK_SLIP && total > 1000.00` (`service/Validation.java:144`).

Cada forma tem código em três lugares.
Conclusão: omissão

**P3-selecao, a forma de pagamento: como o caso é escolhido.** O caso é escolhido por `switch (req.paymentMethod())` no método que faz a conta do ajuste (`service/CheckoutService.java:179`). É uma condicional no cálculo.
Conclusão: omissão

**P4-local, o nível do clube: onde mora o código de cada caso.** `ClubTier` é um enum sem corpo e sem dados (`model/ClubTier.java:3-6`). As vantagens estão espalhadas por três métodos do serviço:
- o crédito: `case BRONZE -> 0`, `SILVER -> 2%`, `GOLD -> 5%` (`service/CheckoutService.java:205-209`);
- o frete grátis: `req.clubTier() == ClubTier.GOLD` (`service/CheckoutService.java:109`);
- o brinde: `req.clubTier() == ClubTier.GOLD && subtotal > 500.00` (`service/CheckoutService.java:213`).

Um nível novo com o seu "conjunto de vantagens" mexe nos três.
Conclusão: omissão

**P4-selecao, o nível do clube: como o caso é escolhido.** A escolha é feita por `switch (req.clubTier())` e por `== ClubTier.GOLD` dentro do cálculo de cada vantagem (`service/CheckoutService.java:109`, `:205`, `:213`). É uma condicional no cálculo.
Conclusão: omissão

**R1, o GOLD não paga frete, exceto no motoboy.** O remendo aparece literalmente no cálculo geral do frete: `if (req.clubTier() == ClubTier.GOLD && req.shippingMethod() != ShippingMethod.COURIER) return ZERO` (`service/CheckoutService.java:109-111`). Não há casa do GOLD nem uma política de frete onde a regra pudesse morar.
Conclusão: omissão

**R2, o FREESHIPPING não vale na retirada.** A regra está em dois lugares de fora, nenhum deles uma casa do cupom:
- a recusa: `case "FREESHIPPING": if (req.shippingMethod() == ShippingMethod.STORE_PICKUP)` em `Validation` (`service/Validation.java:130-133`);
- o desconto: o mesmo teste repetido no serviço, devolvendo zero (`service/CheckoutService.java:79-84`).

É o remendo, e ainda duplicado.
Conclusão: omissão

**R3, o desconto do Pix só sem cupom.** O teste `if (req.coupon() == null || req.coupon().isEmpty())` fica dentro do `case PIX` do `switch` do serviço (`service/CheckoutService.java:180-186`). Como o Pix não tem casa, a regra mora no cálculo do serviço, que lê o cupom direto da requisição.
Conclusão: omissão

**R4, o WELCOME10 só na primeira compra.** A consulta ao histórico, `customers.findCompletedOrders(...)` (`service/Validation.java:116-121`), está em `Validation.validateCoupon`. Essa validação é chamada pelo controller antes do serviço (`OrderRoutes.java:36`, `:73`). A consulta SQL em si é genérica (`service/CustomerRepository.java:81-96`). A regra do cupom mora numa validação de fora, acionada pelo controller, e não no cupom.
Conclusão: omissão

**P5-forma, a região.** `Region` é um enum que carrega dois números por constante, `insuranceRate` e `insuranceLimit` (`model/Region.java:6-10`, `:12-13`). O seguro é uma conta única que lê esses campos e não nomeia nenhuma região (`service/CheckoutService.java:167-176`). Os dias a mais vêm de um `switch (this)` que só devolve números: `NORTH -> 3`, `NORTHEAST -> 2`, `default -> 0` (`model/Region.java:29-35`). Nenhuma constante tem corpo próprio. A forma é um enum de dados, com um `switch` de números para o prazo.
Conclusão: contenção

**P5-proporcao, a região: o tamanho da resposta.** A resposta tem o tamanho do problema. O seguro é dado puro. O `switch` dos dias é condicional, não dado, mas a régua o aceita porque devolve só um número por região. Seria mais uniforme se fosse um terceiro campo do enum, ao lado dos dois do seguro. Não há classe por região nem comportamento próprio de nenhuma região.
Conclusão: contenção

**E, estrutura sem necessidade fora dos pontos.** O projeto não tem nenhuma interface, classe abstrata ou herança. A busca por `interface`, `abstract`, `extends` e `implements` só acha `CarrierUnavailableException extends Exception` (`service/TransLogClient.java:69`). Os repositórios e o `TransLogClient` são classes concretas e diretas, e não há fábrica, builder nem camada que só repassa. Perto do Strategy, não há estrutura sobrando.
Conclusão: contenção

## O que mudaria primeiro

1. **A entrega como estratégia.** Uma interface com frete, prazo e disponibilidade, e uma implementação por opção. As duas da TransLog delegariam a um adaptador que faz a tradução `STD`/`EXP` e a cotação uma vez só. Os quatro lugares de hoje (`CheckoutService` frete e prazo, `Validation` motoboy e TransLog) viram um por caso. Isso também acaba com as três chamadas à cotação do mesmo pedido. É o ponto que mais cresce ("quase toda semana").
2. **O cupom como estratégia, com o contexto do pedido.** Cada cupom com o seu desconto e a sua condição de uso. A condição recebe a forma de entrega e o histórico do cliente. Assim o R2 e o R4 vão para a casa do FREESHIPPING e do WELCOME10, e somem os dois `switch` paralelos de `CheckoutService` e `Validation`. O mesmo vale para o nível do clube, que poderia virar um enum com dados (as porcentagens) e métodos como `freeShipping(method)` e `gift(subtotal)`. Isso leva o R1 para dentro do nível.

## Dúvidas

- Em R1 a R4 usei `omissão`, e não `malfeito`, porque não existe estrutura que o `if` tenha atravessado: a regra só está no lugar errado porque os casos não têm casa.
- Em P5, li o `switch (this)` de `Region.getExtraDeliveryDays` (`model/Region.java:29-35`) como o "switch que só devolve o número de cada região", que a régua aceita. Por isso mantive `contenção`, e não chamei de enum com corpo.
- A tradução `"STD"`/`"EXP"` fora do `TransLogClient` (`service/CheckoutService.java:122`, `:142`, `service/Validation.java:72`) entrou como código de caso fora da casa no P1-local. A régua só isenta a tradução que fica dentro do lado da TransLog.
