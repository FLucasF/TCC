# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 7 testes rodaram (4 falhas, 0 erros, 0 pulados); 7 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 441 de 802 (55.0%)
- ramos (if/switch): 59 de 148 (39.9%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `com.pousadamarealta` | 0 de 22 | 0 de 0 |
| `com.pousadamarealta.db` | 25 de 29 | 3 de 6 |
| `com.pousadamarealta.dto` | 0 de 1 | 0 de 0 |
| `com.pousadamarealta.exception` | 0 de 12 | 0 de 0 |
| `com.pousadamarealta.model` | 77 de 102 | 0 de 0 |
| `com.pousadamarealta.repository` | 113 de 190 | 20 de 48 |
| `com.pousadamarealta.service` | 123 de 255 | 26 de 74 |
| `com.pousadamarealta.web` | 103 de 191 | 10 de 20 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `com.pousadamarealta.IntegrationTest` | 6 | 4 | 0 |
| `com.pousadamarealta.SimpleTest` | 1 | 0 | 0 |

## Os testes que falharam

```
[ERROR]   IntegrationTest.testCreateAndConfirmReservation:117 expected: <201> but was: <500>
[ERROR]   IntegrationTest.testHighSeasonPricing:161 expected: <201> but was: <500>
[ERROR]   IntegrationTest.testNonRefundableDiscount:189 expected: <201> but was: <500>
[ERROR]   IntegrationTest.testRoomConflict:217 expected: <201> but was: <500>
```

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 2
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 1
- contra o banco (DataSource, JDBC, Testcontainers): 2
- com H2 ou outro banco em memória: 2
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
6 testes  src/test/java/com/pousadamarealta/IntegrationTest.java
1 testes  src/test/java/com/pousadamarealta/SimpleTest.java
```
