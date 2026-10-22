# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 4 testes rodaram (0 falhas, 0 erros, 0 pulados); 4 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 33 de 803 (4.1%)
- ramos (if/switch): 9 de 124 (7.3%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `com.pousadamarealta` | 0 de 47 | 0 de 0 |
| `com.pousadamarealta.api` | 0 de 187 | 0 de 12 |
| `com.pousadamarealta.database` | 0 de 29 | 0 de 6 |
| `com.pousadamarealta.domain` | 14 de 98 | 0 de 0 |
| `com.pousadamarealta.events` | 0 de 101 | 0 de 18 |
| `com.pousadamarealta.repository` | 0 de 192 | 0 de 38 |
| `com.pousadamarealta.service` | 19 de 149 | 9 de 50 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `com.pousadamarealta.PricingServiceTest` | 4 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 1
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
4 testes  src/test/java/com/pousadamarealta/PricingServiceTest.java
```
