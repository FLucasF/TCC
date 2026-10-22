# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 7 pacotes, 16 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `br.com.marealta` | 1 | 26 |
| `br.com.marealta.domain` | 10 | 189 |
| `br.com.marealta.event` | 6 | 44 |
| `br.com.marealta.notify` | 3 | 84 |
| `br.com.marealta.service` | 3 | 201 |
| `br.com.marealta.store` | 7 | 386 |
| `br.com.marealta.web` | 1 | 203 |

## As dependências entre os pacotes do projeto

```
br.com.marealta -> br.com.marealta.event
br.com.marealta -> br.com.marealta.notify
br.com.marealta -> br.com.marealta.service
br.com.marealta -> br.com.marealta.store
br.com.marealta -> br.com.marealta.web
br.com.marealta.event -> br.com.marealta.domain
br.com.marealta.notify -> br.com.marealta.domain
br.com.marealta.notify -> br.com.marealta.event
br.com.marealta.service -> br.com.marealta.domain
br.com.marealta.service -> br.com.marealta.event
br.com.marealta.service -> br.com.marealta.store
br.com.marealta.store -> br.com.marealta.domain
br.com.marealta.store -> br.com.marealta.event
br.com.marealta.web -> br.com.marealta.domain
br.com.marealta.web -> br.com.marealta.event
br.com.marealta.web -> br.com.marealta.service
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `br.com.marealta` | 5 | 0 |
| `br.com.marealta.domain` | 0 | 5 |
| `br.com.marealta.event` | 1 | 5 |
| `br.com.marealta.notify` | 2 | 1 |
| `br.com.marealta.service` | 3 | 2 |
| `br.com.marealta.store` | 2 | 2 |
| `br.com.marealta.web` | 3 | 1 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `br.com.marealta` | sim |  |  |  |  |  |
| `br.com.marealta.domain` |  |  |  |  |  |  |
| `br.com.marealta.event` |  |  |  |  |  |  |
| `br.com.marealta.notify` |  |  |  |  |  |  |
| `br.com.marealta.service` |  |  |  |  |  |  |
| `br.com.marealta.store` |  | sim |  |  | sim |  |
| `br.com.marealta.web` | sim |  | sim |  |  |  |

## As maiores classes (linhas, src/main)

```
  287 src/main/java/br/com/marealta/store/JdbcStore.java
  203 src/main/java/br/com/marealta/web/Api.java
  142 src/main/java/br/com/marealta/service/ReservationService.java
   59 src/main/java/br/com/marealta/service/ReportService.java
   57 src/main/java/br/com/marealta/store/MemoryStore.java
   42 src/main/java/br/com/marealta/store/Store.java
   39 src/main/java/br/com/marealta/domain/Pricing.java
   36 src/main/java/br/com/marealta/domain/Money.java
   32 src/main/java/br/com/marealta/notify/HousekeepingNotifier.java
   31 src/main/java/br/com/marealta/domain/Reservation.java
```
