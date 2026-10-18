# Padrões: Adapter

## Visão geral

O agente isolou toda a conversa com a TransLog numa classe só, `TransLogService` (`src/main/java/com/example/application/service/TransLogService.java`). Ela recebe o CEP, o peso em quilos e a nossa `ShippingMethod`. Dentro dela ficam a tradução para `STD`/`EXP`, a conversão para gramas, a montagem da rota `/v2/quote`, a leitura de `price_cents`/`eta_hours`, a detecção do `error` e a conversão de volta para reais e dias. O que ela devolve é um valor nosso, `OrderSummaryCalculator.ShippingQuote(cost, days)`. Nenhum termo, campo ou unidade da TransLog aparece fora dessa classe, e nisso o desenho acerta.

Os pontos fracos não vazam vocabulário, mas tocam a fronteira:
- Não há uma porta (interface) de transportadora. O enunciado permite isso, mas trocar de transportadora vai obrigar a mexer nos dois lugares que chamam a classe concreta.
- O adaptador devolve `null` para três falhas diferentes: o CEP não atendido, a resposta inesperada e a queda. Com isso ele não entrega ao nosso lado a distinção que o enunciado pede. A `CarrierUnavailableException` existe e nunca é lançada.

A retirada e o motoboy ficaram como valores fixos na própria regra de entrega, sem nenhuma peça de transportadora.

## Item a item

**P6, o vocabulário da transportadora fica do lado dela?** O lado da TransLog é só o `TransLogService.java`. Os outros arquivos da pasta `application/service` conhecem os nossos casos, e não há records da resposta: ela é lida num `Map`. Tudo o que é dela está dentro dessa classe:
- a tradução da nossa entrega para o código de serviço, `method == ShippingMethod.ECONOMY ? "STD" : "EXP"` (`TransLogService.java:31`);
- quilos para gramas (`:32`);
- a rota e os parâmetros, `"%s/v2/quote?zip=%s&weight_g=%d&service=%s"` (`:34`);
- o tempo limite de 2 s (`:24`, `:39`);
- a detecção de `error` (`:52`);
- a leitura de `price_cents` e `eta_hours` (`:56-57`);
- centavos para reais (`:59`) e horas para dias, arredondando para cima (`:60`).

A saída é `new OrderSummaryCalculator.ShippingQuote(price, days)` (`:62`), em reais e dias. Os dois lugares que chamam a classe passam o peso em quilos e recebem esse valor: `OrderSummaryCalculator.java:148` e `OrderService.java:173-177`. O peso vem de `calculateTotalWeight`, em `BigDecimal` de quilos (`OrderSummaryCalculator.java:186-194`, `OrderService.java:224-231`). Procurei `price_cents`, `priceCents`, `eta_hours`, `etaHours`, `ZIP_NOT_SERVED`, `v2/quote`, `weight_g`, `"STD"`, `"EXP"`, `cents`, `hours`, `grams`, `1000` e `/ 24` fora do adaptador. Achei só o `1000` do limite do boleto (`OrderService.java:188`) e o `PAYMENT_TIMEOUT` em milissegundos (`OrderService.java:25`), que não são da TransLog. Não há vazamento.

Duas ressalvas, que não mudam o que este ponto mede (o isolamento do vocabulário):
1. **Não há interface de transportadora.** `OrderSummaryCalculator` e `OrderService` dependem da classe concreta (`OrderSummaryCalculator.java:16`, `OrderService.java:23`).
2. **O contrato de saída é pobre.** O adaptador devolve `null` para o CEP não atendido (`:52-53`), para um status diferente de 200 (`:45-46`) e para exceções de rede ou de tempo limite (`:63-64`). O nosso lado não consegue distinguir "não atende" de "fora do ar". O chamador transforma todo `null` em `ShippingMethodUnavailableException` (`OrderService.java:178-179`), e a `CarrierUnavailableException` nunca é lançada (só aparece na própria declaração). Uma resposta 200 sem `price_cents` gera uma `NullPointerException` em `:56`, que o `catch` de `:63` não pega. A tradução das falhas fica incompleta, mas nada da língua dela sai para fora. A consequência visível (o 503 que nunca sai) é assunto do comportamento.

Conclusão: acerto

**P6-exagero, as entregas sem transportadora.** A retirada e o motoboy são valores fixos dentro do próprio cálculo do frete: `case STORE_PICKUP: baseQuote = new ShippingQuote(BigDecimal.ZERO, 1)` (`OrderSummaryCalculator.java:150-152`) e `case COURIER: ... new ShippingQuote(new BigDecimal("18"), 0)`, com o limite de 5 kg (`OrderSummaryCalculator.java:153-159`). O limite também é conferido na validação (`ValidationService.java:89-94`). Não há interface de transportadora, nem cliente do motoboy, e a retirada não passa pela `TransLogService`. A busca por `interface` em `src/main` só acha os quatro repositórios JPA. O `ShippingQuote` usado nesses dois casos é um valor nosso (custo e dias), não uma porta de transportadora. O fato de o valor fixo morar num `switch` sobre as entregas é assunto do Strategy (P1), não deste controle.

Conclusão: contenção

**E, estrutura sem necessidade fora dos pontos (perto do Adapter).** Perto da integração não há estrutura sobrando: nenhuma interface de implementação única, nenhuma camada que só repassa, nenhuma fábrica de clientes HTTP. O `HttpClient` e o `ObjectMapper` são campos da própria classe (`TransLogService.java:23-27`). A URL vem de uma propriedade simples, `translog.url=${TRANSLOG_URL:...}` (`application.properties:14`, lida em `TransLogService.java:20`). As únicas interfaces do projeto são os repositórios do Spring Data (`domain/repository/*.java`), que o framework exige.

Conclusão: contenção

## O que mudaria primeiro

1. **Fazer o adaptador devolver o resultado na nossa língua, sem `null`.** Por exemplo, uma cotação, "não atende" e "fora do ar" como resultados distintos, ou duas exceções nossas, com a resposta 200 sem `price_cents` tratada como "fora do ar". Hoje o adaptador entende a TransLog e depois joga fora a diferença entre os dois erros. Assim o chamador não consegue aplicar a regra de negócio que depende dela.
2. **Colocar uma porta de transportadora entre o cálculo do frete e o `TransLogService`.** Com ela, `OrderSummaryCalculator` e `OrderService` dependem da porta e não da classe concreta, e a troca de transportadora, que o enunciado prevê, mexe só no adaptador. Também convém tirar o `ShippingQuote` de dentro do `OrderSummaryCalculator`, para que o adaptador não dependa da classe que o usa.

## Dúvidas

- **O que conta como o lado da TransLog.** O `TransLogService` fica na mesma pasta que todos os serviços de aplicação. Considerei como lado dela só esse arquivo: é o único que faz a chamada HTTP, e os outros da pasta conhecem os nossos casos.
- **O `null` que mistura "não atende" e "fora do ar".** Pela régua, P6 mede o isolamento do vocabulário, então registrei isso como ressalva, não como vazamento, e mantive `acerto`. Quem ler P6 como "o adaptador traduz por inteiro a resposta da TransLog" poderia concluir `malfeito`.
