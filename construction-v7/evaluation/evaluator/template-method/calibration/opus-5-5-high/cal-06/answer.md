# Cenário 01

## Visão geral

O agente fez os quatro relatórios com Template Method, e do jeito certo. `CsvReport<P>` tem um método-modelo `final` com a sequência inteira: seleção, título, cabeçalho, linhas e total. Os quatro relatórios são subclasses que só preenchem os passos abstratos, o de ocupação e o financeiro inclusive. A linha de título aparece num lugar só, e nenhum relatório refaz a sequência. O resto do sistema é enxuto: classes concretas, sem interfaces, fábricas nem builders. Os quatro relatórios e as partes da parte comum que testei deram o que o enunciado pede. Nenhum problema sério apareceu. Há só duas miudezas que não afetam a classe: o nome da pousada definido em duas constantes iguais e a soma de dinheiro repetida entre `RevenueReport` e `RatePlanReport`.

## Item a item

**O enunciado (pede o pattern).** `src/main/java/hotel/reports/CsvReport.java:11` é uma classe abstrata. O método-modelo `generate` em `CsvReport.java:15` é `final` e fixa a ordem: `select` (linha 16), o título "Pousada Maré Alta,<nome>,<período>" (linha 18), `header()` (linha 19), uma linha por item de `rows` (linhas 20-22) e "total," seguido de `total(...)` (linha 23). Os passos que mudam são abstratos: `name`, `header`, `select`, `rows` e `total` (`CsvReport.java:27-37`). Os quatro relatórios são subclasses e só implementam esses passos:
- `OccupancyReport.java:12`
- `RevenueReport.java:14`
- `TourismTaxReport.java:11`
- `RatePlanReport.java:12`

Nenhum deles escreve título nem cabeçalho por conta própria, e ninguém pergunta qual é o relatório concreto. O `HttpApi` só chama `generate` em cada rota (`web/HttpApi.java:93`, `:97`, `:101`, `:105`). O relatório por plano agrupa os planos dentro do seu próprio passo `rows`, percorrendo `RatePlan.values()` (`RatePlanReport.java:41-44`), sem furar o esqueleto. Para fazer um relatório novo, como o de cancelamentos ou o de hóspedes por estado, basta uma subclasse nova. É o Template Method que o enunciado pedia, aplicado aos quatro casos.
Conclusão: acerto

**O exagero colateral.** Não há:
- Não existe nenhuma interface no projeto.
- `ReservationStore` (`reservations/ReservationStore.java:10`), `PriceCalculator` (`reservations/PriceCalculator.java:14`) e `Rooms` (`rooms/Rooms.java:8`) são classes concretas, cada uma com trabalho próprio.
- O plano de tarifa é tratado com um `switch` simples no desconto (`PriceCalculator.java:26-29`) e no reembolso (`ReservationService.java:86-89`), o que basta para dois casos.
- O ponto mais próximo de uma camada que só repassa é `EmailSender` (`messages/EmailSender.java:12-14`). Mas ele fixa o canal `EMAIL` e dá nome ao envio, então não vejo nele estrutura sem necessidade.

Conclusão: contenção

**O comportamento.** Montei o jar de uma cópia (`target/hotel.jar`), subi o serviço na porta 7070 e usei sete reservas:
- 101 STANDARD de 10/07 a 12/07/2027, 2 hóspedes, confirmada;
- 201 NON_REFUNDABLE de 05/07 a 08/07, 3 hóspedes, confirmada e cancelada;
- 301 STANDARD de 05/07 a 08/07, 2 hóspedes, confirmada, com check-in e check-out;
- 102 de 20/07 a 21/07, pendente;
- 202 STANDARD de 15/07 a 17/07, confirmada e cancelada, com reembolso;
- 102 de 01/08 a 03/08, confirmada;
- 201 de 10/03 a 12/03, confirmada.

O que veio:
- **Título:** os quatro relatórios começam com `Pousada Maré Alta,occupancy,2027-07-10`, `...,revenue,2027-07`, `...,tourism-tax,2027-07` e `...,rate-plans,2027-07`. Todos vêm em `text/csv; charset=utf-8`.
- **Taxa de turismo de 2027-07:** veio o cabeçalho `reservation,room,check_in,check_out,guests,nights,tax`, depois `3,301,2027-07-05,2027-07-08,2,3,27.00` e `1,101,2027-07-10,2027-07-12,2,2,18.00`, e no fim `total,45.00`. É a ordem do financeiro, por data de entrada e depois por número. A não reembolsável cancelada, a pendente e a reembolsada ficaram de fora, como o enunciado pede.
- **Taxa de turismo de 2027-09:** só o cabeçalho e `total,0.00`.
- **Planos de 2027-07:** `STANDARD,2,2687.40`, `NON_REFUNDABLE,1,1193.09` e `total,3,3880.49`. A não reembolsável cancelada conta, e o total bate com o financeiro.
- **Planos de 2027-08:** `NON_REFUNDABLE,0,0.00`. Em 2027-09 vêm as duas linhas zeradas e `total,0,0.00`.
- **Financeiro de 2027-07:** reservas 2 (CANCELLED, 1193.09), 3 (CHECKED_OUT, 2087.64) e 1 (599.76), com `total,3880.49`.
- **Ocupação:** em 10/07 deu `101,...` e `total,1/5`. Em 08/07 deu `total,0/5`, porque quem sai nesse dia não conta. Em 06/07 só aparece a 301, porque a 201 foi cancelada.
- **Parte comum:** 101 de 10/07 a 12/07 deu 599.76. A não reembolsável deu desconto de 210.55 e total de 1193.09. O e-mail da reserva nº 7 saiu idêntico ao do enunciado. O check-out devolveu o comprovante. Recusas: quarto ocupado dá 409 ROOM_UNAVAILABLE, 3 hóspedes no 101 dão 422 CAPACITY_EXCEEDED, check-in de reserva pendente dá 409 OPERATION_NOT_ALLOWED, segunda via de reserva pendente também dá 409, `month=2027-13` dá 400 INVALID_REQUEST e mês ausente também dá 400.
- **Testes do agente:** o `mvn verify` da cópia passou, com 28 testes, 0 falhas e 0 erros. O serviço foi derrubado no fim.

Conclusão: segue

## Dúvidas

A classe e o comportamento ficaram decididos pela evidência. Restam três observações que não mudam nada:
- O nome da pousada está em duas constantes iguais, em `reports/CsvReport.java:13` e em `reservations/ReservationService.java:12`.
- A soma do dinheiro que ficou com a pousada é repetida em `RatePlanReport.java:20-22` e em `RevenueReport.java:55-56`.
- O valor da taxa é uma constante no código (`TourismTaxReport.java:13`), embora a prefeitura mude esse valor todo ano. Isso atende ao pedido de hoje, mas cada ano vai exigir mudar o código.
