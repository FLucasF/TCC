# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 7 pacotes, 13 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.pousada` | 1 | 94 |
| `com.pousada.api` | 6 | 316 |
| `com.pousada.config` | 1 | 84 |
| `com.pousada.domain` | 9 | 467 |
| `com.pousada.repository` | 3 | 422 |
| `com.pousada.service` | 10 | 386 |
| `com.pousada.service.messaging` | 7 | 201 |

## As dependências entre os pacotes do projeto

```
com.pousada.api -> com.pousada.domain
com.pousada.api -> com.pousada.service
com.pousada -> com.pousada.api
com.pousada -> com.pousada.config
com.pousada -> com.pousada.repository
com.pousada -> com.pousada.service
com.pousada -> com.pousada.service.messaging
com.pousada.repository -> com.pousada.config
com.pousada.repository -> com.pousada.domain
com.pousada.service -> com.pousada.domain
com.pousada.service -> com.pousada.repository
com.pousada.service -> com.pousada.service.messaging
com.pousada.service.messaging -> com.pousada.domain
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.pousada` | 5 | 0 |
| `com.pousada.api` | 2 | 1 |
| `com.pousada.config` | 0 | 2 |
| `com.pousada.domain` | 0 | 4 |
| `com.pousada.repository` | 2 | 2 |
| `com.pousada.service` | 3 | 2 |
| `com.pousada.service.messaging` | 1 | 2 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.pousada` | sim |  | sim |  |  |  |
| `com.pousada.api` | sim |  | sim |  |  |  |
| `com.pousada.config` |  | sim |  |  | sim |  |
| `com.pousada.domain` |  |  |  |  |  |  |
| `com.pousada.repository` |  | sim |  |  |  |  |
| `com.pousada.service` |  |  |  |  |  |  |
| `com.pousada.service.messaging` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  246 src/main/java/com/pousada/repository/ReservationRepository.java
  239 src/main/java/com/pousada/service/ReservationService.java
  208 src/main/java/com/pousada/domain/Reservation.java
  190 src/main/java/com/pousada/api/ReservationController.java
   98 src/main/java/com/pousada/repository/RoomRepository.java
   94 src/main/java/com/pousada/App.java
   89 src/main/java/com/pousada/service/ReportService.java
   84 src/main/java/com/pousada/domain/ReservationDocument.java
   84 src/main/java/com/pousada/config/Database.java
   78 src/main/java/com/pousada/repository/MessageRepository.java
```
