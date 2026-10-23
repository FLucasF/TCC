# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 5 pacotes, 8 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.mareaalta` | 1 | 46 |
| `com.mareaalta.api` | 4 | 350 |
| `com.mareaalta.domain` | 7 | 212 |
| `com.mareaalta.infrastructure` | 4 | 373 |
| `com.mareaalta.service` | 5 | 426 |

## As dependências entre os pacotes do projeto

```
com.mareaalta.api -> com.mareaalta.domain
com.mareaalta.api -> com.mareaalta.infrastructure
com.mareaalta.api -> com.mareaalta.service
com.mareaalta -> com.mareaalta.api
com.mareaalta -> com.mareaalta.infrastructure
com.mareaalta.infrastructure -> com.mareaalta.domain
com.mareaalta.service -> com.mareaalta.domain
com.mareaalta.service -> com.mareaalta.infrastructure
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.mareaalta` | 2 | 0 |
| `com.mareaalta.api` | 3 | 1 |
| `com.mareaalta.domain` | 0 | 3 |
| `com.mareaalta.infrastructure` | 1 | 3 |
| `com.mareaalta.service` | 2 | 1 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.mareaalta` | sim |  |  |  |  |  |
| `com.mareaalta.api` | sim |  |  |  |  | com.google |
| `com.mareaalta.domain` |  |  |  |  |  |  |
| `com.mareaalta.infrastructure` |  | sim |  |  |  |  |
| `com.mareaalta.service` |  | sim |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  243 src/main/java/com/mareaalta/api/ReservationController.java
  171 src/main/java/com/mareaalta/service/ReservationService.java
  167 src/main/java/com/mareaalta/infrastructure/ReservationRepository.java
  111 src/main/java/com/mareaalta/service/DocumentGenerator.java
   96 src/main/java/com/mareaalta/domain/Reservation.java
   91 src/main/java/com/mareaalta/infrastructure/Database.java
   89 src/main/java/com/mareaalta/service/ReportService.java
   67 src/main/java/com/mareaalta/infrastructure/RoomRepository.java
   66 src/main/java/com/mareaalta/domain/DocumentResponse.java
   55 src/main/java/com/mareaalta/service/PricingService.java
```
