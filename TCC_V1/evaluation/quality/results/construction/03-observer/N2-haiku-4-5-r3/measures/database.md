# Banco: o esquema que o serviço cria e as conferências simples

Resultado: o serviço não subiu contra um banco vazio; o esquema abaixo é o que ficou (0 tabelas). O fim do log do serviço:
```
00:17:40.075 [main] INFO  c.p.m.i.database.DatabaseSetup - Creating database connection pool to jdbc:postgresql://db:5432/review_schema
00:17:40.106 [main] INFO  com.zaxxer.hikari.HikariDataSource - HikariPool-1 - Starting...
00:17:40.468 [main] INFO  com.zaxxer.hikari.pool.HikariPool - HikariPool-1 - Added connection org.postgresql.jdbc.PgConnection@31e5415e
00:17:40.474 [main] INFO  com.zaxxer.hikari.HikariDataSource - HikariPool-1 - Start completed.
00:17:40.475 [main] INFO  c.p.m.i.database.DatabaseSetup - Running database migrations
00:17:41.173 [main] ERROR com.pousada.marealta.Main - Failed to start application
org.flywaydb.core.api.FlywayException: Unsupported Database: PostgreSQL 18.6
	at org.flywaydb.core.internal.database.DatabaseTypeRegister.getDatabaseTypeForConnection(DatabaseTypeRegister.java:105)
	at org.flywaydb.core.internal.jdbc.JdbcConnectionFactory.<init>(JdbcConnectionFactory.java:73)
	at org.flywaydb.core.FlywayExecutor.execute(FlywayExecutor.java:134)
	at org.flywaydb.core.Flyway.migrate(Flyway.java:147)
	at com.pousada.marealta.infrastructure.database.DatabaseSetup.runMigrations(DatabaseSetup.java:44)
	at com.pousada.marealta.Main.main(Main.java:20)
```

## Como o esquema é criado

- arquivos de esquema em src/main/resources: 4
  - `src/main/resources/db/migration/V001__create_rooms_table.sql`
  - `src/main/resources/db/migration/V002__create_reservations_table.sql`
  - `src/main/resources/db/migration/V003__create_notifications_table.sql`
  - `src/main/resources/db/migration/V004__initial_room_data.sql`

## As tabelas

| tabela | colunas | NOT NULL | chave primária | chaves estrangeiras | CHECK | UNIQUE / EXCLUDE |
|---|---|---|---|---|---|---|

## As conferências

**Tabela sem chave primária:** nenhuma

**Dinheiro** (colunas com nome de valor: price, total, subtotal, discount, amount, refund, paid, rate, tax, fee, valor, preco, desconto; o certo é `numeric(p, 2)`):

- nenhuma coluna com nome de valor

**Chave estrangeira sem índice** (nenhum índice começando pelas colunas da chave):

- nenhuma

**Datas guardadas como texto** (colunas com nome de data em text ou varchar):

- nenhuma

**As colunas de situação e de tipo** (status, plano, tipo, canal) e se há CHECK sobre elas:

- nenhuma

**Restrições que protegem uma regra** (UNIQUE, EXCLUDE, CHECK):

- `nenhuma`

## O código que fala com o banco

- arquivos com JDBC (`java.sql`/`javax.sql`): 6
- pool de conexões (HikariCP ou outro): 1 arquivos
- `DriverManager.getConnection`: 0 chamadas
- transações explícitas (`setAutoCommit(false)`): 0
- `FOR UPDATE` (trava de linha): 0

SQL montado com `+` (candidatos; confira se o que entra é constante ou dado de fora):
```
```
