# Revisão: manutenção, cenário 09

## Visão geral

A base é um serviço Javalin em sete pacotes: `hotel.web` (as rotas, `HttpApi`), `hotel.reservations` (as regras e o
`ReservationStore`, com JDBC), `hotel.rooms`, `hotel.messages`, `hotel.documents`, `hotel.reports` e a montagem em
`hotel.Hotel`. Os dados ficam no PostgreSQL, num esquema criado por uma migração do Flyway. Uma requisição de relatório
entra em `HttpApi` (`web/HttpApi.java:91-106`), que lê o período e chama `generate` do relatório. O relatório lê as
reservas por `ReservationStore.all()` e monta o CSV. O agente mexeu só em `hotel.reports`, em `HttpApi` e em `Hotel`.
Ele criou uma classe abstrata `CsvReport<P>` com o método-modelo `final generate` (título, cabeçalho, linhas, total) e
passou para ela os dois relatórios antigos e os dois novos (taxa de turismo e planos de tarifa). Ligou as duas rotas
novas e ajustou os testes antigos com a linha de título. O roteiro do comportamento passou inteiro (64 de 64). O ponto
mais fraco são os testes novos: todos os hóspedes dos testes são 1 (`src/test/java/hotel/reports/ReportsTest.java:33`),
então o fator "por hóspede" da taxa não está conferido. As rotas novas também não têm teste pela API. Um detalhe
menor: o README não foi atualizado com as duas rotas novas (`README.md:56-57` lista só ocupação e financeiro).

## Arquitetura

**Números:** 7 pacotes, 17 dependências entre eles, 1 ciclo (`hotel.documents` ↔ `hotel.reservations`). Na base
eram os mesmos 7 pacotes, as mesmas 17 dependências e o mesmo ciclo. `hotel.reports` passou de 2 classes e 95 linhas
para 5 classes e 258 linhas. `HttpApi` passou de 170 para 186 linhas.
**Nota:** bom

O agente não criou nenhuma dependência nova entre pacotes. O ciclo `documents` ↔ `reservations` veio da base. Os
relatórios continuam sem Javalin e sem JDBC: eles dependem só de `ReservationStore` e `Rooms`
(`reports/TourismTaxReport.java:15-19`, `reports/RatePlanReport.java:14-18`). As rotas novas são finas. Cada uma lê o
mês, chama `generate` e escreve o CSV (`web/HttpApi.java:99-106`). A montagem continua num lugar só
(`Hotel.java:28-30`). Um ponto para o futuro, que não chega a defeito: cada relatório novo vira mais um parâmetro do
construtor de `HttpApi` (`web/HttpApi.java:37-44`, agora com sete), mais um campo e mais uma rota quase igual. Com os
dois relatórios que o contador já anunciou, essa lista vai continuar crescendo.

## Design de baixo nível

**Números:** 4 relatórios, 4 dentro da estrutura (`OccupancyReport`, `RevenueReport`, `TourismTaxReport`,
`RatePlanReport`, todos `extends CsvReport`) e 0 fora dela. A linha de título está escrita num lugar só
(`reports/CsvReport.java:18`).
**Classe:** acerto
**Exagero:** contenção
**Nota:** bom

O pattern é Template Method, e o agente o aplicou como a referência pede:

- `CsvReport.generate` é `final` (`reports/CsvReport.java:15-25`) e guarda o esqueleto: selecionar, título, cabeçalho,
  uma linha por item, `total,`.
- Os passos que mudam são abstratos: `name`, `header`, `select`, `rows` e `total` (`reports/CsvReport.java:27-37`).
- Os dois relatórios que já existiam foram reescritos como subclasses e perderam a montagem própria do CSV
  (`reports/OccupancyReport.java:22-53`, `reports/RevenueReport.java:31-57`). Nenhum deles monta mais título ou
  cabeçalho por conta própria.
- A linha de data que o dono anunciou para o ano que vem entra num só lugar (`CsvReport.java:23`).
- O tipo genérico `P` (data ou mês) é o que permite à ocupação, que é por noite, usar o mesmo esqueleto. Não é
  cerimônia.
- O relatório por plano reaproveita a seleção do financeiro (`reports/RatePlanReport.java:36` →
  `reports/RevenueReport.java:23-29`), e assim garante "as mesmas reservas que o financeiro conta".

Não há interface de uma implementação só, fábrica nem camada que só repassa. Por isso o exagero é contenção.

No resto do código novo há pequenas repetições, nada que impeça aceitar:

- a soma de dinheiro `reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add)` aparece três vezes
  (`reports/RevenueReport.java:55-56`, `reports/RatePlanReport.java:21`, `reports/TourismTaxReport.java:54-55`);
- a ordem "como o financeiro" foi copiada (`reports/TourismTaxReport.java:41` repete `reports/RevenueReport.java:27`).
  Se a ordem do financeiro mudar, a da taxa não acompanha.

Os tipos estão certos: `BigDecimal` com a taxa em `new BigDecimal("4.50")` (`reports/TourismTaxReport.java:13`) e o
produto feito em `long` (`TourismTaxReport.java:22-23`). O enum `RatePlan.values()` dá a ordem STANDARD,
NON_REFUNDABLE e inclui o plano sem reservas (`RatePlanReport.java:42-45`). A taxa é uma constante no código, e o
dono avisou que ela muda todo ano. Ela fica num lugar só e com nome, o que é razoável, mas cada mudança pede um
deploy.

## Testes

**Números:** 28 testes rodaram, 0 falhas, 0 erros, 0 pulados, 28 métodos escritos. Na base eram 26, todos passando.
Cobertura de linhas de 93,1% (500 de 537) e de ramos de 85,6% (89 de 104). Na base, linhas 93,8% e ramos 87,1%.
`hotel.reports` tem 73 de 73 linhas. Em `hotel.web` ficaram 11 linhas sem cobertura (eram 5 na base).
**Nota:** com problemas

O que está bom:

- O agente manteve os 26 testes da base. Os quatro testes de relatório que já existiam foram atualizados só com a
  linha de título (`ReportsTest.java:61, 74, 87, 104, 142`), sem ficarem mais fracos.
- Os dois testes novos conferem o CSV inteiro, com valores:
  - a taxa de turismo de abril dá 9.00 + 9.00 = 18.00 (`ReportsTest.java:112-122`);
  - o relatório por plano dá STANDARD 2 / 1939.60 e NON_REFUNDABLE 0 / 0.00 (`ReportsTest.java:125-135`).

O que ficou sem rede, pela pergunta da mutação:

- **O fator "por hóspede":** `book` cria toda reserva com 1 hóspede (`ReportsTest.java:33`). Se `taxOf` multiplicasse
  só pelas noites, nenhum teste falharia. O exemplo do próprio pedido (2 hóspedes × 3 noites = 27.00) não está
  testado.
- **Quais reservas a taxa conta:** a taxa vale para as reservas "pagas" (`TourismTaxReport.java:39`, `isPaid`), e o
  financeiro conta as que deixaram dinheiro. A diferença entre os dois aparece numa NON_REFUNDABLE cancelada, e esse
  caso não está no teste da taxa. Se a taxa usasse a seleção do financeiro, o teste de abril continuaria passando.
- **NON_REFUNDABLE com dinheiro:** no relatório por plano, essa linha só é testada com 0. A cancelada não
  reembolsável, que tem de contar, nunca aparece nele.
- **As rotas novas:** `/reports/tourism-tax` e `/reports/rate-plans` não são chamadas em `HttpApiTest`
  (`src/test/java/hotel/web/HttpApiTest.java:113-120` cobre só ocupação e financeiro). Um erro no caminho ou no
  parâmetro passaria.

## Banco

**Números:** 3 tabelas, todas com chave primária. O dinheiro está em `numeric(10,2)` (6 colunas). Nenhuma chave
estrangeira sem índice e nenhuma data em texto. `status`, `rate_plan`, `type` e `channel` estão em `text` sem CHECK.
Há um EXCLUDE por quarto e período. Uma migração do Flyway, `V1__create_tables.sql`, igual à da base.
**Nota:** bom

O pedido não mexe no banco, e o agente não mexeu: nenhuma migração nova nem editada, e nenhum SQL novo. O que as
conferências acusam veio da base: as colunas de situação e de plano sem CHECK. A taxa usa as noites e os hóspedes
que já estavam guardados (`reservations/ReservationStore.java:130-133`). Uma observação de carga, que também vem da
base: os relatórios trazem a tabela inteira (`ReservationStore.java:108-121`, `SELECT ... FROM reservations ORDER BY
id`) e filtram o mês em Java. O agente repetiu esse padrão nos dois relatórios novos (`TourismTaxReport.java:38-42`,
e `RatePlanReport.java:36` pelo `keptMoneyIn`). Numa pousada de cinco quartos isso não pesa.

## Comportamento

**Números:** 64 de 64 conferências passaram, nenhuma falhou:

- a parte comum: 46 de 46;
- depois de reiniciar: 3 de 3;
- cenário 09: 13 de 13;
- reservas simultâneas: 2 de 2.

**Nota:** bom

O que passou no cenário:

- Os quatro relatórios começam com "Pousada Maré Alta,<nome>,<período>".
- A ocupação e o financeiro continuam iguais depois do título.
- A taxa de turismo responde em `text/csv` com as pagas que começam no mês e o total.
- Num mês vazio, a taxa traz título, cabeçalho e `total,0.00`, e um mês fora do formato dá 400.
- O relatório por plano traz STANDARD e NON_REFUNDABLE, inclusive o plano sem reserva com 0, e o total.

Para o dono da pousada, os relatórios que ele pediu saem como ele descreveu, e o que já funcionava continua
funcionando.

## Dúvidas

O pedido não fecha se a taxa de turismo conta uma reserva NON_REFUNDABLE cancelada, que deixou dinheiro mas não tem
mais hóspede. O agente seguiu a lista do pedido ("as confirmadas, as que já fizeram check-in e as que já fizeram
check-out", via `isPaid`, `TourismTaxReport.java:39`) e não a conta. É uma leitura razoável, mas nenhum teste do
agente a fixa.
