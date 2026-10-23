# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 11 pacotes, 25 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.pousadamarealta` | 1 | 108 |
| `com.pousadamarealta.api.dto` | 6 | 86 |
| `com.pousadamarealta.api.handler` | 4 | 300 |
| `com.pousadamarealta.api.mapper` | 1 | 74 |
| `com.pousadamarealta.database` | 4 | 506 |
| `com.pousadamarealta.domain` | 8 | 268 |
| `com.pousadamarealta.messaging` | 4 | 125 |
| `com.pousadamarealta.messaging.rules` | 3 | 82 |
| `com.pousadamarealta.reports` | 2 | 89 |
| `com.pousadamarealta.service` | 4 | 349 |
| `com.pousadamarealta.service.exceptions` | 6 | 42 |

## As dependências entre os pacotes do projeto

```
com.pousadamarealta.api.handler -> com.pousadamarealta.api.dto
com.pousadamarealta.api.handler -> com.pousadamarealta.api.mapper
com.pousadamarealta.api.handler -> com.pousadamarealta.database
com.pousadamarealta.api.handler -> com.pousadamarealta.domain
com.pousadamarealta.api.handler -> com.pousadamarealta.reports
com.pousadamarealta.api.handler -> com.pousadamarealta.service
com.pousadamarealta.api.handler -> com.pousadamarealta.service.exceptions
com.pousadamarealta.api.mapper -> com.pousadamarealta.api.dto
com.pousadamarealta.api.mapper -> com.pousadamarealta.database
com.pousadamarealta.api.mapper -> com.pousadamarealta.domain
com.pousadamarealta -> com.pousadamarealta.api.handler
com.pousadamarealta -> com.pousadamarealta.database
com.pousadamarealta -> com.pousadamarealta.messaging
com.pousadamarealta -> com.pousadamarealta.messaging.rules
com.pousadamarealta -> com.pousadamarealta.reports
com.pousadamarealta -> com.pousadamarealta.service
com.pousadamarealta.database -> com.pousadamarealta.domain
com.pousadamarealta.messaging -> com.pousadamarealta.domain
com.pousadamarealta.messaging.rules -> com.pousadamarealta.messaging
com.pousadamarealta.reports -> com.pousadamarealta.database
com.pousadamarealta.reports -> com.pousadamarealta.domain
com.pousadamarealta.service -> com.pousadamarealta.database
com.pousadamarealta.service -> com.pousadamarealta.domain
com.pousadamarealta.service -> com.pousadamarealta.messaging
com.pousadamarealta.service -> com.pousadamarealta.service.exceptions
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.pousadamarealta` | 6 | 0 |
| `com.pousadamarealta.api.dto` | 0 | 2 |
| `com.pousadamarealta.api.handler` | 7 | 1 |
| `com.pousadamarealta.api.mapper` | 3 | 1 |
| `com.pousadamarealta.database` | 1 | 5 |
| `com.pousadamarealta.domain` | 0 | 6 |
| `com.pousadamarealta.messaging` | 1 | 3 |
| `com.pousadamarealta.messaging.rules` | 1 | 1 |
| `com.pousadamarealta.reports` | 2 | 2 |
| `com.pousadamarealta.service` | 4 | 2 |
| `com.pousadamarealta.service.exceptions` | 0 | 2 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.pousadamarealta` | sim |  | sim |  |  |  |
| `com.pousadamarealta.api.dto` |  |  | sim |  |  |  |
| `com.pousadamarealta.api.handler` | sim |  |  |  |  |  |
| `com.pousadamarealta.api.mapper` |  |  |  |  |  |  |
| `com.pousadamarealta.database` |  | sim |  |  | sim |  |
| `com.pousadamarealta.domain` |  |  |  |  |  |  |
| `com.pousadamarealta.messaging` |  |  |  |  |  |  |
| `com.pousadamarealta.messaging.rules` |  |  |  |  |  |  |
| `com.pousadamarealta.reports` |  |  |  |  |  |  |
| `com.pousadamarealta.service` |  |  |  |  |  |  |
| `com.pousadamarealta.service.exceptions` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  253 src/main/java/com/pousadamarealta/database/ReservationDao.java
  210 src/main/java/com/pousadamarealta/service/ReservationService.java
  189 src/main/java/com/pousadamarealta/api/handler/ReservationHandler.java
  141 src/main/java/com/pousadamarealta/domain/Reservation.java
  108 src/main/java/com/pousadamarealta/Application.java
  105 src/main/java/com/pousadamarealta/database/Database.java
   99 src/main/java/com/pousadamarealta/service/PricingCalculator.java
   81 src/main/java/com/pousadamarealta/database/MessageDao.java
   74 src/main/java/com/pousadamarealta/api/mapper/ApiMapper.java
   67 src/main/java/com/pousadamarealta/database/RoomDao.java
```
