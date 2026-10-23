# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 12 testes rodaram (0 falhas, 0 erros, 0 pulados); 12 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 493 de 534 (92.3%)
- ramos (if/switch): 81 de 106 (76.4%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `pousada` | 48 de 61 | 6 de 14 |
| `pousada.api` | 128 de 145 | 18 de 30 |
| `pousada.domain` | 79 de 79 | 23 de 24 |
| `pousada.identity` | 94 de 97 | 22 de 24 |
| `pousada.persistence` | 125 de 131 | 6 de 6 |
| `pousada.report` | 19 de 21 | 6 de 8 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `pousada.ApiTest` | 2 | 0 | 0 |
| `pousada.DocumentsTest` | 10 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 2
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 1
- contra o banco (DataSource, JDBC, Testcontainers): 1
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
10 testes  src/test/java/pousada/DocumentsTest.java
2 testes  src/test/java/pousada/ApiTest.java
```
