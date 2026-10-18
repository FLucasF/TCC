# padrões: Adapter

## Visão geral

O agente não separou a conversa com a TransLog do resto. Toda ela fica dentro de `CheckoutService.java`, a mesma classe que valida o pedido, calcula o cupom, o frete, o seguro, o ajuste do pagamento e o crédito do clube. A chamada HTTP e a leitura do JSON estão num método privado (`queryTranslog`, `CheckoutService.java:388-418`) e num record privado aninhado (`TranslogResponse`, `:480-484`). O vocabulário dela aparece espalhado por dois trechos do cálculo, e o primeiro está copiado no segundo:
- a escolha de `"STD"`/`"EXP"`;
- a conversão de quilos para gramas;
- o `ZIP_NOT_SERVED`;
- a divisão de `priceCents` por 100;
- a divisão de `etaHours` por 24.

Trocar de transportadora obrigaria a mexer no meio do serviço de checkout, em três lugares. Esse é o problema mais sério. A parte que está boa é a das entregas sem transportadora: a retirada e o motoboy são valores fixos escritos na própria regra, sem nenhuma estrutura de transportadora em volta. O projeto também não tem nenhuma interface ou camada de sobra.

## Item a item

**P6, o vocabulário da transportadora fica do lado dela?** Não há um lado da TransLog separado. A chamada (`HttpURLConnection`, a rota `/v2/quote`, o parâmetro `weight_g`, `CheckoutService.java:390-394`) e a leitura de `price_cents`/`eta_hours` (`:412-413`) moram num método privado de `CheckoutService`. Essa é a classe do cálculo do frete (`calculateShipping`, `:337`) e de todo o resumo (`calculateSummary`, `:199`). Fora desse método, o vocabulário dela aparece duas vezes, no meio das regras:

- na validação da entrega (`validateShippingMethod`):
  - o código de serviço escolhido ali: `String service = method == ECONOMY ? "STD" : "EXP"` (`:144`);
  - a unidade dela: `int weightGrams = (int) (totalWeight * 1000)` (`:145`);
  - o erro dela: `response.error.equals("ZIP_NOT_SERVED")` (`:151`);
- no cálculo do frete (`calculateShipping`):
  - o mesmo `"STD"`/`"EXP"` (`:362`) e a mesma conversão para gramas (`:363`), copiados;
  - os campos e as unidades dela: `response.priceCents / 100.0` (`:374`) e `Math.ceil(response.etaHours / 24.0)` (`:375`). Os dias a mais do Norte e do Nordeste são somados logo em seguida (`:377-385`), ali mesmo, junto com a conversão.

Nada no resto do código pede uma cotação em quilos e recebe reais e dias. Quem calcula o frete trabalha direto com gramas, centavos, horas e os códigos dela.

Conclusão: omissão

**P6-exagero, as entregas sem transportadora.** A retirada e o motoboy estão em `calculateShipping` como valores fixos: `return new ShippingInfo(0, 1)` para `STORE_PICKUP` (`CheckoutService.java:346-348`) e `return new ShippingInfo(18.0, 0)` para `COURIER` (`:350-355`). Não passam por nenhuma porta de transportadora, e não há "cliente" do motoboy. Ficarem na mesma cadeia de `if` sobre as entregas é questão do pattern Strategy, não deste.

Conclusão: contenção

**E, estrutura sem necessidade fora dos pontos.** O projeto não tem nenhuma interface, classe abstrata, `implements`, fábrica ou builder em `src/main` (busca por `interface `, `implements `, `abstract class`, `Factory`, `Builder`, `Client`, `Gateway`: nenhum resultado). Perto da transportadora existem só `ShippingInfo` (`:470-478`) e `TranslogResponse` (`:480-484`), dois recipientes de dados privados e aninhados, que não são camadas.

Conclusão: contenção

## O que mudaria primeiro

1. Tirar a conversa com a TransLog de `CheckoutService` e pôr numa classe própria, por exemplo `TranslogCarrier.quote(zip, region?, weightKg, ShippingMethod)`, que devolva o frete em reais e o prazo em dias. Esse resultado pode ser "atende", "CEP não atendido" ou "fora do ar". A escolha de `STD`/`EXP`, a conversão para gramas, a leitura de `price_cents`/`eta_hours`, a divisão por 100 e por 24 e o `ZIP_NOT_SERVED` iriam todos para dentro dela. Assim, trocar de transportadora mexeria só nessa peça.
2. Fazer a validação e o cálculo usarem essa mesma peça, uma vez só. Hoje o trecho de `:138-147` é cópia de `:340-365`, e o pedido chega a consultar a TransLog duas ou três vezes: na validação, no cálculo e de novo no FREESHIPPING (`:303`).

## Dúvidas

O método `queryTranslog` e o record `TranslogResponse` já separam um pouco a chamada HTTP. Lidos como uma "peça só dela", o ponto seria **malfeito**, com vazamento nas linhas 144-145, 151, 362-363 e 374-375. Escolhi **omissão** porque a régua define o lado da TransLog pelos arquivos que fazem a chamada. Aqui esse arquivo é o próprio `CheckoutService`, que calcula o frete e todo o resumo, e o método privado não pode ser trocado nem usado por fora dessa classe. Ou seja, a chamada fica "no meio do cálculo do frete, misturada com o resto". Nas duas leituras, o vocabulário da TransLog vaza para a regra da entrega.
