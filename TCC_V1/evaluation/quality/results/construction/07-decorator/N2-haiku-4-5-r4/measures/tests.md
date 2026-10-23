# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 22 testes rodaram (0 falhas, 0 erros, 0 pulados); 22 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 47 de 739 (6.4%)
- ramos (if/switch): 39 de 199 (19.6%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `com.pousadamarealta` | 0 de 38 | 0 de 8 |
| `com.pousadamarealta.db` | 0 de 60 | 0 de 6 |
| `com.pousadamarealta.handler` | 0 de 208 | 0 de 43 |
| `com.pousadamarealta.model` | 0 de 46 | 0 de 0 |
| `com.pousadamarealta.service` | 0 de 338 | 0 de 102 |
| `com.pousadamarealta.util` | 47 de 49 | 39 de 40 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `com.pousadamarealta.util.EmailValidatorTest` | 10 | 0 | 0 |
| `com.pousadamarealta.util.PricingTest` | 12 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 2
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
12 testes  src/test/java/com/pousadamarealta/util/PricingTest.java
10 testes  src/test/java/com/pousadamarealta/util/EmailValidatorTest.java
```
