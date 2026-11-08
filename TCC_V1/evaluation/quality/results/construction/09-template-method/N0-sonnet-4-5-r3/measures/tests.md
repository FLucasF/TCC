# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 13 testes rodaram (0 falhas, 0 erros, 0 pulados); 13 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 25 de 878 (2.8%)
- ramos (if/switch): 14 de 140 (10.0%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `com.pousada.marealba` | 0 de 52 | 0 de 8 |
| `com.pousada.marealba.config` | 0 de 25 | 0 de 0 |
| `com.pousada.marealba.controller` | 0 de 184 | 0 de 30 |
| `com.pousada.marealba.exception` | 0 de 19 | 0 de 0 |
| `com.pousada.marealba.model` | 0 de 143 | 0 de 0 |
| `com.pousada.marealba.repository` | 0 de 190 | 0 de 36 |
| `com.pousada.marealba.service` | 0 de 236 | 0 de 52 |
| `com.pousada.marealba.util` | 25 de 29 | 14 de 14 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `com.pousada.marealba.util.CpfUtilTest` | 3 | 0 | 0 |
| `com.pousada.marealba.util.DateUtilTest` | 7 | 0 | 0 |
| `com.pousada.marealba.util.MoneyUtilTest` | 3 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 3
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
7 testes  src/test/java/com/pousada/marealba/util/DateUtilTest.java
3 testes  src/test/java/com/pousada/marealba/util/MoneyUtilTest.java
3 testes  src/test/java/com/pousada/marealba/util/CpfUtilTest.java
```
