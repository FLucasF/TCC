# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 18 testes rodaram (0 falhas, 0 erros, 0 pulados); 18 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 472 de 523 (90.2%)
- ramos (if/switch): 103 de 126 (81.7%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `br.com.pousadamarealta.reservas` | 13 de 29 | 0 de 8 |
| `br.com.pousadamarealta.reservas.domain` | 60 de 60 | 34 de 36 |
| `br.com.pousadamarealta.reservas.events` | 10 de 10 | 2 de 2 |
| `br.com.pousadamarealta.reservas.notification` | 61 de 61 | 16 de 18 |
| `br.com.pousadamarealta.reservas.persistence` | 155 de 185 | 19 de 22 |
| `br.com.pousadamarealta.reservas.service` | 85 de 85 | 12 de 12 |
| `br.com.pousadamarealta.reservas.web` | 88 de 93 | 20 de 28 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `br.com.pousadamarealta.reservas.ApiTest` | 12 | 0 | 0 |
| `br.com.pousadamarealta.reservas.PricingTest` | 6 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 2
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 1
- contra o banco (DataSource, JDBC, Testcontainers): 1
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
12 testes  src/test/java/br/com/pousadamarealta/reservas/ApiTest.java
6 testes  src/test/java/br/com/pousadamarealta/reservas/PricingTest.java
```
