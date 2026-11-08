# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 13 testes rodaram (0 falhas, 0 erros, 0 pulados); 13 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 21 de 708 (3.0%)
- ramos (if/switch): 14 de 170 (8.2%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `com.pousadamarealta` | 0 de 26 | 0 de 8 |
| `com.pousadamarealta.api` | 0 de 218 | 0 de 28 |
| `com.pousadamarealta.database` | 0 de 31 | 0 de 6 |
| `com.pousadamarealta.exceptions` | 0 de 6 | 0 de 0 |
| `com.pousadamarealta.models` | 2 de 45 | 0 de 0 |
| `com.pousadamarealta.repositories` | 0 de 164 | 0 de 38 |
| `com.pousadamarealta.services` | 19 de 218 | 14 de 90 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `com.pousadamarealta.services.PricingServiceTest` | 13 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 1
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
13 testes  src/test/java/com/pousadamarealta/services/PricingServiceTest.java
```
