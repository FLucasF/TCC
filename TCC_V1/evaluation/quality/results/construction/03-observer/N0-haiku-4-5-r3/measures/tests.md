# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 6 testes rodaram (0 falhas, 0 erros, 0 pulados); 6 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 29 de 813 (3.6%)
- ramos (if/switch): 16 de 164 (9.8%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `br.marealta` | 0 de 33 | 0 de 2 |
| `br.marealta.api` | 0 de 205 | 0 de 46 |
| `br.marealta.db` | 0 de 208 | 0 de 38 |
| `br.marealta.model` | 3 de 105 | 0 de 0 |
| `br.marealta.service` | 0 de 219 | 0 de 52 |
| `br.marealta.util` | 26 de 43 | 16 de 26 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `br.marealta.util.PricingCalculatorTest` | 6 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 1
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
6 testes  src/test/java/br/marealta/util/PricingCalculatorTest.java
```
