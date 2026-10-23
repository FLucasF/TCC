# Banco: o esquema que o serviço cria e as conferências simples

Resultado: medido. O serviço subiu contra um banco vazio e criou 3 tabelas (o esquema inteiro em `measures/database-schema.sql`).

## Como o esquema é criado

- arquivos de esquema em src/main/resources: 0
- `CREATE TABLE` dentro do código Java: src/main/java/br/com/marealta/Db.java

## As tabelas

| tabela | colunas | NOT NULL | chave primária | chaves estrangeiras | CHECK | UNIQUE / EXCLUDE |
|---|---|---|---|---|---|---|
| `public.messages` | 8 | 7 | sim | 0 | 0 | 0 |
| `public.reservations` | 16 | 15 | sim | 1 | 0 | 0 |
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

- messages.channel: character varying, sem CHECK
- messages.status: character varying, sem CHECK
- reservations.rate_plan: character varying, sem CHECK
- reservations.status: character varying, sem CHECK
- rooms.type: character varying, sem CHECK

**Restrições que protegem uma regra** (UNIQUE, EXCLUDE, CHECK):

- `nenhuma`

## O código que fala com o banco

- arquivos com JDBC (`java.sql`/`javax.sql`): 4
- pool de conexões (HikariCP ou outro): 0 arquivos
- `DriverManager.getConnection`: 1 chamadas
- transações explícitas (`setAutoCommit(false)`): 1
- `FOR UPDATE` (trava de linha): 1

SQL montado com `+` (candidatos; confira se o que entra é constante ou dado de fora):
```
src/main/java/br/com/marealta/domain/Reports.java:32:                            + "WHERE r.status IN ('CONFIRMED','CHECKED_IN','CHECKED_OUT') AND r.check_in <= ? AND r.check_out > ? "
src/main/java/br/com/marealta/domain/Reports.java:33:                            + "ORDER BY r.room_number, r.id")) {
src/main/java/br/com/marealta/domain/Reports.java:55:                            + "FROM reservations WHERE check_in >= ? AND check_in < ? AND amount_paid - COALESCE(refund, 0) > 0 "
src/main/java/br/com/marealta/domain/Reports.java:56:                            + "ORDER BY check_in, id")) {
src/main/java/br/com/marealta/domain/ReservationService.java:60:                        + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,'PENDING',0) RETURNING " + COLS)) {
src/main/java/br/com/marealta/domain/ReservationService.java:167:                + "AND status <> 'CANCELLED' AND check_in < ? AND check_out > ? LIMIT 1")) {
src/main/java/br/com/marealta/domain/ReservationService.java:179:                "SELECT " + COLS + " FROM reservations WHERE id = ?" + (forUpdate ? " FOR UPDATE" : ""))) {
```
