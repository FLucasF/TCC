# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 8 pacotes, 15 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.pousada.marealto` | 1 | 71 |
| `com.pousada.marealto.api` | 4 | 357 |
| `com.pousada.marealto.database` | 1 | 83 |
| `com.pousada.marealto.domain` | 5 | 265 |
| `com.pousada.marealto.messaging` | 2 | 64 |
| `com.pousada.marealto.rateplan` | 9 | 281 |
| `com.pousada.marealto.repository` | 3 | 389 |
| `com.pousada.marealto.service` | 10 | 382 |

## As dependências entre os pacotes do projeto

```
com.pousada.marealto.api -> com.pousada.marealto.domain
com.pousada.marealto.api -> com.pousada.marealto.messaging
com.pousada.marealto.api -> com.pousada.marealto.service
com.pousada.marealto -> com.pousada.marealto.api
com.pousada.marealto -> com.pousada.marealto.database
com.pousada.marealto -> com.pousada.marealto.repository
com.pousada.marealto -> com.pousada.marealto.service
com.pousada.marealto.rateplan -> com.pousada.marealto.domain
com.pousada.marealto.repository -> com.pousada.marealto.database
com.pousada.marealto.repository -> com.pousada.marealto.domain
com.pousada.marealto.repository -> com.pousada.marealto.messaging
com.pousada.marealto.service -> com.pousada.marealto.domain
com.pousada.marealto.service -> com.pousada.marealto.messaging
com.pousada.marealto.service -> com.pousada.marealto.rateplan
com.pousada.marealto.service -> com.pousada.marealto.repository
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.pousada.marealto` | 4 | 0 |
| `com.pousada.marealto.api` | 3 | 1 |
| `com.pousada.marealto.database` | 0 | 2 |
| `com.pousada.marealto.domain` | 0 | 4 |
| `com.pousada.marealto.messaging` | 0 | 3 |
| `com.pousada.marealto.rateplan` | 1 | 1 |
| `com.pousada.marealto.repository` | 3 | 2 |
| `com.pousada.marealto.service` | 4 | 2 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.pousada.marealto` | sim |  | sim |  |  |  |
| `com.pousada.marealto.api` | sim |  | sim |  |  |  |
| `com.pousada.marealto.database` |  | sim |  |  |  |  |
| `com.pousada.marealto.domain` |  |  |  |  |  |  |
| `com.pousada.marealto.messaging` |  |  |  |  |  |  |
| `com.pousada.marealto.rateplan` |  |  |  |  |  |  |
| `com.pousada.marealto.repository` |  | sim |  |  |  |  |
| `com.pousada.marealto.service` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  258 src/main/java/com/pousada/marealto/repository/ReservationRepository.java
  227 src/main/java/com/pousada/marealto/api/ReservationController.java
  199 src/main/java/com/pousada/marealto/domain/Reservation.java
  167 src/main/java/com/pousada/marealto/service/ReservationService.java
   94 src/main/java/com/pousada/marealto/service/MessageService.java
   92 src/main/java/com/pousada/marealto/service/ReportService.java
   83 src/main/java/com/pousada/marealto/database/DatabaseManager.java
   71 src/main/java/com/pousada/marealto/Main.java
   66 src/main/java/com/pousada/marealto/repository/MessageRepository.java
   65 src/main/java/com/pousada/marealto/repository/RoomRepository.java
```
