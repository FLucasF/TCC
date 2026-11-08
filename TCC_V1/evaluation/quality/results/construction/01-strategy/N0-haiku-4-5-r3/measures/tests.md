# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 7 testes rodaram (0 falhas, 0 erros, 0 pulados); 7 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 54 de 744 (7.3%)
- ramos (if/switch): 20 de 204 (9.8%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `com.pousadamareaita` | 0 de 304 | 0 de 50 |
| `com.pousadamareaita.model` | 0 de 69 | 0 de 0 |
| `com.pousadamareaita.repository` | 0 de 179 | 0 de 54 |
| `com.pousadamareaita.service` | 0 de 134 | 0 de 74 |
| `com.pousadamareaita.util` | 54 de 58 | 20 de 26 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `com.pousadamareaita.util.PricingCalculatorTest` | 7 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 1
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
7 testes  src/test/java/com/pousadamareaita/util/PricingCalculatorTest.java
```
