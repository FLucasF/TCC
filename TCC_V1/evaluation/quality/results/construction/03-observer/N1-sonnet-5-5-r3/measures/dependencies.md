# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 6 pacotes, 12 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `br.com.marealta.pousada` | 1 | 54 |
| `br.com.marealta.pousada.domain` | 12 | 296 |
| `br.com.marealta.pousada.notification` | 7 | 144 |
| `br.com.marealta.pousada.persistence` | 4 | 280 |
| `br.com.marealta.pousada.service` | 6 | 258 |
| `br.com.marealta.pousada.web` | 2 | 163 |

## As dependências entre os pacotes do projeto

```
br.com.marealta.pousada -> br.com.marealta.pousada.notification
br.com.marealta.pousada -> br.com.marealta.pousada.persistence
br.com.marealta.pousada -> br.com.marealta.pousada.service
br.com.marealta.pousada -> br.com.marealta.pousada.web
br.com.marealta.pousada.notification -> br.com.marealta.pousada.domain
br.com.marealta.pousada.persistence -> br.com.marealta.pousada.domain
br.com.marealta.pousada.persistence -> br.com.marealta.pousada.notification
br.com.marealta.pousada.service -> br.com.marealta.pousada.domain
br.com.marealta.pousada.service -> br.com.marealta.pousada.notification
br.com.marealta.pousada.service -> br.com.marealta.pousada.persistence
br.com.marealta.pousada.web -> br.com.marealta.pousada.domain
br.com.marealta.pousada.web -> br.com.marealta.pousada.service
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `br.com.marealta.pousada` | 4 | 0 |
| `br.com.marealta.pousada.domain` | 0 | 4 |
| `br.com.marealta.pousada.notification` | 1 | 3 |
| `br.com.marealta.pousada.persistence` | 2 | 2 |
| `br.com.marealta.pousada.service` | 3 | 2 |
| `br.com.marealta.pousada.web` | 2 | 1 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `br.com.marealta.pousada` | sim |  |  |  |  |  |
| `br.com.marealta.pousada.domain` |  |  |  |  |  |  |
| `br.com.marealta.pousada.notification` |  |  |  |  |  |  |
| `br.com.marealta.pousada.persistence` |  | sim |  |  |  |  |
| `br.com.marealta.pousada.service` |  | sim |  |  |  |  |
| `br.com.marealta.pousada.web` | sim |  | sim |  |  |  |

## As maiores classes (linhas, src/main)

```
  150 src/main/java/br/com/marealta/pousada/persistence/ReservationRepository.java
  143 src/main/java/br/com/marealta/pousada/service/ReservationService.java
   93 src/main/java/br/com/marealta/pousada/web/App.java
   88 src/main/java/br/com/marealta/pousada/persistence/Database.java
   70 src/main/java/br/com/marealta/pousada/web/ReservationRequestReader.java
   65 src/main/java/br/com/marealta/pousada/service/ReportService.java
   54 src/main/java/br/com/marealta/pousada/Main.java
   52 src/main/java/br/com/marealta/pousada/domain/Reservation.java
   42 src/main/java/br/com/marealta/pousada/persistence/MessageRepository.java
   42 src/main/java/br/com/marealta/pousada/domain/Receipt.java
```
