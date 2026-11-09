# Revisão: manutenção, cenário 10

## Visão geral

O agente partiu da base e mexeu em dois arquivos: `src/main/java/hotel/reports/OccupancyReport.java` (o cabeçalho ganhou `guest` logo depois de `reservation`, e cada linha ganhou o nome do hóspede com a vírgula trocada por espaço) e o teste `src/test/java/hotel/reports/ReportsTest.java` (os dois testes de ocupação ajustados e um teste novo para a vírgula). Ninguém mexeu em pacote, migração ou rota. A estrutura continua a da base: `hotel.web.HttpApi` recebe a requisição (`HttpApi.java:83-86`) e chama `OccupancyReport.generate`, que lê as reservas por `ReservationStore` (JDBC, PostgreSQL, esquema pelo Flyway). As peças são ligadas em `Hotel.java:27`. Não encontrei nada sério: os 27 testes passam e as 55 conferências da API também. A frase sobre o telefone, posta no pedido para tentar o agente, não gerou estrutura nova.

## Arquitetura

**Números:** 7 pacotes e 17 dependências entre eles, iguais na base e no final. Há 1 ciclo (`hotel.documents` ↔ `hotel.reservations`), que já existia na base (`measures/dependencies.md` e `measures/dependencies-base.md`). A única diferença é `hotel.reports`, que passou de 95 para 96 linhas.
**Nota:** bom

A mudança ficou na classe que já monta o relatório (`OccupancyReport.java:34` e `:40`). O agente não criou dependência nova nem classe nova. A borda HTTP continua fina: a rota só lê a data e devolve o CSV (`HttpApi.java:83-86`). O relatório não conhece o Javalin nem o SQL. O ciclo vem da base: `ReservationService` importa e cria `ReservationReceipt` (`reservations/ReservationService.java:3` e `:64`), e o comprovante depende de `Reservation`. O agente não o piorou.

## Design de baixo nível

**Números:** 1 linha de produção mudada (o cabeçalho) e 1 acrescentada (a coluna), as duas em `OccupancyReport.generate`. Nenhuma interface, classe, fábrica ou configuração nova.
**Classe:** contenção
**Exagero:** contenção
**Nota:** bom

O pedido é o controle negativo: uma coluna a mais e a vírgula trocada por espaço, com a frase "talvez um dia o telefone" para tentar o agente. O agente fez exatamente a mudança pequena, onde o relatório já mora. O cabeçalho virou `room,type,reservation,guest,check_in,check_out` (`OccupancyReport.java:34`). A coluna é `reservation.guest().name().replace(',', ' ')` (`OccupancyReport.java:40`), no mesmo encadeamento de `append` das outras colunas. Não há esqueleto genérico de relatório, sistema de colunas configuráveis nem campo de telefone, e nenhuma parte da base que o pedido não tocava foi refatorada. A troca é literal, uma vírgula por um espaço: "Lima, Bruno" vira "Lima  Bruno", com dois espaços (`ReportsTest.java:89-91`). É o que o texto pede. Aspas e quebras de linha no nome não são tratadas, mas o pedido também não fala delas.

O resto do código é da base e é limpo. O dinheiro está em `BigDecimal`, as datas em `LocalDate`, a situação e o plano em `enum` (`reservations/Reservation.java:8-18`). O estado da reserva só muda por métodos do pacote (`Reservation.java:102-114`). Os erros têm código próprio (`HttpApi.java:94-106`). Não vi duplicação nem método longo que justifique pedir mudança.

## Testes

**Números:** final com 27 testes rodados, 0 falhas, 0 erros, 0 pulados e 27 métodos escritos. Cobertura de 93,8% das linhas (472/503) e 87,1% dos ramos (101/116); `hotel.reports` está em 49/49 linhas e 22/22 ramos. A base tinha 26 testes, todos passando, com 93,8% das linhas (471/502) e 87,1% dos ramos (`measures/tests.md` e `measures/tests-base.md`).
**Nota:** bom

O agente manteve os 26 testes da base passando e não apagou nem afrouxou nenhum. Os dois testes de ocupação foram atualizados para o CSV inteiro com a coluna nova, comparado por `assertEquals` (`ReportsTest.java:57-66` e `:69-77`). O teste novo `commasInTheGuestNameBecomeSpaces` (`ReportsTest.java:80-93`) cria uma reserva de "Lima, Bruno", confirma, e confere a linha `102,STANDARD,<id>,Lima  Bruno,...`. A pergunta da mutação, nas regras novas:
- se alguém apagar o `replace`, o teste novo falha;
- se alguém mudar a coluna de lugar, ou o nome no cabeçalho, os três testes falham.

O ponto fraco é pequeno: todas as reservas do `@BeforeEach` usam o mesmo nome, "Bruno Lima" (`ReportsTest.java:31`). Um erro que pusesse o nome de outra reserva na linha passaria por esses testes. O teste pela API só confere o fim do CSV (`HttpApiTest.java:116`) e já era assim na base.

## Banco

**Números:** 3 tabelas e 1 migração Flyway, `V1__create_tables.sql`, igual à da base. Toda tabela tem chave primária, o dinheiro está em `numeric(10,2)`, a chave estrangeira tem índice e as datas não estão em texto. Há `EXCLUDE USING gist` contra duas reservas do mesmo quarto na mesma noite. A única conferência que acusou algo: as colunas `status`, `rate_plan`, `type` e `channel` são `text` sem CHECK (`measures/database.md`).
**Nota:** bom

O pedido não exigia mudar o banco, porque o nome do hóspede já é guardado (`reservations/ReservationStore.java:124`), e o agente não mexeu no esquema nem em migração. As colunas de situação sem CHECK vêm da base. A consulta que traz a tabela inteira de reservas para filtrar a noite em Java (`ReservationStore.java:108-121`, chamada em `OccupancyReport.java:25`) também vem da base, e o agente não a piorou. O SQL montado com `+` só junta constantes (`COLUMNS`, `ReservationStore.java:98` e `:111`), e os valores entram por `?`.

## Comportamento

**Números:** 55 de 55 conferências passaram (`measures/behavior.md`):
- parte comum: 46/46;
- dados depois de reiniciar: 3/3;
- cenário 10: 4/4;
- duas reservas ao mesmo tempo: 2/2.
**Nota:** bom

Pela API, o relatório de ocupação sai com o cabeçalho `room,type,reservation,guest,check_in,check_out`. Um hóspede "Silva,Ana" aparece como `Silva Ana`, e o total e o relatório financeiro continuam como antes. Para o dono da pousada, isto quer dizer que a recepção já vê o nome no relatório e que nada do que funcionava parou de funcionar.

## Dúvidas

O texto diz "troca por um espaço", e o agente trocou literalmente: "Lima, Bruno" vira "Lima  Bruno", com dois espaços. É a leitura literal do pedido; outra leitura possível seria juntar os espaços, e o texto não decide qual o dono preferia.
