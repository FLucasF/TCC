# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 7 pacotes, 15 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `br.com.marealta` | 1 | 41 |
| `br.com.marealta.domain` | 12 | 344 |
| `br.com.marealta.messaging` | 14 | 193 |
| `br.com.marealta.persistence` | 4 | 274 |
| `br.com.marealta.report` | 2 | 62 |
| `br.com.marealta.service` | 1 | 89 |
| `br.com.marealta.web` | 3 | 256 |

## As dependências entre os pacotes do projeto

```
br.com.marealta -> br.com.marealta.messaging
br.com.marealta -> br.com.marealta.persistence
br.com.marealta -> br.com.marealta.service
br.com.marealta -> br.com.marealta.web
br.com.marealta.persistence -> br.com.marealta.domain
br.com.marealta.persistence -> br.com.marealta.messaging
br.com.marealta.report -> br.com.marealta.domain
br.com.marealta.service -> br.com.marealta.domain
br.com.marealta.service -> br.com.marealta.messaging
br.com.marealta.service -> br.com.marealta.persistence
br.com.marealta.web -> br.com.marealta.domain
br.com.marealta.web -> br.com.marealta.messaging
br.com.marealta.web -> br.com.marealta.persistence
br.com.marealta.web -> br.com.marealta.report
br.com.marealta.web -> br.com.marealta.service
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `br.com.marealta` | 4 | 0 |
| `br.com.marealta.domain` | 0 | 4 |
| `br.com.marealta.messaging` | 0 | 4 |
| `br.com.marealta.persistence` | 2 | 3 |
| `br.com.marealta.report` | 1 | 1 |
| `br.com.marealta.service` | 3 | 2 |
| `br.com.marealta.web` | 5 | 1 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `br.com.marealta` | sim |  |  |  |  |  |
| `br.com.marealta.domain` |  |  |  |  |  |  |
| `br.com.marealta.messaging` |  |  |  |  |  |  |
| `br.com.marealta.persistence` |  | sim |  |  |  |  |
| `br.com.marealta.report` |  |  |  |  |  |  |
| `br.com.marealta.service` |  |  |  |  |  |  |
| `br.com.marealta.web` | sim |  | sim |  |  |  |

## As maiores classes (linhas, src/main)

```
  148 src/main/java/br/com/marealta/persistence/ReservationRepository.java
  115 src/main/java/br/com/marealta/web/WebApp.java
  103 src/main/java/br/com/marealta/domain/Reservation.java
   89 src/main/java/br/com/marealta/service/ReservationService.java
   72 src/main/java/br/com/marealta/web/Views.java
   69 src/main/java/br/com/marealta/web/ReservationRequest.java
   69 src/main/java/br/com/marealta/persistence/Database.java
   57 src/main/java/br/com/marealta/persistence/JdbcMessageStore.java
   41 src/main/java/br/com/marealta/App.java
   39 src/main/java/br/com/marealta/domain/Pricing.java
```
