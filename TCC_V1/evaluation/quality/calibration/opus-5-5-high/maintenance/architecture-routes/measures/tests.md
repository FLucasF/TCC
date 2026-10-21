# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 10 testes rodaram (0 falhas, 0 erros, 0 pulados); 10 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 423 de 561 (75.4%)
- ramos (if/switch): 62 de 106 (58.5%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `hotel` | 24 de 29 | 2 de 4 |
| `hotel.documents` | 29 de 30 | 2 de 4 |
| `hotel.messages` | 29 de 33 | 2 de 2 |
| `hotel.reports` | 50 de 73 | 5 de 10 |
| `hotel.reservations` | 99 de 158 | 22 de 30 |
| `hotel.rooms` | 25 de 29 | 3 de 4 |
| `hotel.web` | 167 de 209 | 26 de 52 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `hotel.reservations.PriceCalculatorTest` | 6 | 0 | 0 |
| `hotel.web.HttpApiTest` | 4 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 3
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 1
- contra o banco (DataSource, JDBC, Testcontainers): 2
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
6 testes  src/test/java/hotel/reservations/PriceCalculatorTest.java
4 testes  src/test/java/hotel/web/HttpApiTest.java
0 testes  src/test/java/hotel/EmptyDatabase.java
```
