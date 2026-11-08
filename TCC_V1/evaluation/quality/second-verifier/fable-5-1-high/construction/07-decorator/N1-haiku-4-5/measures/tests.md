# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 5 testes rodaram (0 falhas, 0 erros, 0 pulados); 0 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 27 de 854 (3.2%)
- ramos (if/switch): 24 de 230 (10.4%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `com.pousada.marealta` | 0 de 29 | 0 de 8 |
| `com.pousada.marealta.api` | 0 de 246 | 0 de 46 |
| `com.pousada.marealta.domain` | 0 de 110 | 0 de 30 |
| `com.pousada.marealta.infrastructure.messaging` | 0 de 53 | 0 de 14 |
| `com.pousada.marealta.infrastructure.persistence` | 0 de 217 | 0 de 58 |
| `com.pousada.marealta.service` | 0 de 156 | 0 de 48 |
| `com.pousada.marealta.util` | 27 de 43 | 24 de 26 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `com.pousada.marealta.util.EmailValidatorTest` | 2 | 0 | 0 |
| `com.pousada.marealta.util.MoneyRounderTest` | 1 | 0 | 0 |
| `com.pousada.marealta.util.SeasonCalculatorTest` | 2 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 3
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
0 testes  src/test/java/com/pousada/marealta/util/SeasonCalculatorTest.java
0 testes  src/test/java/com/pousada/marealta/util/MoneyRounderTest.java
0 testes  src/test/java/com/pousada/marealta/util/EmailValidatorTest.java
```
