# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 6 pacotes, 14 dependências entre eles, com ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `pousada` | 1 | 49 |
| `pousada.app` | 10 | 217 |
| `pousada.domain` | 14 | 361 |
| `pousada.infra` | 5 | 279 |
| `pousada.notification` | 3 | 97 |
| `pousada.web` | 4 | 174 |

## As dependências entre os pacotes do projeto

```
pousada.app -> pousada.domain
pousada.app -> pousada.infra
pousada.infra -> pousada.app
pousada.infra -> pousada.domain
pousada.notification -> pousada.app
pousada.notification -> pousada.domain
pousada -> pousada.app
pousada -> pousada.domain
pousada -> pousada.infra
pousada -> pousada.notification
pousada -> pousada.web
pousada.web -> pousada.app
pousada.web -> pousada.domain
pousada.web -> pousada.infra
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `pousada` | 5 | 0 |
| `pousada.app` | 2 | 4 |
| `pousada.domain` | 0 | 5 |
| `pousada.infra` | 2 | 3 |
| `pousada.notification` | 2 | 1 |
| `pousada.web` | 3 | 1 |

## Os ciclos

O tsort achou ciclos; cada bloco abaixo é um ciclo (os pacotes que o formam):
```
--- um ciclo:
pousada.app
pousada.infra
```

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `pousada` | sim |  |  |  |  |  |
| `pousada.app` |  | sim |  |  |  |  |
| `pousada.domain` |  |  |  |  |  |  |
| `pousada.infra` |  | sim |  |  | sim |  |
| `pousada.notification` |  |  |  |  |  |  |
| `pousada.web` | sim |  | sim |  |  |  |

## As maiores classes (linhas, src/main)

```
  142 src/main/java/pousada/web/Api.java
  135 src/main/java/pousada/infra/ReservationRepository.java
   96 src/main/java/pousada/domain/Reservation.java
   89 src/main/java/pousada/infra/Database.java
   84 src/main/java/pousada/app/ReservationService.java
   52 src/main/java/pousada/app/ReportService.java
   49 src/main/java/pousada/Main.java
   48 src/main/java/pousada/infra/MessageLog.java
   37 src/main/java/pousada/domain/Document.java
   35 src/main/java/pousada/notification/HousekeepingNotifier.java
```
