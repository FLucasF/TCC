# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 7 pacotes, 12 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.pousada` | 1 | 45 |
| `com.pousada.config` | 1 | 124 |
| `com.pousada.dao` | 3 | 309 |
| `com.pousada.handler` | 5 | 538 |
| `com.pousada.model` | 3 | 225 |
| `com.pousada.service` | 3 | 288 |
| `com.pousada.util` | 2 | 212 |

## As dependências entre os pacotes do projeto

```
com.pousada -> com.pousada.config
com.pousada -> com.pousada.handler
com.pousada.dao -> com.pousada.config
com.pousada.dao -> com.pousada.model
com.pousada.handler -> com.pousada.dao
com.pousada.handler -> com.pousada.model
com.pousada.handler -> com.pousada.service
com.pousada.handler -> com.pousada.util
com.pousada.service -> com.pousada.dao
com.pousada.service -> com.pousada.model
com.pousada.service -> com.pousada.util
com.pousada.util -> com.pousada.model
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.pousada` | 2 | 0 |
| `com.pousada.config` | 0 | 2 |
| `com.pousada.dao` | 2 | 2 |
| `com.pousada.handler` | 4 | 1 |
| `com.pousada.model` | 0 | 4 |
| `com.pousada.service` | 3 | 1 |
| `com.pousada.util` | 1 | 2 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.pousada` | sim |  |  |  |  |  |
| `com.pousada.config` |  | sim |  |  |  |  |
| `com.pousada.dao` |  | sim |  |  |  |  |
| `com.pousada.handler` | sim |  |  |  |  | com.google |
| `com.pousada.model` |  |  |  |  |  |  |
| `com.pousada.service` |  |  |  |  |  |  |
| `com.pousada.util` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  385 src/main/java/com/pousada/handler/ReservationHandler.java
  184 src/main/java/com/pousada/service/ReservationService.java
  183 src/main/java/com/pousada/dao/ReservationDAO.java
  147 src/main/java/com/pousada/model/Reservation.java
  135 src/main/java/com/pousada/util/DocumentFormatter.java
  124 src/main/java/com/pousada/config/DatabaseConfig.java
   82 src/main/java/com/pousada/service/ReportService.java
   77 src/main/java/com/pousada/util/MoneyUtil.java
   63 src/main/java/com/pousada/dao/RoomDAO.java
   63 src/main/java/com/pousada/dao/MessageDAO.java
```
