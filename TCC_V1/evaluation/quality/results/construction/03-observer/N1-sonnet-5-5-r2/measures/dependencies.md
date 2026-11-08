# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 6 pacotes, 14 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `br.com.marealta.pousada` | 1 | 42 |
| `br.com.marealta.pousada.app` | 3 | 152 |
| `br.com.marealta.pousada.domain` | 12 | 342 |
| `br.com.marealta.pousada.notification` | 10 | 179 |
| `br.com.marealta.pousada.persistence` | 3 | 297 |
| `br.com.marealta.pousada.web` | 3 | 236 |

## As dependências entre os pacotes do projeto

```
br.com.marealta.pousada.app -> br.com.marealta.pousada.domain
br.com.marealta.pousada.app -> br.com.marealta.pousada.notification
br.com.marealta.pousada.app -> br.com.marealta.pousada.persistence
br.com.marealta.pousada -> br.com.marealta.pousada.app
br.com.marealta.pousada -> br.com.marealta.pousada.notification
br.com.marealta.pousada -> br.com.marealta.pousada.persistence
br.com.marealta.pousada -> br.com.marealta.pousada.web
br.com.marealta.pousada.notification -> br.com.marealta.pousada.domain
br.com.marealta.pousada.persistence -> br.com.marealta.pousada.domain
br.com.marealta.pousada.persistence -> br.com.marealta.pousada.notification
br.com.marealta.pousada.web -> br.com.marealta.pousada.app
br.com.marealta.pousada.web -> br.com.marealta.pousada.domain
br.com.marealta.pousada.web -> br.com.marealta.pousada.notification
br.com.marealta.pousada.web -> br.com.marealta.pousada.persistence
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `br.com.marealta.pousada` | 4 | 0 |
| `br.com.marealta.pousada.app` | 3 | 2 |
| `br.com.marealta.pousada.domain` | 0 | 4 |
| `br.com.marealta.pousada.notification` | 1 | 4 |
| `br.com.marealta.pousada.persistence` | 2 | 3 |
| `br.com.marealta.pousada.web` | 4 | 1 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `br.com.marealta.pousada` | sim |  |  |  |  |  |
| `br.com.marealta.pousada.app` |  |  |  |  |  |  |
| `br.com.marealta.pousada.domain` |  |  |  |  |  |  |
| `br.com.marealta.pousada.notification` |  |  |  |  |  |  |
| `br.com.marealta.pousada.persistence` |  | sim |  |  | sim |  |
| `br.com.marealta.pousada.web` | sim |  | sim |  |  |  |

## As maiores classes (linhas, src/main)

```
  143 src/main/java/br/com/marealta/pousada/persistence/ReservationRepository.java
  118 src/main/java/br/com/marealta/pousada/web/Routes.java
  114 src/main/java/br/com/marealta/pousada/domain/Reservation.java
  104 src/main/java/br/com/marealta/pousada/persistence/Database.java
   95 src/main/java/br/com/marealta/pousada/app/ReservationService.java
   77 src/main/java/br/com/marealta/pousada/web/BookingParser.java
   50 src/main/java/br/com/marealta/pousada/persistence/MessageRepository.java
   48 src/main/java/br/com/marealta/pousada/app/ReportService.java
   42 src/main/java/br/com/marealta/pousada/Main.java
   41 src/main/java/br/com/marealta/pousada/web/Json.java
```
