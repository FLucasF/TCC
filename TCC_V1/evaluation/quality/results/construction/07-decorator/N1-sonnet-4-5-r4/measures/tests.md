# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 16 testes rodaram (0 falhas, 0 erros, 0 pulados); 16 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 471 de 807 (58.4%)
- ramos (if/switch): 57 de 124 (46.0%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `com.pousadamarealta` | 0 de 41 | 0 de 2 |
| `com.pousadamarealta.controller` | 59 de 194 | 3 de 12 |
| `com.pousadamarealta.database` | 20 de 24 | 1 de 2 |
| `com.pousadamarealta.messaging` | 69 de 78 | 19 de 26 |
| `com.pousadamarealta.model` | 157 de 166 | 14 de 16 |
| `com.pousadamarealta.repository` | 91 de 148 | 11 de 26 |
| `com.pousadamarealta.service` | 75 de 156 | 9 de 40 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `com.pousadamarealta.MessageRulesTest` | 7 | 0 | 0 |
| `com.pousadamarealta.PricingTest` | 6 | 0 | 0 |
| `com.pousadamarealta.ReservationIT` | 3 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 3
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 1
- contra o banco (DataSource, JDBC, Testcontainers): 1
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
7 testes  src/test/java/com/pousadamarealta/MessageRulesTest.java
6 testes  src/test/java/com/pousadamarealta/PricingTest.java
3 testes  src/test/java/com/pousadamarealta/ReservationIT.java
```
