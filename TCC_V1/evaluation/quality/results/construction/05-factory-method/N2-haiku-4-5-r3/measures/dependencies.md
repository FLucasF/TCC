# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 8 pacotes, 19 dependências entre eles, com ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.pousada.api` | 2 | 121 |
| `com.pousada.api.handler` | 4 | 289 |
| `com.pousada.db` | 1 | 84 |
| `com.pousada.document` | 6 | 229 |
| `com.pousada.domain` | 6 | 233 |
| `com.pousada.repository` | 3 | 363 |
| `com.pousada.service` | 2 | 290 |
| `com.pousada.util` | 2 | 122 |

## As dependências entre os pacotes do projeto

```
com.pousada.api -> com.pousada.api.handler
com.pousada.api -> com.pousada.db
com.pousada.api -> com.pousada.document
com.pousada.api -> com.pousada.domain
com.pousada.api -> com.pousada.repository
com.pousada.api.handler -> com.pousada.api
com.pousada.api.handler -> com.pousada.document
com.pousada.api.handler -> com.pousada.domain
com.pousada.api.handler -> com.pousada.repository
com.pousada.api.handler -> com.pousada.service
com.pousada.document -> com.pousada.domain
com.pousada.document -> com.pousada.util
com.pousada.repository -> com.pousada.db
com.pousada.repository -> com.pousada.domain
com.pousada.service -> com.pousada.document
com.pousada.service -> com.pousada.domain
com.pousada.service -> com.pousada.repository
com.pousada.service -> com.pousada.util
com.pousada.util -> com.pousada.domain
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.pousada.api` | 5 | 1 |
| `com.pousada.api.handler` | 5 | 1 |
| `com.pousada.db` | 0 | 2 |
| `com.pousada.document` | 2 | 3 |
| `com.pousada.domain` | 0 | 6 |
| `com.pousada.repository` | 2 | 3 |
| `com.pousada.service` | 4 | 1 |
| `com.pousada.util` | 1 | 2 |

## Os ciclos

O tsort achou ciclos; cada bloco abaixo é um ciclo (os pacotes que o formam):
```
--- um ciclo:
com.pousada.api
com.pousada.api.handler
```

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.pousada.api` | sim |  |  |  |  | org |
| `com.pousada.api.handler` | sim |  |  |  |  | org |
| `com.pousada.db` |  | sim |  |  |  |  |
| `com.pousada.document` |  |  |  |  |  |  |
| `com.pousada.domain` |  |  |  |  |  |  |
| `com.pousada.repository` |  | sim |  |  |  |  |
| `com.pousada.service` |  |  |  |  |  |  |
| `com.pousada.util` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  219 src/main/java/com/pousada/repository/ReservationRepository.java
  205 src/main/java/com/pousada/service/ReservationService.java
  186 src/main/java/com/pousada/api/handler/ReservationHandler.java
  163 src/main/java/com/pousada/domain/Reservation.java
   85 src/main/java/com/pousada/service/ReportService.java
   84 src/main/java/com/pousada/repository/RoomRepository.java
   84 src/main/java/com/pousada/db/Database.java
   66 src/main/java/com/pousada/api/JsonUtil.java
   61 src/main/java/com/pousada/util/PriceCalculator.java
   61 src/main/java/com/pousada/util/Formatter.java
```
