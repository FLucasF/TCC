# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 7 pacotes, 17 dependências entre eles, com ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `br.com.marealta` | 1 | 46 |
| `br.com.marealta.app` | 7 | 151 |
| `br.com.marealta.domain` | 13 | 349 |
| `br.com.marealta.notification` | 8 | 159 |
| `br.com.marealta.persistence` | 4 | 262 |
| `br.com.marealta.report` | 3 | 80 |
| `br.com.marealta.web` | 3 | 193 |

## As dependências entre os pacotes do projeto

```
br.com.marealta.app -> br.com.marealta.domain
br.com.marealta.app -> br.com.marealta.notification
br.com.marealta -> br.com.marealta.app
br.com.marealta -> br.com.marealta.domain
br.com.marealta -> br.com.marealta.notification
br.com.marealta -> br.com.marealta.persistence
br.com.marealta -> br.com.marealta.report
br.com.marealta -> br.com.marealta.web
br.com.marealta.notification -> br.com.marealta.app
br.com.marealta.notification -> br.com.marealta.domain
br.com.marealta.persistence -> br.com.marealta.app
br.com.marealta.persistence -> br.com.marealta.domain
br.com.marealta.report -> br.com.marealta.app
br.com.marealta.report -> br.com.marealta.domain
br.com.marealta.web -> br.com.marealta.app
br.com.marealta.web -> br.com.marealta.domain
br.com.marealta.web -> br.com.marealta.report
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `br.com.marealta` | 6 | 0 |
| `br.com.marealta.app` | 2 | 5 |
| `br.com.marealta.domain` | 0 | 6 |
| `br.com.marealta.notification` | 2 | 2 |
| `br.com.marealta.persistence` | 2 | 1 |
| `br.com.marealta.report` | 2 | 2 |
| `br.com.marealta.web` | 3 | 1 |

## Os ciclos

O tsort achou ciclos; cada bloco abaixo é um ciclo (os pacotes que o formam):
```
--- um ciclo:
br.com.marealta.app
br.com.marealta.notification
```

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `br.com.marealta` | sim | sim |  |  | sim |  |
| `br.com.marealta.app` |  |  |  |  |  |  |
| `br.com.marealta.domain` |  |  |  |  |  |  |
| `br.com.marealta.notification` |  |  |  |  |  |  |
| `br.com.marealta.persistence` |  | sim |  |  | sim |  |
| `br.com.marealta.report` |  |  |  |  |  |  |
| `br.com.marealta.web` | sim |  | sim |  |  |  |

## As maiores classes (linhas, src/main)

```
  143 src/main/java/br/com/marealta/persistence/JdbcReservationRepository.java
   98 src/main/java/br/com/marealta/web/WebApp.java
   87 src/main/java/br/com/marealta/domain/Reservation.java
   87 src/main/java/br/com/marealta/app/ReservationService.java
   83 src/main/java/br/com/marealta/web/ReservationRequestParser.java
   60 src/main/java/br/com/marealta/persistence/Database.java
   59 src/main/java/br/com/marealta/persistence/JdbcMessageRepository.java
   46 src/main/java/br/com/marealta/Main.java
   41 src/main/java/br/com/marealta/domain/Receipt.java
   32 src/main/java/br/com/marealta/notification/Notifier.java
```
