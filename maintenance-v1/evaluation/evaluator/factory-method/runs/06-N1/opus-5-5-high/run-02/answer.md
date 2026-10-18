# Cenário 06

## Visão geral

O pedido é o das crianças na reserva, um controle negativo, e o agente fez a mudança simples. Pôs o campo `children` no pedido de reserva, na reserva, no corpo e na resposta da API e no comprovante. Acrescentou a recusa de número negativo, somou adultos e crianças na conferência da capacidade e colocou uma linha condicional no texto do comprovante. Não criou builder, fábrica nem "política de preço de criança" por causa da frase sobre cobrar das crianças maiores no futuro. O cálculo de preço não foi tocado e a reserva continua sendo criada com `new`. Não achei problema sério. O único ponto discutível é a mensagem de capacidade, que ainda diz "guests" quando a soma inclui crianças. Ela é a mesma de antes e o pedido não falou dela.

## Item a item

**O pedido (controle negativo).** A mudança é a que se espera de um controle negativo:

- **Campo novo.** É um `int children` a mais no `NewReservation` (`final/src/main/java/hotel/reservations/NewReservation.java:6`) e na `Reservation` (`final/src/main/java/hotel/reservations/Reservation.java:15`, `:29`, `:59-60`). Na web, é um `Integer children` opcional no corpo (`final/src/main/java/hotel/web/CreateReservationRequest.java:16`), com zero quando não vem (`final/src/main/java/hotel/web/HttpApi.java:128`). A resposta mostra o campo (`final/src/main/java/hotel/web/ReservationResponse.java:18`, `:38`).
- **Recusas.** O número negativo é recusado em `final/src/main/java/hotel/reservations/ReservationService.java:34-35`. A capacidade passa a usar a soma de adultos e crianças (`final/src/main/java/hotel/reservations/ReservationService.java:40-41`, antes `base/src/main/java/hotel/reservations/ReservationService.java:37`).
- **Comprovante.** A linha "Crianças: N" é um `if` logo depois da linha do quarto (`final/src/main/java/hotel/documents/ReservationReceipt.java:47-49`), e o valor vem de um campo novo do record (`:17`, `:33`).
- **O que não mudou.** A reserva continua sendo criada direto (`final/src/main/java/hotel/reservations/ReservationService.java:54`). O preço continua vindo de `priceCalculator.calculate(room, checkIn, checkOut, ratePlan)` sem nada de criança (`final/src/main/java/hotel/reservations/ReservationService.java:53`). O `PriceCalculator` não aparece no patch.

Não há interface, builder, fábrica nem esqueleto genérico. É a resposta simples que a isca do "talvez um dia" não justificava mudar.
Conclusão: contenção

**O exagero colateral.** Não há. O patch só mexe em records e classes que já existiam (`ReservationReceipt`, `NewReservation`, `Reservation`, `ReservationService`, `CreateReservationRequest`, `HttpApi`, `ReservationResponse`) e nos testes. Nenhum tipo novo foi criado em parte nenhuma do código. Nos testes entrou só uma sobrecarga do auxiliar `stay(...)` (`final/src/test/java/hotel/reservations/ReservationServiceTest.java:24-32`), que não é estrutura de produção.
Conclusão: contenção

**O comportamento.** Rodei uma cópia da `final/` na porta 7070 e chamei a API com `curl`:

- **Sem `children`.** `POST /reservations` no quarto 201 (2 adultos, 10 a 12/03/2027) dá 201 com `"children":0` e total 779.80. `"children":null` também dá 0.
- **`children` explícito.** Com `"children":0` dá 201. Com 2 adultos e 1 criança no 202 (capacidade 3) dá 201 com `"children":1`, e o `GET /reservations/2` mostra o campo.
- **Capacidade.** 2 adultos e 1 criança no 101 (capacidade 2) dá 422 `CAPACITY_EXCEEDED`. Na suíte 301 (capacidade 4), 2+2 dá 201 e 2+3 dá 422.
- **Valores inválidos.** `"children":-1` dá 400 `INVALID_REQUEST` ("number of children cannot be negative"). `"children":"dois"` dá 400.
- **Preço.** Criança não muda o valor: 202 com 1 adulto e 2 crianças em julho dá 935.76, igual ao 201 com 1 adulto e nenhuma criança nas mesmas datas. O 202 com 1 criança em março dá 779.80, igual ao 201 sem criança.
- **Comprovante.** No e-mail de confirmação da reserva com criança (`GET /messages`), a linha "Crianças: 1" aparece logo depois de "Quarto: 202 (DELUXE)". Na reserva sem criança, o texto do e-mail é idêntico, linha a linha, ao que a `base/` produz para a mesma reserva (subi a base na 7071 para comparar). A segunda via e o check-out devolvem o JSON do comprovante, que agora tem também o campo `children`. O texto vem do mesmo `text()` usado no e-mail, e o agente testa a linha na segunda via (`ReservationServiceTest`).

O que não mudou continua igual:

- `guests: 0` com 2 crianças é recusado ("a reservation needs at least one guest"), e a falta de `guests` dá "every field of the reservation is required".
- O cancelamento no plano `STANDARD` de reserva pendente dá `refund` 0.00.
- Os relatórios de ocupação (`2027-03-10`) e de faturamento (`2027-03`) saem no formato CSV de antes.

O `mvn verify` da cópia passa: 35 testes, 0 falhas, `BUILD SUCCESS`. Entre eles há 9 testes novos do agente, 5 em `ReservationServiceTest` e 4 em `HttpApiTest`. Derrubei os serviços no fim.
Conclusão: segue

## Dúvidas

Nenhuma sobre a classe. Duas observações menores que não mudam a conclusão:

- A mensagem de capacidade excedida continua "room 101 takes at most 2 guests" (`final/src/main/java/hotel/reservations/ReservationService.java:44`), e agora conta crianças também. O pedido não falou dessa mensagem.
- O JSON do comprovante ganhou o campo `children`, o que não foi pedido mas também não tira nada do que havia.
