# padrões: Adapter

## Visão geral

O agente separou a TransLog numa pasta própria, `shipping/carrier/`. Nela estão uma porta (`Carrier`), um adaptador que faz a chamada HTTP (`TranslogCarrier`), o resultado da cotação (`CarrierQuote`), um enum dos níveis de serviço (`CarrierService`) e duas exceções. O adaptador cuida bem do formato da API: monta a rota `/v2/quote` com `zip`, `weight_g` e `service`, traduz o nível de serviço para `"STD"`/`"EXP"`, lê `price_cents` e `eta_hours`, trata `ZIP_NOT_SERVED` e transforma o preço de centavos em reais. Do lado do controle negativo, a retirada e o motoboy são valores fixos dentro da própria regra de entrega e não passam pela transportadora.

O problema mais sério está na porta. Ela foi desenhada nas unidades da TransLog: pede o peso em gramas e devolve o prazo em horas. Por isso duas conversões que são trabalho do adaptador ficaram na estratégia de entrega `CarrierShippingOption`: de quilos para gramas e de horas para dias. A régua chama exatamente isso de vazamento, então o P6 fica em malfeito. Fora isso, não há estrutura sem necessidade perto deste pattern.

## Item a item

**P6, a TransLog.**

*O lado da TransLog.* `src/main/java/com/loja/orders/shipping/carrier/TranslogCarrier.java` faz a chamada HTTP. Os outros arquivos da pasta (`Carrier.java`, `CarrierQuote.java`, `CarrierService.java`, `CarrierUnavailableException.java`, `ZipNotServedException.java`) não citam nenhum dos nossos casos de entrega, então contam como lado dela.

*O que fica dentro do adaptador, como deve:*
- a rota e os parâmetros (`TranslogCarrier.java:49-52`);
- a tradução do nosso nível de serviço para o código dela, `STANDARD -> "STD"` e `EXPRESS -> "EXP"` (`TranslogCarrier.java:115-119`);
- o reconhecimento de `ZIP_NOT_SERVED`, transformado numa exceção nossa (`TranslogCarrier.java:27`, `:96-101`);
- a leitura de `price_cents`/`eta_hours` (`TranslogCarrier.java:103-104`);
- a conversão do preço de centavos para reais (`BigDecimal.valueOf(cents.asLong(), 2)`, `TranslogCarrier.java:108`);
- o timeout de 2 s e a resposta inválida tratados como fora do ar (`TranslogCarrier.java:57-58`, `:71-83`).

Os nomes `etaHours` (`CarrierQuote.java:5`) e `weightGrams` (`Carrier.java:10`) estão declarados do lado dela, e só declarar não é vazamento.

*O que vaza, e por onde.* A porta ficou nas unidades da TransLog: `CarrierQuote quote(String zipCode, int weightGrams, CarrierService service)` (`Carrier.java:10`) e `record CarrierQuote(BigDecimal price, int etaHours)` (`CarrierQuote.java:5`). Por isso quem a usa tem de trabalhar com gramas e horas:
- `src/main/java/com/loja/orders/shipping/CarrierShippingOption.java:19` declara `GRAMS_PER_KG`, e a `:32` converte o peso do pedido de quilos para gramas (`request.weightKg().multiply(GRAMS_PER_KG).setScale(0, RoundingMode.CEILING)`) antes de chamar a transportadora. A régua dá esse caso como exemplo de vazamento. Até a regra de arredondamento do peso exigido pela API dela (um inteiro em gramas) mora na estratégia.
- `CarrierShippingOption.java:20` declara `HOURS_PER_DAY`, e a `:41` lê `quote.etaHours()` e faz `Math.ceilDiv(quote.etaHours(), HOURS_PER_DAY)`. Isso é a conversão de horas para dias fora do adaptador, outro exemplo citado na régua.

`CarrierShippingOption` é a estratégia de entrega de que `EconomyShipping` e `ExpressShipping` herdam (`EconomyShipping.java:9`, `ExpressShipping.java:9`). Fica no pacote `shipping`, fora do lado da TransLog.

A conversão do preço foi feita dentro do adaptador, mas a do peso e a do prazo não. Por isso uma transportadora nova que cobrasse em quilos ou desse o prazo em dias teria de se moldar a uma porta em gramas e horas, ou obrigaria a mexer na estratégia.

*O que não vaza.* A tradução de `ECONOMY` para `CarrierService.STANDARD` acontece em `EconomyShipping.java:12`, mas `STANDARD`/`EXPRESS` são o nosso vocabulário de nível de serviço, não o código dela (`"STD"`/`"EXP"` só aparecem no adaptador). O acréscimo de dias por região (`Region.carrierDispatchExtraDays()`, `CarrierShippingOption.java:41`) é regra nossa, não da TransLog. Nos testes, `src/test/java/com/loja/orders/support/FakeTranslog.java:64-65` é um falso servidor HTTP que imita a API dela, e não conta como vazamento no código de produção.

O lado da TransLog existe e cuida bem da chamada e do formato da resposta, mas duas das três unidades dela (gramas e horas) vazam para a estratégia de entrega.
Conclusão: malfeito

**P6-exagero, as entregas sem transportadora.**
- `src/main/java/com/loja/orders/shipping/StorePickupShipping.java:16-18` devolve `new ShippingQuote(Money.ZERO, 1)` direto.
- `src/main/java/com/loja/orders/shipping/CourierShipping.java:15` guarda o preço como constante (`Money.of("18.00")`), e as linhas `:24-29` devolvem `new ShippingQuote(PRICE, 0)` e conferem o limite de 5 kg na própria regra.

Nenhuma das duas recebe um `Carrier`, nem há um "cliente" do motoboy ou um adaptador que devolva um número fixo. A única porta de transportadora é implementada só por `TranslogCarrier` e só é usada por `CarrierShippingOption`. As duas são implementações de `ShippingOption`, mas isso é a estratégia de entrega (P1), não uma porta de transportadora. Valor e prazo moram na própria regra de cada entrega.
Conclusão: contenção

**E, estrutura sem necessidade perto deste pattern.**
- A interface `Carrier` tem uma implementação só (`TranslogCarrier`), mas o enunciado diz "Pode ser que um dia a gente troque de transportadora", então ela responde a um pedido do enunciado e não é sobra.
- `CarrierService` é um enum de dois valores que separa o nosso nível de serviço do código da TransLog. É pequeno e tem uso.
- As duas exceções (`ZipNotServedException`, `CarrierUnavailableException`) distinguem os dois erros que o enunciado trata de forma diferente (422 e 503).
- `CarrierShippingOption` é uma classe-base que serve aos dois casos com transportadora.

Um detalhe que não chega a ser estrutura: o campo `productsSubtotal` de `ShippingRequest` (`ShippingRequest.java:8`) não é lido por nenhuma entrega, só preenchido em `CheckoutCalculator.java:65`. Não encontrei camada de repasse, fábrica ou builder sem uso em volta da transportadora.
Conclusão: contenção

## O que mudaria primeiro

1. **Pôr a porta `Carrier` nas nossas unidades.** A cotação passaria a receber o peso em quilos (`BigDecimal weightKg`) e a devolver o frete em reais e o prazo em dias (`CarrierQuote(BigDecimal price, int deliveryDays)`). As conversões para gramas e de horas para dias, hoje em `CarrierShippingOption.java:32` e `:41`, iriam para dentro de `TranslogCarrier`, junto da conversão de centavos que já está lá (`:108`). Assim a estratégia de entrega só soma os dias da região, e trocar de transportadora volta a mexer numa peça só.
2. Com isso, as constantes `GRAMS_PER_KG` e `HOURS_PER_DAY` (`CarrierShippingOption.java:19-20`) também passariam para o adaptador.

## Dúvidas

- **Se `CarrierService` conta como lado da TransLog.** Ele está na pasta dela e não cita os nossos casos de entrega (`ECONOMY`/`EXPRESS` do site), só os níveis `STANDARD`/`EXPRESS`. O valor `EXPRESS` tem o mesmo nome de um caso nosso, mas é o nível de serviço contratado, não a opção do site. Li-o como o vocabulário neutro de nível de serviço que o adaptador traduz para `"STD"`/`"EXP"`, e não contei como vazamento.
- **Se a assinatura da porta é vazamento por si só.** A régua diz que declarar um campo com o nome da TransLog do lado dela não é vazamento, e `Carrier`/`CarrierQuote` estão na pasta dela. Por isso não contei a assinatura, e sim o uso dela na estratégia (`CarrierShippingOption.java:32` e `:41`). O resultado (malfeito) seria o mesmo pela outra leitura.
