# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 6 testes rodaram (0 falhas, 0 erros, 0 pulados); 6 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 77 de 795 (9.7%)
- ramos (if/switch): 15 de 132 (11.4%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `com.pousadamarealta` | 0 de 41 | 0 de 0 |
| `com.pousadamarealta.controller` | 0 de 151 | 0 de 24 |
| `com.pousadamarealta.dto` | 0 de 43 | 0 de 0 |
| `com.pousadamarealta.model` | 23 de 110 | 0 de 0 |
| `com.pousadamarealta.repository` | 0 de 203 | 0 de 32 |
| `com.pousadamarealta.service` | 54 de 247 | 15 de 76 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `com.pousadamarealta.PricingServiceTest` | 6 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 1
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
6 testes  src/test/java/com/pousadamarealta/PricingServiceTest.java
```
