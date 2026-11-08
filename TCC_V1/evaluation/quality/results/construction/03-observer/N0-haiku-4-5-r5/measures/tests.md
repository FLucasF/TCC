# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 6 testes rodaram (0 falhas, 0 erros, 0 pulados); 6 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 39 de 917 (4.3%)
- ramos (if/switch): 14 de 178 (7.9%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `com.pousada.marealta` | 0 de 12 | 0 de 0 |
| `com.pousada.marealta.config` | 0 de 20 | 0 de 10 |
| `com.pousada.marealta.controller` | 0 de 326 | 0 de 44 |
| `com.pousada.marealta.domain` | 11 de 110 | 0 de 0 |
| `com.pousada.marealta.notification` | 0 de 79 | 0 de 16 |
| `com.pousada.marealta.repository` | 0 de 154 | 0 de 50 |
| `com.pousada.marealta.service` | 27 de 187 | 14 de 52 |
| `com.pousada.marealta.util` | 1 de 29 | 0 de 6 |

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
