# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 1 pacotes, 0 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `br.com.mare.pousada` | 35 | 1289 |

## As dependências entre os pacotes do projeto

Nenhuma (um pacote só, ou pacotes que não se usam).

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `br.com.mare.pousada` | sim | sim | sim |  |  |  |

## As maiores classes (linhas, src/main)

```
  195 src/main/java/br/com/mare/pousada/Api.java
  111 src/main/java/br/com/mare/pousada/JdbcReservationRepository.java
  104 src/main/java/br/com/mare/pousada/Reservation.java
   81 src/main/java/br/com/mare/pousada/ReservationService.java
   79 src/main/java/br/com/mare/pousada/IdentityKinds.java
   70 src/main/java/br/com/mare/pousada/JdbcDatabase.java
   53 src/main/java/br/com/mare/pousada/JdbcMessageRepository.java
   49 src/main/java/br/com/mare/pousada/Document.java
   46 src/main/java/br/com/mare/pousada/Reports.java
   42 src/main/java/br/com/mare/pousada/DocumentStyle.java
```
