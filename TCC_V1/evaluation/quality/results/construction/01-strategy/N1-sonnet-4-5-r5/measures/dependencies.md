# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 9 pacotes, 20 dependências entre eles, com ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.marealta.pousada` | 1 | 133 |
| `com.marealta.pousada.controller` | 4 | 197 |
| `com.marealta.pousada.domain.model` | 6 | 281 |
| `com.marealta.pousada.domain.rateplan` | 8 | 288 |
| `com.marealta.pousada.dto` | 6 | 147 |
| `com.marealta.pousada.exception` | 6 | 42 |
| `com.marealta.pousada.repository` | 3 | 442 |
| `com.marealta.pousada.service` | 4 | 326 |
| `com.marealta.pousada.util` | 2 | 54 |

## As dependências entre os pacotes do projeto

```
com.marealta.pousada -> com.marealta.pousada.controller
com.marealta.pousada -> com.marealta.pousada.dto
com.marealta.pousada -> com.marealta.pousada.exception
com.marealta.pousada -> com.marealta.pousada.repository
com.marealta.pousada -> com.marealta.pousada.service
com.marealta.pousada.controller -> com.marealta.pousada.domain.model
com.marealta.pousada.controller -> com.marealta.pousada.dto
com.marealta.pousada.controller -> com.marealta.pousada.exception
com.marealta.pousada.controller -> com.marealta.pousada.service
com.marealta.pousada.domain.model -> com.marealta.pousada.domain.rateplan
com.marealta.pousada.domain.model -> com.marealta.pousada.exception
com.marealta.pousada.domain.model -> com.marealta.pousada.util
com.marealta.pousada.domain.rateplan -> com.marealta.pousada.domain.model
com.marealta.pousada.domain.rateplan -> com.marealta.pousada.exception
com.marealta.pousada.domain.rateplan -> com.marealta.pousada.util
com.marealta.pousada.dto -> com.marealta.pousada.domain.model
com.marealta.pousada.repository -> com.marealta.pousada.domain.model
com.marealta.pousada.service -> com.marealta.pousada.domain.model
com.marealta.pousada.service -> com.marealta.pousada.exception
com.marealta.pousada.service -> com.marealta.pousada.repository
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.marealta.pousada` | 5 | 0 |
| `com.marealta.pousada.controller` | 4 | 1 |
| `com.marealta.pousada.domain.model` | 3 | 5 |
| `com.marealta.pousada.domain.rateplan` | 3 | 1 |
| `com.marealta.pousada.dto` | 1 | 2 |
| `com.marealta.pousada.exception` | 0 | 5 |
| `com.marealta.pousada.repository` | 1 | 2 |
| `com.marealta.pousada.service` | 3 | 2 |
| `com.marealta.pousada.util` | 0 | 2 |

## Os ciclos

O tsort achou ciclos; cada bloco abaixo é um ciclo (os pacotes que o formam):
```
--- um ciclo:
com.marealta.pousada.domain.model
com.marealta.pousada.domain.rateplan
```

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.marealta.pousada` | sim | sim | sim |  |  |  |
| `com.marealta.pousada.controller` | sim |  |  |  |  |  |
| `com.marealta.pousada.domain.model` |  |  |  |  |  |  |
| `com.marealta.pousada.domain.rateplan` |  |  |  |  |  |  |
| `com.marealta.pousada.dto` |  |  |  |  |  |  |
| `com.marealta.pousada.exception` |  |  |  |  |  |  |
| `com.marealta.pousada.repository` |  | sim |  |  |  |  |
| `com.marealta.pousada.service` |  | sim |  |  |  |  |
| `com.marealta.pousada.util` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  260 src/main/java/com/marealta/pousada/repository/ReservationRepository.java
  174 src/main/java/com/marealta/pousada/domain/model/Reservation.java
  133 src/main/java/com/marealta/pousada/service/ReservationService.java
  133 src/main/java/com/marealta/pousada/Main.java
  105 src/main/java/com/marealta/pousada/repository/RoomRepository.java
  101 src/main/java/com/marealta/pousada/controller/ReservationController.java
   86 src/main/java/com/marealta/pousada/service/MessageService.java
   77 src/main/java/com/marealta/pousada/repository/MessageRepository.java
   73 src/main/java/com/marealta/pousada/service/ReportService.java
   56 src/main/java/com/marealta/pousada/domain/rateplan/WeekendPackageRatePlan.java
```
