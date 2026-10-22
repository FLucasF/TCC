# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 6 pacotes, 14 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `pousada` | 1 | 42 |
| `pousada.db` | 4 | 288 |
| `pousada.domain` | 16 | 364 |
| `pousada.report` | 8 | 268 |
| `pousada.service` | 3 | 127 |
| `pousada.web` | 1 | 134 |

## As dependências entre os pacotes do projeto

```
pousada.db -> pousada.domain
pousada -> pousada.db
pousada -> pousada.domain
pousada -> pousada.report
pousada -> pousada.service
pousada -> pousada.web
pousada.report -> pousada.db
pousada.report -> pousada.domain
pousada.service -> pousada.db
pousada.service -> pousada.domain
pousada.web -> pousada.db
pousada.web -> pousada.domain
pousada.web -> pousada.report
pousada.web -> pousada.service
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `pousada` | 5 | 0 |
| `pousada.db` | 1 | 4 |
| `pousada.domain` | 0 | 5 |
| `pousada.report` | 2 | 2 |
| `pousada.service` | 2 | 2 |
| `pousada.web` | 4 | 1 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `pousada` | sim |  |  |  |  |  |
| `pousada.db` |  | sim |  |  |  |  |
| `pousada.domain` |  |  |  |  |  |  |
| `pousada.report` |  |  |  |  |  |  |
| `pousada.service` |  |  |  |  |  |  |
| `pousada.web` | sim |  | sim |  |  |  |

## As maiores classes (linhas, src/main)

```
  134 src/main/java/pousada/web/Api.java
  123 src/main/java/pousada/db/ReservationRepository.java
  121 src/main/java/pousada/db/Database.java
  117 src/main/java/pousada/domain/Reservation.java
  101 src/main/java/pousada/service/ReservationService.java
   59 src/main/java/pousada/report/OccupancyReport.java
   50 src/main/java/pousada/report/MonthlyReport.java
   44 src/main/java/pousada/db/MessageRepository.java
   42 src/main/java/pousada/Main.java
   41 src/main/java/pousada/domain/Receipt.java
```
