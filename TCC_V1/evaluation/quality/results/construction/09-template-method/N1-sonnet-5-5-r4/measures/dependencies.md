# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 6 pacotes, 11 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `br.com.mare.pousada` | 1 | 15 |
| `br.com.mare.pousada.domain` | 15 | 391 |
| `br.com.mare.pousada.infra` | 4 | 260 |
| `br.com.mare.pousada.report` | 8 | 295 |
| `br.com.mare.pousada.service` | 3 | 118 |
| `br.com.mare.pousada.web` | 2 | 172 |

## As dependências entre os pacotes do projeto

```
br.com.mare.pousada -> br.com.mare.pousada.infra
br.com.mare.pousada -> br.com.mare.pousada.web
br.com.mare.pousada.infra -> br.com.mare.pousada.domain
br.com.mare.pousada.report -> br.com.mare.pousada.domain
br.com.mare.pousada.report -> br.com.mare.pousada.infra
br.com.mare.pousada.service -> br.com.mare.pousada.domain
br.com.mare.pousada.service -> br.com.mare.pousada.infra
br.com.mare.pousada.web -> br.com.mare.pousada.domain
br.com.mare.pousada.web -> br.com.mare.pousada.infra
br.com.mare.pousada.web -> br.com.mare.pousada.report
br.com.mare.pousada.web -> br.com.mare.pousada.service
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `br.com.mare.pousada` | 2 | 0 |
| `br.com.mare.pousada.domain` | 0 | 4 |
| `br.com.mare.pousada.infra` | 1 | 4 |
| `br.com.mare.pousada.report` | 2 | 1 |
| `br.com.mare.pousada.service` | 2 | 1 |
| `br.com.mare.pousada.web` | 4 | 1 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `br.com.mare.pousada` | sim |  |  |  |  |  |
| `br.com.mare.pousada.domain` |  |  |  |  |  |  |
| `br.com.mare.pousada.infra` |  | sim |  |  | sim |  |
| `br.com.mare.pousada.report` |  | sim |  |  |  |  |
| `br.com.mare.pousada.service` |  | sim |  |  |  |  |
| `br.com.mare.pousada.web` | sim | sim | sim |  |  |  |

## As maiores classes (linhas, src/main)

```
  132 src/main/java/br/com/mare/pousada/infra/ReservationRepository.java
  109 src/main/java/br/com/mare/pousada/service/ReservationService.java
   93 src/main/java/br/com/mare/pousada/domain/Reservation.java
   90 src/main/java/br/com/mare/pousada/web/Api.java
   90 src/main/java/br/com/mare/pousada/infra/Db.java
   82 src/main/java/br/com/mare/pousada/web/CreateReservationRequest.java
   64 src/main/java/br/com/mare/pousada/report/Report.java
   55 src/main/java/br/com/mare/pousada/report/OccupancyReport.java
   45 src/main/java/br/com/mare/pousada/report/RevenueReport.java
   43 src/main/java/br/com/mare/pousada/report/RatePlanReport.java
```
