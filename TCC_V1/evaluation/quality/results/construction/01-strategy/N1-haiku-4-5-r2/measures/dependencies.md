# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 6 pacotes, 11 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.pousada` | 2 | 242 |
| `com.pousada.api.dto` | 5 | 140 |
| `com.pousada.domain` | 6 | 160 |
| `com.pousada.domain.rateplans` | 10 | 291 |
| `com.pousada.infra` | 5 | 369 |
| `com.pousada.service` | 3 | 325 |

## As dependências entre os pacotes do projeto

```
com.pousada.api.dto -> com.pousada.domain
com.pousada.api.dto -> com.pousada.infra
com.pousada -> com.pousada.api.dto
com.pousada -> com.pousada.domain
com.pousada -> com.pousada.infra
com.pousada -> com.pousada.service
com.pousada.domain.rateplans -> com.pousada.domain
com.pousada.infra -> com.pousada.domain
com.pousada.service -> com.pousada.domain
com.pousada.service -> com.pousada.domain.rateplans
com.pousada.service -> com.pousada.infra
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.pousada` | 4 | 0 |
| `com.pousada.api.dto` | 2 | 1 |
| `com.pousada.domain` | 0 | 5 |
| `com.pousada.domain.rateplans` | 1 | 1 |
| `com.pousada.infra` | 1 | 3 |
| `com.pousada.service` | 3 | 1 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.pousada` | sim |  |  |  |  | com.google |
| `com.pousada.api.dto` |  |  |  |  |  |  |
| `com.pousada.domain` |  |  |  |  |  |  |
| `com.pousada.domain.rateplans` |  |  |  |  |  |  |
| `com.pousada.infra` |  | sim |  |  |  |  |
| `com.pousada.service` |  | sim |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  242 src/main/java/com/pousada/Application.java
  209 src/main/java/com/pousada/service/ReservationService.java
  184 src/main/java/com/pousada/infra/ReservationRepository.java
   96 src/main/java/com/pousada/service/ReportService.java
   93 src/main/java/com/pousada/infra/Database.java
   65 src/main/java/com/pousada/domain/Reservation.java
   50 src/main/java/com/pousada/domain/rateplans/WeekendPackageRatePlan.java
   48 src/main/java/com/pousada/infra/RoomRepository.java
   47 src/main/java/com/pousada/api/dto/ReservationResponse.java
   46 src/main/java/com/pousada/domain/rateplans/CorporateRatePlan.java
```
