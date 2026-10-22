# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 7 pacotes, 10 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.pousadamareaita` | 1 | 49 |
| `com.pousadamareaita.api` | 6 | 437 |
| `com.pousadamareaita.database` | 1 | 100 |
| `com.pousadamareaita.exception` | 1 | 20 |
| `com.pousadamareaita.model` | 4 | 338 |
| `com.pousadamareaita.service` | 4 | 622 |
| `com.pousadamareaita.util` | 1 | 54 |

## As dependências entre os pacotes do projeto

```
com.pousadamareaita.api -> com.pousadamareaita.database
com.pousadamareaita.api -> com.pousadamareaita.exception
com.pousadamareaita.api -> com.pousadamareaita.model
com.pousadamareaita.api -> com.pousadamareaita.service
com.pousadamareaita -> com.pousadamareaita.api
com.pousadamareaita -> com.pousadamareaita.database
com.pousadamareaita.service -> com.pousadamareaita.database
com.pousadamareaita.service -> com.pousadamareaita.exception
com.pousadamareaita.service -> com.pousadamareaita.model
com.pousadamareaita.service -> com.pousadamareaita.util
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.pousadamareaita` | 2 | 0 |
| `com.pousadamareaita.api` | 4 | 1 |
| `com.pousadamareaita.database` | 0 | 3 |
| `com.pousadamareaita.exception` | 0 | 2 |
| `com.pousadamareaita.model` | 0 | 2 |
| `com.pousadamareaita.service` | 4 | 1 |
| `com.pousadamareaita.util` | 0 | 1 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.pousadamareaita` | sim |  | sim |  |  |  |
| `com.pousadamareaita.api` | sim |  | sim |  |  |  |
| `com.pousadamareaita.database` |  | sim |  |  |  |  |
| `com.pousadamareaita.exception` |  |  |  |  |  |  |
| `com.pousadamareaita.model` |  |  | sim |  |  |  |
| `com.pousadamareaita.service` |  | sim |  |  |  |  |
| `com.pousadamareaita.util` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  364 src/main/java/com/pousadamareaita/service/ReservationService.java
  256 src/main/java/com/pousadamareaita/api/ReservationHandler.java
  161 src/main/java/com/pousadamareaita/model/Reservation.java
  145 src/main/java/com/pousadamareaita/service/DocumentService.java
  145 src/main/java/com/pousadamareaita/api/ReportHandler.java
  102 src/main/java/com/pousadamareaita/model/Document.java
  100 src/main/java/com/pousadamareaita/database/Database.java
   58 src/main/java/com/pousadamareaita/service/MessageService.java
   55 src/main/java/com/pousadamareaita/service/RoomService.java
   54 src/main/java/com/pousadamareaita/util/MoneyUtils.java
```
