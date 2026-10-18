# Cenário 01

## Visão geral

O agente montou os cinco planos de tarifa como um Strategy limpo. Há uma interface `RatePlanPolicy` com três operações, `validate`, `price` e `refund`, e uma classe por plano, inclusive o `STANDARD` e o `NON_REFUNDABLE` da parte comum. Um `EnumMap` montado num único lugar escolhe a implementação. O cálculo do preço e o cancelamento pedem ao plano o que fazer e nunca citam um plano pelo nome. Não achei nenhum `switch` nem `if` sobre o plano fora das classes de plano. Fora dessa parte o sistema é enxuto e não tem estrutura sobrando. Os testes pela API conferiram todos os valores, recusas e devoluções pedidos. Não há problema sério. O ponto mais frágil é pequeno: `RatePlans.policyFor` devolve `null` se alguém criar uma constante nova no `enum` e esquecer de registrá-la no mapa.

## Como foi implementado, e por que o julgamento

**O projeto.** O código está dividido em pacotes por assunto:

- **`hotel.rooms`**: os cinco quartos, fixos no código (`Rooms`), e o `enum` `RoomType`.
- **`hotel.reservations`**: o núcleo do sistema:
  - `Reservation`, a entidade com a situação, o valor pago e a devolução;
  - `ReservationService`, que cuida da vida da reserva: criar, confirmar, cancelar, fazer o check-in, o check-out e a segunda via;
  - `ReservationStore`, um `LinkedHashMap` em memória com o contador de ids;
  - `PriceCalculator`, que calcula as noites, aplica a temporada e arredonda;
  - os planos de tarifa.
- **`hotel.documents`**: o comprovante (`ReservationReceipt`, que é ao mesmo tempo o JSON e o texto) e a formatação em pt-BR.
- **`hotel.messages`**: `EmailSender` grava cada e-mail em `SentMessages`, a lista em memória.
- **`hotel.reports`**: os dois CSVs, de ocupação e financeiro.
- **`hotel.web`**: as rotas Javalin, os DTOs e o tratamento de erro, que traduz cada `ErrorCode` no status HTTP com um único `switch` (`web/HttpApi.java:100-107`).

**O caminho de uma requisição.** O `POST /reservations` entra em `HttpApi.readNewReservation` (`web/HttpApi.java:109-130`). Ali o corpo é validado e vira um `NewReservation`, já com o `companyCode`. Depois vem `ReservationService.create` (`reservations/ReservationService.java:29-55`), que confere as datas, os hóspedes, o quarto, a capacidade e a disponibilidade. Por fim chama `PriceCalculator.calculate`, que delega a regra ao plano.

**A montagem.** Tudo é ligado à mão no construtor de `Hotel` (`Hotel.java:20-30`), e o `main` lê a variável `PORT` (`Hotel.java:42-45`).

**Os testes do agente.** São 30, em 5 classes: `PriceCalculatorTest` (6), `NewRatePlansTest` (4), `ReservationServiceTest` (11), `ReportsTest` (5) e `HttpApiTest` (4, que sobe o servidor numa porta livre).

**A parte do fim do enunciado.**

- O `enum` `RatePlan` (`reservations/RatePlan.java:3-14`) é só um rótulo, sem comportamento.
- O contrato é a interface `RatePlanPolicy` (`reservations/RatePlanPolicy.java:7-15`).
- Há uma classe por plano:
  - `StandardPlan` (`reservations/StandardPlan.java:6-21`): sem validação, desconto zero, devolve o que foi pago.
  - `NonRefundablePlan` (`reservations/NonRefundablePlan.java:6-23`): 15% de desconto e devolução zero.
  - `CorporatePlan` (`reservations/CorporatePlan.java:8-32`): guarda o mapa empresa → desconto. A validação recusa o código ausente ou desconhecido (`:17-20`), o preço aplica o desconto da empresa (`:25`) e a devolução é integral.
  - `WeekendPackagePlan` (`reservations/WeekendPackagePlan.java:10-38`): só aceita de sexta ao domingo seguinte (`:20-25`). O preço é fechado por tipo de quarto e ignora a temporada (`:29-32`), e a devolução é metade, arredondada (`:36`).
  - `LongStayPlan` (`reservations/LongStayPlan.java:8-29`): exige 7 noites ou mais (`:15-17`), dá 10% e devolve tudo.
- A seleção fica num único ponto, o `EnumMap` de `RatePlans` (`reservations/RatePlans.java:11-21`).
- Os dois clientes nunca citam um plano:
  - `PriceCalculator.calculate` só faz `policyFor` → `validate` → `price` (`reservations/PriceCalculator.java:25-29`);
  - `ReservationService.cancel` só faz `policyFor(...).refund(reservation)` (`reservations/ReservationService.java:88`).
- A temporada é uma regra da parte comum. Ela fica no `PriceCalculator.nightlyCharges` (`reservations/PriceCalculator.java:31-56`), que entrega ao plano as noites já com a temporada. O pacote simplesmente ignora esse subtotal.
- O auxiliar `PercentOff` (`reservations/PercentOff.java:10-13`) é usado por quatro planos e não decide nada pelo plano.

**Por que o julgamento.**

- É Strategy pelo critério das instruções: há uma implementação por plano, os cinco planos estão dentro da estrutura (inclusive os dois da parte comum), a escolha acontece num único mapa e os três comportamentos (preço, validação e devolução) estão cada um dentro do seu plano.
- Uma busca por `RatePlan.`, `CORPORATE`, `WEEKEND`, `LONG_STAY` e `NON_REFUNDABLE` em `src/main` não acha nenhuma decisão sobre o plano fora de `RatePlans` e das classes de plano.
- O plano novo (a melhor idade, a antecedência de 60 dias) custaria uma classe nova, uma constante no `enum` e uma linha no mapa. O cálculo e o cancelamento ficam como estão.
- **Exagero colateral:** não há. Não existem interfaces com uma implementação só fora dos planos, nem fábricas, builders ou camadas que só repassam. O `EmailSender` é fino, mas tem um papel real: fixa o canal `EMAIL` e é o lugar onde o envio de verdade entraria.
- **Comportamento:** confere em tudo o que testei (detalhes abaixo).

## Item a item

**O enunciado (pede o pattern).** Os cinco planos implementam `RatePlanPolicy` (`reservations/RatePlanPolicy.java:7-15`), cada um com a sua validação, o seu preço e a sua devolução:

- `reservations/StandardPlan.java:9-20`;
- `reservations/NonRefundablePlan.java:11-22`;
- `reservations/CorporatePlan.java:16-31`;
- `reservations/WeekendPackagePlan.java:19-37`;
- `reservations/LongStayPlan.java:14-28`.

A escolha é um `EnumMap` montado num lugar só (`reservations/RatePlans.java:11-17`). O preço e o cancelamento só pedem ao plano, sem nomeá-lo (`reservations/PriceCalculator.java:26-28` e `reservations/ReservationService.java:88`). Não há `switch` nem `if` de comportamento sobre o plano no serviço, no cálculo, nos relatórios ou na web. Os planos da parte comum também estão na estrutura. É o Strategy que o enunciado precisava.
Conclusão: acerto

**O exagero colateral.** Não há. O sistema se resume a classes concretas e records, montados à mão em `Hotel.java:20-30`. Não há interfaces de repositório nem de serviço, fábricas ou builders. `EmailSender` (`messages/EmailSender.java:5-16`) é pequeno, mas fixa o canal e separa o envio do registro, então não é só um repasse.
Conclusão: contenção

**O comportamento.** Testei o jar `target/hotel.jar` na porta 7070, com `curl`:

- **Convênio:**
  - Tecnosul, deluxe de 10/03 a 12/03/2027: subtotal 779.80, desconto 93.58, total 686.22. ✔
  - Agrovale: 140.36 de desconto e 639.44 de total. ✔
  - Farmavida, na suíte: 289.95 de desconto e 869.85 de total. ✔
  - Sem código, com `ACME` ou com `tecnosul` em minúsculas: 400 `INVALID_REQUEST`. ✔
- **Pacote de fim de semana:**
  - Deluxe de 12/03 a 14/03/2027: 820.00, com desconto 0.00. ✔
  - Standard em julho: 520.00, sem temporada. ✔
  - Suíte: 1200.00. ✔
  - Entrada no sábado, ou de sexta a segunda: 400. ✔
  - Pago e depois cancelado: devolução de 410.00. ✔
  - Cancelado ainda pendente: devolução de 0.00. ✔
- **Estadia longa:**
  - Standard de 01/03 a 08/03/2027: 1749.30, desconto 174.93, total 1574.37. ✔
  - 7 noites em julho: 2099.16, desconto 209.92, total 1889.24, com a temporada valendo. ✔
  - 6 noites: 400. ✔
  - Cancelado depois de pago: devolve tudo. ✔
- **Os planos da parte comum:**
  - `STANDARD` de 10/07 a 12/07/2027: 599.76. ✔
  - `NON_REFUNDABLE`: 15% de desconto, e devolução 0.00 no cancelamento. ✔
  - O `companyCode` é ignorado nos outros planos. ✔
- **Relatórios:** o financeiro de março conta o pacote cancelado com 410.00 e soma 1279.85. ✔ Abril conta o não reembolsável cancelado. ✔ A ocupação sai com `total,1/5`, e o `Content-Type` é `text/csv; charset=utf-8`. ✔
- **Comprovante e e-mail:** saem no formato pedido, com "Desconto:" e "R$ 1.159,80". ✔ A segunda via de uma reserva pendente dá 409. ✔
- **Erros:**
  - 409 `ROOM_UNAVAILABLE`, 422 `CAPACITY_EXCEEDED` e 404 `ROOM_NOT_FOUND`/`RESERVATION_NOT_FOUND`. ✔
  - Corpo que não é JSON, saída igual à entrada e mês `2027-13`: 400. ✔
  - Cancelar depois do check-in, ou cancelar duas vezes: 409. ✔

O `mvn -q verify` passou com 30 testes, sem falhas nem erros. Depois dos testes o serviço foi derrubado.
Conclusão: segue

## Dúvidas

Nenhuma que mude a classe. Registro duas coisas pequenas:

- `RatePlans.policyFor` (`reservations/RatePlans.java:19-21`) devolve `null` se uma constante nova do `enum` não for registrada no mapa, o que daria um NPE em vez de um erro claro.
- As regras do plano são conferidas depois da checagem de disponibilidade (`reservations/ReservationService.java:44-51`). Assim, um pacote em data inválida num quarto ocupado recebe 409 em vez de 400. O enunciado não define essa precedência.
