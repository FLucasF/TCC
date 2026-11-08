# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 6 pacotes, 12 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.pousada.marealta` | 2 | 326 |
| `com.pousada.marealta.config` | 1 | 141 |
| `com.pousada.marealta.dto` | 2 | 78 |
| `com.pousada.marealta.model` | 7 | 314 |
| `com.pousada.marealta.repository` | 3 | 239 |
| `com.pousada.marealta.service` | 5 | 509 |

## As dependências entre os pacotes do projeto

```
com.pousada.marealta -> com.pousada.marealta.config
com.pousada.marealta -> com.pousada.marealta.dto
com.pousada.marealta -> com.pousada.marealta.model
com.pousada.marealta -> com.pousada.marealta.repository
com.pousada.marealta -> com.pousada.marealta.service
com.pousada.marealta.dto -> com.pousada.marealta.model
com.pousada.marealta.repository -> com.pousada.marealta.config
com.pousada.marealta.repository -> com.pousada.marealta.model
com.pousada.marealta.service -> com.pousada.marealta.config
com.pousada.marealta.service -> com.pousada.marealta.dto
com.pousada.marealta.service -> com.pousada.marealta.model
com.pousada.marealta.service -> com.pousada.marealta.repository
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.pousada.marealta` | 5 | 0 |
| `com.pousada.marealta.config` | 0 | 3 |
| `com.pousada.marealta.dto` | 1 | 2 |
| `com.pousada.marealta.model` | 0 | 4 |
| `com.pousada.marealta.repository` | 2 | 2 |
| `com.pousada.marealta.service` | 4 | 1 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.pousada.marealta` | sim | sim | sim |  |  |  |
| `com.pousada.marealta.config` |  | sim |  |  | sim |  |
| `com.pousada.marealta.dto` |  |  |  |  |  |  |
| `com.pousada.marealta.model` |  |  |  |  |  |  |
| `com.pousada.marealta.repository` |  | sim |  |  |  |  |
| `com.pousada.marealta.service` |  | sim |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  326 src/main/java/com/pousada/marealta/Application.java
  214 src/main/java/com/pousada/marealta/model/Reservation.java
  166 src/main/java/com/pousada/marealta/service/ReservationService.java
  141 src/main/java/com/pousada/marealta/config/DatabaseConfig.java
  133 src/main/java/com/pousada/marealta/service/DocumentService.java
  128 src/main/java/com/pousada/marealta/repository/ReservationRepository.java
  107 src/main/java/com/pousada/marealta/service/ReportService.java
   68 src/main/java/com/pousada/marealta/service/PricingService.java
   63 src/main/java/com/pousada/marealta/model/Message.java
   57 src/main/java/com/pousada/marealta/repository/MessageRepository.java
```
