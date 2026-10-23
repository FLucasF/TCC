# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 10 pacotes, 19 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.pousada.mare_alta` | 1 | 50 |
| `com.pousada.mare_alta.database` | 1 | 95 |
| `com.pousada.mare_alta.document` | 6 | 278 |
| `com.pousada.mare_alta.http` | 1 | 251 |
| `com.pousada.mare_alta.http.request` | 1 | 41 |
| `com.pousada.mare_alta.http.response` | 5 | 155 |
| `com.pousada.mare_alta.model` | 7 | 418 |
| `com.pousada.mare_alta.repository` | 3 | 312 |
| `com.pousada.mare_alta.service` | 2 | 345 |
| `com.pousada.mare_alta.util` | 2 | 79 |

## As dependências entre os pacotes do projeto

```
com.pousada.mare_alta -> com.pousada.mare_alta.database
com.pousada.mare_alta -> com.pousada.mare_alta.http
com.pousada.mare_alta -> com.pousada.mare_alta.repository
com.pousada.mare_alta -> com.pousada.mare_alta.service
com.pousada.mare_alta.document -> com.pousada.mare_alta.model
com.pousada.mare_alta.document -> com.pousada.mare_alta.util
com.pousada.mare_alta.http -> com.pousada.mare_alta.http.request
com.pousada.mare_alta.http -> com.pousada.mare_alta.http.response
com.pousada.mare_alta.http -> com.pousada.mare_alta.model
com.pousada.mare_alta.http -> com.pousada.mare_alta.repository
com.pousada.mare_alta.http -> com.pousada.mare_alta.service
com.pousada.mare_alta.http.response -> com.pousada.mare_alta.model
com.pousada.mare_alta.http.response -> com.pousada.mare_alta.util
com.pousada.mare_alta.repository -> com.pousada.mare_alta.database
com.pousada.mare_alta.repository -> com.pousada.mare_alta.model
com.pousada.mare_alta.service -> com.pousada.mare_alta.document
com.pousada.mare_alta.service -> com.pousada.mare_alta.model
com.pousada.mare_alta.service -> com.pousada.mare_alta.repository
com.pousada.mare_alta.service -> com.pousada.mare_alta.util
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.pousada.mare_alta` | 4 | 0 |
| `com.pousada.mare_alta.database` | 0 | 2 |
| `com.pousada.mare_alta.document` | 2 | 1 |
| `com.pousada.mare_alta.http` | 5 | 1 |
| `com.pousada.mare_alta.http.request` | 0 | 1 |
| `com.pousada.mare_alta.http.response` | 2 | 1 |
| `com.pousada.mare_alta.model` | 0 | 5 |
| `com.pousada.mare_alta.repository` | 2 | 3 |
| `com.pousada.mare_alta.service` | 4 | 2 |
| `com.pousada.mare_alta.util` | 0 | 3 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.pousada.mare_alta` | sim |  |  |  |  |  |
| `com.pousada.mare_alta.database` |  | sim |  |  |  |  |
| `com.pousada.mare_alta.document` |  |  |  |  |  |  |
| `com.pousada.mare_alta.http` | sim |  | sim |  |  |  |
| `com.pousada.mare_alta.http.request` |  |  | sim |  |  |  |
| `com.pousada.mare_alta.http.response` |  |  | sim |  |  |  |
| `com.pousada.mare_alta.model` |  |  |  |  |  |  |
| `com.pousada.mare_alta.repository` |  | sim |  |  |  |  |
| `com.pousada.mare_alta.service` |  |  |  |  |  |  |
| `com.pousada.mare_alta.util` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  254 src/main/java/com/pousada/mare_alta/service/ReservationService.java
  251 src/main/java/com/pousada/mare_alta/http/AppRoutes.java
  192 src/main/java/com/pousada/mare_alta/repository/ReservationRepository.java
  150 src/main/java/com/pousada/mare_alta/model/Reservation.java
  119 src/main/java/com/pousada/mare_alta/model/Document.java
  106 src/main/java/com/pousada/mare_alta/document/DocumentData.java
   95 src/main/java/com/pousada/mare_alta/database/Database.java
   91 src/main/java/com/pousada/mare_alta/service/ReportService.java
   66 src/main/java/com/pousada/mare_alta/repository/MessageRepository.java
   55 src/main/java/com/pousada/mare_alta/http/response/ReservationResponse.java
```
