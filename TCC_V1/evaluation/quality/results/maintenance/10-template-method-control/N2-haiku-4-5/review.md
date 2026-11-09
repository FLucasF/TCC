# Revisão: manutenção, cenário 10

## Visão geral

O sistema da base está em 7 pacotes: `hotel.web` (Javalin, `HttpApi`) recebe a requisição e chama as regras em
`hotel.reservations` (`ReservationService`, `PriceCalculator`), que guardam os dados no PostgreSQL por
`ReservationStore` (JDBC com pool, esquema por uma migração Flyway). Os relatórios ficam em `hotel.reports`. O pedido
era pontual: uma coluna `guest` no relatório de ocupação, logo depois de `reservation`, com a vírgula do nome trocada
por espaço. O agente mudou só dois arquivos (`changes.patch`): duas linhas e o cabeçalho em
`src/main/java/hotel/reports/OccupancyReport.java:34,37,41`, os testes da ocupação acertados e um teste novo em
`src/test/java/hotel/reports/ReportsTest.java:117-131`. Não criou estrutura nenhuma para o "telefone, talvez um dia".
Os 27 testes passam e as 55 conferências do roteiro também. Não achei nada sério.

## Arquitetura

**Números:** 7 pacotes, 17 dependências entre eles, 1 ciclo (`hotel.documents` ↔ `hotel.reservations`). Na base são
os mesmos 7 pacotes, as mesmas 17 dependências e o mesmo ciclo. A única diferença é que `hotel.reports` passou de 95
para 97 linhas.
**Nota:** bom

A mudança fica no lugar onde o relatório já morava (`src/main/java/hotel/reports/OccupancyReport.java:34-43`). Ela
não cria dependência nova: `OccupancyReport` já importava `hotel.reservations.Reservation`
(`OccupancyReport.java:3`), e `Guest` é do mesmo pacote. A borda HTTP continua fina: a rota só lê `date` e devolve o
CSV (`src/main/java/hotel/web/HttpApi.java:83-85`). O ciclo vem da base, e o agente não o piorou:
`ReservationService` importa `hotel.documents.ReservationReceipt` (`src/main/java/hotel/reservations/ReservationService.java:3`),
e o recibo importa `Reservation` (`src/main/java/hotel/documents/ReservationReceipt.java:3`).

## Design de baixo nível

**Números:** 1 arquivo de produção mudado, 3 linhas (1 trocada e 2 novas). Nenhuma classe, interface ou método novo.
**Classe:** contenção
**Exagero:** contenção
**Nota:** bom

O controle negativo pede "uma coluna a mais no `OccupancyReport`, e a troca da vírgula por espaço", e foi isso o que
o agente fez. O cabeçalho ganhou `guest` depois de `reservation` (`OccupancyReport.java:34`). O nome é limpo numa
variável local com `replace(",", " ")` (`OccupancyReport.java:37`) e entra na linha no lugar pedido
(`OccupancyReport.java:41`). A frase sobre o telefone não virou nada: não há colunas configuráveis, nem um esqueleto
genérico de relatório, nem um formatador de CSV à parte. O `RevenueReport` não foi tocado
(`src/main/java/hotel/reports/RevenueReport.java:35`).

O resto do código vem da base e está limpo. Dinheiro usa `BigDecimal` (`RevenueReport.java:36`), datas usam
`LocalDate`, situação e plano são `enum`, e as exceções têm código (`ReservationService.java:133-137`). O `switch` do
plano de tarifa no cancelamento (`ReservationService.java:82-85`) também vem da base. O nome não chega nulo ao
relatório, porque a API recusa nome em branco (`HttpApi.java:118`).

## Testes

**Números:** 27 testes rodaram, 0 falhas, 0 erros, 0 pulados; 27 métodos escritos (a base tinha 26). Cobertura de
linhas de 93,8% (473 de 504; a base tem 471 de 502). Cobertura de ramos de 87,1% (101 de 116, igual à base). No
pacote `hotel.reports`: 50 de 50 linhas e 22 de 22 ramos.
**Nota:** bom

O agente atualizou os dois testes da ocupação que já existiam, sem enfraquecê-los: a comparação continua sendo do CSV
inteiro, agora com a coluna nova (`src/test/java/hotel/reports/ReportsTest.java:60-65` e `72-76`). Ele também
acrescentou `occupancyReplacesCommasInGuestName` (`ReportsTest.java:117-131`), que reserva para "Silva, Maria" e
espera `Silva  Maria` na posição certa (`ReportsTest.java:128`).

A pergunta da mutação:
- se alguém apagasse o `replace`, o teste novo falharia;
- se a coluna fosse para outra posição, ou saísse do cabeçalho, os três testes falhariam;
- se alguém trocasse o nome por outro campo da reserva, o teste novo falharia.

Dois pontos pequenos. Todas as reservas do `@BeforeEach` têm o mesmo hóspede, "Bruno Lima" (`ReportsTest.java:31`),
então uma troca de hóspede entre linhas não seria pega. E o teste novo depende do id 6 gerado pelo banco
(`ReportsTest.java:128`). Nenhum dos dois pede mudança antes de aceitar. O teste pela API só confere o fim do CSV
(`src/test/java/hotel/web/HttpApiTest.java:116`), como já fazia na base.

## Banco

**Números:** 3 tabelas, todas com chave primária. O dinheiro está todo em `numeric(10,2)`. Nenhuma chave estrangeira
sem índice e nenhuma data em texto. A migração `V1__create_tables.sql` está igual à da base. A regra de quarto ocupado
é protegida por um `EXCLUDE USING gist`. As colunas de situação, plano, tipo e canal são `text` sem CHECK.
**Nota:** bom

O pedido não precisava mudar o banco: o nome do hóspede já é guardado (`guest_name`, na lista de colunas de
`src/main/java/hotel/reservations/ReservationStore.java:18`), e o agente não mexeu no esquema nem nas migrações. O
que as conferências apontam vem da base:
- as colunas de situação e de tipo sem CHECK;
- o relatório que lê a tabela inteira com `store.all()` e filtra em Java (`ReservationStore.java:108`,
  `OccupancyReport.java:25`).

O SQL montado com `+` só junta constantes (`ReservationStore.java:33,83,98,111`).

## Comportamento

**Números:** 55 de 55 conferências passaram:
- a parte comum: 46 de 46;
- depois de reiniciar: 3 de 3;
- o cenário 10: 4 de 4 (o cabeçalho com `guest` depois de `reservation`, a vírgula que vira espaço, o total, o
  financeiro igual);
- as reservas ao mesmo tempo: 2 de 2.

**Nota:** bom

Para o dono da pousada: a recepção passa a ver o nome do hóspede no relatório de ocupação, na coluna pedida. Um nome
com vírgula não quebra o CSV. O resto do relatório, o relatório financeiro e o que já funcionava continuam iguais.

## Dúvidas

Quanto à arrumação do nome, o agente só faz o que o pedido diz: troca a vírgula por espaço. Uma quebra de linha ou
uma aspa no nome ainda bagunçaria o CSV. A API aceita esses nomes (só recusa nome em branco, `HttpApi.java:118`), mas
o texto só falou da vírgula, então não contei isso como defeito.
