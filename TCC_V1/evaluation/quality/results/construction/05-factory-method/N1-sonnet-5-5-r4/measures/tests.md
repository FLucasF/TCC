# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 11 testes rodaram (0 falhas, 0 erros, 0 pulados); 11 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 501 de 537 (93.3%)
- ramos (if/switch): 77 de 98 (78.6%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `pousada` | 0 de 17 | 0 de 8 |
| `pousada.document` | 23 de 23 | 4 de 4 |
| `pousada.domain` | 88 de 89 | 22 de 24 |
| `pousada.identity` | 58 de 58 | 5 de 6 |
| `pousada.persistence` | 120 de 126 | 10 de 10 |
| `pousada.service` | 72 de 72 | 12 de 12 |
| `pousada.web` | 140 de 152 | 24 de 34 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `pousada.ApiIntegrationTest` | 3 | 0 | 0 |
| `pousada.PricingAndDocumentsTest` | 8 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 2
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 1
- contra o banco (DataSource, JDBC, Testcontainers): 1
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
8 testes  src/test/java/pousada/PricingAndDocumentsTest.java
3 testes  src/test/java/pousada/ApiIntegrationTest.java
```
