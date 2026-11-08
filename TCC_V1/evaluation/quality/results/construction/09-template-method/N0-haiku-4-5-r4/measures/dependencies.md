# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 7 pacotes, 11 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.marealata` | 2 | 43 |
| `com.marealata.dao` | 3 | 287 |
| `com.marealata.database` | 1 | 109 |
| `com.marealata.handler` | 10 | 359 |
| `com.marealata.model` | 6 | 207 |
| `com.marealata.service` | 4 | 322 |
| `com.marealata.util` | 2 | 134 |

## As dependências entre os pacotes do projeto

```
com.marealata -> com.marealata.database
com.marealata -> com.marealata.handler
com.marealata.dao -> com.marealata.database
com.marealata.dao -> com.marealata.model
com.marealata.handler -> com.marealata.dao
com.marealata.handler -> com.marealata.model
com.marealata.handler -> com.marealata.service
com.marealata.service -> com.marealata.dao
com.marealata.service -> com.marealata.model
com.marealata.service -> com.marealata.util
com.marealata.util -> com.marealata.model
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.marealata` | 2 | 0 |
| `com.marealata.dao` | 2 | 2 |
| `com.marealata.database` | 0 | 2 |
| `com.marealata.handler` | 3 | 1 |
| `com.marealata.model` | 0 | 4 |
| `com.marealata.service` | 3 | 1 |
| `com.marealata.util` | 1 | 1 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.marealata` | sim |  |  |  |  | com.google |
| `com.marealata.dao` |  | sim |  |  |  |  |
| `com.marealata.database` |  | sim |  |  |  |  |
| `com.marealata.handler` | sim | sim |  |  |  |  |
| `com.marealata.model` |  |  |  |  |  | com.google.gson |
| `com.marealata.service` |  | sim |  |  |  |  |
| `com.marealata.util` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  202 src/main/java/com/marealata/handler/ReservationHandler.java
  186 src/main/java/com/marealata/dao/ReservationDao.java
  146 src/main/java/com/marealata/service/ReportService.java
  135 src/main/java/com/marealata/service/ReservationService.java
  109 src/main/java/com/marealata/database/Database.java
   97 src/main/java/com/marealata/handler/ReportHandler.java
   77 src/main/java/com/marealata/model/Reservation.java
   74 src/main/java/com/marealata/util/PriceCalculator.java
   62 src/main/java/com/marealata/model/ReservationDocument.java
   60 src/main/java/com/marealata/util/DocumentFormatter.java
```
