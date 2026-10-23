# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 7 pacotes, 16 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.marealta` | 1 | 44 |
| `com.marealta.domain` | 11 | 284 |
| `com.marealta.mail` | 12 | 178 |
| `com.marealta.persistence` | 5 | 323 |
| `com.marealta.report` | 1 | 57 |
| `com.marealta.service` | 3 | 137 |
| `com.marealta.web` | 3 | 187 |

## As dependências entre os pacotes do projeto

```
com.marealta -> com.marealta.mail
com.marealta -> com.marealta.persistence
com.marealta -> com.marealta.report
com.marealta -> com.marealta.service
com.marealta -> com.marealta.web
com.marealta.persistence -> com.marealta.domain
com.marealta.persistence -> com.marealta.mail
com.marealta.report -> com.marealta.domain
com.marealta.report -> com.marealta.persistence
com.marealta.service -> com.marealta.domain
com.marealta.service -> com.marealta.mail
com.marealta.service -> com.marealta.persistence
com.marealta.web -> com.marealta.domain
com.marealta.web -> com.marealta.persistence
com.marealta.web -> com.marealta.report
com.marealta.web -> com.marealta.service
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.marealta` | 5 | 0 |
| `com.marealta.domain` | 0 | 4 |
| `com.marealta.mail` | 0 | 3 |
| `com.marealta.persistence` | 2 | 4 |
| `com.marealta.report` | 2 | 2 |
| `com.marealta.service` | 3 | 2 |
| `com.marealta.web` | 4 | 1 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.marealta` | sim |  |  |  |  |  |
| `com.marealta.domain` |  |  | sim |  |  |  |
| `com.marealta.mail` |  |  |  |  |  |  |
| `com.marealta.persistence` |  | sim |  |  | sim |  |
| `com.marealta.report` |  | sim |  |  |  |  |
| `com.marealta.service` |  | sim |  |  |  |  |
| `com.marealta.web` | sim |  | sim |  |  |  |

## As maiores classes (linhas, src/main)

```
  127 src/main/java/com/marealta/persistence/ReservationRepository.java
  104 src/main/java/com/marealta/domain/Reservation.java
  100 src/main/java/com/marealta/service/ReservationService.java
   95 src/main/java/com/marealta/persistence/Database.java
   86 src/main/java/com/marealta/web/RequestParser.java
   80 src/main/java/com/marealta/web/Api.java
   58 src/main/java/com/marealta/persistence/MessageLog.java
   57 src/main/java/com/marealta/report/Reports.java
   44 src/main/java/com/marealta/mail/MailPipeline.java
   44 src/main/java/com/marealta/App.java
```
