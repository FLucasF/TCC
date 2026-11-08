# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 6 pacotes, 12 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `br.com.pousadamarealta.reservas` | 1 | 42 |
| `br.com.pousadamarealta.reservas.domain` | 11 | 279 |
| `br.com.pousadamarealta.reservas.notification` | 10 | 156 |
| `br.com.pousadamarealta.reservas.persistence` | 5 | 363 |
| `br.com.pousadamarealta.reservas.service` | 3 | 213 |
| `br.com.pousadamarealta.reservas.web` | 3 | 201 |

## As dependências entre os pacotes do projeto

```
br.com.pousadamarealta.reservas -> br.com.pousadamarealta.reservas.notification
br.com.pousadamarealta.reservas -> br.com.pousadamarealta.reservas.persistence
br.com.pousadamarealta.reservas -> br.com.pousadamarealta.reservas.service
br.com.pousadamarealta.reservas -> br.com.pousadamarealta.reservas.web
br.com.pousadamarealta.reservas.notification -> br.com.pousadamarealta.reservas.domain
br.com.pousadamarealta.reservas.persistence -> br.com.pousadamarealta.reservas.domain
br.com.pousadamarealta.reservas.persistence -> br.com.pousadamarealta.reservas.notification
br.com.pousadamarealta.reservas.service -> br.com.pousadamarealta.reservas.domain
br.com.pousadamarealta.reservas.service -> br.com.pousadamarealta.reservas.notification
br.com.pousadamarealta.reservas.service -> br.com.pousadamarealta.reservas.persistence
br.com.pousadamarealta.reservas.web -> br.com.pousadamarealta.reservas.domain
br.com.pousadamarealta.reservas.web -> br.com.pousadamarealta.reservas.service
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `br.com.pousadamarealta.reservas` | 4 | 0 |
| `br.com.pousadamarealta.reservas.domain` | 0 | 4 |
| `br.com.pousadamarealta.reservas.notification` | 1 | 3 |
| `br.com.pousadamarealta.reservas.persistence` | 2 | 2 |
| `br.com.pousadamarealta.reservas.service` | 3 | 2 |
| `br.com.pousadamarealta.reservas.web` | 2 | 1 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `br.com.pousadamarealta.reservas` | sim |  |  |  |  |  |
| `br.com.pousadamarealta.reservas.domain` |  |  |  |  |  |  |
| `br.com.pousadamarealta.reservas.notification` |  |  |  |  |  |  |
| `br.com.pousadamarealta.reservas.persistence` |  | sim |  |  | sim |  |
| `br.com.pousadamarealta.reservas.service` |  | sim |  |  |  |  |
| `br.com.pousadamarealta.reservas.web` | sim |  | sim |  |  |  |

## As maiores classes (linhas, src/main)

```
  144 src/main/java/br/com/pousadamarealta/reservas/persistence/ReservationRepository.java
  129 src/main/java/br/com/pousadamarealta/reservas/service/ReservationService.java
  128 src/main/java/br/com/pousadamarealta/reservas/persistence/Database.java
  101 src/main/java/br/com/pousadamarealta/reservas/web/App.java
  100 src/main/java/br/com/pousadamarealta/reservas/web/RequestParser.java
   89 src/main/java/br/com/pousadamarealta/reservas/domain/Reservation.java
   68 src/main/java/br/com/pousadamarealta/reservas/service/ReportService.java
   53 src/main/java/br/com/pousadamarealta/reservas/persistence/RoomRepository.java
   49 src/main/java/br/com/pousadamarealta/reservas/domain/Receipt.java
   43 src/main/java/br/com/pousadamarealta/reservas/domain/Pricing.java
```
