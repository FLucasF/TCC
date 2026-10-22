# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 22 testes rodaram (0 falhas, 16 erros, 0 pulados); 22 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 128 de 712 (18.0%)
- ramos (if/switch): 17 de 97 (17.5%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `com.pousadamareaalta` | 17 de 23 | 0 de 0 |
| `com.pousadamareaalta.api` | 13 de 155 | 0 de 4 |
| `com.pousadamareaalta.api.dto` | 0 de 5 | 0 de 0 |
| `com.pousadamareaalta.exception` | 0 de 12 | 0 de 0 |
| `com.pousadamareaalta.model` | 9 de 117 | 0 de 0 |
| `com.pousadamareaalta.pricing` | 57 de 87 | 17 de 33 |
| `com.pousadamareaalta.repository` | 19 de 165 | 0 de 28 |
| `com.pousadamareaalta.service` | 13 de 148 | 0 de 32 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `com.pousadamareaalta.pricing.PricingTest` | 6 | 0 | 0 |
| `com.pousadamareaalta.ReservationSystemIT` | 16 | 16 | 0 |

## Os testes que falharam

```
[ERROR]   ReservationSystemIT.shouldApplyHighSeasonPricing:187->post:375 » Connect
[ERROR]   ReservationSystemIT.shouldCancelWeekendPackageWithPartialRefund:323->post:375 » Connect
[ERROR]   ReservationSystemIT.shouldCheckInAndCheckOut:308->post:375 » Connect
[ERROR]   ReservationSystemIT.shouldConfirmReservationAndSendEmail:290->post:375 » Connect
[ERROR]   ReservationSystemIT.shouldCreateCorporateReservation:111->post:375 » Connect
[ERROR]   ReservationSystemIT.shouldCreateLongStayReservation:161->post:375 » Connect
[ERROR]   ReservationSystemIT.shouldCreateNonRefundableReservation:85->post:375 » Connect
[ERROR]   ReservationSystemIT.shouldCreateStandardReservation:56->post:375 » Connect
[ERROR]   ReservationSystemIT.shouldCreateWeekendPackageReservation:136->post:375 » Connect
[ERROR]   ReservationSystemIT.shouldGenerateOccupancyReport:336->get:366 » Connect
[ERROR]   ReservationSystemIT.shouldGenerateRevenueReport:350->get:366 » Connect
[ERROR]   ReservationSystemIT.shouldGetAllRooms:29->get:366 » Connect
[ERROR]   ReservationSystemIT.shouldRejectCapacityExceeded:280->post:375 » Connect
[ERROR]   ReservationSystemIT.shouldRejectInvalidCorporateCode:211->post:375 » Connect
[ERROR]   ReservationSystemIT.shouldRejectLongStayUnder7Nights:257->post:375 » Connect
[ERROR]   ReservationSystemIT.shouldRejectWeekendPackageOnWrongDates:234->post:375 » Connect
```

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 3
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 1
- contra o banco (DataSource, JDBC, Testcontainers): 1
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 1

Os arquivos:
```
16 testes  src/test/java/com/pousadamareaalta/ReservationSystemIT.java
6 testes  src/test/java/com/pousadamareaalta/pricing/PricingTest.java
0 testes  src/test/java/com/pousadamareaalta/TestAppRunner.java
```
