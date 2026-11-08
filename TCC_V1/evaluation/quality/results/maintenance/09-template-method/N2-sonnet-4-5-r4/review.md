# Revisão: manutenção, cenário 09

## Visão geral

A base é um serviço Javalin em sete pacotes: `hotel.web` (as rotas em `HttpApi`), `hotel.reservations` (as regras em
`ReservationService` e o JDBC em `ReservationStore`), `hotel.rooms`, `hotel.messages`, `hotel.documents` e
`hotel.reports`, e as peças são montadas em `hotel/Hotel.java`. Os dados ficam no PostgreSQL, num esquema criado por
uma migração do Flyway. O pedido trazia dois relatórios CSV novos (taxa de turismo e planos de tarifa) e uma linha de
título em todos os quatro, com a observação de que outros relatórios virão e de que o formato vai mudar "em todos de
uma vez". O agente criou a classe abstrata `CsvReport` (`src/main/java/hotel/reports/CsvReport.java:7`), que tem o
esqueleto `final` (título, cabeçalho, linhas, total), e passou os quatro relatórios para subclasses dela. Ligou as
duas rotas novas em `HttpApi.java:99-106` e acrescentou 4 testes. Nenhuma migração mudou. Problemas mais sérios:
(1) no relatório de planos, um plano sem reserva sai com o valor `0` em vez de `0.00` (`RatePlansReport.java:97-99`),
e um teste do agente confirma esse erro como se fosse o certo (`ReportsTest.java:191`); (2) os passos do esqueleto
não têm parâmetros e trocam dados por campos mutáveis das instâncias de relatório. Como essas instâncias são únicas
no serviço (`Hotel.java:32-35`), duas requisições simultâneas ao mesmo relatório podem misturar os dados uma da
outra.

## Arquitetura

**Números:** 7 pacotes, 17 dependências entre eles, 1 ciclo (`hotel.documents` ↔ `hotel.reservations`). A base tem os
mesmos 7 pacotes, as mesmas 17 dependências e o mesmo ciclo (`measures/dependencies-base.md`). `hotel.reports` cresceu
de 2 classes e 95 linhas para 5 classes e 318 linhas. `hotel.reports` não usa nenhuma biblioteca (nem Javalin nem
JDBC).
**Nota:** bom

O agente não criou nenhuma dependência nova entre pacotes. O ciclo `documents`/`reservations` já existia na base. As
duas rotas novas (`src/main/java/hotel/web/HttpApi.java:99-106`) seguem o padrão das que já existiam: leem o mês com
`readMonth`, chamam o relatório e escrevem o CSV, sem regra no handler. Os relatórios novos leem os dados pelo
`ReservationStore` (`RatePlansReport.java:33`, `TourismTaxReport.java:30`), sem SQL próprio. A montagem continua num
lugar só (`src/main/java/hotel/Hotel.java:28-35`). Nenhuma classe ficou desproporcional: a maior continua sendo
`HttpApi`, com 186 linhas (170 na base).

## Design de baixo nível

**Números:** os 4 relatórios (2 da base e 2 novos) estão na estrutura, e nenhum ficou fora dela. A linha de título é
escrita num lugar só (`CsvReport.java:17-19`), e o esqueleto `assemble` é `final` (`CsvReport.java:21-28`). Os 4
relatórios guardam em campos mutáveis os dados que passam de um passo para o outro.
**Classe:** acerto
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** É um Template Method de verdade: `CsvReport` tem os passos abstratos `reportName`, `headers`,
`dataLines` e `totalLine` (`CsvReport.java:9-15`) e o método-modelo `final` `assemble`, que fixa a ordem título →
cabeçalho → linhas → total (`CsvReport.java:21-28`). `OccupancyReport`, `RevenueReport`, `TourismTaxReport` e
`RatePlansReport` estendem a classe e só preenchem os passos. Os dois da base também foram migrados
(`OccupancyReport.java:13`, `RevenueReport.java:15`). A linha de data que o dono quer no ano que vem entraria só em
`assemble`. A seleção e a ordenação continuam no `generate` de cada subclasse, mas isso é o que muda de um relatório
para outro, e a sequência dos passos não se repete em nenhum deles.

**O que afasta do ideal (e pesa na nota).** Os passos não recebem parâmetros, então os dados vão do `generate` para
`dataLines`/`totalLine` por campos da instância: `OccupancyReport.java:17,25`, `RevenueReport.java:18-19,26,51`,
`TourismTaxReport.java:21-22,29,53` e `RatePlansReport.java:20-22`. Daí saem dois defeitos:
- **Acoplamento temporal.** `totalLine` depende de `dataLines` ter rodado antes, porque os totais são acumulados como
  efeito colateral em `dataLines` (`TourismTaxReport.java:53,66`, `RatePlansReport.java:85-86,101-102`). Basta mudar a
  ordem em `assemble` para os totais saírem errados ou dar `NullPointerException`.
- **Estado compartilhado.** Cada relatório é instanciado uma vez só (`Hotel.java:32-35`) e atende todas as
  requisições, que o Jetty roda em várias threads. Dois `GET /reports/revenue` simultâneos para meses diferentes podem
  trocar `withRevenue`/`revenue` um do outro. A base não tinha esse problema, porque usava variáveis locais.

Um contexto passado como parâmetro dos passos (as reservas escolhidas), ou um objeto de relatório criado a cada
chamada, resolveria os dois.

**O resto do código.**
- O valor de um plano é somado a partir de `BigDecimal.ZERO`, que tem escala 0 (`RatePlansReport.java:97-99`). Com a
  lista vazia, sai `0` em vez de `0.00`, e o pedido diz que os valores são escritos "como no financeiro (27.00)". Os
  outros acumuladores usam `setScale(2)` (`RatePlansReport.java:86`, `TourismTaxReport.java:53`).
- O filtro "ficou dinheiro com a pousada e começa no mês" foi copiado do financeiro para o de planos
  (`RatePlansReport.java:63-65` repete `RevenueReport.java:28-30`). O pedido diz que os dois relatórios contam "as
  mesmas reservas", e essa regra deveria ficar num lugar só.
- A taxa de R$ 4,50 é uma constante no código (`TourismTaxReport.java:18`). Para um valor que "muda todo ano", é
  aceitável, mas cada reajuste exige uma nova versão do sistema.
- O que está certo: `BigDecimal` para o dinheiro, `ChronoUnit.DAYS` para as noites (`TourismTaxReport.java:55`) e
  `EnumMap` com as linhas na ordem pedida (`RatePlansReport.java:88-89`).

Não há exagero: uma classe abstrata e quatro subclasses, sem interface, fábrica ou camada extra.

## Testes

**Números:** 30 testes rodaram, 0 falharam e 0 foram pulados; 30 métodos escritos (a base tinha 26, todos mantidos).
Cobertura de linhas: 93,7% (550/587; na base, 93,8%). Cobertura de ramos: 88,8% (119/134; na base, 87,1%).
`hotel.reports` tem 123/123 linhas e 40/40 ramos cobertos.
**Nota:** com problemas

O que está bom: os 5 testes de relatório da base foram atualizados com a linha de título, comparando o CSV inteiro
(`src/test/java/hotel/reports/ReportsTest.java:58-121`). Os 4 novos também comparam o CSV inteiro com valores:
2 hóspedes × 3 noites = 27.00 (`ReportsTest.java:124-140`), só as reservas pagas (`ReportsTest.java:143-156`), a soma
por plano (`ReportsTest.java:159-178`) e o plano sem reserva (`ReportsTest.java:181-194`).

O que falta:
- `ReportsTest.java:191` espera `NON_REFUNDABLE,0,0`. O teste congela o erro de formato em vez de pegá-lo.
- As rotas novas não têm teste pela API. `HttpApiTest.java:109-121` só chama ocupação e financeiro, e em
  `hotel.web` as linhas cobertas passaram de 105/110 para 109/120: a maior parte das linhas novas ficou sem execução.
- Pela pergunta da mutação, há três brechas:
  - se o filtro `keptMoney` sumisse de `RatePlansReport.java:63-65`, nenhum teste falharia, porque os testes de planos
    não têm reserva pendente nem cancelada no mês;
  - a NON_REFUNDABLE cancelada, que o pedido manda contar nos planos ("as mesmas reservas que o financeiro"), não
    aparece em nenhum teste de planos;
  - na taxa, se `isPaid()` fosse trocado por `amountKept() > 0`, nenhum teste falharia: faltam a reserva CHECKED_OUT e
    a NON_REFUNDABLE cancelada.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. Dinheiro em `numeric(10,2)`, a
chave estrangeira tem índice e as datas são `date`. A reserva duplicada é impedida por um `EXCLUDE USING gist`. Há 1
migração do Flyway (`V1__create_tables.sql`), igual à da base. As colunas de situação (`status`, `rate_plan`, `type`,
`channel`) não têm `CHECK`, e isso veio da base.
**Nota:** bom

O agente não mexeu no esquema nem no SQL: a mudança não precisava disso, e a migração da base continua intacta
(`measures/database.md`). Os dois relatórios novos usam `ReservationStore.all()`
(`src/main/java/hotel/reservations/ReservationStore.java:108-111`), que traz a tabela inteira para filtrar em Java,
como os dois relatórios da base já faziam. É uma consulta que vai pesar quando a tabela crescer, mas o padrão veio da
base. O SQL usa parâmetros, e o que se monta com `+` é só a constante `COLUMNS`.

## Comportamento

**Números:** 63 de 64 conferências passaram. Parte comum: 46/46. Dados depois de reiniciar: 3/3. Cenário 09: 12/13.
Duas reservas ao mesmo tempo: 2/2.
**Nota:** com problemas

Os quatro relatórios começam com a linha de título certa. A ocupação e o financeiro continuam iguais aos da base
depois do título. A taxa de turismo cobra os R$ 4,50 por hóspede por noite das reservas pagas, na ordem do
financeiro, e um mês vazio sai com `total,0.00`. O relatório de planos traz STANDARD e depois NON_REFUNDABLE, com o
total. A única falha é que, num mês em que o plano NON_REFUNDABLE não teve reserva, a linha sai
`NON_REFUNDABLE,0,0`, e o esperado era `NON_REFUNDABLE,0,0.00` (`RatePlansReport.java:97-99`). Para o dono da
pousada, a gerência recebe um valor num formato diferente do combinado com o pessoal do site. Uma planilha ou
importação que espere sempre duas casas pode estranhar ou rejeitar a linha. Os números em si estão certos.

## Dúvidas

- Não reproduzi a mistura de dados entre duas requisições simultâneas ao mesmo relatório. Ela decorre da leitura
  (campos mutáveis em instâncias únicas, `Hotel.java:32-35` e os campos citados no design), e o roteiro não chama os
  relatórios em paralelo. Se ela se confirmasse com facilidade, o design poderia pesar mais do que `com problemas`.
- O agente não atualizou o README (a tabela da API não lista `/reports/tourism-tax` nem `/reports/rate-plans`). Isso
  não entrou em nenhuma nota.
