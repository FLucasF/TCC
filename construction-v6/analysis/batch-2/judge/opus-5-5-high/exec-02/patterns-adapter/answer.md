# Padrões: Adapter

## Visão geral

O agente juntou toda a conversa com a TransLog numa classe só, `src/main/java/com/loja/infrastructure/TransLogClient.java`. É ela que monta a rota `/v2/quote`, converte quilos em gramas, troca a nossa entrega por `STD`/`EXP`, lê `price_cents` e `eta_hours` e devolve o frete em reais e o prazo em dias. As unidades e os campos da TransLog não saem dessa classe, e esse é o ponto forte. O defeito é estreito, mas existe: o erro `ZIP_NOT_SERVED` sai da classe como um tipo dela, `TransLogClient.ZipNotServedException`. As estratégias de entrega capturam esse tipo e traduzem o erro para o nosso, e depois o `ValidationService` separa "CEP não atendido" de "fora do ar" procurando o texto `"not served"` dentro da mensagem da exceção. Ou seja, parte do trabalho de tradução, que é do adaptador, foi feita fora dele. Também não há uma porta de transportadora: as estratégias dependem direto da classe concreta e dos tipos aninhados nela. A retirada e o motoboy ficaram simples, como devem.

## Item a item

**P6, o vocabulário da transportadora fica do lado dela?**

O enunciado desta execução tem o vocabulário estrangeiro (`prompt.md:205-213`), então o ponto mede alguma coisa aqui. O lado da TransLog é `src/main/java/com/loja/infrastructure/TransLogClient.java`, e nele estão:
- **a rota e os parâmetros:** `/v2/quote?zip=...&weight_g=...&service=...` (`:30-31`);
- **quilos para gramas:** `(int) (weightKg * 1000)` (`:27`);
- **a nossa entrega para o código dela:** `ECONOMY -> "STD"`, senão `"EXP"` (`:28`), a partir de um enum próprio do lado dela, `TransLogService` (`:66-69`);
- **o erro dela:** `ZIP_NOT_SERVED` (`:50`);
- **os campos da resposta:** `price_cents` e `eta_hours`, lidos em `priceCents` e `etaHours` (`:56-57`);
- **centavos para reais:** `priceCents / 100.0` (`:81`);
- **horas para dias:** `Math.ceil(etaHours / 24.0)` (`:85`).

O resto do código fala a nossa língua. As estratégias passam o peso em quilos e leem só `getPriceReais()` e `getDeliveryDays()` (`src/main/java/com/loja/domain/strategies/shipping/EconomyShipping.java:18-20,33` e `ExpressShipping.java:18-20,33`). Fora do `TransLogClient.java`, nenhum arquivo usa `price_cents`, `eta_hours`, `weight_g`, `STD`/`EXP`, centavos, horas ou gramas (busquei com `grep` em todo o `src/main`).

O que vaza:
- **O código de erro dela como tipo.** `ZIP_NOT_SERVED` virou `TransLogClient.ZipNotServedException`, e é a estratégia que captura esse tipo e o traduz para o nosso `ShippingUnavailableException` (`EconomyShipping.java:21-24`, `ExpressShipping.java:21-24`). Essa tradução é trabalho do adaptador e ficou fora dele.
- **A mesma distinção, lida no texto.** As estratégias põem a distinção da TransLog só no texto da mensagem (`"ZIP not served"` × `"Carrier unavailable"`). O `ValidationService` decide entre `SHIPPING_METHOD_UNAVAILABLE` e `CARRIER_UNAVAILABLE` procurando `"unavailable"` e `"not served"` nesse texto (`src/main/java/com/loja/services/ValidationService.java:126-130`). Assim, o "CEP não atendido" da TransLog atravessa duas camadas até o validador.
- **Os tipos dela no domínio.** As estratégias importam e usam `TransLogClient.TransLogQuote`, `TransLogClient.TransLogService` e `TransLogClient.TransLogException` (`EconomyShipping.java:6,18-23,31-32`). Sem uma porta de transportadora, trocar de transportadora mexe nas duas estratégias, e não só no adaptador. A régua diz que a falta de interface não decide o ponto sozinha, mas aqui ela vem junto com a tradução de erro feita fora do adaptador.

Uma observação à parte: `calculateDeliveryDays` faz uma segunda cotação com CEP `"00000000"` e peso `1.0` (`EconomyShipping.java:31-32`) e, se der erro, usa 7 ou 2 dias inventados (`:36`, `ExpressShipping.java:36`). É um defeito de comportamento e de desenho da estratégia, e não um vazamento de vocabulário. Não pesou neste item.

Dúvida sobre a conclusão: se `ZipNotServedException` não contar como "termo da TransLog", sobram só os tipos concretos usados nas estratégias, e o item ficaria perto do acerto. Contei como vazamento pela analogia com a régua, que trata `priceCents` (o nome do campo dela no nosso código) como vocabulário dela. `ZipNotServedException` é o nome do erro dela no nosso código.
Conclusão: malfeito

**P6-exagero, as entregas sem transportadora.** A retirada devolve `Money.zero()` e 1 dia direto na regra (`src/main/java/com/loja/domain/strategies/shipping/StorePickupShipping.java:11,16`). O motoboy devolve a constante `COST = Money.of(18.00)`, 0 dias e o limite de 5 kg (`CourierShipping.java:8-9,13,18,23`). Nenhuma das duas recebe um cliente ou uma porta de transportadora; em `Main.java:48-49` são criadas sem dependência nenhuma. Não há "cliente do motoboy" nem retirada passando pela TransLog.
Conclusão: contenção

**E, estrutura sem necessidade fora dos pontos.** Perto deste pattern (a fronteira com sistemas de fora), há quatro interfaces de repositório com uma implementação cada: `CustomerRepository`/`CustomerRepositoryImpl`, `OrderRepository`/`OrderRepositoryImpl`, `ProductRepository`/`ProductRepositoryImpl` e `NotificationRepository`/`NotificationRepositoryImpl` (`src/main/java/com/loja/repositories/impl/*.java:12-13`). Nada as substitui: o único teste é `src/test/java/com/loja/MoneyTest.java`, sem dublês, e `Main.java` liga sempre a implementação JDBC. É uma convenção comum e o custo é baixo, mas é o caso que a régua descreve: interface de uma implementação que nada troca. Do lado da TransLog não criaram estrutura a mais (nem a interface que caberia ali). Os observadores do pagamento (`domain/observers/*`) ficam para os outros patterns.
Conclusão: exagero

## O que mudaria primeiro

1. **Levar a tradução dos erros para dentro do adaptador.** O `TransLogClient` passaria a lançar, ele mesmo, duas exceções do nosso domínio: "entrega indisponível para o CEP" e "transportadora fora do ar". O `ValidationService` capturaria esses dois tipos, em vez de procurar `"not served"` no texto. Isso tira o `ZipNotServedException` das estratégias e acaba com a decisão por texto, que é frágil.
2. **Criar uma porta de transportadora no domínio.** Seria algo como `CarrierQuote quote(String zip, double weightKg, ShippingMethod method)`, que o `TransLogClient` implementa e que devolve um tipo nosso (frete em reais, prazo em dias). Com uma cotação só, usada para o frete e para o prazo, trocar de transportadora passa a mexer só no adaptador e em `Main.java`.

## Dúvidas

- Contei `ZipNotServedException` como vocabulário da TransLog fora do lado dela, pelo mesmo motivo que a régua conta `priceCents`. Uma leitura que só conte formato, campos, códigos de serviço e unidades daria acerto no P6.
- Contei as interfaces de repositório no E, como fronteira com o banco, perto do Adapter. Uma leitura que limite o E ao lado da TransLog daria contenção.
