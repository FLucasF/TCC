# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 7 testes rodaram (0 falhas, 0 erros, 0 pulados); 7 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 27 de 696 (3.9%)
- ramos (if/switch): 7 de 150 (4.7%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `br.com.pousada` | 0 de 21 | 0 de 0 |
| `br.com.pousada.config` | 0 de 46 | 0 de 8 |
| `br.com.pousada.controller` | 0 de 209 | 0 de 40 |
| `br.com.pousada.model` | 0 de 53 | 0 de 0 |
| `br.com.pousada.repository` | 0 de 180 | 0 de 38 |
| `br.com.pousada.service` | 0 de 155 | 0 de 56 |
| `br.com.pousada.util` | 27 de 32 | 7 de 8 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `br.com.pousada.service.ReservationServiceTest` | 1 | 0 | 0 |
| `br.com.pousada.util.DateUtilTest` | 2 | 0 | 0 |
| `br.com.pousada.util.MoneyUtilTest` | 4 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 3
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
4 testes  src/test/java/br/com/pousada/util/MoneyUtilTest.java
2 testes  src/test/java/br/com/pousada/util/DateUtilTest.java
1 testes  src/test/java/br/com/pousada/service/ReservationServiceTest.java
```
