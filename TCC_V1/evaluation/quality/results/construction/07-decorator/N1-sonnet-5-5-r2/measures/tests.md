# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 9 testes rodaram (0 falhas, 0 erros, 0 pulados); 9 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 97 de 467 (20.8%)
- ramos (if/switch): 40 de 105 (38.1%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `br.com.marealta.pousada` | 0 de 15 | 0 de 0 |
| `br.com.marealta.pousada.domain` | 41 de 86 | 15 de 28 |
| `br.com.marealta.pousada.mail` | 37 de 37 | 13 de 13 |
| `br.com.marealta.pousada.mail.rules` | 19 de 20 | 12 de 14 |
| `br.com.marealta.pousada.service` | 0 de 92 | 0 de 14 |
| `br.com.marealta.pousada.store` | 0 de 141 | 0 de 12 |
| `br.com.marealta.pousada.web` | 0 de 76 | 0 de 24 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `br.com.marealta.pousada.MailPipelineTest` | 3 | 0 | 0 |
| `br.com.marealta.pousada.PricingTest` | 4 | 0 | 0 |
| `br.com.marealta.pousada.ReceiptTest` | 2 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 3
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
4 testes  src/test/java/br/com/marealta/pousada/PricingTest.java
3 testes  src/test/java/br/com/marealta/pousada/MailPipelineTest.java
2 testes  src/test/java/br/com/marealta/pousada/ReceiptTest.java
```
