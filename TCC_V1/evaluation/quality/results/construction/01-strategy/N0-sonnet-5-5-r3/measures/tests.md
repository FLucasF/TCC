# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 8 testes rodaram (0 falhas, 0 erros, 0 pulados); 8 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 93 de 424 (21.9%)
- ramos (if/switch): 30 de 128 (23.4%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `com.marealta` | 45 de 375 | 15 de 112 |
| `com.marealta.plan` | 48 de 49 | 15 de 16 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `com.marealta.PricingTest` | 8 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 1
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
8 testes  src/test/java/com/marealta/PricingTest.java
```
