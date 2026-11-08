# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 7 pacotes, 11 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `br.com.pousada` | 2 | 44 |
| `br.com.pousada.config` | 1 | 126 |
| `br.com.pousada.controller` | 6 | 356 |
| `br.com.pousada.model` | 7 | 206 |
| `br.com.pousada.repository` | 3 | 329 |
| `br.com.pousada.service` | 3 | 321 |
| `br.com.pousada.util` | 2 | 86 |

## As dependências entre os pacotes do projeto

```
br.com.pousada -> br.com.pousada.config
br.com.pousada -> br.com.pousada.controller
br.com.pousada.controller -> br.com.pousada.model
br.com.pousada.controller -> br.com.pousada.repository
br.com.pousada.controller -> br.com.pousada.service
br.com.pousada.controller -> br.com.pousada.util
br.com.pousada.repository -> br.com.pousada.config
br.com.pousada.repository -> br.com.pousada.model
br.com.pousada.service -> br.com.pousada.model
br.com.pousada.service -> br.com.pousada.repository
br.com.pousada.service -> br.com.pousada.util
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `br.com.pousada` | 2 | 0 |
| `br.com.pousada.config` | 0 | 2 |
| `br.com.pousada.controller` | 4 | 1 |
| `br.com.pousada.model` | 0 | 3 |
| `br.com.pousada.repository` | 2 | 2 |
| `br.com.pousada.service` | 3 | 1 |
| `br.com.pousada.util` | 0 | 2 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `br.com.pousada` | sim |  |  |  |  |  |
| `br.com.pousada.config` |  | sim |  |  | sim |  |
| `br.com.pousada.controller` | sim |  | sim |  |  |  |
| `br.com.pousada.model` |  |  | sim |  |  |  |
| `br.com.pousada.repository` |  | sim |  |  |  |  |
| `br.com.pousada.service` |  |  |  |  |  |  |
| `br.com.pousada.util` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  208 src/main/java/br/com/pousada/controller/ReservationController.java
  203 src/main/java/br/com/pousada/repository/ReservationRepository.java
  194 src/main/java/br/com/pousada/service/ReservationService.java
  126 src/main/java/br/com/pousada/config/DatabaseConfig.java
  117 src/main/java/br/com/pousada/controller/ReportController.java
  115 src/main/java/br/com/pousada/service/NotificationService.java
   78 src/main/java/br/com/pousada/model/Reservation.java
   69 src/main/java/br/com/pousada/repository/MessageRepository.java
   59 src/main/java/br/com/pousada/model/ReservationDocument.java
   57 src/main/java/br/com/pousada/repository/RoomRepository.java
```
