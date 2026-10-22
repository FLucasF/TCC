# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 19 testes rodaram (0 falhas, 0 erros, 0 pulados); 12 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 462 de 498 (92.8%)
- ramos (if/switch): 106 de 132 (80.3%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `br.com.maretalta` | 0 de 10 | 0 de 4 |
| `br.com.maretalta.domain` | 73 de 73 | 26 de 28 |
| `br.com.maretalta.report` | 79 de 85 | 16 de 22 |
| `br.com.maretalta.service` | 70 de 71 | 21 de 22 |
| `br.com.maretalta.store` | 150 de 162 | 23 de 26 |
| `br.com.maretalta.web` | 90 de 97 | 20 de 30 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `br.com.maretalta.ApiTest` | 7 | 0 | 0 |
| `br.com.maretalta.PostgresApiTest` | 7 | 0 | 0 |
| `br.com.maretalta.PricingTest` | 5 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 3
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 1
- contra o banco (DataSource, JDBC, Testcontainers): 1
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
7 testes  src/test/java/br/com/maretalta/ApiTest.java
5 testes  src/test/java/br/com/maretalta/PricingTest.java
0 testes  src/test/java/br/com/maretalta/PostgresApiTest.java
```
