# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 7 testes rodaram (0 falhas, 0 erros, 0 pulados); 7 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 437 de 471 (92.8%)
- ramos (if/switch): 77 de 102 (75.5%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `br.com.maralta.pousada` | 437 de 471 | 77 de 102 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `br.com.maralta.pousada.ApiTest` | 3 | 0 | 0 |
| `br.com.maralta.pousada.DocumentTest` | 4 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 2
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 1
- contra o banco (DataSource, JDBC, Testcontainers): 1
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
4 testes  src/test/java/br/com/maralta/pousada/DocumentTest.java
3 testes  src/test/java/br/com/maralta/pousada/ApiTest.java
```
