# Adapter

## Visão geral

O agente não fez uma peça só para a TransLog. Tudo o que fala com ela está num método privado, `calculateTranslog`, dentro de `ShippingService`, e essa classe é também a regra das entregas. É lá que fica o `switch` sobre as quatro formas de entrega, os valores fixos da retirada e do motoboy e os dias a mais do Norte e do Nordeste. Nesse método estão a conversão de quilos para gramas, a montagem da URL `/v2/quote`, a chamada HTTP, a leitura de `price_cents` e `eta_hours`, a conversão de centavos para reais e de horas para dias e, no meio de tudo isso, a regra regional. Os códigos `"STD"` e `"EXP"` são escolhidos no próprio `switch` da regra.

Há um ponto forte: de `ShippingService` para fora, o resto do serviço só vê a nossa língua. `CheckoutService` manda o peso em quilos e recebe `ShippingQuote` com o frete em reais (`cost`) e o prazo em dias (`deliveryDays`). Nenhum termo da TransLog aparece fora desse arquivo. O problema mais sério é a mistura dentro dele: trocar de transportadora obrigaria a reescrever a classe que também guarda a regra das quatro entregas.

As entregas sem transportadora estão do tamanho do problema: são valores fixos no `switch`, sem interface e sem adaptador.

## Item a item

**P6, o vocabulário da transportadora fica do lado dela?** O arquivo que faz a chamada HTTP é `src/main/java/com/shop/service/ShippingService.java`. Ele é a regra de entrega do serviço, e não uma peça só da TransLog:
- **a escolha do código de serviço fica na regra:** `case ECONOMY -> calculateTranslog(zipCode, weightKg, "STD", region, 7)` e `case EXPRESS -> ... "EXP" ...` estão no `switch` que decide as quatro entregas (`ShippingService.java:41-46`);
- **o mesmo `switch` traz as outras entregas:** a retirada e o motoboy estão nele, ao lado das chamadas à TransLog (`ShippingService.java:44-45`);
- **o método da TransLog faz tudo de uma vez:**
  - converte quilos para gramas: `weightKg.multiply(new BigDecimal("1000")).intValue()` (`:50`);
  - monta a rota `/v2/quote` com `weight_g` (`:51-52`);
  - faz a chamada HTTP (`:55-61`);
  - lê `ZIP_NOT_SERVED`, `price_cents` e `eta_hours` (`:69`, `:73-74`);
  - converte centavos para reais (`:76-77`) e horas para dias (`:79`);
- **a regra regional está misturada com a leitura da resposta:** o `switch (region)` com Norte +3 e Nordeste +2 (`:81-85`) fica nesse mesmo método, entre a leitura da resposta e o `return`;
- **um parâmetro que não serve para nada:** `baseDays` (7 e 0) é recebido em `:49` e nunca usado.

Fora do arquivo, o vocabulário não aparece. Busquei `price_cents|eta_hours|priceCents|etaHours|weight_g|ZIP_NOT_SERVED|"STD"|"EXP"|/v2/quote|HttpClient` em `src/main/java`: todas as ocorrências estão em `ShippingService.java`. `CheckoutService` chama `calculateShipping` com `totalWeight` em quilos e lê `shipping.cost` e `shipping.deliveryDays` (`CheckoutService.java:53-57`, `:192-194`). Também não existe nenhuma interface no projeto (nenhum `interface` nem `implements` em `src/main/java`).

Então a fronteira com o resto do serviço está na nossa língua. Mas o lado da TransLog não é uma peça só dela: a chamada e a leitura da resposta ficam dentro da regra de entrega, junto com a escolha de `"STD"`/`"EXP"` e com os dias a mais por região. Isso é o que a régua chama de omissão.
Conclusão: omissão

**P6-exagero, as entregas sem transportadora.** A retirada é `new ShippingQuote(BigDecimal.ZERO, 1)` e o motoboy é `new ShippingQuote(new BigDecimal("18.00"), 0)`, ambos no próprio `switch` da entrega (`ShippingService.java:44-45`). Não há porta de transportadora, nem "cliente" do motoboy, e a retirada não passa por nada que imite a TransLog. A regra do limite de 5 kg do motoboy fica em `CheckoutService.java:164-166`; o lugar dela é assunto do Strategy, e aqui não cria estrutura nenhuma. É a resposta simples.
Conclusão: contenção

**E, estrutura sem necessidade fora dos pontos.** Perto deste pattern não há estrutura que ninguém pediu. O projeto não tem nenhuma interface ou classe abstrata; a única herança é `BusinessException extends RuntimeException` (`exception/BusinessException.java:3`). `ShippingService.ShippingQuote` (`ShippingService.java:30-38`) é só um objeto de retorno, não uma camada. Não encontrei fábrica, builder ou camada que só repassa ligada à entrega ou à transportadora.
Conclusão: contenção

## O que mudaria primeiro

1. **Tirar de `ShippingService` tudo o que é da TransLog e pôr numa classe só dela** (por exemplo, `TransLogClient`). A interface pública seria algo como `quote(ShippingMethod, zip, weightKg)` → frete em reais e prazo em dias. Por dentro, essa classe faria a tradução `ECONOMY -> "STD"` / `EXPRESS -> "EXP"`, a conversão para gramas, a chamada, a leitura de `price_cents`/`eta_hours`/`ZIP_NOT_SERVED` e as conversões de volta. Assim, trocar de transportadora mexeria só nessa classe.
2. **Tirar os dias a mais do Norte e do Nordeste de dentro da leitura da resposta** (`:81-85`) e devolvê-los à regra da entrega. A regra é nossa, não da transportadora; hoje ela iria embora junto com a TransLog numa troca.

## Dúvidas

- **Omissão ou malfeito.** O código da TransLog está todo num método privado de uma classe só, e nada vaza para fora dela. Quem visse `ShippingService` como "a peça da TransLog" poderia chamar isto de malfeito, com o `"STD"`/`"EXP"` e a regra regional como vazamentos internos. Escolhi **omissão** porque a classe não é só da TransLog: ela é a regra de todas as entregas (as quatro opções, os valores fixos, os dias regionais). Pela régua, isso é a chamada HTTP e a leitura da resposta "no meio do cálculo do frete ou da regra de entrega, misturada com o resto".
