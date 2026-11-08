# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 6 pacotes, 13 dependências entre eles, com ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `pousada` | 1 | 17 |
| `pousada.db` | 7 | 417 |
| `pousada.domain` | 17 | 455 |
| `pousada.error` | 1 | 45 |
| `pousada.report` | 8 | 260 |
| `pousada.web` | 5 | 145 |

## As dependências entre os pacotes do projeto

```
pousada.db -> pousada.domain
pousada.domain -> pousada.db
pousada.domain -> pousada.error
pousada.domain -> pousada.web
pousada -> pousada.db
pousada -> pousada.web
pousada.report -> pousada.db
pousada.report -> pousada.domain
pousada.report -> pousada.error
pousada.web -> pousada.db
pousada.web -> pousada.domain
pousada.web -> pousada.error
pousada.web -> pousada.report
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `pousada` | 2 | 0 |
| `pousada.db` | 1 | 4 |
| `pousada.domain` | 3 | 3 |
| `pousada.error` | 0 | 3 |
| `pousada.report` | 3 | 1 |
| `pousada.web` | 4 | 2 |

## Os ciclos

O tsort achou ciclos; cada bloco abaixo é um ciclo (os pacotes que o formam):
```
--- um ciclo:
pousada.web
pousada.report
pousada.domain
--- um ciclo:
pousada.db
pousada.domain
```

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `pousada` | sim |  |  |  |  |  |
| `pousada.db` |  | sim |  |  | sim |  |
| `pousada.domain` |  |  | sim |  |  |  |
| `pousada.error` |  |  |  |  |  |  |
| `pousada.report` |  |  |  |  |  |  |
| `pousada.web` | sim |  | sim |  |  |  |

## As maiores classes (linhas, src/main)

```
  145 src/main/java/pousada/db/ReservationRepository.java
   84 src/main/java/pousada/db/Schema.java
   83 src/main/java/pousada/domain/NewReservation.java
   82 src/main/java/pousada/domain/ReservationService.java
   75 src/main/java/pousada/db/Database.java
   68 src/main/java/pousada/domain/Reservation.java
   58 src/main/java/pousada/db/RoomRepository.java
   55 src/main/java/pousada/web/App.java
   55 src/main/java/pousada/report/OccupancyReport.java
   46 src/main/java/pousada/db/MessageRepository.java
```
