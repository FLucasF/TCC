# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 28 testes rodaram (0 falhas, 0 erros, 0 pulados); 16 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 481 de 523 (92.0%)
- ramos (if/switch): 108 de 138 (78.3%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `br.com.marealta` | 6 de 28 | 0 de 12 |
| `br.com.marealta.domain` | 39 de 39 | 23 de 24 |
| `br.com.marealta.report` | 91 de 93 | 19 de 26 |
| `br.com.marealta.service` | 95 de 98 | 27 de 28 |
| `br.com.marealta.store` | 181 de 189 | 27 de 28 |
| `br.com.marealta.web` | 69 de 76 | 12 de 20 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `br.com.marealta.MemoryApiTest` | 12 | 0 | 0 |
| `br.com.marealta.PostgresApiTest` | 12 | 0 | 0 |
| `br.com.marealta.PricingTest` | 4 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 4
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 1
- contra o banco (DataSource, JDBC, Testcontainers): 1
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
12 testes  src/test/java/br/com/marealta/ApiTestBase.java
4 testes  src/test/java/br/com/marealta/PricingTest.java
0 testes  src/test/java/br/com/marealta/PostgresApiTest.java
0 testes  src/test/java/br/com/marealta/MemoryApiTest.java
```
