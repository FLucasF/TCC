# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 22 testes rodaram (0 falhas, 0 erros, 0 pulados); 22 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 343 de 805 (42.6%)
- ramos (if/switch): 53 de 114 (46.5%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `br.com.marealta` | 0 de 74 | 0 de 2 |
| `br.com.marealta.api.controller` | 0 de 98 | 0 de 2 |
| `br.com.marealta.api.dto` | 0 de 34 | 0 de 0 |
| `br.com.marealta.database` | 19 de 21 | 3 de 4 |
| `br.com.marealta.domain` | 85 de 98 | 0 de 0 |
| `br.com.marealta.pricing` | 29 de 30 | 16 de 16 |
| `br.com.marealta.report` | 0 de 143 | 0 de 18 |
| `br.com.marealta.repository` | 112 de 191 | 15 de 40 |
| `br.com.marealta.service` | 98 de 116 | 19 de 32 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `br.com.marealta.IntegrationTest` | 5 | 0 | 0 |
| `br.com.marealta.pricing.PricingServiceTest` | 7 | 0 | 0 |
| `br.com.marealta.pricing.SeasonCalculatorTest` | 7 | 0 | 0 |
| `br.com.marealta.service.DocumentServiceTest` | 3 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 4
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 1
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
7 testes  src/test/java/br/com/marealta/pricing/SeasonCalculatorTest.java
7 testes  src/test/java/br/com/marealta/pricing/PricingServiceTest.java
5 testes  src/test/java/br/com/marealta/IntegrationTest.java
3 testes  src/test/java/br/com/marealta/service/DocumentServiceTest.java
```
