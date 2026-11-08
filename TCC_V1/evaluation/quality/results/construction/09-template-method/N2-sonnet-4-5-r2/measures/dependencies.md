# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 6 pacotes, 12 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `br.com.marealta` | 1 | 53 |
| `br.com.marealta.api` | 5 | 349 |
| `br.com.marealta.domain` | 7 | 323 |
| `br.com.marealta.report` | 5 | 345 |
| `br.com.marealta.repository` | 7 | 499 |
| `br.com.marealta.service` | 10 | 346 |

## As dependências entre os pacotes do projeto

```
br.com.marealta.api -> br.com.marealta.domain
br.com.marealta.api -> br.com.marealta.report
br.com.marealta.api -> br.com.marealta.repository
br.com.marealta.api -> br.com.marealta.service
br.com.marealta -> br.com.marealta.api
br.com.marealta -> br.com.marealta.repository
br.com.marealta -> br.com.marealta.service
br.com.marealta.report -> br.com.marealta.domain
br.com.marealta.report -> br.com.marealta.repository
br.com.marealta.repository -> br.com.marealta.domain
br.com.marealta.service -> br.com.marealta.domain
br.com.marealta.service -> br.com.marealta.repository
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `br.com.marealta` | 3 | 0 |
| `br.com.marealta.api` | 4 | 1 |
| `br.com.marealta.domain` | 0 | 4 |
| `br.com.marealta.report` | 2 | 1 |
| `br.com.marealta.repository` | 1 | 4 |
| `br.com.marealta.service` | 2 | 2 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `br.com.marealta` | sim | sim |  |  |  |  |
| `br.com.marealta.api` | sim |  | sim |  |  |  |
| `br.com.marealta.domain` |  |  |  |  |  |  |
| `br.com.marealta.report` |  |  |  |  |  |  |
| `br.com.marealta.repository` |  | sim |  |  | sim |  |
| `br.com.marealta.service` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  231 src/main/java/br/com/marealta/repository/ReservationRepositoryImpl.java
  196 src/main/java/br/com/marealta/api/RestApi.java
  182 src/main/java/br/com/marealta/service/ReservationService.java
  149 src/main/java/br/com/marealta/api/ApiMapper.java
  133 src/main/java/br/com/marealta/domain/Reservation.java
   89 src/main/java/br/com/marealta/repository/RoomRepositoryImpl.java
   88 src/main/java/br/com/marealta/report/TourismTaxReport.java
   86 src/main/java/br/com/marealta/report/RatePlanReport.java
   84 src/main/java/br/com/marealta/domain/Document.java
   81 src/main/java/br/com/marealta/repository/Database.java
```
