# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 3 testes rodaram (0 falhas, 0 erros, 0 pulados); 3 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 28 de 807 (3.5%)
- ramos (if/switch): 11 de 187 (5.9%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `com.pousada` | 0 de 65 | 0 de 0 |
| `com.pousada.api` | 0 de 102 | 0 de 42 |
| `com.pousada.api.dto` | 0 de 82 | 0 de 14 |
| `com.pousada.domain` | 28 de 221 | 11 de 69 |
| `com.pousada.infrastructure` | 0 de 223 | 0 de 46 |
| `com.pousada.service` | 0 de 114 | 0 de 16 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `com.pousada.PricingCalculatorTest` | 3 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 1
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
3 testes  src/test/java/com/pousada/PricingCalculatorTest.java
```
