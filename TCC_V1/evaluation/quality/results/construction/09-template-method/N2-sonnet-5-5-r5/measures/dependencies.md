# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 5 pacotes, 11 dependências entre eles, com ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `br.com.mare` | 1 | 61 |
| `br.com.mare.domain` | 20 | 539 |
| `br.com.mare.persistence` | 3 | 259 |
| `br.com.mare.report` | 8 | 309 |
| `br.com.mare.web` | 5 | 173 |

## As dependências entre os pacotes do projeto

```
br.com.mare -> br.com.mare.domain
br.com.mare -> br.com.mare.persistence
br.com.mare -> br.com.mare.report
br.com.mare -> br.com.mare.web
br.com.mare.domain -> br.com.mare.persistence
br.com.mare.persistence -> br.com.mare.domain
br.com.mare.report -> br.com.mare.domain
br.com.mare.report -> br.com.mare.persistence
br.com.mare.web -> br.com.mare.domain
br.com.mare.web -> br.com.mare.persistence
br.com.mare.web -> br.com.mare.report
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `br.com.mare` | 4 | 0 |
| `br.com.mare.domain` | 1 | 4 |
| `br.com.mare.persistence` | 1 | 4 |
| `br.com.mare.report` | 2 | 2 |
| `br.com.mare.web` | 3 | 1 |

## Os ciclos

O tsort achou ciclos; cada bloco abaixo é um ciclo (os pacotes que o formam):
```
--- um ciclo:
br.com.mare.domain
br.com.mare.persistence
```

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `br.com.mare` | sim | sim |  |  |  |  |
| `br.com.mare.domain` |  |  |  |  |  |  |
| `br.com.mare.persistence` |  | sim |  |  | sim |  |
| `br.com.mare.report` |  |  |  |  |  |  |
| `br.com.mare.web` | sim |  | sim |  |  |  |

## As maiores classes (linhas, src/main)

```
  125 src/main/java/br/com/mare/persistence/JdbcReservationRepository.java
  116 src/main/java/br/com/mare/domain/Reservation.java
   91 src/main/java/br/com/mare/web/Api.java
   90 src/main/java/br/com/mare/domain/ReservationService.java
   82 src/main/java/br/com/mare/web/RequestParser.java
   78 src/main/java/br/com/mare/persistence/Database.java
   78 src/main/java/br/com/mare/domain/ReservationStatus.java
   61 src/main/java/br/com/mare/Main.java
   58 src/main/java/br/com/mare/report/OccupancyReport.java
   56 src/main/java/br/com/mare/persistence/JdbcMessageRepository.java
```
