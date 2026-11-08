# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 7 pacotes, 12 dependências entre eles, com ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `pousada` | 1 | 72 |
| `pousada.api` | 8 | 474 |
| `pousada.config` | 1 | 27 |
| `pousada.domain` | 5 | 309 |
| `pousada.domain.rateplan` | 9 | 301 |
| `pousada.repository` | 3 | 451 |
| `pousada.service` | 1 | 82 |

## As dependências entre os pacotes do projeto

```
pousada.api -> pousada.domain
pousada.api -> pousada.repository
pousada.api -> pousada.service
pousada.domain -> pousada.domain.rateplan
pousada.domain.rateplan -> pousada.domain
pousada -> pousada.api
pousada -> pousada.config
pousada -> pousada.repository
pousada -> pousada.service
pousada.repository -> pousada.domain
pousada.service -> pousada.domain
pousada.service -> pousada.repository
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `pousada` | 4 | 0 |
| `pousada.api` | 3 | 1 |
| `pousada.config` | 0 | 1 |
| `pousada.domain` | 1 | 4 |
| `pousada.domain.rateplan` | 1 | 1 |
| `pousada.repository` | 1 | 3 |
| `pousada.service` | 2 | 2 |

## Os ciclos

O tsort achou ciclos; cada bloco abaixo é um ciclo (os pacotes que o formam):
```
--- um ciclo:
pousada.domain
pousada.domain.rateplan
```

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `pousada` | sim | sim | sim |  |  |  |
| `pousada.api` | sim |  |  |  |  |  |
| `pousada.config` |  | sim |  |  | sim |  |
| `pousada.domain` |  |  |  |  |  |  |
| `pousada.domain.rateplan` |  |  |  |  |  |  |
| `pousada.repository` |  | sim |  |  |  |  |
| `pousada.service` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  284 src/main/java/pousada/api/ReservationController.java
  256 src/main/java/pousada/repository/ReservationRepository.java
  202 src/main/java/pousada/domain/Reservation.java
  115 src/main/java/pousada/api/ReportController.java
  110 src/main/java/pousada/repository/RoomRepository.java
   85 src/main/java/pousada/repository/MessageRepository.java
   82 src/main/java/pousada/service/NotificationService.java
   72 src/main/java/pousada/Main.java
   67 src/main/java/pousada/domain/rateplan/AbstractRatePlan.java
   43 src/main/java/pousada/domain/rateplan/CorporateRatePlan.java
```
