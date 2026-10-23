# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 17 testes rodaram (0 falhas, 0 erros, 0 pulados); 17 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 365 de 423 (86.3%)
- ramos (if/switch): 134 de 169 (79.3%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `br.com.marealta.pousada` | 365 de 423 | 134 de 169 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `br.com.marealta.pousada.ApiIT` | 4 | 0 | 0 |
| `br.com.marealta.pousada.MailDispatcherTest` | 9 | 0 | 0 |
| `br.com.marealta.pousada.PricingTest` | 4 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 3
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 1
- contra o banco (DataSource, JDBC, Testcontainers): 1
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
9 testes  src/test/java/br/com/marealta/pousada/MailDispatcherTest.java
4 testes  src/test/java/br/com/marealta/pousada/PricingTest.java
4 testes  src/test/java/br/com/marealta/pousada/ApiIT.java
```
