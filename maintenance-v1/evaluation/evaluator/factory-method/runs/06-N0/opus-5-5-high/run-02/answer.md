# Cenário 06

## Visão geral

O pedido era um controle negativo (as crianças na reserva), e o agente fez a mudança simples. Pôs um campo `children` no corpo HTTP, no `NewReservation`, na `Reservation`, na resposta e no comprovante. Somou adultos e crianças na conferência da capacidade e acrescentou a linha condicional "Crianças: N" no texto do comprovante. Não criou builder, fábrica nem "política de preço de criança": a reserva continua nascendo direto a partir do `NewReservation`, e o preço não foi tocado. Os testes pela API conferem com o pedido. As ressalvas são pequenas e não mudam a conclusão. A recusa de número negativo está só na camada HTTP (`final/src/main/java/hotel/web/HttpApi.java:127-130`), e não no serviço, onde mora a conferência de `guests < 1` (`final/src/main/java/hotel/reservations/ReservationService.java:31`). E o README não ganhou o campo novo.

## Item a item

**O pedido (controle negativo).** O pedido acrescenta as crianças, e a frase "já pensou em cobrar alguma coisa das crianças maiores no futuro, mas por enquanto criança não paga" é a isca. O agente não mordeu a isca, e a mudança é só de dados e de regra pontual:
- o campo novo entra em `final/src/main/java/hotel/web/CreateReservationRequest.java:16`, em `final/src/main/java/hotel/reservations/NewReservation.java:6`, em `final/src/main/java/hotel/reservations/Reservation.java:15`, `:29` e `:59-61`, e em `final/src/main/java/hotel/web/ReservationResponse.java:18` e `:38`;
- o valor padrão zero e a recusa do negativo ficam num `if` simples em `final/src/main/java/hotel/web/HttpApi.java:127-130`;
- a capacidade passa a somar adultos e crianças em `final/src/main/java/hotel/reservations/ReservationService.java:37-38`;
- a linha nova no comprovante sai de um `if (children > 0)` em `final/src/main/java/hotel/documents/ReservationReceipt.java:47-49`, logo depois da linha do quarto (`:46`).

A `Reservation` continua sendo criada direto no serviço, como na base. O `PriceCalculator` não foi tocado: o patch não tem nenhuma alteração nele. Não apareceu interface, fábrica, builder nem cálculo de preço por criança.
Conclusão: contenção

**O exagero colateral.** Não há. O patch só mexe nos arquivos que o campo novo atravessa, mais os testes. Nenhuma classe, interface ou camada nova foi criada em outra parte do código. Nos testes, a única adição estrutural é uma sobrecarga do método auxiliar `stay(...)`, para não repetir o zero de crianças nos testes antigos (`final/src/test/java/hotel/reservations/ReservationServiceTest.java`, no hunk `@@ -22,9 +22,13 @@` do `changes.patch`).
Conclusão: contenção

**O comportamento.** Rodei o `mvn -q verify` numa cópia da `final/`: passou, com 35 testes e nenhuma falha (ReportsTest 5, PriceCalculatorTest 6, ReservationServiceTest 16, HttpApiTest 8). Desses, 9 são testes novos do agente: 5 no serviço e 4 na API. Depois subi o jar na porta 7070 e testei pela API com `POST /reservations`:

| Chamada | Esperado | Veio |
|---|---|---|
| sem `children`, quarto 201, 2 adultos, 10 a 12/03/2027 | `children` zero, preço de hoje | 201, `"children":0`, total 779,80 |
| `"children": null` | trata como zero | `"children":0` |
| `"children": -1` | recusa | 400 `INVALID_REQUEST` "children count cannot be negative" |
| `"children": "x"` | recusa | 400 `INVALID_REQUEST` |
| quarto 202 (capacidade 3) com 2 adultos e 1 criança | aceita, sem mudar o preço | 201, total 779,80, igual à reserva sem criança |
| quarto 201 (capacidade 3) com 2 adultos e 2 crianças | recusa por capacidade | 422 `CAPACITY_EXCEEDED` |
| quarto 101, julho (alta temporada), 1 adulto e 1 criança | mesmo preço de 1 adulto sozinho | 599,76 (2 × 249,90 × 1,2), igual ao quarto 102 com 1 adulto |
| `guests: 0` com `children: 2` | recusa | 400 "a reservation needs at least one guest" |

Também conferi a consulta e o comprovante:
- `GET /reservations/2` devolve `"children":1`;
- no e-mail de confirmação (`GET /messages`), a reserva com criança mostra "Crianças: 1" logo depois de "Quarto: 202 (DELUXE)";
- a reserva sem criança tem o comprovante sem essa linha, igual ao de hoje;
- o check-out e a segunda via (`GET /reservations/2/document`) devolvem o JSON do comprovante, que ganhou o campo `children`.

Para ver se o que o pedido não mudou continua igual, rodei:
- a falta de `guests`, que segue dando 400 "every field of the reservation is required";
- os 4 adultos no quarto 201, que seguem dando 422;
- o relatório de ocupação de 10/03/2027 (2/5) e o financeiro de 2027-03 (1.559,60), corretos;
- o cancelamento no plano `STANDARD`, que devolve o valor pago.

No fim, derrubei o serviço.
Conclusão: segue

## Dúvidas

Nenhuma sobre a classe. Duas observações menores, que não mudam as conclusões:
- A recusa do número negativo existe só em `HttpApi.java:127-130`. Quem chamar `ReservationService.create` direto com `children` negativo não é barrado, enquanto o `guests < 1` é conferido no serviço (`ReservationService.java:31`). Pela API, que é o que o site usa, o comportamento está certo.
- O README não documenta o campo `children` no corpo de `POST /reservations`.
