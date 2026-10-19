# Cenário 09

## Visão geral

O agente entregou os dois relatórios novos, `TourismTaxReport` e `RatePlanReport`, e pôs a linha de título nos quatro. Fez isso sem nenhuma estrutura comum. Cada relatório continua sendo uma classe solta que repete, inteira, a sequência de passos: escolher as reservas, ordenar, escrever o título (cada um tem a sua cópia do literal "Pousada Maré Alta"), o cabeçalho, as linhas e o total. Não há classe abstrata, método-modelo nem escritor genérico, e o contador já avisou que vai pedir mais relatórios. Isso é a omissão que as instruções descrevem. O comportamento, por outro lado, está correto em tudo o que testei: os valores, a ordem, as linhas por plano mesmo vazias, a reserva não reembolsável cancelada que entra na conta e os relatórios antigos, que ficaram iguais aos da base depois do título.

## Como foi implementado, e por que o julgamento

**O projeto.** É o mesmo da base: `hotel.Hotel` monta tudo à mão e sobe o Javalin. `hotel.web.HttpApi` registra as rotas. `hotel.reservations` guarda o modelo (`Reservation`, `RatePlan`, `ReservationStatus`), o `ReservationService`, o `PriceCalculator` e o `ReservationStore`, que mantém os dados em memória. `hotel.rooms` tem os cinco quartos fixos e `hotel.reports` tem os relatórios em CSV. Uma requisição de relatório chega numa lambda de `HttpApi` (`final/src/main/java/hotel/web/HttpApi.java:91-106`). A lambda lê `date` ou `month` e chama `generate(...)` da classe concreta do relatório, que lê `store.all()` e monta o CSV num `StringBuilder`. A montagem ganhou dois construtores concretos a mais (`final/src/main/java/hotel/Hotel.java:28-29`), e o construtor de `HttpApi` passou a receber os quatro relatórios, cada um com o seu tipo (`final/src/main/java/hotel/web/HttpApi.java:41-44`). Os testes do agente ficam em `final/src/test/java/hotel/reports/ReportsTest.java`: ele pôs a linha de título nos testes antigos e criou `tourismTaxIsChargedPerGuestPerNight` (linha 104) e `ratePlansSplitTheRevenue` (linha 117). O README não foi atualizado com as rotas novas.

**A parte que pede o pattern.** Há quatro classes `final` independentes, sem supertipo comum:
- `OccupancyReport` (`final/src/main/java/hotel/reports/OccupancyReport.java:23-46`): filtra, ordena por quarto, escreve o título na linha 34, depois o cabeçalho, as linhas e o total `n/5`.
- `RevenueReport` (`final/src/main/java/hotel/reports/RevenueReport.java:23-49`): filtra por `amountKept() > 0` e mês, ordena por check-in e id, escreve o título na linha 35, o cabeçalho, as linhas e o total.
- `TourismTaxReport` (`final/src/main/java/hotel/reports/TourismTaxReport.java:22-50`) é uma cópia da estrutura do financeiro. O filtro é `isPaid()` (linha 26), a ordenação foi copiada na linha 30, o título está na linha 33 e o imposto é calculado em cada linha (37-38) com a constante `TAX_PER_GUEST_PER_NIGHT` da linha 14.
- `RatePlanReport` (`final/src/main/java/hotel/reports/RatePlanReport.java:20-48`) repete o filtro do financeiro (linhas 21-29, copiadas de `RevenueReport.java:24-31`), escreve o título na linha 31, percorre `RatePlan.values()` (linha 34) e escreve o total na linha 46.

Nenhum caso é "escolhido" por uma estrutura. Cada rota chama a sua classe concreta, e cada classe tem o esqueleto completo. A única mudança nos dois relatórios antigos foi uma linha `csv.append("Pousada Maré Alta,<nome>,")` colada em cada um.

**O julgamento.** As instruções aceitam como acerto uma classe abstrata com o método-modelo e os quatro relatórios como subclasses, e como alternativa equivalente um escritor genérico que recebe as partes que variam. Não há nenhuma das duas coisas. Não há sequer uma função utilitária de CSV que pudesse levar a "malfeito": o título, o cabeçalho, o laço e o total são escritos à mão nas quatro classes. É exatamente o caso de omissão das instruções: "os quatro relatórios ficam escritos inteiros, cada um com a sua linha de título". O sinal mais claro é o título, a mudança que valia para todos, que ficou repetido quatro vezes (`OccupancyReport.java:34`, `RevenueReport.java:35`, `TourismTaxReport.java:33`, `RatePlanReport.java:31`). Também não há exagero colateral: o agente não criou interface, fábrica nem camada nova. O que fez foi o oposto, menos estrutura do que o necessário. O comportamento passou em todas as chamadas que fiz, e o que já existia ficou igual ao da base.

## Item a item

**O pattern pedido.** Os quatro relatórios são classes independentes e cada uma repete a sequência inteira (seleção, ordenação, título, cabeçalho, linhas, total): `final/src/main/java/hotel/reports/OccupancyReport.java:23-46`, `final/src/main/java/hotel/reports/RevenueReport.java:23-49`, `final/src/main/java/hotel/reports/TourismTaxReport.java:22-50` e `final/src/main/java/hotel/reports/RatePlanReport.java:20-48`. A linha de título está copiada em cada uma (linhas 34, 35, 33 e 31). O filtro do financeiro foi duplicado em `RatePlanReport.java:21-29`, e a ordenação em `TourismTaxReport.java:30`. Não existe esqueleto comum, nem por herança nem por composição. Com o aviso de que virão os relatórios de cancelamentos e de hóspedes por estado, o texto pedia o Template Method, e a variação ficou como cópia por caso.
Conclusão: omissão

**O exagero colateral.** Não há. O agente só criou as duas classes de relatório e as rotas (`final/src/main/java/hotel/web/HttpApi.java:99-106`), sem interfaces, fábricas ou camadas novas.
Conclusão: contenção

**O comportamento.** Rodei uma cópia da `final/` na porta 7070. Criei sete reservas e as passei por estados diferentes: confirmada, não reembolsável confirmada e cancelada, STANDARD cancelada com reembolso, pendente, com check-out feito, não reembolsável com check-in feito, e uma de junho que atravessa para julho. Os resultados:
- `GET /reports/tourism-tax?month=2027-07` veio com o título `Pousada Maré Alta,tourism-tax,2027-07` e as colunas pedidas. A reserva 1 (2 hóspedes, 3 noites) deu `27.00`, a 3 (1×2) deu `9.00` e a 6 (3×3) deu `40.50`, na ordem do financeiro, com `total,76.50`. A pendente, a cancelada e a de junho ficaram de fora, como deviam.
- `GET /reports/rate-plans?month=2027-07` deu `STANDARD,2,2003.40`, `NON_REFUNDABLE,2,2376.09` e `total,4,4379.49`, que é o mesmo total do financeiro, com a não reembolsável cancelada (`1183.00`) incluída. Em agosto, os dois planos saem com `0,0.00` e `total,0,0.00`. Em junho, a reserva que começa em 29/06 conta em junho nos dois relatórios novos.
- Ocupação e financeiro: começam com `Pousada Maré Alta,occupancy,2027-07-12` e `Pousada Maré Alta,revenue,2027-07`. Rodei o mesmo roteiro na base (porta 7071), tirei a linha de título da saída da final e comparei com `diff`. O financeiro de julho e de junho, a ocupação, o JSON da reserva, a contagem de mensagens e a recusa `OPERATION_NOT_ALLOWED` no check-in de uma reserva pendente saíram idênticos.
- Os erros (`month=xx` e `month` ausente) devolvem `INVALID_REQUEST` com mensagem, como as outras rotas.
- `mvn -q verify` na cópia passou: 28 testes, 0 falhas. Os de relatório são 7, com 2 novos do agente. Os dois serviços foram derrubados no fim.

Tudo conferiu com o pedido.
Conclusão: segue

## Dúvidas

Nenhuma.
