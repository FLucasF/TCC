# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 6 pacotes, 13 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `pousada` | 1 | 49 |
| `pousada.domain` | 16 | 372 |
| `pousada.persistence` | 5 | 344 |
| `pousada.report` | 14 | 327 |
| `pousada.service` | 4 | 177 |
| `pousada.web` | 4 | 201 |

## As dependências entre os pacotes do projeto

```
pousada.persistence -> pousada.domain
pousada -> pousada.domain
pousada -> pousada.persistence
pousada -> pousada.report
pousada -> pousada.service
pousada -> pousada.web
pousada.report -> pousada.domain
pousada.report -> pousada.persistence
pousada.service -> pousada.domain
pousada.service -> pousada.persistence
pousada.web -> pousada.domain
pousada.web -> pousada.report
pousada.web -> pousada.service
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `pousada` | 5 | 0 |
| `pousada.domain` | 0 | 5 |
| `pousada.persistence` | 1 | 3 |
| `pousada.report` | 2 | 2 |
| `pousada.service` | 2 | 2 |
| `pousada.web` | 3 | 1 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `pousada` | sim |  |  |  |  |  |
| `pousada.domain` |  |  |  |  |  |  |
| `pousada.persistence` |  | sim |  |  | sim |  |
| `pousada.report` |  |  |  |  |  |  |
| `pousada.service` |  |  |  |  |  |  |
| `pousada.web` | sim |  | sim |  |  |  |

## As maiores classes (linhas, src/main)

```
  144 src/main/java/pousada/persistence/ReservationRepository.java
  117 src/main/java/pousada/domain/Reservation.java
   99 src/main/java/pousada/service/ReservationService.java
   86 src/main/java/pousada/web/Api.java
   82 src/main/java/pousada/web/ReservationRequestParser.java
   53 src/main/java/pousada/report/PeriodKind.java
   53 src/main/java/pousada/persistence/Database.java
   52 src/main/java/pousada/persistence/Tx.java
   49 src/main/java/pousada/persistence/MessageRepository.java
   49 src/main/java/pousada/App.java
```
