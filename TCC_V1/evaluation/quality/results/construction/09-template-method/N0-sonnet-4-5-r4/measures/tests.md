# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 3 testes rodaram (0 falhas, 0 erros, 0 pulados); 3 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 14 de 911 (1.5%)
- ramos (if/switch): 11 de 152 (7.2%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `com.pousadamarealta` | 0 de 38 | 0 de 0 |
| `com.pousadamarealta.database` | 0 de 27 | 0 de 2 |
| `com.pousadamarealta.dto` | 0 de 35 | 0 de 0 |
| `com.pousadamarealta.handler` | 0 de 253 | 0 de 26 |
| `com.pousadamarealta.model` | 0 de 108 | 0 de 0 |
| `com.pousadamarealta.repository` | 0 de 194 | 0 de 36 |
| `com.pousadamarealta.service` | 14 de 256 | 11 de 88 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `com.pousadamarealta.PricingServiceTest` | 3 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 1
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
3 testes  src/test/java/com/pousadamarealta/PricingServiceTest.java
```
