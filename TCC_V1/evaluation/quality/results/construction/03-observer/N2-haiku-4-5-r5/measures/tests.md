# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 6 testes rodaram (0 falhas, 0 erros, 0 pulados); 6 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 12 de 731 (1.6%)
- ramos (if/switch): 10 de 147 (6.8%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `com.pousadamarealta` | 0 de 44 | 0 de 12 |
| `com.pousadamarealta.api` | 0 de 202 | 0 de 17 |
| `com.pousadamarealta.domain` | 0 de 75 | 0 de 16 |
| `com.pousadamarealta.infrastructure` | 12 de 213 | 10 de 36 |
| `com.pousadamarealta.services` | 0 de 197 | 0 de 66 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `com.pousadamarealta.PriceCalculatorTest` | 6 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 1
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
6 testes  src/test/java/com/pousadamarealta/PriceCalculatorTest.java
```
