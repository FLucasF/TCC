# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 13 testes rodaram (0 falhas, 0 erros, 0 pulados); 13 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 428 de 461 (92.8%)
- ramos (if/switch): 125 de 166 (75.3%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `pousada` | 428 de 461 | 125 de 166 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `pousada.ApiTest` | 6 | 0 | 0 |
| `pousada.UnitTest` | 7 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 2
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 1
- contra o banco (DataSource, JDBC, Testcontainers): 1
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
7 testes  src/test/java/pousada/UnitTest.java
6 testes  src/test/java/pousada/ApiTest.java
```
