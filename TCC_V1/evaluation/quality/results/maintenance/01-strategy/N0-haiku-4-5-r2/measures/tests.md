# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 56 testes rodaram (0 falhas, 0 erros, 0 pulados); 56 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 536 de 567 (94.5%)
- ramos (if/switch): 138 de 157 (87.9%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `hotel` | 25 de 30 | 2 de 4 |
| `hotel.documents` | 30 de 30 | 4 de 4 |
| `hotel.messages` | 29 de 33 | 2 de 2 |
| `hotel.reports` | 48 de 48 | 22 de 22 |
| `hotel.reservations` | 273 de 286 | 86 de 93 |
| `hotel.rooms` | 25 de 29 | 4 de 4 |
| `hotel.web` | 106 de 111 | 18 de 28 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `hotel.reports.ReportsTest` | 5 | 0 | 0 |
| `hotel.reservations.PriceCalculatorTest` | 21 | 0 | 0 |
| `hotel.reservations.ReservationServiceTest` | 21 | 0 | 0 |
| `hotel.web.HttpApiTest` | 9 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 5
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 1
- contra o banco (DataSource, JDBC, Testcontainers): 4
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
21 testes  src/test/java/hotel/reservations/ReservationServiceTest.java
21 testes  src/test/java/hotel/reservations/PriceCalculatorTest.java
9 testes  src/test/java/hotel/web/HttpApiTest.java
5 testes  src/test/java/hotel/reports/ReportsTest.java
0 testes  src/test/java/hotel/EmptyDatabase.java
```
