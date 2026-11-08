# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 7 pacotes, 13 dependências entre eles, com ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.pousada.marealta` | 1 | 55 |
| `com.pousada.marealta.api` | 11 | 459 |
| `com.pousada.marealta.domain` | 6 | 292 |
| `com.pousada.marealta.notification` | 5 | 182 |
| `com.pousada.marealta.persistence` | 2 | 119 |
| `com.pousada.marealta.repository` | 3 | 275 |
| `com.pousada.marealta.service` | 4 | 374 |

## As dependências entre os pacotes do projeto

```
com.pousada.marealta.api -> com.pousada.marealta.domain
com.pousada.marealta.api -> com.pousada.marealta.repository
com.pousada.marealta.api -> com.pousada.marealta.service
com.pousada.marealta -> com.pousada.marealta.api
com.pousada.marealta -> com.pousada.marealta.persistence
com.pousada.marealta.notification -> com.pousada.marealta.domain
com.pousada.marealta.notification -> com.pousada.marealta.repository
com.pousada.marealta.notification -> com.pousada.marealta.service
com.pousada.marealta.repository -> com.pousada.marealta.domain
com.pousada.marealta.repository -> com.pousada.marealta.persistence
com.pousada.marealta.service -> com.pousada.marealta.domain
com.pousada.marealta.service -> com.pousada.marealta.notification
com.pousada.marealta.service -> com.pousada.marealta.repository
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.pousada.marealta` | 2 | 0 |
| `com.pousada.marealta.api` | 3 | 1 |
| `com.pousada.marealta.domain` | 0 | 4 |
| `com.pousada.marealta.notification` | 3 | 1 |
| `com.pousada.marealta.persistence` | 0 | 2 |
| `com.pousada.marealta.repository` | 2 | 3 |
| `com.pousada.marealta.service` | 3 | 2 |

## Os ciclos

O tsort achou ciclos; cada bloco abaixo é um ciclo (os pacotes que o formam):
```
--- um ciclo:
com.pousada.marealta.service
com.pousada.marealta.notification
```

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.pousada.marealta` | sim |  |  |  |  |  |
| `com.pousada.marealta.api` | sim |  |  |  |  |  |
| `com.pousada.marealta.domain` |  |  |  |  |  |  |
| `com.pousada.marealta.notification` |  | sim |  |  |  |  |
| `com.pousada.marealta.persistence` |  | sim |  |  | sim |  |
| `com.pousada.marealta.repository` |  | sim |  |  |  |  |
| `com.pousada.marealta.service` |  | sim |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  212 src/main/java/com/pousada/marealta/api/ReservationController.java
  147 src/main/java/com/pousada/marealta/repository/ReservationRepository.java
  145 src/main/java/com/pousada/marealta/service/ReservationService.java
  120 src/main/java/com/pousada/marealta/domain/Reservation.java
  105 src/main/java/com/pousada/marealta/service/ReportService.java
   77 src/main/java/com/pousada/marealta/domain/Receipt.java
   69 src/main/java/com/pousada/marealta/repository/MessageRepository.java
   66 src/main/java/com/pousada/marealta/service/ReceiptFormatter.java
   66 src/main/java/com/pousada/marealta/persistence/Database.java
   64 src/main/java/com/pousada/marealta/api/ReportController.java
```
