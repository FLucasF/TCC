# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 5 pacotes, 7 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.pousada` | 1 | 22 |
| `com.pousada.api` | 8 | 483 |
| `com.pousada.db` | 5 | 300 |
| `com.pousada.service` | 8 | 594 |
| `com.pousada.util` | 3 | 79 |

## As dependências entre os pacotes do projeto

```
com.pousada.api -> com.pousada.db
com.pousada.api -> com.pousada.service
com.pousada.api -> com.pousada.util
com.pousada -> com.pousada.api
com.pousada -> com.pousada.db
com.pousada.service -> com.pousada.db
com.pousada.service -> com.pousada.util
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.pousada` | 2 | 0 |
| `com.pousada.api` | 3 | 1 |
| `com.pousada.db` | 0 | 3 |
| `com.pousada.service` | 2 | 1 |
| `com.pousada.util` | 0 | 2 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.pousada` | sim |  |  |  |  |  |
| `com.pousada.api` | sim | sim | sim |  |  |  |
| `com.pousada.db` |  | sim | sim |  | sim |  |
| `com.pousada.service` |  | sim |  |  |  |  |
| `com.pousada.util` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  291 src/main/java/com/pousada/api/ReservationAPI.java
  185 src/main/java/com/pousada/service/ReservationService.java
  139 src/main/java/com/pousada/service/RatePlanCalculator.java
  133 src/main/java/com/pousada/api/ReportAPI.java
  111 src/main/java/com/pousada/db/Database.java
   96 src/main/java/com/pousada/service/Validator.java
   77 src/main/java/com/pousada/db/Reservation.java
   72 src/main/java/com/pousada/service/ReceiptService.java
   59 src/main/java/com/pousada/db/Receipt.java
   51 src/main/java/com/pousada/service/RoomService.java
```
