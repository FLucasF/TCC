# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 1 pacotes, 0 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `br.com.marealta` | 34 | 1220 |

## As dependências entre os pacotes do projeto

Nenhuma (um pacote só, ou pacotes que não se usam).

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `br.com.marealta` | sim | sim | sim |  |  |  |

## As maiores classes (linhas, src/main)

```
  164 src/main/java/br/com/marealta/Api.java
  122 src/main/java/br/com/marealta/JdbcReservationRepository.java
  104 src/main/java/br/com/marealta/Reservation.java
   82 src/main/java/br/com/marealta/ReservationService.java
   78 src/main/java/br/com/marealta/Views.java
   69 src/main/java/br/com/marealta/Database.java
   54 src/main/java/br/com/marealta/DocumentFormat.java
   53 src/main/java/br/com/marealta/IdentityTypes.java
   49 src/main/java/br/com/marealta/ReportService.java
   48 src/main/java/br/com/marealta/JdbcMessageRepository.java
```
