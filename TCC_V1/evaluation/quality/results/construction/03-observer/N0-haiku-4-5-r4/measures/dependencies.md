# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 7 pacotes, 13 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.pousadamarealta` | 1 | 61 |
| `com.pousadamarealta.api` | 1 | 328 |
| `com.pousadamarealta.database` | 1 | 100 |
| `com.pousadamarealta.exceptions` | 1 | 20 |
| `com.pousadamarealta.models` | 6 | 111 |
| `com.pousadamarealta.repositories` | 3 | 329 |
| `com.pousadamarealta.services` | 4 | 468 |

## As dependências entre os pacotes do projeto

```
com.pousadamarealta.api -> com.pousadamarealta.exceptions
com.pousadamarealta.api -> com.pousadamarealta.models
com.pousadamarealta.api -> com.pousadamarealta.repositories
com.pousadamarealta.api -> com.pousadamarealta.services
com.pousadamarealta -> com.pousadamarealta.api
com.pousadamarealta -> com.pousadamarealta.database
com.pousadamarealta -> com.pousadamarealta.repositories
com.pousadamarealta -> com.pousadamarealta.services
com.pousadamarealta.repositories -> com.pousadamarealta.database
com.pousadamarealta.repositories -> com.pousadamarealta.models
com.pousadamarealta.services -> com.pousadamarealta.exceptions
com.pousadamarealta.services -> com.pousadamarealta.models
com.pousadamarealta.services -> com.pousadamarealta.repositories
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.pousadamarealta` | 4 | 0 |
| `com.pousadamarealta.api` | 4 | 1 |
| `com.pousadamarealta.database` | 0 | 2 |
| `com.pousadamarealta.exceptions` | 0 | 2 |
| `com.pousadamarealta.models` | 0 | 3 |
| `com.pousadamarealta.repositories` | 2 | 3 |
| `com.pousadamarealta.services` | 3 | 2 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.pousadamarealta` | sim |  |  |  |  | com.google |
| `com.pousadamarealta.api` | sim |  |  |  |  | com.google |
| `com.pousadamarealta.database` |  | sim |  |  |  |  |
| `com.pousadamarealta.exceptions` |  |  |  |  |  |  |
| `com.pousadamarealta.models` |  |  |  |  |  |  |
| `com.pousadamarealta.repositories` |  | sim |  |  |  |  |
| `com.pousadamarealta.services` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  328 src/main/java/com/pousadamarealta/api/ApiRoutes.java
  223 src/main/java/com/pousadamarealta/repositories/ReservationRepository.java
  186 src/main/java/com/pousadamarealta/services/ReservationService.java
  145 src/main/java/com/pousadamarealta/services/NotificationService.java
  100 src/main/java/com/pousadamarealta/database/DatabaseConfig.java
   87 src/main/java/com/pousadamarealta/services/ReportService.java
   77 src/main/java/com/pousadamarealta/models/Reservation.java
   61 src/main/java/com/pousadamarealta/Application.java
   55 src/main/java/com/pousadamarealta/repositories/RoomRepository.java
   51 src/main/java/com/pousadamarealta/repositories/MessageRepository.java
```
