# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 11 testes rodaram (0 falhas, 0 erros, 0 pulados); 11 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 340 de 488 (69.7%)
- ramos (if/switch): 101 de 144 (70.1%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `br.com.marealta` | 9 de 30 | 0 de 8 |
| `br.com.marealta.domain` | 145 de 146 | 41 de 46 |
| `br.com.marealta.report` | 98 de 102 | 25 de 32 |
| `br.com.marealta.store` | 30 de 144 | 20 de 34 |
| `br.com.marealta.web` | 58 de 66 | 15 de 24 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `br.com.marealta.ApiTest` | 6 | 0 | 0 |
| `br.com.marealta.PricingTest` | 5 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 2
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 1
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
6 testes  src/test/java/br/com/marealta/ApiTest.java
5 testes  src/test/java/br/com/marealta/PricingTest.java
```
