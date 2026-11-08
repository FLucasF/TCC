# Banco: o esquema que o serviço cria e as conferências simples

Resultado: o serviço não subiu contra um banco vazio; o esquema abaixo é o que ficou (0 tabelas). O fim do log do serviço:
```

PostgreSQL JDBC Driver 42.7.1
Found in: jar:file:/review/projeto/target/lib/postgresql-42.7.1.jar!/org/postgresql/Driver.class

The PgJDBC driver is not an executable Java program.

You must install it according to the JDBC driver installation instructions for your application / container / appserver, then use it by specifying a JDBC URL of the form 
    jdbc:postgresql://
or using an application specific method.

See the PgJDBC documentation: http://jdbc.postgresql.org/documentation/head/index.html

This command has had no effect.
```

## Como o esquema é criado

- arquivos de esquema em src/main/resources: 0
- `CREATE TABLE` dentro do código Java: src/main/java/pousada/repository/MessageRepository.java src/main/java/pousada/repository/ReservationRepository.java src/main/java/pousada/repository/RoomRepository.java

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

- arquivos com JDBC (`java.sql`/`javax.sql`): 5
- pool de conexões (HikariCP ou outro): 1 arquivos
- `DriverManager.getConnection`: 0 chamadas
- transações explícitas (`setAutoCommit(false)`): 0
- `FOR UPDATE` (trava de linha): 0

SQL montado com `+` (candidatos; confira se o que entra é constante ou dado de fora):
```
```
