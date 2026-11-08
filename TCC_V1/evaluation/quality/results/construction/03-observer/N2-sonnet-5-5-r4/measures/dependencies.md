# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 6 pacotes, 11 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `br.com.marealta.pousada` | 1 | 45 |
| `br.com.marealta.pousada.domain` | 19 | 498 |
| `br.com.marealta.pousada.notification` | 6 | 120 |
| `br.com.marealta.pousada.persistence` | 3 | 255 |
| `br.com.marealta.pousada.report` | 2 | 60 |
| `br.com.marealta.pousada.web` | 4 | 213 |

## As dependências entre os pacotes do projeto

```
br.com.marealta.pousada -> br.com.marealta.pousada.domain
br.com.marealta.pousada -> br.com.marealta.pousada.notification
br.com.marealta.pousada -> br.com.marealta.pousada.persistence
br.com.marealta.pousada -> br.com.marealta.pousada.web
br.com.marealta.pousada.notification -> br.com.marealta.pousada.domain
br.com.marealta.pousada.persistence -> br.com.marealta.pousada.domain
br.com.marealta.pousada.persistence -> br.com.marealta.pousada.notification
br.com.marealta.pousada.report -> br.com.marealta.pousada.domain
br.com.marealta.pousada.web -> br.com.marealta.pousada.domain
br.com.marealta.pousada.web -> br.com.marealta.pousada.notification
br.com.marealta.pousada.web -> br.com.marealta.pousada.report
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `br.com.marealta.pousada` | 4 | 0 |
| `br.com.marealta.pousada.domain` | 0 | 5 |
| `br.com.marealta.pousada.notification` | 1 | 3 |
| `br.com.marealta.pousada.persistence` | 2 | 1 |
| `br.com.marealta.pousada.report` | 1 | 1 |
| `br.com.marealta.pousada.web` | 3 | 1 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `br.com.marealta.pousada` | sim |  |  |  |  |  |
| `br.com.marealta.pousada.domain` |  |  |  |  |  |  |
| `br.com.marealta.pousada.notification` |  |  |  |  |  |  |
| `br.com.marealta.pousada.persistence` |  | sim |  |  |  |  |
| `br.com.marealta.pousada.report` |  |  |  |  |  |  |
| `br.com.marealta.pousada.web` | sim |  | sim |  |  |  |

## As maiores classes (linhas, src/main)

```
  129 src/main/java/br/com/marealta/pousada/persistence/JdbcReservationRepository.java
  100 src/main/java/br/com/marealta/pousada/domain/Reservation.java
   99 src/main/java/br/com/marealta/pousada/domain/ReservationService.java
   94 src/main/java/br/com/marealta/pousada/web/RequestParser.java
   85 src/main/java/br/com/marealta/pousada/web/Api.java
   69 src/main/java/br/com/marealta/pousada/persistence/Database.java
   57 src/main/java/br/com/marealta/pousada/persistence/JdbcMessageLog.java
   45 src/main/java/br/com/marealta/pousada/Main.java
   37 src/main/java/br/com/marealta/pousada/notification/HousekeepingNotice.java
   37 src/main/java/br/com/marealta/pousada/notification/FinanceNotice.java
```
