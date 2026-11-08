# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 9 pacotes, 19 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `br.com.pousadamarealta` | 1 | 79 |
| `br.com.pousadamarealta.api` | 5 | 280 |
| `br.com.pousadamarealta.api.dto` | 6 | 151 |
| `br.com.pousadamarealta.config` | 1 | 16 |
| `br.com.pousadamarealta.domain` | 6 | 380 |
| `br.com.pousadamarealta.events` | 5 | 84 |
| `br.com.pousadamarealta.events.listeners` | 3 | 179 |
| `br.com.pousadamarealta.repository` | 4 | 450 |
| `br.com.pousadamarealta.service` | 8 | 235 |

## As dependências entre os pacotes do projeto

```
br.com.pousadamarealta.api -> br.com.pousadamarealta.api.dto
br.com.pousadamarealta.api -> br.com.pousadamarealta.domain
br.com.pousadamarealta.api -> br.com.pousadamarealta.repository
br.com.pousadamarealta.api -> br.com.pousadamarealta.service
br.com.pousadamarealta.api.dto -> br.com.pousadamarealta.domain
br.com.pousadamarealta -> br.com.pousadamarealta.api
br.com.pousadamarealta -> br.com.pousadamarealta.config
br.com.pousadamarealta -> br.com.pousadamarealta.events
br.com.pousadamarealta -> br.com.pousadamarealta.events.listeners
br.com.pousadamarealta -> br.com.pousadamarealta.repository
br.com.pousadamarealta -> br.com.pousadamarealta.service
br.com.pousadamarealta.events -> br.com.pousadamarealta.domain
br.com.pousadamarealta.events.listeners -> br.com.pousadamarealta.domain
br.com.pousadamarealta.events.listeners -> br.com.pousadamarealta.events
br.com.pousadamarealta.events.listeners -> br.com.pousadamarealta.repository
br.com.pousadamarealta.repository -> br.com.pousadamarealta.domain
br.com.pousadamarealta.service -> br.com.pousadamarealta.domain
br.com.pousadamarealta.service -> br.com.pousadamarealta.events
br.com.pousadamarealta.service -> br.com.pousadamarealta.repository
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `br.com.pousadamarealta` | 6 | 0 |
| `br.com.pousadamarealta.api` | 4 | 1 |
| `br.com.pousadamarealta.api.dto` | 1 | 1 |
| `br.com.pousadamarealta.config` | 0 | 1 |
| `br.com.pousadamarealta.domain` | 0 | 6 |
| `br.com.pousadamarealta.events` | 1 | 3 |
| `br.com.pousadamarealta.events.listeners` | 3 | 1 |
| `br.com.pousadamarealta.repository` | 1 | 4 |
| `br.com.pousadamarealta.service` | 3 | 2 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `br.com.pousadamarealta` | sim | sim | sim |  |  |  |
| `br.com.pousadamarealta.api` | sim |  |  |  |  |  |
| `br.com.pousadamarealta.api.dto` |  |  |  |  |  |  |
| `br.com.pousadamarealta.config` |  | sim |  |  |  |  |
| `br.com.pousadamarealta.domain` |  |  |  |  |  |  |
| `br.com.pousadamarealta.events` |  |  |  |  |  |  |
| `br.com.pousadamarealta.events.listeners` |  |  |  |  |  |  |
| `br.com.pousadamarealta.repository` |  | sim |  |  |  |  |
| `br.com.pousadamarealta.service` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  251 src/main/java/br/com/pousadamarealta/domain/Reservation.java
  224 src/main/java/br/com/pousadamarealta/repository/ReservationRepository.java
  161 src/main/java/br/com/pousadamarealta/service/ReservationService.java
  133 src/main/java/br/com/pousadamarealta/api/ReservationsController.java
   98 src/main/java/br/com/pousadamarealta/repository/Database.java
   79 src/main/java/br/com/pousadamarealta/Application.java
   74 src/main/java/br/com/pousadamarealta/service/ReportService.java
   69 src/main/java/br/com/pousadamarealta/repository/MessageRepository.java
   67 src/main/java/br/com/pousadamarealta/events/listeners/HousekeepingNotifier.java
   59 src/main/java/br/com/pousadamarealta/repository/RoomRepository.java
```
