# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 25 testes rodaram (0 falhas, 0 erros, 0 pulados); 25 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 77 de 588 (13.1%)
- ramos (if/switch): 44 de 171 (25.7%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `com.pousada.marealta` | 0 de 179 | 0 de 23 |
| `com.pousada.marealta.api` | 0 de 5 | 0 de 0 |
| `com.pousada.marealta.db` | 0 de 186 | 0 de 58 |
| `com.pousada.marealta.models` | 14 de 59 | 0 de 0 |
| `com.pousada.marealta.services` | 0 de 93 | 0 de 46 |
| `com.pousada.marealta.util` | 63 de 66 | 44 de 44 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `com.pousada.marealta.EmailValidatorTest` | 11 | 0 | 0 |
| `com.pousada.marealta.IntegrationTest` | 3 | 0 | 0 |
| `com.pousada.marealta.PricingTest` | 11 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 3
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
11 testes  src/test/java/com/pousada/marealta/PricingTest.java
11 testes  src/test/java/com/pousada/marealta/EmailValidatorTest.java
3 testes  src/test/java/com/pousada/marealta/IntegrationTest.java
```
