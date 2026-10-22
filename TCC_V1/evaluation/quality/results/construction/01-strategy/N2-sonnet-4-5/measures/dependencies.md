# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 10 pacotes, 24 dependências entre eles, com ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.pousadamarealta` | 1 | 85 |
| `com.pousadamarealta.api` | 5 | 282 |
| `com.pousadamarealta.api.dto` | 6 | 146 |
| `com.pousadamarealta.domain` | 6 | 309 |
| `com.pousadamarealta.domain.rateplan` | 7 | 230 |
| `com.pousadamarealta.exception` | 6 | 42 |
| `com.pousadamarealta.infrastructure` | 1 | 82 |
| `com.pousadamarealta.repository` | 3 | 404 |
| `com.pousadamarealta.service` | 3 | 190 |
| `com.pousadamarealta.util` | 2 | 109 |

## As dependências entre os pacotes do projeto

```
com.pousadamarealta.api -> com.pousadamarealta.api.dto
com.pousadamarealta.api -> com.pousadamarealta.domain
com.pousadamarealta.api -> com.pousadamarealta.exception
com.pousadamarealta.api -> com.pousadamarealta.repository
com.pousadamarealta.api -> com.pousadamarealta.service
com.pousadamarealta.api.dto -> com.pousadamarealta.domain
com.pousadamarealta -> com.pousadamarealta.api
com.pousadamarealta -> com.pousadamarealta.exception
com.pousadamarealta -> com.pousadamarealta.infrastructure
com.pousadamarealta -> com.pousadamarealta.repository
com.pousadamarealta -> com.pousadamarealta.service
com.pousadamarealta.domain -> com.pousadamarealta.domain.rateplan
com.pousadamarealta.domain -> com.pousadamarealta.exception
com.pousadamarealta.domain -> com.pousadamarealta.util
com.pousadamarealta.domain.rateplan -> com.pousadamarealta.domain
com.pousadamarealta.domain.rateplan -> com.pousadamarealta.exception
com.pousadamarealta.domain.rateplan -> com.pousadamarealta.util
com.pousadamarealta.repository -> com.pousadamarealta.domain
com.pousadamarealta.repository -> com.pousadamarealta.exception
com.pousadamarealta.service -> com.pousadamarealta.domain
com.pousadamarealta.service -> com.pousadamarealta.exception
com.pousadamarealta.service -> com.pousadamarealta.repository
com.pousadamarealta.service -> com.pousadamarealta.util
com.pousadamarealta.util -> com.pousadamarealta.domain
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.pousadamarealta` | 5 | 0 |
| `com.pousadamarealta.api` | 5 | 1 |
| `com.pousadamarealta.api.dto` | 1 | 1 |
| `com.pousadamarealta.domain` | 3 | 6 |
| `com.pousadamarealta.domain.rateplan` | 3 | 1 |
| `com.pousadamarealta.exception` | 0 | 6 |
| `com.pousadamarealta.infrastructure` | 0 | 1 |
| `com.pousadamarealta.repository` | 2 | 3 |
| `com.pousadamarealta.service` | 4 | 2 |
| `com.pousadamarealta.util` | 1 | 3 |

## Os ciclos

O tsort achou ciclos; cada bloco abaixo é um ciclo (os pacotes que o formam):
```
--- um ciclo:
com.pousadamarealta.domain
com.pousadamarealta.domain.rateplan
--- um ciclo:
com.pousadamarealta.domain
com.pousadamarealta.util
```

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.pousadamarealta` | sim | sim | sim |  |  |  |
| `com.pousadamarealta.api` | sim |  | sim |  |  |  |
| `com.pousadamarealta.api.dto` |  |  |  |  |  |  |
| `com.pousadamarealta.domain` |  |  |  |  |  |  |
| `com.pousadamarealta.domain.rateplan` |  |  |  |  |  |  |
| `com.pousadamarealta.exception` |  |  |  |  |  |  |
| `com.pousadamarealta.infrastructure` |  | sim |  |  | sim |  |
| `com.pousadamarealta.repository` |  | sim |  |  |  |  |
| `com.pousadamarealta.service` |  |  |  |  |  |  |
| `com.pousadamarealta.util` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  242 src/main/java/com/pousadamarealta/repository/ReservationRepository.java
  199 src/main/java/com/pousadamarealta/domain/Reservation.java
  128 src/main/java/com/pousadamarealta/api/ReservationController.java
   97 src/main/java/com/pousadamarealta/repository/RoomRepository.java
   85 src/main/java/com/pousadamarealta/App.java
   82 src/main/java/com/pousadamarealta/infrastructure/Database.java
   79 src/main/java/com/pousadamarealta/service/ReservationService.java
   69 src/main/java/com/pousadamarealta/service/ReportService.java
   65 src/main/java/com/pousadamarealta/repository/MessageRepository.java
   57 src/main/java/com/pousadamarealta/util/DocumentFormatter.java
```
