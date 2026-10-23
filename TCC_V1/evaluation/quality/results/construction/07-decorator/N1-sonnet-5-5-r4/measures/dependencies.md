# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 6 pacotes, 12 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `br.com.marealta.pousada` | 2 | 43 |
| `br.com.marealta.pousada.domain` | 13 | 388 |
| `br.com.marealta.pousada.messaging` | 12 | 227 |
| `br.com.marealta.pousada.persistence` | 4 | 333 |
| `br.com.marealta.pousada.service` | 3 | 138 |
| `br.com.marealta.pousada.web` | 6 | 215 |

## As dependências entre os pacotes do projeto

```
br.com.marealta.pousada -> br.com.marealta.pousada.messaging
br.com.marealta.pousada -> br.com.marealta.pousada.persistence
br.com.marealta.pousada -> br.com.marealta.pousada.service
br.com.marealta.pousada -> br.com.marealta.pousada.web
br.com.marealta.pousada.messaging -> br.com.marealta.pousada.domain
br.com.marealta.pousada.persistence -> br.com.marealta.pousada.domain
br.com.marealta.pousada.persistence -> br.com.marealta.pousada.messaging
br.com.marealta.pousada.service -> br.com.marealta.pousada.domain
br.com.marealta.pousada.service -> br.com.marealta.pousada.messaging
br.com.marealta.pousada.service -> br.com.marealta.pousada.persistence
br.com.marealta.pousada.web -> br.com.marealta.pousada.domain
br.com.marealta.pousada.web -> br.com.marealta.pousada.service
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `br.com.marealta.pousada` | 4 | 0 |
| `br.com.marealta.pousada.domain` | 0 | 4 |
| `br.com.marealta.pousada.messaging` | 1 | 3 |
| `br.com.marealta.pousada.persistence` | 2 | 2 |
| `br.com.marealta.pousada.service` | 3 | 2 |
| `br.com.marealta.pousada.web` | 2 | 1 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `br.com.marealta.pousada` | sim |  |  |  |  |  |
| `br.com.marealta.pousada.domain` |  |  |  |  |  |  |
| `br.com.marealta.pousada.messaging` |  |  |  |  |  |  |
| `br.com.marealta.pousada.persistence` |  | sim |  |  | sim |  |
| `br.com.marealta.pousada.service` |  |  |  |  |  |  |
| `br.com.marealta.pousada.web` | sim |  | sim |  |  |  |

## As maiores classes (linhas, src/main)

```
  155 src/main/java/br/com/marealta/pousada/persistence/ReservationRepository.java
  110 src/main/java/br/com/marealta/pousada/domain/Reservation.java
  105 src/main/java/br/com/marealta/pousada/web/App.java
   93 src/main/java/br/com/marealta/pousada/persistence/Database.java
   83 src/main/java/br/com/marealta/pousada/service/ReservationService.java
   75 src/main/java/br/com/marealta/pousada/web/ReservationRequestParser.java
   63 src/main/java/br/com/marealta/pousada/persistence/MessageRepository.java
   49 src/main/java/br/com/marealta/pousada/messaging/MessageRules.java
   45 src/main/java/br/com/marealta/pousada/domain/Receipt.java
   41 src/main/java/br/com/marealta/pousada/service/ReportService.java
```
