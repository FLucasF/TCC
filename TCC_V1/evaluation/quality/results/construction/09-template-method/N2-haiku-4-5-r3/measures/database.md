# Banco: o esquema que o serviço cria e as conferências simples

Resultado: o serviço não subiu contra um banco vazio; o esquema abaixo é o que ficou (3 tabelas). O fim do log do serviço:
```
05:17:00.780 [main] INFO com.zaxxer.hikari.pool.HikariPool -- HikariPool-1 - Added connection org.postgresql.jdbc.PgConnection@318ba8c8
05:17:00.785 [main] INFO com.zaxxer.hikari.HikariDataSource -- HikariPool-1 - Start completed.
05:17:00.786 [main] INFO com.marealta.db.Database -- Database connection pool initialized
05:17:00.818 [main] INFO com.marealta.db.Database -- Database schema created successfully
05:17:00.885 [HikariPool-1 housekeeper] DEBUG com.zaxxer.hikari.pool.HikariPool -- HikariPool-1 - Before cleanup stats (total=1, active=0, idle=1, waiting=0)
05:17:00.887 [HikariPool-1 housekeeper] DEBUG com.zaxxer.hikari.pool.HikariPool -- HikariPool-1 - After cleanup  stats (total=1, active=0, idle=1, waiting=0)
05:17:00.897 [HikariPool-1 connection adder] DEBUG com.zaxxer.hikari.pool.HikariPool -- HikariPool-1 - Added connection org.postgresql.jdbc.PgConnection@32646ef4
Exception in thread "main" java.lang.ExceptionInInitializerError
	at com.marealta.Application.main(Application.java:45)
Caused by: java.lang.IllegalArgumentException: Pattern includes reserved character: '#'
	at java.base/java.time.format.DateTimeFormatterBuilder.parsePattern(DateTimeFormatterBuilder.java:2045)
	at java.base/java.time.format.DateTimeFormatterBuilder.appendPattern(DateTimeFormatterBuilder.java:1899)
	at java.base/java.time.format.DateTimeFormatter.ofPattern(DateTimeFormatter.java:569)
	at com.marealta.service.ReceiptService.<clinit>(ReceiptService.java:11)
	... 1 more
```

## Como o esquema é criado

- arquivos de esquema em src/main/resources: 0
- `CREATE TABLE` dentro do código Java: src/main/java/com/marealta/db/Database.java

## As tabelas

| tabela | colunas | NOT NULL | chave primária | chaves estrangeiras | CHECK | UNIQUE / EXCLUDE |
|---|---|---|---|---|---|---|
| `public.messages` | 5 | 5 | sim | 0 | 0 | 0 |
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
```
