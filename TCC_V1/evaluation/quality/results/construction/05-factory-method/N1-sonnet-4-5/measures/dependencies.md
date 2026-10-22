# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 7 pacotes, 13 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.pousada.maralta` | 1 | 49 |
| `com.pousada.maralta.controller` | 2 | 227 |
| `com.pousada.maralta.controller.dto` | 7 | 220 |
| `com.pousada.maralta.domain` | 12 | 312 |
| `com.pousada.maralta.infrastructure` | 1 | 102 |
| `com.pousada.maralta.repository` | 6 | 422 |
| `com.pousada.maralta.service` | 12 | 505 |

## As dependências entre os pacotes do projeto

```
com.pousada.maralta -> com.pousada.maralta.controller
com.pousada.maralta -> com.pousada.maralta.infrastructure
com.pousada.maralta -> com.pousada.maralta.repository
com.pousada.maralta -> com.pousada.maralta.service
com.pousada.maralta.controller -> com.pousada.maralta.controller.dto
com.pousada.maralta.controller -> com.pousada.maralta.domain
com.pousada.maralta.controller -> com.pousada.maralta.repository
com.pousada.maralta.controller -> com.pousada.maralta.service
com.pousada.maralta.controller.dto -> com.pousada.maralta.domain
com.pousada.maralta.repository -> com.pousada.maralta.domain
com.pousada.maralta.repository -> com.pousada.maralta.infrastructure
com.pousada.maralta.service -> com.pousada.maralta.domain
com.pousada.maralta.service -> com.pousada.maralta.repository
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.pousada.maralta` | 4 | 0 |
| `com.pousada.maralta.controller` | 4 | 1 |
| `com.pousada.maralta.controller.dto` | 1 | 1 |
| `com.pousada.maralta.domain` | 0 | 4 |
| `com.pousada.maralta.infrastructure` | 0 | 2 |
| `com.pousada.maralta.repository` | 2 | 3 |
| `com.pousada.maralta.service` | 2 | 2 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.pousada.maralta` | sim |  | sim |  |  |  |
| `com.pousada.maralta.controller` | sim |  |  |  |  |  |
| `com.pousada.maralta.controller.dto` |  |  |  |  |  |  |
| `com.pousada.maralta.domain` |  |  |  |  |  |  |
| `com.pousada.maralta.infrastructure` |  | sim |  |  | sim |  |
| `com.pousada.maralta.repository` |  | sim |  |  |  |  |
| `com.pousada.maralta.service` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  259 src/main/java/com/pousada/maralta/repository/JdbcReservationRepository.java
  227 src/main/java/com/pousada/maralta/controller/ApiController.java
  196 src/main/java/com/pousada/maralta/service/DocumentGenerator.java
  163 src/main/java/com/pousada/maralta/domain/Reservation.java
  150 src/main/java/com/pousada/maralta/service/ReservationService.java
  107 src/main/java/com/pousada/maralta/controller/dto/ReservationResponse.java
  102 src/main/java/com/pousada/maralta/infrastructure/DatabaseConfig.java
   74 src/main/java/com/pousada/maralta/service/ReportService.java
   66 src/main/java/com/pousada/maralta/service/PricingService.java
   64 src/main/java/com/pousada/maralta/repository/JdbcMessageRepository.java
```
