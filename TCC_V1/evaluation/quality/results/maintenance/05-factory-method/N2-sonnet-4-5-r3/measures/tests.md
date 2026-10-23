# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 39 testes rodaram (0 falhas, 0 erros, 0 pulados); 39 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 619 de 649 (95.4%)
- ramos (if/switch): 152 de 167 (91.0%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `hotel` | 25 de 30 | 2 de 4 |
| `hotel.documents` | 109 de 109 | 15 de 15 |
| `hotel.messages` | 29 de 33 | 2 de 2 |
| `hotel.reports` | 48 de 48 | 22 de 22 |
| `hotel.reservations` | 243 de 256 | 58 de 62 |
| `hotel.rooms` | 25 de 29 | 4 de 4 |
| `hotel.web` | 140 de 144 | 49 de 58 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `hotel.documents.DocumentsTest` | 6 | 0 | 0 |
| `hotel.IntegrationTest` | 4 | 0 | 0 |
| `hotel.reports.ReportsTest` | 5 | 0 | 0 |
| `hotel.reservations.PriceCalculatorTest` | 6 | 0 | 0 |
| `hotel.reservations.ReservationServiceTest` | 11 | 0 | 0 |
| `hotel.web.HttpApiTest` | 7 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 7
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 1
- contra o banco (DataSource, JDBC, Testcontainers): 5
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
11 testes  src/test/java/hotel/reservations/ReservationServiceTest.java
7 testes  src/test/java/hotel/web/HttpApiTest.java
6 testes  src/test/java/hotel/reservations/PriceCalculatorTest.java
6 testes  src/test/java/hotel/documents/DocumentsTest.java
5 testes  src/test/java/hotel/reports/ReportsTest.java
4 testes  src/test/java/hotel/IntegrationTest.java
0 testes  src/test/java/hotel/EmptyDatabase.java
```
