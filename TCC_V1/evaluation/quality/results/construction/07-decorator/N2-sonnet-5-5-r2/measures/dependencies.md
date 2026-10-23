# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 7 pacotes, 16 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `br.com.marealta` | 1 | 46 |
| `br.com.marealta.domain` | 13 | 316 |
| `br.com.marealta.messaging` | 13 | 156 |
| `br.com.marealta.persistence` | 3 | 279 |
| `br.com.marealta.report` | 3 | 81 |
| `br.com.marealta.service` | 2 | 70 |
| `br.com.marealta.web` | 1 | 190 |

## As dependências entre os pacotes do projeto

```
br.com.marealta -> br.com.marealta.messaging
br.com.marealta -> br.com.marealta.persistence
br.com.marealta -> br.com.marealta.service
br.com.marealta -> br.com.marealta.web
br.com.marealta.persistence -> br.com.marealta.domain
br.com.marealta.persistence -> br.com.marealta.messaging
br.com.marealta.report -> br.com.marealta.domain
br.com.marealta.report -> br.com.marealta.persistence
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
| `br.com.marealta.persistence` | 2 | 4 |
| `br.com.marealta.report` | 2 | 1 |
| `br.com.marealta.service` | 3 | 2 |
| `br.com.marealta.web` | 5 | 1 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `br.com.marealta` | sim | sim |  |  |  |  |
| `br.com.marealta.domain` |  |  | sim |  |  |  |
| `br.com.marealta.messaging` |  |  |  |  |  |  |
| `br.com.marealta.persistence` |  | sim |  |  |  |  |
| `br.com.marealta.report` |  |  |  |  |  |  |
| `br.com.marealta.service` |  |  |  |  |  |  |
| `br.com.marealta.web` | sim |  | sim |  |  |  |

## As maiores classes (linhas, src/main)

```
  190 src/main/java/br/com/marealta/web/Routes.java
  161 src/main/java/br/com/marealta/persistence/ReservationRepository.java
   98 src/main/java/br/com/marealta/domain/Reservation.java
   70 src/main/java/br/com/marealta/service/ReservationService.java
   68 src/main/java/br/com/marealta/persistence/Database.java
   50 src/main/java/br/com/marealta/persistence/PostgresMessageLog.java
   46 src/main/java/br/com/marealta/Main.java
   39 src/main/java/br/com/marealta/domain/ApiException.java
   36 src/main/java/br/com/marealta/domain/Pricing.java
   35 src/main/java/br/com/marealta/domain/Receipt.java
```
