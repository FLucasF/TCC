# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 6 pacotes, 13 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.pousadamarealta` | 1 | 88 |
| `com.pousadamarealta.document` | 6 | 323 |
| `com.pousadamarealta.domain` | 10 | 266 |
| `com.pousadamarealta.repository` | 3 | 404 |
| `com.pousadamarealta.service` | 8 | 244 |
| `com.pousadamarealta.web` | 2 | 301 |

## As dependências entre os pacotes do projeto

```
com.pousadamarealta -> com.pousadamarealta.document
com.pousadamarealta -> com.pousadamarealta.repository
com.pousadamarealta -> com.pousadamarealta.service
com.pousadamarealta -> com.pousadamarealta.web
com.pousadamarealta.document -> com.pousadamarealta.domain
com.pousadamarealta.repository -> com.pousadamarealta.domain
com.pousadamarealta.service -> com.pousadamarealta.document
com.pousadamarealta.service -> com.pousadamarealta.domain
com.pousadamarealta.service -> com.pousadamarealta.repository
com.pousadamarealta.web -> com.pousadamarealta.document
com.pousadamarealta.web -> com.pousadamarealta.domain
com.pousadamarealta.web -> com.pousadamarealta.repository
com.pousadamarealta.web -> com.pousadamarealta.service
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.pousadamarealta` | 4 | 0 |
| `com.pousadamarealta.document` | 1 | 3 |
| `com.pousadamarealta.domain` | 0 | 4 |
| `com.pousadamarealta.repository` | 1 | 3 |
| `com.pousadamarealta.service` | 3 | 2 |
| `com.pousadamarealta.web` | 4 | 1 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.pousadamarealta` | sim | sim | sim |  | sim |  |
| `com.pousadamarealta.document` |  |  |  |  |  |  |
| `com.pousadamarealta.domain` |  |  |  |  |  |  |
| `com.pousadamarealta.repository` |  | sim |  |  |  |  |
| `com.pousadamarealta.service` |  |  |  |  |  |  |
| `com.pousadamarealta.web` | sim |  | sim |  |  |  |

## As maiores classes (linhas, src/main)

```
  301 src/main/java/com/pousadamarealta/web/ApiController.java
  237 src/main/java/com/pousadamarealta/repository/ReservationRepository.java
  155 src/main/java/com/pousadamarealta/domain/Reservation.java
  153 src/main/java/com/pousadamarealta/service/ReservationService.java
   94 src/main/java/com/pousadamarealta/repository/RoomRepository.java
   91 src/main/java/com/pousadamarealta/service/ReportService.java
   88 src/main/java/com/pousadamarealta/App.java
   73 src/main/java/com/pousadamarealta/repository/MessageRepository.java
   69 src/main/java/com/pousadamarealta/document/ForeignGuestDocumentGenerator.java
   66 src/main/java/com/pousadamarealta/document/CorporateGuestDocumentGenerator.java
```
