# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 10 pacotes, 23 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `br.com.pousadamarealta` | 1 | 71 |
| `br.com.pousadamarealta.api` | 1 | 56 |
| `br.com.pousadamarealta.api.controller` | 4 | 315 |
| `br.com.pousadamarealta.api.dto` | 6 | 147 |
| `br.com.pousadamarealta.domain.exception` | 6 | 42 |
| `br.com.pousadamarealta.domain.model` | 8 | 322 |
| `br.com.pousadamarealta.domain.rateplan` | 9 | 333 |
| `br.com.pousadamarealta.domain.repository` | 3 | 365 |
| `br.com.pousadamarealta.domain.service` | 4 | 314 |
| `br.com.pousadamarealta.infrastructure.database` | 2 | 108 |

## As dependências entre os pacotes do projeto

```
br.com.pousadamarealta.api -> br.com.pousadamarealta.api.dto
br.com.pousadamarealta.api -> br.com.pousadamarealta.domain.exception
br.com.pousadamarealta.api.controller -> br.com.pousadamarealta.api.dto
br.com.pousadamarealta.api.controller -> br.com.pousadamarealta.domain.exception
br.com.pousadamarealta.api.controller -> br.com.pousadamarealta.domain.model
br.com.pousadamarealta.api.controller -> br.com.pousadamarealta.domain.rateplan
br.com.pousadamarealta.api.controller -> br.com.pousadamarealta.domain.repository
br.com.pousadamarealta.api.controller -> br.com.pousadamarealta.domain.service
br.com.pousadamarealta.api.controller -> br.com.pousadamarealta.infrastructure.database
br.com.pousadamarealta.api.dto -> br.com.pousadamarealta.domain.model
br.com.pousadamarealta -> br.com.pousadamarealta.api
br.com.pousadamarealta -> br.com.pousadamarealta.api.controller
br.com.pousadamarealta -> br.com.pousadamarealta.domain.service
br.com.pousadamarealta -> br.com.pousadamarealta.infrastructure.database
br.com.pousadamarealta.domain.rateplan -> br.com.pousadamarealta.domain.exception
br.com.pousadamarealta.domain.rateplan -> br.com.pousadamarealta.domain.model
br.com.pousadamarealta.domain.repository -> br.com.pousadamarealta.domain.model
br.com.pousadamarealta.domain.service -> br.com.pousadamarealta.domain.exception
br.com.pousadamarealta.domain.service -> br.com.pousadamarealta.domain.model
br.com.pousadamarealta.domain.service -> br.com.pousadamarealta.domain.rateplan
br.com.pousadamarealta.domain.service -> br.com.pousadamarealta.domain.repository
br.com.pousadamarealta.infrastructure.database -> br.com.pousadamarealta.domain.model
br.com.pousadamarealta.infrastructure.database -> br.com.pousadamarealta.domain.repository
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `br.com.pousadamarealta` | 4 | 0 |
| `br.com.pousadamarealta.api` | 2 | 1 |
| `br.com.pousadamarealta.api.controller` | 7 | 1 |
| `br.com.pousadamarealta.api.dto` | 1 | 2 |
| `br.com.pousadamarealta.domain.exception` | 0 | 4 |
| `br.com.pousadamarealta.domain.model` | 0 | 6 |
| `br.com.pousadamarealta.domain.rateplan` | 2 | 2 |
| `br.com.pousadamarealta.domain.repository` | 1 | 3 |
| `br.com.pousadamarealta.domain.service` | 4 | 2 |
| `br.com.pousadamarealta.infrastructure.database` | 2 | 2 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `br.com.pousadamarealta` | sim | sim | sim |  |  |  |
| `br.com.pousadamarealta.api` | sim |  | sim |  |  |  |
| `br.com.pousadamarealta.api.controller` | sim | sim |  |  |  |  |
| `br.com.pousadamarealta.api.dto` |  |  |  |  |  |  |
| `br.com.pousadamarealta.domain.exception` |  |  |  |  |  |  |
| `br.com.pousadamarealta.domain.model` |  |  |  |  |  |  |
| `br.com.pousadamarealta.domain.rateplan` |  |  |  |  |  |  |
| `br.com.pousadamarealta.domain.repository` |  | sim |  |  |  |  |
| `br.com.pousadamarealta.domain.service` |  |  |  |  |  |  |
| `br.com.pousadamarealta.infrastructure.database` |  | sim |  |  | sim |  |

## As maiores classes (linhas, src/main)

```
  225 src/main/java/br/com/pousadamarealta/domain/repository/ReservationRepository.java
  184 src/main/java/br/com/pousadamarealta/api/controller/ReservationController.java
  165 src/main/java/br/com/pousadamarealta/domain/model/Reservation.java
  151 src/main/java/br/com/pousadamarealta/domain/service/ReservationService.java
   86 src/main/java/br/com/pousadamarealta/domain/service/ReportService.java
   73 src/main/java/br/com/pousadamarealta/domain/repository/RoomRepository.java
   73 src/main/java/br/com/pousadamarealta/api/controller/ReportController.java
   71 src/main/java/br/com/pousadamarealta/Main.java
   71 src/main/java/br/com/pousadamarealta/infrastructure/database/DatabaseInitializer.java
   67 src/main/java/br/com/pousadamarealta/domain/repository/MessageRepository.java
```
