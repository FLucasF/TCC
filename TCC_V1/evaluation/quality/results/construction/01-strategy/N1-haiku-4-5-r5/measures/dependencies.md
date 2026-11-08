# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 6 pacotes, 11 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.pousadamarealta` | 1 | 294 |
| `com.pousadamarealta.api` | 6 | 241 |
| `com.pousadamarealta.database` | 4 | 401 |
| `com.pousadamarealta.models` | 3 | 96 |
| `com.pousadamarealta.rateplans` | 10 | 342 |
| `com.pousadamarealta.services` | 3 | 301 |

## As dependências entre os pacotes do projeto

```
com.pousadamarealta.api -> com.pousadamarealta.models
com.pousadamarealta -> com.pousadamarealta.api
com.pousadamarealta -> com.pousadamarealta.database
com.pousadamarealta -> com.pousadamarealta.models
com.pousadamarealta -> com.pousadamarealta.rateplans
com.pousadamarealta -> com.pousadamarealta.services
com.pousadamarealta.database -> com.pousadamarealta.models
com.pousadamarealta.rateplans -> com.pousadamarealta.models
com.pousadamarealta.services -> com.pousadamarealta.database
com.pousadamarealta.services -> com.pousadamarealta.models
com.pousadamarealta.services -> com.pousadamarealta.rateplans
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.pousadamarealta` | 5 | 0 |
| `com.pousadamarealta.api` | 1 | 1 |
| `com.pousadamarealta.database` | 1 | 2 |
| `com.pousadamarealta.models` | 0 | 5 |
| `com.pousadamarealta.rateplans` | 1 | 2 |
| `com.pousadamarealta.services` | 3 | 1 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.pousadamarealta` | sim | sim | sim |  |  |  |
| `com.pousadamarealta.api` |  |  | sim |  |  |  |
| `com.pousadamarealta.database` |  | sim |  |  | sim |  |
| `com.pousadamarealta.models` |  |  |  |  |  |  |
| `com.pousadamarealta.rateplans` |  |  |  |  |  |  |
| `com.pousadamarealta.services` |  | sim |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  294 src/main/java/com/pousadamarealta/Main.java
  182 src/main/java/com/pousadamarealta/database/ReservationRepository.java
  180 src/main/java/com/pousadamarealta/services/ReservationService.java
   90 src/main/java/com/pousadamarealta/database/Database.java
   74 src/main/java/com/pousadamarealta/api/ReservationResponse.java
   73 src/main/java/com/pousadamarealta/services/ReportService.java
   69 src/main/java/com/pousadamarealta/database/MessageRepository.java
   64 src/main/java/com/pousadamarealta/api/ReceiptResponse.java
   62 src/main/java/com/pousadamarealta/models/Reservation.java
   60 src/main/java/com/pousadamarealta/database/RoomRepository.java
```
