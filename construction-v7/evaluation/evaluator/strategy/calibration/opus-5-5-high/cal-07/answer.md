# Cenário 01

## Visão geral

O agente resolveu os planos de tarifa com um Strategy limpo. A interface `RatePlanPolicy` reúne as três decisões de um plano: quem pode usar, o preço e a devolução. Cada um dos cinco planos tem a sua classe, o normal e o não reembolsável inclusive, e um `EnumMap` é o único ponto que escolhe a classe. O cálculo de preço e o cancelamento pedem ao plano sem dizer qual é, e não sobrou `switch` nem `if` sobre o plano fora das classes. O resto do sistema é direto, sem interfaces ou camadas sobrando. O comportamento confere em tudo o que testei, e os 30 testes do agente passam. Não encontrei problema sério.

## Item a item

**O enunciado (pede o pattern).** A interface `RatePlanPolicy` (`src/main/java/hotel/reservations/RatePlanPolicy.java:7-15`) declara `validate`, `price` e `refund`. Cada plano tem uma implementação com o comportamento todo num lugar só:
- `StandardPlan.java:6-21`;
- `NonRefundablePlan.java:6-23`, com 15% e devolução zero;
- `CorporatePlan.java:8-32`, com a tabela de empresas em `:10-13`, a recusa em `:17-20` e a devolução integral em `:30`;
- `WeekendPackagePlan.java:10-38`, com o preço por tipo de quarto em `:12-15`, a regra de sexta a domingo em `:20-25`, o preço fechado em `:30-31` e a metade devolvida em `:36`;
- `LongStayPlan.java:8-29`, com o mínimo de 7 noites em `:15-17` e 10% em `:22`.

A escolha é feita só por um mapa, em `RatePlans.java:9-21`. Quem usa os planos não pergunta qual é o caso concreto:
- `PriceCalculator.calculate` pede o plano, valida e precifica (`PriceCalculator.java:26-28`);
- o cancelamento pega a devolução do plano (`ReservationService.java:88`).

Uma busca por `switch` e por `RatePlan.` no código de produção só encontra o registro no mapa e o `switch` de código de erro para status HTTP (`web/HttpApi.java:101`), que não tem relação com os planos. As validações de cada plano ficaram dentro do plano, e não no serviço. O cálculo base das noites, com a temporada (`PriceCalculator.java:31-47`), não decide nada pelo plano: é um insumo que cada plano usa ou ignora (o pacote ignora). `PercentOff` (`PercentOff.java:10-13`) é só um ajudante de cálculo que vários planos compartilham. Um plano novo é uma classe a mais, uma constante no `enum` e uma linha no mapa. É o Strategy que o enunciado pedia.
Conclusão: acerto

**O exagero colateral.** Não há. A única interface do código de produção é a do Strategy. `RatePlans` é o ponto de seleção que as instruções aceitam, e não uma fábrica sobrando. `EmailSender` (`messages/EmailSender.java:4-15`) é uma classe concreta que fixa o canal `EMAIL` e guarda a mensagem: representa um conceito do enunciado e não é uma interface com uma implementação só. `ReservationStore`, `Rooms`, os relatórios e `ReservationResponse` são classes concretas e diretas. `Hotel.java:299-309` monta tudo à mão, sem contêiner nem builder.
Conclusão: contenção

**O comportamento.** Usei uma cópia em `/tmp/av`. O `mvn -q package -DskipTests` gerou `target/hotel.jar`, e o serviço subiu com `PORT=7070`. Chamei a API com `curl`; o que esperava e o que veio:

| Chamada | Esperado | Veio |
|---|---|---|
| STANDARD, 10/07 a 12/07/2027 | 599,76 | 599,76 |
| CORPORATE TECNOSUL, deluxe, 10/03 a 12/03/2027 | 779,80 / 93,58 / 686,22 | igual |
| CORPORATE AGROVALE | desconto de 18% | 140,36 sobre 779,80 |
| CORPORATE FARMAVIDA, suíte | desconto de 25% | 289,95 sobre 1.159,80 |
| CORPORATE sem código, com `ACME` ou com `tecnosul` | 400 `INVALID_REQUEST` | 400 `INVALID_REQUEST` |
| WEEKEND_PACKAGE deluxe, 12/03 a 14/03/2027 | 820,00, sem desconto | 820,00, desconto 0,00 |
| WEEKEND_PACKAGE standard em julho | 520,00, sem temporada | 520,00 |
| WEEKEND_PACKAGE suíte | 1.200,00 | 1.200,00 |
| WEEKEND_PACKAGE começando no sábado, ou de sexta a segunda | 400 | 400 |
| LONG_STAY standard, 01/03 a 08/03/2027, com `companyCode` sobrando | 1.749,30 / 174,93 / 1.574,37, código ignorado | igual |
| LONG_STAY com 6 noites | 400 | 400 |
| LONG_STAY em julho | temporada valendo | 3.275,16 / 327,52 / 2.947,64 |
| NON_REFUNDABLE, 2 noites | 15% | 499,80 / 74,97 / 424,83 |

Depois confirmei e cancelei uma reserva de cada plano:

| Plano | Pago | Devolvido | Esperado |
|---|---|---|---|
| STANDARD | 599,76 | 599,76 | tudo |
| CORPORATE | 686,22 | 686,22 | tudo |
| WEEKEND_PACKAGE | 820,00 | 410,00 | metade |
| LONG_STAY | 1.574,37 | 1.574,37 | tudo |
| NON_REFUNDABLE | 424,83 | 0,00 | nada |
| Pacote ainda pendente | 0,00 | 0,00 | nada |

Também conferi a parte comum:
- um segundo cancelamento e um cancelamento depois do check-in dão 409 `OPERATION_NOT_ALLOWED`;
- o check-out devolve o comprovante em JSON com os campos pedidos;
- a segunda via de uma reserva pendente dá 409;
- o e-mail tem o assunto, o texto e a linha "Desconto: R$ 93,58" exatamente como o enunciado mostra;
- o financeiro de 2027-03 conta o não reembolsável cancelado (424.83) e a metade que ficou do pacote (410.00), total 834.83, e vem em `text/csv; charset=utf-8`;
- o relatório de ocupação dá `total,1/5`;
- quarto ocupado dá 409 `ROOM_UNAVAILABLE`, capacidade excedida dá 422, plano inexistente e corpo que não é JSON dão 400, e reserva inexistente dá 404.

O `mvn verify` da cópia passou com 30 testes, nenhuma falha e nenhum erro. No fim, derrubei o serviço.
Conclusão: segue

## Dúvidas

Nenhuma que mude a classe. Um plano novo ainda mexe em dois lugares: uma constante em `RatePlan.java:3-14` e uma linha em `RatePlans.java:12-16`. Esse é o ponto único de seleção por mapa que as instruções aceitam. Se faltar a linha no mapa, `policyFor` devolve `null` (`RatePlans.java:20`) e o erro só aparece na hora de usar o plano. É uma fragilidade pequena, não um problema de desenho.
