# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 4 pacotes, 8 dependências entre eles, com ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `br.com.marealta` | 3 | 131 |
| `br.com.marealta.domain` | 11 | 425 |
| `br.com.marealta.mail` | 6 | 225 |
| `br.com.marealta.web` | 1 | 195 |

## As dependências entre os pacotes do projeto

```
br.com.marealta -> br.com.marealta.domain
br.com.marealta -> br.com.marealta.mail
br.com.marealta -> br.com.marealta.web
br.com.marealta.domain -> br.com.marealta
br.com.marealta.domain -> br.com.marealta.mail
br.com.marealta.web -> br.com.marealta
br.com.marealta.web -> br.com.marealta.domain
br.com.marealta.web -> br.com.marealta.mail
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `br.com.marealta` | 3 | 2 |
| `br.com.marealta.domain` | 2 | 2 |
| `br.com.marealta.mail` | 0 | 3 |
| `br.com.marealta.web` | 3 | 1 |

## Os ciclos

O tsort achou ciclos; cada bloco abaixo é um ciclo (os pacotes que o formam):
```
--- um ciclo:
br.com.marealta
br.com.marealta.web
br.com.marealta.domain
--- um ciclo:
br.com.marealta
br.com.marealta.web
```

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `br.com.marealta` | sim | sim |  |  |  |  |
| `br.com.marealta.domain` |  | sim |  |  |  |  |
| `br.com.marealta.mail` |  | sim |  |  |  |  |
| `br.com.marealta.web` | sim | sim | sim |  |  |  |

## As maiores classes (linhas, src/main)

```
  209 src/main/java/br/com/marealta/domain/ReservationService.java
  195 src/main/java/br/com/marealta/web/Api.java
  102 src/main/java/br/com/marealta/Db.java
   78 src/main/java/br/com/marealta/mail/Mailer.java
   72 src/main/java/br/com/marealta/domain/Reports.java
   65 src/main/java/br/com/marealta/mail/MessagePolicy.java
   52 src/main/java/br/com/marealta/mail/MailSettings.java
   48 src/main/java/br/com/marealta/domain/Receipt.java
   41 src/main/java/br/com/marealta/domain/Pricing.java
   29 src/main/java/br/com/marealta/Main.java
```
