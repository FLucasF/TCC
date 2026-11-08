# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 7 pacotes, 13 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.pousadamarealta` | 3 | 298 |
| `com.pousadamarealta.api` | 2 | 23 |
| `com.pousadamarealta.db` | 4 | 364 |
| `com.pousadamarealta.model` | 7 | 112 |
| `com.pousadamarealta.notification` | 4 | 110 |
| `com.pousadamarealta.service` | 2 | 289 |
| `com.pousadamarealta.util` | 3 | 81 |

## As dependências entre os pacotes do projeto

```
com.pousadamarealta -> com.pousadamarealta.api
com.pousadamarealta -> com.pousadamarealta.db
com.pousadamarealta -> com.pousadamarealta.model
com.pousadamarealta -> com.pousadamarealta.notification
com.pousadamarealta -> com.pousadamarealta.service
com.pousadamarealta.db -> com.pousadamarealta.model
com.pousadamarealta.notification -> com.pousadamarealta.db
com.pousadamarealta.notification -> com.pousadamarealta.model
com.pousadamarealta.notification -> com.pousadamarealta.util
com.pousadamarealta.service -> com.pousadamarealta.db
com.pousadamarealta.service -> com.pousadamarealta.model
com.pousadamarealta.service -> com.pousadamarealta.notification
com.pousadamarealta.service -> com.pousadamarealta.util
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.pousadamarealta` | 5 | 0 |
| `com.pousadamarealta.api` | 0 | 1 |
| `com.pousadamarealta.db` | 1 | 3 |
| `com.pousadamarealta.model` | 0 | 4 |
| `com.pousadamarealta.notification` | 3 | 2 |
| `com.pousadamarealta.service` | 4 | 1 |
| `com.pousadamarealta.util` | 0 | 2 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.pousadamarealta` | sim | sim |  |  |  | com.google |
| `com.pousadamarealta.api` |  |  |  |  |  |  |
| `com.pousadamarealta.db` |  | sim |  |  | sim |  |
| `com.pousadamarealta.model` |  |  |  |  |  |  |
| `com.pousadamarealta.notification` |  | sim |  |  |  |  |
| `com.pousadamarealta.service` |  | sim |  |  |  |  |
| `com.pousadamarealta.util` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  298 src/main/java/com/pousadamarealta/Main.java
  213 src/main/java/com/pousadamarealta/service/ReservationService.java
  169 src/main/java/com/pousadamarealta/db/ReservationRepository.java
  104 src/main/java/com/pousadamarealta/db/Database.java
   76 src/main/java/com/pousadamarealta/service/ReportService.java
   59 src/main/java/com/pousadamarealta/model/Reservation.java
   46 src/main/java/com/pousadamarealta/db/RoomRepository.java
   45 src/main/java/com/pousadamarealta/db/MessageRepository.java
   43 src/main/java/com/pousadamarealta/notification/HousekeepingNotifier.java
   34 src/main/java/com/pousadamarealta/notification/FinanceNotifier.java
```
