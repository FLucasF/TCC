# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 17 testes rodaram (0 falhas, 0 erros, 0 pulados); 17 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 454 de 477 (95.2%)
- ramos (if/switch): 71 de 94 (75.5%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `br.com.marealta` | 0 de 11 | 0 de 8 |
| `br.com.marealta.domain` | 73 de 73 | 20 de 22 |
| `br.com.marealta.infra` | 113 de 115 | 6 de 6 |
| `br.com.marealta.plan` | 44 de 44 | 12 de 12 |
| `br.com.marealta.service` | 100 de 100 | 13 de 14 |
| `br.com.marealta.web` | 124 de 134 | 20 de 32 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `br.com.marealta.ApiTest` | 6 | 0 | 0 |
| `br.com.marealta.PricingTest` | 11 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 2
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 1
- contra o banco (DataSource, JDBC, Testcontainers): 1
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
11 testes  src/test/java/br/com/marealta/PricingTest.java
6 testes  src/test/java/br/com/marealta/ApiTest.java
```
