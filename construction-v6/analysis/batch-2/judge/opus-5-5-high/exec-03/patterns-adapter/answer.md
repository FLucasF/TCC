Já li o enunciado, o pacote `shipping` e a montagem do serviço. Segue a correção.

# Padrões: Adapter

## Visão geral

O agente separou a conversa com a TransLog numa peça própria. `TransLogClient` monta a chamada `/v2/quote`, aplica o limite de 2 segundos, lê `price_cents`, `eta_hours` e `ZIP_NOT_SERVED` e traduz as falhas em duas exceções. Por cima dela fica uma porta, `CarrierClient`, que os testes trocam por um falso. A porta, porém, foi escrita na língua da TransLog: recebe gramas e o código de serviço e devolve `CarrierQuote(priceCents, etaHours)`. Por isso o trabalho com as unidades dela ficou do lado de fora:
- a estratégia de entrega `CarrierShipping` converte quilos em gramas, horas em dias e centavos em reais;
- o registro das nossas entregas escolhe `"STD"` e `"EXP"`.

O problema mais sério é esse vazamento de unidades e de códigos para a regra de entrega. Trocar de transportadora mexeria em `CarrierShipping` e em `ShippingOptions`, e não só no adaptador. As entregas sem transportadora (retirada e motoboy) foram resolvidas do jeito simples, com o valor e o prazo na própria regra.

## Item a item

**P6, o vocabulário da transportadora fica do lado dela?**

O enunciado desta execução tem o vocabulário estrangeiro (`prompt.md:205-213`: `/v2/quote`, `weight_g`, `STD`/`EXP`, `price_cents`, `eta_hours`, `ZIP_NOT_SERVED`), então o ponto mede.

O lado da TransLog existe:
- `TransLogClient.java` faz a chamada HTTP: a rota e os parâmetros em `src/main/java/shop/domain/shipping/TransLogClient.java:28-29`, o limite de 2 s em `:12`, `:15` e `:30`, a leitura de `ZIP_NOT_SERVED` em `:45` e de `price_cents`/`eta_hours` em `:50-55`;
- `CarrierClient.java`, na mesma pasta, não conhece nenhum dos nossos casos e declara a resposta dela, `record CarrierQuote(long priceCents, int etaHours)` (`src/main/java/shop/domain/shipping/CarrierClient.java:7`). Ter esses nomes ali não é vazamento.

O vocabulário dela vaza para fora desse lado em três lugares:
- **`CarrierShipping.java:26`**: `request.weightKg().movePointRight(3).longValue()`. A estratégia de entrega converte quilos em gramas, a unidade da TransLog, antes de chamar a porta.
- **`CarrierShipping.java:29`**: `(quote.etaHours() + 23) / 24`. Ela lê o campo `etaHours` e converte horas em dias. É exatamente o caso que a régua chama de vazamento.
- **`CarrierShipping.java:30`**: `new BigDecimal(quote.priceCents()).movePointLeft(2)`. Ela lê `priceCents` e converte centavos em reais.
- **`ShippingOptions.java:18-19`**: `new CarrierShipping("ECONOMY", "STD", carrier)` e `new CarrierShipping("EXPRESS", "EXP", carrier)`. A tradução da nossa entrega para o código de serviço dela fica no registro das nossas entregas, e não no adaptador. O código chega à porta como texto livre (`CarrierClient.java:5`, o parâmetro `String service`).

O resto do código está limpo. `CheckoutCalculator` só pede a cotação com `ShippingRequest(zipCode, weightKg, region)` e recebe `ShippingQuote(cost, days)` (`src/main/java/shop/service/CheckoutCalculator.java:55`), sem nenhum termo da TransLog. A tradução das falhas para os nossos erros (`CarrierShipping.java:31-35`) usa exceções neutras da porta, o que está certo.

Em resumo, a peça própria existe e faz a chamada e a leitura da resposta, mas as unidades (gramas, horas, centavos) e os códigos `STD`/`EXP` são tratados numa estratégia de entrega e no registro, fora dela.
Conclusão: malfeito

**P6-exagero, as entregas sem transportadora.** As duas implementam a mesma interface de entrega que as outras (`ShippingOption`, a estrutura do P1) e não passam pela porta da transportadora:
- a retirada devolve `new ShippingQuote(Money.ZERO, 1)` (`src/main/java/shop/domain/shipping/StorePickup.java:13`);
- o motoboy guarda `COST = Money.of("18.00")` e o limite de 5 kg como constantes e devolve `new ShippingQuote(COST, 0)` (`src/main/java/shop/domain/shipping/Courier.java:9-10`, `:19-22`).

Nenhuma das duas usa `CarrierClient`. Não há "cliente" do motoboy nem adaptador que devolva um número fixo, e só `CarrierShipping` recebe a porta (`ShippingOptions.java:18-21`).
Conclusão: contenção

**E, estrutura sem necessidade fora dos pontos.** Perto deste pattern:
- `CarrierClient` tem uma única implementação de produção, mas o teste a substitui por um falso (`src/test/java/shop/CheckoutExamplesTest.java:41-46`), então ela tem uso;
- a fronteira de persistência `Store`/`Tx` (`src/main/java/shop/persistence/Store.java:6`, `Tx.java`) tem duas implementações, `PostgresStore` em produção (`App.java:25`) e `MemoryStore` nos testes (`CheckoutExamplesTest.java:36`), então também é substituída;
- `ShippingRequest` é só o objeto de parâmetros da cotação.

Não achei interface sem substituto, camada que só repassa nem fábrica desnecessária nessa região do código.
Conclusão: contenção

## O que mudaria primeiro

1. **Pôr a porta na nossa língua.** `CarrierClient.quote` passaria a receber o CEP, o peso em quilos e a nossa entrega (ou um serviço nosso, neutro) e a devolver o frete em reais e o prazo em dias. A conversão para gramas, o arredondamento das horas para dias e a passagem de centavos para reais iriam para `TransLogClient`. Assim `CarrierShipping` fica só com a regra nossa (os dias a mais da região, `CarrierShipping.java:29`), e uma transportadora nova não precisa fingir que fala em gramas, horas e centavos.
2. **Levar o `"STD"`/`"EXP"` para dentro do adaptador.** Hoje o código de serviço da TransLog está em `ShippingOptions.java:18-19`. Num adaptador de outra transportadora, esses códigos não significariam nada.

## Dúvidas

- **Onde fica a fronteira.** O lado da TransLog e as estratégias de entrega estão na mesma pasta (`shop/domain/shipping`). Segui a definição da régua: contei `CarrierClient.java` do lado dela, porque não conhece nenhum dos nossos casos e declara a resposta. `CarrierShipping` e `ShippingOptions` ficaram de fora: o primeiro é uma estratégia de entrega (implementa `ShippingOption` e aplica a regra da região) e o segundo nomeia os nossos casos. Se a porta fosse lida como "nossa", o vazamento seria maior (a própria porta falaria em `weightGrams`, `priceCents` e `etaHours`), mas a conclusão continuaria malfeito.
- **O código de serviço em `CarrierShipping`.** A classe guarda o código como um parâmetro genérico (`carrierService`) e não o cita. Contei o vazamento na linha que escolhe `"STD"`/`"EXP"` (`ShippingOptions.java:18-19`), e não em `CarrierShipping`.
