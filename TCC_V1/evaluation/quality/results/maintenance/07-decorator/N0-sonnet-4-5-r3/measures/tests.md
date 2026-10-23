# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 40 testes rodaram (0 falhas, 0 erros, 0 pulados); 40 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 516 de 548 (94.2%)
- ramos (if/switch): 123 de 138 (89.1%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `hotel` | 26 de 32 | 2 de 4 |
| `hotel.documents` | 30 de 30 | 4 de 4 |
| `hotel.messages` | 73 de 77 | 24 de 24 |
| `hotel.reports` | 48 de 48 | 22 de 22 |
| `hotel.reservations` | 209 de 222 | 49 de 52 |
| `hotel.rooms` | 25 de 29 | 4 de 4 |
| `hotel.web` | 105 de 110 | 18 de 28 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `hotel.messages.EmailSenderIntegrationTest` | 4 | 0 | 0 |
| `hotel.messages.EmailValidatorTest` | 2 | 0 | 0 |
| `hotel.messages.MessageProcessorTest` | 4 | 0 | 0 |
| `hotel.reports.ReportsTest` | 5 | 0 | 0 |
| `hotel.reservations.PriceCalculatorTest` | 6 | 0 | 0 |
| `hotel.reservations.ReservationServiceTest` | 11 | 0 | 0 |
| `hotel.web.HttpApiTest` | 4 | 0 | 0 |
| `hotel.web.MessageApiTest` | 4 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 9
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 2
- contra o banco (DataSource, JDBC, Testcontainers): 6
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
11 testes  src/test/java/hotel/reservations/ReservationServiceTest.java
6 testes  src/test/java/hotel/reservations/PriceCalculatorTest.java
5 testes  src/test/java/hotel/reports/ReportsTest.java
4 testes  src/test/java/hotel/web/MessageApiTest.java
4 testes  src/test/java/hotel/web/HttpApiTest.java
4 testes  src/test/java/hotel/messages/MessageProcessorTest.java
4 testes  src/test/java/hotel/messages/EmailSenderIntegrationTest.java
2 testes  src/test/java/hotel/messages/EmailValidatorTest.java
0 testes  src/test/java/hotel/EmptyDatabase.java
```
