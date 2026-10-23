# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 5 testes rodaram (0 falhas, 0 erros, 0 pulados); 5 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 284 de 962 (29.5%)
- ramos (if/switch): 25 de 170 (14.7%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `com.pousada` | 0 de 163 | 0 de 24 |
| `com.pousada.api.dto` | 0 de 56 | 0 de 0 |
| `com.pousada.application` | 0 de 120 | 0 de 26 |
| `com.pousada.domain.guest` | 134 de 168 | 14 de 38 |
| `com.pousada.domain.model` | 113 de 189 | 0 de 30 |
| `com.pousada.domain.service` | 37 de 50 | 11 de 16 |
| `com.pousada.infrastructure` | 0 de 216 | 0 de 36 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `com.pousada.DocumentGenerationTest` | 3 | 0 | 0 |
| `com.pousada.PricingServiceTest` | 2 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 2
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
3 testes  src/test/java/com/pousada/DocumentGenerationTest.java
2 testes  src/test/java/com/pousada/PricingServiceTest.java
```
