# Comportamento: o roteiro do cenário 09

Resultado: não deu para testar (o serviço não subiu contra um banco vazio; o fim do log: 	at com.pousada.Application.main(Application.java:8) Caused by: org.postgresql.util.PSQLException: Cannot commit when autoCommit is enabled. 	at org.postgresql.jdbc.PgConnection.commit(PgConnection.java:998) 	at com.pousada.infrastructure.Database.initialize(Database.java:70) 	... 1 more ).
