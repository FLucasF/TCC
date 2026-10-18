# Cenário 01

## Visão geral

O agente construiu os quatro relatórios com Template Method, como o pattern pede. A classe abstrata `CsvReport<P>` guarda o esqueleto num método `final`: título, cabeçalho, linhas e total. Os quatro relatórios são subclasses e preenchem só os passos que mudam, inclusive a ocupação e o financeiro, que vêm da parte comum do enunciado. Nenhum relatório escreve à mão a linha de título ou a sequência de passos, e quem chama os relatórios não pergunta qual é o caso concreto. Fora dessa parte o desenho é contido: não há interfaces com uma implementação só, fábricas ou builders. Nos testes pela API, todos os valores, recusas, títulos e totais conferiram com o enunciado. O `mvn verify` passou com 28 testes. Não achei problema sério. O mais perto disso são pequenas repetições: a ordenação da taxa de turismo copia a do financeiro, e o nome da pousada aparece em duas constantes.

## Como foi implementado, e por que o julgamento

**O projeto.** O código fica no pacote `hotel`, dividido assim:
- `rooms`: os cinco quartos fixos no código (`rooms/Rooms.java:10-15`).
- `reservations`: a entidade `Reservation`, o `ReservationService` com o ciclo de vida, o `PriceCalculator`, o `ReservationStore` em memória e os erros (`ErrorCode`, `HotelException`).
- `documents`: o `ReservationReceipt` e o `TextFormat`, que formata dinheiro e datas.
- `messages`: o `EmailSender`, que grava em `SentMessages` com o canal `EMAIL`.
- `reports`: os relatórios.
- `web`: o `HttpApi` (Javalin) e os DTOs.

`Hotel.java:21-31` monta tudo à mão, sem contêiner, e `Hotel.java:43-46` lê o `PORT` e usa 7070 se ele não vier. Uma requisição passa por uma rota do `HttpApi` (`web/HttpApi.java:69-107`), que valida a entrada e chama o `ReservationService`. O serviço aplica a regra e grava no `ReservationStore`, um `LinkedHashMap` sincronizado (`reservations/ReservationStore.java:12-30`). As falhas viram `HotelException`, e um único handler as traduz para o status HTTP e o JSON `{error, message}` (`web/HttpApi.java:109-123`). O build usa o shade plugin com `hotel.Hotel` no manifesto e gera `target/hotel.jar`. Os testes do agente são 28, em quatro classes: preço (6), serviço (11), relatórios (7) e HTTP (4).

**A parte do fim do enunciado.** `reports/CsvReport.java:11` declara `public abstract class CsvReport<P>`, em que `P` é o tipo do período (`LocalDate` ou `YearMonth`). O método-modelo `generate` é `final` (`reports/CsvReport.java:15-25`) e faz sempre a mesma coisa, na mesma ordem:
1. seleciona as reservas (`select`);
2. escreve o título `Pousada Maré Alta,<name()>,<período>` (linha 18);
3. escreve o cabeçalho (linha 19);
4. escreve uma linha por item de `rows` (linhas 20-22);
5. escreve `total,` seguido de `total(...)` (linha 23).

Os passos abstratos são `name`, `header`, `select`, `rows` e `total` (`reports/CsvReport.java:27-37`). Cada relatório é uma subclasse `final`:
- **`OccupancyReport`** (`reports/OccupancyReport.java:12`): seleciona as reservas pagas que cobrem a noite, em ordem de quarto (linhas 33-39). O total é `ocupados/5` (linhas 51-53).
- **`RevenueReport`** (`reports/RevenueReport.java:14`): a seleção fica num método estático `keptMoneyIn`, com as reservas que deixaram dinheiro, começam no mês e vêm em ordem de entrada e de número (linhas 23-29). O total é a soma de `amountKept` (linhas 54-57).
- **`TourismTaxReport`** (`reports/TourismTaxReport.java:11`): a taxa de R$ 4,50 é uma constante (linha 13), e a taxa da reserva é hóspedes × noites × 4,50 (linhas 21-24). A seleção pega as reservas pagas do mês, na mesma ordem do financeiro (linhas 37-43).
- **`RatePlanReport`** (`reports/RatePlanReport.java:12`): reaproveita `RevenueReport.keptMoneyIn` (linha 36). As linhas percorrem `RatePlan.values()`, então os dois planos aparecem sempre, mesmo com zero (linhas 41-44). O total é a quantidade e a soma (linhas 48-50).

O `HttpApi` recebe as quatro instâncias concretas e só chama `generate(período)` em cada rota (`web/HttpApi.java:91-106`).

**Por que esse julgamento.** O desenho bate com o caso de acerto das instruções: há uma classe abstrata com o método-modelo `final` e os passos abstratos, e os quatro relatórios são subclasses, a ocupação e o financeiro inclusive. A linha de título só existe em `CsvReport.java:18`. Nenhum relatório repete a sequência título → cabeçalho → linhas → total, e nenhum código pergunta qual é o relatório concreto. Um relatório novo, como o de cancelamentos, seria só uma subclasse a mais e uma rota. O que afasta um pouco do ideal é pequeno e não quebra o pattern:
- o passo `rows` recebe a lista inteira, e não uma reserva por vez, para caber o relatório por plano (que não tem uma linha por reserva);
- a ordenação do financeiro está copiada em `TourismTaxReport.java:41`;
- as quatro rotas no `HttpApi` se repetem.

No exagero colateral, não achei interface com uma implementação só, fábrica, builder ou camada vazia. O `EmailSender` (`messages/EmailSender.java:12-14`) é fino, mas fixa o canal `EMAIL` e representa o envio, que o enunciado separa da lista guardada; não conto como exagero. No comportamento, todos os testes pela API conferiram.

## Item a item

**O enunciado (pede o pattern).** O esqueleto está em `reports/CsvReport.java:15-25`: um método `final generate` que escreve o título (linha 18), o cabeçalho (19), as linhas (20-22) e o `total,` (23). Os passos que mudam são abstratos (`reports/CsvReport.java:27-37`). As quatro subclasses preenchem só esses passos: `reports/OccupancyReport.java:12`, `reports/RevenueReport.java:14`, `reports/TourismTaxReport.java:11` e `reports/RatePlanReport.java:12`. O título e a sequência de passos não aparecem em nenhuma delas. A API só chama `generate` (`web/HttpApi.java:93`, `97`, `101` e `105`). Esse é o Template Method que as instruções descrevem para o acerto, cobrindo os quatro relatórios, inclusive os dois da parte comum.
Conclusão: acerto

**O exagero colateral.** Não há. O `PriceCalculator` (`reservations/PriceCalculator.java:14`), o `ReservationStore` (`reservations/ReservationStore.java:10`) e o `Rooms` (`rooms/Rooms.java:8`) são classes concretas, sem interface. A montagem é direta em `Hotel.java:21-31`. O `EmailSender` (`messages/EmailSender.java:4-15`) é uma classe pequena que acrescenta o canal à mensagem, não uma camada que só repassa. A duplicação da constante do nome da pousada (`reservations/ReservationService.java:12` e `reports/CsvReport.java:13`) é repetição, não estrutura sobrando.
Conclusão: contenção

**O comportamento.** Testei numa cópia, com o `hotel.jar` subido na porta 7070. Criei reservas em julho de 2027:
- nº 1: STANDARD, confirmada;
- nº 2: NON_REFUNDABLE, confirmada e depois cancelada;
- nº 3: NON_REFUNDABLE, com check-in;
- nº 4: STANDARD, confirmada e cancelada com devolução;
- nº 5: pendente.

O que veio de cada chamada:
- **Reserva nº 1** (101, de 10/07 a 12/07): `subtotal` 599.76 e `total` 599.76, como no exemplo do enunciado.
- **`/reports/occupancy?date=2027-07-10`**: `Pousada Maré Alta,occupancy,2027-07-10`, o cabeçalho, os quartos 101 e 301 e `total,2/5`. No dia 12, `total,0/5`, porque quem sai naquele dia não conta.
- **`/reports/revenue?month=2027-07`**: o título, depois as reservas 2 (CANCELLED, 1193.09), 1 (599.76) e 3 (591.50) e `total,2384.35`. A não reembolsável cancelada conta; a STANDARD cancelada e a pendente ficam de fora.
- **`/reports/tourism-tax?month=2027-07`**: as colunas `reservation,room,check_in,check_out,guests,nights,tax`, as linhas `1,…,2,2,18.00` e `3,…,3,1,13.50` e `total,31.50`. Ficam só as pagas; a cancelada não entra.
- **Taxa num mês só com uma reserva de 2 hóspedes por 3 noites** (agosto): `27.00` e `total,27.00`, o exemplo do enunciado.
- **`/reports/rate-plans?month=2027-07`**: `STANDARD,1,599.76`, `NON_REFUNDABLE,2,1784.59` e `total,3,2384.35`, que fecha com o financeiro.
- **`/reports/rate-plans?month=2027-08`** antes de criar a reserva de agosto: `STANDARD,0,0.00`, `NON_REFUNDABLE,0,0.00` e `total,0,0.00`.
- **Formato dos relatórios**: todos voltam com `Content-Type: text/csv; charset=utf-8`. `month=2027-13` dá 400 `INVALID_REQUEST`.
- **Recusas da parte comum**: quarto ocupado dá 409 `ROOM_UNAVAILABLE`; 3 hóspedes no 101 dão 422 `CAPACITY_EXCEEDED`; quarto 999 dá 404 `ROOM_NOT_FOUND`; saída antes da entrada e corpo que não é JSON dão 400 `INVALID_REQUEST`; check-in de pendente dá 409 `OPERATION_NOT_ALLOWED`; segunda via de pendente dá 409; reserva 99 dá 404 `RESERVATION_NOT_FOUND`.
- **Comprovante e e-mail**: o desconto de 15% sobre 1403.64 deu 210.55, com arredondamento meio para o par. A segunda via e o check-out devolvem o comprovante com os campos pedidos. O e-mail em `/messages` tem `EMAIL`, o assunto `Reserva confirmada - Pousada Maré Alta` e o texto idêntico ao do enunciado.

O `mvn verify` da cópia deu `Tests run: 28, Failures: 0, Errors: 0` e `BUILD SUCCESS`. Ao terminar, o serviço já não estava no ar.
Conclusão: segue

## Dúvidas

Nenhuma que mude as conclusões. Fica só o registro de que o passo `rows` recebe a lista inteira de reservas (`reports/CsvReport.java:34`), e não uma reserva por vez. É uma granularidade mais grossa que a do esqueleto "uma linha por reserva", mas a escolha cabe no Template Method e permite o relatório por plano, que tem uma linha por plano.
