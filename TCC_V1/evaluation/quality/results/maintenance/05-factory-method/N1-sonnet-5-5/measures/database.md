# Banco: o esquema que o serviço cria e as conferências simples

Resultado: medido. O serviço subiu contra um banco vazio e criou 3 tabelas (o esquema inteiro em `measures/database-schema.sql`).

## Como o esquema é criado

- Flyway: 2 migrações aplicadas
- arquivos de esquema em src/main/resources: 2
  - `src/main/resources/db/migration/V1__create_tables.sql`
  - `src/main/resources/db/migration/V2__guest_identification.sql`

Na manutenção, as migrações da base (`base/`) comparadas com as do projeto final:
- `src/main/resources/db/migration/V1__create_tables.sql`: igual
- `src/main/resources/db/migration/V2__guest_identification.sql`: nova

## As tabelas

| tabela | colunas | NOT NULL | chave primária | chaves estrangeiras | CHECK | UNIQUE / EXCLUDE |
|---|---|---|---|---|---|---|
| `public.messages` | 5 | 5 | sim | 0 | 0 | 0 |
| `public.reservations` | 20 | 14 | sim | 1 | 2 | 1 |
| `public.rooms` | 4 | 4 | sim | 0 | 2 | 0 |

## As conferências

**Tabela sem chave primária:** nenhuma

**Dinheiro** (colunas com nome de valor: price, total, subtotal, discount, amount, refund, paid, rate, tax, fee, valor, preco, desconto; o certo é `numeric(p, 2)`):

- `reservations.amount_paid`: numeric(10,2), ok
- `reservations.discount`: numeric(10,2), ok
- `reservations.refund`: numeric(10,2), ok
- `reservations.subtotal`: numeric(10,2), ok
- `reservations.total`: numeric(10,2), ok
- `rooms.nightly_rate`: numeric(10,2), ok

**Chave estrangeira sem índice** (nenhum índice começando pelas colunas da chave):

- nenhuma

**Datas guardadas como texto** (colunas com nome de data em text ou varchar):

- nenhuma

**As colunas de situação e de tipo** (status, plano, tipo, canal) e se há CHECK sobre elas:

- messages.channel: text, sem CHECK
- reservations.rate_plan: text, sem CHECK
- reservations.status: text, sem CHECK
- rooms.type: text, sem CHECK

**Restrições que protegem uma regra** (UNIQUE, EXCLUDE, CHECK):

- `reservations: CHECK ((check_out > check_in))`
- `reservations: CHECK ((guests >= 1))`
- `reservations: EXCLUDE USING gist (room_number WITH =, daterange(check_in, check_out) WITH &&) WHERE ((status <> 'CANCELLED'::text))`
- `rooms: CHECK ((capacity > 0))`
- `rooms: CHECK ((nightly_rate > (0)::numeric))`

## O código que fala com o banco

- arquivos com JDBC (`java.sql`/`javax.sql`): 5
- pool de conexões (HikariCP ou outro): 1 arquivos
- `DriverManager.getConnection`: 0 chamadas
- transações explícitas (`setAutoCommit(false)`): 0
- `FOR UPDATE` (trava de linha): 0

SQL montado com `+` (candidatos; confira se o que entra é constante ou dado de fora):
```
src/main/java/hotel/reservations/ReservationStore.java:35:                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";
src/main/java/hotel/reservations/ReservationStore.java:90:                                + "AND check_in < ? AND ? < check_out")) {
src/main/java/hotel/reservations/ReservationStore.java:105:                        "SELECT " + COLUMNS + " FROM reservations WHERE id = ?")) {
src/main/java/hotel/reservations/ReservationStore.java:118:                        "SELECT " + COLUMNS + " FROM reservations ORDER BY id");
```
