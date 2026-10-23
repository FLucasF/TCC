# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 18 testes rodaram (10 falhas, 0 erros, 0 pulados); 15 métodos de teste escritos no código.

O Maven saiu com 1. Os erros do log:
```
[ERROR] Tests run: 3, Failures: 1, Errors: 0, Skipped: 0, Time elapsed: 18.31 s <<< FAILURE! -- in com.pousadamarealta.TestModeIT
[ERROR] com.pousadamarealta.TestModeIT.testModeStillBlocksInvalidEmails -- Time elapsed: 0.322 s <<< FAILURE!
[ERROR] Surefire is going to kill self fork JVM. The exit has elapsed 30 seconds after System.exit(0).
[ERROR] Failures: 
[ERROR]   TestModeIT.testModeStillBlocksInvalidEmails:226 1 expectation failed.
[ERROR] Tests run: 3, Failures: 1, Errors: 0, Skipped: 0
[ERROR] There are test failures.
[ERROR] Tests run: 12, Failures: 8, Errors: 0, Skipped: 0, Time elapsed: 13.05 s <<< FAILURE! -- in com.pousadamarealta.ReservationSystemIT
[ERROR] com.pousadamarealta.ReservationSystemIT.testCreateReservationLowSeason -- Time elapsed: 0.502 s <<< FAILURE!
[ERROR] com.pousadamarealta.ReservationSystemIT.testCreateReservationHighSeason -- Time elapsed: 0.026 s <<< FAILURE!
[ERROR] com.pousadamarealta.ReservationSystemIT.testCreateReservationNonRefundable -- Time elapsed: 0.019 s <<< FAILURE!
[ERROR] com.pousadamarealta.ReservationSystemIT.testRoomUnavailable -- Time elapsed: 0.017 s <<< FAILURE!
[ERROR] com.pousadamarealta.ReservationSystemIT.testConfirmReservationSendsEmail -- Time elapsed: 0.024 s <<< FAILURE!
[ERROR] com.pousadamarealta.ReservationSystemIT.testEmailValidation -- Time elapsed: 0.017 s <<< FAILURE!
[ERROR] com.pousadamarealta.ReservationSystemIT.testCancelReservationStandard -- Time elapsed: 0.015 s <<< FAILURE!
[ERROR] com.pousadamarealta.ReservationSystemIT.testCheckInAndCheckOut -- Time elapsed: 0.016 s <<< FAILURE!
[ERROR] Tests run: 3, Failures: 1, Errors: 0, Skipped: 0, Time elapsed: 0.691 s <<< FAILURE! -- in com.pousadamarealta.TestModeIT
[ERROR] com.pousadamarealta.TestModeIT.testModeStillBlocksInvalidEmails -- Time elapsed: 0.168 s <<< FAILURE!
[ERROR] Surefire is going to kill self fork JVM. The exit has elapsed 30 seconds after System.exit(0).
[ERROR] Failures: 
[ERROR]   ReservationSystemIT.testCancelReservationStandard:393 1 expectation failed.
[ERROR]   ReservationSystemIT.testCheckInAndCheckOut:432 1 expectation failed.
[ERROR]   ReservationSystemIT.testConfirmReservationSendsEmail:305 1 expectation failed.
[ERROR]   ReservationSystemIT.testCreateReservationHighSeason:179 1 expectation failed.
[ERROR]   ReservationSystemIT.testCreateReservationLowSeason:146 1 expectation failed.
[ERROR]   ReservationSystemIT.testCreateReservationNonRefundable:206 1 expectation failed.
[ERROR]   ReservationSystemIT.testEmailValidation:353 1 expectation failed.
[ERROR]   ReservationSystemIT.testRoomUnavailable:233 1 expectation failed.
[ERROR]   TestModeIT.testModeStillBlocksInvalidEmails:226 1 expectation failed.
[ERROR] Tests run: 15, Failures: 9, Errors: 0, Skipped: 0
```

## A cobertura (JaCoCo)

Sem relatório do JaCoCo: o agente do JaCoCo não entrou nos testes (por exemplo, um `argLine` no surefire sem `@{argLine}`), ou nenhum teste rodou.

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `com.pousadamarealta.ReservationSystemIT` | 12 | 8 | 0 |
| `com.pousadamarealta.TestModeIT` | 3 | 1 | 0 |
| `com.pousadamarealta.TestModeIT` | 3 | 1 | 0 |

## Os testes que falharam

```
[ERROR]   TestModeIT.testModeStillBlocksInvalidEmails:226 1 expectation failed.
[ERROR]   ReservationSystemIT.testCancelReservationStandard:393 1 expectation failed.
[ERROR]   ReservationSystemIT.testCheckInAndCheckOut:432 1 expectation failed.
[ERROR]   ReservationSystemIT.testConfirmReservationSendsEmail:305 1 expectation failed.
[ERROR]   ReservationSystemIT.testCreateReservationHighSeason:179 1 expectation failed.
[ERROR]   ReservationSystemIT.testCreateReservationLowSeason:146 1 expectation failed.
[ERROR]   ReservationSystemIT.testCreateReservationNonRefundable:206 1 expectation failed.
[ERROR]   ReservationSystemIT.testEmailValidation:353 1 expectation failed.
[ERROR]   ReservationSystemIT.testRoomUnavailable:233 1 expectation failed.
[ERROR]   TestModeIT.testModeStillBlocksInvalidEmails:226 1 expectation failed.
```

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 2
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 2
- contra o banco (DataSource, JDBC, Testcontainers): 2
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
12 testes  src/test/java/com/pousadamarealta/ReservationSystemIT.java
3 testes  src/test/java/com/pousadamarealta/TestModeIT.java
```
