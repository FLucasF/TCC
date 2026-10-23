# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 11 testes rodaram (0 falhas, 0 erros, 0 pulados); 11 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 441 de 487 (90.6%)
- ramos (if/switch): 82 de 109 (75.2%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `br.com.marealta` | 6 de 20 | 2 de 8 |
| `br.com.marealta.domain` | 128 de 129 | 35 de 40 |
| `br.com.marealta.messaging` | 44 de 44 | 15 de 17 |
| `br.com.marealta.persistence` | 111 de 131 | 12 de 14 |
| `br.com.marealta.report` | 27 de 27 | 1 de 2 |
| `br.com.marealta.service` | 23 de 23 | 2 de 2 |
| `br.com.marealta.web` | 102 de 113 | 15 de 26 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `br.com.marealta.ApiIntegrationTest` | 2 | 0 | 0 |
| `br.com.marealta.MessageRulesTest` | 4 | 0 | 0 |
| `br.com.marealta.PricingAndReceiptTest` | 5 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 3
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 1
- contra o banco (DataSource, JDBC, Testcontainers): 1
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
5 testes  src/test/java/br/com/marealta/PricingAndReceiptTest.java
4 testes  src/test/java/br/com/marealta/MessageRulesTest.java
2 testes  src/test/java/br/com/marealta/ApiIntegrationTest.java
```
