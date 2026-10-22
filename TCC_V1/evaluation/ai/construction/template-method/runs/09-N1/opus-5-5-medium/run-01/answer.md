Já li o código todo; segue a correção.

# Cenário 09

## Visão geral

O agente montou os relatórios com composição. Um escritor genérico, `ReportWriter`, é o único que sabe o formato: título, cabeçalho, linhas e total. Cada um dos quatro relatórios só devolve as partes que variam, num `ReportData` (período, cabeçalho, linhas e total). Os quatro passam pelo escritor, os dois que já existiam inclusive (ocupação e financeiro), e a rota de cada um é registrada num laço sobre o catálogo. A linha de título está num lugar só e nenhum relatório a repete. A linha de data pedida para o ano que vem, por exemplo, seria uma mudança numa linha só. A base `MonthlyReport` junta a escolha e a ordem das reservas do mês para os três relatórios mensais. Não achei problema sério. O mais perto disso é que cada relatório ainda monta, dentro do seu `generate`, a própria sequência "escolher, montar as linhas, somar o total". Mas essa é a parte que muda de um relatório para outro, e quem decide o formato é o escritor.

## Como foi implementado, e por que o julgamento

**O projeto.**
- **Pacotes:**
  - `pousada.domain`: `Reservation` é um record imutável, e as transições de estado devolvem um novo. Estão aqui também `Quote` (preço com alta temporada e desconto), `RatePlan` (desconto e reembolso por plano), `RoomType`, `ReservationStatus` (com `isPaid`), `Money` (arredondamento HALF_EVEN) e `Receipt`.
  - `pousada.service`: `ReservationService` (criar, confirmar, cancelar, check-in, check-out, segunda via), `ReservationRepository` (um `ConcurrentHashMap` na memória com `AtomicInteger` para os ids), `RoomCatalog` (os cinco quartos fixos), `Notifications` e `Outbox` (e-mails guardados numa lista), `ReceiptText` e `Formats`.
  - `pousada.web`: `Api` (as rotas Javalin), `ReservationRequest` (validação do corpo) e `Views`.
  - `pousada.report`: os relatórios.
- **Caminho de uma requisição:** a rota em `web/Api.java:44-51` valida o corpo (`web/ReservationRequest.java:17-38`) e chama o serviço. O serviço lê o repositório, aplica a regra que está no record de domínio (`domain/Reservation.java:39-57`) e grava a nova versão (`service/ReservationService.java:76-80`).
- **Erros:** os `BusinessException` viram `{error, message}` em `web/Api.java:38-39`.
- **Como sobe:** `Main` lê `PORT` e usa 7070 se ela não vier (`Main.java:7-8`). A montagem é feita à mão em `Api.create()` (`web/Api.java:25-34`).
- **Testes:** o agente escreveu um teste de API com 4 casos (`ApiTest.java`). Um deles confere o texto exato da ocupação, da taxa e dos planos (`ApiTest.java:88-104`).

**A parte que pede o pattern.**
- **O contrato:** `Report` (`report/Report.java:3-10`) tem `name()`, `parameter()` (`date` ou `month`) e `generate(period)`, que devolve um `ReportData` (`report/ReportData.java:5`).
- **O esqueleto:** fica em `ReportWriter.write` (`report/ReportWriter.java:11-18`). Ele escreve o título `Pousada Maré Alta,<nome>,<período>`, depois o cabeçalho, as linhas e o total, e cuida do escape das células CSV.
- **Ocupação:** `OccupancyReport` implementa `Report` direto (`report/OccupancyReport.java:13`) e faz a seleção, a ordem e o total `n/5` (`:33-47`).
- **Relatórios mensais:** os outros três estendem `MonthlyReport` (`report/MonthlyReport.java:13`). Essa base interpreta o mês e dá 400 quando o formato está errado (`:45-51`). Ela também fornece `startingIn` (as reservas que começam no mês, em ordem de entrada e de número, `:33-38`) e `keptMoneyStartingIn` (só as que deixaram dinheiro, `:41-43`).
- **Financeiro:** `RevenueReport` (`report/RevenueReport.java:21-32`).
- **Taxa de turismo:** `TourismTaxReport` (`report/TourismTaxReport.java:24-39`). O valor de 4,50 está numa constante (`:12`).
- **Planos:** `RatePlanReport` (`report/RatePlanReport.java:23-36`) percorre `RatePlan.values()`, por isso a linha de cada plano sai mesmo sem nenhuma reserva.
- **Escolha do relatório:** a lista dos quatro está em `ReportCatalog.all` (`report/ReportCatalog.java:12-18`). `Api` registra uma rota por relatório e sempre chama `writer.write(report, report.generate(period))` (`web/Api.java:53-61`). Ninguém pergunta qual é o relatório concreto.

**Por que o julgamento.**
- **Não é o Template Method clássico:** não existe uma classe abstrata com um método-modelo `final` que chame passos abstratos.
- **É a composição que as instruções aceitam:** um escritor genérico recebe as partes que variam (o nome por `Report.name()`; as colunas, as linhas e o total por `ReportData`), e os quatro relatórios passam por ele. O título, a ordem das seções e o formato CSV aparecem uma vez só (`report/ReportWriter.java:13-17`).
- **Não é malfeito:** nenhum relatório escreve CSV nem título por conta própria, e o escritor não é um utilitário que cada relatório chama passo a passo. Ele dita a sequência inteira.
- **Exagero colateral:** `Report` tem quatro implementações, `MonthlyReport` evita repetir a seleção em três relatórios e `ReportCatalog` é só uma lista. Nada disso é estrutura sem uso.
- **Comportamento:** a leitura do código confere com as regras pedidas, item a item abaixo.

## Item a item

**O pattern pedido.** O esqueleto do relatório (título, cabeçalho, linhas, total) está só em `report/ReportWriter.java:11-18`. Cada relatório entrega só as partes que variam por meio de `Report` e `ReportData` (`report/Report.java:3-10`, `report/ReportData.java:5`). Os quatro passam pelo escritor: os que já existiam (`report/OccupancyReport.java:33-47`, `report/RevenueReport.java:21-32`) e os novos (`report/TourismTaxReport.java:24-35`, `report/RatePlanReport.java:23-31`). A chamada é sempre a mesma, em `web/Api.java:53-61`, sem perguntar o tipo concreto. É a composição que as instruções do cenário aceitam de antemão, e não um método-modelo numa classe abstrata.
Conclusão: alternativa equivalente

**O exagero colateral.** Não há. A interface `Report` tem quatro implementações em uso, `MonthlyReport` (`report/MonthlyReport.java:13-43`) junta uma seleção que três relatórios usam de verdade, e `ReportCatalog` (`report/ReportCatalog.java:12-18`) é só a lista dos relatórios. Fora dos relatórios, a montagem é feita à mão, sem fábricas nem camadas que só repassam.
Conclusão: contenção

**O comportamento.** Não subi o serviço nem chamei a API: conferi só pela leitura do código.
- **Título:** sai como `Pousada Maré Alta,<nome>,<período>` (`report/ReportWriter.java:13`). Os nomes são `occupancy`, `revenue`, `tourism-tax` e `rate-plans`, e o período é o da chamada (`month.toString()` ou `night.toString()`).
- **Ocupação:** conta só reservas pagas que ocupam a noite, com entrada inclusiva e saída exclusiva (`domain/Reservation.java:31-33`). A ordem é por número de quarto, e o total sai como `n/5` (`report/OccupancyReport.java:36-46`).
- **Financeiro:**
  - conta as reservas do mês com `heldAmount > 0`, em ordem de entrada e depois de número (`report/MonthlyReport.java:33-43`);
  - `heldAmount` é o pago menos o devolvido (`domain/Reservation.java:35-37`), então uma NON_REFUNDABLE cancelada continua contando (`domain/RatePlan.java:152-154`);
  - as colunas e o total conferem.
- **Taxa de turismo:** conta as reservas pagas que começam no mês, na mesma ordem (`report/TourismTaxReport.java:25`). A taxa é 4,50 × hóspedes × noites, arredondada, e as colunas e o total conferem (`:26-38`). 2 hóspedes por 3 noites dão 27.00, como o teste do agente também confere.
- **Planos:** sai `STANDARD` e depois `NON_REFUNDABLE` mesmo sem reservas. As linhas contam as mesmas reservas do financeiro, e a linha final é `total,<quantidade>,<valor>` (`report/RatePlanReport.java:24-35`).
- **Resto do sistema:** confere por leitura:
  - o preço com alta temporada e o desconto de 15% (`domain/Quote.java:110-130`);
  - as transições e os reembolsos (`domain/Reservation.java:39-57`);
  - o conflito de quarto ignorando as reservas canceladas (`service/ReservationService.java:31-38`);
  - os erros 400, 404, 409 e 422;
  - o comprovante e o e-mail (`service/ReceiptText.java`, `service/Notifications.java:114-121`);
  - o CSV com `text/csv; charset=utf-8` (`web/Api.java:60`).
Conclusão: segue

## Dúvidas

Cada `generate` ainda faz a sua própria sequência "escolher, mapear as linhas, somar o total", por exemplo em `report/RevenueReport.java:22-31` e `report/TourismTaxReport.java:25-34`. Num Template Method mais estrito, essa sequência também ficaria no esqueleto, com os passos `select`, `row` e `total` separados. Classifiquei como alternativa equivalente porque o que os relatórios têm em comum e o que o dono quer poder mudar em todos de uma vez (o título, a ordem das seções, a futura linha de data) está só no `ReportWriter`. O que fica em cada relatório é justamente o que varia.
