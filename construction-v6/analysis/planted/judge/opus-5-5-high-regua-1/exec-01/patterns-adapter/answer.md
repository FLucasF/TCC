# padrões: Adapter

## Visão geral

O agente isolou a conversa com a TransLog numa peça própria: `TranslogCarrier`, no pacote `shipping/carrier`, atrás de uma porta `Carrier`. A rota `/v2/quote`, os parâmetros `zip`, `weight_g` e `service`, os códigos `STD`/`EXP`, os campos `price_cents`/`eta_hours`, o erro `ZIP_NOT_SERVED`, o limite de 2 segundos e a conversão de centavos para reais ficam todos dentro do adaptador. Esse é o ponto forte. O problema mais sério é que a porta foi desenhada nas unidades da TransLog: ela recebe o peso em gramas e devolve o prazo em horas. Por isso, a conversão de quilos para gramas (com o arredondamento para inteiro que a API exige) e a de horas para dias (`÷ 24`, para cima) ficaram do nosso lado, em `CarrierShippingOption`. Pela régua, isso é vazamento. Trocar por uma transportadora que trabalhe com quilos ou com dias mexeria na regra de entrega, e não só no adaptador. O controle negativo foi bem resolvido: a retirada e o motoboy devolvem valores fixos na própria regra, sem passar pela porta da transportadora.

## Item a item

**P6, o vocabulário da transportadora fica do lado dela?** O lado da TransLog é a pasta `shipping/carrier`. São os arquivos que fazem a chamada HTTP (`TranslogCarrier.java`) e os que não conhecem os nossos casos: `Carrier.java`, `CarrierQuote.java`, `CarrierService.java` (`STANDARD`, `EXPRESS`, níveis de serviço dela, sem citar `ECONOMY`) e as duas exceções.

O que fica do lado dela:
- a rota e os parâmetros: `"/v2/quote"`, `"?zip="`, `"&weight_g="`, `"&service="` (`src/main/java/com/loja/orders/shipping/carrier/TranslogCarrier.java:49-52`);
- os códigos de serviço `"STD"` e `"EXP"`, traduzidos de `CarrierService` dentro do adaptador (`TranslogCarrier.java:115-120`);
- o erro `ZIP_NOT_SERVED` e as respostas que não são `200`, transformados em `ZipNotServedException` e `CarrierUnavailableException` (`TranslogCarrier.java:27`, `:57-59`, `:96-101`);
- os campos `price_cents` e `eta_hours` (`TranslogCarrier.java:103-104`);
- a conversão de centavos para reais, `BigDecimal.valueOf(cents.asLong(), 2)` (`TranslogCarrier.java:108`), e o limite de 2 s (`:73`, `src/main/resources/application.properties:6`).

Uma busca por `price_cents`, `eta_hours`, `weight_g`, `v2/quote`, `"STD"` e `"EXP"` fora de `shipping/carrier` não acha nada no código de produção.

O que vaza, todo em `src/main/java/com/loja/orders/shipping/CarrierShippingOption.java`:
- as unidades dela viram constantes do nosso lado: `GRAMS_PER_KG = 1000` (`:19`) e `HOURS_PER_DAY = 24` (`:20`);
- o peso é convertido de quilos para gramas fora do adaptador, com o arredondamento para inteiro que a API pede (`.multiply(GRAMS_PER_KG).setScale(0, RoundingMode.CEILING).intValueExact()`, `:32`). Esse valor vai para `carrier.quote(..., weightGrams, ...)` (`:35`);
- o prazo é convertido de horas para dias fora do adaptador, com o campo dela: `Math.ceilDiv(quote.etaHours(), HOURS_PER_DAY)` (`:41`).

A causa está na porta. `Carrier.quote(String zipCode, int weightGrams, CarrierService service)` (`carrier/Carrier.java:10`) e `CarrierQuote(BigDecimal price, int etaHours)` (`carrier/CarrierQuote.java:5`) falam em gramas e horas. Declarar esses nomes do lado dela não é vazamento, mas obriga quem chama a trabalhar nas unidades dela. A régua cita exatamente estes dois casos ("`etaHours / 24`", "converte os quilos em gramas antes de chamar a transportadora") como vazamento.

Fica uma dúvida sobre `ZipNotServedException`, capturada fora da pasta (`CarrierShippingOption.java:14`, `:36`). O nome espelha o código `ZIP_NOT_SERVED` da TransLog, mas "CEP não atendido" é uma situação de qualquer transportadora, e o próprio enunciado a descreve assim. Não conto esse nome como vazamento, e ele não muda a conclusão.

O isolamento do formato da API é bom e a troca de transportadora está prevista pela porta. Mesmo assim, há termos e unidades da TransLog fora do lado dela, em duas conversões. Pela régua, isso é vazamento, a omissão deste ponto, ainda que parcial.
Conclusão: omissão

**P6-exagero, as entregas sem transportadora.** A retirada devolve `new ShippingQuote(Money.ZERO, 1)` na própria classe (`src/main/java/com/loja/orders/shipping/StorePickupShipping.java:16-18`). O motoboy tem o preço `18.00` e o limite de 5 kg em constantes próprias e devolve `new ShippingQuote(PRICE, 0)` (`src/main/java/com/loja/orders/shipping/CourierShipping.java:15-16`, `:24-29`). Nenhuma das duas recebe `Carrier` ou estende `CarrierShippingOption`. Não há "cliente" do motoboy nem retirada passando pela porta da TransLog. As duas implementam `ShippingOption`, a abstração das formas de entrega (P1), e não a da transportadora. O valor e o prazo ficam na própria regra da entrega.
Conclusão: contenção

**E, estrutura sem necessidade fora dos pontos (perto do Adapter).** A porta `Carrier` tem uma implementação só (`TranslogCarrier`), mas o enunciado prevê a troca de transportadora ("Pode ser que um dia a gente troque de transportadora"), e a régua aceita a porta neste ponto. Não conto como exagero. O enum `CarrierService` (`carrier/CarrierService.java:4-7`) é uma tradução a mais entre as nossas entregas e os códigos `STD`/`EXP`. É pequeno e tem função: o adaptador não precisa conhecer `ECONOMY`/`EXPRESS`, e as nossas regras não precisam conhecer `STD`/`EXP` (`EconomyShipping.java:12`, `ExpressShipping.java:12`). A classe-base `CarrierShippingOption` é assunto do Strategy (P1). Perto do Adapter, não encontrei camada que só repassa, fábrica nem builder sem necessidade.
Conclusão: contenção

## O que mudaria primeiro

1. Passar a porta para as nossas unidades: `Carrier.quote(String zipCode, BigDecimal weightKg, CarrierService service)`, com um `CarrierQuote(BigDecimal price, int deliveryDays)`. A conversão para gramas inteiras e a de horas para dias (`ceilDiv(eta_hours, 24)`) iriam para dentro de `TranslogCarrier`. `CarrierShippingOption` passaria a somar só os dias de despacho da região. Assim, uma transportadora nova que cote em quilos ou em dias mexeria só no adaptador dela, que é o motivo de o ponto existir.
2. De menor importância: dar à exceção de CEP não atendido um nome nosso e neutro em relação à transportadora (por exemplo, `ZipCodeNotServedByCarrierException`), para que nada fora da pasta lembre o código de erro da TransLog.
