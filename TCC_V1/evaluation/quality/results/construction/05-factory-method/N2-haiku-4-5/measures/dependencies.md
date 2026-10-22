# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 8 pacotes, 16 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `br.com.pousadamareal` | 1 | 72 |
| `br.com.pousadamareal.api.dto` | 2 | 92 |
| `br.com.pousadamareal.api.handlers` | 4 | 332 |
| `br.com.pousadamareal.db` | 2 | 110 |
| `br.com.pousadamareal.domain` | 9 | 531 |
| `br.com.pousadamareal.repository` | 3 | 294 |
| `br.com.pousadamareal.service` | 5 | 455 |
| `br.com.pousadamareal.util` | 2 | 63 |

## As dependências entre os pacotes do projeto

```
br.com.pousadamareal.api.dto -> br.com.pousadamareal.domain
br.com.pousadamareal.api.handlers -> br.com.pousadamareal.api.dto
br.com.pousadamareal.api.handlers -> br.com.pousadamareal.domain
br.com.pousadamareal.api.handlers -> br.com.pousadamareal.repository
br.com.pousadamareal.api.handlers -> br.com.pousadamareal.service
br.com.pousadamareal.api.handlers -> br.com.pousadamareal.util
br.com.pousadamareal -> br.com.pousadamareal.api.handlers
br.com.pousadamareal -> br.com.pousadamareal.db
br.com.pousadamareal -> br.com.pousadamareal.repository
br.com.pousadamareal -> br.com.pousadamareal.service
br.com.pousadamareal.domain -> br.com.pousadamareal.util
br.com.pousadamareal.repository -> br.com.pousadamareal.db
br.com.pousadamareal.repository -> br.com.pousadamareal.domain
br.com.pousadamareal.service -> br.com.pousadamareal.domain
br.com.pousadamareal.service -> br.com.pousadamareal.repository
br.com.pousadamareal.service -> br.com.pousadamareal.util
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `br.com.pousadamareal` | 4 | 0 |
| `br.com.pousadamareal.api.dto` | 1 | 1 |
| `br.com.pousadamareal.api.handlers` | 5 | 1 |
| `br.com.pousadamareal.db` | 0 | 2 |
| `br.com.pousadamareal.domain` | 1 | 4 |
| `br.com.pousadamareal.repository` | 2 | 3 |
| `br.com.pousadamareal.service` | 3 | 2 |
| `br.com.pousadamareal.util` | 0 | 3 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `br.com.pousadamareal` | sim |  |  |  |  |  |
| `br.com.pousadamareal.api.dto` |  |  |  |  |  | com.google |
| `br.com.pousadamareal.api.handlers` | sim | sim |  |  |  | com.google |
| `br.com.pousadamareal.db` |  | sim |  |  | sim |  |
| `br.com.pousadamareal.domain` |  |  |  |  |  |  |
| `br.com.pousadamareal.repository` |  | sim |  |  |  |  |
| `br.com.pousadamareal.service` |  | sim |  |  |  |  |
| `br.com.pousadamareal.util` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  212 src/main/java/br/com/pousadamareal/service/ReservationService.java
  207 src/main/java/br/com/pousadamareal/api/handlers/ReservationHandler.java
  196 src/main/java/br/com/pousadamareal/domain/Reservation.java
  194 src/main/java/br/com/pousadamareal/repository/ReservationRepository.java
  167 src/main/java/br/com/pousadamareal/domain/Document.java
  111 src/main/java/br/com/pousadamareal/service/DocumentService.java
   74 src/main/java/br/com/pousadamareal/service/ReportService.java
   72 src/main/java/br/com/pousadamareal/Main.java
   70 src/main/java/br/com/pousadamareal/db/SchemaInitializer.java
   59 src/main/java/br/com/pousadamareal/domain/Message.java
```
