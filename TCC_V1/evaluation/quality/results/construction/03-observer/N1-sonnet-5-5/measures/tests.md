# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 17 testes rodaram (0 falhas, 0 erros, 0 pulados); 17 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 446 de 468 (95.3%)
- ramos (if/switch): 74 de 86 (86.0%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `pousada` | 7 de 16 | 0 de 4 |
| `pousada.app` | 82 de 84 | 22 de 22 |
| `pousada.domain` | 132 de 132 | 29 de 30 |
| `pousada.infra` | 118 de 122 | 8 de 8 |
| `pousada.notification` | 40 de 40 | 2 de 2 |
| `pousada.web` | 67 de 74 | 13 de 20 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `pousada.ApiTest` | 8 | 0 | 0 |
| `pousada.DomainTest` | 9 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 2
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 1
- contra o banco (DataSource, JDBC, Testcontainers): 1
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
9 testes  src/test/java/pousada/DomainTest.java
8 testes  src/test/java/pousada/ApiTest.java
```
