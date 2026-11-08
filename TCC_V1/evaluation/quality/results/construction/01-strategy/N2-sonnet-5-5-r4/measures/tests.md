# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 6 testes rodaram (0 falhas, 0 erros, 0 pulados); 6 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 74 de 414 (17.9%)
- ramos (if/switch): 22 de 100 (22.0%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `pousada` | 0 de 20 | 0 de 4 |
| `pousada.domain` | 74 de 120 | 22 de 44 |
| `pousada.persistence` | 0 de 131 | 0 de 14 |
| `pousada.service` | 0 de 81 | 0 de 14 |
| `pousada.web` | 0 de 62 | 0 de 24 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `pousada.domain.RatePlanTest` | 6 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 1
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
6 testes  src/test/java/pousada/domain/RatePlanTest.java
```
