# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 6 pacotes, 13 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `pousada` | 1 | 43 |
| `pousada.domain` | 13 | 373 |
| `pousada.repo` | 8 | 391 |
| `pousada.reports` | 7 | 252 |
| `pousada.service` | 3 | 159 |
| `pousada.web` | 3 | 97 |

## As dependências entre os pacotes do projeto

```
pousada -> pousada.repo
pousada -> pousada.reports
pousada -> pousada.service
pousada -> pousada.web
pousada.repo -> pousada.domain
pousada.reports -> pousada.domain
pousada.reports -> pousada.repo
pousada.service -> pousada.domain
pousada.service -> pousada.repo
pousada.web -> pousada.domain
pousada.web -> pousada.repo
pousada.web -> pousada.reports
pousada.web -> pousada.service
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `pousada` | 4 | 0 |
| `pousada.domain` | 0 | 4 |
| `pousada.repo` | 1 | 4 |
| `pousada.reports` | 2 | 2 |
| `pousada.service` | 2 | 2 |
| `pousada.web` | 4 | 1 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `pousada` | sim | sim |  |  |  |  |
| `pousada.domain` |  |  |  |  |  |  |
| `pousada.repo` |  | sim |  |  |  |  |
| `pousada.reports` |  |  |  |  |  |  |
| `pousada.service` |  |  | sim |  |  |  |
| `pousada.web` | sim |  | sim |  |  |  |

## As maiores classes (linhas, src/main)

```
  152 src/main/java/pousada/repo/JdbcReservationRepository.java
  108 src/main/java/pousada/domain/Reservation.java
   97 src/main/java/pousada/web/Api.java
   74 src/main/java/pousada/service/ReservationService.java
   68 src/main/java/pousada/repo/Database.java
   59 src/main/java/pousada/service/NewReservation.java
   58 src/main/java/pousada/repo/InMemoryReservationRepository.java
   53 src/main/java/pousada/repo/JdbcMessageRepository.java
   49 src/main/java/pousada/reports/Report.java
   46 src/main/java/pousada/reports/OccupancyReport.java
```
