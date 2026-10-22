# Cenário 09

## Visão geral

O agente separou o conteúdo de cada relatório do formato da saída. Cada um dos quatro relatórios (ocupação, financeiro, taxa de turismo e planos) implementa a interface `Report` e devolve um `ReportTable`, um registro só de dados com nome, período, cabeçalho, linhas e total. Um único `CsvReportWriter` escreve o CSV na ordem título, cabeçalho, linhas e total. Os quatro passam por esse escritor, inclusive os dois que já existiam, e a linha de título só aparece nele. É a composição que as instruções aceitam de antemão, e não uma classe abstrata com método-modelo. O que mais se aproxima de um problema é pequeno: cada `build()` repete a mesma sequência curta de escolher as reservas, montar as linhas, somar o total e montar a tabela. Só que é justamente ali que ficam as partes que variam, e nenhum relatório escreve uma linha de CSV sozinho. Pela leitura do código, o comportamento confere com o enunciado.

## Como foi implementado, e por que o julgamento

**O projeto.** Há três pacotes.

- `domain` traz o modelo e as regras:
  - `Reservation` guarda o estado e o dinheiro e calcula `amountKept`.
  - `Pricing` calcula as diárias, a alta temporada e o desconto.
  - `RatePlan` é um enum com o desconto e se o plano é reembolsável.
  - `ReservationService` tem a criação, as transições, a recusa de quarto ocupado e o envio do e-mail.
  - `MessageLog` é a lista de mensagens enviadas, atrás da interface `MessageSender`.
  - `Money` cuida do arredondamento meio para o par e dos formatos do dinheiro.
  - `Receipt` monta o comprovante.
- `reports` traz a interface `Report`, os quatro relatórios, o `ReportTable`, o `CsvReportWriter`, o `ReservationQueries` (as seleções comuns e a ordenação) e o `Periods` (a leitura de `date` e `month`, com a recusa 400).
- `web` traz o `App`, que registra as rotas Javalin, o `Json`, que converte para JSON, e o `RequestParser`.

O caminho de uma requisição é sempre o mesmo: a rota chama um método do `ReservationService`, que guarda as reservas numa lista em memória (`domain/ReservationService.java:17`). O `Main` lê `PORT` (7070 se não vier) e `TOURISM_TAX_PER_GUEST_NIGHT` (4,50 se não vier), monta o `App` e sobe o servidor (`Main.java:12-16`). Os testes ficam todos em `ApiTest`: são testes de API, que cobrem preço, ciclo de vida, erros, reembolso e os quatro relatórios com o CSV exato (`src/test/java/br/com/marealta/reservas/ApiTest.java:167-223`).

**A parte do pattern.**

- O `App` cria uma lista com os quatro relatórios (`web/App.java:37-41`) e registra as rotas num laço. Cada rota é `/reports/` mais o nome do relatório, e a resposta é `csv.write(report.build(ctx.queryParam(report.periodParam())))` (`web/App.java:59-63`). Não há `if` nem `switch` por relatório.
- O esqueleto mora todo em `CsvReportWriter.write`: título (`reports/CsvReportWriter.java:12`, com `INN_NAME`, o nome e o período), cabeçalho (`:13`), linhas (`:14`) e total (`:15`).
- Cada relatório preenche só o que muda: o nome, o parâmetro de período e um `build()` que escolhe e ordena as reservas e devolve cabeçalho, linhas e total.
  - Ocupação: `reports/OccupancyReport.java:28-41`.
  - Financeiro: `reports/RevenueReport.java:29-40`.
  - Taxa de turismo: `reports/TourismTaxReport.java:35-47`, com o valor da taxa recebido no construtor (`:15-18`).
  - Planos: `reports/RatePlanReport.java:31-46`.
- O financeiro e o de planos usam a mesma seleção, `ReservationQueries.keptMoneyStartingIn`, e a taxa usa `paidStartingIn`. As duas ordenam por entrada e depois por número da reserva (`reports/ReservationQueries.java:12-33`).

**O julgamento.** Não há classe abstrata com método-modelo: `Report` é uma interface sem esqueleto (`reports/Report.java:4-13`). Por isso a classe não é `acerto`. O que existe é um escritor genérico que recebe as partes que variam como objeto, o `ReportTable`, e todos os quatro relatórios passam por ele. É exatamente a composição descrita como `alternativa equivalente`.

Também não é `malfeito`. Nenhum relatório escreve título, cabeçalho ou total por conta própria. O escritor não é uma simples função de CSV que cada relatório chama em sequência: é ele quem decide a ordem das partes e escreve o título. A linha com a data, prometida para o ano que vem, entraria só em `CsvReportWriter.write`, e o comentário em `reports/ReportTable.java:5-8` deixa essa intenção clara. A semelhança entre os `build()` (filtrar, mapear linhas, somar) é o preenchimento de cada caso, não uma cópia do esqueleto.

**Exagero colateral.** `Report`, `ReportTable` e `ReservationQueries` fazem parte da solução da variação, ou evitam regra duplicada. Fora dela, o único candidato é `MessageSender`, uma interface com uma só implementação, `MessageLog` (`domain/MessageSender.java:4`, `domain/MessageLog.java:6`), que nem os testes substituem. É uma porta de um método, e o enunciado diz que os e-mails "por enquanto" não saem de verdade. Considerei que isso não chega a ser estrutura sem necessidade.

**Comportamento.** Conferi pela leitura do código e pelos testes do agente, como explico no item abaixo. As regras pedidas estão como o enunciado descreve.

## Item a item

**O pattern pedido.** Os quatro relatórios (`reports/OccupancyReport.java:10`, `reports/RevenueReport.java:11`, `reports/TourismTaxReport.java:11`, `reports/RatePlanReport.java:13`) devolvem um `ReportTable` (`reports/ReportTable.java:9-10`). Um único `CsvReportWriter` escreve título, cabeçalho, linhas e total (`reports/CsvReportWriter.java:10-17`), e o `App` passa todos por ele num laço, sem distinguir casos (`web/App.java:59-63`). A linha de título está só em `CsvReportWriter.java:12`. É composição, um escritor genérico que recebe as partes que variam, e não herança com método-modelo. As instruções aceitam essa forma desde que os quatro passem pelo escritor, e passam.
Conclusão: alternativa equivalente

**O exagero colateral.** Não encontrei fábrica, builder nem camada que só repassa. `MessageSender` (`domain/MessageSender.java:4`) tem uma implementação só, mas é uma porta mínima para o envio que o enunciado diz que hoje é provisório. `ReservationQueries` evita que o financeiro e o de planos divirjam no critério de seleção.
Conclusão: contenção

**O comportamento.** Conferi por leitura do código, sem subir o serviço, sem rodar o Maven e sem chamar a API, e comparei com as asserções de `ApiTest.java:167-223`.

- **Título.** É `Pousada Maré Alta,<nome>,<período>` (`CsvReportWriter.java:12`). Os nomes são `occupancy`, `revenue`, `tourism-tax` e `rate-plans`, e o período é o `date` ou o `month` validado e reescrito em ISO, igual ao da chamada.
- **Taxa de turismo.**
  - O cálculo é 4,50 × hóspedes × noites, arredondado (`TourismTaxReport.java:30-32`; o valor padrão está em `App.java:27`).
  - Entram só as reservas pagas, isto é, CONFIRMED, CHECKED_IN ou CHECKED_OUT (`domain/ReservationStatus.java:6-8`), que começam no mês, na ordem do financeiro (`ReservationQueries.java:19-21`).
  - As colunas estão certas (`TourismTaxReport.java:45`) e o total vem no fim.
  - Os casos 2 × 3 = 27,00 e o total 31,50 estão no teste.
- **Planos.**
  - Há uma linha por `RatePlan.values()`, na ordem STANDARD e depois NON_REFUNDABLE, mesmo com zero reservas (`RatePlanReport.java:37-43`).
  - A seleção é a mesma do financeiro, então a não reembolsável cancelada conta (`ReservationQueries.java:24-26` e `domain/Reservation.java:55-57`).
  - O total traz a quantidade e o valor (`RatePlanReport.java:45`).
- **Ocupação.**
  - Entram as reservas pagas que ocupam a noite, com a entrada incluída e a saída excluída (`Reservation.java:60-62`).
  - A ordem é pelo número do quarto, e o total é quartos ocupados sobre 5 (`OccupancyReport.java:30-40`).
- **Financeiro.**
  - Entram as reservas com dinheiro retido maior que zero que começam no mês, em ordem de entrada e depois de número da reserva.
  - As colunas e o total estão certos (`RevenueReport.java:29-39`).
- **Formato e recusas.**
  - O tipo da resposta é `text/csv; charset=utf-8` (`App.java:61`).
  - Mês ou data fora do formato e parâmetro faltando dão 400 com INVALID_REQUEST (`reports/Periods.java:14-39`).

Não encontrei divergência.
Conclusão: segue

## Dúvidas

1. **Alternativa equivalente ou malfeito.** Cada `build()` repete a sequência de escolher, ordenar, mapear as linhas, somar e montar a tabela. Dá para ver nisso a "sequência dos passos" repetida em cada relatório. Mesmo assim, o esqueleto de saída (título, cabeçalho, linhas, total) está num lugar só, e os relatórios só fornecem dados. Por isso a evidência sustenta melhor `alternativa equivalente`.
2. **`MessageSender`.** Pela letra das instruções, é uma interface com uma só implementação que nada substitui hoje. Não contei como exagero porque é uma porta de um método só e o enunciado anuncia que o envio é provisório ("por enquanto").
