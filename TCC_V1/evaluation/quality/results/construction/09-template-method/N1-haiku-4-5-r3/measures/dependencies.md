# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 8 pacotes, 14 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.pousada` | 2 | 79 |
| `com.pousada.api` | 4 | 330 |
| `com.pousada.config` | 1 | 96 |
| `com.pousada.exception` | 1 | 20 |
| `com.pousada.model` | 6 | 324 |
| `com.pousada.repository` | 3 | 368 |
| `com.pousada.service` | 5 | 433 |
| `com.pousada.util` | 2 | 62 |

## As dependências entre os pacotes do projeto

```
com.pousada.api -> com.pousada.exception
com.pousada.api -> com.pousada.model
com.pousada.api -> com.pousada.repository
com.pousada.api -> com.pousada.service
com.pousada -> com.pousada.api
com.pousada -> com.pousada.config
com.pousada -> com.pousada.exception
com.pousada -> com.pousada.repository
com.pousada.repository -> com.pousada.config
com.pousada.repository -> com.pousada.model
com.pousada.service -> com.pousada.exception
com.pousada.service -> com.pousada.model
com.pousada.service -> com.pousada.repository
com.pousada.service -> com.pousada.util
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.pousada` | 4 | 0 |
| `com.pousada.api` | 4 | 1 |
| `com.pousada.config` | 0 | 2 |
| `com.pousada.exception` | 0 | 3 |
| `com.pousada.model` | 0 | 3 |
| `com.pousada.repository` | 2 | 3 |
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
| `com.pousada.config` |  | sim |  |  | sim |  |
| `com.pousada.exception` |  |  |  |  |  |  |
| `com.pousada.model` |  |  |  |  |  |  |
| `com.pousada.repository` |  | sim |  |  | sim |  |
| `com.pousada.service` |  |  |  |  |  |  |
| `com.pousada.util` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  244 src/main/java/com/pousada/repository/ReservationRepository.java
  183 src/main/java/com/pousada/api/ReservationController.java
  169 src/main/java/com/pousada/model/Reservation.java
  163 src/main/java/com/pousada/service/ReportService.java
  152 src/main/java/com/pousada/service/ReservationService.java
   96 src/main/java/com/pousada/config/DatabaseConfig.java
   94 src/main/java/com/pousada/api/ReportController.java
   79 src/main/java/com/pousada/Main.java
   73 src/main/java/com/pousada/service/EmailService.java
   69 src/main/java/com/pousada/repository/RoomRepository.java
```
