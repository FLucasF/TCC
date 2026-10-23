# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 12 testes rodaram (0 falhas, 0 erros, 0 pulados); 12 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 142 de 515 (27.6%)
- ramos (if/switch): 34 de 90 (37.8%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `pousada` | 15 de 26 | 0 de 4 |
| `pousada.document` | 79 de 88 | 14 de 14 |
| `pousada.domain` | 48 de 57 | 20 de 22 |
| `pousada.persistence` | 0 de 125 | 0 de 8 |
| `pousada.service` | 0 de 67 | 0 de 12 |
| `pousada.web` | 0 de 152 | 0 de 30 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `pousada.DocumentTest` | 4 | 0 | 0 |
| `pousada.PricingTest` | 4 | 0 | 0 |
| `pousada.ReservationTest` | 4 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 3
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
4 testes  src/test/java/pousada/ReservationTest.java
4 testes  src/test/java/pousada/PricingTest.java
4 testes  src/test/java/pousada/DocumentTest.java
```
