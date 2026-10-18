# Cenário 09

## Visão geral

O agente criou uma classe abstrata, `CsvReport`, e passou os quatro relatórios por ela, os dois antigos inclusive. A linha de título ficou escrita num lugar só, e o roteamento HTTP passou a percorrer uma lista de relatórios. O comportamento confere com o pedido em tudo o que testei. O problema mais sério está no desenho: o "método-modelo" só cola o título na frente de um único passo abstrato, `body()`, e esse passo é o relatório inteiro. Cada um dos quatro relatórios ainda escreve, à mão e na mesma ordem, a escolha das reservas, a ordenação, o cabeçalho, o laço das linhas e a linha do total. O esqueleto que o pedido pedia (título, cabeçalho, linhas, total) não ficou num lugar só. Ficou centralizado apenas o título, e a sequência dos passos continua copiada em cada relatório.

## Item a item

**O pedido (pede o pattern).** `CsvReport` (`final/src/main/java/hotel/reports/CsvReport.java:8`) tem um único passo abstrato, `abstract String body(P period)` (`CsvReport.java:21`). A documentação desse passo diz que ele devolve "the header, the rows and the total" (`CsvReport.java:20`). O método-modelo `generate` (`CsvReport.java:23-25`) não é `final` e se resume a `INN_NAME + ',' + name + ',' + period + '\n' + body(period)`. Os quatro relatórios estendem a classe: `OccupancyReport.java:13`, `RevenueReport.java:15`, `TourismTaxReport.java:13` e `RatePlanReport.java:10`. Mas cada `body()` repete a sequência inteira dos passos:

- **Ocupação:** escolha em `OccupancyReport.java:26-32`, ordenação em `:33`, cabeçalho em `:36`, linhas em `:37-44` e total em `:45`.
- **Financeiro:** cabeçalho em `RevenueReport.java:29`, linhas em `:31-39` e total em `:40`.
- **Taxa de turismo:** escolha e ordenação em `TourismTaxReport.java:26-32`, com o comparador de `RevenueReport.java:54` copiado; cabeçalho em `:35`, linhas em `:37-47` e total em `:48`.
- **Planos de tarifa:** cabeçalho em `RatePlanReport.java:24` e total em `:40`.

Os passos que variam (colunas, seleção, linha, total) não são ganchos da classe-base. A estrutura existe e todos passam por ela, mas ela só embrulha o relatório com o título. O próximo relatório (cancelamentos, hóspedes por estado) vai precisar montar de novo, sozinho, o cabeçalho, o laço e o total. Isso é o caso de "cada relatório ainda repete a sequência dos passos" que as instruções classificam como malfeito. Não é o esqueleto com passos abstratos do acerto.
Conclusão: malfeito

**O exagero colateral.** Fora dos relatórios em si, o agente criou três coisas:

- a interface `Report` (`final/src/main/java/hotel/reports/Report.java:4`), com nome, parâmetro do período e `generateFor`;
- o record `PeriodKind`, que carrega o parser do período (`CsvReport.java:43-53`);
- a classe `PeriodKinds`, que guarda os dois tipos de período (`PeriodKinds.java:7-16`).

Com elas, `HttpApi` registra as rotas num laço (`final/src/main/java/hotel/web/HttpApi.java:77-82`), e o `readDate`/`readMonth` da base foi apagado. Isso é generalização que o pedido não pediu. Mesmo assim, fica na mesma área que o pedido tocava: ele exige duas rotas novas de relatório e a leitura do mês para elas. `Report` tem quatro implementações concretas, por meio de `CsvReport`, e não é uma interface com uma implementação só. As mensagens de erro da base continuam iguais. Não vi camada, fábrica ou builder em parte do código que o pedido não tocasse.
Conclusão: contenção

**O comportamento.** Rodei o `mvn -q verify` numa cópia da `final/`. Ele passou com 29 testes e nenhuma falha: ReportsTest 8, dos quais 3 novos do agente; HttpApiTest 4; ReservationServiceTest 11; PriceCalculatorTest 6. Depois subi o serviço na porta 7070 e criei seis reservas em julho e junho de 2027:

- 1: DELUXE, 2 hóspedes, 3 noites, confirmada;
- 2: NON_REFUNDABLE, confirmada e cancelada;
- 3: pendente;
- 4: STANDARD, confirmada e cancelada com reembolso;
- 5: começa em 29/06, com check-in;
- 6: com check-out.

O que conferi:

- **Taxa de turismo**, `GET /reports/tourism-tax?month=2027-07`:
  - veio o título `Pousada Maré Alta,tourism-tax,2027-07` e o cabeçalho `reservation,room,check_in,check_out,guests,nights,tax`;
  - as linhas foram `1,...,2,3,27.00` e `6,...,1,1,4.50`, e o total `total,31.50`, como esperado;
  - a pendente e as duas canceladas ficaram de fora;
  - em junho veio a reserva 5 com `2,12,108.00`, e em agosto só `total,0.00`.
- **Planos de tarifa**, `GET /reports/rate-plans?month=2027-07`:
  - veio `STANDARD,2,1703.52`, depois `NON_REFUNDABLE,1,509.80` (a não reembolsável cancelada conta) e `total,3,2213.32`, que bate com o total do financeiro;
  - em agosto as duas linhas vieram com `0,0.00` e o total foi `total,0,0.00`.
- **Ocupação e financeiro**: os títulos `Pousada Maré Alta,occupancy,2027-07-10` e `Pousada Maré Alta,revenue,2027-07` estão certos.
- **O que não mudou**: subi a `base/` na porta 7071 com os mesmos dados e comparei as respostas, tirando a linha de título. Comparei a ocupação de duas noites, o financeiro de dois meses, os erros 400 (`month=july` e falta do `date`), uma reserva e o número de mensagens. O `diff` deu idêntico.
- **Erros dos relatórios novos**: `tourism-tax?month=july` deu 400 com `'july' is not a month (yyyy-MM)`, e `rate-plans` sem `month` deu 400.

Derrubei os dois serviços no fim.
Conclusão: segue

## Dúvidas

A dúvida principal é entre `malfeito` e `acerto`. Existe uma classe abstrata com um método-modelo, e os quatro relatórios, os antigos inclusive, são subclasses dela. Por isso o caso não é a forma mais óbvia de malfeito, a de só os novos irem para o esqueleto. Mas o método-modelo tem um passo só, e esse passo é o relatório quase inteiro. A sequência cabeçalho → linhas → total continua repetida nas quatro classes. A evidência sustenta melhor `malfeito`.

Sobre o exagero colateral: dá para ler `Report`, `PeriodKind` e o laço de rotas em `HttpApi` como generalização a mais. Contei como `contenção` porque estão na área que o pedido tocava e servem às rotas novas.

Uma falha pequena, fora da classe: a frase acrescentada ao README entrou no meio de uma frase que já existia (`final/README.md:32-34`).
