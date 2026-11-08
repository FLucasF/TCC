# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 5 testes rodaram (0 falhas, 0 erros, 0 pulados); 5 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 432 de 497 (86.9%)
- ramos (if/switch): 55 de 84 (65.5%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `com.marealta` | 340 de 403 | 51 de 80 |
| `com.marealta.report` | 92 de 94 | 4 de 4 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `com.marealta.AppTest` | 5 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 1
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 1
- contra o banco (DataSource, JDBC, Testcontainers): 1
- com H2 ou outro banco em memória: 1
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
5 testes  src/test/java/com/marealta/AppTest.java
```
