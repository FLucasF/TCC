# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 7 testes rodaram (0 falhas, 0 erros, 0 pulados); 7 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 50 de 693 (7.2%)
- ramos (if/switch): 16 de 170 (9.4%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `com.marealata` | 0 de 16 | 0 de 0 |
| `com.marealata.dao` | 0 de 143 | 0 de 30 |
| `com.marealata.database` | 0 de 44 | 0 de 6 |
| `com.marealata.handler` | 0 de 210 | 0 de 52 |
| `com.marealata.model` | 1 de 56 | 0 de 0 |
| `com.marealata.service` | 0 de 162 | 0 de 56 |
| `com.marealata.util` | 49 de 62 | 16 de 26 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `com.marealata.util.DocumentFormatterTest` | 4 | 0 | 0 |
| `com.marealata.util.PriceCalculatorTest` | 3 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 2
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
4 testes  src/test/java/com/marealata/util/DocumentFormatterTest.java
3 testes  src/test/java/com/marealata/util/PriceCalculatorTest.java
```
