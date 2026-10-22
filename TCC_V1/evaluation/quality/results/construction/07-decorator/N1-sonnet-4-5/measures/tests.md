# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 34 testes rodaram (0 falhas, 10 erros, 0 pulados); 34 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 151 de 823 (18.3%)
- ramos (if/switch): 44 de 158 (27.8%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `com.pousadamarealta` | 0 de 56 | 0 de 12 |
| `com.pousadamarealta.domain` | 85 de 162 | 28 de 44 |
| `com.pousadamarealta.messaging` | 66 de 69 | 16 de 20 |
| `com.pousadamarealta.repository` | 0 de 194 | 0 de 34 |
| `com.pousadamarealta.service` | 0 de 138 | 0 de 18 |
| `com.pousadamarealta.web` | 0 de 157 | 0 de 30 |
| `com.pousadamarealta.web.dto` | 0 de 47 | 0 de 0 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `com.pousadamarealta.domain.ReservationTest` | 16 | 0 | 0 |
| `com.pousadamarealta.messaging.MessageProcessorTest` | 8 | 0 | 0 |
| `com.pousadamarealta.ReservationSystemIT` | 10 | 10 | 0 |

## Os testes que falharam

```
[ERROR]   ReservationSystemIT.testCapacityExceeded:265 » Connect
[ERROR]   ReservationSystemIT.testConfirmReservation:178 » Connect
[ERROR]   ReservationSystemIT.testCreateReservation:126 » Connect
[ERROR]   ReservationSystemIT.testCreateReservationWithHighSeasonAndDiscount:160 » Connect
[ERROR]   ReservationSystemIT.testInvalidEmail:294 » Connect
[ERROR]   ReservationSystemIT.testListRooms:93 » Connect
[ERROR]   ReservationSystemIT.testMessagesAfterConfirmation:194 » Connect
[ERROR]   ReservationSystemIT.testOccupancyReport:325 » Connect
[ERROR]   ReservationSystemIT.testRevenueReport:345 » Connect
[ERROR]   ReservationSystemIT.testRoomUnavailable:236 » Connect
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
16 testes  src/test/java/com/pousadamarealta/domain/ReservationTest.java
10 testes  src/test/java/com/pousadamarealta/ReservationSystemIT.java
8 testes  src/test/java/com/pousadamarealta/messaging/MessageProcessorTest.java
```
