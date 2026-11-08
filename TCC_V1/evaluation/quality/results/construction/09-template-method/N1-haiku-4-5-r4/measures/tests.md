# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 7 testes rodaram (0 falhas, 0 erros, 0 pulados); 7 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 28 de 829 (3.4%)
- ramos (if/switch): 14 de 138 (10.1%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `com.pousada` | 0 de 286 | 0 de 36 |
| `com.pousada.db` | 0 de 217 | 0 de 36 |
| `com.pousada.dto` | 0 de 11 | 0 de 0 |
| `com.pousada.model` | 0 de 92 | 0 de 0 |
| `com.pousada.service` | 0 de 194 | 0 de 52 |
| `com.pousada.util` | 28 de 29 | 14 de 14 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `com.pousada.util.PriceCalculatorTest` | 7 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 1
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
7 testes  src/test/java/com/pousada/util/PriceCalculatorTest.java
```
