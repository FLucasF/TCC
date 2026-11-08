# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 22 testes rodaram (0 falhas, 12 erros, 0 pulados); 22 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 28 de 1041 (2.7%)
- ramos (if/switch): 15 de 152 (9.9%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `com.pousadamarealta` | 0 de 56 | 0 de 0 |
| `com.pousadamarealta.config` | 0 de 9 | 0 de 0 |
| `com.pousadamarealta.domain.events` | 0 de 10 | 0 de 0 |
| `com.pousadamarealta.domain.model` | 0 de 106 | 0 de 8 |
| `com.pousadamarealta.repository` | 0 de 178 | 0 de 36 |
| `com.pousadamarealta.service` | 0 de 143 | 0 de 34 |
| `com.pousadamarealta.service.listeners` | 0 de 84 | 0 de 14 |
| `com.pousadamarealta.util` | 28 de 31 | 15 de 16 |
| `com.pousadamarealta.web.controller` | 0 de 216 | 0 de 38 |
| `com.pousadamarealta.web.dto` | 0 de 157 | 0 de 0 |
| `com.pousadamarealta.web.mapper` | 0 de 51 | 0 de 6 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `com.pousadamarealta.FormatterTest` | 3 | 0 | 0 |
| `com.pousadamarealta.PriceCalculatorTest` | 7 | 0 | 0 |
| `com.pousadamarealta.ReservationSystemIT` | 12 | 12 | 0 |

## Os testes que falharam

```
[ERROR]   ReservationSystemIT.testCancelReservationWithRefund:288 » Connect
[ERROR]   ReservationSystemIT.testCapacityExceeded:224 » Connect
[ERROR]   ReservationSystemIT.testCheckIn:240 » Connect
[ERROR]   ReservationSystemIT.testCheckOut:256 » Connect
[ERROR]   ReservationSystemIT.testConfirmReservation:121 » Connect
[ERROR]   ReservationSystemIT.testCreateReservation:66 » Connect
[ERROR]   ReservationSystemIT.testCreateReservationWithDiscount:102 » Connect
[ERROR]   ReservationSystemIT.testGetRooms:31 » Connect
[ERROR]   ReservationSystemIT.testMessagesAfterConfirmation:138 » Connect
[ERROR]   ReservationSystemIT.testOccupancyReport:320 » Connect
[ERROR]   ReservationSystemIT.testRevenueReport:338 » Connect
[ERROR]   ReservationSystemIT.testRoomUnavailable:194 » Connect
```

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 3
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 1
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
12 testes  src/test/java/com/pousadamarealta/ReservationSystemIT.java
7 testes  src/test/java/com/pousadamarealta/PriceCalculatorTest.java
3 testes  src/test/java/com/pousadamarealta/FormatterTest.java
```
