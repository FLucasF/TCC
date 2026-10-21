# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 29 testes rodaram (0 falhas, 0 erros, 0 pulados); 29 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 541 de 574 (94.3%)
- ramos (if/switch): 126 de 152 (82.9%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `hotel` | 27 de 34 | 2 de 4 |
| `hotel.documents` | 85 de 87 | 12 de 16 |
| `hotel.messages` | 29 de 33 | 2 de 2 |
| `hotel.reports` | 48 de 48 | 22 de 22 |
| `hotel.reservations` | 210 de 220 | 55 de 56 |
| `hotel.rooms` | 25 de 29 | 4 de 4 |
| `hotel.web` | 117 de 123 | 29 de 48 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `hotel.documents.DocumentsByGuestTest` | 3 | 0 | 0 |
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
6 testes  src/test/java/hotel/reservations/PriceCalculatorTest.java
5 testes  src/test/java/hotel/reports/ReportsTest.java
4 testes  src/test/java/hotel/web/HttpApiTest.java
3 testes  src/test/java/hotel/documents/DocumentsByGuestTest.java
0 testes  src/test/java/hotel/EmptyDatabase.java
```
