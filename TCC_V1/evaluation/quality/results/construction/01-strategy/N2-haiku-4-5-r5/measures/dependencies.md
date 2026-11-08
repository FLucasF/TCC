# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 8 pacotes, 15 dependências entre eles, com ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.pousadamarealta` | 1 | 23 |
| `com.pousadamarealta.api` | 2 | 231 |
| `com.pousadamarealta.api.request` | 1 | 25 |
| `com.pousadamarealta.api.response` | 5 | 181 |
| `com.pousadamarealta.database` | 5 | 403 |
| `com.pousadamarealta.domain` | 5 | 102 |
| `com.pousadamarealta.pricing` | 11 | 316 |
| `com.pousadamarealta.service` | 2 | 388 |

## As dependências entre os pacotes do projeto

```
com.pousadamarealta.api -> com.pousadamarealta.api.request
com.pousadamarealta.api -> com.pousadamarealta.api.response
com.pousadamarealta.api -> com.pousadamarealta.database
com.pousadamarealta.api -> com.pousadamarealta.domain
com.pousadamarealta.api -> com.pousadamarealta.service
com.pousadamarealta -> com.pousadamarealta.api
com.pousadamarealta -> com.pousadamarealta.database
com.pousadamarealta.database -> com.pousadamarealta.domain
com.pousadamarealta.pricing -> com.pousadamarealta.api
com.pousadamarealta.pricing -> com.pousadamarealta.domain
com.pousadamarealta.service -> com.pousadamarealta.api
com.pousadamarealta.service -> com.pousadamarealta.api.request
com.pousadamarealta.service -> com.pousadamarealta.database
com.pousadamarealta.service -> com.pousadamarealta.domain
com.pousadamarealta.service -> com.pousadamarealta.pricing
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.pousadamarealta` | 2 | 0 |
| `com.pousadamarealta.api` | 5 | 3 |
| `com.pousadamarealta.api.request` | 0 | 2 |
| `com.pousadamarealta.api.response` | 0 | 1 |
| `com.pousadamarealta.database` | 1 | 3 |
| `com.pousadamarealta.domain` | 0 | 4 |
| `com.pousadamarealta.pricing` | 2 | 1 |
| `com.pousadamarealta.service` | 5 | 1 |

## Os ciclos

O tsort achou ciclos; cada bloco abaixo é um ciclo (os pacotes que o formam):
```
--- um ciclo:
com.pousadamarealta.api
com.pousadamarealta.service
com.pousadamarealta.pricing
--- um ciclo:
com.pousadamarealta.api
com.pousadamarealta.service
```

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.pousadamarealta` | sim |  |  |  |  |  |
| `com.pousadamarealta.api` | sim | sim |  |  |  | com.google |
| `com.pousadamarealta.api.request` |  |  |  |  |  |  |
| `com.pousadamarealta.api.response` |  |  |  |  |  | com.google.gson |
| `com.pousadamarealta.database` |  | sim |  |  |  |  |
| `com.pousadamarealta.domain` |  |  |  |  |  |  |
| `com.pousadamarealta.pricing` |  |  |  |  |  |  |
| `com.pousadamarealta.service` |  | sim |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  279 src/main/java/com/pousadamarealta/service/ReservationService.java
  205 src/main/java/com/pousadamarealta/api/Routes.java
  204 src/main/java/com/pousadamarealta/database/ReservationDao.java
  109 src/main/java/com/pousadamarealta/service/ReportService.java
   96 src/main/java/com/pousadamarealta/database/Database.java
   77 src/main/java/com/pousadamarealta/domain/Reservation.java
   71 src/main/java/com/pousadamarealta/api/response/ReservationResponse.java
   60 src/main/java/com/pousadamarealta/pricing/CorporatePlan.java
   51 src/main/java/com/pousadamarealta/database/MessageDao.java
   49 src/main/java/com/pousadamarealta/pricing/LongStayPlan.java
```
