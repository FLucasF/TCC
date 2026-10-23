# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 7 pacotes, 14 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.pousadamarealta` | 1 | 76 |
| `com.pousadamarealta.api` | 5 | 422 |
| `com.pousadamarealta.database` | 2 | 88 |
| `com.pousadamarealta.document` | 5 | 248 |
| `com.pousadamarealta.domain` | 8 | 588 |
| `com.pousadamarealta.repository` | 3 | 369 |
| `com.pousadamarealta.service` | 4 | 270 |

## As dependências entre os pacotes do projeto

```
com.pousadamarealta.api -> com.pousadamarealta.domain
com.pousadamarealta.api -> com.pousadamarealta.repository
com.pousadamarealta.api -> com.pousadamarealta.service
com.pousadamarealta -> com.pousadamarealta.api
com.pousadamarealta -> com.pousadamarealta.database
com.pousadamarealta -> com.pousadamarealta.document
com.pousadamarealta -> com.pousadamarealta.repository
com.pousadamarealta -> com.pousadamarealta.service
com.pousadamarealta.document -> com.pousadamarealta.domain
com.pousadamarealta.repository -> com.pousadamarealta.database
com.pousadamarealta.repository -> com.pousadamarealta.domain
com.pousadamarealta.service -> com.pousadamarealta.document
com.pousadamarealta.service -> com.pousadamarealta.domain
com.pousadamarealta.service -> com.pousadamarealta.repository
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.pousadamarealta` | 5 | 0 |
| `com.pousadamarealta.api` | 3 | 1 |
| `com.pousadamarealta.database` | 0 | 2 |
| `com.pousadamarealta.document` | 1 | 2 |
| `com.pousadamarealta.domain` | 0 | 4 |
| `com.pousadamarealta.repository` | 2 | 3 |
| `com.pousadamarealta.service` | 3 | 2 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.pousadamarealta` | sim | sim | sim |  |  |  |
| `com.pousadamarealta.api` | sim | sim | sim |  |  |  |
| `com.pousadamarealta.database` |  | sim |  |  |  |  |
| `com.pousadamarealta.document` |  |  |  |  |  |  |
| `com.pousadamarealta.domain` |  |  |  |  |  |  |
| `com.pousadamarealta.repository` |  | sim |  |  |  |  |
| `com.pousadamarealta.service` |  | sim |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  283 src/main/java/com/pousadamarealta/domain/Reservation.java
  275 src/main/java/com/pousadamarealta/api/ReservationController.java
  237 src/main/java/com/pousadamarealta/repository/ReservationRepository.java
  160 src/main/java/com/pousadamarealta/domain/Document.java
  124 src/main/java/com/pousadamarealta/service/ReservationService.java
   82 src/main/java/com/pousadamarealta/service/ReportService.java
   76 src/main/java/com/pousadamarealta/document/CorporateDocumentGenerator.java
   76 src/main/java/com/pousadamarealta/App.java
   71 src/main/java/com/pousadamarealta/repository/MessageRepository.java
   71 src/main/java/com/pousadamarealta/document/InternationalDocumentGenerator.java
```
