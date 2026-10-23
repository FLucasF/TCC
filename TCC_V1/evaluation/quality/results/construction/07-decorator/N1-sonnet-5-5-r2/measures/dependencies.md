# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 7 pacotes, 13 dependências entre eles, com ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `br.com.marealta.pousada` | 1 | 35 |
| `br.com.marealta.pousada.domain` | 10 | 263 |
| `br.com.marealta.pousada.mail` | 8 | 120 |
| `br.com.marealta.pousada.mail.rules` | 3 | 68 |
| `br.com.marealta.pousada.service` | 5 | 249 |
| `br.com.marealta.pousada.store` | 5 | 332 |
| `br.com.marealta.pousada.web` | 2 | 176 |

## As dependências entre os pacotes do projeto

```
br.com.marealta.pousada -> br.com.marealta.pousada.mail
br.com.marealta.pousada -> br.com.marealta.pousada.service
br.com.marealta.pousada -> br.com.marealta.pousada.store
br.com.marealta.pousada -> br.com.marealta.pousada.web
br.com.marealta.pousada.mail -> br.com.marealta.pousada.mail.rules
br.com.marealta.pousada.mail.rules -> br.com.marealta.pousada.mail
br.com.marealta.pousada.service -> br.com.marealta.pousada.domain
br.com.marealta.pousada.service -> br.com.marealta.pousada.mail
br.com.marealta.pousada.service -> br.com.marealta.pousada.store
br.com.marealta.pousada.store -> br.com.marealta.pousada.domain
br.com.marealta.pousada.store -> br.com.marealta.pousada.mail
br.com.marealta.pousada.web -> br.com.marealta.pousada.domain
br.com.marealta.pousada.web -> br.com.marealta.pousada.service
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `br.com.marealta.pousada` | 4 | 0 |
| `br.com.marealta.pousada.domain` | 0 | 3 |
| `br.com.marealta.pousada.mail` | 1 | 4 |
| `br.com.marealta.pousada.mail.rules` | 1 | 1 |
| `br.com.marealta.pousada.service` | 3 | 2 |
| `br.com.marealta.pousada.store` | 2 | 2 |
| `br.com.marealta.pousada.web` | 2 | 1 |

## Os ciclos

O tsort achou ciclos; cada bloco abaixo é um ciclo (os pacotes que o formam):
```
--- um ciclo:
br.com.marealta.pousada.mail
br.com.marealta.pousada.mail.rules
```

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `br.com.marealta.pousada` | sim |  |  |  |  |  |
| `br.com.marealta.pousada.domain` |  |  |  |  |  |  |
| `br.com.marealta.pousada.mail` |  |  |  |  |  |  |
| `br.com.marealta.pousada.mail.rules` |  |  |  |  |  |  |
| `br.com.marealta.pousada.service` |  | sim |  |  |  |  |
| `br.com.marealta.pousada.store` |  | sim |  |  | sim |  |
| `br.com.marealta.pousada.web` | sim |  | sim |  |  |  |

## As maiores classes (linhas, src/main)

```
  146 src/main/java/br/com/marealta/pousada/store/ReservationRepository.java
  112 src/main/java/br/com/marealta/pousada/service/ReservationService.java
   99 src/main/java/br/com/marealta/pousada/store/Database.java
   93 src/main/java/br/com/marealta/pousada/web/Routes.java
   83 src/main/java/br/com/marealta/pousada/web/ReservationRequest.java
   65 src/main/java/br/com/marealta/pousada/domain/Reservation.java
   62 src/main/java/br/com/marealta/pousada/service/ReportService.java
   48 src/main/java/br/com/marealta/pousada/store/RoomRepository.java
   44 src/main/java/br/com/marealta/pousada/domain/Receipt.java
   39 src/main/java/br/com/marealta/pousada/store/MessageRepository.java
```
