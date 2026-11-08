# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 7 testes rodaram (0 falhas, 0 erros, 0 pulados); 7 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 441 de 476 (92.6%)
- ramos (if/switch): 72 de 102 (70.6%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `pousada` | 332 de 364 | 60 de 84 |
| `pousada.report` | 109 de 112 | 12 de 18 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `pousada.ApiIntegrationTest` | 2 | 0 | 0 |
| `pousada.PricingTest` | 5 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 2
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 1
- contra o banco (DataSource, JDBC, Testcontainers): 1
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
5 testes  src/test/java/pousada/PricingTest.java
2 testes  src/test/java/pousada/ApiIntegrationTest.java
```
