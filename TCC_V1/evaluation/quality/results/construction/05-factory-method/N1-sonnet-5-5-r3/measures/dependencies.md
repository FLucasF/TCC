# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 6 pacotes, 14 dependências entre eles, com ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `pousada` | 4 | 157 |
| `pousada.api` | 3 | 280 |
| `pousada.domain` | 13 | 300 |
| `pousada.identity` | 7 | 326 |
| `pousada.persistence` | 5 | 308 |
| `pousada.report` | 1 | 46 |

## As dependências entre os pacotes do projeto

```
pousada.api -> pousada
pousada.api -> pousada.domain
pousada.api -> pousada.identity
pousada.api -> pousada.report
pousada.domain -> pousada
pousada.domain -> pousada.identity
pousada.identity -> pousada
pousada.identity -> pousada.domain
pousada.persistence -> pousada.domain
pousada.persistence -> pousada.identity
pousada -> pousada.api
pousada -> pousada.domain
pousada -> pousada.persistence
pousada.report -> pousada.domain
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `pousada` | 3 | 3 |
| `pousada.api` | 4 | 1 |
| `pousada.domain` | 2 | 5 |
| `pousada.identity` | 2 | 3 |
| `pousada.persistence` | 2 | 1 |
| `pousada.report` | 1 | 1 |

## Os ciclos

O tsort achou ciclos; cada bloco abaixo é um ciclo (os pacotes que o formam):
```
--- um ciclo:
pousada
pousada.api
--- um ciclo:
pousada
pousada.persistence
pousada.identity
pousada.domain
--- um ciclo:
pousada
pousada.persistence
pousada.identity
--- um ciclo:
pousada.domain
pousada.identity
```

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `pousada` | sim | sim |  |  |  |  |
| `pousada.api` | sim |  | sim |  |  |  |
| `pousada.domain` |  |  |  |  |  |  |
| `pousada.identity` |  |  |  |  |  |  |
| `pousada.persistence` |  | sim | sim |  | sim |  |
| `pousada.report` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  177 src/main/java/pousada/persistence/ReservationRepository.java
  109 src/main/java/pousada/api/ReservationRequest.java
   96 src/main/java/pousada/api/Routes.java
   92 src/main/java/pousada/persistence/Database.java
   87 src/main/java/pousada/ReservationService.java
   83 src/main/java/pousada/domain/Reservation.java
   78 src/main/java/pousada/identity/IdentityKind.java
   78 src/main/java/pousada/identity/DocumentLanguage.java
   75 src/main/java/pousada/api/Views.java
   46 src/main/java/pousada/report/Reports.java
```
