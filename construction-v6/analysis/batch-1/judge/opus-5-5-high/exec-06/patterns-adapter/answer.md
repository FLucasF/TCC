# padrões: Adapter

## Visão geral

O agente separou bem a chamada HTTP. A classe `HttpTransLogClient` monta a rota `/v2/quote`, aplica o limite de 2 segundos, entende a resposta `ZIP_NOT_SERVED` e lê `price_cents` e `eta_hours`. Ela implementa a interface `TransLogClient`, que tem o record `TransLogQuote(priceCents, etaHours)`. Nenhum termo da TransLog chega ao `CheckoutService` nem aos outros pacotes. O problema mais sério é a fronteira, que fica um passo antes do lugar certo. A interface `TransLogClient` fala a língua da TransLog: recebe o peso em gramas e o código de serviço em texto, e devolve centavos e horas. Por isso a tradução para a nossa língua ficou fora do adaptador:
- a estratégia de entrega `TransLogShipping` converte quilos em gramas, centavos em reais e horas em dias;
- a configuração `ShippingConfig` escolhe `"STD"` e `"EXP"` junto com `ECONOMY` e `EXPRESS`.

Trocar de transportadora mexeria no adaptador e também nessas duas classes. As entregas sem transportadora estão resolvidas com contenção: valores fixos na própria classe, sem passar pela porta da TransLog.

## Item a item

**P6, o vocabulário da transportadora fica do lado dela?**

O lado da TransLog existe. Ele é formado por dois arquivos:
- `HttpTransLogClient`, que monta a chamada (`shipping/HttpTransLogClient.java:38-39`, com `/v2/quote`, `zip`, `weight_g` e `service`), aplica o limite de 2 s (`:25`, `:42-44`) e lê a resposta (`ZIP_NOT_SERVED` em `:63`, `price_cents` e `eta_hours` em `:66-67`);
- `TransLogClient`, da mesma pasta, que não conhece os nossos casos e declara `quote(String zipCode, long weightGrams, String service)` e `record TransLogQuote(int priceCents, int etaHours)` (`shipping/TransLogClient.java:11`, `:13`).

Até aqui, segue a prática. Mas três coisas do vocabulário da TransLog vazam para fora desse lado:
- **As unidades dela numa estratégia de entrega.** `TransLogShipping` implementa a nossa `ShippingOption` e aplica a nossa regra dos dias a mais por região. É ela que faz a conversão para gramas, `request.weightKg().movePointRight(3).setScale(0, RoundingMode.CEILING)` (`shipping/TransLogShipping.java:29`). Ela também lê `quote.priceCents()` e passa de centavos para reais com `BigDecimal.valueOf(quote.priceCents(), 2)` (`:32`). E lê `quote.etaHours()` e passa de horas para dias com `Math.ceilDiv(quote.etaHours(), 24)` (`:33`). A régua dá esse caso como exemplo de vazamento.
- **Os códigos de serviço dela fora do adaptador.** A tradução `ECONOMY -> "STD"` e `EXPRESS -> "EXP"` está em `ShippingConfig`, que conhece os nossos casos (`shipping/ShippingConfig.java:11`, `:16`). Esses códigos chegam como campo `service` na estratégia (`TransLogShipping.java:13`, `:30`). Essa tradução seria trabalho do adaptador.
- **A porta na língua dela.** Como `TransLogClient` pede gramas e um código de serviço e devolve centavos e horas, quem usa a porta precisa conhecer a TransLog. Uma transportadora nova não caberia nessa interface.

Fora do pacote `shipping` não há vazamento. O `CheckoutService` só monta um `ShippingRequest(zipCode, region, weightKg)` com o peso em quilos e recebe um `ShippingQuote(cost, days)` em reais e dias (`checkout/CheckoutService.java:72-73`; `shipping/ShippingRequest.java:7`; `shipping/ShippingQuote.java:5`). A busca por `cents`, `hours`, `gram`, `STD` e `EXP` fora de `shipping/` não acha nada da TransLog.

Resumindo: o lado da TransLog existe, mas as unidades, os campos e os códigos de serviço dela saem desse lado e entram na estratégia e na configuração.
Conclusão: malfeito

**P6-exagero, as entregas sem transportadora.** Os dois casos ficam cada um na sua classe, com o valor fixo e sem passar pela porta da TransLog:
- a retirada devolve `new ShippingQuote(Money.ZERO, 1)` (`shipping/StorePickupShipping.java:17`) e diz que não cobra frete (`:21-23`);
- o motoboy tem o preço como constante (`shipping/CourierShipping.java:14`), o limite de 5 kg na própria regra (`:13`, `:23-25`) e o prazo 0 (`:26`).

Nenhuma das duas classes recebe um `TransLogClient`. Não há "cliente" do motoboy, nem porta de transportadora para devolver um número fixo. As duas implementam `ShippingOption`, mas essa é a estrutura da variação das entregas (P1), e não um adaptador.
Conclusão: contenção

**E, estrutura sem necessidade fora dos pontos (perto do Adapter).** Fora do P6 não achei estrutura sem pedido nesta região do código:
- `ShippingConfig` só cria as duas entregas cotadas na TransLog (`shipping/ShippingConfig.java:9-17`);
- `Coded.index` é um registro por código de poucas linhas (`common/Coded.java:12-16`), usado pelos pontos que pedem variação.

A interface `TransLogClient` tem uma implementação só, e os testes não a substituem: eles sobem um servidor HTTP falso (`src/test/java/br/com/loja/pedidos/PedidosApiTest.java:39-46`). Mesmo assim, ela faz parte do próprio P6, por isso foi avaliada lá e não entra aqui (veja em Dúvidas).
Conclusão: contenção

## O que mudaria primeiro

1. **Pôr a porta da transportadora na nossa língua.** Por exemplo, `quote(zipCode, ShippingService ou o código da nossa entrega, BigDecimal weightKg)` devolvendo frete em reais e prazo em dias, e passar para o `HttpTransLogClient` a conversão para gramas, a de centavos para reais e a de horas para dias, que hoje estão em `TransLogShipping.java:29-33`. Assim, trocar de transportadora mexeria só no adaptador, e `TransLogShipping` ficaria com a nossa regra (os dias a mais por região).
2. **Levar a tradução `ECONOMY/EXPRESS -> STD/EXP` de `ShippingConfig.java:11,16` para dentro do adaptador.** Assim, os códigos de serviço da TransLog deixam de circular como campo das nossas estratégias.

## Dúvidas

- **De que lado fica `TransLogShipping`.** Ela não cita `ECONOMY` nem `EXPRESS` (recebe o código pelo construtor), e por isso poderia parecer "da mesma pasta, sem conhecer os nossos casos". Contei como nossa: ela implementa a `ShippingOption`, aplica a nossa regra de dias a mais por região (`TransLogShipping.java:33`, `request.region().carrierExtraDays()`) e lança o nosso erro `SHIPPING_METHOD_UNAVAILABLE` (`:31`). Isso é exatamente a "estratégia de entrega" que a régua cita como lugar de vazamento.
- **Onde avaliar a interface `TransLogClient`.** Ela tem uma implementação só, e nada a substitui. Avaliei no P6, e não no E, porque fica dentro do ponto que pede o padrão. Ela não ajuda a trocar de transportadora porque fala a língua da TransLog. Se essa interface for contada como estrutura sem necessidade, a conclusão do E passaria a `exagero`, o que me parece contar o mesmo problema duas vezes.
