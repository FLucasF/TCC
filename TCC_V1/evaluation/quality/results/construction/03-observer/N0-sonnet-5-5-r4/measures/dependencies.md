# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 6 pacotes, 11 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `br.com.marealta.pousada` | 1 | 44 |
| `br.com.marealta.pousada.domain` | 12 | 255 |
| `br.com.marealta.pousada.events` | 7 | 121 |
| `br.com.marealta.pousada.persistence` | 5 | 390 |
| `br.com.marealta.pousada.service` | 4 | 183 |
| `br.com.marealta.pousada.web` | 1 | 159 |

## As dependências entre os pacotes do projeto

```
br.com.marealta.pousada -> br.com.marealta.pousada.events
br.com.marealta.pousada -> br.com.marealta.pousada.persistence
br.com.marealta.pousada -> br.com.marealta.pousada.service
br.com.marealta.pousada -> br.com.marealta.pousada.web
br.com.marealta.pousada.events -> br.com.marealta.pousada.domain
br.com.marealta.pousada.persistence -> br.com.marealta.pousada.domain
br.com.marealta.pousada.service -> br.com.marealta.pousada.domain
br.com.marealta.pousada.service -> br.com.marealta.pousada.events
br.com.marealta.pousada.service -> br.com.marealta.pousada.persistence
br.com.marealta.pousada.web -> br.com.marealta.pousada.domain
br.com.marealta.pousada.web -> br.com.marealta.pousada.service
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `br.com.marealta.pousada` | 4 | 0 |
| `br.com.marealta.pousada.domain` | 0 | 4 |
| `br.com.marealta.pousada.events` | 1 | 2 |
| `br.com.marealta.pousada.persistence` | 1 | 2 |
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
| `br.com.marealta.pousada.events` |  |  |  |  |  |  |
| `br.com.marealta.pousada.persistence` |  | sim |  |  | sim |  |
| `br.com.marealta.pousada.service` |  |  |  |  |  |  |
| `br.com.marealta.pousada.web` | sim |  | sim |  |  |  |

## As maiores classes (linhas, src/main)

```
  159 src/main/java/br/com/marealta/pousada/web/Api.java
  140 src/main/java/br/com/marealta/pousada/persistence/ReservationRepository.java
  130 src/main/java/br/com/marealta/pousada/service/ReservationService.java
   96 src/main/java/br/com/marealta/pousada/persistence/Database.java
   83 src/main/java/br/com/marealta/pousada/domain/Reservation.java
   74 src/main/java/br/com/marealta/pousada/persistence/RoomRepository.java
   53 src/main/java/br/com/marealta/pousada/service/ReportService.java
   46 src/main/java/br/com/marealta/pousada/persistence/MessageRepository.java
   44 src/main/java/br/com/marealta/pousada/Main.java
   42 src/main/java/br/com/marealta/pousada/domain/Receipt.java
```
