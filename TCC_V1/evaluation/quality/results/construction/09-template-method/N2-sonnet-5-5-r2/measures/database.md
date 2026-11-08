# Banco: o esquema que o serviço cria e as conferências simples

Resultado: medido. O serviço subiu contra um banco vazio e criou 2 tabelas (o esquema inteiro em `measures/database-schema.sql`).

## Como o esquema é criado

- arquivos de esquema em src/main/resources: 0
- `CREATE TABLE` dentro do código Java: src/main/java/pousada/repo/Database.java

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
- transações explícitas (`setAutoCommit(false)`): 0
- `FOR UPDATE` (trava de linha): 0

SQL montado com `+` (candidatos; confira se o que entra é constante ou dado de fora):
```
src/main/java/pousada/repo/JdbcReservationRepository.java:35:                + " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) RETURNING id";
src/main/java/pousada/repo/JdbcReservationRepository.java:64:        return query("SELECT " + COLUMNS + " FROM reservations WHERE id=?", ps -> ps.setLong(1, id))
src/main/java/pousada/repo/JdbcReservationRepository.java:71:                + " AND check_in < ? AND check_out > ? LIMIT 1";
src/main/java/pousada/repo/JdbcReservationRepository.java:86:        return query("SELECT " + COLUMNS + " FROM reservations WHERE check_in >= ? AND check_in <= ?"
src/main/java/pousada/repo/JdbcReservationRepository.java:87:                + " ORDER BY check_in, id", ps -> {
src/main/java/pousada/repo/JdbcReservationRepository.java:95:        return query("SELECT " + COLUMNS + " FROM reservations WHERE status IN ('CONFIRMED','CHECKED_IN','CHECKED_OUT')"
src/main/java/pousada/repo/JdbcReservationRepository.java:96:                + " AND check_in <= ? AND check_out > ? ORDER BY room_number, id", ps -> {
```
