# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 11 testes rodaram (0 falhas, 0 erros, 0 pulados); 11 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 130 de 845 (15.4%)
- ramos (if/switch): 31 de 178 (17.4%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `com.pousadamarealta` | 0 de 51 | 0 de 4 |
| `com.pousadamarealta.api.dto` | 0 de 6 | 0 de 0 |
| `com.pousadamarealta.api.handler` | 0 de 159 | 0 de 40 |
| `com.pousadamarealta.api.mapper` | 0 de 45 | 0 de 2 |
| `com.pousadamarealta.database` | 0 de 214 | 0 de 52 |
| `com.pousadamarealta.domain` | 17 de 94 | 0 de 10 |
| `com.pousadamarealta.messaging` | 44 de 45 | 4 de 4 |
| `com.pousadamarealta.messaging.rules` | 29 de 31 | 13 de 18 |
| `com.pousadamarealta.reports` | 0 de 40 | 0 de 6 |
| `com.pousadamarealta.service` | 40 de 148 | 14 de 42 |
| `com.pousadamarealta.service.exceptions` | 0 de 12 | 0 de 0 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `com.pousadamarealta.MessagePipelineTest` | 6 | 0 | 0 |
| `com.pousadamarealta.PricingCalculatorTest` | 5 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 2
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
6 testes  src/test/java/com/pousadamarealta/MessagePipelineTest.java
5 testes  src/test/java/com/pousadamarealta/PricingCalculatorTest.java
```
