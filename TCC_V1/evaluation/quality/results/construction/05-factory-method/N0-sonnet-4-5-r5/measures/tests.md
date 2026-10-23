# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 17 testes rodaram (0 falhas, 0 erros, 0 pulados); 25 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 243 de 901 (27.0%)
- ramos (if/switch): 34 de 176 (19.3%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `br.com.maralta` | 0 de 117 | 0 de 24 |
| `br.com.maralta.dto` | 0 de 114 | 0 de 2 |
| `br.com.maralta.exception` | 0 de 12 | 0 de 0 |
| `br.com.maralta.model` | 128 de 199 | 5 de 38 |
| `br.com.maralta.repository` | 0 de 195 | 0 de 44 |
| `br.com.maralta.service` | 97 de 223 | 19 de 56 |
| `br.com.maralta.util` | 18 de 41 | 10 de 12 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `br.com.maralta.DocumentServiceTest` | 5 | 0 | 0 |
| `br.com.maralta.PricingServiceTest` | 7 | 0 | 0 |
| `br.com.maralta.SeasonUtilsTest` | 5 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 4
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 1
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
8 testes  src/test/java/br/com/maralta/ReservationIT.java
7 testes  src/test/java/br/com/maralta/PricingServiceTest.java
5 testes  src/test/java/br/com/maralta/SeasonUtilsTest.java
5 testes  src/test/java/br/com/maralta/DocumentServiceTest.java
```
