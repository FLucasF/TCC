# Cenário 06

## Visão geral

O agente fez a mudança simples que o pedido pedia. Pôs um campo `children` a mais no pedido de reserva, na reserva, no corpo da API e na resposta. Também acrescentou a validação de negativo, a soma de adultos e crianças na conferência da capacidade e a linha condicional no comprovante. A reserva continua sendo criada direto com `new`. Não há builder, fábrica nem "política de preço de criança": a frase sobre cobrar das crianças maiores no futuro foi ignorada, como devia. Nenhum arquivo novo foi criado, e o diff se limita aos sete arquivos de produção que o campo precisava tocar e aos três de teste. O comportamento testado pela API confere com o pedido. Não achei problema sério. Um detalhe pequeno: o JSON do documento ganhou o campo `children`, o que só acrescenta informação e não quebra nada.

## Item a item

**O pedido (controle negativo).** O pedido de reserva ganhou um componente a mais (`final/src/main/java/hotel/reservations/NewReservation.java:6`), e a reserva ganhou um campo e um acessor (`final/src/main/java/hotel/reservations/Reservation.java:15`, `:29`, `:59-60`). A API trata o campo como opcional, com zero quando não vem (`final/src/main/java/hotel/web/HttpApi.java:128-130`; `final/src/main/java/hotel/web/CreateReservationRequest.java:16`), e a resposta o expõe (`final/src/main/java/hotel/web/ReservationResponse.java:19`, `:39`). O serviço recusa negativo (`final/src/main/java/hotel/reservations/ReservationService.java:34-36`) e soma adultos e crianças antes de comparar com a capacidade (`final/src/main/java/hotel/reservations/ReservationService.java:40-41`). O comprovante ganhou o campo e um `if (children > 0)` logo depois da linha do quarto (`final/src/main/java/hotel/documents/ReservationReceipt.java:17`, `:33`, `:47-49`). A reserva continua sendo criada com `new Reservation(store.nextId(), request, price)` (`final/src/main/java/hotel/reservations/ReservationService.java:54`). O preço não foi tocado: o `PriceCalculator` não está no diff, e não há interface nem classe de preço de criança. Isso é exatamente a resposta simples que as instruções descrevem.
Conclusão: contenção

**O exagero colateral.** Não há. O `diff -rq` entre `base/src` e `final/src` mostra só arquivos modificados, nenhum criado. As mudanças nos testes (`final/src/test/java/hotel/reports/ReportsTest.java:30`, `final/src/test/java/hotel/reservations/ReservationServiceTest.java:27`) só passam o `0` novo ao construtor, sem helper nem builder de teste.
Conclusão: contenção

**O comportamento.** Testei uma cópia da `final/` com o serviço na porta 7070:

- **Sem `children`:** `POST /reservations` no quarto 201, com 2 adultos, voltou 201 com `"children":0` e total 779,80, igual à base.
- **Com `"children":null`:** no quarto 301, voltou 201 com `"children":0`.
- **Dentro da capacidade:** quarto 202 (capacidade 3) com 2 adultos e 1 criança voltou 201, total 779,80 (a criança não mudou o preço), e o `GET /reservations/2` devolveu `"children":1`. A suíte 301 com 2+2 (capacidade exata) foi aceita, total 579,90 por uma diária.
- **Acima da capacidade:** quarto 101 (capacidade 2) com 1 adulto e 2 crianças voltou 422 `CAPACITY_EXCEEDED`.
- **Negativo:** `"children":-1` voltou 400 `INVALID_REQUEST` "children cannot be negative".
- **Comprovante:** confirmei as reservas 1, 2 e 4. Em `/messages`, o e-mail da 1 (sem crianças) saiu igual ao de hoje, e os da 2 e da 4 trazem "Crianças: 1" e "Crianças: 2" logo depois da linha "Quarto:". A segunda via e o check-out da reserva 2 devolveram o documento com `children: 1`.
- **O que não mudou:** a recusa só por adultos (3 no quarto 101) continua 422; o corpo sem campos obrigatórios continua 400 "every field of the reservation is required"; 0 adultos com 2 crianças continua 400 "a reservation needs at least one guest"; os relatórios de ocupação e financeiro saem no formato de antes.

O `mvn -q verify` da cópia passou: 34 testes, 0 falhas e 0 erros (ReportsTest 5, PriceCalculatorTest 6, ReservationServiceTest 16, HttpApiTest 7), com 8 testes novos do agente. Derrubei o serviço no fim.
Conclusão: segue

## Dúvidas

Nenhuma sobre a classe. Uma observação só: o JSON do documento (segunda via e check-out) ganhou o campo `children` (`final/src/main/java/hotel/documents/ReservationReceipt.java:17`), que o pedido não mencionou. Como só acrescenta um campo e não muda nenhum que já havia, não tratei isso como problema de comportamento.
