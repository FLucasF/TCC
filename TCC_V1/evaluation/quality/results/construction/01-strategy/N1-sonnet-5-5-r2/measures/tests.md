# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 9 testes rodaram (0 falhas, 0 erros, 0 pulados); 9 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 298 de 456 (65.4%)
- ramos (if/switch): 54 de 88 (61.4%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `pousada` | 0 de 7 | 0 de 0 |
| `pousada.api` | 67 de 83 | 13 de 24 |
| `pousada.domain` | 121 de 121 | 24 de 32 |
| `pousada.infra` | 0 de 130 | 0 de 12 |
| `pousada.plan` | 51 de 55 | 14 de 16 |
| `pousada.report` | 27 de 27 | 1 de 2 |
| `pousada.service` | 32 de 33 | 2 de 2 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `pousada.ApiTest` | 4 | 0 | 0 |
| `pousada.RatePlanTest` | 5 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 3
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 1
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
5 testes  src/test/java/pousada/RatePlanTest.java
4 testes  src/test/java/pousada/ApiTest.java
0 testes  src/test/java/pousada/InMemoryRepository.java
```
