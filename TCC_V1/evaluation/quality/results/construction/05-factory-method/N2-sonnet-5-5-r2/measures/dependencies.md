# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 4 pacotes, 6 dependências entre eles, com ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `br.com.marealta` | 5 | 181 |
| `br.com.marealta.domain` | 23 | 656 |
| `br.com.marealta.persistence` | 5 | 269 |
| `br.com.marealta.web` | 3 | 265 |

## As dependências entre os pacotes do projeto

```
br.com.marealta -> br.com.marealta.domain
br.com.marealta -> br.com.marealta.persistence
br.com.marealta -> br.com.marealta.web
br.com.marealta.persistence -> br.com.marealta.domain
br.com.marealta.web -> br.com.marealta
br.com.marealta.web -> br.com.marealta.domain
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `br.com.marealta` | 3 | 1 |
| `br.com.marealta.domain` | 0 | 3 |
| `br.com.marealta.persistence` | 1 | 1 |
| `br.com.marealta.web` | 2 | 1 |

## Os ciclos

O tsort achou ciclos; cada bloco abaixo é um ciclo (os pacotes que o formam):
```
--- um ciclo:
br.com.marealta
br.com.marealta.web
```

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `br.com.marealta` | sim | sim |  |  |  |  |
| `br.com.marealta.domain` |  |  |  |  |  |  |
| `br.com.marealta.persistence` |  | sim |  |  |  |  |
| `br.com.marealta.web` | sim |  | sim |  |  |  |

## As maiores classes (linhas, src/main)

```
  146 src/main/java/br/com/marealta/persistence/ReservationRepository.java
  119 src/main/java/br/com/marealta/ReservationService.java
  116 src/main/java/br/com/marealta/web/RequestParser.java
  108 src/main/java/br/com/marealta/domain/Reservation.java
   87 src/main/java/br/com/marealta/persistence/Database.java
   81 src/main/java/br/com/marealta/web/Views.java
   76 src/main/java/br/com/marealta/domain/PassportIdentity.java
   68 src/main/java/br/com/marealta/web/Routes.java
   55 src/main/java/br/com/marealta/domain/PortugueseIdentity.java
   54 src/main/java/br/com/marealta/domain/Identity.java
```
