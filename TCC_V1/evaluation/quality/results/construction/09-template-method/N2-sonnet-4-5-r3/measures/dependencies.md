# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 9 pacotes, 22 dependências entre eles, com ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.pousada` | 1 | 85 |
| `com.pousada.api` | 6 | 452 |
| `com.pousada.api.dto` | 5 | 84 |
| `com.pousada.domain` | 7 | 346 |
| `com.pousada.domain.rateplan` | 4 | 74 |
| `com.pousada.domain.state` | 8 | 190 |
| `com.pousada.report` | 6 | 365 |
| `com.pousada.repository` | 5 | 481 |
| `com.pousada.service` | 2 | 127 |

## As dependências entre os pacotes do projeto

```
com.pousada.api -> com.pousada.api.dto
com.pousada.api -> com.pousada.domain
com.pousada.api -> com.pousada.domain.rateplan
com.pousada.api -> com.pousada.report
com.pousada.api -> com.pousada.repository
com.pousada.api -> com.pousada.service
com.pousada -> com.pousada.api
com.pousada -> com.pousada.domain
com.pousada -> com.pousada.repository
com.pousada -> com.pousada.service
com.pousada.domain -> com.pousada.domain.rateplan
com.pousada.domain -> com.pousada.domain.state
com.pousada.domain.state -> com.pousada.domain
com.pousada.domain.state -> com.pousada.domain.rateplan
com.pousada.report -> com.pousada.domain
com.pousada.report -> com.pousada.domain.rateplan
com.pousada.report -> com.pousada.repository
com.pousada.repository -> com.pousada.domain
com.pousada.repository -> com.pousada.domain.rateplan
com.pousada.repository -> com.pousada.domain.state
com.pousada.service -> com.pousada.domain
com.pousada.service -> com.pousada.domain.rateplan
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.pousada` | 4 | 0 |
| `com.pousada.api` | 6 | 1 |
| `com.pousada.api.dto` | 0 | 1 |
| `com.pousada.domain` | 2 | 6 |
| `com.pousada.domain.rateplan` | 0 | 6 |
| `com.pousada.domain.state` | 2 | 2 |
| `com.pousada.report` | 3 | 1 |
| `com.pousada.repository` | 3 | 3 |
| `com.pousada.service` | 2 | 2 |

## Os ciclos

O tsort achou ciclos; cada bloco abaixo é um ciclo (os pacotes que o formam):
```
--- um ciclo:
com.pousada.domain
com.pousada.domain.state
```

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.pousada` | sim |  | sim |  |  |  |
| `com.pousada.api` | sim |  |  |  |  |  |
| `com.pousada.api.dto` |  |  | sim |  |  |  |
| `com.pousada.domain` |  |  |  |  |  |  |
| `com.pousada.domain.rateplan` |  |  |  |  |  |  |
| `com.pousada.domain.state` |  |  |  |  |  |  |
| `com.pousada.report` |  |  |  |  |  |  |
| `com.pousada.repository` |  | sim |  |  |  |  |
| `com.pousada.service` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  289 src/main/java/com/pousada/api/ReservationController.java
  250 src/main/java/com/pousada/repository/ReservationRepository.java
  151 src/main/java/com/pousada/domain/Reservation.java
  100 src/main/java/com/pousada/api/ReportController.java
   98 src/main/java/com/pousada/report/RatePlansReport.java
   86 src/main/java/com/pousada/repository/RoomRepository.java
   86 src/main/java/com/pousada/report/TourismTaxReport.java
   85 src/main/java/com/pousada/Application.java
   84 src/main/java/com/pousada/domain/Document.java
   79 src/main/java/com/pousada/report/RevenueReport.java
```
