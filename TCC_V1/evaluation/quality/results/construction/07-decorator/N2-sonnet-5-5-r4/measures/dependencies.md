# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 6 pacotes, 12 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `pousada` | 1 | 44 |
| `pousada.domain` | 10 | 261 |
| `pousada.messaging` | 11 | 186 |
| `pousada.persistence` | 6 | 365 |
| `pousada.service` | 3 | 159 |
| `pousada.web` | 2 | 161 |

## As dependências entre os pacotes do projeto

```
pousada.persistence -> pousada.domain
pousada.persistence -> pousada.messaging
pousada -> pousada.messaging
pousada -> pousada.persistence
pousada -> pousada.service
pousada -> pousada.web
pousada.service -> pousada.domain
pousada.service -> pousada.messaging
pousada.service -> pousada.persistence
pousada.web -> pousada.domain
pousada.web -> pousada.persistence
pousada.web -> pousada.service
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `pousada` | 4 | 0 |
| `pousada.domain` | 0 | 3 |
| `pousada.messaging` | 0 | 3 |
| `pousada.persistence` | 2 | 3 |
| `pousada.service` | 3 | 2 |
| `pousada.web` | 3 | 1 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `pousada` | sim |  |  |  |  |  |
| `pousada.domain` |  |  |  |  |  |  |
| `pousada.messaging` |  |  |  |  |  |  |
| `pousada.persistence` |  | sim |  |  |  |  |
| `pousada.service` |  |  |  |  |  |  |
| `pousada.web` | sim |  | sim |  |  |  |

## As maiores classes (linhas, src/main)

```
  153 src/main/java/pousada/persistence/ReservationRepository.java
  101 src/main/java/pousada/service/ReservationService.java
   92 src/main/java/pousada/web/Routes.java
   76 src/main/java/pousada/domain/Reservation.java
   69 src/main/java/pousada/web/ReservationRequestParser.java
   64 src/main/java/pousada/persistence/Database.java
   53 src/main/java/pousada/persistence/MessageRepository.java
   49 src/main/java/pousada/service/ReportService.java
   48 src/main/java/pousada/persistence/Schema.java
   47 src/main/java/pousada/persistence/RoomRepository.java
```
