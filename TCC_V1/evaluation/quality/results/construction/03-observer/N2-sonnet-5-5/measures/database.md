# Banco: o esquema que o serviço cria e as conferências simples

Resultado: medido. O serviço subiu contra um banco vazio e criou 3 tabelas (o esquema inteiro em `measures/database-schema.sql`).

## Como o esquema é criado

- arquivos de esquema em src/main/resources: 0
- `CREATE TABLE` dentro do código Java: src/main/java/br/com/marealta/reservas/persistence/Database.java

## As tabelas

| tabela | colunas | NOT NULL | chave primária | chaves estrangeiras | CHECK | UNIQUE / EXCLUDE |
|---|---|---|---|---|---|---|
| `public.messages` | 5 | 5 | sim | 0 | 0 | 0 |
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

- messages.channel: text, sem CHECK
- reservations.rate_plan: text, sem CHECK
- reservations.status: text, sem CHECK
- rooms.type: text, sem CHECK

**Restrições que protegem uma regra** (UNIQUE, EXCLUDE, CHECK):

- `nenhuma`

## O código que fala com o banco

- arquivos com JDBC (`java.sql`/`javax.sql`): 4
- pool de conexões (HikariCP ou outro): 0 arquivos
- `DriverManager.getConnection`: 2 chamadas
- transações explícitas (`setAutoCommit(false)`): 1
- `FOR UPDATE` (trava de linha): 2

SQL montado com `+` (candidatos; confira se o que entra é constante ou dado de fora):
```
src/main/java/br/com/marealta/reservas/persistence/ReservationRepository.java:32:                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
src/main/java/br/com/marealta/reservas/persistence/ReservationRepository.java:73:        return findOne("SELECT " + COLUMNS + " FROM reservations WHERE id = ?", id);
src/main/java/br/com/marealta/reservas/persistence/ReservationRepository.java:77:        return findOne("SELECT " + COLUMNS + " FROM reservations WHERE id = ? FOR UPDATE", id);
src/main/java/br/com/marealta/reservas/persistence/ReservationRepository.java:84:                        + "AND check_in < ? AND check_out > ? LIMIT 1")) {
src/main/java/br/com/marealta/reservas/persistence/ReservationRepository.java:98:        return findMany("SELECT " + COLUMNS + " FROM reservations "
src/main/java/br/com/marealta/reservas/persistence/ReservationRepository.java:99:                + "WHERE status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT') AND check_in <= ? AND check_out > ? "
src/main/java/br/com/marealta/reservas/persistence/ReservationRepository.java:100:                + "ORDER BY room_number", night, night);
src/main/java/br/com/marealta/reservas/persistence/ReservationRepository.java:105:        return findMany("SELECT " + COLUMNS + " FROM reservations "
src/main/java/br/com/marealta/reservas/persistence/ReservationRepository.java:106:                + "WHERE check_in >= ? AND check_in < ? AND amount_paid - COALESCE(refund, 0) > 0 "
src/main/java/br/com/marealta/reservas/persistence/ReservationRepository.java:107:                + "ORDER BY check_in, id", month.atDay(1), month.plusMonths(1).atDay(1));
```
