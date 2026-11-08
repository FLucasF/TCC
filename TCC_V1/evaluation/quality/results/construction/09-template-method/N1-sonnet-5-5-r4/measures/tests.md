# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 8 testes rodaram (0 falhas, 0 erros, 0 pulados); 8 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 441 de 466 (94.6%)
- ramos (if/switch): 74 de 101 (73.3%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `br.com.mare.pousada` | 0 de 4 | 0 de 4 |
| `br.com.mare.pousada.domain` | 150 de 151 | 40 de 47 |
| `br.com.mare.pousada.infra` | 103 de 105 | 7 de 8 |
| `br.com.mare.pousada.report` | 84 de 87 | 8 de 14 |
| `br.com.mare.pousada.service` | 39 de 39 | 6 de 6 |
| `br.com.mare.pousada.web` | 65 de 80 | 13 de 22 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `br.com.mare.pousada.ApiIntegrationTest` | 2 | 0 | 0 |
| `br.com.mare.pousada.DomainTest` | 6 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 2
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 1
- contra o banco (DataSource, JDBC, Testcontainers): 1
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
6 testes  src/test/java/br/com/mare/pousada/DomainTest.java
2 testes  src/test/java/br/com/mare/pousada/ApiIntegrationTest.java
```
