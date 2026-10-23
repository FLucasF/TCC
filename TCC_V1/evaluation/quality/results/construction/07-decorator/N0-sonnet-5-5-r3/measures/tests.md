# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 11 testes rodaram (0 falhas, 0 erros, 0 pulados); 11 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 89 de 444 (20.0%)
- ramos (if/switch): 59 de 164 (36.0%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `br.com.marealta` | 0 de 46 | 0 de 8 |
| `br.com.marealta.domain` | 33 de 189 | 18 de 60 |
| `br.com.marealta.mail` | 56 de 97 | 41 de 60 |
| `br.com.marealta.web` | 0 de 112 | 0 de 36 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `br.com.marealta.MessagePolicyTest` | 7 | 0 | 0 |
| `br.com.marealta.PricingTest` | 4 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 2
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
7 testes  src/test/java/br/com/marealta/MessagePolicyTest.java
4 testes  src/test/java/br/com/marealta/PricingTest.java
```
