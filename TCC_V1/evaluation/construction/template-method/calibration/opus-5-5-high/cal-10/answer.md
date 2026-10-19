# Cenário 09

## Visão geral

O agente resolveu os relatórios com um Template Method correto. A classe abstrata `CsvReport<P>` guarda o esqueleto num método `final generate`, que escreve o título, o cabeçalho, as linhas e o total. Os quatro relatórios são subclasses que preenchem só o que muda: os dois que já existiam (ocupação e financeiro) e os dois novos (taxa de turismo e planos de tarifa). Nenhum relatório escreve a sequência dos passos por conta própria, e nenhum código que usa os relatórios pergunta qual é o relatório concreto. Fora dos relatórios o desenho é contido: classes concretas, sem interfaces, fábricas ou builders desnecessários. Nos testes pela API, todos os valores, recusas, títulos e totais conferiram. Não achei problema sério. Há só detalhes pequenos: o nome da pousada está repetido em duas constantes, e a `HttpApi` recebe os relatórios pelos tipos concretos.

## Como foi implementado, e por que o julgamento

**O projeto.** O código está dividido em pacotes por assunto:

- `hotel.rooms`: os quartos fixos no código (`Rooms`, `Room`, `RoomType`).
- `hotel.reservations`: a reserva e as regras dela.
  - `Reservation` tem os estados, o valor pago, a devolução e `amountKept` (`Reservation.java:87`).
  - `PriceCalculator` calcula o preço, com alta temporada, desconto e arredondamento meio para o par (`PriceCalculator.java:19-52`).
  - `ReservationService` cuida do ciclo de vida da reserva: criar, confirmar, cancelar, check-in, check-out e segunda via.
  - `ReservationStore` guarda as reservas na memória, num `LinkedHashMap`.
  - `HotelException` com `ErrorCode` representa as recusas.
- `hotel.documents`: o comprovante (`ReservationReceipt`, que também gera o texto) e a formatação em pt-BR (`TextFormat`).
- `hotel.messages`: `EmailSender` grava cada envio em `SentMessages`.
- `hotel.reports`: os relatórios.
- `hotel.web.HttpApi`: as rotas Javalin, a leitura do corpo e dos parâmetros, e a tradução de `ErrorCode` para status HTTP (`HttpApi.java:116-123`).

Uma requisição vai da rota em `HttpApi.registerRoutes` para o `ReservationService` ou para o relatório. Os dados vêm do `ReservationStore`. A montagem é manual: `Hotel` cria as peças e as entrega à `HttpApi` (`Hotel.java:21-31`). O `main` lê `PORT` e usa 7070 se ela não vier (`Hotel.java:44`). O shade gera `target/hotel.jar` com `hotel.Hotel` no manifesto. O agente escreveu 28 testes: 6 de preço, 11 do serviço, 7 dos relatórios e 4 da API HTTP.

**A parte que pede o pattern.**

- `CsvReport<P>` (`reports/CsvReport.java:11`) tem o método-modelo `public final String generate(P period)` (`CsvReport.java:15-25`). Ele seleciona as reservas, escreve `"Pousada Maré Alta,<name>,<period>"`, depois o cabeçalho, uma linha para cada item de `rows`, e por fim `"total," + total(...)`.
- Os passos que variam são abstratos: `name()`, `header()`, `select(P)`, `rows(List)` e `total(List)` (`CsvReport.java:27-37`).
- Cada relatório é uma subclasse `final`:
  - `OccupancyReport extends CsvReport<LocalDate>` (`OccupancyReport.java:12`): escolhe as reservas pagas que ocupam a noite, ordena pelo quarto e devolve `n/5` como total (`OccupancyReport.java:33-53`).
  - `RevenueReport extends CsvReport<YearMonth>` (`RevenueReport.java:14`): a seleção fica em `keptMoneyIn` (`RevenueReport.java:23-29`).
  - `TourismTaxReport` (`TourismTaxReport.java:11`): a taxa é a constante `TAX_PER_GUEST_PER_NIGHT = 4.50` (`TourismTaxReport.java:13`), e a seleção pega as reservas pagas do mês na ordem do financeiro (`TourismTaxReport.java:37-43`).
  - `RatePlanReport` (`RatePlanReport.java:12`): reaproveita `RevenueReport.keptMoneyIn` (`RatePlanReport.java:36`) e gera uma linha para cada valor de `RatePlan.values()` (`RatePlanReport.java:41-44`).
- As rotas chamam só `generate` (`HttpApi.java:91-106`). Não existe `switch` nem `instanceof` sobre o tipo de relatório.

**O julgamento.** É o que as instruções do pattern chamam de acerto: há uma classe abstrata com o método-modelo `final`, os passos são abstratos, e os quatro relatórios são subclasses, inclusive os dois que já existiam. O título está num lugar só (`CsvReport.java:18`). Não está copiado em nenhuma subclasse. Os passos foram escolhidos de um jeito geral: `rows` recebe a lista inteira, e é isso que deixa o relatório por plano, que tem uma linha por plano e não por reserva, caber no mesmo esqueleto sem forçar nada. Há dois pontos menores que não mudam a classe:

- a `HttpApi` recebe os quatro relatórios pelos tipos concretos (`HttpApi.java:37-44`) em vez de `CsvReport`, mas cada rota é fixa, como o enunciado pede, e ninguém pergunta o caso concreto;
- o ordenador do financeiro aparece de novo em `TourismTaxReport.java:41`.

No exagero colateral, não achei interface com uma implementação só, fábrica, builder ou camada que só repassa. O `EmailSender` é fino, mas é ele que fixa o canal `EMAIL` (`EmailSender.java:12-14`). No comportamento, todos os testes pela API conferiram.

## Item a item

**O pattern pedido.** O esqueleto está em `CsvReport.generate`, que é `final` (`reports/CsvReport.java:15-25`), e os passos são abstratos (`CsvReport.java:27-37`). As quatro subclasses preenchem só esses passos: `OccupancyReport.java:12`, `RevenueReport.java:14`, `TourismTaxReport.java:11` e `RatePlanReport.java:12`. A linha de título é escrita uma vez só (`CsvReport.java:18`), e as rotas usam só `generate` (`HttpApi.java:93,97,101,105`). Assim, um relatório novo, como o de cancelamentos, é só mais uma subclasse. Esse é o Template Method que o texto precisava, aplicado aos quatro relatórios.
Conclusão: acerto

**O exagero colateral.** Não há. As classes de serviço, armazenamento, preço e envio são concretas e sem indireção extra (`Hotel.java:22-29`). Há uma repetição pequena, que não é estrutura a mais: a constante `HOTEL_NAME` existe em `CsvReport.java:13` e em `ReservationService.java:12`.
Conclusão: contenção

**O comportamento.** Subi o jar de uma cópia na porta 7070. Criei seis reservas em julho de 2027:

- duas confirmadas;
- uma não reembolsável confirmada e cancelada;
- uma normal confirmada e cancelada;
- uma não reembolsável com check-in e check-out;
- uma pendente.

Os resultados:

- **Ocupação de 10/07:** veio o título `Pousada Maré Alta,occupancy,2027-07-10`, o cabeçalho, as reservas dos quartos 201 e 202 em ordem de quarto e `total,2/5`. Em 13/07, o dia da saída, veio `total,0/5`, como esperado.
- **Financeiro de julho:** vieram as reservas 4, 6, 1 e 2, em ordem de entrada e depois de número. A não reembolsável cancelada aparece com 509.80, a normal cancelada e a pendente ficam de fora, e veio `total,4623.69`.
- **Taxa de turismo de julho:** veio o cabeçalho `reservation,room,check_in,check_out,guests,nights,tax`. As linhas foram 4 hóspedes × 3 noites = 54.00, 1 × 2 = 9.00 e 2 × 3 = 27.00, com `total,90.00`. A cancelada ficou de fora.
- **Planos de julho:** vieram `STANDARD,2,2339.40`, `NON_REFUNDABLE,2,2284.29` e `total,4,4623.69`, que bate com o financeiro, contando a não reembolsável cancelada.
- **Meses vazios (agosto):** os planos vieram com `STANDARD,0,0.00`, `NON_REFUNDABLE,0,0.00` e `total,0,0.00`. A taxa e o financeiro vieram com `total,0.00`.
- **Tipo da resposta:** sempre `text/csv; charset=utf-8`.
- **Recusas dos relatórios:** `month=2027-7` e a falta do parâmetro deram 400 `INVALID_REQUEST`.

Da parte comum do enunciado:

- O DELUXE em julho custou 467.88 por noite, e o não reembolsável de R$ 599,76 teve desconto de 89.96 (meio para o par).
- A reserva de Carla Dias em março deu 779.80. O e-mail e a segunda via saíram exatamente como no enunciado.
- As recusas vieram certas: segunda via de reserva pendente deu 409, conflito de quarto deu 409 `ROOM_UNAVAILABLE`, excesso de hóspedes deu 422, corpo inválido deu 400, e reserva inexistente deu 404.

O `mvn -q verify` passou com 28 testes, sem falhas nem erros. Derrubei o serviço no fim.
Conclusão: segue

## Dúvidas

Nenhuma.
