# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 5 pacotes, 9 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.pousadamarealta` | 2 | 87 |
| `com.pousadamarealta.api` | 4 | 322 |
| `com.pousadamarealta.domain` | 7 | 181 |
| `com.pousadamarealta.infrastructure` | 5 | 449 |
| `com.pousadamarealta.services` | 7 | 451 |

## As dependências entre os pacotes do projeto

```
com.pousadamarealta.api -> com.pousadamarealta.domain
com.pousadamarealta.api -> com.pousadamarealta.infrastructure
com.pousadamarealta.api -> com.pousadamarealta.services
com.pousadamarealta -> com.pousadamarealta.api
com.pousadamarealta -> com.pousadamarealta.infrastructure
com.pousadamarealta -> com.pousadamarealta.services
com.pousadamarealta.infrastructure -> com.pousadamarealta.domain
com.pousadamarealta.services -> com.pousadamarealta.domain
com.pousadamarealta.services -> com.pousadamarealta.infrastructure
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.pousadamarealta` | 3 | 0 |
| `com.pousadamarealta.api` | 3 | 1 |
| `com.pousadamarealta.domain` | 0 | 3 |
| `com.pousadamarealta.infrastructure` | 1 | 3 |
| `com.pousadamarealta.services` | 2 | 2 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.pousadamarealta` | sim |  | sim |  |  |  |
| `com.pousadamarealta.api` | sim |  |  |  |  |  |
| `com.pousadamarealta.domain` |  |  |  |  |  |  |
| `com.pousadamarealta.infrastructure` |  | sim |  |  | sim |  |
| `com.pousadamarealta.services` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  208 src/main/java/com/pousadamarealta/api/ReservationController.java
  198 src/main/java/com/pousadamarealta/infrastructure/ReservationRepository.java
  149 src/main/java/com/pousadamarealta/services/HostelNotificationListener.java
  139 src/main/java/com/pousadamarealta/services/ReservationService.java
  102 src/main/java/com/pousadamarealta/infrastructure/Database.java
   87 src/main/java/com/pousadamarealta/Main.java
   77 src/main/java/com/pousadamarealta/services/ReportService.java
   66 src/main/java/com/pousadamarealta/api/ReportController.java
   58 src/main/java/com/pousadamarealta/domain/Reservation.java
   55 src/main/java/com/pousadamarealta/services/ReservationEventPublisher.java
```
