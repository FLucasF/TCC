# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 8 pacotes, 16 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.pousada` | 1 | 40 |
| `com.pousada.api` | 4 | 399 |
| `com.pousada.db` | 1 | 138 |
| `com.pousada.domain` | 6 | 148 |
| `com.pousada.rateplan` | 9 | 246 |
| `com.pousada.repository` | 3 | 279 |
| `com.pousada.service` | 4 | 341 |
| `com.pousada.utils` | 3 | 106 |

## As dependências entre os pacotes do projeto

```
com.pousada.api -> com.pousada.domain
com.pousada.api -> com.pousada.repository
com.pousada.api -> com.pousada.service
com.pousada.api -> com.pousada.utils
com.pousada -> com.pousada.api
com.pousada -> com.pousada.db
com.pousada.rateplan -> com.pousada.domain
com.pousada.rateplan -> com.pousada.utils
com.pousada.repository -> com.pousada.db
com.pousada.repository -> com.pousada.domain
com.pousada.service -> com.pousada.db
com.pousada.service -> com.pousada.domain
com.pousada.service -> com.pousada.rateplan
com.pousada.service -> com.pousada.repository
com.pousada.service -> com.pousada.utils
com.pousada.utils -> com.pousada.domain
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.pousada` | 2 | 0 |
| `com.pousada.api` | 4 | 1 |
| `com.pousada.db` | 0 | 3 |
| `com.pousada.domain` | 0 | 5 |
| `com.pousada.rateplan` | 2 | 1 |
| `com.pousada.repository` | 2 | 2 |
| `com.pousada.service` | 5 | 1 |
| `com.pousada.utils` | 1 | 3 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.pousada` | sim |  |  |  |  |  |
| `com.pousada.api` | sim |  |  |  |  | org |
| `com.pousada.db` |  | sim |  |  | sim |  |
| `com.pousada.domain` |  |  |  |  |  |  |
| `com.pousada.rateplan` |  |  |  |  |  |  |
| `com.pousada.repository` |  | sim |  |  |  |  |
| `com.pousada.service` |  | sim |  |  |  |  |
| `com.pousada.utils` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  269 src/main/java/com/pousada/api/ReservationApi.java
  186 src/main/java/com/pousada/service/ReservationService.java
  154 src/main/java/com/pousada/repository/ReservationRepository.java
  138 src/main/java/com/pousada/db/Database.java
  119 src/main/java/com/pousada/service/ReportService.java
   84 src/main/java/com/pousada/domain/Reservation.java
   69 src/main/java/com/pousada/repository/MessageRepository.java
   57 src/main/java/com/pousada/api/ReportApi.java
   56 src/main/java/com/pousada/repository/RoomRepository.java
   49 src/main/java/com/pousada/utils/ComprovantePrinter.java
```
