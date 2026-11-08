# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 8 pacotes, 14 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `br.com.maralta` | 1 | 82 |
| `br.com.maralta.controller` | 5 | 296 |
| `br.com.maralta.controller.dto` | 6 | 150 |
| `br.com.maralta.database` | 2 | 127 |
| `br.com.maralta.domain` | 7 | 306 |
| `br.com.maralta.domain.rateplan` | 8 | 309 |
| `br.com.maralta.repository` | 3 | 353 |
| `br.com.maralta.service` | 8 | 303 |

## As dependências entre os pacotes do projeto

```
br.com.maralta -> br.com.maralta.controller
br.com.maralta -> br.com.maralta.database
br.com.maralta -> br.com.maralta.repository
br.com.maralta -> br.com.maralta.service
br.com.maralta.controller -> br.com.maralta.controller.dto
br.com.maralta.controller -> br.com.maralta.domain
br.com.maralta.controller -> br.com.maralta.repository
br.com.maralta.controller -> br.com.maralta.service
br.com.maralta.controller.dto -> br.com.maralta.domain
br.com.maralta.domain.rateplan -> br.com.maralta.domain
br.com.maralta.repository -> br.com.maralta.domain
br.com.maralta.service -> br.com.maralta.domain
br.com.maralta.service -> br.com.maralta.domain.rateplan
br.com.maralta.service -> br.com.maralta.repository
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `br.com.maralta` | 4 | 0 |
| `br.com.maralta.controller` | 4 | 1 |
| `br.com.maralta.controller.dto` | 1 | 1 |
| `br.com.maralta.database` | 0 | 1 |
| `br.com.maralta.domain` | 0 | 5 |
| `br.com.maralta.domain.rateplan` | 1 | 1 |
| `br.com.maralta.repository` | 1 | 3 |
| `br.com.maralta.service` | 3 | 2 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `br.com.maralta` | sim | sim | sim |  |  |  |
| `br.com.maralta.controller` | sim |  |  |  |  |  |
| `br.com.maralta.controller.dto` |  |  |  |  |  |  |
| `br.com.maralta.database` |  | sim |  |  |  |  |
| `br.com.maralta.domain` |  |  |  |  |  |  |
| `br.com.maralta.domain.rateplan` |  |  |  |  |  |  |
| `br.com.maralta.repository` |  | sim |  |  |  |  |
| `br.com.maralta.service` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  236 src/main/java/br/com/maralta/service/ReservationService.java
  225 src/main/java/br/com/maralta/repository/ReservationRepository.java
  170 src/main/java/br/com/maralta/domain/Reservation.java
  109 src/main/java/br/com/maralta/controller/ReportController.java
   91 src/main/java/br/com/maralta/database/DatabaseInitializer.java
   86 src/main/java/br/com/maralta/controller/ReservationController.java
   82 src/main/java/br/com/maralta/Application.java
   69 src/main/java/br/com/maralta/repository/MessageRepository.java
   67 src/main/java/br/com/maralta/domain/rateplan/CorporateRatePlan.java
   59 src/main/java/br/com/maralta/repository/RoomRepository.java
```
