# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 7 pacotes, 12 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `br.com.maralta` | 1 | 191 |
| `br.com.maralta.dto` | 4 | 337 |
| `br.com.maralta.exception` | 1 | 44 |
| `br.com.maralta.model` | 8 | 570 |
| `br.com.maralta.repository` | 3 | 391 |
| `br.com.maralta.service` | 5 | 436 |
| `br.com.maralta.util` | 3 | 130 |

## As dependências entre os pacotes do projeto

```
br.com.maralta -> br.com.maralta.dto
br.com.maralta -> br.com.maralta.exception
br.com.maralta -> br.com.maralta.model
br.com.maralta -> br.com.maralta.repository
br.com.maralta -> br.com.maralta.service
br.com.maralta -> br.com.maralta.util
br.com.maralta.dto -> br.com.maralta.model
br.com.maralta.repository -> br.com.maralta.model
br.com.maralta.service -> br.com.maralta.exception
br.com.maralta.service -> br.com.maralta.model
br.com.maralta.service -> br.com.maralta.repository
br.com.maralta.service -> br.com.maralta.util
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `br.com.maralta` | 6 | 0 |
| `br.com.maralta.dto` | 1 | 1 |
| `br.com.maralta.exception` | 0 | 2 |
| `br.com.maralta.model` | 0 | 4 |
| `br.com.maralta.repository` | 1 | 2 |
| `br.com.maralta.service` | 4 | 1 |
| `br.com.maralta.util` | 0 | 2 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `br.com.maralta` | sim | sim | sim |  |  |  |
| `br.com.maralta.dto` |  |  |  |  |  |  |
| `br.com.maralta.exception` |  |  |  |  |  |  |
| `br.com.maralta.model` |  |  |  |  |  |  |
| `br.com.maralta.repository` |  | sim |  |  |  |  |
| `br.com.maralta.service` |  |  |  |  |  |  |
| `br.com.maralta.util` |  | sim |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  252 src/main/java/br/com/maralta/repository/ReservationRepository.java
  191 src/main/java/br/com/maralta/Main.java
  182 src/main/java/br/com/maralta/model/Reservation.java
  168 src/main/java/br/com/maralta/service/ReservationService.java
  160 src/main/java/br/com/maralta/model/Document.java
  141 src/main/java/br/com/maralta/dto/ReservationResponse.java
  139 src/main/java/br/com/maralta/dto/CreateReservationRequest.java
  120 src/main/java/br/com/maralta/service/DocumentService.java
   91 src/main/java/br/com/maralta/model/GuestIdentification.java
   76 src/main/java/br/com/maralta/util/DatabaseInitializer.java
```
