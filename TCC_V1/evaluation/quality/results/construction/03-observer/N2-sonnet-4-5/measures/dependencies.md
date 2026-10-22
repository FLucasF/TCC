# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 7 pacotes, 15 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `br.com.pousadamarealta` | 2 | 137 |
| `br.com.pousadamarealta.api` | 5 | 269 |
| `br.com.pousadamarealta.api.dto` | 6 | 148 |
| `br.com.pousadamarealta.domain` | 6 | 307 |
| `br.com.pousadamarealta.repository` | 3 | 381 |
| `br.com.pousadamarealta.service` | 8 | 248 |
| `br.com.pousadamarealta.service.observer` | 4 | 207 |

## As dependências entre os pacotes do projeto

```
br.com.pousadamarealta.api -> br.com.pousadamarealta.api.dto
br.com.pousadamarealta.api -> br.com.pousadamarealta.domain
br.com.pousadamarealta.api -> br.com.pousadamarealta.repository
br.com.pousadamarealta.api -> br.com.pousadamarealta.service
br.com.pousadamarealta.api.dto -> br.com.pousadamarealta.domain
br.com.pousadamarealta -> br.com.pousadamarealta.api
br.com.pousadamarealta -> br.com.pousadamarealta.repository
br.com.pousadamarealta -> br.com.pousadamarealta.service
br.com.pousadamarealta -> br.com.pousadamarealta.service.observer
br.com.pousadamarealta.repository -> br.com.pousadamarealta.domain
br.com.pousadamarealta.service -> br.com.pousadamarealta.domain
br.com.pousadamarealta.service -> br.com.pousadamarealta.repository
br.com.pousadamarealta.service -> br.com.pousadamarealta.service.observer
br.com.pousadamarealta.service.observer -> br.com.pousadamarealta.domain
br.com.pousadamarealta.service.observer -> br.com.pousadamarealta.repository
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `br.com.pousadamarealta` | 4 | 0 |
| `br.com.pousadamarealta.api` | 4 | 1 |
| `br.com.pousadamarealta.api.dto` | 1 | 1 |
| `br.com.pousadamarealta.domain` | 0 | 5 |
| `br.com.pousadamarealta.repository` | 1 | 4 |
| `br.com.pousadamarealta.service` | 3 | 2 |
| `br.com.pousadamarealta.service.observer` | 2 | 2 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `br.com.pousadamarealta` | sim | sim | sim |  |  |  |
| `br.com.pousadamarealta.api` | sim |  |  |  |  |  |
| `br.com.pousadamarealta.api.dto` |  |  |  |  |  |  |
| `br.com.pousadamarealta.domain` |  |  |  |  |  |  |
| `br.com.pousadamarealta.repository` |  | sim |  |  |  |  |
| `br.com.pousadamarealta.service` |  |  |  |  |  |  |
| `br.com.pousadamarealta.service.observer` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  244 src/main/java/br/com/pousadamarealta/repository/ReservationRepository.java
  186 src/main/java/br/com/pousadamarealta/domain/Reservation.java
  171 src/main/java/br/com/pousadamarealta/api/ReservationController.java
  127 src/main/java/br/com/pousadamarealta/service/ReservationService.java
   79 src/main/java/br/com/pousadamarealta/service/observer/GuestNotificationObserver.java
   76 src/main/java/br/com/pousadamarealta/DatabaseSetup.java
   72 src/main/java/br/com/pousadamarealta/repository/MessageRepository.java
   70 src/main/java/br/com/pousadamarealta/service/ReportService.java
   65 src/main/java/br/com/pousadamarealta/repository/RoomRepository.java
   61 src/main/java/br/com/pousadamarealta/service/observer/HousekeepingNotificationObserver.java
```
