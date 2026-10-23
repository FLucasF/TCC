# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 22 testes rodaram (0 falhas, 0 erros, 0 pulados); 22 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 419 de 461 (90.9%)
- ramos (if/switch): 132 de 172 (76.7%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `br.com.marealta.pousada` | 419 de 461 | 132 de 172 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `br.com.marealta.pousada.ApiTest` | 9 | 0 | 0 |
| `br.com.marealta.pousada.MessageDispatcherTest` | 8 | 0 | 0 |
| `br.com.marealta.pousada.PricingTest` | 5 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 3
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 1
- contra o banco (DataSource, JDBC, Testcontainers): 1
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
9 testes  src/test/java/br/com/marealta/pousada/ApiTest.java
8 testes  src/test/java/br/com/marealta/pousada/MessageDispatcherTest.java
5 testes  src/test/java/br/com/marealta/pousada/PricingTest.java
```
