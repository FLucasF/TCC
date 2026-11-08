# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 7 pacotes, 16 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `br.com.pousadamarealta.reservas` | 2 | 74 |
| `br.com.pousadamarealta.reservas.domain` | 11 | 201 |
| `br.com.pousadamarealta.reservas.events` | 5 | 41 |
| `br.com.pousadamarealta.reservas.notification` | 7 | 162 |
| `br.com.pousadamarealta.reservas.persistence` | 6 | 407 |
| `br.com.pousadamarealta.reservas.service` | 3 | 201 |
| `br.com.pousadamarealta.reservas.web` | 2 | 198 |

## As dependências entre os pacotes do projeto

```
br.com.pousadamarealta.reservas -> br.com.pousadamarealta.reservas.events
br.com.pousadamarealta.reservas -> br.com.pousadamarealta.reservas.notification
br.com.pousadamarealta.reservas -> br.com.pousadamarealta.reservas.persistence
br.com.pousadamarealta.reservas -> br.com.pousadamarealta.reservas.service
br.com.pousadamarealta.reservas -> br.com.pousadamarealta.reservas.web
br.com.pousadamarealta.reservas.events -> br.com.pousadamarealta.reservas.domain
br.com.pousadamarealta.reservas.notification -> br.com.pousadamarealta.reservas.domain
br.com.pousadamarealta.reservas.notification -> br.com.pousadamarealta.reservas.events
br.com.pousadamarealta.reservas.persistence -> br.com.pousadamarealta.reservas.domain
br.com.pousadamarealta.reservas.persistence -> br.com.pousadamarealta.reservas.notification
br.com.pousadamarealta.reservas.service -> br.com.pousadamarealta.reservas.domain
br.com.pousadamarealta.reservas.service -> br.com.pousadamarealta.reservas.events
br.com.pousadamarealta.reservas.service -> br.com.pousadamarealta.reservas.persistence
br.com.pousadamarealta.reservas.web -> br.com.pousadamarealta.reservas.domain
br.com.pousadamarealta.reservas.web -> br.com.pousadamarealta.reservas.persistence
br.com.pousadamarealta.reservas.web -> br.com.pousadamarealta.reservas.service
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `br.com.pousadamarealta.reservas` | 5 | 0 |
| `br.com.pousadamarealta.reservas.domain` | 0 | 5 |
| `br.com.pousadamarealta.reservas.events` | 1 | 3 |
| `br.com.pousadamarealta.reservas.notification` | 2 | 2 |
| `br.com.pousadamarealta.reservas.persistence` | 2 | 3 |
| `br.com.pousadamarealta.reservas.service` | 3 | 2 |
| `br.com.pousadamarealta.reservas.web` | 3 | 1 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `br.com.pousadamarealta.reservas` | sim |  |  |  |  |  |
| `br.com.pousadamarealta.reservas.domain` |  |  |  |  |  |  |
| `br.com.pousadamarealta.reservas.events` |  |  |  |  |  |  |
| `br.com.pousadamarealta.reservas.notification` |  |  |  |  |  |  |
| `br.com.pousadamarealta.reservas.persistence` |  | sim |  |  | sim |  |
| `br.com.pousadamarealta.reservas.service` |  |  |  |  |  |  |
| `br.com.pousadamarealta.reservas.web` | sim |  | sim |  |  |  |

## As maiores classes (linhas, src/main)

```
  157 src/main/java/br/com/pousadamarealta/reservas/persistence/ReservationRepository.java
  119 src/main/java/br/com/pousadamarealta/reservas/service/ReservationService.java
  114 src/main/java/br/com/pousadamarealta/reservas/web/RequestParser.java
   84 src/main/java/br/com/pousadamarealta/reservas/web/Routes.java
   71 src/main/java/br/com/pousadamarealta/reservas/persistence/Database.java
   70 src/main/java/br/com/pousadamarealta/reservas/persistence/Schema.java
   67 src/main/java/br/com/pousadamarealta/reservas/service/ReportService.java
   64 src/main/java/br/com/pousadamarealta/reservas/domain/Reservation.java
   52 src/main/java/br/com/pousadamarealta/reservas/persistence/RoomRepository.java
   50 src/main/java/br/com/pousadamarealta/reservas/persistence/MessageRepository.java
```
