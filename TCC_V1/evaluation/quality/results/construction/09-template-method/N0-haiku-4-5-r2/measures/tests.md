# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 5 testes rodaram (0 falhas, 0 erros, 0 pulados); 5 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 40 de 756 (5.3%)
- ramos (if/switch): 17 de 158 (10.8%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `com.pousada` | 0 de 237 | 0 de 48 |
| `com.pousada.models` | 0 de 88 | 0 de 0 |
| `com.pousada.repositories` | 0 de 178 | 0 de 32 |
| `com.pousada.services` | 0 de 202 | 0 de 60 |
| `com.pousada.utils` | 40 de 51 | 17 de 18 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `com.pousada.PriceCalculatorTest` | 5 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 1
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
5 testes  src/test/java/com/pousada/PriceCalculatorTest.java
```
