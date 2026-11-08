# Revisão: manutenção, cenário 09

## Visão geral

A base é um serviço Javalin em 7 pacotes: `hotel.web` (as rotas, `HttpApi`), `hotel.reservations` (as regras em
`ReservationService` e o JDBC em `ReservationStore`), `hotel.rooms`, `hotel.messages`, `hotel.documents` e
`hotel.reports`, montados em `hotel/Hotel.java:22-32`. Os dados ficam no PostgreSQL, num esquema criado por uma
migração do Flyway. Uma requisição de relatório entra em `HttpApi` (`web/HttpApi.java:91-106`), que chama
`generate(...)` da classe do relatório. A classe lê todas as reservas por `store.all()`, filtra em Java e escreve o
CSV à mão. O agente pôs a linha de título nos dois relatórios que já existiam e criou `TourismTaxReport` e
`RatePlansReport`, cada um escrito inteiro, com as duas rotas novas. O mais sério tem duas partes. Primeiro, os quatro
relatórios continuam cópias uns dos outros, com o título copiado quatro vezes, e o pedido era exatamente o contrário:
"quando o jeito dos relatórios mudar, quero que mude em todos de uma vez". Segundo, o relatório por plano não conta as
mesmas reservas que o financeiro. Ele deixa de fora a NON_REFUNDABLE cancelada, que o financeiro conta, e por isso
mostra para a gerência menos dinheiro do que entrou (roteiro: `NON_REFUNDABLE,0,0.00` em vez de `1,795.40`).

## Arquitetura

**Números:** 7 pacotes e 17 dependências entre eles, iguais aos da base. Há 1 ciclo (`hotel.documents` ↔
`hotel.reservations`), que já existia na base. `hotel.reports` passou de 2 classes e 95 linhas para 4 classes e
199 linhas; `HttpApi` passou de 170 para 186 linhas.
**Nota:** bom

O agente não mexeu na forma do sistema. As classes novas ficam em `hotel.reports`, que depende só de
`hotel.reservations` e `hotel.rooms` e não usa Javalin nem JDBC (tabela de bibliotecas em `measures/dependencies.md`).
As rotas novas só leem o mês, chamam o relatório e escrevem o CSV (`web/HttpApi.java:99-106`). A montagem continua
num lugar só (`Hotel.java:28-30`). O ciclo `documents`↔`reservations` veio da base. Fica um custo pequeno: cada
relatório novo acrescenta um campo e um parâmetro ao construtor de `HttpApi` (`web/HttpApi.java:37-51`) e uma rota
copiada. É mais um sintoma da falta de estrutura comum aos relatórios, e esse ponto conta no design.

## Design de baixo nível

**Números:** dos 4 relatórios, nenhum passa por um esqueleto comum. Os 4 repetem a sequência inteira (selecionar,
ordenar, título, cabeçalho, linhas, total), e a linha "Pousada Maré Alta,..." está copiada em 4 lugares.
**Classe:** omissão
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** O texto descreve 4 relatórios com os mesmos passos e avisa que outros virão. Também pede que uma
mudança no formato (a linha de data no fim, no ano que vem) valha "em todos de uma vez". O agente não criou classe
abstrata com método-modelo nem escritor genérico. Ele copiou o título em `reports/OccupancyReport.java:34`,
`reports/RevenueReport.java:35`, `reports/TourismTaxReport.java:30` e `reports/RatePlansReport.java:39`, e escreveu as
duas classes novas inteiras, repetindo o laço de seleção e o `StringBuilder`. Pela referência, isso é o caso de
omissão: "os quatro relatórios ficam escritos inteiros, cada um com a sua linha de título". A linha de data do ano que
vem vai exigir mexer de novo nos 4 arquivos. Não há estrutura sem necessidade (contenção).

**O resto do código.**
- **Regra copiada que divergiu.** A seleção "as mesmas reservas que o relatório financeiro conta" foi reescrita em vez
  de reaproveitada, e saiu diferente. O financeiro usa `amountKept().signum() > 0` (`reports/RevenueReport.java:26-28`);
  o de planos usa `isPaid() && startsInMonth` (`reports/RatePlansReport.java:26-27`), que deixa de fora a
  NON_REFUNDABLE cancelada. Esse é o erro de comportamento abaixo, e nasce justamente da duplicação.
- **Um bloco por plano em vez do enum.** `RatePlansReport` tem duas variáveis por plano e um `if/else if` sobre
  `RatePlan` (`reports/RatePlansReport.java:20-34`, `42-43`) em vez de percorrer `RatePlan.values()`. Um terceiro
  plano sumiria do relatório sem erro.
- **O valor da taxa preso no método.** O dono avisou que a taxa muda todo ano, e ela é um literal criado dentro de um
  método privado (`reports/TourismTaxReport.java:48-50`). O `RoundingMode` está escrito com o nome completo no meio da
  expressão.
- **Imports sem uso** de `ArrayList` e `List` (`reports/RatePlansReport.java:8-9`). O nome da pousada também está
  copiado como texto em 4 lugares.
- **O que está certo:** `BigDecimal` com 2 casas para a taxa e os valores, `YearMonth` e `LocalDate` para os períodos,
  e nada de estado mutável novo.

São defeitos localizados no pacote de relatórios, mas um deles já virou dado errado.

## Testes

**Números:** 29 testes rodaram, nenhum falhou ou foi pulado (29 métodos escritos; a base tinha 26). Cobertura de
linhas 93.5% (532/569), de ramos 88.1% (118/134); a base tinha 93.8% e 87.1%. Em `hotel.reports`, 105/105 linhas
e 39/40 ramos.
**Nota:** com problemas

O agente atualizou os 5 testes de relatório da base para a linha de título, sem enfraquecer nenhum
(`reports/ReportsTest.java:62`, `75`, `88`, `105`, `117`). Escreveu 3 testes novos com o CSV inteiro comparado. Mesmo
assim, as regras novas ficaram com pouca rede:
- **A taxa de turismo** só é testada com 1 hóspede (`reports/ReportsTest.java:34`, todas as reservas com `guests` = 1;
  `:124-136`). Se alguém tirasse `guests` da conta em `TourismTaxReport.java:50`, nenhum teste falharia. O exemplo do
  próprio pedido (2 hóspedes × 3 noites = 27,00) não está testado.
- `tourismTaxOnlyIncludesPaidReservations` (`reports/ReportsTest.java:138-145`) só conta linhas e repete o que o teste
  anterior já confere.
- **O relatório por plano.** `ratePlansShowsBothPlansEvenIfEmpty` (`reports/ReportsTest.java:147-161`) não tem nenhum
  plano vazio, apesar do nome. Também não tem a NON_REFUNDABLE cancelada que o financeiro conta (a do conjunto de
  testes é de maio, `:98-107`, e o teste novo só olha abril). Por isso o erro de seleção passou. A frase do pedido
  "mesmo que um deles não tenha nenhuma reserva" ficou sem teste.
- **As rotas novas** (`/reports/tourism-tax`, `/reports/rate-plans`) não têm teste pela API: `web/HttpApiTest.java`
  continua igual ao da base.

## Banco

**Números:** 3 tabelas, todas com chave primária. Dinheiro em `numeric(10,2)`, nenhuma chave estrangeira sem índice,
nenhuma data em texto, a restrição `EXCLUDE` contra duas reservas na mesma noite. As colunas `status`, `rate_plan`,
`type` e `channel` estão em texto sem CHECK. 1 migração do Flyway (`V1__create_tables.sql`), igual à da base.
**Nota:** bom

O pedido não mexe no banco, e o agente não mexeu: a migração da base ficou intacta e não há SQL novo. Os candidatos
de SQL com `+` em `reservations/ReservationStore.java:33,83,98,111` juntam só constantes, e vieram da base. O que as
conferências apontam (colunas de situação sem CHECK, nenhuma transação explícita) veio da base. Fica uma observação
de carga: os dois relatórios novos repetem o padrão da base de trazer a tabela inteira por `store.all()`
(`reservations/ReservationStore.java:108`) e filtrar em Java (`reports/TourismTaxReport.java:21`,
`reports/RatePlansReport.java:25`). Isso não muda a nota.

## Comportamento

**Números:** 63 de 64 conferências passaram. Parte comum: 46/46. Dados depois de reiniciar: 3/3. Cenário 09: 12/13.
Reservas simultâneas: 2/2.
**Nota:** com problemas

Tudo o que já existia continua funcionando, e os 4 relatórios saem com a linha de título certa. A taxa de turismo
está correta, inclusive no mês vazio e no mês fora do formato. Falhou o relatório por plano. Esperado:
`NON_REFUNDABLE,1,795.40` e `total,3,2390.92`. Veio: `NON_REFUNDABLE,0,0.00` e `total,2,1595.52`. O texto era
explícito ("contando as mesmas reservas que o relatório financeiro conta"), e o financeiro do mesmo mês conta a
NON_REFUNDABLE cancelada, em que a pousada ficou com o dinheiro. Para o dono, isso quer dizer que o relatório que a
gerência vai usar para decidir qual plano rende mais mostra o não reembolsável rendendo menos do que rendeu, e o total
dele não bate com o do financeiro do mesmo mês. A causa está em `reports/RatePlansReport.java:27`.

## Dúvidas

- A nota do design ficou entre `com problemas` e `ruim`. Os defeitos estão concentrados no pacote de relatórios, mas
  a duplicação ali já produziu um dado errado. Fiquei com `com problemas` porque o resto do código, que veio da base,
  não foi piorado.
- O README não lista as duas rotas novas (`README.md:56-57`). É um ponto menor e não pesou em nenhuma coluna.
