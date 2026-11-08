# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 48 testes rodaram (0 falhas, 0 erros, 0 pulados); 48 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 518 de 551 (94.0%)
- ramos (if/switch): 121 de 138 (87.7%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `hotel` | 25 de 30 | 2 de 4 |
| `hotel.documents` | 30 de 30 | 4 de 4 |
| `hotel.messages` | 29 de 33 | 2 de 2 |
| `hotel.reports` | 48 de 48 | 22 de 22 |
| `hotel.reservations` | 251 de 266 | 69 de 74 |
| `hotel.rooms` | 25 de 29 | 4 de 4 |
| `hotel.web` | 110 de 115 | 18 de 28 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `hotel.reports.ReportsTest` | 5 | 0 | 0 |
| `hotel.reservations.PriceCalculatorTest` | 19 | 0 | 0 |
| `hotel.reservations.ReservationServiceTest` | 14 | 0 | 0 |
| `hotel.web.HttpApiTest` | 10 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 5
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 1
- contra o banco (DataSource, JDBC, Testcontainers): 4
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
19 testes  src/test/java/hotel/reservations/PriceCalculatorTest.java
14 testes  src/test/java/hotel/reservations/ReservationServiceTest.java
10 testes  src/test/java/hotel/web/HttpApiTest.java
5 testes  src/test/java/hotel/reports/ReportsTest.java
0 testes  src/test/java/hotel/EmptyDatabase.java
```
