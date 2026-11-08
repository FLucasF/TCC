# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 5 pacotes, 8 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `pousada` | 1 | 25 |
| `pousada.domain` | 19 | 510 |
| `pousada.persistence` | 4 | 278 |
| `pousada.service` | 3 | 184 |
| `pousada.web` | 2 | 157 |

## As dependências entre os pacotes do projeto

```
pousada.persistence -> pousada.domain
pousada -> pousada.persistence
pousada -> pousada.service
pousada -> pousada.web
pousada.service -> pousada.domain
pousada.service -> pousada.persistence
pousada.web -> pousada.domain
pousada.web -> pousada.service
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `pousada` | 3 | 0 |
| `pousada.domain` | 0 | 3 |
| `pousada.persistence` | 1 | 2 |
| `pousada.service` | 2 | 2 |
| `pousada.web` | 2 | 1 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `pousada` | sim |  |  |  |  |  |
| `pousada.domain` |  |  |  |  |  |  |
| `pousada.persistence` |  | sim |  |  | sim |  |
| `pousada.service` |  | sim |  |  |  |  |
| `pousada.web` | sim |  | sim |  |  |  |

## As maiores classes (linhas, src/main)

```
  152 src/main/java/pousada/persistence/ReservationRepository.java
  122 src/main/java/pousada/service/ReservationService.java
   92 src/main/java/pousada/web/RequestParser.java
   89 src/main/java/pousada/persistence/Database.java
   68 src/main/java/pousada/domain/Reservation.java
   65 src/main/java/pousada/web/Api.java
   47 src/main/java/pousada/service/Reports.java
   38 src/main/java/pousada/domain/Receipt.java
   37 src/main/java/pousada/persistence/MessageRepository.java
   36 src/main/java/pousada/domain/WeekendPackagePlan.java
```
