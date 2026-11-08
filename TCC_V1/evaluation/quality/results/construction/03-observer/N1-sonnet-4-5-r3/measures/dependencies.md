# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 9 pacotes, 20 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.pousadamarealta` | 1 | 144 |
| `com.pousadamarealta.api` | 4 | 269 |
| `com.pousadamarealta.api.dto` | 2 | 17 |
| `com.pousadamarealta.domain` | 8 | 434 |
| `com.pousadamarealta.domain.events` | 5 | 44 |
| `com.pousadamarealta.infrastructure` | 1 | 74 |
| `com.pousadamarealta.repository` | 6 | 480 |
| `com.pousadamarealta.service` | 9 | 280 |
| `com.pousadamarealta.service.listeners` | 5 | 163 |

## As dependências entre os pacotes do projeto

```
com.pousadamarealta.api -> com.pousadamarealta.api.dto
com.pousadamarealta.api -> com.pousadamarealta.domain
com.pousadamarealta.api -> com.pousadamarealta.repository
com.pousadamarealta.api -> com.pousadamarealta.service
com.pousadamarealta -> com.pousadamarealta.api
com.pousadamarealta -> com.pousadamarealta.api.dto
com.pousadamarealta -> com.pousadamarealta.domain.events
com.pousadamarealta -> com.pousadamarealta.infrastructure
com.pousadamarealta -> com.pousadamarealta.repository
com.pousadamarealta -> com.pousadamarealta.service
com.pousadamarealta -> com.pousadamarealta.service.listeners
com.pousadamarealta.domain.events -> com.pousadamarealta.domain
com.pousadamarealta.repository -> com.pousadamarealta.domain
com.pousadamarealta.service -> com.pousadamarealta.domain
com.pousadamarealta.service -> com.pousadamarealta.domain.events
com.pousadamarealta.service -> com.pousadamarealta.repository
com.pousadamarealta.service.listeners -> com.pousadamarealta.domain
com.pousadamarealta.service.listeners -> com.pousadamarealta.domain.events
com.pousadamarealta.service.listeners -> com.pousadamarealta.repository
com.pousadamarealta.service.listeners -> com.pousadamarealta.service
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.pousadamarealta` | 7 | 0 |
| `com.pousadamarealta.api` | 4 | 1 |
| `com.pousadamarealta.api.dto` | 0 | 2 |
| `com.pousadamarealta.domain` | 0 | 5 |
| `com.pousadamarealta.domain.events` | 1 | 3 |
| `com.pousadamarealta.infrastructure` | 0 | 1 |
| `com.pousadamarealta.repository` | 1 | 4 |
| `com.pousadamarealta.service` | 3 | 3 |
| `com.pousadamarealta.service.listeners` | 4 | 1 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.pousadamarealta` | sim | sim | sim |  | sim |  |
| `com.pousadamarealta.api` | sim |  |  |  |  |  |
| `com.pousadamarealta.api.dto` |  |  |  |  |  |  |
| `com.pousadamarealta.domain` |  |  |  |  |  |  |
| `com.pousadamarealta.domain.events` |  |  |  |  |  |  |
| `com.pousadamarealta.infrastructure` |  | sim |  |  |  |  |
| `com.pousadamarealta.repository` |  | sim |  |  |  |  |
| `com.pousadamarealta.service` |  |  |  |  |  |  |
| `com.pousadamarealta.service.listeners` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  302 src/main/java/com/pousadamarealta/repository/JdbcReservationRepository.java
  187 src/main/java/com/pousadamarealta/domain/Reservation.java
  171 src/main/java/com/pousadamarealta/service/ReservationService.java
  147 src/main/java/com/pousadamarealta/api/ReservationController.java
  144 src/main/java/com/pousadamarealta/Application.java
   92 src/main/java/com/pousadamarealta/service/ReportService.java
   74 src/main/java/com/pousadamarealta/repository/JdbcMessageRepository.java
   74 src/main/java/com/pousadamarealta/infrastructure/DatabaseInitializer.java
   68 src/main/java/com/pousadamarealta/repository/JdbcRoomRepository.java
   64 src/main/java/com/pousadamarealta/domain/DocumentFormatter.java
```
