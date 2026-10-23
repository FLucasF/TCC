# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 5 pacotes, 9 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.pousada.marealta` | 1 | 65 |
| `com.pousada.marealta.api` | 4 | 530 |
| `com.pousada.marealta.domain` | 8 | 386 |
| `com.pousada.marealta.infrastructure` | 4 | 370 |
| `com.pousada.marealta.service` | 25 | 643 |

## As dependências entre os pacotes do projeto

```
com.pousada.marealta.api -> com.pousada.marealta.domain
com.pousada.marealta.api -> com.pousada.marealta.infrastructure
com.pousada.marealta.api -> com.pousada.marealta.service
com.pousada.marealta -> com.pousada.marealta.api
com.pousada.marealta -> com.pousada.marealta.infrastructure
com.pousada.marealta -> com.pousada.marealta.service
com.pousada.marealta.infrastructure -> com.pousada.marealta.domain
com.pousada.marealta.service -> com.pousada.marealta.domain
com.pousada.marealta.service -> com.pousada.marealta.infrastructure
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.pousada.marealta` | 3 | 0 |
| `com.pousada.marealta.api` | 3 | 1 |
| `com.pousada.marealta.domain` | 0 | 3 |
| `com.pousada.marealta.infrastructure` | 1 | 3 |
| `com.pousada.marealta.service` | 2 | 2 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.pousada.marealta` | sim |  |  |  |  |  |
| `com.pousada.marealta.api` | sim |  |  |  |  | com.google |
| `com.pousada.marealta.domain` |  |  |  |  |  |  |
| `com.pousada.marealta.infrastructure` |  | sim |  |  |  |  |
| `com.pousada.marealta.service` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  410 src/main/java/com/pousada/marealta/api/ReservationController.java
  168 src/main/java/com/pousada/marealta/infrastructure/ReservationRepository.java
  153 src/main/java/com/pousada/marealta/domain/Reservation.java
   84 src/main/java/com/pousada/marealta/domain/Document.java
   82 src/main/java/com/pousada/marealta/infrastructure/Database.java
   70 src/main/java/com/pousada/marealta/service/PriceCalculator.java
   68 src/main/java/com/pousada/marealta/infrastructure/MessageRepository.java
   68 src/main/java/com/pousada/marealta/domain/Message.java
   65 src/main/java/com/pousada/marealta/Application.java
   56 src/main/java/com/pousada/marealta/service/EmailValidator.java
```
