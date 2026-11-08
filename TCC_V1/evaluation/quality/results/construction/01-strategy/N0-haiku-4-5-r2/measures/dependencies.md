# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 6 pacotes, 9 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.pousada` | 1 | 53 |
| `com.pousada.database` | 1 | 99 |
| `com.pousada.domain` | 7 | 230 |
| `com.pousada.handler` | 4 | 444 |
| `com.pousada.service` | 6 | 597 |
| `com.pousada.util` | 3 | 57 |

## As dependências entre os pacotes do projeto

```
com.pousada -> com.pousada.database
com.pousada -> com.pousada.handler
com.pousada -> com.pousada.service
com.pousada.handler -> com.pousada.domain
com.pousada.handler -> com.pousada.service
com.pousada.handler -> com.pousada.util
com.pousada.service -> com.pousada.database
com.pousada.service -> com.pousada.domain
com.pousada.service -> com.pousada.util
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.pousada` | 3 | 0 |
| `com.pousada.database` | 0 | 2 |
| `com.pousada.domain` | 0 | 2 |
| `com.pousada.handler` | 3 | 1 |
| `com.pousada.service` | 3 | 2 |
| `com.pousada.util` | 0 | 2 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.pousada` | sim |  |  |  |  |  |
| `com.pousada.database` |  | sim |  |  | sim |  |
| `com.pousada.domain` |  |  |  |  |  |  |
| `com.pousada.handler` | sim |  |  |  |  | com.google |
| `com.pousada.service` |  | sim |  |  |  |  |
| `com.pousada.util` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  362 src/main/java/com/pousada/service/ReservationService.java
  356 src/main/java/com/pousada/handler/ReservationHandler.java
  141 src/main/java/com/pousada/service/ReportService.java
   99 src/main/java/com/pousada/database/Database.java
   85 src/main/java/com/pousada/domain/Reservation.java
   56 src/main/java/com/pousada/handler/ReportHandler.java
   54 src/main/java/com/pousada/service/MessageService.java
   53 src/main/java/com/pousada/Application.java
   49 src/main/java/com/pousada/domain/ReservationDocument.java
   40 src/main/java/com/pousada/service/RoomService.java
```
