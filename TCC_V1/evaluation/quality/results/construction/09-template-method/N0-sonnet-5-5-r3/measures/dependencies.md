# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 6 pacotes, 13 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `br.com.marealta` | 2 | 67 |
| `br.com.marealta.db` | 5 | 335 |
| `br.com.marealta.domain` | 10 | 207 |
| `br.com.marealta.report` | 10 | 327 |
| `br.com.marealta.service` | 4 | 188 |
| `br.com.marealta.web` | 2 | 138 |

## As dependências entre os pacotes do projeto

```
br.com.marealta -> br.com.marealta.db
br.com.marealta -> br.com.marealta.report
br.com.marealta -> br.com.marealta.service
br.com.marealta -> br.com.marealta.web
br.com.marealta.db -> br.com.marealta.domain
br.com.marealta.report -> br.com.marealta.db
br.com.marealta.report -> br.com.marealta.domain
br.com.marealta.report -> br.com.marealta.service
br.com.marealta.service -> br.com.marealta.db
br.com.marealta.service -> br.com.marealta.domain
br.com.marealta.web -> br.com.marealta.domain
br.com.marealta.web -> br.com.marealta.report
br.com.marealta.web -> br.com.marealta.service
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `br.com.marealta` | 4 | 0 |
| `br.com.marealta.db` | 1 | 3 |
| `br.com.marealta.domain` | 0 | 4 |
| `br.com.marealta.report` | 3 | 2 |
| `br.com.marealta.service` | 2 | 3 |
| `br.com.marealta.web` | 3 | 1 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `br.com.marealta` | sim |  |  |  |  |  |
| `br.com.marealta.db` |  | sim |  |  | sim |  |
| `br.com.marealta.domain` |  |  | sim |  |  |  |
| `br.com.marealta.report` |  | sim |  |  |  |  |
| `br.com.marealta.service` |  | sim |  |  |  |  |
| `br.com.marealta.web` | sim |  | sim |  |  |  |

## As maiores classes (linhas, src/main)

```
  141 src/main/java/br/com/marealta/service/ReservationService.java
  129 src/main/java/br/com/marealta/db/ReservationRepository.java
  118 src/main/java/br/com/marealta/db/Database.java
   69 src/main/java/br/com/marealta/web/RequestParser.java
   69 src/main/java/br/com/marealta/web/Api.java
   48 src/main/java/br/com/marealta/domain/Reservation.java
   47 src/main/java/br/com/marealta/db/RoomRepository.java
   46 src/main/java/br/com/marealta/domain/Receipt.java
   46 src/main/java/br/com/marealta/App.java
   43 src/main/java/br/com/marealta/report/TourismTaxReport.java
```
