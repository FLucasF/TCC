# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 12 testes rodaram (0 falhas, 0 erros, 0 pulados); 12 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 35 de 585 (6.0%)
- ramos (if/switch): 33 de 148 (22.3%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `com.pousadamarealta` | 0 de 7 | 0 de 2 |
| `com.pousadamarealta.api` | 0 de 214 | 0 de 46 |
| `com.pousadamarealta.database` | 0 de 30 | 0 de 8 |
| `com.pousadamarealta.model` | 0 de 32 | 0 de 0 |
| `com.pousadamarealta.service` | 0 de 258 | 0 de 58 |
| `com.pousadamarealta.util` | 35 de 44 | 33 de 34 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `com.pousadamarealta.util.PricingUtilTest` | 8 | 0 | 0 |
| `com.pousadamarealta.util.ValidationUtilTest` | 4 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 2
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
8 testes  src/test/java/com/pousadamarealta/util/PricingUtilTest.java
4 testes  src/test/java/com/pousadamarealta/util/ValidationUtilTest.java
```
