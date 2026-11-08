# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 8 pacotes, 15 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.pousada` | 1 | 22 |
| `com.pousada.api` | 6 | 475 |
| `com.pousada.api.dto` | 5 | 344 |
| `com.pousada.config` | 1 | 84 |
| `com.pousada.domain` | 8 | 330 |
| `com.pousada.report` | 5 | 295 |
| `com.pousada.repository` | 3 | 378 |
| `com.pousada.service` | 11 | 384 |

## As dependências entre os pacotes do projeto

```
com.pousada.api -> com.pousada.api.dto
com.pousada.api -> com.pousada.config
com.pousada.api -> com.pousada.domain
com.pousada.api -> com.pousada.report
com.pousada.api -> com.pousada.repository
com.pousada.api -> com.pousada.service
com.pousada.api.dto -> com.pousada.domain
com.pousada -> com.pousada.api
com.pousada -> com.pousada.config
com.pousada.report -> com.pousada.domain
com.pousada.report -> com.pousada.repository
com.pousada.repository -> com.pousada.config
com.pousada.repository -> com.pousada.domain
com.pousada.service -> com.pousada.domain
com.pousada.service -> com.pousada.repository
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.pousada` | 2 | 0 |
| `com.pousada.api` | 6 | 1 |
| `com.pousada.api.dto` | 1 | 1 |
| `com.pousada.config` | 0 | 3 |
| `com.pousada.domain` | 0 | 5 |
| `com.pousada.report` | 2 | 1 |
| `com.pousada.repository` | 2 | 3 |
| `com.pousada.service` | 2 | 1 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.pousada` |  |  |  |  |  |  |
| `com.pousada.api` | sim |  | sim |  |  |  |
| `com.pousada.api.dto` |  |  | sim |  |  |  |
| `com.pousada.config` |  | sim |  |  |  |  |
| `com.pousada.domain` |  |  |  |  |  |  |
| `com.pousada.report` |  |  |  |  |  |  |
| `com.pousada.repository` |  | sim |  |  |  |  |
| `com.pousada.service` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  257 src/main/java/com/pousada/repository/ReservationRepository.java
  196 src/main/java/com/pousada/api/ReservationController.java
  172 src/main/java/com/pousada/service/ReservationService.java
  131 src/main/java/com/pousada/domain/Reservation.java
  121 src/main/java/com/pousada/api/ReportController.java
  116 src/main/java/com/pousada/api/dto/ReservationResponse.java
   91 src/main/java/com/pousada/api/dto/DocumentResponse.java
   84 src/main/java/com/pousada/domain/Document.java
   84 src/main/java/com/pousada/config/Database.java
   81 src/main/java/com/pousada/api/ApiServer.java
```
