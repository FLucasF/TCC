# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 8 pacotes, 18 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.pousadamarealta` | 2 | 132 |
| `com.pousadamarealta.api.controller` | 4 | 249 |
| `com.pousadamarealta.api.dto` | 2 | 117 |
| `com.pousadamarealta.database` | 4 | 494 |
| `com.pousadamarealta.domain` | 8 | 317 |
| `com.pousadamarealta.exception` | 6 | 42 |
| `com.pousadamarealta.messaging` | 5 | 149 |
| `com.pousadamarealta.service` | 4 | 401 |

## As dependências entre os pacotes do projeto

```
com.pousadamarealta.api.controller -> com.pousadamarealta.api.dto
com.pousadamarealta.api.controller -> com.pousadamarealta.domain
com.pousadamarealta.api.controller -> com.pousadamarealta.exception
com.pousadamarealta.api.controller -> com.pousadamarealta.messaging
com.pousadamarealta.api.controller -> com.pousadamarealta.service
com.pousadamarealta -> com.pousadamarealta.api.controller
com.pousadamarealta -> com.pousadamarealta.api.dto
com.pousadamarealta -> com.pousadamarealta.database
com.pousadamarealta -> com.pousadamarealta.exception
com.pousadamarealta -> com.pousadamarealta.messaging
com.pousadamarealta -> com.pousadamarealta.service
com.pousadamarealta.database -> com.pousadamarealta.domain
com.pousadamarealta.messaging -> com.pousadamarealta.database
com.pousadamarealta.messaging -> com.pousadamarealta.domain
com.pousadamarealta.service -> com.pousadamarealta.database
com.pousadamarealta.service -> com.pousadamarealta.domain
com.pousadamarealta.service -> com.pousadamarealta.exception
com.pousadamarealta.service -> com.pousadamarealta.messaging
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.pousadamarealta` | 6 | 0 |
| `com.pousadamarealta.api.controller` | 5 | 1 |
| `com.pousadamarealta.api.dto` | 0 | 2 |
| `com.pousadamarealta.database` | 1 | 3 |
| `com.pousadamarealta.domain` | 0 | 4 |
| `com.pousadamarealta.exception` | 0 | 3 |
| `com.pousadamarealta.messaging` | 2 | 3 |
| `com.pousadamarealta.service` | 4 | 2 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.pousadamarealta` | sim |  | sim |  |  |  |
| `com.pousadamarealta.api.controller` | sim |  |  |  |  |  |
| `com.pousadamarealta.api.dto` |  |  | sim |  |  |  |
| `com.pousadamarealta.database` |  | sim |  |  |  |  |
| `com.pousadamarealta.domain` |  |  |  |  |  |  |
| `com.pousadamarealta.exception` |  |  |  |  |  |  |
| `com.pousadamarealta.messaging` |  |  |  |  |  |  |
| `com.pousadamarealta.service` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  243 src/main/java/com/pousadamarealta/database/ReservationDAO.java
  228 src/main/java/com/pousadamarealta/service/ReservationService.java
  156 src/main/java/com/pousadamarealta/domain/Reservation.java
  132 src/main/java/com/pousadamarealta/Application.java
  130 src/main/java/com/pousadamarealta/api/controller/ReservationController.java
  124 src/main/java/com/pousadamarealta/database/DatabaseManager.java
   93 src/main/java/com/pousadamarealta/api/dto/CreateReservationRequest.java
   85 src/main/java/com/pousadamarealta/service/ReportService.java
   69 src/main/java/com/pousadamarealta/domain/Message.java
   69 src/main/java/com/pousadamarealta/database/MessageDAO.java
```
