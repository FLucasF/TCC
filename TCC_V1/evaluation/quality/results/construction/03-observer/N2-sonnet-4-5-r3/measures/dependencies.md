# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 7 pacotes, 15 dependências entre eles, com ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.pousadamarealta` | 1 | 56 |
| `com.pousadamarealta.api` | 7 | 411 |
| `com.pousadamarealta.domain` | 7 | 236 |
| `com.pousadamarealta.notification` | 4 | 145 |
| `com.pousadamarealta.repository` | 7 | 539 |
| `com.pousadamarealta.service` | 8 | 257 |
| `com.pousadamarealta.util` | 2 | 64 |

## As dependências entre os pacotes do projeto

```
com.pousadamarealta.api -> com.pousadamarealta.domain
com.pousadamarealta.api -> com.pousadamarealta.repository
com.pousadamarealta.api -> com.pousadamarealta.service
com.pousadamarealta -> com.pousadamarealta.api
com.pousadamarealta -> com.pousadamarealta.notification
com.pousadamarealta -> com.pousadamarealta.repository
com.pousadamarealta -> com.pousadamarealta.service
com.pousadamarealta.notification -> com.pousadamarealta.domain
com.pousadamarealta.notification -> com.pousadamarealta.service
com.pousadamarealta.notification -> com.pousadamarealta.util
com.pousadamarealta.repository -> com.pousadamarealta.domain
com.pousadamarealta.service -> com.pousadamarealta.domain
com.pousadamarealta.service -> com.pousadamarealta.notification
com.pousadamarealta.service -> com.pousadamarealta.repository
com.pousadamarealta.util -> com.pousadamarealta.domain
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.pousadamarealta` | 4 | 0 |
| `com.pousadamarealta.api` | 3 | 1 |
| `com.pousadamarealta.domain` | 0 | 5 |
| `com.pousadamarealta.notification` | 3 | 2 |
| `com.pousadamarealta.repository` | 1 | 3 |
| `com.pousadamarealta.service` | 3 | 3 |
| `com.pousadamarealta.util` | 1 | 1 |

## Os ciclos

O tsort achou ciclos; cada bloco abaixo é um ciclo (os pacotes que o formam):
```
--- um ciclo:
com.pousadamarealta.service
com.pousadamarealta.notification
```

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.pousadamarealta` | sim |  |  |  |  |  |
| `com.pousadamarealta.api` | sim |  | sim |  |  |  |
| `com.pousadamarealta.domain` |  |  |  |  |  |  |
| `com.pousadamarealta.notification` |  |  |  |  |  |  |
| `com.pousadamarealta.repository` |  | sim |  |  |  |  |
| `com.pousadamarealta.service` |  |  |  |  |  |  |
| `com.pousadamarealta.util` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  279 src/main/java/com/pousadamarealta/repository/PostgresReservationRepository.java
  267 src/main/java/com/pousadamarealta/api/ApiServer.java
  190 src/main/java/com/pousadamarealta/domain/Reservation.java
  154 src/main/java/com/pousadamarealta/service/ReservationService.java
   86 src/main/java/com/pousadamarealta/repository/Database.java
   80 src/main/java/com/pousadamarealta/service/ReportService.java
   70 src/main/java/com/pousadamarealta/repository/PostgresRoomRepository.java
   65 src/main/java/com/pousadamarealta/repository/PostgresMessageRepository.java
   56 src/main/java/com/pousadamarealta/Main.java
   53 src/main/java/com/pousadamarealta/notification/HousekeepingNotificationListener.java
```
