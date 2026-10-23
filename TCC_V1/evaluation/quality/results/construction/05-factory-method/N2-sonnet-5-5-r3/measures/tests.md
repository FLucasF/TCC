# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 10 testes rodaram (0 falhas, 0 erros, 0 pulados); 10 métodos de teste escritos no código.

## A cobertura (JaCoCo)

Sem relatório do JaCoCo: o agente do JaCoCo não entrou nos testes (por exemplo, um `argLine` no surefire sem `@{argLine}`), ou nenhum teste rodou.

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `br.com.mare.pousada.ApiTest` | 3 | 0 | 0 |
| `br.com.mare.pousada.DomainTest` | 7 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 3
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 1
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
7 testes  src/test/java/br/com/mare/pousada/DomainTest.java
3 testes  src/test/java/br/com/mare/pousada/ApiTest.java
0 testes  src/test/java/br/com/mare/pousada/InMemoryRepositories.java
```
