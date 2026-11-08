# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 14 testes rodaram (0 falhas, 0 erros, 0 pulados); 14 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 415 de 443 (93.7%)
- ramos (if/switch): 71 de 86 (82.6%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `pousada` | 0 de 5 | 0 de 4 |
| `pousada.db` | 149 de 157 | 18 de 18 |
| `pousada.domain` | 137 de 141 | 42 de 50 |
| `pousada.error` | 12 de 12 | 0 de 0 |
| `pousada.report` | 80 de 81 | 9 de 12 |
| `pousada.web` | 37 de 47 | 2 de 2 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `pousada.ApiIntegrationTest` | 5 | 0 | 0 |
| `pousada.PricingTest` | 5 | 0 | 0 |
| `pousada.ReceiptAndReportFormatTest` | 4 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 3
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 1
- contra o banco (DataSource, JDBC, Testcontainers): 1
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
5 testes  src/test/java/pousada/PricingTest.java
5 testes  src/test/java/pousada/ApiIntegrationTest.java
4 testes  src/test/java/pousada/ReceiptAndReportFormatTest.java
```
