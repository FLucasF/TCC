# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 5 pacotes, 11 dependências entre eles, com ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.marealta.pousada` | 1 | 30 |
| `com.marealta.pousada.domain` | 16 | 494 |
| `com.marealta.pousada.mail` | 11 | 176 |
| `com.marealta.pousada.persistence` | 6 | 292 |
| `com.marealta.pousada.web` | 5 | 203 |

## As dependências entre os pacotes do projeto

```
com.marealta.pousada -> com.marealta.pousada.domain
com.marealta.pousada -> com.marealta.pousada.mail
com.marealta.pousada -> com.marealta.pousada.persistence
com.marealta.pousada -> com.marealta.pousada.web
com.marealta.pousada.domain -> com.marealta.pousada.mail
com.marealta.pousada.domain -> com.marealta.pousada.persistence
com.marealta.pousada.mail -> com.marealta.pousada.persistence
com.marealta.pousada.persistence -> com.marealta.pousada.domain
com.marealta.pousada.persistence -> com.marealta.pousada.mail
com.marealta.pousada.web -> com.marealta.pousada.domain
com.marealta.pousada.web -> com.marealta.pousada.mail
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.marealta.pousada` | 4 | 0 |
| `com.marealta.pousada.domain` | 2 | 3 |
| `com.marealta.pousada.mail` | 1 | 4 |
| `com.marealta.pousada.persistence` | 2 | 3 |
| `com.marealta.pousada.web` | 2 | 1 |

## Os ciclos

O tsort achou ciclos; cada bloco abaixo é um ciclo (os pacotes que o formam):
```
--- um ciclo:
com.marealta.pousada.domain
com.marealta.pousada.persistence
--- um ciclo:
com.marealta.pousada.mail
com.marealta.pousada.persistence
```

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.marealta.pousada` | sim | sim |  |  |  |  |
| `com.marealta.pousada.domain` |  |  |  |  |  |  |
| `com.marealta.pousada.mail` |  |  |  |  |  |  |
| `com.marealta.pousada.persistence` |  | sim |  |  | sim |  |
| `com.marealta.pousada.web` | sim |  | sim |  |  |  |

## As maiores classes (linhas, src/main)

```
  128 src/main/java/com/marealta/pousada/persistence/JdbcReservationRepository.java
  103 src/main/java/com/marealta/pousada/domain/Reservation.java
   99 src/main/java/com/marealta/pousada/domain/ReservationService.java
   87 src/main/java/com/marealta/pousada/web/Api.java
   75 src/main/java/com/marealta/pousada/persistence/Database.java
   74 src/main/java/com/marealta/pousada/web/NewReservationParser.java
   52 src/main/java/com/marealta/pousada/persistence/JdbcMessageRepository.java
   44 src/main/java/com/marealta/pousada/domain/ReportService.java
   36 src/main/java/com/marealta/pousada/mail/MailService.java
   34 src/main/java/com/marealta/pousada/domain/ReservationDocument.java
```
