# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 13 testes rodaram (0 falhas, 0 erros, 0 pulados); 13 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 384 de 403 (95.3%)
- ramos (if/switch): 77 de 96 (80.2%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `br.com.marealta` | 384 de 403 | 77 de 96 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `br.com.marealta.ApiTest` | 4 | 0 | 0 |
| `br.com.marealta.NotificationsTest` | 5 | 0 | 0 |
| `br.com.marealta.PricingTest` | 4 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 3
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 1
- contra o banco (DataSource, JDBC, Testcontainers): 1
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
5 testes  src/test/java/br/com/marealta/NotificationsTest.java
4 testes  src/test/java/br/com/marealta/PricingTest.java
4 testes  src/test/java/br/com/marealta/ApiTest.java
```
