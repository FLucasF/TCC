# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 14 testes rodaram (0 falhas, 0 erros, 0 pulados); 14 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 300 de 925 (32.4%)
- ramos (if/switch): 42 de 188 (22.3%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `com.pousadamarealta` | 0 de 39 | 0 de 0 |
| `com.pousadamarealta.api` | 0 de 246 | 0 de 54 |
| `com.pousadamarealta.database` | 0 de 16 | 0 de 0 |
| `com.pousadamarealta.document` | 134 de 136 | 14 de 18 |
| `com.pousadamarealta.domain` | 166 de 216 | 28 de 48 |
| `com.pousadamarealta.repository` | 0 de 158 | 0 de 44 |
| `com.pousadamarealta.service` | 0 de 114 | 0 de 24 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `com.pousadamarealta.document.DocumentGeneratorTest` | 6 | 0 | 0 |
| `com.pousadamarealta.domain.ReservationTest` | 8 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 2
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
8 testes  src/test/java/com/pousadamarealta/domain/ReservationTest.java
6 testes  src/test/java/com/pousadamarealta/document/DocumentGeneratorTest.java
```
