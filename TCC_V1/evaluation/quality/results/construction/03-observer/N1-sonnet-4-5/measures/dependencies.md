# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 11 pacotes, 31 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.pousadamarealta` | 1 | 104 |
| `com.pousadamarealta.config` | 1 | 89 |
| `com.pousadamarealta.controller` | 4 | 273 |
| `com.pousadamarealta.domain.enums` | 4 | 57 |
| `com.pousadamarealta.domain.events` | 3 | 27 |
| `com.pousadamarealta.domain.model` | 4 | 274 |
| `com.pousadamarealta.dto` | 6 | 147 |
| `com.pousadamarealta.repository` | 3 | 382 |
| `com.pousadamarealta.service` | 10 | 342 |
| `com.pousadamarealta.service.event` | 2 | 27 |
| `com.pousadamarealta.service.event.listeners` | 3 | 250 |

## As dependências entre os pacotes do projeto

```
com.pousadamarealta -> com.pousadamarealta.config
com.pousadamarealta -> com.pousadamarealta.controller
com.pousadamarealta -> com.pousadamarealta.domain.enums
com.pousadamarealta -> com.pousadamarealta.domain.model
com.pousadamarealta -> com.pousadamarealta.repository
com.pousadamarealta -> com.pousadamarealta.service
com.pousadamarealta -> com.pousadamarealta.service.event
com.pousadamarealta -> com.pousadamarealta.service.event.listeners
com.pousadamarealta.controller -> com.pousadamarealta.domain.enums
com.pousadamarealta.controller -> com.pousadamarealta.domain.model
com.pousadamarealta.controller -> com.pousadamarealta.dto
com.pousadamarealta.controller -> com.pousadamarealta.repository
com.pousadamarealta.controller -> com.pousadamarealta.service
com.pousadamarealta.domain.events -> com.pousadamarealta.domain.model
com.pousadamarealta.domain.model -> com.pousadamarealta.domain.enums
com.pousadamarealta.dto -> com.pousadamarealta.domain.enums
com.pousadamarealta.dto -> com.pousadamarealta.domain.model
com.pousadamarealta.repository -> com.pousadamarealta.config
com.pousadamarealta.repository -> com.pousadamarealta.domain.enums
com.pousadamarealta.repository -> com.pousadamarealta.domain.model
com.pousadamarealta.service -> com.pousadamarealta.domain.enums
com.pousadamarealta.service -> com.pousadamarealta.domain.events
com.pousadamarealta.service -> com.pousadamarealta.domain.model
com.pousadamarealta.service -> com.pousadamarealta.repository
com.pousadamarealta.service -> com.pousadamarealta.service.event
com.pousadamarealta.service.event -> com.pousadamarealta.domain.events
com.pousadamarealta.service.event.listeners -> com.pousadamarealta.domain.enums
com.pousadamarealta.service.event.listeners -> com.pousadamarealta.domain.events
com.pousadamarealta.service.event.listeners -> com.pousadamarealta.domain.model
com.pousadamarealta.service.event.listeners -> com.pousadamarealta.repository
com.pousadamarealta.service.event.listeners -> com.pousadamarealta.service.event
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.pousadamarealta` | 8 | 0 |
| `com.pousadamarealta.config` | 0 | 2 |
| `com.pousadamarealta.controller` | 5 | 1 |
| `com.pousadamarealta.domain.enums` | 0 | 7 |
| `com.pousadamarealta.domain.events` | 1 | 3 |
| `com.pousadamarealta.domain.model` | 1 | 7 |
| `com.pousadamarealta.dto` | 2 | 1 |
| `com.pousadamarealta.repository` | 3 | 4 |
| `com.pousadamarealta.service` | 5 | 2 |
| `com.pousadamarealta.service.event` | 1 | 3 |
| `com.pousadamarealta.service.event.listeners` | 5 | 1 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.pousadamarealta` | sim |  | sim |  |  |  |
| `com.pousadamarealta.config` |  | sim |  |  | sim |  |
| `com.pousadamarealta.controller` | sim |  |  |  |  |  |
| `com.pousadamarealta.domain.enums` |  |  |  |  |  |  |
| `com.pousadamarealta.domain.events` |  |  |  |  |  |  |
| `com.pousadamarealta.domain.model` |  |  |  |  |  |  |
| `com.pousadamarealta.dto` |  |  | sim |  |  |  |
| `com.pousadamarealta.repository` |  | sim |  |  |  |  |
| `com.pousadamarealta.service` |  |  |  |  |  |  |
| `com.pousadamarealta.service.event` |  |  |  |  |  |  |
| `com.pousadamarealta.service.event.listeners` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  235 src/main/java/com/pousadamarealta/repository/ReservationRepository.java
  198 src/main/java/com/pousadamarealta/service/ReservationService.java
  175 src/main/java/com/pousadamarealta/controller/ReservationController.java
  174 src/main/java/com/pousadamarealta/domain/model/Reservation.java
  111 src/main/java/com/pousadamarealta/service/event/listeners/GuestNotificationListener.java
  104 src/main/java/com/pousadamarealta/App.java
   89 src/main/java/com/pousadamarealta/config/Database.java
   81 src/main/java/com/pousadamarealta/service/ReportService.java
   77 src/main/java/com/pousadamarealta/repository/RoomRepository.java
   72 src/main/java/com/pousadamarealta/service/event/listeners/FinanceNotificationListener.java
```
