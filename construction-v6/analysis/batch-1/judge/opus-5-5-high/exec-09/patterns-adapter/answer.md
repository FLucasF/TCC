# padrões: Adapter

## Visão geral

A conversa com a TransLog fica numa peça própria, `external/TransLogClient.java`. É ela que monta a chamada HTTP para `/v2/quote`, passa o peso de quilos para gramas, lê `price_cents` e `eta_hours` e transforma centavos em reais e horas em dias. Ela devolve um `TransLogQuote` que já fala a nossa língua: preço em reais e prazo em dias. Esse é o ponto forte do serviço. O ponto mais sério é que o código de serviço da TransLog (`"STD"` / `"EXP"`) é escolhido do lado de fora, nas duas estratégias de entrega. Assim, uma troca de transportadora mexeria nelas também. O segundo ponto é que o adaptador junta "CEP não atendido" e "fora do ar" numa resposta só, `available=false`. Com isso, o `CheckoutService` precisa decidir o erro perguntando qual é a entrega. A retirada e o motoboy ficaram com valores fixos dentro da própria regra, sem estrutura a mais.

## Item a item

**P6, o vocabulário da transportadora fica do lado dela?** O lado da TransLog é `external/TransLogClient.java`, o único arquivo do pacote `external`. Esse arquivo guarda bem quase todo o vocabulário dela:
- a rota e os parâmetros: `/v2/quote?zip=…&weight_g=…&service=…` (`TransLogClient.java:28`);
- a conversão de quilos para gramas (`TransLogClient.java:26`);
- a leitura de `"error"` / `"ZIP_NOT_SERVED"` (`TransLogClient.java:33-38`);
- a leitura de `price_cents` / `eta_hours` (`TransLogClient.java:45-46`);
- a conversão de centavos para reais (`TransLogClient.java:48`) e de horas para dias arredondando para cima (`TransLogClient.java:49`);
- o timeout de 2 s (`TransLogClient.java:19-20`).

O resultado volta como `TransLogQuote(available, price, deliveryDays)` (`TransLogClient.java:57-71`), com reais e dias. As estratégias recebem o peso em quilos e não fazem nenhuma conta de unidade. A busca por `cents`, `hours`, `weight_g`, `ZIP_NOT` e `quote` fora desse arquivo só acha o uso do `TransLogQuote` já traduzido.

O vazamento está nos **códigos de serviço**. `EconomyShipping.java:23` e `:40` chamam `getQuote(…, "STD")`, e `ExpressShipping.java:23` e `:40` chamam `getQuote(…, "EXP")`. A tradução `ECONOMY -> "STD"` e `EXPRESS -> "EXP"` é trabalho do adaptador, mas aqui ela mora nas estratégias de entrega, que conhecem os nossos casos. Na prática, o método `getQuote` aceita um `String service` cru da API dela (`TransLogClient.java:24`). Se a transportadora mudar, as duas estratégias também mudam.

Há também uma tradução incompleta no adaptador. Ele devolve `available=false` tanto para `ZIP_NOT_SERVED` (`:36`) quanto para resposta nula ou qualquer exceção (`:42`, `:53`). O nosso lado fica sem saber se a transportadora não atende aquele CEP ou se está fora do ar. Para decidir, o `CheckoutService.java:199-203` usa o código da entrega (`"ECONOMY"` / `"EXPRESS"`), e por isso qualquer recusa da TransLog vira `CARRIER_UNAVAILABLE`. Não é vazamento de termo, mas a informação se perdeu na fronteira, e o efeito no resultado cabe à dimensão do comportamento.

À parte: cada estratégia chama a TransLog duas vezes por cálculo, uma em `isAvailable` e outra em `calculate` (`EconomyShipping.java:23,40`).

O lado da TransLog existe e cuida do formato, dos campos e das unidades, mas o código de serviço dela vaza para as estratégias.
Conclusão: malfeito

**P6-exagero, as entregas sem transportadora.** `StorePickupShipping.java:16` devolve `new ShippingResult(BigDecimal.ZERO, 1)`. `CourierShipping.java:16` devolve `new ShippingResult(new BigDecimal("18.00"), 0)`, e o limite de 5 kg está na própria regra (`CourierShipping.java:21`). Nenhuma das duas usa o `TransLogClient`, um "cliente" próprio ou uma porta de transportadora. Os valores fixos ficam na regra da entrega, que é a resposta do tamanho do problema.
Conclusão: contenção

**E, estrutura sem necessidade fora dos pontos.** Perto do Adapter não há estrutura a mais. O pacote `external` tem um arquivo só, e não existe interface de transportadora com uma implementação única nem camada que só repassa. A busca por `interface`, `Builder` e `Factory` no `src/main` só acha os repositórios JPA, as interfaces das estratégias e as do State (`state/OrderState.java`, `state/OrderStateContext.java`). Essas pertencem a outros patterns e não são avaliadas aqui.
Conclusão: contenção

## O que mudaria primeiro

1. **Levar a escolha do código de serviço para dentro do adaptador.** O `TransLogClient` passaria a receber o nosso caso, por exemplo o código da entrega ou um tipo nosso de "econômica/expressa", e ele mesmo traduziria para `"STD"`/`"EXP"`. Assim as estratégias param de usar o vocabulário da TransLog, e trocar de transportadora mexe só nessa peça.
2. **Fazer o adaptador separar "não atende" de "fora do ar".** Pode ser com dois resultados diferentes ou com duas exceções do nosso lado. Com isso, o `CheckoutService` não precisaria mais perguntar se a entrega é `ECONOMY`/`EXPRESS` para escolher o erro, e a fronteira passaria toda a informação que a TransLog dá.

## Dúvidas

Para o P6, tratei o problema "não atende" vs. "fora do ar" como tradução incompleta do adaptador, e não como vazamento: nenhum termo, campo ou unidade da TransLog aparece fora do arquivo por causa disso. Ele não pesou na conclusão. O que decidiu o `malfeito` foram os `"STD"`/`"EXP"` nas estratégias, que a régua cita como vazamento.

Também não contei como vazamento o nome `TransLogClient`/`TransLogQuote` usado nas estratégias. A régua diz que a interface é opcional, e esse nome não faz parte do vocabulário da API dela (formato, campos, códigos, unidades).
