# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 9 pacotes, 17 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `pousada` | 1 | 78 |
| `pousada.config` | 1 | 87 |
| `pousada.controller` | 4 | 403 |
| `pousada.domain` | 7 | 298 |
| `pousada.dto` | 2 | 120 |
| `pousada.pricing` | 2 | 97 |
| `pousada.report` | 7 | 332 |
| `pousada.repository` | 3 | 391 |
| `pousada.service` | 9 | 275 |

## As dependências entre os pacotes do projeto

```
pousada.controller -> pousada.domain
pousada.controller -> pousada.dto
pousada.controller -> pousada.report
pousada.controller -> pousada.repository
pousada.controller -> pousada.service
pousada -> pousada.config
pousada -> pousada.controller
pousada -> pousada.repository
pousada -> pousada.service
pousada.pricing -> pousada.domain
pousada.report -> pousada.domain
pousada.report -> pousada.repository
pousada.repository -> pousada.config
pousada.repository -> pousada.domain
pousada.service -> pousada.domain
pousada.service -> pousada.pricing
pousada.service -> pousada.repository
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `pousada` | 4 | 0 |
| `pousada.config` | 0 | 2 |
| `pousada.controller` | 5 | 1 |
| `pousada.domain` | 0 | 5 |
| `pousada.dto` | 0 | 1 |
| `pousada.pricing` | 1 | 1 |
| `pousada.report` | 2 | 1 |
| `pousada.repository` | 2 | 4 |
| `pousada.service` | 3 | 2 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `pousada` | sim |  | sim |  |  |  |
| `pousada.config` |  | sim |  |  | sim |  |
| `pousada.controller` | sim |  |  |  |  |  |
| `pousada.domain` |  |  |  |  |  |  |
| `pousada.dto` |  |  | sim |  |  |  |
| `pousada.pricing` |  |  |  |  |  |  |
| `pousada.report` |  |  |  |  |  |  |
| `pousada.repository` |  | sim |  |  |  |  |
| `pousada.service` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  247 src/main/java/pousada/repository/ReservationRepository.java
  230 src/main/java/pousada/controller/ReservationController.java
  195 src/main/java/pousada/service/ReservationService.java
  166 src/main/java/pousada/domain/Reservation.java
  104 src/main/java/pousada/controller/ReportController.java
   97 src/main/java/pousada/pricing/PricingCalculator.java
   93 src/main/java/pousada/dto/CreateReservationRequest.java
   87 src/main/java/pousada/config/Database.java
   83 src/main/java/pousada/repository/RoomRepository.java
   83 src/main/java/pousada/report/RatePlanReport.java
```
