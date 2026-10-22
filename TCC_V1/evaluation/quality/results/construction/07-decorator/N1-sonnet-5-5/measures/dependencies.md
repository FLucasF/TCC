# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 6 pacotes, 15 dependências entre eles, com ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `br.com.marealta` | 3 | 182 |
| `br.com.marealta.domain` | 12 | 297 |
| `br.com.marealta.mail` | 14 | 231 |
| `br.com.marealta.persistence` | 6 | 354 |
| `br.com.marealta.report` | 3 | 99 |
| `br.com.marealta.web` | 3 | 215 |

## As dependências entre os pacotes do projeto

```
br.com.marealta -> br.com.marealta.domain
br.com.marealta -> br.com.marealta.mail
br.com.marealta -> br.com.marealta.persistence
br.com.marealta -> br.com.marealta.report
br.com.marealta -> br.com.marealta.web
br.com.marealta.mail -> br.com.marealta.domain
br.com.marealta.mail -> br.com.marealta.persistence
br.com.marealta.persistence -> br.com.marealta.domain
br.com.marealta.persistence -> br.com.marealta.mail
br.com.marealta.report -> br.com.marealta.domain
br.com.marealta.report -> br.com.marealta.persistence
br.com.marealta.web -> br.com.marealta
br.com.marealta.web -> br.com.marealta.domain
br.com.marealta.web -> br.com.marealta.persistence
br.com.marealta.web -> br.com.marealta.report
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `br.com.marealta` | 5 | 1 |
| `br.com.marealta.domain` | 0 | 5 |
| `br.com.marealta.mail` | 2 | 2 |
| `br.com.marealta.persistence` | 2 | 4 |
| `br.com.marealta.report` | 2 | 2 |
| `br.com.marealta.web` | 4 | 1 |

## Os ciclos

O tsort achou ciclos; cada bloco abaixo é um ciclo (os pacotes que o formam):
```
--- um ciclo:
br.com.marealta
br.com.marealta.web
--- um ciclo:
br.com.marealta.persistence
br.com.marealta.mail
```

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `br.com.marealta` | sim | sim |  |  |  |  |
| `br.com.marealta.domain` |  |  |  |  |  |  |
| `br.com.marealta.mail` |  | sim |  |  |  |  |
| `br.com.marealta.persistence` |  | sim |  |  |  |  |
| `br.com.marealta.report` |  | sim |  |  |  |  |
| `br.com.marealta.web` | sim | sim | sim |  |  |  |

## As maiores classes (linhas, src/main)

```
  145 src/main/java/br/com/marealta/persistence/ReservationRepository.java
  116 src/main/java/br/com/marealta/ReservationService.java
  102 src/main/java/br/com/marealta/web/Routes.java
   94 src/main/java/br/com/marealta/web/RequestParser.java
   84 src/main/java/br/com/marealta/domain/Reservation.java
   80 src/main/java/br/com/marealta/persistence/Schema.java
   52 src/main/java/br/com/marealta/App.java
   51 src/main/java/br/com/marealta/persistence/RoomRepository.java
   41 src/main/java/br/com/marealta/report/OccupancyReport.java
   40 src/main/java/br/com/marealta/persistence/MessageRepository.java
```
