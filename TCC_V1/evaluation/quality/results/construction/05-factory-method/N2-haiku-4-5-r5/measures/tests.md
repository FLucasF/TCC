# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 9 testes rodaram (0 falhas, 0 erros, 0 pulados); 9 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 29 de 770 (3.8%)
- ramos (if/switch): 12 de 174 (6.9%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `com.pousadamareaalta` | 0 de 56 | 0 de 4 |
| `com.pousadamareaalta.documents` | 6 de 74 | 0 de 26 |
| `com.pousadamareaalta.handlers` | 0 de 404 | 0 de 82 |
| `com.pousadamareaalta.models` | 1 de 54 | 0 de 0 |
| `com.pousadamareaalta.repositories` | 1 de 159 | 0 de 46 |
| `com.pousadamareaalta.utils` | 21 de 23 | 12 de 16 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `com.pousadamareaalta.IntegrationTest` | 4 | 0 | 0 |
| `com.pousadamareaalta.PriceCalculatorTest` | 5 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 2
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
5 testes  src/test/java/com/pousadamareaalta/PriceCalculatorTest.java
4 testes  src/test/java/com/pousadamareaalta/IntegrationTest.java
```
