# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 5 testes rodaram (0 falhas, 0 erros, 0 pulados); 5 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 32 de 650 (4.9%)
- ramos (if/switch): 17 de 144 (11.8%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `com.pousadamareaalta` | 0 de 245 | 0 de 32 |
| `com.pousadamareaalta.database` | 0 de 161 | 0 de 32 |
| `com.pousadamareaalta.domain` | 0 de 7 | 0 de 0 |
| `com.pousadamareaalta.service` | 0 de 74 | 0 de 34 |
| `com.pousadamareaalta.service.report` | 0 de 116 | 0 de 24 |
| `com.pousadamareaalta.util` | 32 de 47 | 17 de 22 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `com.pousadamareaalta.PriceCalculatorTest` | 5 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 1
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
5 testes  src/test/java/com/pousadamareaalta/PriceCalculatorTest.java
```
