# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 6 pacotes, 10 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `br.com.marealta` | 1 | 19 |
| `br.com.marealta.domain` | 13 | 334 |
| `br.com.marealta.http` | 3 | 234 |
| `br.com.marealta.persistence` | 4 | 247 |
| `br.com.marealta.plan` | 7 | 193 |
| `br.com.marealta.service` | 3 | 193 |

## As dependências entre os pacotes do projeto

```
br.com.marealta -> br.com.marealta.http
br.com.marealta -> br.com.marealta.persistence
br.com.marealta -> br.com.marealta.service
br.com.marealta.http -> br.com.marealta.domain
br.com.marealta.http -> br.com.marealta.service
br.com.marealta.persistence -> br.com.marealta.domain
br.com.marealta.plan -> br.com.marealta.domain
br.com.marealta.service -> br.com.marealta.domain
br.com.marealta.service -> br.com.marealta.persistence
br.com.marealta.service -> br.com.marealta.plan
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `br.com.marealta` | 3 | 0 |
| `br.com.marealta.domain` | 0 | 4 |
| `br.com.marealta.http` | 2 | 1 |
| `br.com.marealta.persistence` | 1 | 2 |
| `br.com.marealta.plan` | 1 | 1 |
| `br.com.marealta.service` | 3 | 2 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `br.com.marealta` | sim |  |  |  |  |  |
| `br.com.marealta.domain` |  |  |  |  |  |  |
| `br.com.marealta.http` | sim |  | sim |  |  |  |
| `br.com.marealta.persistence` |  | sim |  |  | sim |  |
| `br.com.marealta.plan` |  |  |  |  |  |  |
| `br.com.marealta.service` |  | sim |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  133 src/main/java/br/com/marealta/service/ReservationService.java
  128 src/main/java/br/com/marealta/persistence/ReservationRepository.java
   94 src/main/java/br/com/marealta/domain/Reservation.java
   82 src/main/java/br/com/marealta/persistence/Database.java
   82 src/main/java/br/com/marealta/http/App.java
   79 src/main/java/br/com/marealta/http/Json.java
   73 src/main/java/br/com/marealta/http/RequestParser.java
   52 src/main/java/br/com/marealta/service/ReportService.java
   41 src/main/java/br/com/marealta/plan/WeekendPackagePlan.java
   37 src/main/java/br/com/marealta/persistence/MessageRepository.java
```
