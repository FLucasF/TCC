# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 7 pacotes, 12 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.pousadamarealta` | 1 | 73 |
| `com.pousadamarealta.database` | 1 | 87 |
| `com.pousadamarealta.dto` | 2 | 103 |
| `com.pousadamarealta.handler` | 4 | 411 |
| `com.pousadamarealta.model` | 7 | 301 |
| `com.pousadamarealta.repository` | 3 | 379 |
| `com.pousadamarealta.service` | 9 | 477 |

## As dependências entre os pacotes do projeto

```
com.pousadamarealta -> com.pousadamarealta.database
com.pousadamarealta -> com.pousadamarealta.handler
com.pousadamarealta -> com.pousadamarealta.repository
com.pousadamarealta -> com.pousadamarealta.service
com.pousadamarealta.handler -> com.pousadamarealta.dto
com.pousadamarealta.handler -> com.pousadamarealta.model
com.pousadamarealta.handler -> com.pousadamarealta.repository
com.pousadamarealta.handler -> com.pousadamarealta.service
com.pousadamarealta.repository -> com.pousadamarealta.database
com.pousadamarealta.repository -> com.pousadamarealta.model
com.pousadamarealta.service -> com.pousadamarealta.model
com.pousadamarealta.service -> com.pousadamarealta.repository
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.pousadamarealta` | 4 | 0 |
| `com.pousadamarealta.database` | 0 | 2 |
| `com.pousadamarealta.dto` | 0 | 1 |
| `com.pousadamarealta.handler` | 4 | 1 |
| `com.pousadamarealta.model` | 0 | 3 |
| `com.pousadamarealta.repository` | 2 | 3 |
| `com.pousadamarealta.service` | 2 | 2 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.pousadamarealta` | sim |  | sim |  |  |  |
| `com.pousadamarealta.database` |  | sim |  |  |  |  |
| `com.pousadamarealta.dto` |  |  |  |  |  |  |
| `com.pousadamarealta.handler` | sim |  |  |  |  |  |
| `com.pousadamarealta.model` |  |  |  |  |  |  |
| `com.pousadamarealta.repository` |  | sim |  |  |  |  |
| `com.pousadamarealta.service` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  260 src/main/java/com/pousadamarealta/repository/ReservationRepository.java
  244 src/main/java/com/pousadamarealta/service/ReservationService.java
  232 src/main/java/com/pousadamarealta/handler/ReservationHandler.java
  193 src/main/java/com/pousadamarealta/service/ReportService.java
  156 src/main/java/com/pousadamarealta/model/Reservation.java
  114 src/main/java/com/pousadamarealta/handler/ReportHandler.java
   87 src/main/java/com/pousadamarealta/database/DatabaseManager.java
   76 src/main/java/com/pousadamarealta/dto/CreateReservationRequest.java
   73 src/main/java/com/pousadamarealta/App.java
   64 src/main/java/com/pousadamarealta/repository/MessageRepository.java
```
