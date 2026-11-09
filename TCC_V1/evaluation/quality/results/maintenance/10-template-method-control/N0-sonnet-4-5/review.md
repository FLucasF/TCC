# Revisão: manutenção, cenário 10

## Visão geral

Já na base, o projeto tem sete pacotes: `hotel` (montagem), `hotel.web` (Javalin), `hotel.reservations` (regras e
`ReservationStore` com JDBC), `hotel.rooms`, `hotel.messages`, `hotel.documents` e `hotel.reports`. Uma requisição
chega em `HttpApi` (`src/main/java/hotel/web/HttpApi.java:61-91`), que chama o serviço ou o relatório. Os dados ficam
no PostgreSQL, num esquema criado pelo Flyway (`src/main/resources/db/migration/V1__create_tables.sql`). O pedido era
pontual: pôr a coluna `guest` logo depois de `reservation` no relatório de ocupação, trocando a vírgula por espaço. A
mudança do agente é a menor possível. Ele mexeu em dois arquivos, `OccupancyReport.java` (o cabeçalho na linha 34 e
o nome tratado e escrito nas linhas 37 e 41) e `ReportsTest.java` (duas asserções atualizadas e um teste novo para a
vírgula). Não criou estrutura nova por causa da frase sobre o telefone. Os 27 testes passam e as 55 conferências do
roteiro passam. Não achei nada sério.

## Arquitetura

**Números:** 7 pacotes e 17 dependências entre eles, iguais aos da base (`measures/dependencies.md` e
`measures/dependencies-base.md` só diferem nas linhas de `hotel.reports`, que foram de 95 para 97). Há 1 ciclo,
`hotel.documents` ↔ `hotel.reservations`, que já existia na base.
**Nota:** bom

A mudança ficou toda em `hotel.reports`, e o relatório continua sem Javalin e sem SQL. Ele lê as reservas pelo
`ReservationStore` e o nome por `reservation.guest().name()` (`src/main/java/hotel/reports/OccupancyReport.java:37`),
que é o modelo que o pacote já usava. O agente não criou dependência nova entre pacotes. A borda HTTP continua fina
(`src/main/java/hotel/web/HttpApi.java:83-86`) e a montagem continua num lugar só (`src/main/java/hotel/Hotel.java:27`).
O ciclo `documents`/`reservations` vem da base: `ReservationService` cria `ReservationReceipt`
(`src/main/java/hotel/reservations/ReservationService.java:64`). O agente não o piorou.

## Design de baixo nível

**Números:** a variação é uma coluna a mais em uma classe: 2 linhas mudadas e 2 acrescentadas em `OccupancyReport`. O
agente não criou nenhuma classe, interface ou abstração de colunas.
**Classe:** contenção
**Exagero:** contenção
**Nota:** bom

É o controle negativo. A frase "talvez um dia a recepção peça o telefone do hóspede também" não levou o agente a um
esqueleto de relatórios nem a colunas configuráveis. Ele fez o que `controls.md` chama de contenção: acrescentou a
coluna no próprio `OccupancyReport` (cabeçalho em `src/main/java/hotel/reports/OccupancyReport.java:34`, valor em
`:41`) e trocou a vírgula por espaço com um `replace(',', ' ')` (`:37`). A variável local `guestName` deixa claro que
o nome sai tratado. O relatório financeiro ficou intocado (`src/main/java/hotel/reports/RevenueReport.java:35`).

O resto do código vem da base e está limpo. O dinheiro usa `BigDecimal`, as datas usam `LocalDate`/`YearMonth`, a
situação e o plano são `enum`, as regras de transição ficam no serviço com mensagens claras
(`src/main/java/hotel/reservations/ReservationService.java:58-110`) e os erros vêm tipados por `ErrorCode`. Há dois
detalhes menores, ambos da base: o `catch (Exception)` ao ler o corpo (`src/main/java/hotel/web/HttpApi.java:113`), que
é justificado e vira 400, e a montagem do CSV repetida à mão nos dois relatórios. Nada a pedir ao agente.

## Testes

**Números:** em `final`, 27 testes rodaram (0 falhas, 0 erros, 0 pulados) e há 27 métodos escritos. Na base eram 26,
todos passando. A cobertura de linhas é 473/504 (93,8%) e a de ramos 101/116 (87,1%), praticamente igual à da base
(471/502 e 101/116). `hotel.reports` tem 50/50 linhas e 22/22 ramos cobertos.
**Nota:** bom

O agente atualizou as duas asserções de ocupação, que conferem o CSV inteiro com valores, incluindo o cabeçalho novo e
o nome (`src/test/java/hotel/reports/ReportsTest.java:60-65` e `:72-76`). Também acrescentou
`commasInGuestNamesAreReplacedWithSpaces` (`src/test/java/hotel/reports/ReportsTest.java:117-131`), que reserva para
"Silva, João" e espera `Silva  João` na linha. Não enfraqueceu nenhum teste da base.

A pergunta da mutação:

- Apagar o `replace` faz o teste novo falhar.
- Trocar a coluna de posição faz os três testes de ocupação falharem.
- Tirar a coluna do cabeçalho também faz os três falharem.

Há duas fragilidades pequenas. O id `6` está fixo no teste novo (`:128`); é determinístico, porque cada teste usa um
banco vazio com 5 reservas no `@BeforeEach`. E nos testes antigos todos os hóspedes se chamam "Bruno Lima"
(`:32`), o que não distingue um nome trocado entre reservas. O teste pela API continua só conferindo o total da
ocupação (`src/test/java/hotel/web/HttpApiTest.java:116`), como na base.

## Banco

**Números:** 3 tabelas, todas com chave primária. O dinheiro está todo em `numeric(10,2)`. Não há chave estrangeira
sem índice nem data em texto. O banco tem um `EXCLUDE` gist contra duas reservas sobrepostas no mesmo quarto. A
migração `V1__create_tables.sql` está igual à da base, e não há migração nova.
**Nota:** bom

O pedido não exigia mudar o banco, e o agente não mexeu nele: o nome já estava em `reservations.guest_name text NOT
NULL` (`measures/database-schema.sql:92`) e já era lido em `src/main/java/hotel/reservations/ReservationStore.java:124`.
O SQL usa parâmetros. Os candidatos com `+` em `ReservationStore.java:33,83,98,111` só concatenam constantes, como
`COLUMNS`. A base já tinha três defeitos, que o agente não mudou:

- as colunas `status`, `rate_plan`, `type` e `channel` não têm `CHECK`;
- o relatório traz a tabela inteira (`store.all()`, `src/main/java/hotel/reports/OccupancyReport.java:25`) para filtrar
  em Java;
- não há transação explícita (0 `setAutoCommit(false)`).

## Comportamento

**Números:** 55 de 55 conferências passaram, nenhuma falhou:

| parte | passaram |
|---|---|
| a parte comum | 46 de 46 |
| depois de reiniciar | 3 de 3 |
| o cenário 10 | 4 de 4 |
| reservas simultâneas | 2 de 2 |

**Nota:** bom

Pela API, o relatório de ocupação traz a coluna `guest` logo depois de `reservation`, a vírgula do nome vira espaço,
a linha do total não mudou e o relatório financeiro continua com o mesmo cabeçalho. Para o dono da pousada, a
recepção passa a ver o nome do hóspede, e o resto do sistema funciona como antes: preços, recusas, e-mails,
cancelamentos, os dados depois de reiniciar e a recusa da segunda reserva simultânea.

## Dúvidas

O pedido fala só da vírgula. Um nome com aspas ou com quebra de linha continua podendo bagunçar o CSV
(`src/main/java/hotel/reports/OccupancyReport.java:37`). O texto não pedia mais que isso, então não contei como
defeito.
