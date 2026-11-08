# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 9 testes rodaram (0 falhas, 0 erros, 0 pulados); 9 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 263 de 440 (59.8%)
- ramos (if/switch): 63 de 98 (64.3%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `br.com.marealta` | 0 de 14 | 0 de 8 |
| `br.com.marealta.domain` | 87 de 87 | 32 de 34 |
| `br.com.marealta.report` | 65 de 90 | 6 de 14 |
| `br.com.marealta.service` | 45 de 46 | 6 de 6 |
| `br.com.marealta.store` | 0 de 127 | 0 de 6 |
| `br.com.marealta.web` | 66 de 76 | 19 de 30 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `br.com.marealta.ApiTest` | 5 | 0 | 0 |
| `br.com.marealta.PricingTest` | 4 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 3
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 1
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
5 testes  src/test/java/br/com/marealta/ApiTest.java
4 testes  src/test/java/br/com/marealta/PricingTest.java
0 testes  src/test/java/br/com/marealta/InMemoryStore.java
```
