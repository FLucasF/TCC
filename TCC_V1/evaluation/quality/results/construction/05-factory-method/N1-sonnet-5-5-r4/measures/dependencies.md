# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 7 pacotes, 18 dependências entre eles, com ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `pousada` | 1 | 38 |
| `pousada.document` | 3 | 88 |
| `pousada.domain` | 13 | 294 |
| `pousada.identity` | 10 | 240 |
| `pousada.persistence` | 5 | 315 |
| `pousada.service` | 4 | 194 |
| `pousada.web` | 3 | 294 |

## As dependências entre os pacotes do projeto

```
pousada.document -> pousada.domain
pousada.domain -> pousada.document
pousada.domain -> pousada.identity
pousada.identity -> pousada.document
pousada.identity -> pousada.domain
pousada.persistence -> pousada.domain
pousada.persistence -> pousada.identity
pousada -> pousada.persistence
pousada -> pousada.service
pousada -> pousada.web
pousada.service -> pousada.document
pousada.service -> pousada.domain
pousada.service -> pousada.identity
pousada.service -> pousada.persistence
pousada.web -> pousada.document
pousada.web -> pousada.domain
pousada.web -> pousada.identity
pousada.web -> pousada.service
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `pousada` | 3 | 0 |
| `pousada.document` | 1 | 4 |
| `pousada.domain` | 2 | 5 |
| `pousada.identity` | 2 | 4 |
| `pousada.persistence` | 2 | 2 |
| `pousada.service` | 4 | 2 |
| `pousada.web` | 4 | 1 |

## Os ciclos

O tsort achou ciclos; cada bloco abaixo é um ciclo (os pacotes que o formam):
```
--- um ciclo:
pousada.identity
pousada.domain
--- um ciclo:
pousada.document
pousada.domain
```

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `pousada` | sim |  |  |  |  |  |
| `pousada.document` |  |  |  |  |  |  |
| `pousada.domain` |  |  |  |  |  |  |
| `pousada.identity` |  |  |  |  |  |  |
| `pousada.persistence` |  | sim | sim |  | sim |  |
| `pousada.service` |  | sim |  |  |  |  |
| `pousada.web` | sim |  | sim |  |  |  |

## As maiores classes (linhas, src/main)

```
  175 src/main/java/pousada/persistence/ReservationRepository.java
  117 src/main/java/pousada/web/RequestParser.java
  104 src/main/java/pousada/service/ReservationService.java
  101 src/main/java/pousada/persistence/Database.java
   96 src/main/java/pousada/web/Api.java
   81 src/main/java/pousada/web/Views.java
   65 src/main/java/pousada/domain/Reservation.java
   64 src/main/java/pousada/document/Wording.java
   57 src/main/java/pousada/identity/CnpjIdentity.java
   56 src/main/java/pousada/service/ReportService.java
```
