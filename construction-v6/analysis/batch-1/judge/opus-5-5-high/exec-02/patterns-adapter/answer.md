# Padrões: Adapter

## Visão geral

O agente separou a chamada à TransLog numa classe só dela, `integration/TransLogClient.java`. Essa classe monta a URL `/v2/quote`, faz o GET com o limite de 2 segundos e lê o JSON (`price_cents`, `eta_hours`, `ZIP_NOT_SERVED`). O lado da transportadora existe, mas fala só a língua da TransLog, nos dois sentidos. Recebe o peso já em gramas e o código de serviço já como `"STD"` ou `"EXP"`. Devolve um `TransLogQuoteResponse` com `priceCents` e `etaHours`. Por isso o `CheckoutService`, no meio do cálculo do frete, faz todo o trabalho de tradução: escolhe `"STD"`/`"EXP"`, converte quilos em gramas, centavos em reais e horas em dias. Esse é o problema mais sério: trocar de transportadora mexeria no cálculo do frete, e não só numa peça. Há um segundo problema, menor e ligado ao primeiro. O adaptador devolve `Optional.empty()` tanto para "CEP não atendido" quanto para "fora do ar", então a nossa língua perdeu uma distinção que o enunciado faz. No controle negativo, o agente foi contido: a retirada e o motoboy são constantes dentro da regra de entrega, sem porta nem cliente.

## Item a item

**P6, o vocabulário da transportadora fica do lado dela?**

O lado da TransLog é `src/main/java/com/loja/orderservice/integration/TransLogClient.java`, a classe que faz a chamada HTTP. A classe aninhada `TransLogQuoteResponse` (linhas 71–74) está nela. Ali estão, como trabalho legítimo do adaptador:
- a rota e os parâmetros `"%s/v2/quote?zip=%s&weight_g=%d&service=%s"` (linhas 26–27);
- o limite de 2 segundos (linhas 30 e 35);
- a leitura de `ZIP_NOT_SERVED` (linha 46) e de `price_cents`/`eta_hours` (linhas 52–53).

O vocabulário dela vaza para `src/main/java/com/loja/orderservice/service/CheckoutService.java`, dentro de `calculateShipping`:
- **a assinatura do adaptador já está na língua dela**: `getQuote(String zipCode, Integer weightGrams, String service)` (`TransLogClient.java:24`) pede gramas e o código de serviço dela, e devolve `priceCents`/`etaHours` (linhas 72–73). Quem chama é obrigado a falar a língua da TransLog;
- **o código de serviço é escolhido no cálculo do frete**: `String service = method == ShippingMethod.ECONOMY ? "STD" : "EXP";` (`CheckoutService.java:273`). A tradução `ECONOMY -> "STD"` é trabalho do adaptador e ficou fora dele;
- **a conversão de quilos para gramas fica fora do adaptador**: `totalWeight.multiply(new BigDecimal("1000")).setScale(0, RoundingMode.DOWN)` (`CheckoutService.java:274–275`), guardado numa variável chamada `weightGrams`;
- **o tipo da resposta da TransLog chega ao serviço**: `Optional<TransLogClient.TransLogQuoteResponse>` (`CheckoutService.java:277` e `284`);
- **a conversão de centavos para reais fica fora**: `new BigDecimal(response.getPriceCents()).divide(new BigDecimal("100"), 2, RoundingMode.HALF_EVEN)` (`CheckoutService.java:285`);
- **a conversão de horas para dias fica fora**: `(int) Math.ceil(response.getEtaHours() / 24.0)` (`CheckoutService.java:286`).

Há também uma perda de informação na fronteira. O adaptador traduz duas situações diferentes da TransLog para o mesmo `Optional.empty()`: o `ZIP_NOT_SERVED` (`TransLogClient.java:46–47`) e "fora do ar" (outro status, linha 61; exceção ou tempo esgotado, linhas 62–63). O `if` da linha 46 devolve o mesmo que a linha 49, então ele não distingue nada. Do lado de fora, `CheckoutService.java:279–281` só pode transformar as duas situações em `SHIPPING_METHOD_UNAVAILABLE`, e o `CARRIER_UNAVAILABLE` não aparece em lugar nenhum do código (`grep` sem resultado). Se a resposta fica certa é assunto do comportamento. Aqui, isso mostra que a interface do adaptador não foi desenhada na nossa língua.

A peça da TransLog existe e é a única que faz HTTP, mas o vocabulário vaza por várias linhas: o código de serviço, as gramas, os centavos, as horas e os nomes `priceCents`/`etaHours`.

Conclusão: malfeito

**P6-exagero, as entregas sem transportadora.** A retirada e o motoboy são valores fixos, declarados como constantes:
- `COURIER_COST = new BigDecimal("18.00")`, `STORE_PICKUP_DAYS = 1` e `COURIER_DELIVERY_DAYS = 0` (`CheckoutService.java:25`, `31–32`);
- eles são devolvidos direto na regra da entrega: `return new ShippingInfo(COURIER_COST, COURIER_DELIVERY_DAYS);` (`CheckoutService.java:269`) e `return new ShippingInfo(BigDecimal.ZERO, STORE_PICKUP_DAYS);` (`CheckoutService.java:271`);
- o limite de 5 kg do motoboy também está ali (`CheckoutService.java:266–268`).

Não há interface de transportadora, nem "cliente" do motoboy. A retirada também não passa pela porta da TransLog: só `ECONOMY`/`EXPRESS` chamam `transLogClient.getQuote` (`CheckoutService.java:272–277`). A cadeia de `if` sobre as entregas é assunto do P1 (Strategy), não deste controle.

Conclusão: contenção

**E, estrutura sem necessidade fora dos pontos (perto do Adapter).**
- O `TransLogClient` não tem interface com implementação única. Ele é injetado direto (`CheckoutService.java:35`).
- `ShippingInfo` é um portador de valor privado de dois campos (`CheckoutService.java:382–386`), não uma camada.
- `TransLogQuoteResponse` tem `@lombok.Builder` (`TransLogClient.java:70`) e é montado com builder num único lugar (linhas 55–58). É uma anotação da Lombok sobre um record de dois campos: excesso de cerimônia, não estrutura nova.

Não encontrei camada de repasse, fábrica ou interface supérflua em volta da integração.

Conclusão: contenção

## O que mudaria primeiro

1. **Levar toda a tradução para dentro do `TransLogClient`.** A interface dele passaria a ser algo como `quote(ShippingMethod, String zip, BigDecimal weightKg)`, devolvendo uma cotação nossa (frete em reais, prazo em dias). A escolha de `"STD"`/`"EXP"`, a conversão para gramas, a divisão por 100 e o `ceil(horas/24)` sairiam de `CheckoutService.java:273–286`. Só assim trocar de transportadora mexe numa peça só, que é o motivo de o enunciado mencionar essa troca.
2. **Fazer o adaptador distinguir, na nossa língua, "não atende o CEP" de "fora do ar".** Por exemplo, com dois resultados ou duas exceções próprias, em vez do mesmo `Optional.empty()`. Assim o serviço pode responder `SHIPPING_METHOD_UNAVAILABLE` ou `CARRIER_UNAVAILABLE` sem conhecer os códigos da TransLog.

## Dúvidas

O `TransLogQuoteResponse` é uma classe aninhada no próprio `TransLogClient`, com nomes de campo da TransLog (`priceCents`, `etaHours`). Segundo a régua, declarar esses nomes do lado dela não é vazamento. Por isso não contei a declaração (`TransLogClient.java:71–74`). Contei o uso dela e dos getters no `CheckoutService` (linhas 277, 284–286).
