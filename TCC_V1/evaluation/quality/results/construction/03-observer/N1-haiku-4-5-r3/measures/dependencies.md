# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 9 pacotes, 19 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.pousadamarealta` | 1 | 58 |
| `com.pousadamarealta.database` | 1 | 85 |
| `com.pousadamarealta.dto` | 5 | 72 |
| `com.pousadamarealta.model` | 5 | 206 |
| `com.pousadamarealta.notification` | 4 | 122 |
| `com.pousadamarealta.repository` | 3 | 307 |
| `com.pousadamarealta.route` | 4 | 303 |
| `com.pousadamarealta.service` | 2 | 359 |
| `com.pousadamarealta.util` | 2 | 67 |

## As dependências entre os pacotes do projeto

```
com.pousadamarealta -> com.pousadamarealta.database
com.pousadamarealta -> com.pousadamarealta.notification
com.pousadamarealta -> com.pousadamarealta.repository
com.pousadamarealta -> com.pousadamarealta.route
com.pousadamarealta -> com.pousadamarealta.service
com.pousadamarealta.notification -> com.pousadamarealta.model
com.pousadamarealta.notification -> com.pousadamarealta.repository
com.pousadamarealta.notification -> com.pousadamarealta.util
com.pousadamarealta.repository -> com.pousadamarealta.database
com.pousadamarealta.repository -> com.pousadamarealta.model
com.pousadamarealta.route -> com.pousadamarealta.dto
com.pousadamarealta.route -> com.pousadamarealta.model
com.pousadamarealta.route -> com.pousadamarealta.repository
com.pousadamarealta.route -> com.pousadamarealta.service
com.pousadamarealta.service -> com.pousadamarealta.dto
com.pousadamarealta.service -> com.pousadamarealta.model
com.pousadamarealta.service -> com.pousadamarealta.notification
com.pousadamarealta.service -> com.pousadamarealta.repository
com.pousadamarealta.service -> com.pousadamarealta.util
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.pousadamarealta` | 5 | 0 |
| `com.pousadamarealta.database` | 0 | 2 |
| `com.pousadamarealta.dto` | 0 | 2 |
| `com.pousadamarealta.model` | 0 | 4 |
| `com.pousadamarealta.notification` | 3 | 2 |
| `com.pousadamarealta.repository` | 2 | 4 |
| `com.pousadamarealta.route` | 4 | 1 |
| `com.pousadamarealta.service` | 5 | 2 |
| `com.pousadamarealta.util` | 0 | 2 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.pousadamarealta` | sim | sim |  |  |  |  |
| `com.pousadamarealta.database` |  | sim |  |  |  |  |
| `com.pousadamarealta.dto` |  |  |  |  |  |  |
| `com.pousadamarealta.model` |  |  |  |  |  |  |
| `com.pousadamarealta.notification` |  |  |  |  |  |  |
| `com.pousadamarealta.repository` |  | sim |  |  |  |  |
| `com.pousadamarealta.route` | sim |  |  |  |  |  |
| `com.pousadamarealta.service` |  | sim |  |  |  |  |
| `com.pousadamarealta.util` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  265 src/main/java/com/pousadamarealta/service/ReservationService.java
  196 src/main/java/com/pousadamarealta/route/ReservationRoutes.java
  183 src/main/java/com/pousadamarealta/repository/ReservationRepository.java
  121 src/main/java/com/pousadamarealta/model/Reservation.java
   94 src/main/java/com/pousadamarealta/service/ReportService.java
   85 src/main/java/com/pousadamarealta/database/Database.java
   73 src/main/java/com/pousadamarealta/repository/MessageRepository.java
   58 src/main/java/com/pousadamarealta/Main.java
   53 src/main/java/com/pousadamarealta/route/ReportRoutes.java
   51 src/main/java/com/pousadamarealta/repository/RoomRepository.java
```
