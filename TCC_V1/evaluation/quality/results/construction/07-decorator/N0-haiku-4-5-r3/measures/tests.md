# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 15 testes rodaram (0 falhas, 0 erros, 0 pulados); 15 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 55 de 664 (8.3%)
- ramos (if/switch): 35 de 176 (19.9%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `com.pousada.marealta` | 0 de 17 | 0 de 0 |
| `com.pousada.marealta.db` | 0 de 33 | 0 de 8 |
| `com.pousada.marealta.exception` | 0 de 4 | 0 de 0 |
| `com.pousada.marealta.model` | 0 de 34 | 0 de 0 |
| `com.pousada.marealta.routes` | 0 de 256 | 0 de 44 |
| `com.pousada.marealta.service` | 0 de 262 | 0 de 88 |
| `com.pousada.marealta.util` | 55 de 58 | 35 de 36 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `com.pousada.marealta.util.EmailValidatorTest` | 8 | 0 | 0 |
| `com.pousada.marealta.util.PriceCalculatorTest` | 7 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 2
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
8 testes  src/test/java/com/pousada/marealta/util/EmailValidatorTest.java
7 testes  src/test/java/com/pousada/marealta/util/PriceCalculatorTest.java
```
