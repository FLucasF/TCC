# Banco: o esquema que o serviço cria e as conferências simples

Resultado: o serviço não subiu contra um banco vazio; o esquema abaixo é o que ficou (3 tabelas). O fim do log do serviço:
```
[main] INFO com.zaxxer.hikari.HikariDataSource - HikariPool-1 - Starting...
[main] INFO com.zaxxer.hikari.pool.HikariPool - HikariPool-1 - Added connection org.postgresql.jdbc.PgConnection@76a4d6c
[main] INFO com.zaxxer.hikari.HikariDataSource - HikariPool-1 - Start completed.
Exception in thread "main" java.lang.RuntimeException: Failed to create tables
	at com.pousada.database.Database.createTables(Database.java:76)
	at com.pousada.database.Database.initialize(Database.java:29)
	at com.pousada.Application.main(Application.java:18)
Caused by: org.postgresql.util.PSQLException: Cannot commit when autoCommit is enabled.
	at org.postgresql.jdbc.PgConnection.commit(PgConnection.java:998)
	at com.zaxxer.hikari.pool.ProxyConnection.commit(ProxyConnection.java:378)
	at com.zaxxer.hikari.pool.HikariProxyConnection.commit(HikariProxyConnection.java)
	at com.pousada.database.Database.createTables(Database.java:74)
	... 2 more
```

## Como o esquema é criado

- arquivos de esquema em src/main/resources: 0
- `CREATE TABLE` dentro do código Java: src/main/java/com/pousada/database/Database.java

## As tabelas

| tabela | colunas | NOT NULL | chave primária | chaves estrangeiras | CHECK | UNIQUE / EXCLUDE |
|---|---|---|---|---|---|---|
| `public.messages` | 6 | 5 | sim | 0 | 0 | 0 |
| `public.reservations` | 16 | 15 | sim | 1 | 0 | 0 |
| `public.rooms` | 4 | 4 | sim | 0 | 0 | 0 |

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

- reservations (room_number) -> rooms

**Datas guardadas como texto** (colunas com nome de data em text ou varchar):

- nenhuma

**As colunas de situação e de tipo** (status, plano, tipo, canal) e se há CHECK sobre elas:

- messages.channel: character varying, sem CHECK
- reservations.rate_plan: character varying, sem CHECK
- reservations.status: character varying, sem CHECK
- rooms.type: character varying, sem CHECK

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
src/main/java/com/pousada/database/Database.java:85:              + "WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE number = 101)");
src/main/java/com/pousada/database/Database.java:89:              + "WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE number = 102)");
src/main/java/com/pousada/database/Database.java:93:              + "WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE number = 201)");
src/main/java/com/pousada/database/Database.java:97:              + "WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE number = 202)");
src/main/java/com/pousada/database/Database.java:101:              + "WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE number = 301)");
src/main/java/com/pousada/database/ReservationRepository.java:34:            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'PENDING', 0) "
src/main/java/com/pousada/database/ReservationRepository.java:68:            + "FROM reservations WHERE id = ?";
src/main/java/com/pousada/database/ReservationRepository.java:132:            + "FROM reservations "
src/main/java/com/pousada/database/ReservationRepository.java:133:            + "WHERE room_number = ? AND NOT status = ? "
src/main/java/com/pousada/database/ReservationRepository.java:134:            + "AND check_in < ? AND check_out > ?";
src/main/java/com/pousada/database/ReservationRepository.java:158:            + "FROM reservations "
src/main/java/com/pousada/database/ReservationRepository.java:159:            + "WHERE (status = ? OR status = ? OR status = ?) "
src/main/java/com/pousada/database/ReservationRepository.java:160:            + "AND check_in <= ? AND check_out > ? "
src/main/java/com/pousada/database/ReservationRepository.java:161:            + "ORDER BY room_number";
src/main/java/com/pousada/database/ReservationRepository.java:186:            + "FROM reservations "
src/main/java/com/pousada/database/ReservationRepository.java:187:            + "WHERE EXTRACT(YEAR FROM check_in) = ? AND EXTRACT(MONTH FROM check_in) = ? "
src/main/java/com/pousada/database/ReservationRepository.java:188:            + "AND status != ? "
src/main/java/com/pousada/database/ReservationRepository.java:189:            + "ORDER BY check_in, id";
```
