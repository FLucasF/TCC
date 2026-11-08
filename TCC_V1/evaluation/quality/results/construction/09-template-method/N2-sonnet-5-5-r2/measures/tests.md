# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 3 testes rodaram (0 falhas, 0 erros, 0 pulados); 3 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 335 de 485 (69.1%)
- ramos (if/switch): 67 de 104 (64.4%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `pousada` | 0 de 14 | 0 de 8 |
| `pousada.domain` | 142 de 142 | 32 de 36 |
| `pousada.repo` | 26 de 144 | 10 de 24 |
| `pousada.reports` | 71 de 72 | 6 de 6 |
| `pousada.service` | 60 de 69 | 16 de 26 |
| `pousada.web` | 36 de 44 | 3 de 4 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `pousada.PousadaTest` | 3 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 1
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 1
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
3 testes  src/test/java/pousada/PousadaTest.java
```
