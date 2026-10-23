# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 8 pacotes, 17 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `br.com.pousada.marealta` | 1 | 92 |
| `br.com.pousada.marealta.controller` | 4 | 350 |
| `br.com.pousada.marealta.dto` | 1 | 27 |
| `br.com.pousada.marealta.exception` | 1 | 44 |
| `br.com.pousada.marealta.model` | 11 | 479 |
| `br.com.pousada.marealta.repository` | 3 | 398 |
| `br.com.pousada.marealta.service` | 4 | 417 |
| `br.com.pousada.marealta.util` | 2 | 152 |

## As dependências entre os pacotes do projeto

```
br.com.pousada.marealta -> br.com.pousada.marealta.controller
br.com.pousada.marealta -> br.com.pousada.marealta.dto
br.com.pousada.marealta -> br.com.pousada.marealta.exception
br.com.pousada.marealta -> br.com.pousada.marealta.repository
br.com.pousada.marealta -> br.com.pousada.marealta.service
br.com.pousada.marealta -> br.com.pousada.marealta.util
br.com.pousada.marealta.controller -> br.com.pousada.marealta.exception
br.com.pousada.marealta.controller -> br.com.pousada.marealta.model
br.com.pousada.marealta.controller -> br.com.pousada.marealta.repository
br.com.pousada.marealta.controller -> br.com.pousada.marealta.service
br.com.pousada.marealta.repository -> br.com.pousada.marealta.model
br.com.pousada.marealta.repository -> br.com.pousada.marealta.util
br.com.pousada.marealta.service -> br.com.pousada.marealta.exception
br.com.pousada.marealta.service -> br.com.pousada.marealta.model
br.com.pousada.marealta.service -> br.com.pousada.marealta.repository
br.com.pousada.marealta.service -> br.com.pousada.marealta.util
br.com.pousada.marealta.util -> br.com.pousada.marealta.model
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `br.com.pousada.marealta` | 6 | 0 |
| `br.com.pousada.marealta.controller` | 4 | 1 |
| `br.com.pousada.marealta.dto` | 0 | 1 |
| `br.com.pousada.marealta.exception` | 0 | 3 |
| `br.com.pousada.marealta.model` | 0 | 4 |
| `br.com.pousada.marealta.repository` | 2 | 3 |
| `br.com.pousada.marealta.service` | 4 | 2 |
| `br.com.pousada.marealta.util` | 1 | 3 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `br.com.pousada.marealta` | sim |  | sim |  |  |  |
| `br.com.pousada.marealta.controller` | sim |  |  |  |  |  |
| `br.com.pousada.marealta.dto` |  |  |  |  |  |  |
| `br.com.pousada.marealta.exception` |  |  |  |  |  |  |
| `br.com.pousada.marealta.model` |  |  |  |  |  |  |
| `br.com.pousada.marealta.repository` |  | sim |  |  |  |  |
| `br.com.pousada.marealta.service` |  |  |  |  |  |  |
| `br.com.pousada.marealta.util` |  | sim |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  278 src/main/java/br/com/pousada/marealta/repository/ReservationRepository.java
  228 src/main/java/br/com/pousada/marealta/controller/ReservationController.java
  177 src/main/java/br/com/pousada/marealta/model/Reservation.java
  160 src/main/java/br/com/pousada/marealta/model/Document.java
  158 src/main/java/br/com/pousada/marealta/service/ReservationService.java
  139 src/main/java/br/com/pousada/marealta/service/DocumentService.java
   92 src/main/java/br/com/pousada/marealta/App.java
   84 src/main/java/br/com/pousada/marealta/util/DatabaseInitializer.java
   84 src/main/java/br/com/pousada/marealta/service/ReportService.java
   68 src/main/java/br/com/pousada/marealta/util/PriceCalculator.java
```
