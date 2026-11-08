# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 5 pacotes, 9 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `pousada` | 1 | 43 |
| `pousada.domain` | 19 | 385 |
| `pousada.persistence` | 6 | 331 |
| `pousada.service` | 3 | 187 |
| `pousada.web` | 1 | 118 |

## As dependências entre os pacotes do projeto

```
pousada.persistence -> pousada.domain
pousada -> pousada.domain
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
| `pousada` | 4 | 0 |
| `pousada.domain` | 0 | 4 |
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
| `pousada.persistence` |  | sim |  |  |  |  |
| `pousada.service` |  | sim |  |  |  |  |
| `pousada.web` | sim |  | sim |  |  |  |

## As maiores classes (linhas, src/main)

```
  126 src/main/java/pousada/persistence/ReservationRepository.java
  122 src/main/java/pousada/service/ReservationService.java
  118 src/main/java/pousada/web/Api.java
   97 src/main/java/pousada/persistence/Database.java
   58 src/main/java/pousada/service/ReportService.java
   57 src/main/java/pousada/domain/Reservation.java
   52 src/main/java/pousada/persistence/RoomRepository.java
   43 src/main/java/pousada/Main.java
   39 src/main/java/pousada/persistence/MessageRepository.java
   38 src/main/java/pousada/domain/Money.java
```
