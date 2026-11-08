# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 7 pacotes, 17 dependências entre eles, com ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `pousada` | 1 | 20 |
| `pousada.api` | 3 | 195 |
| `pousada.domain` | 13 | 337 |
| `pousada.infra` | 3 | 279 |
| `pousada.plan` | 9 | 264 |
| `pousada.report` | 2 | 62 |
| `pousada.service` | 3 | 129 |

## As dependências entre os pacotes do projeto

```
pousada.api -> pousada.domain
pousada.api -> pousada.plan
pousada.api -> pousada.report
pousada.api -> pousada.service
pousada.domain -> pousada.plan
pousada.infra -> pousada.domain
pousada.infra -> pousada.plan
pousada.infra -> pousada.service
pousada.plan -> pousada.domain
pousada -> pousada.api
pousada -> pousada.infra
pousada -> pousada.report
pousada -> pousada.service
pousada.report -> pousada.domain
pousada.report -> pousada.service
pousada.service -> pousada.domain
pousada.service -> pousada.plan
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `pousada` | 4 | 0 |
| `pousada.api` | 4 | 1 |
| `pousada.domain` | 1 | 5 |
| `pousada.infra` | 3 | 1 |
| `pousada.plan` | 1 | 4 |
| `pousada.report` | 2 | 2 |
| `pousada.service` | 2 | 4 |

## Os ciclos

O tsort achou ciclos; cada bloco abaixo é um ciclo (os pacotes que o formam):
```
--- um ciclo:
pousada.domain
pousada.plan
```

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `pousada` | sim | sim |  |  |  |  |
| `pousada.api` | sim |  | sim |  |  |  |
| `pousada.domain` |  |  |  |  |  |  |
| `pousada.infra` |  | sim |  |  | sim |  |
| `pousada.plan` |  |  |  |  |  |  |
| `pousada.report` |  |  |  |  |  |  |
| `pousada.service` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  201 src/main/java/pousada/infra/PostgresReservationRepository.java
  115 src/main/java/pousada/domain/Reservation.java
  100 src/main/java/pousada/api/Api.java
   90 src/main/java/pousada/service/ReservationService.java
   78 src/main/java/pousada/infra/Database.java
   77 src/main/java/pousada/api/ReservationRequest.java
   52 src/main/java/pousada/domain/ReceiptText.java
   47 src/main/java/pousada/report/Reports.java
   45 src/main/java/pousada/plan/WeekendPackagePlan.java
   38 src/main/java/pousada/plan/CorporatePlan.java
```
