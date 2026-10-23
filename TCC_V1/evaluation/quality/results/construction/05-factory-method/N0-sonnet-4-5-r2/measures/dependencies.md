# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 8 pacotes, 16 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `br.com.marealta` | 1 | 124 |
| `br.com.marealta.controller` | 4 | 281 |
| `br.com.marealta.database` | 1 | 61 |
| `br.com.marealta.dto` | 6 | 173 |
| `br.com.marealta.exception` | 7 | 88 |
| `br.com.marealta.model` | 10 | 309 |
| `br.com.marealta.repository` | 3 | 398 |
| `br.com.marealta.service` | 5 | 486 |

## As dependências entre os pacotes do projeto

```
br.com.marealta -> br.com.marealta.controller
br.com.marealta -> br.com.marealta.database
br.com.marealta -> br.com.marealta.dto
br.com.marealta -> br.com.marealta.exception
br.com.marealta -> br.com.marealta.repository
br.com.marealta -> br.com.marealta.service
br.com.marealta.controller -> br.com.marealta.dto
br.com.marealta.controller -> br.com.marealta.exception
br.com.marealta.controller -> br.com.marealta.model
br.com.marealta.controller -> br.com.marealta.repository
br.com.marealta.controller -> br.com.marealta.service
br.com.marealta.dto -> br.com.marealta.model
br.com.marealta.repository -> br.com.marealta.model
br.com.marealta.service -> br.com.marealta.exception
br.com.marealta.service -> br.com.marealta.model
br.com.marealta.service -> br.com.marealta.repository
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `br.com.marealta` | 6 | 0 |
| `br.com.marealta.controller` | 5 | 1 |
| `br.com.marealta.database` | 0 | 1 |
| `br.com.marealta.dto` | 1 | 2 |
| `br.com.marealta.exception` | 0 | 3 |
| `br.com.marealta.model` | 0 | 4 |
| `br.com.marealta.repository` | 1 | 3 |
| `br.com.marealta.service` | 3 | 2 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `br.com.marealta` | sim | sim | sim |  | sim |  |
| `br.com.marealta.controller` | sim |  |  |  |  |  |
| `br.com.marealta.database` |  | sim |  |  |  |  |
| `br.com.marealta.dto` |  |  |  |  |  |  |
| `br.com.marealta.exception` |  |  |  |  |  |  |
| `br.com.marealta.model` |  |  |  |  |  |  |
| `br.com.marealta.repository` |  | sim |  |  |  |  |
| `br.com.marealta.service` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  243 src/main/java/br/com/marealta/repository/ReservationRepository.java
  184 src/main/java/br/com/marealta/controller/ReservationController.java
  173 src/main/java/br/com/marealta/service/ReservationService.java
  156 src/main/java/br/com/marealta/model/Reservation.java
  139 src/main/java/br/com/marealta/service/DocumentService.java
  124 src/main/java/br/com/marealta/Application.java
   85 src/main/java/br/com/marealta/repository/RoomRepository.java
   76 src/main/java/br/com/marealta/service/ReportService.java
   70 src/main/java/br/com/marealta/repository/MessageRepository.java
   65 src/main/java/br/com/marealta/service/PricingService.java
```
