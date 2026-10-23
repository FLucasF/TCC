# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 13 testes rodaram (0 falhas, 0 erros, 0 pulados); 13 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 45 de 940 (4.8%)
- ramos (if/switch): 16 de 160 (10.0%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `br.com.pousada.marealta` | 0 de 46 | 0 de 0 |
| `br.com.pousada.marealta.controller` | 0 de 202 | 0 de 38 |
| `br.com.pousada.marealta.dto` | 0 de 10 | 0 de 0 |
| `br.com.pousada.marealta.exception` | 0 de 12 | 0 de 0 |
| `br.com.pousada.marealta.model` | 15 de 174 | 0 de 0 |
| `br.com.pousada.marealta.repository` | 0 de 210 | 0 de 46 |
| `br.com.pousada.marealta.service` | 0 de 231 | 0 de 58 |
| `br.com.pousada.marealta.util` | 30 de 55 | 16 de 18 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `br.com.pousada.marealta.model.GuestIdentificationTest` | 3 | 0 | 0 |
| `br.com.pousada.marealta.util.PriceCalculatorTest` | 10 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 2
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
10 testes  src/test/java/br/com/pousada/marealta/util/PriceCalculatorTest.java
3 testes  src/test/java/br/com/pousada/marealta/model/GuestIdentificationTest.java
```
