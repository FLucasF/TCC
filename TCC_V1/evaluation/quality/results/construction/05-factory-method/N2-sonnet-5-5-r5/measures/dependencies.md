# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 4 pacotes, 6 dependências entre eles, com ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `br.com.marealta` | 2 | 36 |
| `br.com.marealta.domain` | 35 | 915 |
| `br.com.marealta.persistence` | 5 | 325 |
| `br.com.marealta.web` | 8 | 238 |

## As dependências entre os pacotes do projeto

```
br.com.marealta -> br.com.marealta.domain
br.com.marealta -> br.com.marealta.persistence
br.com.marealta -> br.com.marealta.web
br.com.marealta.domain -> br.com.marealta.persistence
br.com.marealta.persistence -> br.com.marealta.domain
br.com.marealta.web -> br.com.marealta.domain
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `br.com.marealta` | 3 | 0 |
| `br.com.marealta.domain` | 1 | 3 |
| `br.com.marealta.persistence` | 1 | 2 |
| `br.com.marealta.web` | 1 | 1 |

## Os ciclos

O tsort achou ciclos; cada bloco abaixo é um ciclo (os pacotes que o formam):
```
--- um ciclo:
br.com.marealta.domain
br.com.marealta.persistence
```

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `br.com.marealta` | sim |  |  |  |  |  |
| `br.com.marealta.domain` |  |  |  |  |  |  |
| `br.com.marealta.persistence` |  | sim |  |  | sim |  |
| `br.com.marealta.web` | sim |  | sim |  |  |  |

## As maiores classes (linhas, src/main)

```
  158 src/main/java/br/com/marealta/persistence/ReservationRepository.java
  123 src/main/java/br/com/marealta/persistence/Database.java
  119 src/main/java/br/com/marealta/domain/Reservation.java
  103 src/main/java/br/com/marealta/web/RequestReader.java
   79 src/main/java/br/com/marealta/web/Api.java
   77 src/main/java/br/com/marealta/domain/ReservationService.java
   60 src/main/java/br/com/marealta/domain/BookingReceiptDocument.java
   56 src/main/java/br/com/marealta/web/Views.java
   52 src/main/java/br/com/marealta/domain/ReservationDocument.java
   44 src/main/java/br/com/marealta/persistence/MessageRepository.java
```
