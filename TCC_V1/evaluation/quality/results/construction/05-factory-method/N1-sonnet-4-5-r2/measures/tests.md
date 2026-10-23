# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 16 testes rodaram (0 falhas, 0 erros, 0 pulados); 16 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 217 de 892 (24.3%)
- ramos (if/switch): 29 de 203 (14.3%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `br.com.pousada` | 0 de 63 | 0 de 8 |
| `br.com.pousada.api` | 0 de 185 | 0 de 59 |
| `br.com.pousada.domain` | 56 de 106 | 0 de 32 |
| `br.com.pousada.exception` | 0 de 12 | 0 de 0 |
| `br.com.pousada.repository` | 0 de 255 | 0 de 43 |
| `br.com.pousada.service` | 132 de 240 | 13 de 45 |
| `br.com.pousada.util` | 29 de 31 | 16 de 16 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `br.com.pousada.DocumentServiceTest` | 5 | 0 | 0 |
| `br.com.pousada.PriceCalculatorTest` | 11 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 2
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
11 testes  src/test/java/br/com/pousada/PriceCalculatorTest.java
5 testes  src/test/java/br/com/pousada/DocumentServiceTest.java
```
