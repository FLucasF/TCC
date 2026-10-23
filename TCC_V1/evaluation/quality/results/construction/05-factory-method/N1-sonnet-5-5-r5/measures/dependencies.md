# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 6 pacotes, 15 dependências entre eles, com ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `pousada` | 3 | 66 |
| `pousada.document` | 8 | 271 |
| `pousada.domain` | 12 | 238 |
| `pousada.persistence` | 5 | 295 |
| `pousada.service` | 4 | 184 |
| `pousada.web` | 3 | 285 |

## As dependências entre os pacotes do projeto

```
pousada.document -> pousada
pousada.document -> pousada.domain
pousada.domain -> pousada
pousada.persistence -> pousada.domain
pousada -> pousada.persistence
pousada -> pousada.service
pousada -> pousada.web
pousada.service -> pousada
pousada.service -> pousada.document
pousada.service -> pousada.domain
pousada.service -> pousada.persistence
pousada.web -> pousada
pousada.web -> pousada.document
pousada.web -> pousada.domain
pousada.web -> pousada.service
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `pousada` | 3 | 4 |
| `pousada.document` | 2 | 2 |
| `pousada.domain` | 1 | 4 |
| `pousada.persistence` | 1 | 2 |
| `pousada.service` | 4 | 2 |
| `pousada.web` | 4 | 1 |

## Os ciclos

O tsort achou ciclos; cada bloco abaixo é um ciclo (os pacotes que o formam):
```
--- um ciclo:
pousada
pousada.web
pousada.service
pousada.document
--- um ciclo:
pousada
pousada.web
pousada.service
pousada.persistence
pousada.domain
--- um ciclo:
pousada
pousada.web
pousada.service
--- um ciclo:
pousada
pousada.web
```

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `pousada` | sim |  |  |  |  |  |
| `pousada.document` |  |  |  |  |  |  |
| `pousada.domain` |  |  |  |  |  |  |
| `pousada.persistence` |  | sim | sim |  |  |  |
| `pousada.service` |  | sim |  |  |  |  |
| `pousada.web` | sim |  | sim |  |  |  |

## As maiores classes (linhas, src/main)

```
  168 src/main/java/pousada/persistence/ReservationRepository.java
  112 src/main/java/pousada/web/RequestParser.java
   99 src/main/java/pousada/service/ReservationService.java
   92 src/main/java/pousada/web/Routes.java
   91 src/main/java/pousada/persistence/Database.java
   81 src/main/java/pousada/web/Views.java
   66 src/main/java/pousada/domain/Reservation.java
   65 src/main/java/pousada/document/Language.java
   59 src/main/java/pousada/document/DocumentTypes.java
   48 src/main/java/pousada/document/DocumentType.java
```
