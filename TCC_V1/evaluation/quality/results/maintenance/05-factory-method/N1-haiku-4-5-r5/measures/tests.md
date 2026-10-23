# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 30 testes rodaram (0 falhas, 0 erros, 0 pulados); 30 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 557 de 617 (90.3%)
- ramos (if/switch): 133 de 158 (84.2%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `hotel` | 28 de 34 | 2 de 4 |
| `hotel.documents` | 69 de 70 | 13 de 18 |
| `hotel.messages` | 29 de 33 | 2 de 2 |
| `hotel.reports` | 48 de 48 | 22 de 22 |
| `hotel.reservations` | 222 de 235 | 49 de 52 |
| `hotel.rooms` | 25 de 29 | 4 de 4 |
| `hotel.web` | 136 de 168 | 41 de 56 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `hotel.reports.ReportsTest` | 5 | 0 | 0 |
| `hotel.reservations.PriceCalculatorTest` | 6 | 0 | 0 |
| `hotel.reservations.ReservationServiceTest` | 11 | 0 | 0 |
| `hotel.web.HttpApiTest` | 8 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 5
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 1
- contra o banco (DataSource, JDBC, Testcontainers): 4
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
11 testes  src/test/java/hotel/reservations/ReservationServiceTest.java
8 testes  src/test/java/hotel/web/HttpApiTest.java
6 testes  src/test/java/hotel/reservations/PriceCalculatorTest.java
5 testes  src/test/java/hotel/reports/ReportsTest.java
0 testes  src/test/java/hotel/EmptyDatabase.java
```
