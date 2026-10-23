# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 7 pacotes, 11 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.pousadamarealta` | 1 | 26 |
| `com.pousadamarealta.api` | 1 | 285 |
| `com.pousadamarealta.dao` | 3 | 300 |
| `com.pousadamarealta.db` | 1 | 100 |
| `com.pousadamarealta.models` | 9 | 87 |
| `com.pousadamarealta.service` | 4 | 307 |
| `com.pousadamarealta.util` | 3 | 78 |

## As dependências entre os pacotes do projeto

```
com.pousadamarealta.api -> com.pousadamarealta.dao
com.pousadamarealta.api -> com.pousadamarealta.models
com.pousadamarealta.api -> com.pousadamarealta.service
com.pousadamarealta -> com.pousadamarealta.api
com.pousadamarealta -> com.pousadamarealta.db
com.pousadamarealta.dao -> com.pousadamarealta.db
com.pousadamarealta.dao -> com.pousadamarealta.models
com.pousadamarealta.service -> com.pousadamarealta.dao
com.pousadamarealta.service -> com.pousadamarealta.models
com.pousadamarealta.service -> com.pousadamarealta.util
com.pousadamarealta.util -> com.pousadamarealta.models
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.pousadamarealta` | 2 | 0 |
| `com.pousadamarealta.api` | 3 | 1 |
| `com.pousadamarealta.dao` | 2 | 2 |
| `com.pousadamarealta.db` | 0 | 2 |
| `com.pousadamarealta.models` | 0 | 4 |
| `com.pousadamarealta.service` | 3 | 1 |
| `com.pousadamarealta.util` | 1 | 1 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.pousadamarealta` | sim |  |  |  |  |  |
| `com.pousadamarealta.api` | sim | sim | sim |  |  |  |
| `com.pousadamarealta.dao` |  | sim |  |  |  |  |
| `com.pousadamarealta.db` |  | sim |  |  |  |  |
| `com.pousadamarealta.models` |  |  |  |  |  |  |
| `com.pousadamarealta.service` |  | sim |  |  |  |  |
| `com.pousadamarealta.util` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  285 src/main/java/com/pousadamarealta/api/ApiRoutes.java
  180 src/main/java/com/pousadamarealta/dao/ReservationDAO.java
  129 src/main/java/com/pousadamarealta/service/ReservationService.java
  100 src/main/java/com/pousadamarealta/db/Database.java
   71 src/main/java/com/pousadamarealta/dao/MessageDAO.java
   69 src/main/java/com/pousadamarealta/service/ReceiptFormatter.java
   59 src/main/java/com/pousadamarealta/service/ReportService.java
   50 src/main/java/com/pousadamarealta/service/MessageService.java
   49 src/main/java/com/pousadamarealta/dao/RoomDAO.java
   47 src/main/java/com/pousadamarealta/util/PricingCalculator.java
```
