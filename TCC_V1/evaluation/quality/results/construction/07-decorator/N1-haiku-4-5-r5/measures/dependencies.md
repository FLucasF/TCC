# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 6 pacotes, 8 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.pousadamarealta` | 1 | 20 |
| `com.pousadamarealta.api` | 4 | 325 |
| `com.pousadamarealta.database` | 1 | 102 |
| `com.pousadamarealta.model` | 3 | 85 |
| `com.pousadamarealta.service` | 6 | 510 |
| `com.pousadamarealta.util` | 2 | 111 |

## As dependências entre os pacotes do projeto

```
com.pousadamarealta.api -> com.pousadamarealta.model
com.pousadamarealta.api -> com.pousadamarealta.service
com.pousadamarealta.api -> com.pousadamarealta.util
com.pousadamarealta -> com.pousadamarealta.api
com.pousadamarealta -> com.pousadamarealta.database
com.pousadamarealta.service -> com.pousadamarealta.database
com.pousadamarealta.service -> com.pousadamarealta.model
com.pousadamarealta.service -> com.pousadamarealta.util
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.pousadamarealta` | 2 | 0 |
| `com.pousadamarealta.api` | 3 | 1 |
| `com.pousadamarealta.database` | 0 | 2 |
| `com.pousadamarealta.model` | 0 | 2 |
| `com.pousadamarealta.service` | 3 | 1 |
| `com.pousadamarealta.util` | 0 | 2 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.pousadamarealta` | sim |  |  |  |  |  |
| `com.pousadamarealta.api` | sim |  | sim |  |  |  |
| `com.pousadamarealta.database` |  | sim |  |  | sim |  |
| `com.pousadamarealta.model` |  |  | sim |  |  |  |
| `com.pousadamarealta.service` |  | sim |  |  |  |  |
| `com.pousadamarealta.util` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  325 src/main/java/com/pousadamarealta/api/HotelAPI.java
  195 src/main/java/com/pousadamarealta/service/ReservationService.java
  104 src/main/java/com/pousadamarealta/service/ReportService.java
  102 src/main/java/com/pousadamarealta/database/Database.java
   88 src/main/java/com/pousadamarealta/service/MessageService.java
   74 src/main/java/com/pousadamarealta/service/DocumentService.java
   60 src/main/java/com/pousadamarealta/util/PricingUtil.java
   51 src/main/java/com/pousadamarealta/util/ValidationUtil.java
   49 src/main/java/com/pousadamarealta/service/RoomService.java
   49 src/main/java/com/pousadamarealta/model/Reservation.java
```
