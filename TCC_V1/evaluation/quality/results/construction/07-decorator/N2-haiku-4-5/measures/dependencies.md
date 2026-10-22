# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 5 pacotes, 9 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.pousadamarealta` | 1 | 52 |
| `com.pousadamarealta.api` | 1 | 328 |
| `com.pousadamarealta.db` | 4 | 374 |
| `com.pousadamarealta.model` | 3 | 149 |
| `com.pousadamarealta.service` | 4 | 417 |

## As dependências entre os pacotes do projeto

```
com.pousadamarealta.api -> com.pousadamarealta.db
com.pousadamarealta.api -> com.pousadamarealta.model
com.pousadamarealta.api -> com.pousadamarealta.service
com.pousadamarealta -> com.pousadamarealta.api
com.pousadamarealta -> com.pousadamarealta.db
com.pousadamarealta -> com.pousadamarealta.service
com.pousadamarealta.db -> com.pousadamarealta.model
com.pousadamarealta.service -> com.pousadamarealta.db
com.pousadamarealta.service -> com.pousadamarealta.model
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.pousadamarealta` | 3 | 0 |
| `com.pousadamarealta.api` | 3 | 1 |
| `com.pousadamarealta.db` | 1 | 3 |
| `com.pousadamarealta.model` | 0 | 3 |
| `com.pousadamarealta.service` | 2 | 2 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.pousadamarealta` | sim |  |  |  |  |  |
| `com.pousadamarealta.api` | sim |  | sim |  |  |  |
| `com.pousadamarealta.db` |  | sim |  |  |  |  |
| `com.pousadamarealta.model` |  |  |  |  |  |  |
| `com.pousadamarealta.service` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  328 src/main/java/com/pousadamarealta/api/ApiRouter.java
  188 src/main/java/com/pousadamarealta/service/ReservationService.java
  168 src/main/java/com/pousadamarealta/db/ReservationRepository.java
   96 src/main/java/com/pousadamarealta/model/Reservation.java
   96 src/main/java/com/pousadamarealta/db/Database.java
   93 src/main/java/com/pousadamarealta/service/ReportService.java
   69 src/main/java/com/pousadamarealta/service/PricingService.java
   67 src/main/java/com/pousadamarealta/service/MessageService.java
   57 src/main/java/com/pousadamarealta/db/MessageRepository.java
   53 src/main/java/com/pousadamarealta/db/RoomRepository.java
```
