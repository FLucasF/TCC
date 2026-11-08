# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 7 pacotes, 15 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.marealta` | 1 | 77 |
| `com.marealta.api` | 6 | 430 |
| `com.marealta.db` | 4 | 460 |
| `com.marealta.model` | 6 | 151 |
| `com.marealta.report` | 6 | 261 |
| `com.marealta.service` | 3 | 260 |
| `com.marealta.util` | 2 | 61 |

## As dependências entre os pacotes do projeto

```
com.marealta.api -> com.marealta.db
com.marealta.api -> com.marealta.model
com.marealta.api -> com.marealta.report
com.marealta.api -> com.marealta.service
com.marealta -> com.marealta.api
com.marealta -> com.marealta.db
com.marealta -> com.marealta.report
com.marealta -> com.marealta.service
com.marealta.db -> com.marealta.model
com.marealta.report -> com.marealta.db
com.marealta.report -> com.marealta.model
com.marealta.report -> com.marealta.util
com.marealta.service -> com.marealta.db
com.marealta.service -> com.marealta.model
com.marealta.service -> com.marealta.util
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.marealta` | 4 | 0 |
| `com.marealta.api` | 4 | 1 |
| `com.marealta.db` | 1 | 4 |
| `com.marealta.model` | 0 | 4 |
| `com.marealta.report` | 3 | 2 |
| `com.marealta.service` | 3 | 2 |
| `com.marealta.util` | 0 | 2 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.marealta` | sim |  |  |  |  | com.google |
| `com.marealta.api` | sim |  |  |  |  | com.google |
| `com.marealta.db` |  | sim |  |  | sim |  |
| `com.marealta.model` |  |  |  |  |  |  |
| `com.marealta.report` |  |  |  |  |  |  |
| `com.marealta.service` |  |  |  |  |  |  |
| `com.marealta.util` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  258 src/main/java/com/marealta/api/ReservationsHandler.java
  214 src/main/java/com/marealta/db/ReservationRepository.java
  169 src/main/java/com/marealta/service/ReservationService.java
  111 src/main/java/com/marealta/api/ReportsHandler.java
  110 src/main/java/com/marealta/db/Database.java
  103 src/main/java/com/marealta/model/Reservation.java
   77 src/main/java/com/marealta/Application.java
   72 src/main/java/com/marealta/report/RatePlansReport.java
   69 src/main/java/com/marealta/db/MessageRepository.java
   67 src/main/java/com/marealta/db/RoomRepository.java
```
