# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 20 testes rodaram (0 falhas, 0 erros, 0 pulados); 20 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 39 de 933 (4.2%)
- ramos (if/switch): 34 de 192 (17.7%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `com.pousada` | 0 de 30 | 0 de 6 |
| `com.pousada.api` | 0 de 242 | 0 de 58 |
| `com.pousada.api.dto` | 0 de 141 | 0 de 0 |
| `com.pousada.database` | 0 de 20 | 0 de 0 |
| `com.pousada.models` | 0 de 111 | 0 de 0 |
| `com.pousada.repositories` | 0 de 149 | 0 de 30 |
| `com.pousada.services` | 0 de 180 | 0 de 58 |
| `com.pousada.utils` | 39 de 60 | 34 de 40 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `com.pousada.CpfFormatterTest` | 3 | 0 | 0 |
| `com.pousada.DateUtilTest` | 5 | 0 | 0 |
| `com.pousada.EmailValidatorTest` | 8 | 0 | 0 |
| `com.pousada.MoneyTest` | 4 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 4
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
8 testes  src/test/java/com/pousada/EmailValidatorTest.java
5 testes  src/test/java/com/pousada/DateUtilTest.java
4 testes  src/test/java/com/pousada/MoneyTest.java
3 testes  src/test/java/com/pousada/CpfFormatterTest.java
```
