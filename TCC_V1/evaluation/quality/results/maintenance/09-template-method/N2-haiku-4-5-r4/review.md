# Revisão: manutenção, cenário 09

## Visão geral

O sistema da base tem sete pacotes: `hotel` (a montagem em `Hotel.java`), `hotel.web` (as rotas do Javalin em
`HttpApi`), `hotel.reservations` (as regras em `ReservationService` e o JDBC em `ReservationStore`),
`hotel.rooms`, `hotel.messages`, `hotel.documents` e `hotel.reports`. Uma requisição de relatório entra por
`HttpApi`, que lê o parâmetro e chama o relatório. O relatório lê todas as reservas com `ReservationStore.all()`,
filtra em Java e devolve o CSV. Os dados ficam no PostgreSQL, com o esquema criado por uma migração do Flyway. O
agente mexeu só em `hotel.reports`, `HttpApi` e `Hotel`, além dos testes. Ele criou a classe abstrata `Report`, com
o esqueleto `final` (título, cabeçalho, linhas, total), passou os dois relatórios antigos para ela e escreveu os
dois novos (`TourismTaxReport` e `RatePlansReport`) como subclasses. As 64 conferências do roteiro passam e os
32 testes passam. Não há nada sério. Os defeitos são de design dentro dos relatórios: cada objeto se recria a si
mesmo por chamada, o construtor público deixa o período em `null`, e o resultado de `data()` passa para
`total()` por campos mutáveis. O filtro do financeiro foi copiado no relatório por plano. O README não ganhou as
duas rotas novas (`final/README.md`, tabela "A API").

## Arquitetura

**Números:** 7 pacotes e 17 dependências entre eles, iguais aos da base (`measures/dependencies.md` e
`measures/dependencies-base.md`). Há 1 ciclo (`hotel.documents` ↔ `hotel.reservations`), que já existia na base.
`hotel.reports` passou de 2 para 5 classes (de 95 para 307 linhas) e continua sem Javalin e sem JDBC.
**Nota:** bom

- As rotas novas seguem o padrão das antigas: leem o mês, chamam o relatório e escrevem o CSV
  (`src/main/java/hotel/web/HttpApi.java:99-106`). Não há regra no handler.
- Os relatórios dependem só de `ReservationStore`, `Reservation`, `RatePlan` e `Rooms`
  (`src/main/java/hotel/reports/TourismTaxReport.java:3-4`, `RatePlansReport.java:3-5`), e a montagem continua
  num lugar só (`src/main/java/hotel/Hotel.java:28-35`).
- O ciclo `documents` ↔ `reservations` vem da base, e o agente não o piorou.
- Um ponto pequeno: o construtor de `HttpApi` ganha um parâmetro por relatório (`HttpApi.java:37-44`). Com os
  dois relatórios que o contador já anunciou, serão nove parâmetros. Não chega a ser defeito neste tamanho.

## Design de baixo nível

**Números:** os 4 relatórios (occupancy, revenue, tourism-tax, rate-plans) estão na estrutura e nenhum ficou de
fora. Há 1 esqueleto (`Report.generate()`, `final`) e 4 passos abstratos. A linha de título é montada em um só
formato nos quatro, com a constante `POUSADA_NAME` da classe-mãe.
**Classe:** acerto
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** O agente fez o Template Method como a referência descreve:

- `src/main/java/hotel/reports/Report.java:10-21` é o método-modelo `final`. Ele escreve o título, o cabeçalho,
  as linhas de `data()` e o total, cada um seguido de `\n`.
- `Report.java:23-26` são os passos abstratos.
- Os dois relatórios que já existiam viraram subclasses e perderam o `StringBuilder` próprio
  (`OccupancyReport.java:13`, `RevenueReport.java:15`). Os dois novos também são subclasses
  (`TourismTaxReport.java:13`, `RatePlansReport.java:12`).
- Ninguém pergunta "qual relatório é" nem monta CSV à mão fora do esqueleto.
- A linha de data que virá no ano que vem entra num lugar só (`Report.java:19-20`).

**Exagero.** Não há estrutura sem necessidade: nenhuma interface extra, nenhuma fábrica nem registro de
relatórios.

**O design do resto (o que pesa na nota):**

- **Um objeto que se recria a si mesmo, com o período nulo.** Cada relatório tem um construtor público que grava
  o período como `null` (`OccupancyReport.java:23`, `RevenueReport.java:23`, `TourismTaxReport.java:23`,
  `RatePlansReport.java:21`). O `generate(período)` cria uma segunda instância pelo construtor privado e chama o
  esqueleto nela (`RatePlansReport.java:29-31`, igual nos outros). O `generate()` sem argumento herdado de
  `Report` é `public`, então qualquer um pode chamá-lo na instância "sem período":
  - `new OccupancyReport(rooms, store).generate()` cai em `NullPointerException` (`isAfter(null)` em
    `OccupancyReport.java:50`);
  - nos relatórios mensais, sai um CSV vazio com o título `...,revenue,null`.

  Passar o período como parâmetro dos passos, ou ter um objeto de execução separado do objeto injetado, evitaria
  esse caminho.
- **Acoplamento temporal por campos mutáveis.** `data()` grava o resultado em campos (`occupying`, `revenue`,
  `totalTax`, `totalReservations`/`totalAmount`) que `total()` lê depois (`OccupancyReport.java:18,48,66`,
  `RevenueReport.java:19,57,68`, `RatePlansReport.java:16-17,45-46,67`). `total()` só funciona se `data()` rodou
  antes na mesma instância. O esqueleto garante essa ordem, mas o contrato não está escrito em lugar nenhum.
- **A regra do financeiro duplicada.** O pedido diz que o relatório por plano conta "as mesmas reservas que o
  relatório financeiro conta". Em vez de reaproveitar o filtro, o agente copiou a condição: "começa no mês e a
  pousada ficou com dinheiro" está em `RevenueReport.java:49-51` e de novo em `RatePlansReport.java:52-54`. A
  ordenação do financeiro também foi copiada (`RevenueReport.java:55` e `TourismTaxReport.java:54`). Se a regra
  do financeiro mudar, alguém vai ter de lembrar de mudar nos dois lugares.
- **Uma leitura da tabela por plano.** `RatePlansReport.java:48-51` lê a tabela inteira (`store.all()`) dentro do
  laço dos planos, ou seja, uma vez por plano.
- **Pontos menores:**
  - a taxa fica numa constante (`TourismTaxReport.java:15`), e o pedido avisa que "a prefeitura muda o valor da
    taxa todo ano"; num sistema deste tamanho isso é aceitável;
  - `ReportsTest.java:20` importa `DisplayName` sem usar.
- **O que está certo:** `BigDecimal` com 2 casas, o arredondamento explícito (`TourismTaxReport.java:60-62`) e o
  `enum` `RatePlan` percorrido em ordem (`RatePlansReport.java:48`).

## Testes

**Números:** 32 testes rodaram, com 0 falhas e 0 pulados, e há 32 métodos escritos. A base tinha 26 e o agente
acrescentou 6 (4 em `ReportsTest`, 2 em `HttpApiTest`). Cobertura de linhas: 94,7% (base: 93,8%). Cobertura de
ramos: 89,1% (base: 87,1%). `hotel.reports` está em 126 de 126 linhas e 44 de 44 ramos (`measures/tests.md`).
**Nota:** com problemas

**O que está bem:**

- O agente atualizou os 5 testes de relatório da base com a linha de título e o CSV inteiro
  (`src/test/java/hotel/reports/ReportsTest.java:61-120`). Nenhum foi apagado nem enfraquecido.
- Os dois relatórios novos têm testes que comparam o CSV inteiro: a taxa em abril, com 1 hóspede × 2 noites dando
  9,00 por reserva e 18,00 no total; a taxa de um mês vazio; os planos com o `NON_REFUNDABLE` zerado; os planos
  de maio (`ReportsTest.java:123-175`).
- Pela API, `HttpApiTest.java:128-154` confere as rotas novas, o `text/csv` e os valores. A conta 2 × 2 = 18,00
  sai dali.

**A pergunta da mutação nas regras novas:**

- Se a taxa somasse hóspedes e noites em vez de multiplicar, `ReportsTest` falharia (1 + 2 ≠ 1 × 2). Se a conta
  ignorasse os hóspedes, `HttpApiTest` falharia (9,00 ≠ 18,00). Essas duas mudanças estão cobertas.
- A regra que o pedido destaca para os planos, "as mesmas reservas que o relatório financeiro conta" (o que
  inclui a `NON_REFUNDABLE` cancelada), ficou sem rede. `ratePlansCountsCorrectly` (`ReportsTest.java:150-163`)
  confirma a reserva de maio mas não a cancela. Se `RatePlansReport` filtrasse por `isPaid()` em vez de
  `amountKept() > 0`, nenhum teste falharia.
- Na taxa, nenhum teste distingue "paga" de "a pousada ficou com dinheiro". O cenário de abril não tem uma
  `NON_REFUNDABLE` cancelada nem um `CHECKED_OUT`, então a troca de filtro passaria despercebida.
- Os testes novos de `HttpApiTest` usam `contains` (`HttpApiTest.java:134-152`), não o corpo inteiro. A ordem e a
  linha de título como *primeira* linha só são conferidas em `ReportsTest`.

## Banco

**Números:** 3 tabelas, todas com chave primária. O dinheiro está todo em `numeric(10,2)`. Não há chave
estrangeira sem índice nem data como texto. A sobreposição de reservas está protegida por `EXCLUDE USING gist`.
A única migração (`V1__create_tables.sql`) está igual à da base. As 4 colunas de situação e tipo são `text` sem
`CHECK` (`measures/database.md`).
**Nota:** bom

- O pedido não mexe no banco e o agente não mexeu: nenhuma migração editada ou nova, nenhum SQL novo.
- O SQL montado com `+` em `ReservationStore.java:33,83,98,111` só concatena constantes (`COLUMNS`) e vem da
  base. O mesmo vale para as colunas de situação sem `CHECK`.
- O único ponto do agente é de carga. Os dois relatórios novos seguem o padrão da base de trazer a tabela
  `reservations` inteira (`ReservationStore.java:108-111`) e filtrar em Java: `TourismTaxReport.java:48`, e
  `RatePlansReport.java:51`, que lê a tabela uma vez por plano, dentro do laço. Hoje isso não pesa, mas cresce
  com a tabela.

## Comportamento

**Números:** 64 de 64 conferências passaram (`measures/behavior.md`):

| parte | passaram |
|---|---|
| a parte comum | 46 de 46 |
| os dados depois de reiniciar | 3 de 3 |
| o cenário 09 | 13 de 13 |
| as reservas simultâneas | 2 de 2 |

**Nota:** bom

Para o dono da pousada, tudo o que ele pediu funciona pela API:

- os quatro relatórios começam com "Pousada Maré Alta,<nome>,<período>";
- a ocupação e o financeiro continuam iguais depois do título;
- a taxa de turismo cobra R$ 4,50 por hóspede por noite das reservas pagas do mês, na ordem do financeiro, com o
  total;
- o relatório por plano mostra STANDARD e NON_REFUNDABLE, mesmo zerados, com a quantidade, o valor e o total;
- um mês fora do formato é recusado com 400.

Nada do que já funcionava quebrou.

## Dúvidas

- **A nota dos testes.** Ficou entre `bom` e `com problemas`. A cobertura de `hotel.reports` é total e os CSVs são
  conferidos inteiros. Dei `com problemas` porque a regra que o pedido destacou para os planos (contar as mesmas
  reservas do financeiro, inclusive a não reembolsável cancelada) não tem teste que a distinga.
- **A taxa numa constante.** `TourismTaxReport.java:15` é aceitável hoje. Não está claro se o dono esperava poder
  trocar o valor sem mexer no código.
