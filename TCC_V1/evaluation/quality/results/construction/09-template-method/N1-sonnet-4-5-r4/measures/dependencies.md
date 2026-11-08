# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 9 pacotes, 21 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `br.com.marealta` | 1 | 134 |
| `br.com.marealta.api.controller` | 4 | 204 |
| `br.com.marealta.api.dto` | 6 | 137 |
| `br.com.marealta.database` | 1 | 90 |
| `br.com.marealta.domain` | 6 | 270 |
| `br.com.marealta.pricing` | 2 | 74 |
| `br.com.marealta.report` | 5 | 403 |
| `br.com.marealta.repository` | 3 | 413 |
| `br.com.marealta.service` | 10 | 262 |

## As dependências entre os pacotes do projeto

```
br.com.marealta.api.controller -> br.com.marealta.api.dto
br.com.marealta.api.controller -> br.com.marealta.domain
br.com.marealta.api.controller -> br.com.marealta.report
br.com.marealta.api.controller -> br.com.marealta.repository
br.com.marealta.api.controller -> br.com.marealta.service
br.com.marealta.api.dto -> br.com.marealta.domain
br.com.marealta -> br.com.marealta.api.controller
br.com.marealta -> br.com.marealta.api.dto
br.com.marealta -> br.com.marealta.database
br.com.marealta -> br.com.marealta.pricing
br.com.marealta -> br.com.marealta.report
br.com.marealta -> br.com.marealta.repository
br.com.marealta -> br.com.marealta.service
br.com.marealta.pricing -> br.com.marealta.domain
br.com.marealta.report -> br.com.marealta.domain
br.com.marealta.report -> br.com.marealta.repository
br.com.marealta.repository -> br.com.marealta.database
br.com.marealta.repository -> br.com.marealta.domain
br.com.marealta.service -> br.com.marealta.domain
br.com.marealta.service -> br.com.marealta.pricing
br.com.marealta.service -> br.com.marealta.repository
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `br.com.marealta` | 7 | 0 |
| `br.com.marealta.api.controller` | 5 | 1 |
| `br.com.marealta.api.dto` | 1 | 2 |
| `br.com.marealta.database` | 0 | 2 |
| `br.com.marealta.domain` | 0 | 6 |
| `br.com.marealta.pricing` | 1 | 2 |
| `br.com.marealta.report` | 2 | 2 |
| `br.com.marealta.repository` | 2 | 4 |
| `br.com.marealta.service` | 3 | 2 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `br.com.marealta` | sim |  | sim |  |  |  |
| `br.com.marealta.api.controller` | sim |  |  |  |  |  |
| `br.com.marealta.api.dto` |  |  |  |  |  |  |
| `br.com.marealta.database` |  | sim |  |  |  |  |
| `br.com.marealta.domain` |  |  |  |  |  |  |
| `br.com.marealta.pricing` |  |  |  |  |  |  |
| `br.com.marealta.report` |  |  |  |  |  |  |
| `br.com.marealta.repository` |  | sim |  |  |  |  |
| `br.com.marealta.service` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  270 src/main/java/br/com/marealta/repository/ReservationRepository.java
  156 src/main/java/br/com/marealta/domain/Reservation.java
  152 src/main/java/br/com/marealta/service/ReservationService.java
  134 src/main/java/br/com/marealta/App.java
  109 src/main/java/br/com/marealta/api/controller/ReservationController.java
  100 src/main/java/br/com/marealta/report/RatePlansReport.java
   95 src/main/java/br/com/marealta/report/RevenueReport.java
   94 src/main/java/br/com/marealta/report/TourismTaxReport.java
   90 src/main/java/br/com/marealta/database/Database.java
   82 src/main/java/br/com/marealta/report/OccupancyReport.java
```
