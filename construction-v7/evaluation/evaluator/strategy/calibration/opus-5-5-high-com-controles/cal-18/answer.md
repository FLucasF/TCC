# Cenário 02

## Visão geral

O agente tratou a semana da Páscoa como o enunciado pede: como dado. As duas semanas ficaram numa lista fixa de períodos (`EASTER_WEEKS`) e entraram como mais uma condição na conferência de "é alta temporada". O acréscimo de 20% continua numa constante só, e a frase sobre "um acréscimo diferente para cada temporada" não gerou estrutura nenhuma. O resto do sistema também é enxuto: classes concretas, sem interfaces, fábricas ou camadas que só repassam. Todas as regras que testei pela API conferem, e os 27 testes do próprio agente passam. Não achei problema sério. O único deslize é de documentação: o README não cita a Páscoa entre as datas da temporada.

## Item a item

**O enunciado (controle negativo).** As semanas da Páscoa de 2027 (21/03 a 28/03) e de 2028 (09/04 a 16/04) estão numa `List<DateRange>` imutável (`src/main/java/hotel/reservations/PriceCalculator.java:21-23`). O `DateRange` é um `record` privado com um `contains`, nada mais do que um par de datas (`src/main/java/hotel/reservations/PriceCalculator.java:25-29`). A Páscoa entra como mais um `||` no `isHighSeason`, ao lado das condições de dezembro, janeiro, fevereiro e julho (`src/main/java/hotel/reservations/PriceCalculator.java:54-61`, a da Páscoa na linha 60). Continua existindo um único acréscimo, `HIGH_SEASON_SURCHARGE = 0.20` (`src/main/java/hotel/reservations/PriceCalculator.java:17`), aplicado num só lugar (`src/main/java/hotel/reservations/PriceCalculator.java:47-49`). Não há interface de temporada, nem classe por temporada, nem acréscimo parametrizado por período. É a solução simples que o controle negativo pede.
Conclusão: contenção

**O exagero colateral.** Não há. Todas as classes são concretas e cada uma tem um papel próprio: o `PriceCalculator`, o `ReservationService` com as transições de estado, o `ReservationStore` em memória, os dois relatórios e o `HttpApi`. As ligações são feitas à mão num lugar só (`src/main/java/hotel/Hotel.java:19-28`), sem fábrica nem contêiner. Os quartos são uma lista fixa (`src/main/java/hotel/rooms/Rooms.java:10-15`). O `EmailSender` é fino, mas é concreto e cuida de uma coisa: grava o canal `EMAIL` na lista de mensagens enviadas (`src/main/java/hotel/messages/EmailSender.java:12-14`). Isso é razoável, já que o enunciado diz que "por enquanto" os e-mails não saem de verdade. Os dois planos de tarifa, que ficam fora da parte do fim, são resolvidos com `switch` simples, sem estrutura extra (`src/main/java/hotel/reservations/PriceCalculator.java:38-41` e `src/main/java/hotel/reservations/ReservationService.java:86-89`).
Conclusão: contenção

**O comportamento.** Testei a partir de uma cópia em `/tmp/av`. O build gerou o `target/hotel.jar` com a classe principal `hotel.Hotel` (`pom.xml`), e subi o serviço com `PORT=7070`. Chamei a API com `curl`:

- **O exemplo do enunciado:** STANDARD de 28/03/2027 a 30/03/2027 deu `subtotal` e `total` 549.78, como pedido.
- **Os limites da Páscoa em 2027:** a noite de 20/03 custa 249.90; a de 21/03, o domingo de Ramos, 299.88; a de 29/03, 249.90.
- **Os limites da Páscoa em 2028:** de 08/04 a 10/04 deu 549.78 (249.90 + 299.88), e de 16/04 a 18/04 também deu 549.78 (299.88 + 249.90).
- **Fora das duas semanas:** a Páscoa de 2026 (29/03) e 01/04/2029 ficam a 249.90, sem acréscimo.
- **O resto da temporada, como na parte comum:**
  - 10/07 a 12/07/2027 deu 599.76.
  - 14/12 a 16/12/2027 deu 549.78.
  - 28/02 a 02/03/2028, com o 29/02 do ano bissexto, deu 849.66.
  - Na suíte, 30/06 a 02/07/2027 deu 1275.78.
  - No deluxe, 31/07 a 02/08/2027 deu 857.78.
- **Não reembolsável na Páscoa:** deluxe de 27/03 a 29/03/2027 deu `subtotal` 935.76, `discount` 140.36 e `total` 795.40.
- **Confirmação e comprovante:** o e-mail saiu com o assunto e o texto certos, a linha "Desconto: R$ 140,36" e o dinheiro em R$ 1.234,56. A segunda via de uma reserva pendente foi recusada com 409 `OPERATION_NOT_ALLOWED`.
- **Cancelamentos:**
  - Não reembolsável já pago: `refund` 0.00.
  - STANDARD já pago: `refund` 549.78.
  - Reserva pendente: `refund` 0.00.
- **Erros:**
  - Quarto já reservado: 409 `ROOM_UNAVAILABLE`.
  - Hóspedes demais: 422 `CAPACITY_EXCEEDED`.
  - Quarto 999: 404 `ROOM_NOT_FOUND`.
  - Reserva 999: 404 `RESERVATION_NOT_FOUND`.
  - Corpo que não é JSON, saída igual à entrada, 0 hóspedes, campo faltando ou mês `2027-3`: 400 `INVALID_REQUEST`.
- **Check-out:** devolve o comprovante.
- **Relatórios:** chegam em `text/csv; charset=utf-8`. A ocupação de 2027-07-10 dá `total,1/5`. O financeiro de 2027-03 conta a reserva não reembolsável cancelada (795.40).

O `mvn -q verify` da cópia passou com 27 testes, sem falhas nem erros: 11 em `ReservationServiceTest`, 7 em `PriceCalculatorTest` (um deles cobre os limites da Páscoa), 5 em `ReportsTest` e 4 em `HttpApiTest`. No fim, derrubei o serviço.
Conclusão: segue

## Dúvidas

Nenhuma que mude as conclusões. Duas observações menores:

- O README ficou desatualizado: a descrição do preço lista só "de 15 de dezembro ao fim de fevereiro, e o mês de julho inteiro" (`README.md`, seção "Como funciona") e não cita a Páscoa. O código e os testes estão certos.
- O `EmailSender` (`src/main/java/hotel/messages/EmailSender.java:4-15`) quase só repassa para `SentMessages`. Não contei como exagero porque é uma classe concreta, sem interface, e é a ela que cabe o canal `EMAIL`.
