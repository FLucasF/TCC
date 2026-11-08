# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 5 testes rodaram (0 falhas, 0 erros, 0 pulados); 5 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 44 de 977 (4.5%)
- ramos (if/switch): 14 de 132 (10.6%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `com.pousada` | 0 de 12 | 0 de 0 |
| `com.pousada.api` | 0 de 277 | 0 de 28 |
| `com.pousada.api.dto` | 0 de 91 | 0 de 0 |
| `com.pousada.config` | 0 de 24 | 0 de 0 |
| `com.pousada.domain` | 17 de 112 | 0 de 6 |
| `com.pousada.report` | 0 de 117 | 0 de 16 |
| `com.pousada.repository` | 0 de 190 | 0 de 36 |
| `com.pousada.service` | 27 de 154 | 14 de 46 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `com.pousada.PriceCalculatorTest` | 5 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 1
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
5 testes  src/test/java/com/pousada/PriceCalculatorTest.java
```
