# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 28 testes rodaram (0 falhas, 0 erros, 0 pulados); 28 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 547 de 593 (92.2%)
- ramos (if/switch): 135 de 158 (85.4%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `hotel` | 25 de 30 | 2 de 4 |
| `hotel.documents` | 62 de 77 | 6 de 8 |
| `hotel.messages` | 29 de 33 | 2 de 2 |
| `hotel.reports` | 48 de 48 | 22 de 22 |
| `hotel.reservations` | 241 de 255 | 82 de 92 |
| `hotel.rooms` | 25 de 29 | 4 de 4 |
| `hotel.web` | 117 de 121 | 17 de 26 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `hotel.reports.ReportsTest` | 5 | 0 | 0 |
| `hotel.reservations.PriceCalculatorTest` | 6 | 0 | 0 |
| `hotel.reservations.ReservationServiceTest` | 11 | 0 | 0 |
| `hotel.web.HttpApiTest` | 6 | 0 | 0 |

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
6 testes  src/test/java/hotel/web/HttpApiTest.java
6 testes  src/test/java/hotel/reservations/PriceCalculatorTest.java
5 testes  src/test/java/hotel/reports/ReportsTest.java
0 testes  src/test/java/hotel/EmptyDatabase.java
```
