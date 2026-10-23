# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 8 pacotes, 16 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.pousada.marealta` | 1 | 29 |
| `com.pousada.marealta.config` | 2 | 132 |
| `com.pousada.marealta.domain` | 9 | 346 |
| `com.pousada.marealta.http` | 5 | 433 |
| `com.pousada.marealta.messaging` | 6 | 119 |
| `com.pousada.marealta.repository` | 3 | 376 |
| `com.pousada.marealta.service` | 3 | 234 |
| `com.pousada.marealta.util` | 3 | 99 |

## As dependências entre os pacotes do projeto

```
com.pousada.marealta -> com.pousada.marealta.config
com.pousada.marealta -> com.pousada.marealta.http
com.pousada.marealta.http -> com.pousada.marealta.domain
com.pousada.marealta.http -> com.pousada.marealta.messaging
com.pousada.marealta.http -> com.pousada.marealta.repository
com.pousada.marealta.http -> com.pousada.marealta.service
com.pousada.marealta.messaging -> com.pousada.marealta.config
com.pousada.marealta.messaging -> com.pousada.marealta.domain
com.pousada.marealta.messaging -> com.pousada.marealta.util
com.pousada.marealta.repository -> com.pousada.marealta.config
com.pousada.marealta.repository -> com.pousada.marealta.domain
com.pousada.marealta.service -> com.pousada.marealta.domain
com.pousada.marealta.service -> com.pousada.marealta.messaging
com.pousada.marealta.service -> com.pousada.marealta.repository
com.pousada.marealta.service -> com.pousada.marealta.util
com.pousada.marealta.util -> com.pousada.marealta.domain
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.pousada.marealta` | 2 | 0 |
| `com.pousada.marealta.config` | 0 | 3 |
| `com.pousada.marealta.domain` | 0 | 5 |
| `com.pousada.marealta.http` | 4 | 1 |
| `com.pousada.marealta.messaging` | 3 | 2 |
| `com.pousada.marealta.repository` | 2 | 2 |
| `com.pousada.marealta.service` | 4 | 1 |
| `com.pousada.marealta.util` | 1 | 2 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.pousada.marealta` | sim |  |  |  |  |  |
| `com.pousada.marealta.config` |  | sim |  |  |  |  |
| `com.pousada.marealta.domain` |  |  |  |  |  |  |
| `com.pousada.marealta.http` | sim |  |  |  |  | com.google |
| `com.pousada.marealta.messaging` |  |  |  |  |  |  |
| `com.pousada.marealta.repository` |  | sim |  |  |  |  |
| `com.pousada.marealta.service` |  |  |  |  |  |  |
| `com.pousada.marealta.util` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  257 src/main/java/com/pousada/marealta/http/ReservationHandler.java
  200 src/main/java/com/pousada/marealta/repository/ReservationRepository.java
  131 src/main/java/com/pousada/marealta/domain/Reservation.java
  123 src/main/java/com/pousada/marealta/service/ReservationService.java
  108 src/main/java/com/pousada/marealta/domain/Receipt.java
  101 src/main/java/com/pousada/marealta/repository/RoomRepository.java
   90 src/main/java/com/pousada/marealta/config/DatabaseConfig.java
   83 src/main/java/com/pousada/marealta/service/ReportService.java
   75 src/main/java/com/pousada/marealta/repository/MessageRepository.java
   70 src/main/java/com/pousada/marealta/domain/Message.java
```
