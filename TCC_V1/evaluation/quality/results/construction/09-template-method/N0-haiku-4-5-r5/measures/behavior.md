# Comportamento: o roteiro do cenário 09

Resultado: não deu para testar (o serviço não subiu contra um banco vazio; o fim do log: Caused by: org.postgresql.util.PSQLException: Cannot commit when autoCommit is enabled. 	at org.postgresql.jdbc.PgConnection.commit(PgConnection.java:982) 	at com.pousadamarealta.db.Database.createTables(Database.java:74) 	at com.pousadamarealta.db.Database.init(Database.java:22) 	... 1 more ).
