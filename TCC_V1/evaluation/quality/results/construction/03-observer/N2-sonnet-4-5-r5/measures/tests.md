# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 18 testes rodaram (0 falhas, 0 erros, 0 pulados); 18 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 98 de 803 (12.2%)
- ramos (if/switch): 30 de 142 (21.1%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `br.com.pousadamarealta` | 0 de 38 | 0 de 0 |
| `br.com.pousadamarealta.api` | 0 de 168 | 0 de 20 |
| `br.com.pousadamarealta.api.dto` | 0 de 45 | 0 de 0 |
| `br.com.pousadamarealta.domain` | 98 de 151 | 30 de 42 |
| `br.com.pousadamarealta.domain.events` | 0 de 2 | 0 de 0 |
| `br.com.pousadamarealta.listeners` | 0 de 83 | 0 de 10 |
| `br.com.pousadamarealta.repository.postgres` | 0 de 232 | 0 de 54 |
| `br.com.pousadamarealta.service` | 0 de 84 | 0 de 16 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `br.com.pousadamarealta.PriceCalculatorTest` | 8 | 0 | 0 |
| `br.com.pousadamarealta.ReservationTest` | 10 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 2
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
10 testes  src/test/java/br/com/pousadamarealta/ReservationTest.java
8 testes  src/test/java/br/com/pousadamarealta/PriceCalculatorTest.java
```
