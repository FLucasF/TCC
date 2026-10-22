# Banco: o esquema que o serviço cria e as conferências simples

Resultado: medido. O serviço subiu contra um banco vazio e criou 2 tabelas (o esquema inteiro em `measures/database-schema.sql`).

## Como o esquema é criado

- arquivos de esquema em src/main/resources: 0
- `CREATE TABLE` dentro do código Java: src/main/java/br/com/marealta/infra/Db.java

## As tabelas

| tabela | colunas | NOT NULL | chave primária | chaves estrangeiras | CHECK | UNIQUE / EXCLUDE |
|---|---|---|---|---|---|---|
| `public.messages` | 5 | 5 | sim | 0 | 0 | 0 |
| `public.reservations` | 16 | 15 | sim | 0 | 0 | 0 |

## As conferências

**Tabela sem chave primária:** nenhuma

**Dinheiro** (colunas com nome de valor: price, total, subtotal, discount, amount, refund, paid, rate, tax, fee, valor, preco, desconto; o certo é `numeric(p, 2)`):

- `reservations.amount_paid`: numeric(12,2), ok
- `reservations.discount`: numeric(12,2), ok
- `reservations.refund`: numeric(12,2), ok
- `reservations.subtotal`: numeric(12,2), ok
- `reservations.total`: numeric(12,2), ok

**Chave estrangeira sem índice** (nenhum índice começando pelas colunas da chave):

- nenhuma

**Datas guardadas como texto** (colunas com nome de data em text ou varchar):

- nenhuma

**As colunas de situação e de tipo** (status, plano, tipo, canal) e se há CHECK sobre elas:

- messages.channel: text, sem CHECK
- reservations.rate_plan: text, sem CHECK
- reservations.status: text, sem CHECK

**Restrições que protegem uma regra** (UNIQUE, EXCLUDE, CHECK):

- `nenhuma`

## O código que fala com o banco

- arquivos com JDBC (`java.sql`/`javax.sql`): 4
- pool de conexões (HikariCP ou outro): 1 arquivos
- `DriverManager.getConnection`: 0 chamadas
- transações explícitas (`setAutoCommit(false)`): 1
- `FOR UPDATE` (trava de linha): 1

SQL montado com `+` (candidatos; confira se o que entra é constante ou dado de fora):
```
src/main/java/br/com/marealta/infra/ReservationRepository.java:35:                        + "AND check_in < ? AND check_out > ? LIMIT 1")) {
src/main/java/br/com/marealta/infra/ReservationRepository.java:49:                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
src/main/java/br/com/marealta/infra/ReservationRepository.java:86:        return findOne(connection, "SELECT " + COLUMNS + " FROM reservations WHERE id = ?", id);
src/main/java/br/com/marealta/infra/ReservationRepository.java:91:        return findOne(connection, "SELECT " + COLUMNS + " FROM reservations WHERE id = ? FOR UPDATE", id);
src/main/java/br/com/marealta/infra/ReservationRepository.java:96:        return findMany(connection, "SELECT " + COLUMNS + " FROM reservations WHERE status IN " + PAID_STATUSES
src/main/java/br/com/marealta/infra/ReservationRepository.java:97:                + " AND check_in <= ? AND check_out > ? ORDER BY room_number, id", night, night);
src/main/java/br/com/marealta/infra/ReservationRepository.java:103:        return findMany(connection, "SELECT " + COLUMNS + " FROM reservations WHERE check_in >= ? AND check_in < ? "
src/main/java/br/com/marealta/infra/ReservationRepository.java:104:                + "AND amount_paid - COALESCE(refund, 0) > 0 ORDER BY check_in, id", from, until);
```
