# Banco: o esquema que o serviço cria e as conferências simples

Resultado: medido. O serviço subiu contra um banco vazio e criou 3 tabelas (o esquema inteiro em `measures/database-schema.sql`).

## Como o esquema é criado

- arquivos de esquema em src/main/resources: 0
- `CREATE TABLE` dentro do código Java: src/main/java/br/com/marealta/pousada/Database.java

## As tabelas

| tabela | colunas | NOT NULL | chave primária | chaves estrangeiras | CHECK | UNIQUE / EXCLUDE |
|---|---|---|---|---|---|---|
| `public.messages` | 8 | 7 | sim | 0 | 0 | 0 |
| `public.reservations` | 16 | 15 | sim | 1 | 1 | 0 |
| `public.rooms` | 4 | 4 | sim | 0 | 0 | 0 |

## As conferências

**Tabela sem chave primária:** nenhuma

**Dinheiro** (colunas com nome de valor: price, total, subtotal, discount, amount, refund, paid, rate, tax, fee, valor, preco, desconto; o certo é `numeric(p, 2)`):

- `reservations.amount_paid`: numeric(12,2), ok
- `reservations.discount`: numeric(12,2), ok
- `reservations.refund`: numeric(12,2), ok
- `reservations.subtotal`: numeric(12,2), ok
- `reservations.total`: numeric(12,2), ok
- `rooms.nightly_rate`: numeric(12,2), ok

**Chave estrangeira sem índice** (nenhum índice começando pelas colunas da chave):

- nenhuma

**Datas guardadas como texto** (colunas com nome de data em text ou varchar):

- nenhuma

**As colunas de situação e de tipo** (status, plano, tipo, canal) e se há CHECK sobre elas:

- messages.channel: text, sem CHECK
- messages.status: text, sem CHECK
- reservations.rate_plan: text, sem CHECK
- reservations.status: text, sem CHECK
- rooms.type: text, sem CHECK

**Restrições que protegem uma regra** (UNIQUE, EXCLUDE, CHECK):

- `reservations: CHECK ((check_out > check_in))`

## O código que fala com o banco

- arquivos com JDBC (`java.sql`/`javax.sql`): 3
- pool de conexões (HikariCP ou outro): 1 arquivos
- `DriverManager.getConnection`: 0 chamadas
- transações explícitas (`setAutoCommit(false)`): 1
- `FOR UPDATE` (trava de linha): 2

SQL montado com `+` (candidatos; confira se o que entra é constante ou dado de fora):
```
src/main/java/br/com/marealta/pousada/ReservationService.java:72:                            + "AND check_in < ? AND check_out > ? LIMIT 1")) {
src/main/java/br/com/marealta/pousada/ReservationService.java:87:                            + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,'PENDING',0) RETURNING id")) {
src/main/java/br/com/marealta/pousada/ReservationService.java:157:                            + "WHERE r.status IN ('CONFIRMED','CHECKED_IN','CHECKED_OUT') AND r.check_in <= ? AND r.check_out > ? "
src/main/java/br/com/marealta/pousada/ReservationService.java:158:                            + "ORDER BY r.room_number, r.id")) {
src/main/java/br/com/marealta/pousada/ReservationService.java:186:                            + "FROM reservations WHERE check_in >= ? AND check_in < ? AND amount_paid - COALESCE(refund, 0) > 0 "
src/main/java/br/com/marealta/pousada/ReservationService.java:187:                            + "ORDER BY check_in, id")) {
src/main/java/br/com/marealta/pousada/ReservationService.java:228:                "SELECT " + RES_COLS + " FROM reservations WHERE id = ?" + (forUpdate ? " FOR UPDATE" : ""))) {
src/main/java/br/com/marealta/pousada/ReservationService.java:244:        try (PreparedStatement ps = c.prepareStatement("UPDATE reservations SET " + setClause + " WHERE id = ?")) {
```
