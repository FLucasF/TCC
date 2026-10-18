Já li o adaptador da TransLog, as quatro entregas e o ponto onde o serviço de checkout chama a entrega. Agora escrevo a correção.

# Padrões: Adapter

## Visão geral

O agente juntou a conversa com a TransLog numa classe só, `TransLogClient`, sem uma interface de transportadora na frente. Ela faz a chamada HTTP, monta a rota `/v2/quote` com `weight_g`, lê `price_cents` e `eta_hours`, reconhece `ZIP_NOT_SERVED` e faz as três conversões de unidade dentro dela: quilos para gramas, centavos para reais e horas para dias. Quem chama passa o peso em quilos e recebe o frete em reais e o prazo em dias. Esse é o ponto forte. O problema mais sério é que os códigos de serviço da TransLog (`"STD"` e `"EXP"`) não são traduzidos no adaptador. Eles estão escritos nas estratégias `EconomyShipping` e `ExpressShipping`, e o adaptador recebe o código dela como parâmetro. Por isso, trocar de transportadora mexe também nessas duas classes. Há um segundo problema, menor: a resposta do adaptador é só um "disponível ou não" e mistura o CEP não atendido com a TransLog fora do ar. Com isso, o `CheckoutService` precisa perguntar qual é a entrega para escolher o erro. No controle negativo, a retirada e o motoboy guardam os seus valores fixos na própria classe, sem passar por nada que imite uma transportadora.

## Item a item

**P6, o vocabulário da transportadora fica do lado dela?** O lado da TransLog é `src/main/java/com/loja/external/TransLogClient.java`, que é o único arquivo da pasta `external`, com o record de resposta `TransLogQuote` aninhado nele.

O que fica do lado dela:
- a rota e os parâmetros `/v2/quote?zip=…&weight_g=…&service=…` (`TransLogClient.java:28`);
- os campos `price_cents` e `eta_hours` (`TransLogClient.java:45-46`) e o erro `ZIP_NOT_SERVED` (`TransLogClient.java:35`);
- as três unidades: gramas (`weightKg.multiply(1000)`, `TransLogClient.java:26`), centavos para reais (`TransLogClient.java:48`) e horas para dias com arredondamento para cima (`Math.ceil(etaHours / 24.0)`, `TransLogClient.java:49`);
- o timeout de 2 segundos (`TransLogClient.java:19-20`).

`TransLogQuote` devolve `price` em reais e `deliveryDays` em dias (`TransLogClient.java:57-70`). A busca por `price_cents`, `eta_hours`, `priceCents`, `etaHours`, `ZIP_NOT_SERVED`, `weight_g`, `/24` e `1000` fora de `external/` não acha nada que seja da TransLog. As únicas ocorrências são `Duration.ofHours(48)` (`OrderService.java:300`, a expiração do pedido) e o limite do boleto (`BankSlipPayment.java:24`). Nenhuma estratégia converte unidade: as duas passam `totalWeightKg` em quilos (`EconomyShipping.java:23`, `ExpressShipping.java:23`).

O que vaza: os códigos de serviço da TransLog estão fora do adaptador. `EconomyShipping` chama `transLogClient.getQuote(..., "STD")` em `EconomyShipping.java:23` e `:40`. `ExpressShipping` chama `getQuote(..., "EXP")` em `ExpressShipping.java:23` e `:40`. O método do adaptador recebe o código dela como `String service` (`TransLogClient.java:24`), em vez de receber a nossa entrega e traduzir `ECONOMY -> STD` e `EXPRESS -> EXP` lá dentro. A régua chama isso de vazamento: uma transportadora nova, com outros códigos, obriga a mudar as duas estratégias. As estratégias também dependem da classe concreta `TransLogClient` (`EconomyShipping.java:4,10`, `ExpressShipping.java:4,10`). A régua não exige uma porta, mas isso reforça o mesmo efeito na troca.

Um ponto à parte, sobre como a resposta do adaptador traduz os erros: o CEP não atendido (`TransLogClient.java:35-36`), a resposta nula (`:41-42`), qualquer outro erro (`:38`) e qualquer exceção, inclusive o timeout (`:52-53`), viram todos o mesmo `TransLogQuote(false, null, null)`. A nossa língua fica sem a diferença entre "não atende o pedido" e "fora do ar". O `CheckoutService` compensa nomeando as entregas: `request.getShippingMethod().equals("ECONOMY") || ... equals("EXPRESS")` decide entre `CARRIER_UNAVAILABLE` e `SHIPPING_METHOD_UNAVAILABLE` (`CheckoutService.java:200-201`). Isso não é vocabulário da TransLog, então não conta como vazamento. Mas mostra que o adaptador não entrega tudo o que o resto do código precisa saber. O comportamento disso, o CEP não atendido parecendo responder `CARRIER_UNAVAILABLE`, é medido em `behavior.md`, não aqui.

Fora os códigos de serviço, o isolamento é bom. Mas a régua manda tratar qualquer termo da TransLog fora do lado dela como vazamento, e vazamento é a omissão deste ponto.
Conclusão: omissão

**P6-exagero, as entregas sem transportadora.** `StorePickupShipping` devolve `new ShippingResult(BigDecimal.ZERO, 1)` (`StorePickupShipping.java:16`). `CourierShipping` devolve `new ShippingResult(new BigDecimal("18.00"), 0)` (`CourierShipping.java:16`) e tem o limite de 5 kg na própria classe (`CourierShipping.java:21`). Nenhuma das duas recebe o `TransLogClient` ou algum "cliente" de transportadora, e não existe porta de transportadora que elas implementem. Elas implementam só a `ShippingStrategy` (`ShippingStrategy.java:6-10`), que é a variação da entrega (P1), e não uma imitação da TransLog. O valor e o prazo ficam na própria regra de cada entrega.
Conclusão: contenção

**E, estrutura sem necessidade perto do Adapter.** Não há interface com uma implementação só em volta da TransLog: `TransLogClient` é concreta e não tem fábrica nem camada de repasse. `ShippingResult` (`ShippingResult.java:5-16`) é o valor de retorno das estratégias de entrega, não uma camada a mais. Não achei estrutura sem pedido perto deste pattern.
Conclusão: contenção

## O que mudaria primeiro

1. **Levar a tradução dos códigos de serviço para dentro do adaptador.** O método passaria a receber a nossa entrega, ou algo como "econômica" e "expressa", e o `TransLogClient` faria o `ECONOMY -> "STD"` e o `EXPRESS -> "EXP"`. Com isso, `EconomyShipping.java:23,40` e `ExpressShipping.java:23,40` deixam de conhecer a língua da TransLog, e uma troca de transportadora mexe só em `external/`. Se houver uma interface de transportadora, as estratégias também deixam de depender da classe concreta.
2. **Fazer a resposta do adaptador distinguir "não atende este pedido" de "fora do ar"**, com dois resultados diferentes ou uma exceção própria para a indisponibilidade, em vez de um único `available=false`. Isso tira o `equals("ECONOMY") || equals("EXPRESS")` de `CheckoutService.java:200`. De quebra, deixa de ser preciso cotar duas vezes na TransLog: hoje `isAvailable` e `calculate` chamam `getQuote` cada um (`EconomyShipping.java:23` e `:40`).

## Dúvidas

- **Omissão ou malfeito.** Aqui a estrutura existe e cobre quase tudo; só os códigos de serviço ficaram de fora. Pela definição geral, isso se parece com malfeito. Segui a régua do Adapter, que chama qualquer vazamento de omissão deste ponto, e expliquei no texto que o isolamento das unidades e dos campos está certo.
- **O erro que mistura dois casos.** Esse problema não está na lista de vocabulário da régua: é sobre como o adaptador traduz os erros, não sobre um termo da TransLog. Por isso registrei no texto e não contei para a conclusão do P6.
- **A soma de dias por região.** O `switch` sobre `"NORTH"`/`"NORTHEAST"`, repetido em `EconomyShipping.java:29-33` e `ExpressShipping.java:29-33`, fica fora do lado da TransLog e não é vocabulário dela. É regra nossa (P1/P5), por isso não entrou nesta correção.
