# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 7 pacotes, 16 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.pousada` | 1 | 46 |
| `com.pousada.domain` | 8 | 154 |
| `com.pousada.error` | 1 | 45 |
| `com.pousada.persistence` | 5 | 327 |
| `com.pousada.plan` | 9 | 237 |
| `com.pousada.service` | 4 | 271 |
| `com.pousada.web` | 2 | 150 |

## As dependências entre os pacotes do projeto

```
com.pousada -> com.pousada.persistence
com.pousada -> com.pousada.plan
com.pousada -> com.pousada.service
com.pousada -> com.pousada.web
com.pousada.domain -> com.pousada.error
com.pousada.persistence -> com.pousada.domain
com.pousada.plan -> com.pousada.domain
com.pousada.plan -> com.pousada.error
com.pousada.service -> com.pousada.domain
com.pousada.service -> com.pousada.error
com.pousada.service -> com.pousada.persistence
com.pousada.service -> com.pousada.plan
com.pousada.web -> com.pousada.domain
com.pousada.web -> com.pousada.error
com.pousada.web -> com.pousada.plan
com.pousada.web -> com.pousada.service
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.pousada` | 4 | 0 |
| `com.pousada.domain` | 1 | 4 |
| `com.pousada.error` | 0 | 4 |
| `com.pousada.persistence` | 1 | 2 |
| `com.pousada.plan` | 2 | 3 |
| `com.pousada.service` | 4 | 2 |
| `com.pousada.web` | 4 | 1 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.pousada` | sim |  | sim |  |  |  |
| `com.pousada.domain` |  |  | sim |  |  |  |
| `com.pousada.error` |  |  |  |  |  |  |
| `com.pousada.persistence` |  | sim |  |  |  |  |
| `com.pousada.plan` |  |  |  |  |  |  |
| `com.pousada.service` |  | sim |  |  |  |  |
| `com.pousada.web` | sim |  | sim |  |  |  |

## As maiores classes (linhas, src/main)

```
  143 src/main/java/com/pousada/service/ReservationService.java
  127 src/main/java/com/pousada/persistence/ReservationRepository.java
  112 src/main/java/com/pousada/persistence/Database.java
   78 src/main/java/com/pousada/web/CreateReservationParser.java
   72 src/main/java/com/pousada/web/Routes.java
   61 src/main/java/com/pousada/domain/Reservation.java
   60 src/main/java/com/pousada/service/ReportService.java
   53 src/main/java/com/pousada/service/ReceiptFormatter.java
   51 src/main/java/com/pousada/persistence/RoomRepository.java
   46 src/main/java/com/pousada/App.java
```
