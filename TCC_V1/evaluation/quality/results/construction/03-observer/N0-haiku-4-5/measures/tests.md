# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 7 testes rodaram (0 falhas, 0 erros, 0 pulados); 7 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 58 de 638 (9.1%)
- ramos (if/switch): 21 de 122 (17.2%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `com.pousadamarealta` | 28 de 477 | 7 de 90 |
| `com.pousadamarealta.api` | 0 de 25 | 0 de 0 |
| `com.pousadamarealta.exceptions` | 0 de 9 | 0 de 0 |
| `com.pousadamarealta.models` | 8 de 37 | 0 de 0 |
| `com.pousadamarealta.notifications` | 0 de 48 | 0 de 14 |
| `com.pousadamarealta.utils` | 22 de 42 | 14 de 18 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `com.pousadamarealta.IntegrationTest` | 1 | 0 | 0 |
| `com.pousadamarealta.PriceCalculatorTest` | 6 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 2
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 1
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
6 testes  src/test/java/com/pousadamarealta/PriceCalculatorTest.java
1 testes  src/test/java/com/pousadamarealta/IntegrationTest.java
```
