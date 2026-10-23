# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 7 pacotes, 13 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.pousada` | 1 | 78 |
| `com.pousada.api` | 4 | 310 |
| `com.pousada.domain` | 8 | 335 |
| `com.pousada.infrastructure` | 1 | 95 |
| `com.pousada.messaging` | 6 | 212 |
| `com.pousada.repository` | 3 | 398 |
| `com.pousada.service` | 10 | 355 |

## As dependências entre os pacotes do projeto

```
com.pousada.api -> com.pousada.domain
com.pousada.api -> com.pousada.service
com.pousada -> com.pousada.api
com.pousada -> com.pousada.infrastructure
com.pousada -> com.pousada.messaging
com.pousada -> com.pousada.repository
com.pousada -> com.pousada.service
com.pousada.messaging -> com.pousada.domain
com.pousada.repository -> com.pousada.domain
com.pousada.repository -> com.pousada.infrastructure
com.pousada.service -> com.pousada.domain
com.pousada.service -> com.pousada.messaging
com.pousada.service -> com.pousada.repository
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.pousada` | 5 | 0 |
| `com.pousada.api` | 2 | 1 |
| `com.pousada.domain` | 0 | 4 |
| `com.pousada.infrastructure` | 0 | 2 |
| `com.pousada.messaging` | 1 | 2 |
| `com.pousada.repository` | 2 | 2 |
| `com.pousada.service` | 3 | 2 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.pousada` | sim |  | sim |  |  |  |
| `com.pousada.api` | sim |  | sim |  |  |  |
| `com.pousada.domain` |  |  |  |  |  |  |
| `com.pousada.infrastructure` |  | sim |  |  | sim |  |
| `com.pousada.messaging` |  |  |  |  |  |  |
| `com.pousada.repository` |  | sim |  |  |  |  |
| `com.pousada.service` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  253 src/main/java/com/pousada/repository/ReservationRepository.java
  230 src/main/java/com/pousada/api/ApiController.java
  193 src/main/java/com/pousada/domain/Reservation.java
  166 src/main/java/com/pousada/service/ReservationService.java
   95 src/main/java/com/pousada/infrastructure/Database.java
   91 src/main/java/com/pousada/service/MessageService.java
   79 src/main/java/com/pousada/repository/MessageRepository.java
   78 src/main/java/com/pousada/Main.java
   77 src/main/java/com/pousada/api/ApiMappers.java
   75 src/main/java/com/pousada/service/ReportService.java
```
