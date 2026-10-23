# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 15 testes rodaram (0 falhas, 0 erros, 0 pulados); 15 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 68 de 739 (9.2%)
- ramos (if/switch): 21 de 210 (10.0%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `com.posadamareaita` | 0 de 14 | 0 de 8 |
| `com.posadamareaita.api` | 0 de 204 | 0 de 72 |
| `com.posadamareaita.database` | 0 de 33 | 0 de 8 |
| `com.posadamareaita.domain` | 17 de 84 | 0 de 0 |
| `com.posadamareaita.repository` | 0 de 147 | 0 de 26 |
| `com.posadamareaita.service` | 0 de 202 | 0 de 74 |
| `com.posadamareaita.util` | 51 de 55 | 21 de 22 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `com.posadamareaita.util.FormattingUtilTest` | 7 | 0 | 0 |
| `com.posadamareaita.util.PriceCalculatorTest` | 3 | 0 | 0 |
| `com.posadamareaita.util.SeasonUtilTest` | 5 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 3
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
7 testes  src/test/java/com/posadamareaita/util/FormattingUtilTest.java
5 testes  src/test/java/com/posadamareaita/util/SeasonUtilTest.java
3 testes  src/test/java/com/posadamareaita/util/PriceCalculatorTest.java
```
