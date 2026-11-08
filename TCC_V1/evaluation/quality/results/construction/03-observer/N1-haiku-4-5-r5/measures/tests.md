# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 12 testes rodaram (0 falhas, 0 erros, 0 pulados); 12 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 44 de 756 (5.8%)
- ramos (if/switch): 16 de 191 (8.4%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `com.pousada.marealta` | 0 de 195 | 0 de 45 |
| `com.pousada.marealta.api` | 0 de 6 | 0 de 0 |
| `com.pousada.marealta.api.request` | 0 de 1 | 0 de 0 |
| `com.pousada.marealta.api.response` | 0 de 40 | 0 de 2 |
| `com.pousada.marealta.db` | 0 de 183 | 0 de 50 |
| `com.pousada.marealta.enums` | 3 de 18 | 0 de 0 |
| `com.pousada.marealta.model` | 0 de 85 | 0 de 0 |
| `com.pousada.marealta.notification` | 0 de 17 | 0 de 2 |
| `com.pousada.marealta.notification.notifiers` | 0 de 41 | 0 de 14 |
| `com.pousada.marealta.service` | 0 de 101 | 0 de 54 |
| `com.pousada.marealta.util` | 41 de 69 | 16 de 24 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `com.pousada.marealta.FormatterTest` | 6 | 0 | 0 |
| `com.pousada.marealta.PricingCalculatorTest` | 6 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 2
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
6 testes  src/test/java/com/pousada/marealta/PricingCalculatorTest.java
6 testes  src/test/java/com/pousada/marealta/FormatterTest.java
```
