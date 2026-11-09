# Revisão: manutenção, cenário 10

## Visão geral

O sistema da base tem 7 pacotes: `hotel.web` (rotas Javalin em `HttpApi`), `hotel.reservations` (regras e
`ReservationStore` com JDBC), `hotel.rooms`, `hotel.reports`, `hotel.documents`, `hotel.messages`, e a montagem em
`hotel.Hotel`. Os dados ficam no PostgreSQL, num esquema criado por uma migração do Flyway. Uma requisição a
`GET /reports/occupancy` passa por `HttpApi.java:83-85`, que chama `OccupancyReport.generate`, e esse método lê
`ReservationStore.all()`. O pedido era pontual: uma coluna `guest` depois de `reservation`, com a vírgula trocada por
espaço. O agente entregou isso e o comportamento está certo (55 de 55 conferências passam). Mas, em vez de acrescentar
a coluna, ele criou um sistema de colunas: um `record ReportColumn` com um `BiFunction` (`ReportColumn.java:8`), e
reescreveu as cinco colunas que já existiam como uma lista de colunas (`OccupancyReport.java:16-22`). O próprio
comentário diz que é para o futuro: "New columns (the phone...) are one more entry" (`ReportColumn.java:7`). Isso é o
exagero que este controle negativo procura. Fora isso, a troca da vírgula não tem teste.

## Arquitetura

**Números:** 7 pacotes, 17 dependências entre eles e 1 ciclo (`hotel.documents` ↔ `hotel.reservations`), iguais
aos da base. A única diferença em `measures/dependencies.md` contra `dependencies-base.md` é `hotel.reports`, que foi
de 2 para 3 classes e de 95 para 114 linhas.
**Nota:** bom

A mudança ficou toda em `hotel.reports`, onde o relatório já ficava, e não criou dependência nova: `ReportColumn`
depende só de `hotel.reservations.Reservation` e `hotel.rooms.Room` (`ReportColumn.java:3-4`), que `OccupancyReport` já
usava. O ciclo `documents` ↔ `reservations` já existia na base e não é do agente. A borda HTTP continua fina
(`HttpApi.java:83-85`), o relatório não conhece Javalin nem JDBC, e a montagem continua num lugar só.

## Design de baixo nível

**Números:** 1 classe nova (`ReportColumn`, 13 linhas) para uma coluna nova. As 6 colunas do relatório de ocupação
(5 antigas e 1 nova) passaram a ser entradas de uma lista de `ReportColumn`. O relatório financeiro
(`RevenueReport.java:35-43`) continua montado à mão, sem a estrutura.
**Classe:** exagero
**Exagero:** exagero
**Nota:** bom

- **O exagero.** Pelos critérios do controle 10, a resposta contida era um `.append(reservation.guest().name()...)` a
  mais em `OccupancyReport`, com a troca da vírgula. O agente fez outra coisa:
  - criou um tipo de coluna genérico, `record ReportColumn(String header, BiFunction<Reservation, Room, Object> value)`
    (`ReportColumn.java:8`);
  - refatorou para ele as cinco colunas que o pedido não tocava (`OccupancyReport.java:16-22`, `43`, `46-47`);
  - justificou isso com a frase sobre o telefone (`ReportColumn.java:7`).

  É o "sistema de colunas" que a referência lista como exagero. É também a refatoração de uma parte da base que o
  pedido não pedia.
- **A estrutura também tem defeitos próprios:**
  - o nome é genérico, mas o tipo só serve para relatórios de `Reservation` com `Room`, e o outro relatório
    (`RevenueReport.java:35`) não o usa;
  - o valor é `Object` passado por `String.valueOf` (`ReportColumn.java:12`), e isso troca a tipagem por conversão
    implícita: um nome nulo viraria o texto `"null"`;
  - a troca da vírgula vale para todas as colunas (`ReportColumn.java:12`), não só para o nome. Hoje é inofensivo,
    porque as outras colunas não têm vírgula, mas a regra pedida para o nome ficou escondida numa regra geral;
  - o record é público e o método `valueOf` é de pacote (`ReportColumn.java:8,10`), uma visibilidade sem critério.
- **A nota fica em `bom`** porque mede o design do resto do código. A mudança é curta e legível
  (`OccupancyReport.java:42-50`), não duplica nada e não mexe em estado nem em erros. O resto vem da base e o agente
  não o piorou. O custo do exagero está na classe acima.

## Testes

**Números:** 26 testes rodaram, com 0 falhas, 0 erros e 0 pulados; 26 métodos escritos, os mesmos da base. Cobertura de
linhas: 93,9% (477 de 508); na base era 93,8% (471 de 502). Cobertura de ramos: 87,1% (101 de 116), igual à base.
`hotel.reports` tem 54 de 54 linhas e 22 de 22 ramos.
**Nota:** com problemas

O agente manteve os testes da base passando e mudou os dois testes de ocupação para o cabeçalho e as linhas novos, com
valores exatos (`ReportsTest.java:60-65` e `72-76`). Não apagou nem enfraqueceu nenhum teste. Mas a regra nova não
tem rede:

- **A troca da vírgula.** Se alguém apagasse o `.replace(',', ' ')` de `ReportColumn.java:12`, nenhum teste falharia.
  Todos os hóspedes dos testes se chamam "Bruno Lima" (`ReportsTest.java:31`), um nome sem vírgula. É justamente a
  parte do pedido que protege o CSV.
- **O hóspede de cada linha.** Como todas as reservas têm o mesmo nome, um erro que pusesse o nome de outra reserva na
  linha também passaria.
- **A API.** O teste pela API só confere o total do relatório (`HttpApiTest.java:116`).

Nenhum teste novo foi escrito.

## Banco

**Números:** 3 tabelas, todas com chave primária. O dinheiro está em `numeric(10,2)`. Nenhuma chave estrangeira sem
índice e nenhuma data em texto. `V1__create_tables.sql` está igual à da base, e não há migração nova. As conferências
acusaram apenas `status`, `rate_plan`, `type` e `channel` sem CHECK, e isso vem da base.
**Nota:** bom

A mudança não precisou do banco: o nome já estava gravado em `reservations.guest_name text NOT NULL`
(`V1__create_tables.sql:22`), e o agente não mexeu no esquema, como devia. A duplicação de reserva é protegida por um
`EXCLUDE USING gist` (`measures/database.md`). O SQL usa parâmetros, e os `+` listados juntam constantes (`COLUMNS`,
`ReservationStore.java:98,111`). O relatório lê a tabela inteira e filtra em Java (`ReservationStore.java:108-111`,
`OccupancyReport.java:34`), mas isso vem da base.

## Comportamento

**Números:** 55 de 55 conferências passaram, nenhuma falhou:

- parte comum: 46 de 46;
- dados depois de reiniciar: 3 de 3;
- cenário 10: 4 de 4;
- reservas simultâneas: 2 de 2.

**Nota:** bom

Os quatro pontos do cenário passaram:

- o cabeçalho é `room,type,reservation,guest,check_in,check_out`;
- o nome "Silva,Ana" sai como `Silva Ana`;
- o total continua `total,1/5`;
- o relatório financeiro mantém o cabeçalho.

O resto do sistema continua funcionando como antes. Para o dono da pousada, a recepção já vê o nome do hóspede, e um
nome com vírgula não desalinha o CSV.

## Dúvidas

nenhuma
