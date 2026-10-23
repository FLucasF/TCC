# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 39 testes rodaram (0 falhas, 0 erros, 0 pulados); 39 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 535 de 566 (94.5%)
- ramos (if/switch): 123 de 140 (87.9%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `hotel` | 33 de 38 | 4 de 6 |
| `hotel.documents` | 30 de 30 | 4 de 4 |
| `hotel.messages` | 85 de 89 | 22 de 24 |
| `hotel.reports` | 48 de 48 | 22 de 22 |
| `hotel.reservations` | 209 de 222 | 49 de 52 |
| `hotel.rooms` | 25 de 29 | 4 de 4 |
| `hotel.web` | 105 de 110 | 18 de 28 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `hotel.messages.EmailFiltersTest` | 7 | 0 | 0 |
| `hotel.messages.EmailRulesTest` | 4 | 0 | 0 |
| `hotel.messages.TestModeIntegrationTest` | 2 | 0 | 0 |
| `hotel.reports.ReportsTest` | 5 | 0 | 0 |
| `hotel.reservations.PriceCalculatorTest` | 6 | 0 | 0 |
| `hotel.reservations.ReservationServiceTest` | 11 | 0 | 0 |
| `hotel.web.HttpApiTest` | 4 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 8
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 1
- contra o banco (DataSource, JDBC, Testcontainers): 7
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
11 testes  src/test/java/hotel/reservations/ReservationServiceTest.java
7 testes  src/test/java/hotel/messages/EmailFiltersTest.java
6 testes  src/test/java/hotel/reservations/PriceCalculatorTest.java
5 testes  src/test/java/hotel/reports/ReportsTest.java
4 testes  src/test/java/hotel/web/HttpApiTest.java
4 testes  src/test/java/hotel/messages/EmailRulesTest.java
2 testes  src/test/java/hotel/messages/TestModeIntegrationTest.java
0 testes  src/test/java/hotel/EmptyDatabase.java
```
