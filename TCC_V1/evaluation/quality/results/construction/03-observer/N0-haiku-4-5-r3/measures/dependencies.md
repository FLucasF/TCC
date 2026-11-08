# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 6 pacotes, 12 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `br.marealta` | 1 | 56 |
| `br.marealta.api` | 6 | 333 |
| `br.marealta.db` | 4 | 401 |
| `br.marealta.model` | 7 | 279 |
| `br.marealta.service` | 7 | 484 |
| `br.marealta.util` | 3 | 113 |

## As dependências entre os pacotes do projeto

```
br.marealta.api -> br.marealta.db
br.marealta.api -> br.marealta.model
br.marealta.api -> br.marealta.service
br.marealta.api -> br.marealta.util
br.marealta -> br.marealta.api
br.marealta -> br.marealta.db
br.marealta -> br.marealta.service
br.marealta.db -> br.marealta.model
br.marealta.service -> br.marealta.db
br.marealta.service -> br.marealta.model
br.marealta.service -> br.marealta.util
br.marealta.util -> br.marealta.model
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `br.marealta` | 3 | 0 |
| `br.marealta.api` | 4 | 1 |
| `br.marealta.db` | 1 | 3 |
| `br.marealta.model` | 0 | 4 |
| `br.marealta.service` | 3 | 2 |
| `br.marealta.util` | 1 | 2 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `br.marealta` | sim |  |  |  |  |  |
| `br.marealta.api` | sim |  |  |  |  |  |
| `br.marealta.db` |  | sim |  |  |  |  |
| `br.marealta.model` |  |  |  |  |  |  |
| `br.marealta.service` |  |  |  |  |  |  |
| `br.marealta.util` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  221 src/main/java/br/marealta/api/ReservationController.java
  201 src/main/java/br/marealta/service/ReservationService.java
  178 src/main/java/br/marealta/db/ReservationDAO.java
  169 src/main/java/br/marealta/model/Reservation.java
  104 src/main/java/br/marealta/db/Database.java
   86 src/main/java/br/marealta/service/DefaultNotificationHandler.java
   81 src/main/java/br/marealta/service/ReportService.java
   65 src/main/java/br/marealta/api/ReportController.java
   60 src/main/java/br/marealta/util/PricingCalculator.java
   60 src/main/java/br/marealta/db/RoomDAO.java
```
