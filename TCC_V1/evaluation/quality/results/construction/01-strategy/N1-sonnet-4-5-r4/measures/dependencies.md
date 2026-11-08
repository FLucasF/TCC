# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 6 pacotes, 10 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.pousada` | 1 | 31 |
| `com.pousada.api` | 1 | 264 |
| `com.pousada.domain` | 11 | 390 |
| `com.pousada.domain.rateplan` | 7 | 271 |
| `com.pousada.repository` | 4 | 386 |
| `com.pousada.service` | 9 | 333 |

## As dependências entre os pacotes do projeto

```
com.pousada.api -> com.pousada.domain
com.pousada.api -> com.pousada.repository
com.pousada.api -> com.pousada.service
com.pousada -> com.pousada.api
com.pousada -> com.pousada.repository
com.pousada.domain -> com.pousada.domain.rateplan
com.pousada.repository -> com.pousada.domain
com.pousada.service -> com.pousada.domain
com.pousada.service -> com.pousada.domain.rateplan
com.pousada.service -> com.pousada.repository
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.pousada` | 2 | 0 |
| `com.pousada.api` | 3 | 1 |
| `com.pousada.domain` | 1 | 3 |
| `com.pousada.domain.rateplan` | 0 | 2 |
| `com.pousada.repository` | 1 | 3 |
| `com.pousada.service` | 3 | 1 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.pousada` | sim |  |  |  |  |  |
| `com.pousada.api` | sim |  | sim |  |  |  |
| `com.pousada.domain` |  |  |  |  |  |  |
| `com.pousada.domain.rateplan` |  |  |  |  |  |  |
| `com.pousada.repository` |  | sim |  |  | sim |  |
| `com.pousada.service` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  264 src/main/java/com/pousada/api/ApiController.java
  232 src/main/java/com/pousada/domain/Reservation.java
  229 src/main/java/com/pousada/repository/ReservationRepository.java
  121 src/main/java/com/pousada/service/ReservationService.java
   90 src/main/java/com/pousada/service/MessageService.java
   80 src/main/java/com/pousada/service/ReportService.java
   71 src/main/java/com/pousada/repository/Database.java
   59 src/main/java/com/pousada/repository/MessageRepository.java
   59 src/main/java/com/pousada/domain/Message.java
   51 src/main/java/com/pousada/domain/rateplan/CorporateRatePlan.java
```
