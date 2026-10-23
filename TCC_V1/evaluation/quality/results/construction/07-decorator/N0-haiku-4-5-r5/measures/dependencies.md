# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 5 pacotes, 8 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `br.pousada.marealta` | 1 | 37 |
| `br.pousada.marealta.api` | 6 | 535 |
| `br.pousada.marealta.database` | 4 | 386 |
| `br.pousada.marealta.model` | 9 | 214 |
| `br.pousada.marealta.service` | 4 | 215 |

## As dependências entre os pacotes do projeto

```
br.pousada.marealta.api -> br.pousada.marealta.database
br.pousada.marealta.api -> br.pousada.marealta.model
br.pousada.marealta.api -> br.pousada.marealta.service
br.pousada.marealta -> br.pousada.marealta.api
br.pousada.marealta -> br.pousada.marealta.database
br.pousada.marealta.database -> br.pousada.marealta.model
br.pousada.marealta.service -> br.pousada.marealta.database
br.pousada.marealta.service -> br.pousada.marealta.model
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `br.pousada.marealta` | 2 | 0 |
| `br.pousada.marealta.api` | 3 | 1 |
| `br.pousada.marealta.database` | 1 | 3 |
| `br.pousada.marealta.model` | 0 | 3 |
| `br.pousada.marealta.service` | 2 | 1 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `br.pousada.marealta` | sim |  |  |  |  |  |
| `br.pousada.marealta.api` | sim | sim |  |  |  | com.google.gson |
| `br.pousada.marealta.database` |  | sim |  |  | sim |  |
| `br.pousada.marealta.model` |  |  |  |  |  | com.google.gson |
| `br.pousada.marealta.service` |  | sim |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  339 src/main/java/br/pousada/marealta/api/ReservationHandler.java
  181 src/main/java/br/pousada/marealta/database/ReservationRepository.java
  116 src/main/java/br/pousada/marealta/api/ReportHandler.java
  111 src/main/java/br/pousada/marealta/database/Database.java
   76 src/main/java/br/pousada/marealta/model/Reservation.java
   70 src/main/java/br/pousada/marealta/service/MessageService.java
   65 src/main/java/br/pousada/marealta/service/DocumentFormatter.java
   59 src/main/java/br/pousada/marealta/service/PricingService.java
   56 src/main/java/br/pousada/marealta/model/Document.java
   48 src/main/java/br/pousada/marealta/database/RoomRepository.java
```
