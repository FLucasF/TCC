# Revisão: manutenção, cenário 09

## Visão geral

A base é um serviço Javalin em sete pacotes: `hotel.web` (as rotas em `HttpApi`), `hotel.reservations` (as regras, em
`ReservationService` e `PriceCalculator`, e o acesso ao banco em `ReservationStore`), `hotel.rooms`, `hotel.messages`,
`hotel.documents`, `hotel.reports` e `hotel` (a montagem, em `Hotel`). Uma requisição de relatório entra em
`HttpApi` (`final/src/main/java/hotel/web/HttpApi.java:91-106`), que lê o parâmetro e chama `generate` do relatório. O
relatório lê `store.all()` e monta o CSV em memória. Os dados ficam no PostgreSQL, num esquema criado pelo Flyway. O
agente fez uma mudança pequena e correta no que faz: criou `TourismTaxReport` e `RatePlansReport` e um utilitário
estático `ReportFormatter.withTitle` para a linha de título, chamado no fim de cada um dos quatro relatórios. Também
registrou as duas rotas novas e atualizou os testes. As 64 conferências do roteiro passaram. O ponto mais sério é o
design: o texto pede que "o jeito dos relatórios" mude em todos de uma vez, mas o agente não juntou o esqueleto comum
(seleção, ordem, cabeçalho, linhas, total) num lugar só. Cada um dos quatro relatórios repete a sequência inteira, e
só a linha de título ficou centralizada. Além disso, a regra "as mesmas reservas que o financeiro conta" foi copiada
em vez de reaproveitada.

## Arquitetura

**Números:** 7 pacotes e 17 dependências entre eles, os mesmos da base (`measures/dependencies.md` e
`measures/dependencies-base.md`). Há 1 ciclo, `hotel.documents` ↔ `hotel.reservations`, que já existia na base.
`hotel.reports` passou de 2 para 6 classes (95 → 218 linhas) e continua dependendo só de `hotel.reservations` e de
`hotel.rooms`, sem Javalin nem JDBC.
**Nota:** bom

As classes novas ficaram onde deviam. `TourismTaxReport` e `RatePlansReport` estão em `hotel.reports` e só usam
`ReservationStore` e `Reservation` (`final/src/main/java/hotel/reports/TourismTaxReport.java:1-21`,
`final/src/main/java/hotel/reports/RatePlansReport.java:1-18`). As rotas novas são finas: leem o mês, chamam
`generate` e escrevem `text/csv` (`final/src/main/java/hotel/web/HttpApi.java:99-106`). A montagem continua num lugar
só (`final/src/main/java/hotel/Hotel.java:28-35`). O ciclo entre `documents` e `reservations` vem da base:
`ReservationService` cria `ReservationReceipt` (`final/src/main/java/hotel/reservations/ReservationService.java:3,64`).
O agente não mexeu nisso. Um detalhe, que não muda a nota: o construtor de `HttpApi` ganha um parâmetro por
relatório e já tem sete (`final/src/main/java/hotel/web/HttpApi.java:37-44`). Com os relatórios de cancelamentos e
de hóspedes por estado que o contador já anunciou, ele vai continuar crescendo. Uma estrutura comum dos relatórios
resolveria isso também.

## Design de baixo nível

**Números:** dos 4 relatórios, nenhum passa por um esqueleto comum. Os 4 repetem a sequência inteira (filtrar
`store.all()`, ordenar, `StringBuilder` com o cabeçalho, laço das linhas, linha do total). Só a linha de título
está num lugar só, `ReportFormatter.withTitle`, que os 4 chamam no fim. A regra de seleção do financeiro está em 2
lugares.
**Classe:** malfeito
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** O texto pede o Template Method sem dizer o nome: quatro relatórios com os mesmos passos, mais dois a
caminho, e "quando o jeito dos relatórios mudar, quero que mude em todos de uma vez". O exemplo dado é a data no fim
de todos, no ano que vem. A resposta do agente é um utilitário estático de uma linha
(`final/src/main/java/hotel/reports/ReportFormatter.java:10-12`), chamado no `return` de cada relatório
(`OccupancyReport.java:44`, `RevenueReport.java:48`, `TourismTaxReport.java:51`, `RatePlansReport.java:38`). Isso é o
caso que a referência descreve como malfeito: o "esqueleto" é só uma função utilitária de CSV, e cada relatório
ainda repete a sequência dos passos. Por exemplo, `RevenueReport.java:24-47` e `TourismTaxReport.java:23-50` são o
mesmo método com outra condição e outra linha. A mudança já anunciada (a linha com a data no fim) teria de ser feita
nos quatro, ou o `withTitle` teria de virar outra coisa. O que aproxima do pattern é que o título, a parte que mudou
agora, ficou num lugar só, e que os dois relatórios antigos também passam por ele. Não há exagero: nenhuma interface,
fábrica ou camada a mais. O utilitário é tão pequeno que não chega a ser estrutura.

**O resto do código.**
- A regra copiada é o defeito mais concreto. O texto diz que o relatório de planos conta "as mesmas reservas que o
  relatório financeiro conta", e o agente copiou a condição (`amountKept().signum() > 0` e o mês da entrada) de
  `final/src/main/java/hotel/reports/RevenueReport.java:26-28` para `RatePlansReport.java:45-48`. Se a regra do
  financeiro mudar, o relatório por plano fica diferente sem ninguém perceber. A taxa de turismo também copia a
  ordenação do financeiro (`TourismTaxReport.java:31` = `RevenueReport.java:32`).
- `RatePlansReport.summarize` percorre a tabela inteira uma vez por plano: `store.all()` dentro do laço sobre
  `RatePlan.values()` (`RatePlansReport.java:22-23,44`).
- O que está bem: dinheiro em `BigDecimal` com escala 2 (`TourismTaxReport.java:15,35`, `RatePlansReport.java:29,43`);
  noites por `ChronoUnit.DAYS.between` sobre `LocalDate` (`TourismTaxReport.java:37`); o plano como `enum`, na ordem
  da declaração (`RatePlansReport.java:22`); o valor da taxa numa constante só (`TourismTaxReport.java:15`); e um
  `record` privado para o resumo (`RatePlansReport.java:56`).
- Um detalhe: `ReportsTest` importa duas classes do próprio pacote (`final/src/test/java/hotel/reports/ReportsTest.java:8-9`).

## Testes

**Números:** 30 testes rodaram, 0 falhas, 0 erros, 0 pulados; 30 métodos escritos (a base tinha 26, todos mantidos e
passando). Cobertura de linhas: 94,6% (543/574; a base tinha 93,8%). Cobertura de ramos: 89,3% (125/140; a base
tinha 87,1%). `hotel.reports` está em 110/110 linhas e 46/46 ramos (`measures/tests.md` e `measures/tests-base.md`).
**Nota:** com problemas

O agente atualizou os cinco testes de relatório da base para conferir a linha de título com o CSV inteiro
(`final/src/test/java/hotel/reports/ReportsTest.java:62-121`), sem enfraquecer nenhum. Também escreveu quatro testes
novos que comparam o CSV inteiro:
- a taxa de turismo do mês com as pagas fora (a pendente e a cancelada) e o total (`ReportsTest.java:124-135`);
- a taxa de uma reserva de 3 noites, que dá 13,50 (`ReportsTest.java:137-150`);
- os planos com o `NON_REFUNDABLE` zerado (`ReportsTest.java:152-163`);
- um mês sem reservas (`ReportsTest.java:165-176`).

Pela API, `HttpApiTest` confere o título dos quatro relatórios e o total da taxa com 2 hóspedes por 2 noites, que dá
18,00 (`final/src/test/java/hotel/web/HttpApiTest.java:113-129`). Assim, trocar 4.50, tirar a multiplicação pelos
hóspedes ou pelas noites, ou errar o título faz algum teste falhar.

O que fica sem rede é a regra que o texto destaca no relatório por plano. Nenhum teste tem uma reserva
`NON_REFUNDABLE` com dinheiro no mês: a única `NON_REFUNDABLE` do cenário de teste começa em março
(`ReportsTest.java:46`), e o teste dos planos é de abril. Também não há uma `NON_REFUNDABLE` cancelada (que deve
contar). Se alguém trocasse a condição `amountKept().signum() > 0` por `isPaid()` em `RatePlansReport.java:45`, a
`NON_REFUNDABLE` cancelada sairia do relatório e todos os testes continuariam passando. O mesmo vale se o valor de um
plano fosse para o outro. A asserção da API é fraca: ela olha só o começo e o fim do corpo (`startsWith`/`endsWith`,
`HttpApiTest.java:123-129`).

## Banco

**Números:** 3 tabelas, todas com chave primária. O dinheiro está em `numeric(10,2)` nas 6 colunas e as datas em
`date`. A chave estrangeira tem índice. A duplicação de reserva é impedida por `EXCLUDE USING gist`. Há 1 migração
(`V1__create_tables.sql`), igual à da base. As conferências acusaram só as colunas de situação e tipo sem `CHECK`
(`messages.channel`, `reservations.rate_plan`, `reservations.status`, `rooms.type`), e isso vem da base
(`measures/database.md`).
**Nota:** bom

O pedido não mexe no banco, e o agente não mexeu: nenhuma migração nova ou editada, nenhum SQL novo. Os relatórios
novos só usam `ReservationStore.all()`, que já existia (`TourismTaxReport.java:25`, `RatePlansReport.java:44`). Os
candidatos a SQL montado com `+` em `ReservationStore.java:33,83,98,111` concatenam só constantes (`COLUMNS`) e vêm
da base. Fica uma nota sobre a carga. Os relatórios trazem a tabela de reservas inteira para filtrar o mês em Java,
como os da base já faziam, e `RatePlansReport` faz isso uma vez por plano (`RatePlansReport.java:22-23,44`), duas
leituras completas por chamada. Numa pousada pequena isso não pesa, mas cresce com o histórico.

## Comportamento

**Números:** 64 de 64 conferências passaram (`measures/behavior.md`): a parte comum 46/46, os dados depois de
reiniciar 3/3, o cenário 09 13/13 e as reservas simultâneas 2/2.
**Nota:** bom

Para o dono da pousada, tudo o que foi pedido funciona pela API. Os quatro relatórios começam com
"Pousada Maré Alta,<nome>,<período>", e a ocupação e o financeiro continuam iguais depois do título. A taxa de
turismo lista as reservas pagas que começam no mês, na ordem do financeiro, com R$ 4,50 por hóspede por noite e o
total; num mês vazio, sai `total,0.00`. O relatório por plano traz o `STANDARD` e depois o `NON_REFUNDABLE`, mesmo
zerado, conta a não reembolsável cancelada e termina com o total. Um mês fora do formato é recusado com 400. Nada do
que já funcionava quebrou.

## Dúvidas

Fiquei entre `malfeito` e `omissão` na classe do pattern. Os quatro relatórios continuam escritos inteiros, o que
está perto da omissão ("cada um com a sua linha de título"). Mas a linha de título não foi copiada em cada um: ela
está num utilitário comum que os quatro chamam. A referência põe esse caso ("o 'esqueleto' é só uma função utilitária
de CSV e cada relatório ainda repete a sequência dos passos") no malfeito, e por isso dei `malfeito`.
