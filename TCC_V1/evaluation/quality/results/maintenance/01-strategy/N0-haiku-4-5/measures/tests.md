# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 41 testes rodaram (0 falhas, 0 erros, 0 pulados); 41 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 506 de 537 (94.2%)
- ramos (if/switch): 129 de 148 (87.2%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `hotel` | 25 de 30 | 2 de 4 |
| `hotel.documents` | 30 de 30 | 4 de 4 |
| `hotel.messages` | 29 de 33 | 2 de 2 |
| `hotel.reports` | 48 de 48 | 22 de 22 |
| `hotel.reservations` | 244 de 257 | 77 de 84 |
| `hotel.rooms` | 25 de 29 | 4 de 4 |
| `hotel.web` | 105 de 110 | 18 de 28 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `hotel.reports.ReportsTest` | 5 | 0 | 0 |
| `hotel.reservations.PriceCalculatorTest` | 12 | 0 | 0 |
| `hotel.reservations.ReservationServiceTest` | 17 | 0 | 0 |
| `hotel.web.HttpApiTest` | 7 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 5
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 1
- contra o banco (DataSource, JDBC, Testcontainers): 4
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
17 testes  src/test/java/hotel/reservations/ReservationServiceTest.java
12 testes  src/test/java/hotel/reservations/PriceCalculatorTest.java
7 testes  src/test/java/hotel/web/HttpApiTest.java
5 testes  src/test/java/hotel/reports/ReportsTest.java
0 testes  src/test/java/hotel/EmptyDatabase.java
```
