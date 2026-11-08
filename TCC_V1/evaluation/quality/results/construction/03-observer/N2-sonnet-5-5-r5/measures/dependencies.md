# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 6 pacotes, 13 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `br.com.marealta.pousada` | 1 | 59 |
| `br.com.marealta.pousada.domain` | 14 | 347 |
| `br.com.marealta.pousada.notification` | 4 | 111 |
| `br.com.marealta.pousada.persistence` | 5 | 339 |
| `br.com.marealta.pousada.service` | 2 | 138 |
| `br.com.marealta.pousada.web` | 6 | 186 |

## As dependências entre os pacotes do projeto

```
br.com.marealta.pousada -> br.com.marealta.pousada.notification
br.com.marealta.pousada -> br.com.marealta.pousada.persistence
br.com.marealta.pousada -> br.com.marealta.pousada.service
br.com.marealta.pousada -> br.com.marealta.pousada.web
br.com.marealta.pousada.notification -> br.com.marealta.pousada.domain
br.com.marealta.pousada.notification -> br.com.marealta.pousada.persistence
br.com.marealta.pousada.persistence -> br.com.marealta.pousada.domain
br.com.marealta.pousada.service -> br.com.marealta.pousada.domain
br.com.marealta.pousada.service -> br.com.marealta.pousada.notification
br.com.marealta.pousada.service -> br.com.marealta.pousada.persistence
br.com.marealta.pousada.web -> br.com.marealta.pousada.domain
br.com.marealta.pousada.web -> br.com.marealta.pousada.persistence
br.com.marealta.pousada.web -> br.com.marealta.pousada.service
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `br.com.marealta.pousada` | 4 | 0 |
| `br.com.marealta.pousada.domain` | 0 | 4 |
| `br.com.marealta.pousada.notification` | 2 | 2 |
| `br.com.marealta.pousada.persistence` | 1 | 4 |
| `br.com.marealta.pousada.service` | 3 | 2 |
| `br.com.marealta.pousada.web` | 3 | 1 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `br.com.marealta.pousada` | sim |  | sim |  |  |  |
| `br.com.marealta.pousada.domain` |  |  |  |  |  |  |
| `br.com.marealta.pousada.notification` |  |  |  |  |  |  |
| `br.com.marealta.pousada.persistence` |  | sim |  |  |  |  |
| `br.com.marealta.pousada.service` |  |  |  |  |  |  |
| `br.com.marealta.pousada.web` | sim |  | sim |  |  |  |

## As maiores classes (linhas, src/main)

```
  159 src/main/java/br/com/marealta/pousada/persistence/ReservationRepository.java
  143 src/main/java/br/com/marealta/pousada/web/Routes.java
  134 src/main/java/br/com/marealta/pousada/domain/Reservation.java
   92 src/main/java/br/com/marealta/pousada/service/ReservationService.java
   81 src/main/java/br/com/marealta/pousada/persistence/Database.java
   59 src/main/java/br/com/marealta/pousada/App.java
   50 src/main/java/br/com/marealta/pousada/persistence/RoomRepository.java
   49 src/main/java/br/com/marealta/pousada/persistence/MessageRepository.java
   46 src/main/java/br/com/marealta/pousada/service/ReportService.java
   43 src/main/java/br/com/marealta/pousada/web/ApiDtos.java
```
