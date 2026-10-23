# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 14 testes rodaram (0 falhas, 0 erros, 0 pulados); 14 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 51 de 830 (6.1%)
- ramos (if/switch): 24 de 212 (11.3%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `com.pousada.api` | 0 de 71 | 0 de 14 |
| `com.pousada.api.handler` | 0 de 195 | 0 de 80 |
| `com.pousada.db` | 0 de 20 | 0 de 2 |
| `com.pousada.document` | 0 de 108 | 0 de 18 |
| `com.pousada.domain` | 3 de 74 | 0 de 0 |
| `com.pousada.repository` | 0 de 181 | 0 de 24 |
| `com.pousada.service` | 0 de 125 | 0 de 42 |
| `com.pousada.util` | 48 de 56 | 24 de 32 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `com.pousada.util.FormatterTest` | 6 | 0 | 0 |
| `com.pousada.util.PriceCalculatorTest` | 8 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 2
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
8 testes  src/test/java/com/pousada/util/PriceCalculatorTest.java
6 testes  src/test/java/com/pousada/util/FormatterTest.java
```
