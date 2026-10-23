# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 8 pacotes, 18 dependências entre eles, com ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.pousada` | 2 | 258 |
| `com.pousada.api.dto` | 6 | 172 |
| `com.pousada.application` | 8 | 277 |
| `com.pousada.domain.guest` | 5 | 377 |
| `com.pousada.domain.model` | 9 | 386 |
| `com.pousada.domain.repository` | 3 | 37 |
| `com.pousada.domain.service` | 3 | 116 |
| `com.pousada.infrastructure` | 4 | 492 |

## As dependências entre os pacotes do projeto

```
com.pousada.api.dto -> com.pousada.domain.guest
com.pousada.api.dto -> com.pousada.domain.model
com.pousada.application -> com.pousada.domain.guest
com.pousada.application -> com.pousada.domain.model
com.pousada.application -> com.pousada.domain.repository
com.pousada.application -> com.pousada.domain.service
com.pousada -> com.pousada.api.dto
com.pousada -> com.pousada.application
com.pousada -> com.pousada.domain.model
com.pousada -> com.pousada.domain.repository
com.pousada -> com.pousada.infrastructure
com.pousada.domain.guest -> com.pousada.domain.model
com.pousada.domain.model -> com.pousada.domain.guest
com.pousada.domain.repository -> com.pousada.domain.model
com.pousada.domain.service -> com.pousada.domain.model
com.pousada.infrastructure -> com.pousada.domain.guest
com.pousada.infrastructure -> com.pousada.domain.model
com.pousada.infrastructure -> com.pousada.domain.repository
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.pousada` | 5 | 0 |
| `com.pousada.api.dto` | 2 | 1 |
| `com.pousada.application` | 4 | 1 |
| `com.pousada.domain.guest` | 1 | 4 |
| `com.pousada.domain.model` | 1 | 7 |
| `com.pousada.domain.repository` | 1 | 3 |
| `com.pousada.domain.service` | 1 | 1 |
| `com.pousada.infrastructure` | 3 | 1 |

## Os ciclos

O tsort achou ciclos; cada bloco abaixo é um ciclo (os pacotes que o formam):
```
--- um ciclo:
com.pousada.domain.guest
com.pousada.domain.model
```

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.pousada` | sim | sim | sim |  |  |  |
| `com.pousada.api.dto` |  |  | sim |  |  |  |
| `com.pousada.application` |  |  |  |  |  |  |
| `com.pousada.domain.guest` |  |  |  |  |  |  |
| `com.pousada.domain.model` |  |  |  |  |  |  |
| `com.pousada.domain.repository` |  |  |  |  |  |  |
| `com.pousada.domain.service` |  |  |  |  |  |  |
| `com.pousada.infrastructure` |  | sim |  |  | sim |  |

## As maiores classes (linhas, src/main)

```
  258 src/main/java/com/pousada/Application.java
  255 src/main/java/com/pousada/infrastructure/ReservationRepositoryImpl.java
  190 src/main/java/com/pousada/application/ReservationService.java
  175 src/main/java/com/pousada/domain/model/Reservation.java
  115 src/main/java/com/pousada/domain/guest/ForeignGuest.java
  110 src/main/java/com/pousada/domain/guest/CorporateGuest.java
  108 src/main/java/com/pousada/domain/model/Document.java
  102 src/main/java/com/pousada/infrastructure/Database.java
  102 src/main/java/com/pousada/domain/guest/IndividualGuest.java
   90 src/main/java/com/pousada/domain/service/PricingService.java
```
