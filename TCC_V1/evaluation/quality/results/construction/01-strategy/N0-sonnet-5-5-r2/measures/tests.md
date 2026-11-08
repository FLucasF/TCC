# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 18 testes rodaram (0 falhas, 0 erros, 0 pulados); 18 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 386 de 409 (94.4%)
- ramos (if/switch): 85 de 112 (75.9%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `pousada` | 333 de 355 | 71 de 98 |
| `pousada.plan` | 53 de 54 | 14 de 14 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `pousada.ApiIntegrationTest` | 7 | 0 | 0 |
| `pousada.PlansTest` | 11 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 2
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 1
- contra o banco (DataSource, JDBC, Testcontainers): 1
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
11 testes  src/test/java/pousada/PlansTest.java
7 testes  src/test/java/pousada/ApiIntegrationTest.java
```
