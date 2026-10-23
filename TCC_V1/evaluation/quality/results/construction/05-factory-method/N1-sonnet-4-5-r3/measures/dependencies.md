# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 6 pacotes, 11 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.pousadamareal` | 1 | 65 |
| `com.pousadamareal.api` | 6 | 269 |
| `com.pousadamareal.controller` | 4 | 258 |
| `com.pousadamareal.model` | 11 | 594 |
| `com.pousadamareal.repository` | 4 | 449 |
| `com.pousadamareal.service` | 10 | 348 |

## As dependências entre os pacotes do projeto

```
com.pousadamareal.api -> com.pousadamareal.model
com.pousadamareal -> com.pousadamareal.controller
com.pousadamareal -> com.pousadamareal.repository
com.pousadamareal -> com.pousadamareal.service
com.pousadamareal.controller -> com.pousadamareal.api
com.pousadamareal.controller -> com.pousadamareal.model
com.pousadamareal.controller -> com.pousadamareal.repository
com.pousadamareal.controller -> com.pousadamareal.service
com.pousadamareal.repository -> com.pousadamareal.model
com.pousadamareal.service -> com.pousadamareal.model
com.pousadamareal.service -> com.pousadamareal.repository
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.pousadamareal` | 3 | 0 |
| `com.pousadamareal.api` | 1 | 1 |
| `com.pousadamareal.controller` | 4 | 1 |
| `com.pousadamareal.model` | 0 | 4 |
| `com.pousadamareal.repository` | 1 | 3 |
| `com.pousadamareal.service` | 2 | 2 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.pousadamareal` | sim |  | sim |  |  |  |
| `com.pousadamareal.api` |  |  | sim |  |  |  |
| `com.pousadamareal.controller` | sim |  |  |  |  |  |
| `com.pousadamareal.model` |  |  |  |  |  |  |
| `com.pousadamareal.repository` |  | sim |  |  |  |  |
| `com.pousadamareal.service` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  239 src/main/java/com/pousadamareal/repository/ReservationRepository.java
  176 src/main/java/com/pousadamareal/service/ReservationService.java
  162 src/main/java/com/pousadamareal/controller/ReservationController.java
  129 src/main/java/com/pousadamareal/model/Reservation.java
   92 src/main/java/com/pousadamareal/repository/Database.java
   92 src/main/java/com/pousadamareal/model/PassportIdentification.java
   92 src/main/java/com/pousadamareal/model/CnpjIdentification.java
   84 src/main/java/com/pousadamareal/model/CpfIdentification.java
   79 src/main/java/com/pousadamareal/service/ReportService.java
   78 src/main/java/com/pousadamareal/api/ReservationResponse.java
```
