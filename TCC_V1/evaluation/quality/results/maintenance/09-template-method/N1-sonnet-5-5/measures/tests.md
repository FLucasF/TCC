# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 29 testes rodaram (0 falhas, 0 erros, 0 pulados); 29 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 526 de 557 (94.4%)
- ramos (if/switch): 111 de 126 (88.1%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `hotel` | 26 de 31 | 2 de 4 |
| `hotel.documents` | 30 de 30 | 4 de 4 |
| `hotel.messages` | 29 de 33 | 2 de 2 |
| `hotel.reports` | 92 de 92 | 32 de 32 |
| `hotel.reservations` | 209 de 222 | 49 de 52 |
| `hotel.rooms` | 25 de 29 | 4 de 4 |
| `hotel.web` | 115 de 120 | 18 de 28 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `hotel.reports.ReportsTest` | 8 | 0 | 0 |
| `hotel.reservations.PriceCalculatorTest` | 6 | 0 | 0 |
| `hotel.reservations.ReservationServiceTest` | 11 | 0 | 0 |
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
11 testes  src/test/java/hotel/reservations/ReservationServiceTest.java
8 testes  src/test/java/hotel/reports/ReportsTest.java
6 testes  src/test/java/hotel/reservations/PriceCalculatorTest.java
4 testes  src/test/java/hotel/web/HttpApiTest.java
0 testes  src/test/java/hotel/EmptyDatabase.java
```
