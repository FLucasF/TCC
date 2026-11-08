# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 11 testes rodaram (1 falhas, 0 erros, 0 pulados); 11 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 409 de 807 (50.7%)
- ramos (if/switch): 61 de 164 (37.2%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `com.pousadamarealta` | 0 de 84 | 0 de 8 |
| `com.pousadamarealta.api` | 0 de 139 | 0 de 22 |
| `com.pousadamarealta.api.dto` | 0 de 2 | 0 de 0 |
| `com.pousadamarealta.domain` | 166 de 184 | 23 de 44 |
| `com.pousadamarealta.domain.events` | 12 de 12 | 3 de 4 |
| `com.pousadamarealta.infrastructure` | 13 de 16 | 2 de 4 |
| `com.pousadamarealta.repository` | 125 de 200 | 19 de 54 |
| `com.pousadamarealta.service` | 38 de 115 | 6 de 18 |
| `com.pousadamarealta.service.listeners` | 55 de 55 | 8 de 10 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `com.pousadamarealta.domain.PriceCalculatorTest` | 7 | 0 | 0 |
| `com.pousadamarealta.integration.ReservationFlowIT` | 4 | 1 | 0 |

## Os testes que falharam

```
[ERROR]   ReservationFlowIT.shouldCancelNonRefundableWithoutRefund:168 
```

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 2
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 1
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
7 testes  src/test/java/com/pousadamarealta/domain/PriceCalculatorTest.java
4 testes  src/test/java/com/pousadamarealta/integration/ReservationFlowIT.java
```
