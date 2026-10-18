# Cenário 06

## Visão geral

Este é o pedido das crianças na reserva, e o agente fez a mudança simples. Pôs o campo `children` no corpo da API, no pedido de reserva, na reserva, na resposta e no comprovante. Somou adultos e crianças na conferência da capacidade, recusa número negativo e escreve a linha "Crianças: N" só quando há criança. A reserva continua sendo criada direto, e não há builder, fábrica nem "política de preço de criança"; a isca do "talvez um dia cobrar" não foi mordida. O problema mais sério é pequeno e fica nos testes: o agente criou um helper de teste que aceita crianças, mas nenhum teste o usa. Os 26 testes são os mesmos da base, então nenhuma regra nova está coberta por teste.

## Item a item

**O pedido (controle negativo).** As mudanças são pontuais, em campos e numa conferência:
- O campo novo entra em `final/src/main/java/hotel/reservations/NewReservation.java:6`, `final/src/main/java/hotel/reservations/Reservation.java:14`, `:28` e `:59-60`, `final/src/main/java/hotel/web/CreateReservationRequest.java:16` e `final/src/main/java/hotel/web/ReservationResponse.java:18` e `:38`.
- O valor zero, quando o campo não vem, é decidido na tradução do corpo, em `final/src/main/java/hotel/web/HttpApi.java:127`. O pedido de reserva continua sendo criado com `new NewReservation(...)` em `final/src/main/java/hotel/web/HttpApi.java:129-130`.
- A recusa do número negativo está em `final/src/main/java/hotel/reservations/ReservationService.java:34-35`.
- A soma na capacidade está em `final/src/main/java/hotel/reservations/ReservationService.java:40-41`.
- A linha condicional do comprovante está em `final/src/main/java/hotel/documents/ReservationReceipt.java:50-51`, logo depois da linha de entrada e saída, com o campo em `:20` e `:36`.
- O `PriceCalculator` não foi tocado, porque criança não paga, e nada foi criado em torno do preço.

É exatamente o que as instruções chamam de contenção.
Conclusão: contenção

**O exagero colateral.** Não há. O patch só acrescenta campos, um `if` de validação, uma soma e uma linha de texto. Não aparece interface, fábrica, builder nem camada nova. Nos testes, a única coisa nova é a sobrecarga de `stay` com crianças (`final/src/test/java/hotel/reservations/ReservationServiceTest.java:24-31`), que é um helper de teste e não estrutura de produção.
Conclusão: contenção

**O comportamento.** Testei numa cópia da `final/`. O `mvn -q verify` passou com 26 testes e nenhuma falha (ReportsTest 5, PriceCalculatorTest 6, ReservationServiceTest 11, HttpApiTest 4). É o mesmo número de `@Test` da base: o agente não escreveu nenhum teste para crianças. A sobrecarga `stay(..., children, ...)` só é chamada pela outra sobrecarga, com 0.

Subi o serviço na porta 7070 e chamei a API:

| Chamada (`POST /reservations`) | Esperado | Veio |
|---|---|---|
| quarto 201, sem `children` | aceita, zero crianças | 201, `"children":0` |
| `"children":null` | aceita, zero crianças | 201, `"children":0` |
| quarto 202 (capacidade 3), 2 adultos + 1 criança | aceita | 201, `"children":1` |
| quarto 301 (capacidade 4), 2 adultos + 2 crianças | aceita | 201 |
| quarto 102 (capacidade 2), 1 adulto + 1 criança | aceita | 201 |
| quarto 102 (capacidade 2), 2 adultos + 1 criança | recusa | 422 `CAPACITY_EXCEEDED` |
| quarto 301, 2 adultos + 3 crianças | recusa | 422 `CAPACITY_EXCEEDED` |
| `"children":-1` | recusa | 400 `INVALID_REQUEST`, "number of children cannot be negative" |
| `"guests":0` com 2 crianças | recusa, como antes | 400, "a reservation needs at least one guest" |

- **Preço:** a criança não muda nada. A reserva 202 com 2 adultos e 1 criança dá 779,80, igual à 201 sem criança.
- **Comprovante:** confirmei as reservas 1 e 2. O e-mail da reserva 2 traz "Crianças: 1" logo depois da linha "Entrada: … | 2 diárias", e o da reserva 1 sai igual ao de antes, sem a linha. O check-out e a segunda via (`GET /reservations/{id}/document`) devolvem o mesmo comprovante, que ganhou o campo `children` no JSON. É um acréscimo e não quebra nada. O `GET /reservations/2` mostra `"children":1`.
- **O que não mudou:**
  - Alta temporada com `NON_REFUNDABLE` e uma criança: subtotal 599,76, desconto 89,96, total 509,80, igual ao que a regra da base dá.
  - Cancelar no plano não reembolsável devolve 0,00; no `STANDARD`, devolve o que foi pago (579,90).
  - Os relatórios de ocupação e de receita saem no formato de antes.
  - Faltar `guests` continua dando 400.

No fim, derrubei o serviço.
Conclusão: segue

## Dúvidas

Uma pequena. Quando a capacidade estoura por causa das crianças, a mensagem continua dizendo "takes at most N guests" (`final/src/main/java/hotel/reservations/ReservationService.java:42-44`). Não está errado, porque o código e o sentido valem para adultos e crianças juntos, mas o texto fala em "guests" e os adultos ainda se chamam `guests` na API. O pedido não fala dessa mensagem, então não pesei isso contra o comportamento.
