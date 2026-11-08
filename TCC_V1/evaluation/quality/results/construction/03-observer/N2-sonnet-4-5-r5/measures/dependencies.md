# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 9 pacotes, 20 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `br.com.pousadamarealta` | 1 | 90 |
| `br.com.pousadamarealta.api` | 2 | 252 |
| `br.com.pousadamarealta.api.dto` | 5 | 127 |
| `br.com.pousadamarealta.domain` | 9 | 300 |
| `br.com.pousadamarealta.domain.events` | 3 | 19 |
| `br.com.pousadamarealta.listeners` | 3 | 203 |
| `br.com.pousadamarealta.repository` | 3 | 36 |
| `br.com.pousadamarealta.repository.postgres` | 4 | 505 |
| `br.com.pousadamarealta.service` | 5 | 193 |

## As dependências entre os pacotes do projeto

```
br.com.pousadamarealta.api -> br.com.pousadamarealta.api.dto
br.com.pousadamarealta.api -> br.com.pousadamarealta.domain
br.com.pousadamarealta.api -> br.com.pousadamarealta.repository
br.com.pousadamarealta.api -> br.com.pousadamarealta.service
br.com.pousadamarealta.api.dto -> br.com.pousadamarealta.domain
br.com.pousadamarealta -> br.com.pousadamarealta.api
br.com.pousadamarealta -> br.com.pousadamarealta.listeners
br.com.pousadamarealta -> br.com.pousadamarealta.repository
br.com.pousadamarealta -> br.com.pousadamarealta.repository.postgres
br.com.pousadamarealta -> br.com.pousadamarealta.service
br.com.pousadamarealta.domain.events -> br.com.pousadamarealta.domain
br.com.pousadamarealta.listeners -> br.com.pousadamarealta.domain
br.com.pousadamarealta.listeners -> br.com.pousadamarealta.domain.events
br.com.pousadamarealta.listeners -> br.com.pousadamarealta.repository
br.com.pousadamarealta.repository -> br.com.pousadamarealta.domain
br.com.pousadamarealta.repository.postgres -> br.com.pousadamarealta.domain
br.com.pousadamarealta.repository.postgres -> br.com.pousadamarealta.repository
br.com.pousadamarealta.service -> br.com.pousadamarealta.domain
br.com.pousadamarealta.service -> br.com.pousadamarealta.domain.events
br.com.pousadamarealta.service -> br.com.pousadamarealta.repository
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `br.com.pousadamarealta` | 5 | 0 |
| `br.com.pousadamarealta.api` | 4 | 1 |
| `br.com.pousadamarealta.api.dto` | 1 | 1 |
| `br.com.pousadamarealta.domain` | 0 | 7 |
| `br.com.pousadamarealta.domain.events` | 1 | 2 |
| `br.com.pousadamarealta.listeners` | 3 | 1 |
| `br.com.pousadamarealta.repository` | 1 | 5 |
| `br.com.pousadamarealta.repository.postgres` | 2 | 1 |
| `br.com.pousadamarealta.service` | 3 | 2 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `br.com.pousadamarealta` | sim | sim | sim |  |  |  |
| `br.com.pousadamarealta.api` | sim |  |  |  |  |  |
| `br.com.pousadamarealta.api.dto` |  |  |  |  |  |  |
| `br.com.pousadamarealta.domain` |  |  |  |  |  |  |
| `br.com.pousadamarealta.domain.events` |  |  |  |  |  |  |
| `br.com.pousadamarealta.listeners` |  |  |  |  |  |  |
| `br.com.pousadamarealta.repository` |  |  |  |  |  |  |
| `br.com.pousadamarealta.repository.postgres` |  | sim |  |  |  |  |
| `br.com.pousadamarealta.service` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  275 src/main/java/br/com/pousadamarealta/repository/postgres/PostgresReservationRepository.java
  248 src/main/java/br/com/pousadamarealta/api/ApiHandler.java
  145 src/main/java/br/com/pousadamarealta/domain/Reservation.java
   95 src/main/java/br/com/pousadamarealta/service/ReservationService.java
   90 src/main/java/br/com/pousadamarealta/Main.java
   89 src/main/java/br/com/pousadamarealta/repository/postgres/Database.java
   77 src/main/java/br/com/pousadamarealta/service/ReportService.java
   74 src/main/java/br/com/pousadamarealta/repository/postgres/PostgresMessageRepository.java
   70 src/main/java/br/com/pousadamarealta/listeners/GuestNotifier.java
   69 src/main/java/br/com/pousadamarealta/listeners/FinanceNotifier.java
```
