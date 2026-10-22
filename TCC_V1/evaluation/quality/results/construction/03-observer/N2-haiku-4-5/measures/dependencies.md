# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 5 pacotes, 10 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.pousadamarealta` | 1 | 60 |
| `com.pousadamarealta.api` | 5 | 366 |
| `com.pousadamarealta.database` | 4 | 375 |
| `com.pousadamarealta.domain` | 11 | 482 |
| `com.pousadamarealta.service` | 6 | 409 |

## As dependências entre os pacotes do projeto

```
com.pousadamarealta.api -> com.pousadamarealta.database
com.pousadamarealta.api -> com.pousadamarealta.domain
com.pousadamarealta.api -> com.pousadamarealta.service
com.pousadamarealta -> com.pousadamarealta.api
com.pousadamarealta -> com.pousadamarealta.database
com.pousadamarealta -> com.pousadamarealta.domain
com.pousadamarealta -> com.pousadamarealta.service
com.pousadamarealta.database -> com.pousadamarealta.domain
com.pousadamarealta.service -> com.pousadamarealta.database
com.pousadamarealta.service -> com.pousadamarealta.domain
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.pousadamarealta` | 4 | 0 |
| `com.pousadamarealta.api` | 3 | 1 |
| `com.pousadamarealta.database` | 1 | 3 |
| `com.pousadamarealta.domain` | 0 | 4 |
| `com.pousadamarealta.service` | 2 | 2 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.pousadamarealta` | sim |  |  |  |  |  |
| `com.pousadamarealta.api` | sim |  | sim |  |  |  |
| `com.pousadamarealta.database` |  | sim |  |  |  |  |
| `com.pousadamarealta.domain` |  |  |  |  |  |  |
| `com.pousadamarealta.service` |  | sim |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  244 reservations/src/main/java/com/pousadamarealta/api/ReservationsHandler.java
  163 reservations/src/main/java/com/pousadamarealta/domain/Reservation.java
  152 reservations/src/main/java/com/pousadamarealta/database/ReservationDAO.java
  134 reservations/src/main/java/com/pousadamarealta/service/ReservationService.java
   94 reservations/src/main/java/com/pousadamarealta/service/ReportService.java
   87 reservations/src/main/java/com/pousadamarealta/database/RoomDAO.java
   79 reservations/src/main/java/com/pousadamarealta/service/GuestNotificationService.java
   79 reservations/src/main/java/com/pousadamarealta/database/DatabaseManager.java
   66 reservations/src/main/java/com/pousadamarealta/domain/Money.java
   60 reservations/src/main/java/com/pousadamarealta/App.java
```
