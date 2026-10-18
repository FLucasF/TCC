# Padrões: Adapter

## Visão geral

O agente montou um lado da TransLog de verdade. `TranslogClient` (`src/main/java/br/com/loja/pedidos/shipping/TranslogClient.java`) é o único arquivo que faz a chamada HTTP. Ele monta a rota `/v2/quote` com `zip`, `weight_g` e `service`, traduz o nosso nível de serviço para `"STD"`/`"EXP"`, lê `price_cents`, `eta_hours` e `ZIP_NOT_SERVED`, e devolve um resultado nosso (`Quoted`, `NotServed`, `Unavailable`) por uma porta, a `CarrierGateway`. O preço já sai em reais. O desenho falha numa coisa, que é a mais séria: a porta foi escrita nas unidades da TransLog. Ela recebe `long weightGrams` e devolve `long etaHours`. Por isso a estratégia de entrega `CarrierShippingOption` converte quilos em gramas antes de chamar e horas em dias depois de receber. A régua dá exatamente esse caso como vazamento, então P6 fica em malfeito. O controle negativo foi bem resolvido: a retirada e o motoboy devolvem valores fixos na própria regra, sem passar pela porta da transportadora. Também não achei estrutura sobrando perto do pattern.

## Item a item

**P6, o vocabulário da transportadora fica do lado dela?**

O que fica do lado da TransLog:
- **A chamada.** `TranslogClient.java:44-45` monta `baseUrl + "/v2/quote?zip=..." + "&weight_g=" + weightGrams + "&service=" + service`. É o único cliente HTTP do código de produção: nada fora de `shipping/` usa `HttpClient`, `RestClient` ou `WebClient`.
- **O código de serviço.** A tradução para `"EXP"`/`"STD"` é feita no próprio adaptador, a partir do nosso enum `ServiceLevel` (`TranslogClient.java:43`, `ServiceLevel.java:4`). A régua conta isso como trabalho do adaptador.
- **A resposta.** `ZIP_NOT_SERVED` (`TranslogClient.java:71`) e `price_cents`/`eta_hours` (`TranslogClient.java:73`) só aparecem aqui. O preço sai convertido de centavos para reais em `BigDecimal.valueOf(cents.longValue(), 2)` (`TranslogClient.java:75`). Os "200 que não é resposta", os outros status e o tempo limite de 2 s viram `Unavailable` dentro do adaptador (`TranslogClient.java:24`, `:48-59`).
- **O resto do código.** `CheckoutService` só conhece `ShippingOption`, `ShippingRequest` (com `weightKg`) e `ShippingQuote` (com `cost` e `days`) (`checkout/CheckoutService.java:59-61`). Os testes citam `price_cents`, mas só no servidor falso que faz o papel da TransLog (`PedidosIntegrationTest.java:29-38`), e isso não é código de produção.

O que vaza, e por onde:
- **A porta fala as unidades dela.** `CarrierGateway.java:8` declara `quote(String zipCode, long weightGrams, ServiceLevel level)` e `CarrierGateway.java:13` declara `Quoted(BigDecimal price, long etaHours)`. Pela letra da régua, a interface pode contar como lado da TransLog: está na mesma pasta e não cita os nossos casos. Mesmo assim, ela obriga quem a usa a trabalhar em gramas e horas.
- **Gramas na estratégia de entrega.** `CarrierShippingOption.java:27-28` faz `request.weightKg().multiply(BigDecimal.valueOf(1000)).setScale(0, RoundingMode.CEILING)`. A estratégia aplica até a regra dela de "grama inteiro", arredondando para cima.
- **Horas na estratégia de entrega.** `CarrierShippingOption.java:31` faz `(q.etaHours() + 23) / 24` e soma os dias da região. É o `etaHours / 24` que a régua cita como exemplo de vazamento. O mesmo arquivo lê o campo `etaHours`, um nome do vocabulário dela, fora do adaptador.

Uma consequência concreta: uma segunda transportadora que implemente `CarrierGateway` teria de aceitar gramas e devolver horas, ou a conversão em `CarrierShippingOption` teria de mudar. Trocar de transportadora não mexe só numa peça.

Conclusão: malfeito

**P6-exagero, as entregas sem transportadora.**
- **A retirada.** `StorePickupShipping.java:15-17` devolve `new ShippingQuote(Money.ZERO, 1)`.
- **O motoboy.** `CourierShipping.java:19-24` confere o limite de 5 kg e devolve `new ShippingQuote(Money.of("18.00"), 0)`.
- As duas implementam direto a `ShippingOption`, a mesma interface das entregas. Nenhuma recebe uma `CarrierGateway`, não existe um "cliente" do motoboy e a retirada não passa pela porta da TransLog. Só `ShippingConfig.java:10-17` liga a porta, e só para `ECONOMY` e `EXPRESS`.

Valor e prazo moram na própria regra da entrega.

Conclusão: contenção

**E, estrutura sem necessidade fora dos pontos (perto do Adapter).**
- **`CarrierGateway`.** Tem uma implementação só, mas o enunciado pede para ela poder ser trocada ("Pode ser que um dia a gente troque de transportadora", `prompt.md:43`). Faz parte do P6, não é sobra.
- **`ServiceLevel`.** Um enum de dois valores, que é o nosso vocabulário para o nível de serviço que o adaptador traduz.
- **`ShippingConfig`.** Só cria os dois beans de entrega por transportadora. Não é uma fábrica que ninguém pediu, é a configuração do Spring.
- Não achei camada que só repassa, nem builder.

Conclusão: contenção

## O que mudaria primeiro

Passar a porta para as nossas unidades: `quote(String zipCode, BigDecimal weightKg, ServiceLevel level)`, devolvendo o preço em reais e o prazo em dias. As conversões das linhas `CarrierShippingOption.java:27-28` e `:31` iriam para dentro de `TranslogClient`. Assim `CarrierShippingOption` só somaria os dias da região, e trocar de transportadora mexeria apenas no adaptador. É uma mudança pequena, que leva o P6 a acerto sem tocar no resto.

## Dúvidas

- **A porta `CarrierGateway` conta como lado da TransLog ou como nossa?** Pela letra da régua, ela conta como lado da TransLog (mesma pasta, não conhece os nossos casos). Isso não muda a conclusão: a régua dá como vazamento explícito a conversão de unidade feita na estratégia de entrega (`CarrierShippingOption`), e essa conversão está lá. Quanto à própria porta, li o fato de ela estar em gramas e horas como a causa do vazamento, e não como um vazamento separado.
- **Fora isso:** nenhuma.
