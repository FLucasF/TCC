# Revisão: manutenção, cenário 09

## Visão geral

O agente mexeu só no que o pedido tocava: criou `hotel.reports.CsvReport`, uma classe abstrata com o esqueleto do
relatório (título, cabeçalho, linhas, total), passou os dois relatórios da base (`OccupancyReport`, `RevenueReport`)
para subclasses dela, acrescentou `TourismTaxReport` e `RatePlansReport` do mesmo jeito, ligou as duas rotas novas em
`HttpApi` e montou tudo em `Hotel`. Uma requisição entra pelo handler do Javalin (`web/HttpApi.java:99-106`), que lê
o mês e chama `generate(month)` no relatório. O relatório lê as reservas por `ReservationStore.all()` (PostgreSQL) e
monta o CSV. O banco não mudou. Os 30 testes passam e o roteiro dá 64 de 64. O defeito mais sério é novo: para
passar o período aos passos do template, o agente guardou o mês (ou a noite) num campo mutável de objetos que o
servidor compartilha entre as requisições (`reports/TourismTaxReport.java:18,25`, e igual nos outros três). Com
pedidos simultâneos, um relatório sai com o título ou as reservas de outro mês. Reproduzi isso pela API: 178 de 240
respostas vieram com a reserva de outro mês. A base não tinha esse problema, porque recebia o período como parâmetro
e o guardava só numa variável local.

## Arquitetura

**Números:** 7 pacotes (os mesmos da base), 17 dependências entre eles (as mesmas da base) e 1 ciclo,
`hotel.documents` ↔ `hotel.reservations`, que já existia na base (`measures/dependencies-base.md`). `hotel.reports`
passou de 2 classes e 95 linhas para 6 classes e 367 linhas. `HttpApi` passou de 170 para 186 linhas.
**Nota:** bom

As rotas novas são finas: leem o mês com o `readMonth` que já existia, chamam o relatório e escrevem `text/csv`
(`web/HttpApi.java:99-106`). Os relatórios não usam Javalin nem JDBC (tabela de bibliotecas em
`measures/dependencies.md`) e leem os dados pelo `ReservationStore` da base. A montagem continua num lugar só
(`Hotel.java:28-35`). Nenhuma dependência nova entre pacotes, e o único ciclo veio da base. Um ponto menor: o
construtor de `HttpApi` agora recebe sete parâmetros, um por relatório (`web/HttpApi.java:37-44`). O próximo
relatório vai aumentar essa lista, mas por enquanto isso não pesa.

## Design de baixo nível

**Números:** 4 de 4 relatórios passam pelo esqueleto (`CsvReport`), os 2 da base inclusive. 0 relatórios montam o
CSV à mão. A linha de título existe em um lugar só (`reports/CsvReport.java:29-31`). Os 4 relatórios guardam o
período num campo mutável de um objeto compartilhado.
**Classe:** acerto
**Exagero:** contenção
**Nota:** ruim

**O pattern.** É o Template Method que a referência descreve. `CsvReport` é abstrata, com o método-modelo
`generate()` marcado `final` (`reports/CsvReport.java:20-27`) e cinco passos abstratos: `reportName`, `period`,
`headerLine`, `dataLines` e `totalLine` (`reports/CsvReport.java:10-18`). Os quatro relatórios são subclasses e só
preenchem os passos (`OccupancyReport.java:13`, `RevenueReport.java:15`, `TourismTaxReport.java:13`,
`RatePlansReport.java:14`). Uma linha de rodapé com a data, que o dono já anunciou, entraria em um lugar só.

**O exagero.** Nenhuma interface, fábrica ou camada a mais. A classe base é o que o pedido exige.

**A nota: o design do resto.**
- **Estado mutável compartilhado (sério).** Os passos do template não recebem o período. Por isso cada relatório
  guarda o período num campo (`OccupancyReport.java:17,25`, `RevenueReport.java:18,25`,
  `TourismTaxReport.java:18,25`, `RatePlansReport.java:17,24`) e depois chama `generate()`. Só existe uma instância
  de cada relatório (`Hotel.java:32-35`), e o Javalin atende as requisições em várias threads. Duas requisições
  simultâneas sobrescrevem o campo uma da outra. Subi o serviço, criei uma reserva paga por mês e fiz 12 pedidos
  simultâneos de `/reports/tourism-tax`, um para cada mês, em 20 rodadas. Resultado: 23 de 240 respostas vieram com o
  título de outro mês, e 178 de 240 com a reserva de outro mês. Em um caso, o pedido de 2027-01 voltou com o título de
  2027-03 e a reserva de junho. Isso é dado errado num relatório que vai para a prefeitura e para o contador. A base
  (`base/.../RevenueReport.java`) recebia o período como parâmetro e não tinha esse defeito.
- **O método-modelo público sem o período.** `generate()` é `public` (`CsvReport.java:20`) e aparece nas quatro
  classes públicas. Quem chamar sem antes chamar `generate(month)` recebe um `NullPointerException` em `period()`
  (`OccupancyReport.java:35-37`).
- **Duas leituras por relatório.** `dataLines` e `totalLine` refazem a seleção cada um por sua conta
  (`TourismTaxReport.java:46,67`, `RevenueReport.java:46,62`, `OccupancyReport.java:46,61`,
  `RatePlansReport.java:45,54`). Assim, cada relatório lê a tabela inteira duas vezes. Se uma reserva mudar entre as
  duas leituras, o total pode não bater com as linhas.
- **Duplicação.** O cálculo da taxa (noites × hóspedes × 4,50) está copiado em `TourismTaxReport.java:49-52` e
  `:68-71`.
- **O que está certo.** `BigDecimal` para a taxa e para o dinheiro, com a taxa numa constante
  (`TourismTaxReport.java:15`). Os planos saem numa ordem fixa (`RatePlansReport.java:47-48`). `isPaid()` da base é
  reaproveitado para escolher as reservas pagas (`TourismTaxReport.java:81`).

## Testes

**Números:** 30 testes rodaram, 0 falhas, 0 erros, 0 pulados. São 30 métodos de teste, contra 26 na base: 4 novos e
os 5 de relatório da base ajustados para a linha de título. Cobertura de linhas: 94,0% (577 de 614), contra 93,8% na
base. Cobertura de ramos: 89,1% (123 de 138), contra 87,1%. `hotel.reports` está com 150 de 150 linhas e 44 de 44
ramos.
**Nota:** com problemas

Os testes da base continuam lá. Os cinco de relatório só ganharam a linha de título, com o CSV inteiro conferido
(`src/test/java/hotel/reports/ReportsTest.java:59-121`). Nenhum foi apagado ou enfraquecido. Os novos conferem o CSV
inteiro com valores:
- taxa de abril, com a pendente e a cancelada fora e a `CHECKED_IN` dentro (`ReportsTest.java:125-135`);
- taxa de maio só com uma cancelada (`:138-150`);
- planos com `NON_REFUNDABLE` zerado (`:153-163`);
- a não reembolsável cancelada contando no plano (`:166-179`).

O que ficou sem rede:
- **Hóspedes na taxa.** Toda reserva do teste tem 1 hóspede (`ReportsTest.java:35`). Se alguém apagasse o
  `.multiply(guests)` (`TourismTaxReport.java:51`), nenhum teste falharia. O exemplo do próprio pedido (2 hóspedes,
  3 noites, R$ 27,00) não está testado.
- **`CHECKED_OUT` na taxa.** Nenhum teste tem uma reserva `CHECKED_OUT` no relatório de taxa.
- **As rotas novas.** `HttpApiTest` não mudou (`src/test/java/hotel/web/HttpApiTest.java:109-121`), e
  `/reports/tourism-tax` e `/reports/rate-plans` não são chamadas em nenhum teste.
- **Concorrência.** Nenhum teste chama um relatório duas vezes ao mesmo tempo, que é onde está o defeito sério.

## Banco

**Números:** 3 tabelas, todas com chave primária. Dinheiro em `numeric(10,2)`. Nenhuma chave estrangeira sem índice e
nenhuma data em texto. A conferência acusou só as colunas de situação e de tipo sem CHECK (`status`, `rate_plan`,
`type`, `channel`), que vêm da base. 1 migração Flyway, `V1__create_tables.sql`, igual à da base. O `EXCLUDE` contra
reservas sobrepostas continua.
**Nota:** bom

O pedido não exigia mudança no banco, e o agente não mexeu no esquema, nas migrações nem no `ReservationStore`. Os
relatórios novos usam o mesmo `store.all()` da base (`reservations/ReservationStore.java:108-121`), com SQL constante
e try-with-resources. A tabela inteira vem para o Java e é filtrada lá, como a base já fazia. O agente piorou isso,
porque cada relatório agora chama `store.all()` duas vezes (ver o design). É um custo de carga, e não perde dado. As
colunas de texto sem CHECK vieram da base.

## Comportamento

**Números:** 64 de 64 conferências passaram:
- a parte comum: 46 de 46;
- os dados depois de reiniciar: 3 de 3;
- o cenário 09: 13 de 13;
- duas reservas ao mesmo tempo: 2 de 2.

**Nota:** bom

O roteiro confirma o que o dono pediu:
- os quatro relatórios começam com `Pousada Maré Alta,<nome>,<período>`;
- a ocupação e o financeiro continuam iguais depois do título;
- a taxa de turismo lista as pagas que começam no mês, na ordem do financeiro, e um mês vazio sai com total 0.00;
- os planos têm STANDARD e NON_REFUNDABLE, mesmo com 0;
- um mês fora do formato no relatório novo dá 400.

O roteiro não chama relatórios ao mesmo tempo. Fora dele, encontrei o defeito de concorrência descrito no design:
relatórios pedidos juntos podem sair com o mês trocado. Para o dono, isso quer dizer que um relatório da taxa ou do
financeiro, tirado enquanto outra pessoa tira outro, pode trazer números de outro mês. Esse defeito pesa na coluna do
design e não muda esta nota.

## Dúvidas

- A gravidade do defeito de concorrência depende de quantas pessoas pedem relatórios ao mesmo tempo, e isso a
  evidência não diz. Numa pousada pequena pode ser raro. Mesmo assim, ele é real, reproduzível pela API e novo em
  relação à base, e por isso a nota do design é `ruim` e não `com problemas`.
