# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 7 pacotes, 14 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `br.com.pousada` | 1 | 116 |
| `br.com.pousada.api` | 6 | 340 |
| `br.com.pousada.domain` | 11 | 336 |
| `br.com.pousada.exception` | 6 | 42 |
| `br.com.pousada.repository` | 3 | 507 |
| `br.com.pousada.service` | 4 | 479 |
| `br.com.pousada.util` | 2 | 76 |

## As dependências entre os pacotes do projeto

```
br.com.pousada.api -> br.com.pousada.domain
br.com.pousada.api -> br.com.pousada.exception
br.com.pousada.api -> br.com.pousada.repository
br.com.pousada.api -> br.com.pousada.service
br.com.pousada -> br.com.pousada.api
br.com.pousada -> br.com.pousada.exception
br.com.pousada -> br.com.pousada.repository
br.com.pousada -> br.com.pousada.service
br.com.pousada.repository -> br.com.pousada.domain
br.com.pousada.service -> br.com.pousada.domain
br.com.pousada.service -> br.com.pousada.exception
br.com.pousada.service -> br.com.pousada.repository
br.com.pousada.service -> br.com.pousada.util
br.com.pousada.util -> br.com.pousada.domain
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `br.com.pousada` | 4 | 0 |
| `br.com.pousada.api` | 4 | 1 |
| `br.com.pousada.domain` | 0 | 4 |
| `br.com.pousada.exception` | 0 | 3 |
| `br.com.pousada.repository` | 1 | 3 |
| `br.com.pousada.service` | 4 | 2 |
| `br.com.pousada.util` | 1 | 1 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `br.com.pousada` | sim | sim | sim |  | sim |  |
| `br.com.pousada.api` | sim |  | sim |  |  |  |
| `br.com.pousada.domain` |  |  |  |  |  |  |
| `br.com.pousada.exception` |  |  |  |  |  |  |
| `br.com.pousada.repository` |  | sim |  |  |  |  |
| `br.com.pousada.service` |  |  |  |  |  |  |
| `br.com.pousada.util` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  311 src/main/java/br/com/pousada/repository/ReservationRepository.java
  230 src/main/java/br/com/pousada/service/DocumentService.java
  185 src/main/java/br/com/pousada/api/ReservationController.java
  172 src/main/java/br/com/pousada/domain/Reservation.java
  138 src/main/java/br/com/pousada/service/ReservationService.java
  116 src/main/java/br/com/pousada/Application.java
  113 src/main/java/br/com/pousada/repository/RoomRepository.java
   83 src/main/java/br/com/pousada/repository/MessageRepository.java
   78 src/main/java/br/com/pousada/service/ReportService.java
   63 src/main/java/br/com/pousada/api/ReportController.java
```
