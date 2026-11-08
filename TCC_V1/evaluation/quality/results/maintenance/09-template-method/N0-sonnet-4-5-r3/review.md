# Revisão: manutenção, cenário 09

## Visão geral

O agente manteve a organização da base: `hotel.web.HttpApi` (Javalin) recebe a requisição, lê o mês ou a data e chama
um relatório de `hotel.reports`. O relatório lê as reservas por `ReservationStore.all()` (JDBC, PostgreSQL, esquema
criado pelo Flyway) e devolve o CSV. A montagem de tudo fica em `hotel.Hotel`. A mudança acrescentou
`TourismTaxReport`, `RatePlansReport` e um auxiliar `ReportFormat`, que escreve a linha de título e junta as partes.
Também passou `OccupancyReport` e `RevenueReport` a usar esse auxiliar, ligou as duas rotas novas e escreveu 4 testes. O
banco não mudou.

Pela API, tudo funciona: 64 de 64 conferências passaram, e os 30 testes passam. O ponto mais fraco é o design.
`ReportFormat` não é um esqueleto. Cada um dos quatro relatórios repete a sequência dos passos, e a seleção, o total e o
cálculo da taxa aparecem duplicados.

## Arquitetura

**Números:** 7 pacotes e 17 dependências entre eles, iguais aos da base (`measures/dependencies.md` e
`measures/dependencies-base.md`). Há 1 ciclo, `hotel.documents` ↔ `hotel.reservations`, e ele já existia na base.
`hotel.reports` cresceu de 2 para 5 classes (95 → 280 linhas), sem nenhuma dependência nova. `hotel.reports` não usa
Javalin nem JDBC.
**Nota:** bom

As rotas novas são finas, do mesmo jeito que as da base: leem o mês, chamam `generate` e escrevem `text/csv`
(`src/main/java/hotel/web/HttpApi.java:99-106`). Os relatórios novos ficam no pacote dos relatórios e só dependem de
`ReservationStore` e de `Reservation` (`src/main/java/hotel/reports/TourismTaxReport.java:3-4`,
`src/main/java/hotel/reports/RatePlansReport.java:3-5`). A montagem continua num lugar só
(`src/main/java/hotel/Hotel.java:29-35`). O agente não criou nenhum ciclo novo e não pôs SQL nem regra nos handlers. Um
custo pequeno: cada relatório é uma classe concreta sem tipo comum, e por isso cada relatório novo exige um campo e um
parâmetro a mais no construtor de `HttpApi` (`src/main/java/hotel/web/HttpApi.java:32-51`) e em `Hotel`. Com dois
relatórios a mais já pedidos, isso vai pesar, mas não é um defeito de camadas.

## Design de baixo nível

**Números:** 4 de 4 relatórios passam por `ReportFormat`, mas só a linha de título e o `StringBuilder` estão
centralizados. Os 4 repetem a sequência `new ReportFormat` → `appendHeader` → `appendRows` → `appendTotal` → `build`. A
seleção, a ordenação e o total ficam em cada relatório.
**Classe:** malfeito
**Exagero:** contenção
**Nota:** com problemas

O pattern pedido é o Template Method: o esqueleto (título, cabeçalho, linhas, total) num lugar só, com cada relatório
preenchendo os passos que mudam.

- **O que aproxima:** a linha de título está num lugar só (`src/main/java/hotel/reports/ReportFormat.java:21`), e os
  quatro relatórios usam esse auxiliar, os dois da base também (`OccupancyReport.java:33-46`, `RevenueReport.java:34-54`).
  Um rodapé com a data poderia entrar em `build()` (`ReportFormat.java:36-38`) e valeria para todos.
- **O que afasta:** `ReportFormat` não controla a ordem dos passos. É um acumulador de texto com métodos públicos, e cada
  relatório chama os passos na mão, na mesma sequência (`OccupancyReport.java:33-46`, `RevenueReport.java:34-54`,
  `TourismTaxReport.java:37-66`, `RatePlansReport.java:39-59`). Nada impede um relatório de chamar `appendTotal` antes
  de `appendHeader`, ou de esquecer o cabeçalho. Um relatório novo (cancelamentos, hóspedes por estado) vai copiar a
  sequência inteira de novo. `appendRows(Consumer<StringBuilder>)` só entrega o `StringBuilder` interno ao chamador
  (`ReportFormat.java:28-30`), então a "estrutura" não sabe o que é uma linha.
- **A conclusão:** a referência descreve esse caso como malfeito: "o esqueleto é só uma função utilitária de CSV e cada
  relatório ainda repete a sequência dos passos". Para ser acerto ou alternativa equivalente, um método-modelo (ou um
  escritor genérico) teria de receber o nome, o período, as colunas, a seleção, a linha e o total, e fazer a sequência
  sozinho.

O exagero é contenção: não há interface com uma implementação só, fábrica ou camada que apenas repassa fora da parte do
pattern.

O resto do código mudado tem defeitos localizados:

- **A taxa calculada duas vezes:** a mesma conta da taxa (`4.50 × hóspedes × noites`) aparece duas vezes, uma para a
  linha e outra para o total (`src/main/java/hotel/reports/TourismTaxReport.java:42-45` e `:58-61`). Mudar uma e
  esquecer a outra põe o total em desacordo com as linhas.
- **O laço do financeiro partido:** em `RevenueReport` o laço da base foi dividido em dois, um dentro do lambda para as
  linhas e outro fora para o total (`RevenueReport.java:38-51`). O motivo é que o lambda não pode alterar `revenue`;
  isso é um efeito colateral do formato escolhido para `ReportFormat`.
- **O filtro do financeiro copiado:** o filtro "dinheiro que ficou + começa no mês" foi copiado do financeiro para o de
  planos (`RevenueReport.java:25-31` e `RatePlansReport.java:31-37`). O texto diz que os dois contam "as mesmas
  reservas", mas nada no código garante isso.
- **Os planos escritos à mão:** `RatePlansReport` monta um `EnumMap` com todos os planos (`:26-29`) e soma o total
  percorrendo o mapa (`:53-56`), mas escreve as linhas à mão, só para `STANDARD` e `NON_REFUNDABLE` (`:45-50`). Um plano
  novo entraria no total e não teria linha. Além disso, `totalReservations` e `totalAmount` são declarados antes do
  lambda e só usados depois dele (`:42-43`).
- **Campos mortos:** os campos `reportName` e `period` de `ReportFormat` são guardados e nunca lidos
  (`ReportFormat.java:13-14`, `:18-19`).
- **A taxa no código:** a taxa é uma constante `BigDecimal` (`TourismTaxReport.java:18`), e mudar o valor todo ano exige
  publicar o sistema de novo. É aceitável, mas é o ponto que o texto avisa que muda.

Os tipos estão certos: `BigDecimal` para dinheiro, `YearMonth` e `LocalDate` para os períodos, e o `enum` `RatePlan`.

## Testes

**Números:** 30 testes rodaram, com 0 falhas, 0 erros e 0 pulados; há 30 métodos escritos. A base tinha 26, e o agente
acrescentou 4. A cobertura é de 93,9% das linhas (568/605) e 89,4% dos ramos (127/142); na base, era 93,8% e 87,1%.
`hotel.reports` está 100% coberto: 141/141 linhas e 48/48 ramos (`measures/tests.md`).
**Nota:** com problemas

**O que está bem:** os testes novos conferem o CSV inteiro, com valores.

- O teste da taxa de turismo do mês cobre as duas reservas pagas e deixa fora a não paga e a cancelada com devolução
  (`src/test/java/hotel/reports/ReportsTest.java:128-143`).
- Há o exemplo do texto, 2 hóspedes × 3 noites = 27.00 (`ReportsTest.java:145-158`).
- Há o relatório de planos com o NON_REFUNDABLE vazio (`:160-175`) e com o STANDARD vazio (`:177-193`).
- Os testes da base foram atualizados só para acrescentar a linha de título (`:67`, `:80`, `:93`, `:110`, `:122`).
  Nenhum foi apagado ou enfraquecido.

**O que fica sem rede:**

- **A reserva NON_REFUNDABLE cancelada nos planos:** o texto diz que o relatório de planos conta "as mesmas reservas que
  o relatório financeiro". Nenhum teste de planos tem uma NON_REFUNDABLE cancelada. Se alguém trocasse
  `amountKept().signum() > 0` por `isPaid()` em `RatePlansReport.java:32`, os quatro testes novos continuariam passando,
  e a NON_REFUNDABLE cancelada sumiria do relatório.
- **As rotas novas:** nada testa `/reports/tourism-tax` nem `/reports/rate-plans` pela API. `HttpApiTest` só chama
  ocupação e financeiro (`src/test/java/hotel/web/HttpApiTest.java:113-120`). Um erro no caminho da rota ou na ligação
  em `Hotel` passaria no `mvn verify`.
- **O check-out na taxa:** nenhum teste da taxa usa uma reserva CHECKED_OUT. Também não há duas reservas com a mesma data
  de entrada para provar o desempate por id na ordem.

## Banco

**Números:** 3 tabelas, todas com chave primária. Há 1 migração Flyway (`V1__create_tables.sql`), igual à da base, e o
agente não acrescentou nenhuma. O dinheiro está todo em `numeric(10,2)`. Nenhuma chave estrangeira está sem índice, e
nenhuma data está em texto. Há uma restrição `EXCLUDE` contra duas reservas do mesmo quarto na mesma noite. Duas
conferências acusaram algo: as colunas `status`, `rate_plan`, `type` e `channel` estão sem CHECK, e 4 SQL são montados
com `+`, só com constantes (`measures/database.md`).
**Nota:** bom

A mudança não precisava mexer no banco, e o agente não mexeu: nenhuma migração foi editada ou criada, e
`ReservationStore` está igual à base. Tudo o que as conferências acusaram veio da base:

- as colunas de situação e de tipo sem CHECK;
- o SQL montado com `+`, que só concatena a constante `COLUMNS` e usa parâmetros `?`
  (`src/main/java/hotel/reservations/ReservationStore.java:111`).

O único efeito da mudança no banco é de carga. Os dois relatórios novos também chamam `ReservationStore.all()`, que
traz a tabela inteira para filtrar o mês em Java (`ReservationStore.java:108-120`; uso em `TourismTaxReport.java:28` e
`RatePlansReport.java:31`). Os dois relatórios da base já faziam isso, e numa pousada pequena não pesa.

## Comportamento

**Números:** 64 de 64 conferências passaram, 0 falharam (`measures/behavior.md`):

| parte | passaram |
|---|---|
| a parte comum | 46 de 46 |
| os dados depois de reiniciar | 3 de 3 |
| os relatórios do cenário 09 | 13 de 13 |
| duas reservas ao mesmo tempo | 2 de 2 |

**Nota:** bom

Pela API, o dono da pousada recebe o que pediu. Os quatro relatórios começam com "Pousada Maré Alta,<nome>,<período>",
e a ocupação e o financeiro continuam iguais depois do título. A taxa de turismo lista as reservas pagas que começam no
mês, na ordem do financeiro, e soma o total; num mês vazio, sai `total,0.00`. O relatório de planos traz STANDARD e
depois NON_REFUNDABLE, mesmo com zero, e o total. Um mês fora do formato é recusado com 400. Todo o resto continua
funcionando, e as reservas simultâneas do mesmo quarto continuam recusadas.

## Dúvidas

- **A classe do design:** fica entre `malfeito` e `alternativa equivalente`. Os quatro relatórios passam pelo mesmo
  `ReportFormat`, e o título (e um futuro rodapé, se for posto em `build()`) muda em todos de uma vez. Mesmo assim, o
  objeto não recebe as partes que variam nem conduz a sequência: cada relatório chama os passos na mão. Por isso fiquei
  com `malfeito`, que é o caso que a referência descreve.
- **A NON_REFUNDABLE cancelada nos planos:** o roteiro diz que confere essa contagem, e pelo código
  (`RatePlansReport.java:32`, o mesmo filtro do financeiro) ela está certa. Os testes do agente não a conferem.
