# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 34 testes rodaram (0 falhas, 0 erros, 0 pulados); 34 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 532 de 567 (93.8%)
- ramos (if/switch): 111 de 128 (86.7%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `hotel` | 25 de 30 | 2 de 4 |
| `hotel.documents` | 30 de 30 | 4 de 4 |
| `hotel.messages` | 29 de 33 | 2 de 2 |
| `hotel.reports` | 48 de 48 | 22 de 22 |
| `hotel.reservations` | 269 de 286 | 59 de 64 |
| `hotel.rooms` | 25 de 29 | 4 de 4 |
| `hotel.web` | 106 de 111 | 18 de 28 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `hotel.reports.ReportsTest` | 5 | 0 | 0 |
| `hotel.reservations.PriceCalculatorTest` | 9 | 0 | 0 |
| `hotel.reservations.ReservationServiceTest` | 16 | 0 | 0 |
| `hotel.web.HttpApiTest` | 4 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 5
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 1
- contra o banco (DataSource, JDBC, Testcontainers): 4
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
16 testes  src/test/java/hotel/reservations/ReservationServiceTest.java
9 testes  src/test/java/hotel/reservations/PriceCalculatorTest.java
5 testes  src/test/java/hotel/reports/ReportsTest.java
4 testes  src/test/java/hotel/web/HttpApiTest.java
0 testes  src/test/java/hotel/EmptyDatabase.java
```
