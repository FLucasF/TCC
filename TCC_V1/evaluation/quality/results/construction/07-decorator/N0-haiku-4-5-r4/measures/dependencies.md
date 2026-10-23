# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 6 pacotes, 9 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.pousadamarealta` | 1 | 22 |
| `com.pousadamarealta.api` | 8 | 529 |
| `com.pousadamarealta.database` | 1 | 101 |
| `com.pousadamarealta.model` | 3 | 82 |
| `com.pousadamarealta.repository` | 3 | 266 |
| `com.pousadamarealta.service` | 4 | 149 |

## As dependências entre os pacotes do projeto

```
com.pousadamarealta.api -> com.pousadamarealta.model
com.pousadamarealta.api -> com.pousadamarealta.repository
com.pousadamarealta.api -> com.pousadamarealta.service
com.pousadamarealta -> com.pousadamarealta.api
com.pousadamarealta -> com.pousadamarealta.database
com.pousadamarealta.repository -> com.pousadamarealta.database
com.pousadamarealta.repository -> com.pousadamarealta.model
com.pousadamarealta.service -> com.pousadamarealta.model
com.pousadamarealta.service -> com.pousadamarealta.repository
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.pousadamarealta` | 2 | 0 |
| `com.pousadamarealta.api` | 3 | 1 |
| `com.pousadamarealta.database` | 0 | 2 |
| `com.pousadamarealta.model` | 0 | 3 |
| `com.pousadamarealta.repository` | 2 | 2 |
| `com.pousadamarealta.service` | 2 | 1 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.pousadamarealta` | sim |  |  |  |  |  |
| `com.pousadamarealta.api` | sim | sim |  |  |  | com.google |
| `com.pousadamarealta.database` |  | sim |  |  |  |  |
| `com.pousadamarealta.model` |  |  |  |  |  |  |
| `com.pousadamarealta.repository` |  | sim |  |  |  |  |
| `com.pousadamarealta.service` |  | sim |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  351 src/main/java/com/pousadamarealta/api/ReservationsEndpoints.java
  171 src/main/java/com/pousadamarealta/repository/ReservationRepository.java
  124 src/main/java/com/pousadamarealta/api/ReportsEndpoints.java
  101 src/main/java/com/pousadamarealta/database/Database.java
   80 src/main/java/com/pousadamarealta/service/MessageService.java
   69 src/main/java/com/pousadamarealta/service/PricingService.java
   49 src/main/java/com/pousadamarealta/repository/MessageRepository.java
   46 src/main/java/com/pousadamarealta/repository/RoomRepository.java
   45 src/main/java/com/pousadamarealta/model/Reservation.java
   27 src/main/java/com/pousadamarealta/api/RoomsEndpoints.java
```
