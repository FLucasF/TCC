# Cenário 09

## Visão geral

O agente criou uma classe `CsvReport` (`final/src/main/java/hotel/reports/CsvReport.java:10`) que monta a saída na ordem título, cabeçalho, linhas e total. Os quatro relatórios passam a usá-la, os dois antigos inclusive, e a linha de título ficou num lugar só (`CsvReport.java:38`). Isso é bom: a linha de data prevista para o ano que vem entraria só em `render()`. O problema mais sério é que a `CsvReport` só acumula e formata texto. Cada relatório continua escrevendo a sequência inteira dentro do seu `generate`: escolhe as reservas, ordena, cria o `CsvReport`, percorre as reservas montando as linhas, calcula e põe o total, e chama `render()`. Não há um método-modelo que conduza os passos, nem um escritor que receba a seleção, a linha e o total como partes. É o caso que as instruções chamam de "função utilitária de CSV" com a sequência repetida em cada relatório. O comportamento confere com o pedido, pela leitura do código.

## Como foi implementado, e por que o julgamento

**O projeto.** O pacote `hotel` tem a classe `Hotel`, que monta as peças e sobe o Javalin. `hotel.web.HttpApi` registra as rotas. `hotel.reservations` guarda o domínio (`Reservation`, `ReservationService`, `ReservationStore` em memória, `RatePlan`, `PriceCalculator`). `hotel.rooms` cuida dos quartos, `hotel.messages` dos e-mails, `hotel.documents` dos recibos e `hotel.reports` dos relatórios CSV. Uma chamada a `GET /reports/tourism-tax?month=2027-07` chega em `HttpApi.java:99`. A rota lê o mês e chama `tourismTaxReport.generate(month)`, que lê as reservas do `ReservationStore` e devolve o texto CSV. `Hotel.java:29-35` cria os quatro relatórios e os passa ao construtor da `HttpApi`, que ganhou dois parâmetros. O valor da taxa (`new BigDecimal("4.50")`) é injetado ali (`Hotel.java:34`). O agente atualizou os testes de `ReportsTest` dos dois relatórios antigos para a linha de título e escreveu quatro testes novos: dois da taxa e dois dos planos, um deles com uma reserva não reembolsável cancelada (`final/src/test/java/hotel/reports/ReportsTest.java`).

**A parte que pede o pattern.** O único elemento comum é a `CsvReport`. O construtor recebe o nome, o período e as colunas (`CsvReport.java:20`). `addRow` (`:27`) e `setTotal` (`:31`) guardam strings. `render()` (`:35-44`) escreve o título com "Pousada Maré Alta", o cabeçalho, as linhas e o total. Cada relatório é uma classe `final` independente, sem tipo comum, e o `generate` de cada uma faz todos os passos por conta própria:
- `OccupancyReport.java:25-31` seleciona e ordena, `:33` cria o `CsvReport`, `:35` faz o laço, `:44` põe o total e `:45` chama `render()`.
- `RevenueReport.java:25-32` seleciona e ordena, `:34` cria, `:37` faz o laço, `:47` põe o total e `:48` chama `render()`.
- `TourismTaxReport.java:28-35` seleciona e ordena, `:37` cria, `:40` faz o laço, `:55` põe o total e `:56` chama `render()`.
- `RatePlansReport.java:25-31` seleciona, `:33` cria, `:39` faz o laço por plano, `:53` põe o total e `:54` chama `render()`.

Cada relatório também repete o padrão de acumular o total ao lado do laço. A regra de cada um (o filtro, a ordem, a linha e o total) mora no próprio `generate`, misturada com a condução dos passos.

**O raciocínio.** O que aproxima do pattern: a ordem das seções da saída está num lugar só, os quatro relatórios passam por esse lugar, e a mudança do título foi feita uma vez só. O que afasta: o esqueleto do algoritmo (selecionar, ordenar, uma linha por reserva, total) não está em lugar nenhum. Não há uma classe abstrata com método-modelo e passos abstratos. Também não há a composição aceita como alternativa: a `CsvReport` recebe o nome e as colunas, mas não a seleção, a linha nem o total como objetos ou funções, e quem percorre as reservas é cada relatório. Um relatório novo, como o de cancelamentos, teria de copiar a sequência toda de novo. As instruções descrevem exatamente esse caso como `malfeito`: "o 'esqueleto' é só uma função utilitária de CSV e cada relatório ainda repete a sequência dos passos". Quanto ao exagero, a `CsvReport` faz parte da resposta à variação, e fora dela não foi criada estrutura nova: os construtores ganharam parâmetros diretos, sem fábrica nem interface. Quanto ao comportamento, as regras conferem pela leitura, como está no item abaixo.

## Item a item

**O pattern pedido.** Os quatro relatórios usam `CsvReport` (`OccupancyReport.java:33`, `RevenueReport.java:34`, `TourismTaxReport.java:37`, `RatePlansReport.java:33`), que centraliza a ordem da saída e o título (`CsvReport.java:35-44`). A `CsvReport` é só um acumulador de texto. Cada `generate` repete a sequência inteira: seleção, ordenação, laço de linhas, cálculo e montagem do total, `render()` (por exemplo `TourismTaxReport.java:27-56` e `RevenueReport.java:24-48`). Não há método-modelo com passos abstratos nem um escritor que receba seleção, linha e total como partes. É a utilitária de CSV com a sequência repetida, que as instruções classificam como malfeito.
Conclusão: malfeito

**O exagero colateral.** Não há. A `CsvReport` é a resposta (incompleta) à variação. Fora dela, as mudanças são parâmetros diretos no construtor da `HttpApi` (`HttpApi.java:37-52`) e na montagem em `Hotel.java:29-35`, sem interfaces de uma implementação só, fábricas ou camadas que só repassam.
Conclusão: contenção

**O comportamento.** A conferência foi por leitura, sem subir o serviço nem rodar testes.
- **Título.** `CsvReport.java:38` escreve "Pousada Maré Alta,<nome>,<período>". Os nomes são `occupancy` (`OccupancyReport.java:33`, com `night.toString()`), `revenue` (`RevenueReport.java:34`), `tourism-tax` (`TourismTaxReport.java:37`) e `rate-plans` (`RatePlansReport.java:33`), com `month.toString()`.
- **Ocupação e financeiro depois do título.** Mantêm o filtro, a ordem, as colunas e o total da base: `total,N/M` em `OccupancyReport.java:44` e `total,<valor>` em `RevenueReport.java:47`.
- **Taxa de turismo.** Filtra `isPaid()` (confirmada, com check-in ou com check-out, `Reservation.java:79-83`) e início no mês (`TourismTaxReport.java:29-31`). Ordena como o financeiro (`:35`). A taxa é 4.50 × hóspedes × noites (`:41-44`), o que dá 27.00 para 2 hóspedes por 3 noites, com escala 2. As colunas são as pedidas (`:38`) e o total vem no fim (`:55`).
- **Planos.** Usa o mesmo filtro do financeiro (`amountKept() > 0` e início no mês, `RatePlansReport.java:26-28`), então conta a não reembolsável cancelada. Tem uma linha por plano em `RatePlan.values()`, na ordem STANDARD e depois NON_REFUNDABLE, mesmo com zero reservas (`:39-50`), e o total no fim é `total,<qtd>,<valor>` (`:53`).
- **Rotas.** Estão em `HttpApi.java:99-106`.

Tudo confere com o pedido.
Conclusão: segue

## Dúvidas

Fiquei entre `malfeito` e `alternativa equivalente`. A `CsvReport` é um objeto comum por onde os quatro relatórios passam, e ela garante a ordem título, cabeçalho, linhas e total. Uma mudança no formato, como a linha de data no fim, seria feita uma vez só. Mas ela não recebe as partes que variam (seleção, linha, total), e cada relatório ainda conduz e repete os passos do algoritmo. A evidência sustenta melhor `malfeito`.
