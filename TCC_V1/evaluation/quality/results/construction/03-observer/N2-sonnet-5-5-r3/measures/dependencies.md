# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 6 pacotes, 13 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.marealta` | 1 | 47 |
| `com.marealta.domain` | 16 | 346 |
| `com.marealta.events` | 9 | 134 |
| `com.marealta.persistence` | 6 | 327 |
| `com.marealta.service` | 3 | 160 |
| `com.marealta.web` | 1 | 222 |

## As dependências entre os pacotes do projeto

```
com.marealta -> com.marealta.domain
com.marealta -> com.marealta.events
com.marealta -> com.marealta.persistence
com.marealta -> com.marealta.service
com.marealta -> com.marealta.web
com.marealta.events -> com.marealta.domain
com.marealta.persistence -> com.marealta.domain
com.marealta.service -> com.marealta.domain
com.marealta.service -> com.marealta.events
com.marealta.service -> com.marealta.persistence
com.marealta.web -> com.marealta.domain
com.marealta.web -> com.marealta.persistence
com.marealta.web -> com.marealta.service
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.marealta` | 5 | 0 |
| `com.marealta.domain` | 0 | 5 |
| `com.marealta.events` | 1 | 2 |
| `com.marealta.persistence` | 1 | 3 |
| `com.marealta.service` | 3 | 2 |
| `com.marealta.web` | 3 | 1 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.marealta` | sim |  |  |  |  |  |
| `com.marealta.domain` |  |  |  |  |  |  |
| `com.marealta.events` |  |  |  |  |  |  |
| `com.marealta.persistence` |  | sim |  |  |  |  |
| `com.marealta.service` |  |  |  |  |  |  |
| `com.marealta.web` | sim |  | sim |  |  |  |

## As maiores classes (linhas, src/main)

```
  222 src/main/java/com/marealta/web/Routes.java
  158 src/main/java/com/marealta/persistence/ReservationRepository.java
  107 src/main/java/com/marealta/persistence/Database.java
  105 src/main/java/com/marealta/domain/Reservation.java
  101 src/main/java/com/marealta/service/ReservationService.java
   53 src/main/java/com/marealta/persistence/MessageRepository.java
   50 src/main/java/com/marealta/service/ReportService.java
   47 src/main/java/com/marealta/App.java
   39 src/main/java/com/marealta/events/HousekeepingNotifier.java
   37 src/main/java/com/marealta/domain/Quote.java
```
