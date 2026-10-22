# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 16 testes rodaram (0 falhas, 1 erros, 0 pulados); 29 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 127 de 848 (15.0%)
- ramos (if/switch): 36 de 150 (24.0%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `com.pousadamarealta` | 0 de 51 | 0 de 10 |
| `com.pousadamarealta.controller` | 0 de 154 | 0 de 12 |
| `com.pousadamarealta.controller.dto` | 0 de 47 | 0 de 0 |
| `com.pousadamarealta.domain` | 86 de 108 | 23 de 34 |
| `com.pousadamarealta.infrastructure` | 0 de 36 | 0 de 4 |
| `com.pousadamarealta.messaging` | 14 de 60 | 0 de 8 |
| `com.pousadamarealta.messaging.rules` | 27 de 31 | 13 de 20 |
| `com.pousadamarealta.repository` | 0 de 234 | 0 de 40 |
| `com.pousadamarealta.service` | 0 de 127 | 0 de 22 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `com.pousadamarealta.IntegrationTest` | 1 | 1 | 0 |
| `com.pousadamarealta.MessageRulesTest` | 4 | 0 | 0 |
| `com.pousadamarealta.PriceCalculatorTest` | 5 | 0 | 0 |
| `com.pousadamarealta.ReservationTest` | 6 | 0 | 0 |

## Os testes que falharam

```
[ERROR]   IntegrationTest » IllegalState Could not find a valid Docker environment. Please see logs and check configuration
```

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 4
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 1
- contra o banco (DataSource, JDBC, Testcontainers): 1
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
14 testes  src/test/java/com/pousadamarealta/IntegrationTest.java
6 testes  src/test/java/com/pousadamarealta/ReservationTest.java
5 testes  src/test/java/com/pousadamarealta/PriceCalculatorTest.java
4 testes  src/test/java/com/pousadamarealta/MessageRulesTest.java
```
