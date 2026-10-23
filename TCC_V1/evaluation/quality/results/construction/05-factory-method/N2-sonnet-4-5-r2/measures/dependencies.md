# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 7 pacotes, 15 dependências entre eles, com ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `br.com.pousada.maralta` | 1 | 73 |
| `br.com.pousada.maralta.controller` | 5 | 370 |
| `br.com.pousada.maralta.database` | 1 | 96 |
| `br.com.pousada.maralta.domain` | 7 | 412 |
| `br.com.pousada.maralta.guesttype` | 5 | 417 |
| `br.com.pousada.maralta.repository` | 3 | 381 |
| `br.com.pousada.maralta.service` | 11 | 401 |

## As dependências entre os pacotes do projeto

```
br.com.pousada.maralta -> br.com.pousada.maralta.controller
br.com.pousada.maralta -> br.com.pousada.maralta.database
br.com.pousada.maralta -> br.com.pousada.maralta.repository
br.com.pousada.maralta -> br.com.pousada.maralta.service
br.com.pousada.maralta.controller -> br.com.pousada.maralta.domain
br.com.pousada.maralta.controller -> br.com.pousada.maralta.guesttype
br.com.pousada.maralta.controller -> br.com.pousada.maralta.service
br.com.pousada.maralta.domain -> br.com.pousada.maralta.guesttype
br.com.pousada.maralta.guesttype -> br.com.pousada.maralta.domain
br.com.pousada.maralta.repository -> br.com.pousada.maralta.database
br.com.pousada.maralta.repository -> br.com.pousada.maralta.domain
br.com.pousada.maralta.repository -> br.com.pousada.maralta.guesttype
br.com.pousada.maralta.service -> br.com.pousada.maralta.domain
br.com.pousada.maralta.service -> br.com.pousada.maralta.guesttype
br.com.pousada.maralta.service -> br.com.pousada.maralta.repository
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `br.com.pousada.maralta` | 4 | 0 |
| `br.com.pousada.maralta.controller` | 3 | 1 |
| `br.com.pousada.maralta.database` | 0 | 2 |
| `br.com.pousada.maralta.domain` | 1 | 4 |
| `br.com.pousada.maralta.guesttype` | 1 | 4 |
| `br.com.pousada.maralta.repository` | 3 | 2 |
| `br.com.pousada.maralta.service` | 3 | 2 |

## Os ciclos

O tsort achou ciclos; cada bloco abaixo é um ciclo (os pacotes que o formam):
```
--- um ciclo:
br.com.pousada.maralta.domain
br.com.pousada.maralta.guesttype
```

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `br.com.pousada.maralta` | sim |  | sim |  |  |  |
| `br.com.pousada.maralta.controller` | sim |  |  |  |  |  |
| `br.com.pousada.maralta.database` |  | sim |  |  | sim |  |
| `br.com.pousada.maralta.domain` |  |  | sim |  |  |  |
| `br.com.pousada.maralta.guesttype` |  |  |  |  |  |  |
| `br.com.pousada.maralta.repository` |  | sim |  |  |  |  |
| `br.com.pousada.maralta.service` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  262 src/main/java/br/com/pousada/maralta/repository/ReservationRepository.java
  231 src/main/java/br/com/pousada/maralta/controller/ReservationController.java
  201 src/main/java/br/com/pousada/maralta/service/ReservationService.java
  159 src/main/java/br/com/pousada/maralta/domain/Reservation.java
  130 src/main/java/br/com/pousada/maralta/domain/Document.java
  114 src/main/java/br/com/pousada/maralta/guesttype/CorporateGuest.java
  109 src/main/java/br/com/pousada/maralta/guesttype/ForeignGuest.java
  106 src/main/java/br/com/pousada/maralta/guesttype/IndividualGuest.java
   96 src/main/java/br/com/pousada/maralta/database/Database.java
   75 src/main/java/br/com/pousada/maralta/service/ReportService.java
```
