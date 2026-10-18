# Padrões: Adapter

## Visão geral

O agente isolou numa peça própria a parte técnica da conversa com a TransLog. `HttpTransLogClient` monta a rota `/v2/quote` com `zip`, `weight_g` e `service`, lê `price_cents`, `eta_hours` e `ZIP_NOT_SERVED`, aplica o limite de 2 segundos e transforma a falha em `CARRIER_UNAVAILABLE`. Nada disso aparece fora dele, e o `CheckoutService` só fala a nossa língua: peso em quilos, frete em reais, prazo em dias. O problema mais sério é que a conversão das unidades da TransLog ficou fora dessa peça. Quem faz a conversão é `TransLogShipping`, a estratégia de entrega da econômica e da expressa: ela passa os quilos para gramas, os centavos para reais e as horas para dias. O segundo problema é que a tradução da nossa entrega para o código de serviço dela (`"STD"`, `"EXP"`) também ficou fora do adaptador, em `ShippingConfig`. A interface `TransLogClient` fala a língua da TransLog (gramas, centavos, horas, código de serviço), e não a nossa. Por isso, trocar de transportadora mexeria em quatro arquivos, e não em um. As entregas sem transportadora foram resolvidas do tamanho certo: valores fixos na própria regra de cada entrega, sem passar pela TransLog.

## Item a item

**P6, o vocabulário da transportadora fica do lado dela?** O lado da TransLog é `HttpTransLogClient` e o `TransLogClient` (a interface e o record da resposta), que estão na mesma pasta e não conhecem os nossos casos.

O que fica bem isolado:
- a rota e os parâmetros: `/v2/quote?zip=…&weight_g=…&service=…` (`src/main/java/br/com/loja/pedidos/shipping/HttpTransLogClient.java:38-39`);
- o formato da resposta: `ZIP_NOT_SERVED` (`HttpTransLogClient.java:63`) e `price_cents`/`eta_hours` (`HttpTransLogClient.java:66-67`);
- o tempo limite e a falha que vira `CARRIER_UNAVAILABLE` (`HttpTransLogClient.java:25`, `:79-80`);
- os campos `priceCents`/`etaHours` e o parâmetro `weightGrams`, declarados do lado dela (`src/main/java/br/com/loja/pedidos/shipping/TransLogClient.java:11`, `:13`). Isso não é vazamento.

O `CheckoutService` só usa a nossa língua: soma o peso em quilos (`src/main/java/br/com/loja/pedidos/checkout/CheckoutService.java:72`), monta o `ShippingRequest(zipCode, region, weightKg)` e recebe o `ShippingQuote(cost, days)` (`CheckoutService.java:73`, `:94`). Nenhum termo da TransLog aparece ali.

Mas o vocabulário vaza em `TransLogShipping`, que é uma estratégia de entrega (implementa `ShippingOption`, aplica a regra nossa dos dias a mais por região) e não o adaptador:
- converte quilos em gramas antes de chamar a transportadora: `request.weightKg().movePointRight(3)…` (`src/main/java/br/com/loja/pedidos/shipping/TransLogShipping.java:29`), e passa `grams` ao cliente (`TransLogShipping.java:30`);
- trabalha com os centavos: `BigDecimal.valueOf(quote.priceCents(), 2)` (`TransLogShipping.java:32`);
- trabalha com as horas: `Math.ceilDiv(quote.etaHours(), 24)` (`TransLogShipping.java:33`). É o caso que a régua dá como exemplo de vazamento.

Os códigos de serviço dela também moram do nosso lado, junto com os nossos casos: `new TransLogShipping("ECONOMY", "STD", client)` e `new TransLogShipping("EXPRESS", "EXP", client)` (`src/main/java/br/com/loja/pedidos/shipping/ShippingConfig.java:11`, `:16`). A régua só aceita a tradução `EXPRESS -> "EXP"` dentro do adaptador, e `ShippingConfig` não é o adaptador.

Por causa disso, a porta `TransLogClient` tem a forma da API da TransLog (`quote(String zipCode, long weightGrams, String service)` devolvendo `TransLogQuote(int priceCents, int etaHours)`). Uma transportadora nova com outras unidades obrigaria a mudar `TransLogClient`, `HttpTransLogClient`, `TransLogShipping` e `ShippingConfig`.

A parte técnica da API está isolada, mas as unidades e os códigos de serviço vazam por dois arquivos. Pela régua, isso é vazamento.
Conclusão: omissão

**P6-exagero, as entregas sem transportadora.** A retirada devolve o valor fixo na própria regra, `new ShippingQuote(Money.ZERO, 1)` (`src/main/java/br/com/loja/pedidos/shipping/StorePickupShipping.java:17`). O motoboy tem o preço e o limite de peso como constantes da própria classe (`src/main/java/br/com/loja/pedidos/shipping/CourierShipping.java:13-14`) e devolve `new ShippingQuote(Money.round(PRICE), 0)` (`CourierShipping.java:26`). Nenhuma das duas recebe nem chama `TransLogClient`. Não existe "cliente" do motoboy nem porta de transportadora para devolver número fixo. As duas implementam `ShippingOption`, mas essa é a interface das formas de entrega (o ponto P1, que tem régua própria), e não a porta da transportadora. É contenção.
Conclusão: contenção

**E, estrutura sem necessidade perto do Adapter.** Olhei o pacote `shipping` e o que ele usa em `common`.
- `TransLogClient` tem uma implementação só (`HttpTransLogClient`), e os testes não a substituem: simulam a TransLog por HTTP (`src/test/java/br/com/loja/pedidos/PedidosApiTest.java:39-46`). Mas essa interface é a porta do próprio P6, julgada lá, e não estrutura fora dos pontos.
- `ShippingConfig` só registra as duas entregas da TransLog como beans (`ShippingConfig.java:9-17`).
- `Coded.index` (`src/main/java/br/com/loja/pedidos/common/Coded.java:52-56`) é um registro simples por código.
- `ShippingRequest` (`src/main/java/br/com/loja/pedidos/shipping/ShippingRequest.java:7`) agrupa os parâmetros da cotação.

Nenhuma dessas peças é camada que só repassa nem fábrica que ninguém pediu.
Conclusão: contenção

## O que mudaria primeiro

1. **Levar as conversões para dentro do adaptador.** Seria uma porta de transportadora na nossa língua, por exemplo `Optional<ShippingQuote> quote(String zipCode, BigDecimal weightKg, ShippingMethod/String method)`, que devolve reais e dias. Dentro dela ficariam quilos→gramas, centavos→reais e horas→dias. `TransLogShipping` ficaria só com a nossa regra (os dias a mais por região, a indisponibilidade). Com isso, trocar de transportadora mexeria só no adaptador, que é o que o enunciado avisa que pode acontecer ("Pode ser que um dia a gente troque de transportadora").
2. **Tirar `"STD"`/`"EXP"` de `ShippingConfig`.** A tradução `ECONOMY -> STD` e `EXPRESS -> EXP` passaria a ser feita dentro do adaptador, para que os códigos da TransLog não fiquem ao lado dos nossos casos.

## Dúvidas

- **Onde termina o lado da TransLog.** `TransLogShipping` está na mesma pasta do cliente HTTP e não nomeia `ECONOMY`/`EXPRESS` (recebe o código de fora). Ainda assim, li essa classe como estratégia de entrega, e não como lado da TransLog: ela implementa `ShippingOption`, recebe o nosso `ShippingRequest`, devolve o nosso `ShippingQuote` e aplica uma regra nossa (`request.region().carrierExtraDays()`, `TransLogShipping.java:33`). É o caso que a régua descreve como vazamento. Quem a lesse como parte do adaptador (o par `TransLogShipping` + `HttpTransLogClient`) teria só o vazamento dos códigos de serviço em `ShippingConfig`.
- **Omissão ou malfeito.** A régua deste pattern chama o vazamento de "a omissão deste ponto", e foi isso que usei. Mas há uma estrutura que isola bem a parte técnica (rota, JSON, erros, tempo limite), e o que vaza são as unidades e os códigos. Pela definição geral da régua comum, também caberia ler como malfeito.
- **Os testes.** `PedidosApiTest.java` usa `price_cents`, `eta_hours` e `/v2/quote` (`PedidosApiTest.java:33`, `:40`, `:125` e seguintes), mas é a TransLog simulada, que tem de falar a língua dela. Não contei como vazamento.
