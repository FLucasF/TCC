# Cenário 01

## Visão geral

O agente aplicou o Strategy aos cinco planos de tarifa. Criou a interface `RatePlanPolicy`, com três métodos: validar, calcular o preço e calcular a devolução. Cada plano tem a sua classe, os dois que já existiam inclusive (`StandardPlan` e `NonRefundablePlan`). A escolha da classe acontece num ponto só, um `EnumMap` em `RatePlans`. Os dois `switch` da base saíram: o do desconto em `PriceCalculator.calculate` e o da devolução em `ReservationService.cancel`. Agora os dois lugares pedem ao plano, sem citar plano nenhum. Todas as regras pedidas conferem pela API, com os três exemplos do financeiro incluídos. O normal e o não reembolsável respondem byte a byte igual à base. Não achei problema sério. Os detalhes menores estão em "Dúvidas".

## Como foi implementado, e por que o julgamento

**O projeto.** É um serviço Javalin, montado à mão em `Hotel.java`. Os pacotes são:

- `hotel.rooms`: os quartos, fixos em memória.
- `hotel.reservations`: o domínio. Tem `ReservationService` (o ciclo de vida da reserva), `PriceCalculator`, `ReservationStore` (memória), `Reservation`, `NewReservation` e `Price`.
- `hotel.web`: `HttpApi`, com as rotas, a tradução do JSON para `NewReservation` e o mapeamento de `ErrorCode` para status HTTP.
- `hotel.documents`: o comprovante.
- `hotel.messages`: os e-mails guardados.
- `hotel.reports`: os relatórios CSV de ocupação e receita.

O caminho de um `POST /reservations` é este:

1. `HttpApi` monta o `NewReservation`, que agora leva `companyCode` (`final/src/main/java/hotel/web/HttpApi.java:128-129`).
2. `ReservationService.create` valida as datas, o quarto, a capacidade e a disponibilidade, e chama `priceCalculator.calculate(room, request)` (`final/src/main/java/hotel/reservations/ReservationService.java:51`).
3. A reserva vai para o `ReservationStore`.

A montagem cria um `RatePlans` só e o entrega ao `PriceCalculator` e ao `ReservationService` (`final/src/main/java/hotel/Hotel.java:24-26`). O agente fez três ajustes nos testes que já existiam: o construtor novo e o `companyCode` nulo. Também escreveu `NewRatePlansTest`, com quatro testes que cobrem os exemplos do pedido e as recusas.

**A parte do pattern.**

- O contrato é `RatePlanPolicy`, com `validate`, `price` e `refund` (`final/src/main/java/hotel/reservations/RatePlanPolicy.java:7-15`).
- Há uma implementação por plano: `StandardPlan`, `NonRefundablePlan`, `CorporatePlan`, `WeekendPackagePlan` e `LongStayPlan`.
- A seleção é um `EnumMap` preenchido no construtor de `RatePlans` (`final/src/main/java/hotel/reservations/RatePlans.java:12-16`) e consultado por `policyFor` (`RatePlans.java:19-21`).
- O `PriceCalculator` só calcula as diárias com a temporada, devolvidas no record `NightlyCharges` (`PriceCalculator.java:31-39`). Depois pede ao plano a validação e o preço (`PriceCalculator.java:25-28`).
- O cancelamento pede a devolução ao plano (`ReservationService.java:88`).

Cada regra mora na classe do seu plano:

- **Convênio.** A tabela de empresas e a recusa ficam em `CorporatePlan.java:10-21`, e o desconto em `CorporatePlan.java:25`.
- **Pacote de fim de semana.** A validação "sexta a domingo" está em `WeekendPackagePlan.java:19-26`. O preço fechado por tipo de quarto, que ignora as diárias, está em `WeekendPackagePlan.java:12-15,29-32`. A metade na devolução está em `WeekendPackagePlan.java:35-36`.
- **Estadia longa.** O mínimo de 7 noites está em `LongStayPlan.java:14-18`, e os 10% em `LongStayPlan.java:21-22`.
- **Não reembolsável.** Os 15% e a devolução zero estão em `NonRefundablePlan.java:8,15-21`.
- **Normal.** Desconto zero e devolução integral, em `StandardPlan.java:13-19`.

O cálculo comum de percentual sobre as diárias ficou num helper, `PercentOff.of` (`PercentOff.java:10-13`). Ele recebe a taxa e não sabe qual plano o chama.

**O julgamento.** As instruções do cenário pedem os cinco planos, os antigos inclusive, cada um com preço, validação e devolução num lugar só seu, e pedem que o cálculo e o cancelamento perguntem ao plano sem nomeá-lo. É exatamente o que está no código:

- Uma busca por `switch`/`case` e por `RatePlan.` no código de produção acha só o `switch` de `ErrorCode` para HTTP (`HttpApi.java:101-105`, que já existia e não tem relação com os planos) e o mapa de seleção.
- Não há `if (ratePlan == …)` em lugar nenhum.
- A validação de cada plano não ficou no serviço.
- Nenhuma implementação devolve o cálculo a um `switch`.

Por isso dei **acerto**.

No exagero colateral, pesei três peças:

- `RatePlans` é o ponto de seleção do próprio pattern.
- `NightlyCharges` é um record pequeno que leva as diárias calculadas até a estratégia.
- `PercentOff` evita repetir a conta do desconto em quatro planos.

Nenhuma delas é uma camada que só repassa, nem uma interface sem razão. Fora do pattern, a mudança se limitou ao campo `companyCode`. Conclusão: contenção.

No comportamento, tudo o que testei bate com o pedido e com a base. Os detalhes estão no item abaixo.

## Item a item

**O pattern pedido.** O agente criou a interface `RatePlanPolicy` (`final/src/main/java/hotel/reservations/RatePlanPolicy.java:7-15`) e uma implementação por plano, os cinco: `StandardPlan.java:6`, `NonRefundablePlan.java:6`, `CorporatePlan.java:8`, `WeekendPackagePlan.java:10` e `LongStayPlan.java:8`. A seleção é um `EnumMap` único (`RatePlans.java:9-21`).

Os `switch` da base sumiram:

- O do desconto ficava em `base/src/main/java/hotel/reservations/PriceCalculator.java:26-29`. Hoje o cálculo virou `plan.validate` seguido de `plan.price` (`final/src/main/java/hotel/reservations/PriceCalculator.java:26-28`).
- O da devolução ficava em `base/src/main/java/hotel/reservations/ReservationService.java:86-89`. Hoje é `ratePlans.policyFor(...).refund(...)` (`final/src/main/java/hotel/reservations/ReservationService.java:88`).

As validações próprias de cada plano ficam nas classes dos planos:

- o código da empresa: `CorporatePlan.java:16-21`;
- sexta a domingo: `WeekendPackagePlan.java:19-26`;
- 7 noites: `LongStayPlan.java:14-18`.

Nada no serviço ou no cálculo pergunta qual é o plano concreto. Para criar um plano novo, basta uma classe nova, uma constante no enum e uma linha no mapa. É o Strategy que o texto pedia.
Conclusão: acerto

**O exagero colateral.** Não há. `RatePlans` é o ponto de seleção do pattern. `NightlyCharges` (`NightlyCharges.java:6`) é um record de dois campos que entrega as diárias à estratégia. `PercentOff` (`PercentOff.java:10-13`) é um helper estático usado por quatro planos. Não apareceu fábrica, builder, interface sem substituição real nem camada que só repassa.
Conclusão: contenção

**O comportamento.** Subi uma cópia da `final/` (`target/hotel.jar`, porta 7070) e chamei a API com `curl`.

Convênio:

| Teste | Esperado | Veio |
|---|---|---|
| Tecnosul, deluxe 201, 10 a 12/03/2027 | 779,80 / 93,58 / 686,22 | 779,80 / 93,58 / 686,22 |
| Agrovale, mesmo período | desconto 18%, 140,36 | 140,36 |
| Farmavida, suíte | desconto 25% sobre 1.159,80 = 289,95 | 289,95 |
| Sem código, com `ACME` e com `tecnosul` minúsculo | recusa | 400 `INVALID_REQUEST` |

Pacote de fim de semana:

| Teste | Esperado | Veio |
|---|---|---|
| Deluxe, sexta 12 a domingo 14/03/2027 | 820,00, desconto 0 | 820,00, desconto 0 |
| Standard em janeiro (temporada) | 520,00 | 520,00 |
| Suíte em julho (temporada) | 1.200,00 | 1.200,00 |
| Sábado a segunda, sexta a sábado, sexta a segunda | recusa | 400 `INVALID_REQUEST` |
| Com `companyCode` enviado | código ignorado | aceito, 520,00 |
| Deluxe pago e cancelado | devolve 410,00 | 410,00 |
| Standard pago e cancelado | devolve 260,00 | 260,00 |
| Pendente cancelado | devolve 0,00 | 0,00 |

Estadia longa:

| Teste | Esperado | Veio |
|---|---|---|
| Standard, 01 a 08/03/2027 | 1.749,30 / 174,93 / 1.574,37 | 1.749,30 / 174,93 / 1.574,37 |
| 6 noites | recusa | 400 `INVALID_REQUEST` |
| 25/02 a 04/03, com 4 noites de temporada | subtotal 1.949,22, desconto 194,92 | 1.949,22 / 194,92 |

Devoluções de reservas pagas e canceladas:

| Plano | Esperado | Veio |
|---|---|---|
| Convênio | tudo (686,22) | 686,22 |
| Estadia longa | tudo (1.574,37) | 1.574,37 |
| Standard | tudo (935,76) | 935,76 |
| Não reembolsável | nada | 0,00 |

Mais duas checagens:

- Cancelar uma reserva com check-in feito continua dando 409 `OPERATION_NOT_ALLOWED`.
- Um STANDARD com `companyCode` enviado ignora o código.

Comparação com a base: rodei a mesma sequência na base (porta 7071) e na final. A sequência tinha STANDARD e NON_REFUNDABLE, inclusive o arredondamento half-even de 37,48 e a suíte em temporada (2.365,99), a recusa de saída antes da entrada, o confirmar e cancelar, os relatórios de receita e de ocupação, o comprovante e o e-mail. O `diff` das saídas deu idêntico.

`mvn verify` na cópia: BUILD SUCCESS, com 30 testes, 0 falhas e 0 erros. Os 4 testes novos de `NewRatePlansTest` passam. No fim, derrubei os dois serviços.
Conclusão: segue

## Dúvidas

Nenhuma afeta a classe nem as conclusões. Ficam três observações menores:

- A validação do plano roda dentro de `PriceCalculator.calculate`, depois da checagem de disponibilidade (`ReservationService.java:44-51`). Assim, um pacote pedido numa data inválida para um quarto ocupado recebe 409 `ROOM_UNAVAILABLE` em vez de 400. O pedido não define essa ordem.
- O `companyCode` não é guardado na `Reservation` nem aparece na resposta. O pedido não exigia isso.
- O README da `final/` não foi atualizado com o campo novo nem com os planos novos.
