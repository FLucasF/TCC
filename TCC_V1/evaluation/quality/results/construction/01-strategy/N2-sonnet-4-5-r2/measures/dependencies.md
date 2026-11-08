# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 6 pacotes, 10 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.pousadamarealta` | 1 | 101 |
| `com.pousadamarealta.domain.model` | 7 | 269 |
| `com.pousadamarealta.domain.service` | 10 | 355 |
| `com.pousadamarealta.domain.service.rateplan` | 8 | 292 |
| `com.pousadamarealta.infrastructure.database` | 4 | 445 |
| `com.pousadamarealta.infrastructure.web` | 6 | 403 |

## As dependências entre os pacotes do projeto

```
com.pousadamarealta -> com.pousadamarealta.domain.service
com.pousadamarealta -> com.pousadamarealta.infrastructure.database
com.pousadamarealta -> com.pousadamarealta.infrastructure.web
com.pousadamarealta.domain.service -> com.pousadamarealta.domain.model
com.pousadamarealta.domain.service -> com.pousadamarealta.domain.service.rateplan
com.pousadamarealta.domain.service -> com.pousadamarealta.infrastructure.database
com.pousadamarealta.domain.service.rateplan -> com.pousadamarealta.domain.model
com.pousadamarealta.infrastructure.database -> com.pousadamarealta.domain.model
com.pousadamarealta.infrastructure.web -> com.pousadamarealta.domain.model
com.pousadamarealta.infrastructure.web -> com.pousadamarealta.domain.service
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.pousadamarealta` | 3 | 0 |
| `com.pousadamarealta.domain.model` | 0 | 4 |
| `com.pousadamarealta.domain.service` | 3 | 2 |
| `com.pousadamarealta.domain.service.rateplan` | 1 | 1 |
| `com.pousadamarealta.infrastructure.database` | 1 | 2 |
| `com.pousadamarealta.infrastructure.web` | 2 | 1 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.pousadamarealta` | sim | sim | sim |  | sim |  |
| `com.pousadamarealta.domain.model` |  |  |  |  |  |  |
| `com.pousadamarealta.domain.service` |  |  |  |  |  |  |
| `com.pousadamarealta.domain.service.rateplan` |  |  |  |  |  |  |
| `com.pousadamarealta.infrastructure.database` |  | sim |  |  |  |  |
| `com.pousadamarealta.infrastructure.web` | sim |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  258 src/main/java/com/pousadamarealta/infrastructure/web/ReservationController.java
  241 src/main/java/com/pousadamarealta/infrastructure/database/ReservationRepository.java
  165 src/main/java/com/pousadamarealta/domain/service/ReservationService.java
  158 src/main/java/com/pousadamarealta/domain/model/Reservation.java
  101 src/main/java/com/pousadamarealta/Application.java
   85 src/main/java/com/pousadamarealta/domain/service/MessageService.java
   83 src/main/java/com/pousadamarealta/domain/service/ReportService.java
   74 src/main/java/com/pousadamarealta/infrastructure/database/DatabaseInitializer.java
   67 src/main/java/com/pousadamarealta/infrastructure/database/MessageRepository.java
   63 src/main/java/com/pousadamarealta/infrastructure/database/RoomRepository.java
```
