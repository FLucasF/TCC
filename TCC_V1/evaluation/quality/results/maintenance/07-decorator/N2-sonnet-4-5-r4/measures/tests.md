# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 34 testes rodaram (0 falhas, 0 erros, 0 pulados); 34 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 550 de 582 (94.5%)
- ramos (if/switch): 128 de 146 (87.7%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `hotel` | 31 de 37 | 3 de 6 |
| `hotel.documents` | 30 de 30 | 4 de 4 |
| `hotel.messages` | 102 de 106 | 28 de 30 |
| `hotel.reports` | 48 de 48 | 22 de 22 |
| `hotel.reservations` | 209 de 222 | 49 de 52 |
| `hotel.rooms` | 25 de 29 | 4 de 4 |
| `hotel.web` | 105 de 110 | 18 de 28 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `hotel.messages.MessageRulesTest` | 8 | 0 | 0 |
| `hotel.reports.ReportsTest` | 5 | 0 | 0 |
| `hotel.reservations.PriceCalculatorTest` | 6 | 0 | 0 |
| `hotel.reservations.ReservationServiceTest` | 11 | 0 | 0 |
| `hotel.web.HttpApiTest` | 4 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 6
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 1
- contra o banco (DataSource, JDBC, Testcontainers): 5
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
11 testes  src/test/java/hotel/reservations/ReservationServiceTest.java
8 testes  src/test/java/hotel/messages/MessageRulesTest.java
6 testes  src/test/java/hotel/reservations/PriceCalculatorTest.java
5 testes  src/test/java/hotel/reports/ReportsTest.java
4 testes  src/test/java/hotel/web/HttpApiTest.java
0 testes  src/test/java/hotel/EmptyDatabase.java
```
