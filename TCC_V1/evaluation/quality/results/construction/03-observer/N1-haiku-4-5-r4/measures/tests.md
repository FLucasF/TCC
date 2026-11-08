# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 9 testes rodaram (0 falhas, 0 erros, 0 pulados); 9 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 36 de 622 (5.8%)
- ramos (if/switch): 15 de 136 (11.0%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `com.pousadamarealta` | 0 de 26 | 0 de 2 |
| `com.pousadamarealta.database` | 0 de 32 | 0 de 6 |
| `com.pousadamarealta.models` | 0 de 51 | 0 de 0 |
| `com.pousadamarealta.notifications` | 0 de 46 | 0 de 8 |
| `com.pousadamarealta.repository` | 0 de 143 | 0 de 42 |
| `com.pousadamarealta.routes` | 0 de 153 | 0 de 20 |
| `com.pousadamarealta.service` | 0 de 114 | 0 de 34 |
| `com.pousadamarealta.utils` | 36 de 57 | 15 de 24 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `com.pousadamarealta.FormatterUtilsTest` | 3 | 0 | 0 |
| `com.pousadamarealta.PricingUtilsTest` | 6 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 2
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
6 testes  src/test/java/com/pousadamarealta/PricingUtilsTest.java
3 testes  src/test/java/com/pousadamarealta/FormatterUtilsTest.java
```
