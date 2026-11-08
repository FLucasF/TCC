# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 16 testes rodaram (0 falhas, 0 erros, 0 pulados); 16 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 62 de 843 (7.4%)
- ramos (if/switch): 14 de 198 (7.1%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `com.pousada` | 0 de 41 | 0 de 2 |
| `com.pousada.api` | 0 de 201 | 0 de 34 |
| `com.pousada.config` | 0 de 30 | 0 de 14 |
| `com.pousada.exception` | 0 de 6 | 0 de 0 |
| `com.pousada.model` | 26 de 121 | 0 de 8 |
| `com.pousada.repository` | 0 de 210 | 0 de 42 |
| `com.pousada.service` | 19 de 215 | 4 de 88 |
| `com.pousada.util` | 17 de 19 | 10 de 10 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `com.pousada.service.PricingServiceTest` | 5 | 0 | 0 |
| `com.pousada.util.DateUtilTest` | 6 | 0 | 0 |
| `com.pousada.util.PriceUtilTest` | 5 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 3
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
6 testes  src/test/java/com/pousada/util/DateUtilTest.java
5 testes  src/test/java/com/pousada/util/PriceUtilTest.java
5 testes  src/test/java/com/pousada/service/PricingServiceTest.java
```
