# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 7 testes rodaram (0 falhas, 0 erros, 0 pulados); 7 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 20 de 951 (2.1%)
- ramos (if/switch): 13 de 216 (6.0%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `com.pousada` | 0 de 30 | 0 de 8 |
| `com.pousada.api` | 0 de 351 | 0 de 50 |
| `com.pousada.domain` | 0 de 92 | 0 de 14 |
| `com.pousada.repo` | 0 de 180 | 0 de 38 |
| `com.pousada.service` | 0 de 102 | 0 de 46 |
| `com.pousada.service.report` | 0 de 150 | 0 de 32 |
| `com.pousada.util` | 20 de 46 | 13 de 28 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `com.pousada.ReservationServiceTest` | 7 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 1
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
7 testes  src/test/java/com/pousada/ReservationServiceTest.java
```
