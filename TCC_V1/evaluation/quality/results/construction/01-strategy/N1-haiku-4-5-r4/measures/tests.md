# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 6 testes rodaram (0 falhas, 0 erros, 0 pulados); 6 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 80 de 919 (8.7%)
- ramos (if/switch): 24 de 203 (11.8%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `com.pousada.marealta` | 0 de 43 | 0 de 6 |
| `com.pousada.marealta.handler` | 0 de 260 | 0 de 70 |
| `com.pousada.marealta.model` | 18 de 160 | 0 de 0 |
| `com.pousada.marealta.persistence` | 0 de 208 | 0 de 36 |
| `com.pousada.marealta.service` | 54 de 218 | 19 de 79 |
| `com.pousada.marealta.util` | 8 de 30 | 5 de 12 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `com.pousada.marealta.service.PricingServiceTest` | 6 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 1
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
6 testes  src/test/java/com/pousada/marealta/service/PricingServiceTest.java
```
