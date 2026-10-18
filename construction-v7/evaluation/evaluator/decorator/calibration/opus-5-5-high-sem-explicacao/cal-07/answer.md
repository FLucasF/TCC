# Cenário 01

## Visão geral

O agente montou um sistema simples e enxuto, com pacotes por assunto, sem interfaces nem camadas sobrando, e a parte comum do enunciado funciona. A parte do fim, porém, ficou sem estrutura nenhuma. As três regras de envio (bloquear endereço inválido, desviar para a caixa de testes, mandar a cópia para o arquivo) são três `if` em sequência dentro do mesmo método `EmailSender.send`, e a combinação por modo vem de um `boolean testMode` guardado no próprio envio. Não existe uma interface de envio, nenhum embrulho e nenhuma montagem por modo. As regras novas que o enunciado já anuncia (madrugada, limite por dia, cada uma valendo sempre, só no teste ou só fora dele) teriam de virar mais `if` cruzados com esse `testMode`. O comportamento das três regras, nos dois modos, confere com o que foi pedido.

## Item a item

**O enunciado (pede o pattern).** Todo o envio está numa classe concreta e `final`, `src/main/java/hotel/messages/EmailSender.java:9`, que recebe o modo como campo, `src/main/java/hotel/messages/EmailSender.java:16` e `:22-25`. O método `send` faz tudo à mão, em sequência:
- o bloqueio por regex: `src/main/java/hotel/messages/EmailSender.java:28-30`;
- o `if (testMode)`, que desvia para `testes@…` com "[TESTE] " e "Destinatário original: ": `src/main/java/hotel/messages/EmailSender.java:31-34`;
- o envio real seguido da cópia para o arquivo: `src/main/java/hotel/messages/EmailSender.java:35-37`.

A cópia só fica de fora do modo de teste porque o `return` antecipado da linha 33 a pula. A combinação das regras por modo está, portanto, escrita dentro do envio, e não numa montagem. A montagem, `src/main/java/hotel/Hotel.java:25`, só passa o booleano lido de `HOTEL_TEST_MODE`. `ReservationService` depende da classe concreta, `src/main/java/hotel/reservations/ReservationService.java:17` e `:69`. Não há interface de envio nem embrulho, corrente ou lista de filtros. É o caso que as instruções descrevem como omissão: os três `if` (o endereço, o modo de teste, a cópia) dentro do envio.
Conclusão: omissão

**O exagero colateral.** Não há. Nenhuma interface tem uma implementação só, e não há fábricas, builders ou camadas que só repassam. Os serviços são classes concretas e `final`: `src/main/java/hotel/reservations/PriceCalculator.java:14`, `src/main/java/hotel/reservations/ReservationStore.java:10`, `src/main/java/hotel/rooms/Rooms.java:8`, `src/main/java/hotel/reports/OccupancyReport.java:13` e `src/main/java/hotel/reports/RevenueReport.java:15`. As peças são ligadas à mão em `src/main/java/hotel/Hotel.java:19-28`. `ReservationResponse` (`src/main/java/hotel/web/ReservationResponse.java:9`) e `ReservationReceipt` (`src/main/java/hotel/documents/ReservationReceipt.java:10`) são records que dão o formato do JSON, o que é necessário.
Conclusão: contenção

**O comportamento.** Testei uma cópia do projeto em `/tmp/av07`. O `mvn -q package -DskipTests` gerou `target/hotel.jar`. Subi o jar uma vez na porta 7070, sem a variável, e outra na 7071, com `HOTEL_TEST_MODE=true`. Em cada uma, criei e confirmei 7 reservas via `POST /reservations` e `POST /reservations/{id}/confirmation`, com os e-mails `carla@gmail.com`, `ana@gmail`, `ana.gmail.com`, `a@@b.com`, `@b.com`, `x@.com` e `x@b.`.

- **Modo normal.** `carla@gmail.com` gerou a mensagem 1, `SENT`, com o assunto "Reserva confirmada - Pousada Maré Alta" e o corpo exatamente como o do enunciado. Logo depois veio a mensagem 2, `SENT`, para `arquivo@pousadamarealta.com.br`, com o assunto "[Cópia] Reserva confirmada - Pousada Maré Alta" e o mesmo corpo (comparação de igualdade deu `true`). Os seis endereços inválidos apareceram como `BLOCKED`, com o endereço original e sem cópia.
- **Modo de teste.** A única mensagem `SENT` foi para `testes@pousadamarealta.com.br`, com o assunto "[TESTE] Reserva confirmada - …" e a primeira linha "Destinatário original: carla@gmail.com", sem cópia. Os inválidos continuaram `BLOCKED`, com o endereço original.
- **Campos das mensagens.** Cada uma tem `id`, `channel` (`EMAIL`), `to`, `subject`, `body` e `status`.
- **Parte comum, preços.** DELUXE de 10/03 a 12/03/2027 deu 779.80. STANDARD de 10/07 a 12/07/2027 deu 599.76, como no exemplo do enunciado. NON_REFUNDABLE no mesmo período deu 509.80.
- **Cancelamentos.** O NON_REFUNDABLE confirmado ficou com `refund` 0.00, e o STANDARD confirmado recebeu 599.76 de volta.
- **Recusas.** Noite já ocupada: 409 `ROOM_UNAVAILABLE`. Três hóspedes no 101: 422 `CAPACITY_EXCEEDED`. Corpo que não é JSON: 400 `INVALID_REQUEST`. Confirmar duas vezes: 409 `OPERATION_NOT_ALLOWED`.
- **Comprovante.** A segunda via e o check-out devolvem os campos pedidos.
- **Relatórios.** Os dois vêm em `text/csv; charset=utf-8`. O financeiro de 2027-07 traz só a reserva não reembolsável cancelada, com 509.80, e `total,509.80`. A ocupação de 2027-07-10 dá `total,0/5`, o que está certo, porque as duas reservas de julho tinham sido canceladas.

O `mvn -q verify` da cópia passou: 26 testes, 0 falhas e 0 erros (ReportsTest 5, HttpApiTest 4, PriceCalculatorTest 6, ReservationServiceTest 11). Os dois serviços foram derrubados no fim.
Conclusão: segue

## Dúvidas

Nenhuma.
