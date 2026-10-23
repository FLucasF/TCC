# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 14 testes rodaram (0 falhas, 0 erros, 0 pulados); 14 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 510 de 539 (94.6%)
- ramos (if/switch): 107 de 131 (81.7%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `br.com.marealta` | 28 de 33 | 1 de 4 |
| `br.com.marealta.db` | 28 de 30 | 1 de 2 |
| `br.com.marealta.message` | 112 de 119 | 39 de 45 |
| `br.com.marealta.reservation` | 213 de 216 | 53 de 56 |
| `br.com.marealta.web` | 129 de 141 | 13 de 24 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `br.com.marealta.ApiTest` | 4 | 0 | 0 |
| `br.com.marealta.MessageRulesTest` | 5 | 0 | 0 |
| `br.com.marealta.PricingTest` | 5 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 3
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 1
- contra o banco (DataSource, JDBC, Testcontainers): 1
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
5 testes  src/test/java/br/com/marealta/PricingTest.java
5 testes  src/test/java/br/com/marealta/MessageRulesTest.java
4 testes  src/test/java/br/com/marealta/ApiTest.java
```
