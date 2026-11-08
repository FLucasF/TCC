# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 7 pacotes, 14 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.pousadamarealta` | 1 | 40 |
| `com.pousadamarealta.handler` | 7 | 577 |
| `com.pousadamarealta.infrastructure` | 1 | 102 |
| `com.pousadamarealta.model` | 6 | 268 |
| `com.pousadamarealta.repository` | 3 | 305 |
| `com.pousadamarealta.service` | 2 | 179 |
| `com.pousadamarealta.util` | 2 | 104 |

## As dependências entre os pacotes do projeto

```
com.pousadamarealta -> com.pousadamarealta.handler
com.pousadamarealta -> com.pousadamarealta.infrastructure
com.pousadamarealta.handler -> com.pousadamarealta.infrastructure
com.pousadamarealta.handler -> com.pousadamarealta.model
com.pousadamarealta.handler -> com.pousadamarealta.repository
com.pousadamarealta.handler -> com.pousadamarealta.service
com.pousadamarealta.handler -> com.pousadamarealta.util
com.pousadamarealta.repository -> com.pousadamarealta.infrastructure
com.pousadamarealta.repository -> com.pousadamarealta.model
com.pousadamarealta.service -> com.pousadamarealta.infrastructure
com.pousadamarealta.service -> com.pousadamarealta.model
com.pousadamarealta.service -> com.pousadamarealta.repository
com.pousadamarealta.service -> com.pousadamarealta.util
com.pousadamarealta.util -> com.pousadamarealta.model
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.pousadamarealta` | 2 | 0 |
| `com.pousadamarealta.handler` | 5 | 1 |
| `com.pousadamarealta.infrastructure` | 0 | 4 |
| `com.pousadamarealta.model` | 0 | 4 |
| `com.pousadamarealta.repository` | 2 | 2 |
| `com.pousadamarealta.service` | 4 | 1 |
| `com.pousadamarealta.util` | 1 | 2 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.pousadamarealta` | sim |  |  |  |  |  |
| `com.pousadamarealta.handler` | sim |  |  |  |  |  |
| `com.pousadamarealta.infrastructure` |  | sim |  |  | sim |  |
| `com.pousadamarealta.model` |  |  |  |  |  |  |
| `com.pousadamarealta.repository` |  | sim |  |  |  |  |
| `com.pousadamarealta.service` |  |  |  |  |  |  |
| `com.pousadamarealta.util` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  276 src/main/java/com/pousadamarealta/handler/ReservationHandler.java
  203 src/main/java/com/pousadamarealta/handler/ReportHandler.java
  188 src/main/java/com/pousadamarealta/repository/ReservationRepository.java
  173 src/main/java/com/pousadamarealta/model/Reservation.java
  165 src/main/java/com/pousadamarealta/service/ReservationService.java
  102 src/main/java/com/pousadamarealta/infrastructure/Database.java
   66 src/main/java/com/pousadamarealta/repository/MessageRepository.java
   59 src/main/java/com/pousadamarealta/util/PriceCalculator.java
   51 src/main/java/com/pousadamarealta/repository/RoomRepository.java
   50 src/main/java/com/pousadamarealta/handler/MessageHandler.java
```
