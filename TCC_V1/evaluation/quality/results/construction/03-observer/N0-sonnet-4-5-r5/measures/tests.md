# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 7 testes rodaram (0 falhas, 0 erros, 0 pulados); 7 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 38 de 785 (4.8%)
- ramos (if/switch): 14 de 120 (11.7%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `com.pousadamarealta` | 0 de 53 | 0 de 2 |
| `com.pousadamarealta.controller` | 0 de 153 | 0 de 16 |
| `com.pousadamarealta.database` | 0 de 32 | 0 de 2 |
| `com.pousadamarealta.domain` | 12 de 116 | 0 de 0 |
| `com.pousadamarealta.event` | 0 de 20 | 0 de 4 |
| `com.pousadamarealta.event.listeners` | 0 de 88 | 0 de 18 |
| `com.pousadamarealta.exception` | 0 de 12 | 0 de 0 |
| `com.pousadamarealta.repository` | 0 de 163 | 0 de 24 |
| `com.pousadamarealta.service` | 26 de 148 | 14 de 54 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `com.pousadamarealta.PricingServiceTest` | 6 | 0 | 0 |
| `com.pousadamarealta.ReservationIT` | 1 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 2
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
6 testes  src/test/java/com/pousadamarealta/PricingServiceTest.java
1 testes  src/test/java/com/pousadamarealta/ReservationIT.java
```
