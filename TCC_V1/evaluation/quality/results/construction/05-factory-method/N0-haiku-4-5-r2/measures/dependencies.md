# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 7 pacotes, 12 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.pousada` | 1 | 30 |
| `com.pousada.api` | 4 | 318 |
| `com.pousada.db` | 1 | 78 |
| `com.pousada.model` | 4 | 154 |
| `com.pousada.repository` | 3 | 319 |
| `com.pousada.service` | 3 | 269 |
| `com.pousada.util` | 2 | 140 |

## As dependências entre os pacotes do projeto

```
com.pousada.api -> com.pousada.db
com.pousada.api -> com.pousada.model
com.pousada.api -> com.pousada.repository
com.pousada.api -> com.pousada.service
com.pousada -> com.pousada.api
com.pousada -> com.pousada.db
com.pousada.repository -> com.pousada.db
com.pousada.repository -> com.pousada.model
com.pousada.service -> com.pousada.db
com.pousada.service -> com.pousada.model
com.pousada.service -> com.pousada.repository
com.pousada.service -> com.pousada.util
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.pousada` | 2 | 0 |
| `com.pousada.api` | 4 | 1 |
| `com.pousada.db` | 0 | 4 |
| `com.pousada.model` | 0 | 3 |
| `com.pousada.repository` | 2 | 2 |
| `com.pousada.service` | 4 | 1 |
| `com.pousada.util` | 0 | 1 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.pousada` | sim |  |  |  |  |  |
| `com.pousada.api` | sim |  |  |  |  | com.google |
| `com.pousada.db` |  | sim |  |  |  |  |
| `com.pousada.model` |  |  |  |  |  |  |
| `com.pousada.repository` |  | sim |  |  |  |  |
| `com.pousada.service` |  |  |  |  |  |  |
| `com.pousada.util` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  207 src/main/java/com/pousada/repository/ReservationRepository.java
  197 src/main/java/com/pousada/api/ReservationController.java
  187 src/main/java/com/pousada/service/ReservationService.java
   92 src/main/java/com/pousada/util/DocumentFormatter.java
   82 src/main/java/com/pousada/service/ReportService.java
   78 src/main/java/com/pousada/db/Database.java
   73 src/main/java/com/pousada/model/Reservation.java
   63 src/main/java/com/pousada/api/ReportController.java
   57 src/main/java/com/pousada/repository/MessageRepository.java
   55 src/main/java/com/pousada/repository/RoomRepository.java
```
