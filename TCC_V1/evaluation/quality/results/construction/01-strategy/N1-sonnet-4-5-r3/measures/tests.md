# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 8 testes rodaram (0 falhas, 0 erros, 0 pulados); 8 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 68 de 773 (8.8%)
- ramos (if/switch): 15 de 152 (9.9%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `pousada` | 0 de 37 | 0 de 4 |
| `pousada.api` | 0 de 268 | 0 de 38 |
| `pousada.config` | 0 de 12 | 0 de 6 |
| `pousada.domain` | 9 de 112 | 0 de 26 |
| `pousada.domain.rateplan` | 59 de 82 | 15 de 30 |
| `pousada.repository` | 0 de 228 | 0 de 42 |
| `pousada.service` | 0 de 34 | 0 de 6 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `pousada.RatePlanTest` | 8 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 1
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
8 testes  src/test/java/pousada/RatePlanTest.java
```
