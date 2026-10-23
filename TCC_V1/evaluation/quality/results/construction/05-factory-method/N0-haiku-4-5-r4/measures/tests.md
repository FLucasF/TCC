# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 9 testes rodaram (0 falhas, 0 erros, 0 pulados); 9 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 32 de 787 (4.1%)
- ramos (if/switch): 16 de 178 (9.0%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `com.pousada.marealta` | 0 de 18 | 0 de 0 |
| `com.pousada.marealta.database` | 0 de 42 | 0 de 12 |
| `com.pousada.marealta.dto` | 0 de 3 | 0 de 0 |
| `com.pousada.marealta.handlers` | 0 de 269 | 0 de 36 |
| `com.pousada.marealta.model` | 0 de 91 | 0 de 0 |
| `com.pousada.marealta.repository` | 0 de 141 | 0 de 26 |
| `com.pousada.marealta.service` | 0 de 118 | 0 de 62 |
| `com.pousada.marealta.util` | 32 de 105 | 16 de 42 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `com.pousada.marealta.util.PriceCalculatorTest` | 9 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 1
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
9 testes  src/test/java/com/pousada/marealta/util/PriceCalculatorTest.java
```
