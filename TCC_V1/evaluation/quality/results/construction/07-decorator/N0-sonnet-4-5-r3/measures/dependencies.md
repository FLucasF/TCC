# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 11 pacotes, 21 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.pousadamarealta` | 1 | 106 |
| `com.pousadamarealta.controller` | 4 | 238 |
| `com.pousadamarealta.database` | 1 | 50 |
| `com.pousadamarealta.dto` | 6 | 473 |
| `com.pousadamarealta.exception` | 6 | 42 |
| `com.pousadamarealta.mapper` | 1 | 70 |
| `com.pousadamarealta.messaging` | 5 | 137 |
| `com.pousadamarealta.messaging.rules` | 3 | 87 |
| `com.pousadamarealta.model` | 8 | 306 |
| `com.pousadamarealta.repository` | 2 | 310 |
| `com.pousadamarealta.service` | 5 | 457 |

## As dependências entre os pacotes do projeto

```
com.pousadamarealta -> com.pousadamarealta.controller
com.pousadamarealta -> com.pousadamarealta.database
com.pousadamarealta -> com.pousadamarealta.dto
com.pousadamarealta -> com.pousadamarealta.exception
com.pousadamarealta -> com.pousadamarealta.repository
com.pousadamarealta -> com.pousadamarealta.service
com.pousadamarealta.controller -> com.pousadamarealta.dto
com.pousadamarealta.controller -> com.pousadamarealta.exception
com.pousadamarealta.controller -> com.pousadamarealta.mapper
com.pousadamarealta.controller -> com.pousadamarealta.model
com.pousadamarealta.controller -> com.pousadamarealta.service
com.pousadamarealta.mapper -> com.pousadamarealta.dto
com.pousadamarealta.mapper -> com.pousadamarealta.model
com.pousadamarealta.messaging -> com.pousadamarealta.model
com.pousadamarealta.messaging.rules -> com.pousadamarealta.messaging
com.pousadamarealta.repository -> com.pousadamarealta.model
com.pousadamarealta.service -> com.pousadamarealta.exception
com.pousadamarealta.service -> com.pousadamarealta.messaging
com.pousadamarealta.service -> com.pousadamarealta.messaging.rules
com.pousadamarealta.service -> com.pousadamarealta.model
com.pousadamarealta.service -> com.pousadamarealta.repository
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.pousadamarealta` | 6 | 0 |
| `com.pousadamarealta.controller` | 5 | 1 |
| `com.pousadamarealta.database` | 0 | 1 |
| `com.pousadamarealta.dto` | 0 | 3 |
| `com.pousadamarealta.exception` | 0 | 3 |
| `com.pousadamarealta.mapper` | 2 | 1 |
| `com.pousadamarealta.messaging` | 1 | 2 |
| `com.pousadamarealta.messaging.rules` | 1 | 1 |
| `com.pousadamarealta.model` | 0 | 5 |
| `com.pousadamarealta.repository` | 1 | 2 |
| `com.pousadamarealta.service` | 5 | 2 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.pousadamarealta` | sim | sim | sim |  | sim |  |
| `com.pousadamarealta.controller` | sim |  |  |  |  |  |
| `com.pousadamarealta.database` |  | sim |  |  |  |  |
| `com.pousadamarealta.dto` |  |  |  |  |  |  |
| `com.pousadamarealta.exception` |  |  |  |  |  |  |
| `com.pousadamarealta.mapper` |  |  |  |  |  |  |
| `com.pousadamarealta.messaging` |  |  |  |  |  |  |
| `com.pousadamarealta.messaging.rules` |  |  |  |  |  |  |
| `com.pousadamarealta.model` |  |  |  |  |  |  |
| `com.pousadamarealta.repository` |  | sim |  |  |  |  |
| `com.pousadamarealta.service` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  234 src/main/java/com/pousadamarealta/repository/ReservationRepository.java
  203 src/main/java/com/pousadamarealta/service/ReservationService.java
  151 src/main/java/com/pousadamarealta/model/Reservation.java
  150 src/main/java/com/pousadamarealta/dto/ReservationDTO.java
  136 src/main/java/com/pousadamarealta/controller/ReservationController.java
  112 src/main/java/com/pousadamarealta/dto/DocumentDTO.java
  106 src/main/java/com/pousadamarealta/Main.java
   89 src/main/java/com/pousadamarealta/service/ReportService.java
   76 src/main/java/com/pousadamarealta/repository/MessageRepository.java
   76 src/main/java/com/pousadamarealta/dto/CreateReservationRequest.java
```
