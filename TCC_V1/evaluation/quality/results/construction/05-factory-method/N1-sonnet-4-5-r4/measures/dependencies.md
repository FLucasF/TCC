# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 6 pacotes, 12 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.pousada` | 1 | 100 |
| `com.pousada.api` | 4 | 217 |
| `com.pousada.api.dto` | 6 | 220 |
| `com.pousada.domain` | 15 | 522 |
| `com.pousada.infrastructure` | 4 | 468 |
| `com.pousada.service` | 8 | 268 |

## As dependências entre os pacotes do projeto

```
com.pousada.api -> com.pousada.api.dto
com.pousada.api -> com.pousada.domain
com.pousada.api -> com.pousada.infrastructure
com.pousada.api -> com.pousada.service
com.pousada.api.dto -> com.pousada.domain
com.pousada -> com.pousada.api
com.pousada -> com.pousada.api.dto
com.pousada -> com.pousada.infrastructure
com.pousada -> com.pousada.service
com.pousada.infrastructure -> com.pousada.domain
com.pousada.service -> com.pousada.domain
com.pousada.service -> com.pousada.infrastructure
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.pousada` | 4 | 0 |
| `com.pousada.api` | 4 | 1 |
| `com.pousada.api.dto` | 1 | 2 |
| `com.pousada.domain` | 0 | 4 |
| `com.pousada.infrastructure` | 1 | 3 |
| `com.pousada.service` | 2 | 2 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.pousada` | sim |  | sim |  |  |  |
| `com.pousada.api` | sim |  |  |  |  |  |
| `com.pousada.api.dto` |  |  | sim |  |  |  |
| `com.pousada.domain` |  |  |  |  |  |  |
| `com.pousada.infrastructure` |  | sim |  |  |  |  |
| `com.pousada.service` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  260 src/main/java/com/pousada/infrastructure/ReservationRepository.java
  183 src/main/java/com/pousada/domain/Reservation.java
  157 src/main/java/com/pousada/domain/DocumentGenerator.java
  145 src/main/java/com/pousada/service/ReservationService.java
  126 src/main/java/com/pousada/api/ReservationController.java
  100 src/main/java/com/pousada/Application.java
   84 src/main/java/com/pousada/api/dto/DocumentDto.java
   81 src/main/java/com/pousada/service/ReportService.java
   78 src/main/java/com/pousada/infrastructure/Database.java
   72 src/main/java/com/pousada/api/dto/ReservationDto.java
```
