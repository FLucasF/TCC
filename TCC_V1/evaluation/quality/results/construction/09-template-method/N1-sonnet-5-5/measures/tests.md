# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 7 testes rodaram (0 falhas, 0 erros, 0 pulados); 7 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 275 de 515 (53.4%)
- ramos (if/switch): 49 de 94 (52.1%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `br.com.mealta` | 0 de 16 | 0 de 4 |
| `br.com.mealta.domain` | 182 de 190 | 42 de 52 |
| `br.com.mealta.report` | 93 de 93 | 7 de 8 |
| `br.com.mealta.store` | 0 de 129 | 0 de 12 |
| `br.com.mealta.web` | 0 de 87 | 0 de 18 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `br.com.mealta.PousadaTest` | 7 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 2
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
7 testes  src/test/java/br/com/mealta/PousadaTest.java
0 testes  src/test/java/br/com/mealta/InMemoryStore.java
```
