# Padrões: Adapter

## Visão geral

O agente criou uma classe própria para a TransLog, `TransLogClient`, e é ela que monta a URL `/v2/quote`, faz a chamada HTTP e lê o JSON. Os nomes `price_cents` e `eta_hours` ficam declarados ali, e a conversão de centavos para reais também. Essa parte está boa. O problema é que a classe não traduz nada para a nossa língua: ela recebe o peso **em gramas** e o código de serviço **dela** (`STD`/`EXP`). Também devolve um objeto com `priceCents` e `etaHours` públicos e avisa o `ZIP_NOT_SERVED` só pelo texto da exceção. Por isso o `PricingService` faz três coisas que deviam ser da peça da TransLog: converte quilos em gramas, traduz a entrega para o código dela e converte horas em dias (essa conta mora num utilitário genérico). Ele ainda compara a string `"ZIP_NOT_SERVED"`. Trocar de transportadora mexeria no cálculo do frete, e não só nessa peça. Do lado do controle negativo, a retirada e o motoboy ficam como valores fixos no próprio cálculo, sem estrutura nenhuma: é a resposta do tamanho do problema.

## Item a item

**P6, o vocabulário da TransLog fica do lado dela?**

O que fica do lado dela:
- a rota e os parâmetros da chamada (`src/main/java/com/loja/orderservice/client/TransLogClient.java:24-25`);
- o record da resposta com `@JsonProperty("price_cents")` e `@JsonProperty("eta_hours")` (`TransLogClient.java:45-53`), o que não conta como vazamento;
- a conversão de centavos para reais em `TransLogQuote.getPrice()` (`TransLogClient.java:64-66`);
- a separação entre "CEP não atendido" e "fora do ar" (`TransLogClient.java:32-41`).

O que vaza, linha a linha, todo pelo `PricingService`:
- **O código de serviço dela.** `String service = method == ShippingMethod.ECONOMY ? "STD" : "EXP";` (`src/main/java/com/loja/orderservice/service/PricingService.java:160`). A tradução da nossa entrega para o código dela está fora do adaptador.
- **A unidade dela, as gramas.** `weight.multiply(new BigDecimal("1000")).intValue()` (`PricingService.java:161`). O adaptador já pede `Integer weightG` na assinatura (`TransLogClient.java:22`), então quem chama precisa saber que ela trabalha com gramas inteiras.
- **O campo e a unidade dela, as horas.** `PricingUtil.ceilDays(quote.etaHours)` (`PricingService.java:164`) lê o campo `etaHours` direto. A divisão por 24 fica em `src/main/java/com/loja/orderservice/util/PricingUtil.java:36-37`, um utilitário de preços fora do lado da TransLog.
- **O objeto devolvido fala a língua dela.** `TransLogQuote` expõe `priceCents` e `etaHours` como campos públicos (`TransLogClient.java:55-61`). Isso deixa o vazamento da linha 164 possível. Só o preço tem um acesso já traduzido.
- **O código de erro dela.** `if ("ZIP_NOT_SERVED".equals(e.getMessage()))` (`PricingService.java:174`). O adaptador passa o código dela como mensagem da exceção (`TransLogClient.java:33-34`), em vez de dar dois resultados nossos, como "não atende" e "fora do ar". O serviço de preços depende do texto da TransLog para escolher entre `SHIPPING_METHOD_UNAVAILABLE` e `CARRIER_UNAVAILABLE`.

Os dias a mais do Norte e do Nordeste (`PricingService.java:166-170`) são regra nossa e não pesam aqui.

Não há interface de transportadora: o `PricingService` depende da classe concreta (`PricingService.java:20`). A régua não cobra a interface. O que pesa é o isolamento, e ele falha em três frentes: as unidades dela (gramas e horas), o código de serviço e o código de erro. Existe uma peça para a TransLog, mas trocar de transportadora obrigaria a mexer nas linhas 160-177 do `PricingService` e no `PricingUtil.ceilDays`. É vazamento, que esta régua chama de omissão.
Conclusão: omissão

**P6-exagero, as entregas sem transportadora.** A retirada devolve `new ShippingCostInfo(BigDecimal.ZERO, 1)` (`PricingService.java:148-150`). O motoboy confere os 5 kg e devolve `new ShippingCostInfo(new BigDecimal("18.00"), 0)` (`PricingService.java:152-157`). As duas são valores fixos, escritos na própria regra da entrega e decididos antes de chegar à TransLog. Não há "cliente" do motoboy nem uma porta de transportadora por onde a retirada passe (o `grep` por `interface` só acha os repositórios JPA). Isso é contenção. O `if` sobre as entregas em si é assunto do Strategy (P1) e não entra nesta conclusão.
Conclusão: contenção

**E, estrutura sem necessidade perto do Adapter.** Perto da TransLog só há três peças: o `TransLogClient`, a configuração que cria o `RestTemplate` (`src/main/java/com/loja/orderservice/config/WebClientConfig.java:9-12`, uma fábrica do Spring que serve para injetar o cliente, não uma camada) e a `TransLogException` aninhada (`TransLogClient.java:69-77`). Nenhuma delas é uma interface com uma implementação só, nem uma camada que só repassa, nem uma fábrica sem uso. Não achei estrutura sobrando.
Conclusão: contenção

## O que mudaria primeiro

1. **Fazer o adaptador falar a nossa língua.** A assinatura seria algo como `quote(zipCode, ShippingMethod, BigDecimal weightKg)`, que devolve um resultado nosso: o frete em reais e o prazo em dias, ou "CEP não atendido", ou "transportadora fora do ar". Isso leva para dentro do `TransLogClient` a conversão para gramas (`PricingService.java:161`), o `"STD"`/`"EXP"` (`:160`), o `eta_hours / 24` (`PricingUtil.java:36-37`) e o `ZIP_NOT_SERVED` (`:174`). É o que de fato deixa a troca de transportadora presa a uma peça só.
2. **Tirar `priceCents` e `etaHours` do objeto devolvido** (`TransLogClient.java:55-61`). Assim ninguém de fora consegue voltar a ler as unidades dela.

## Dúvidas

- O cálculo dos dias está em `PricingUtil.ceilDays`, um utilitário genérico que não cita a TransLog. A régua diz que converter a unidade dela fora do adaptador é vazamento. Li o caso assim: o vazamento é a linha que chama `ceilDays(quote.etaHours)` (`PricingService.java:164`), e a função recebe um valor em horas, que é a unidade dela. Por isso contei como vazamento, mesmo a função tendo nome genérico.
- A conversão de centavos para reais está em `TransLogQuote.getPrice()`, dentro do arquivo do adaptador. Contei como trabalho do lado dela. O `PricingUtil.round` que vem depois (`PricingService.java:163`) só arredonda reais.
