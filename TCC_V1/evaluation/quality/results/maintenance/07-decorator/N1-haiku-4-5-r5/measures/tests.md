# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 47 testes rodaram (0 falhas, 0 erros, 0 pulados); 47 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 524 de 558 (93.9%)
- ramos (if/switch): 128 de 144 (88.9%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `hotel` | 28 de 33 | 2 de 4 |
| `hotel.documents` | 30 de 30 | 4 de 4 |
| `hotel.messages` | 79 de 86 | 29 de 30 |
| `hotel.reports` | 48 de 48 | 22 de 22 |
| `hotel.reservations` | 209 de 222 | 49 de 52 |
| `hotel.rooms` | 25 de 29 | 4 de 4 |
| `hotel.web` | 105 de 110 | 18 de 28 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `hotel.messages.EmailValidatorTest` | 9 | 0 | 0 |
| `hotel.messages.MessageRulesTest` | 4 | 0 | 0 |
| `hotel.messages.SentMessagesIntegrationTest` | 4 | 0 | 0 |
| `hotel.reports.ReportsTest` | 5 | 0 | 0 |
| `hotel.reservations.PriceCalculatorTest` | 6 | 0 | 0 |
| `hotel.reservations.ReservationServiceTest` | 13 | 0 | 0 |
| `hotel.web.HttpApiTest` | 4 | 0 | 0 |
| `hotel.web.InvalidEmailHttpApiTest` | 1 | 0 | 0 |
| `hotel.web.TestModeHttpApiTest` | 1 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 10
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 3
- contra o banco (DataSource, JDBC, Testcontainers): 7
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
13 testes  src/test/java/hotel/reservations/ReservationServiceTest.java
9 testes  src/test/java/hotel/messages/EmailValidatorTest.java
6 testes  src/test/java/hotel/reservations/PriceCalculatorTest.java
5 testes  src/test/java/hotel/reports/ReportsTest.java
4 testes  src/test/java/hotel/web/HttpApiTest.java
4 testes  src/test/java/hotel/messages/SentMessagesIntegrationTest.java
4 testes  src/test/java/hotel/messages/MessageRulesTest.java
1 testes  src/test/java/hotel/web/TestModeHttpApiTest.java
1 testes  src/test/java/hotel/web/InvalidEmailHttpApiTest.java
0 testes  src/test/java/hotel/EmptyDatabase.java
```
