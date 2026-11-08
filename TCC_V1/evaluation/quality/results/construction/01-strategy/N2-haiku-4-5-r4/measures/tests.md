# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 5 testes rodaram (0 falhas, 0 erros, 0 pulados); 5 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 14 de 955 (1.5%)
- ramos (if/switch): 10 de 298 (3.4%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `br.com.mareaalta` | 0 de 27 | 0 de 10 |
| `br.com.mareaalta.api` | 0 de 277 | 0 de 91 |
| `br.com.mareaalta.domain` | 0 de 112 | 0 de 10 |
| `br.com.mareaalta.domain.rateplan` | 0 de 144 | 0 de 71 |
| `br.com.mareaalta.repository` | 0 de 197 | 0 de 50 |
| `br.com.mareaalta.service` | 0 de 161 | 0 de 42 |
| `br.com.mareaalta.util` | 14 de 37 | 10 de 24 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `br.com.mareaalta.DateUtilsTest` | 3 | 0 | 0 |
| `br.com.mareaalta.MoneyTest` | 2 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 2
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
3 testes  src/test/java/br/com/mareaalta/DateUtilsTest.java
2 testes  src/test/java/br/com/mareaalta/MoneyTest.java
```
