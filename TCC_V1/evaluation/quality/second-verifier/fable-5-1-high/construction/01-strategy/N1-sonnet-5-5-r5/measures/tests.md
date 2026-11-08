# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 14 testes rodaram (0 falhas, 0 erros, 0 pulados); 14 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 122 de 400 (30.5%)
- ramos (if/switch): 35 de 96 (36.5%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `pousada` | 0 de 12 | 0 de 8 |
| `pousada.domain` | 122 de 126 | 35 de 40 |
| `pousada.persistence` | 0 de 120 | 0 de 10 |
| `pousada.service` | 0 de 70 | 0 de 10 |
| `pousada.web` | 0 de 72 | 0 de 28 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `pousada.domain.RatePlanTest` | 9 | 0 | 0 |
| `pousada.domain.ReservationTest` | 5 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 2
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
9 testes  src/test/java/pousada/domain/RatePlanTest.java
5 testes  src/test/java/pousada/domain/ReservationTest.java
```
