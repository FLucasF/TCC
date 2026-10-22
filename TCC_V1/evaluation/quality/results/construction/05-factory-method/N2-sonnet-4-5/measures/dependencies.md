# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 9 pacotes, 19 dependências entre eles, com ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.pousadamaralta` | 1 | 102 |
| `com.pousadamaralta.api.controller` | 5 | 414 |
| `com.pousadamaralta.api.dto` | 6 | 171 |
| `com.pousadamaralta.domain.identification` | 4 | 308 |
| `com.pousadamaralta.domain.model` | 7 | 291 |
| `com.pousadamaralta.domain.rateplan` | 4 | 57 |
| `com.pousadamaralta.domain.service` | 11 | 420 |
| `com.pousadamaralta.infrastructure.database` | 1 | 70 |
| `com.pousadamaralta.infrastructure.repository` | 3 | 425 |

## As dependências entre os pacotes do projeto

```
com.pousadamaralta.api.controller -> com.pousadamaralta.api.dto
com.pousadamaralta.api.controller -> com.pousadamaralta.domain.identification
com.pousadamaralta.api.controller -> com.pousadamaralta.domain.model
com.pousadamaralta.api.controller -> com.pousadamaralta.domain.service
com.pousadamaralta.api.dto -> com.pousadamaralta.domain.identification
com.pousadamaralta.api.dto -> com.pousadamaralta.domain.model
com.pousadamaralta -> com.pousadamaralta.api.controller
com.pousadamaralta -> com.pousadamaralta.domain.service
com.pousadamaralta -> com.pousadamaralta.infrastructure.database
com.pousadamaralta -> com.pousadamaralta.infrastructure.repository
com.pousadamaralta.domain.identification -> com.pousadamaralta.domain.model
com.pousadamaralta.domain.model -> com.pousadamaralta.domain.identification
com.pousadamaralta.domain.rateplan -> com.pousadamaralta.domain.model
com.pousadamaralta.domain.service -> com.pousadamaralta.domain.identification
com.pousadamaralta.domain.service -> com.pousadamaralta.domain.model
com.pousadamaralta.domain.service -> com.pousadamaralta.domain.rateplan
com.pousadamaralta.domain.service -> com.pousadamaralta.infrastructure.repository
com.pousadamaralta.infrastructure.repository -> com.pousadamaralta.domain.identification
com.pousadamaralta.infrastructure.repository -> com.pousadamaralta.domain.model
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.pousadamaralta` | 4 | 0 |
| `com.pousadamaralta.api.controller` | 4 | 1 |
| `com.pousadamaralta.api.dto` | 2 | 1 |
| `com.pousadamaralta.domain.identification` | 1 | 5 |
| `com.pousadamaralta.domain.model` | 1 | 6 |
| `com.pousadamaralta.domain.rateplan` | 1 | 1 |
| `com.pousadamaralta.domain.service` | 4 | 2 |
| `com.pousadamaralta.infrastructure.database` | 0 | 1 |
| `com.pousadamaralta.infrastructure.repository` | 2 | 2 |

## Os ciclos

O tsort achou ciclos; cada bloco abaixo é um ciclo (os pacotes que o formam):
```
--- um ciclo:
com.pousadamaralta.domain.identification
com.pousadamaralta.domain.model
```

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.pousadamaralta` | sim | sim | sim |  | sim |  |
| `com.pousadamaralta.api.controller` | sim |  |  |  |  |  |
| `com.pousadamaralta.api.dto` |  |  |  |  |  |  |
| `com.pousadamaralta.domain.identification` |  |  |  |  |  |  |
| `com.pousadamaralta.domain.model` |  |  |  |  |  |  |
| `com.pousadamaralta.domain.rateplan` |  |  |  |  |  |  |
| `com.pousadamaralta.domain.service` |  |  |  |  |  |  |
| `com.pousadamaralta.infrastructure.database` |  | sim |  |  |  |  |
| `com.pousadamaralta.infrastructure.repository` |  | sim |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  311 src/main/java/com/pousadamaralta/api/controller/ReservationController.java
  294 src/main/java/com/pousadamaralta/infrastructure/repository/ReservationRepository.java
  187 src/main/java/com/pousadamaralta/domain/service/ReservationService.java
  180 src/main/java/com/pousadamaralta/domain/model/Reservation.java
  102 src/main/java/com/pousadamaralta/Main.java
   96 src/main/java/com/pousadamaralta/domain/identification/PassportIdentification.java
   81 src/main/java/com/pousadamaralta/domain/identification/GuestIdentification.java
   79 src/main/java/com/pousadamaralta/domain/service/ReportService.java
   74 src/main/java/com/pousadamaralta/domain/service/PricingService.java
   71 src/main/java/com/pousadamaralta/domain/identification/CnpjIdentification.java
```
