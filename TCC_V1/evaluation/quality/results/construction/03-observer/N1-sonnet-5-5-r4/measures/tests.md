# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 14 testes rodaram (0 falhas, 0 erros, 0 pulados); 14 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 296 de 437 (67.7%)
- ramos (if/switch): 79 de 98 (80.6%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `br.com.marealta` | 0 de 14 | 0 de 4 |
| `br.com.marealta.app` | 41 de 41 | 10 de 10 |
| `br.com.marealta.domain` | 105 de 105 | 35 de 38 |
| `br.com.marealta.notification` | 47 de 48 | 8 de 8 |
| `br.com.marealta.persistence` | 0 de 118 | 0 de 4 |
| `br.com.marealta.report` | 29 de 29 | 10 de 10 |
| `br.com.marealta.web` | 74 de 82 | 16 de 24 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `br.com.marealta.PricingTest` | 4 | 0 | 0 |
| `br.com.marealta.SystemTest` | 10 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 3
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 1
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
10 testes  src/test/java/br/com/marealta/SystemTest.java
4 testes  src/test/java/br/com/marealta/PricingTest.java
0 testes  src/test/java/br/com/marealta/InMemoryRepositories.java
```
