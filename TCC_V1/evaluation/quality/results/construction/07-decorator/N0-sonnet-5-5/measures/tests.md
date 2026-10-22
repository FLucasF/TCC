# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 20 testes rodaram (0 falhas, 0 erros, 0 pulados); 20 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 486 de 524 (92.7%)
- ramos (if/switch): 164 de 200 (82.0%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `br.com.marealta` | 486 de 524 | 164 de 200 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `br.com.marealta.ApiIT` | 7 | 0 | 0 |
| `br.com.marealta.DomainTest` | 13 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 2
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 1
- contra o banco (DataSource, JDBC, Testcontainers): 1
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
13 testes  src/test/java/br/com/marealta/DomainTest.java
7 testes  src/test/java/br/com/marealta/ApiIT.java
```
