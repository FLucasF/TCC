# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 9 pacotes, 21 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.pousadamarealta` | 1 | 94 |
| `com.pousadamarealta.application.notification` | 6 | 241 |
| `com.pousadamarealta.application.service` | 8 | 234 |
| `com.pousadamarealta.domain.event` | 5 | 46 |
| `com.pousadamarealta.domain.model` | 7 | 314 |
| `com.pousadamarealta.domain.repository` | 3 | 33 |
| `com.pousadamarealta.infrastructure.http` | 1 | 194 |
| `com.pousadamarealta.infrastructure.http.dto` | 6 | 152 |
| `com.pousadamarealta.infrastructure.persistence` | 4 | 343 |

## As dependências entre os pacotes do projeto

```
com.pousadamarealta.application.notification -> com.pousadamarealta.domain.event
com.pousadamarealta.application.notification -> com.pousadamarealta.domain.model
com.pousadamarealta.application.notification -> com.pousadamarealta.domain.repository
com.pousadamarealta.application.service -> com.pousadamarealta.domain.event
com.pousadamarealta.application.service -> com.pousadamarealta.domain.model
com.pousadamarealta.application.service -> com.pousadamarealta.domain.repository
com.pousadamarealta -> com.pousadamarealta.application.notification
com.pousadamarealta -> com.pousadamarealta.application.service
com.pousadamarealta -> com.pousadamarealta.domain.event
com.pousadamarealta -> com.pousadamarealta.domain.repository
com.pousadamarealta -> com.pousadamarealta.infrastructure.http
com.pousadamarealta -> com.pousadamarealta.infrastructure.persistence
com.pousadamarealta.domain.event -> com.pousadamarealta.domain.model
com.pousadamarealta.domain.repository -> com.pousadamarealta.domain.model
com.pousadamarealta.infrastructure.http -> com.pousadamarealta.application.service
com.pousadamarealta.infrastructure.http -> com.pousadamarealta.domain.model
com.pousadamarealta.infrastructure.http -> com.pousadamarealta.domain.repository
com.pousadamarealta.infrastructure.http -> com.pousadamarealta.infrastructure.http.dto
com.pousadamarealta.infrastructure.http.dto -> com.pousadamarealta.domain.model
com.pousadamarealta.infrastructure.persistence -> com.pousadamarealta.domain.model
com.pousadamarealta.infrastructure.persistence -> com.pousadamarealta.domain.repository
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.pousadamarealta` | 6 | 0 |
| `com.pousadamarealta.application.notification` | 3 | 1 |
| `com.pousadamarealta.application.service` | 3 | 2 |
| `com.pousadamarealta.domain.event` | 1 | 3 |
| `com.pousadamarealta.domain.model` | 0 | 7 |
| `com.pousadamarealta.domain.repository` | 1 | 5 |
| `com.pousadamarealta.infrastructure.http` | 4 | 1 |
| `com.pousadamarealta.infrastructure.http.dto` | 1 | 1 |
| `com.pousadamarealta.infrastructure.persistence` | 2 | 1 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.pousadamarealta` | sim | sim | sim |  | sim |  |
| `com.pousadamarealta.application.notification` |  |  |  |  |  |  |
| `com.pousadamarealta.application.service` |  |  |  |  |  |  |
| `com.pousadamarealta.domain.event` |  |  |  |  |  |  |
| `com.pousadamarealta.domain.model` |  |  |  |  |  |  |
| `com.pousadamarealta.domain.repository` |  |  |  |  |  |  |
| `com.pousadamarealta.infrastructure.http` | sim |  |  |  |  |  |
| `com.pousadamarealta.infrastructure.http.dto` |  |  | sim |  |  |  |
| `com.pousadamarealta.infrastructure.persistence` |  | sim |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  202 src/main/java/com/pousadamarealta/infrastructure/persistence/PostgresReservationRepository.java
  195 src/main/java/com/pousadamarealta/domain/model/Reservation.java
  194 src/main/java/com/pousadamarealta/infrastructure/http/ReservationController.java
  112 src/main/java/com/pousadamarealta/application/service/ReservationService.java
   94 src/main/java/com/pousadamarealta/Main.java
   80 src/main/java/com/pousadamarealta/application/service/ReportService.java
   77 src/main/java/com/pousadamarealta/application/notification/GuestNotificationListener.java
   68 src/main/java/com/pousadamarealta/infrastructure/persistence/PostgresMessageRepository.java
   50 src/main/java/com/pousadamarealta/infrastructure/http/dto/ReservationResponse.java
   45 src/main/java/com/pousadamarealta/domain/model/Message.java
```
