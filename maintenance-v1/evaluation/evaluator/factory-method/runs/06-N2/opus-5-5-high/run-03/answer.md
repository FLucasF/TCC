# Cenário 06

## Visão geral

O agente fez a mudança simples que o pedido pedia. Pôs um campo `children` no pedido de reserva, na reserva, no corpo HTTP, na resposta e no comprovante. Somou adultos e crianças na conferência da capacidade e colocou a linha "Crianças: N" no comprovante, que só aparece quando há criança. A frase sobre cobrar das crianças maiores "no futuro" ficou sem estrutura nenhuma: não há builder, fábrica nem política de preço de criança. A reserva e o pedido de reserva continuam sendo criados direto. Não achei problema sério. Há dois detalhes menores. A recusa de número negativo foi escrita duas vezes, na camada web e no serviço. E o JSON do comprovante passou a trazer `children` sempre, inclusive `0`.

## Item a item

**O pedido (controle negativo).** O agente acrescentou um parâmetro `int children` ao record do pedido de reserva (`src/main/java/hotel/reservations/NewReservation.java:6`). A reserva ganhou o campo e o getter (`src/main/java/hotel/reservations/Reservation.java:14`, `:28`, `:59`). O corpo HTTP ganhou `Integer children`, que pode ficar de fora (`src/main/java/hotel/web/CreateReservationRequest.java:16`). Quando ele não vem, a camada web converte para 0 (`src/main/java/hotel/web/HttpApi.java:132`). O serviço recusa número negativo (`src/main/java/hotel/reservations/ReservationService.java:34-35`) e soma adultos e crianças na capacidade (`src/main/java/hotel/reservations/ReservationService.java:40-41`). O comprovante ganhou a linha condicional logo depois da linha do quarto (`src/main/java/hotel/documents/ReservationReceipt.java:47-48`), e a resposta da reserva expõe o campo (`src/main/java/hotel/web/ReservationResponse.java:18`, `:38`).

A reserva continua sendo criada com `new` direto (`src/main/java/hotel/reservations/ReservationService.java:54`, `src/main/java/hotel/web/HttpApi.java:131`). O cálculo de preço (`PriceCalculator`) não foi tocado. Também não há interface nem classe nova no patch. A única redundância é que a recusa de negativo aparece também em `src/main/java/hotel/web/HttpApi.java:127-128`, o que não é estrutura.

Conclusão: contenção

**O exagero colateral.** Não há. O patch só mexe em records e classes que já existiam, sem criar nenhum tipo novo. Os três `new ReservationReceipt(...)` continuam como estavam (`src/main/java/hotel/reservations/ReservationService.java:72`, `:114`, `:123`).

Conclusão: contenção

**O comportamento.** Subi uma cópia da `final/` na porta 7070 e chamei a API com `curl`:

- **Sem `children`** (quarto 201, 2 adultos): veio 201 com `"children":0` e total 779,80, igual à base. Com `"children":null` também veio `0`.
- **Suíte 301, 2 adultos e 2 crianças:** veio 201, total 1.159,80, o mesmo preço de 2 noites sem criança. Em julho, o quarto 202 com 2+1 e o 201 com 2+0 deram o mesmo total, 935,76. Ou seja, criança não muda o preço.
- **Capacidade:** o quarto 101 (capacidade 2) com 2+1 deu 422 `CAPACITY_EXCEEDED`. Três adultos no 101 continuam recusados com 422.
- **`"children":-1`:** veio 400 `INVALID_REQUEST`, "children count cannot be negative". Com `"children":"dois"` veio 400.
- **Comprovante:**
  - No e-mail de confirmação da reserva com crianças saiu `Crianças: 2` logo depois de `Quarto: 301 (SUITE)` e antes da linha `Entrada:`.
  - O e-mail da reserva sem criança saiu igual ao de hoje, sem essa linha.
  - O check-out e a segunda via trazem `children: 2`, e `GET /reservations/{id}` também mostra o campo.
- **O que o pedido não mudou:**
  - O plano `NON_REFUNDABLE` continua com o desconto (134,95), e cancelar continua devolvendo 0,00.
  - Pedido incompleto continua dando "every field of the reservation is required", e 0 adultos continua recusado.
  - Os relatórios de ocupação e de receita saem no formato de antes.

O `mvn -q verify` da cópia passou com 29 testes e nenhuma falha. A base tinha 26, e o agente acrescentou 3: dois no `ReservationServiceTest` e um no `HttpApiTest`. Depois derrubei o serviço.

Conclusão: segue

## Dúvidas

Nenhuma sobre a classe. Um detalhe que não muda a conclusão: o JSON do comprovante (check-out e segunda via) agora traz o campo `children` sempre, inclusive `0` quando não há criança. O pedido só falava do texto do comprovante, e esse texto continua igual para quem não tem criança. Isso só acrescenta um campo ao JSON e não quebra nada do que já funcionava.
