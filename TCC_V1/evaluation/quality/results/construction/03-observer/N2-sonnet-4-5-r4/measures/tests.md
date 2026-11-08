# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 15 testes rodaram (0 falhas, 0 erros, 0 pulados); 15 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 493 de 813 (60.6%)
- ramos (if/switch): 85 de 172 (49.4%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `br.com.pousadamarealta` | 0 de 36 | 0 de 8 |
| `br.com.pousadamarealta.api` | 0 de 165 | 0 de 28 |
| `br.com.pousadamarealta.api.dto` | 0 de 43 | 0 de 0 |
| `br.com.pousadamarealta.config` | 0 de 6 | 0 de 0 |
| `br.com.pousadamarealta.domain` | 148 de 165 | 22 de 48 |
| `br.com.pousadamarealta.events` | 22 de 22 | 2 de 2 |
| `br.com.pousadamarealta.events.listeners` | 74 de 74 | 15 de 18 |
| `br.com.pousadamarealta.repository` | 173 de 198 | 27 de 38 |
| `br.com.pousadamarealta.service` | 76 de 104 | 19 de 30 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `br.com.pousadamarealta.PricingTest` | 6 | 0 | 0 |
| `br.com.pousadamarealta.ReportServiceIT` | 3 | 0 | 0 |
| `br.com.pousadamarealta.ReservationFlowIT` | 6 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 3
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 2
- com H2 ou outro banco em memória: 2
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
6 testes  src/test/java/br/com/pousadamarealta/ReservationFlowIT.java
6 testes  src/test/java/br/com/pousadamarealta/PricingTest.java
3 testes  src/test/java/br/com/pousadamarealta/ReportServiceIT.java
```
