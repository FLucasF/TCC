# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 8 testes rodaram (0 falhas, 0 erros, 0 pulados); 8 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 11 de 724 (1.5%)
- ramos (if/switch): 8 de 174 (4.6%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `com.pousadamarealta` | 0 de 26 | 0 de 8 |
| `com.pousadamarealta.database` | 0 de 21 | 0 de 0 |
| `com.pousadamarealta.dto` | 0 de 8 | 0 de 0 |
| `com.pousadamarealta.model` | 0 de 71 | 0 de 0 |
| `com.pousadamarealta.notification` | 0 de 49 | 0 de 10 |
| `com.pousadamarealta.repository` | 0 de 146 | 0 de 38 |
| `com.pousadamarealta.route` | 0 de 215 | 0 de 28 |
| `com.pousadamarealta.service` | 0 de 167 | 0 de 80 |
| `com.pousadamarealta.util` | 11 de 21 | 8 de 10 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `com.pousadamarealta.util.DateUtilsTest` | 5 | 0 | 0 |
| `com.pousadamarealta.util.MoneyUtilsTest` | 3 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 2
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
5 testes  src/test/java/com/pousadamarealta/util/DateUtilsTest.java
3 testes  src/test/java/com/pousadamarealta/util/MoneyUtilsTest.java
```
