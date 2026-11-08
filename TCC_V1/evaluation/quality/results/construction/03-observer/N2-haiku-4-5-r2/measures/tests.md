# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 6 testes rodaram (0 falhas, 0 erros, 0 pulados); 6 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 25 de 829 (3.0%)
- ramos (if/switch): 14 de 191 (7.3%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `com.pousada.marealta` | 0 de 31 | 0 de 2 |
| `com.pousada.marealta.api` | 0 de 246 | 0 de 47 |
| `com.pousada.marealta.domain` | 0 de 99 | 0 de 0 |
| `com.pousada.marealta.notification` | 0 de 76 | 0 de 10 |
| `com.pousada.marealta.persistence` | 0 de 40 | 0 de 6 |
| `com.pousada.marealta.repository` | 0 de 160 | 0 de 50 |
| `com.pousada.marealta.service` | 25 de 177 | 14 de 76 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `com.pousada.marealta.service.PricingCalculatorTest` | 6 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 1
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
6 testes  src/test/java/com/pousada/marealta/service/PricingCalculatorTest.java
```
