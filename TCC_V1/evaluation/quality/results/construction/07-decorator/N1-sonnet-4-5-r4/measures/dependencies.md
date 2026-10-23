# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 7 pacotes, 13 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.pousadamarealta` | 1 | 74 |
| `com.pousadamarealta.controller` | 4 | 388 |
| `com.pousadamarealta.database` | 1 | 76 |
| `com.pousadamarealta.messaging` | 7 | 193 |
| `com.pousadamarealta.model` | 9 | 447 |
| `com.pousadamarealta.repository` | 3 | 326 |
| `com.pousadamarealta.service` | 8 | 324 |

## As dependências entre os pacotes do projeto

```
com.pousadamarealta -> com.pousadamarealta.controller
com.pousadamarealta -> com.pousadamarealta.database
com.pousadamarealta -> com.pousadamarealta.repository
com.pousadamarealta -> com.pousadamarealta.service
com.pousadamarealta.controller -> com.pousadamarealta.model
com.pousadamarealta.controller -> com.pousadamarealta.repository
com.pousadamarealta.controller -> com.pousadamarealta.service
com.pousadamarealta.messaging -> com.pousadamarealta.model
com.pousadamarealta.repository -> com.pousadamarealta.database
com.pousadamarealta.repository -> com.pousadamarealta.model
com.pousadamarealta.service -> com.pousadamarealta.messaging
com.pousadamarealta.service -> com.pousadamarealta.model
com.pousadamarealta.service -> com.pousadamarealta.repository
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.pousadamarealta` | 4 | 0 |
| `com.pousadamarealta.controller` | 3 | 1 |
| `com.pousadamarealta.database` | 0 | 2 |
| `com.pousadamarealta.messaging` | 1 | 1 |
| `com.pousadamarealta.model` | 0 | 4 |
| `com.pousadamarealta.repository` | 2 | 3 |
| `com.pousadamarealta.service` | 3 | 2 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.pousadamarealta` | sim |  | sim |  |  |  |
| `com.pousadamarealta.controller` | sim |  |  |  |  |  |
| `com.pousadamarealta.database` |  | sim |  |  | sim |  |
| `com.pousadamarealta.messaging` |  |  |  |  |  |  |
| `com.pousadamarealta.model` |  |  |  |  |  |  |
| `com.pousadamarealta.repository` |  | sim |  |  |  |  |
| `com.pousadamarealta.service` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  243 src/main/java/com/pousadamarealta/controller/ReservationController.java
  225 src/main/java/com/pousadamarealta/repository/ReservationRepository.java
  217 src/main/java/com/pousadamarealta/service/ReservationService.java
  170 src/main/java/com/pousadamarealta/model/Reservation.java
  115 src/main/java/com/pousadamarealta/model/ReservationDocument.java
   77 src/main/java/com/pousadamarealta/service/ReportService.java
   76 src/main/java/com/pousadamarealta/database/DatabaseManager.java
   75 src/main/java/com/pousadamarealta/controller/ReportController.java
   74 src/main/java/com/pousadamarealta/Main.java
   73 src/main/java/com/pousadamarealta/repository/MessageRepository.java
```
