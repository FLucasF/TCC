# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 9 testes rodaram (0 falhas, 0 erros, 0 pulados); 9 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 29 de 643 (4.5%)
- ramos (if/switch): 26 de 174 (14.9%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `com.pousada.marealta` | 0 de 230 | 0 de 26 |
| `com.pousada.marealta.api` | 0 de 4 | 0 de 0 |
| `com.pousada.marealta.db` | 0 de 172 | 0 de 58 |
| `com.pousada.marealta.model` | 0 de 54 | 0 de 0 |
| `com.pousada.marealta.service` | 0 de 134 | 0 de 58 |
| `com.pousada.marealta.util` | 29 de 49 | 26 de 32 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `com.pousada.marealta.util.EmailValidatorTest` | 2 | 0 | 0 |
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
7 testes  src/test/java/com/pousada/marealta/util/PriceCalculatorTest.java
2 testes  src/test/java/com/pousada/marealta/util/EmailValidatorTest.java
```
