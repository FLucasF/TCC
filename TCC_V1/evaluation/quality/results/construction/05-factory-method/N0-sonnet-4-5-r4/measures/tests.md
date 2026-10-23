# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 12 testes rodaram (0 falhas, 0 erros, 0 pulados); 12 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 166 de 881 (18.8%)
- ramos (if/switch): 28 de 184 (15.2%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `br.com.marealto` | 0 de 48 | 0 de 8 |
| `br.com.marealto.dto` | 0 de 108 | 0 de 44 |
| `br.com.marealto.handler` | 0 de 137 | 0 de 6 |
| `br.com.marealto.model` | 59 de 101 | 0 de 0 |
| `br.com.marealto.repository` | 0 de 247 | 0 de 58 |
| `br.com.marealto.service` | 107 de 240 | 28 de 68 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `br.com.marealto.service.DocumentServiceTest` | 4 | 0 | 0 |
| `br.com.marealto.service.PricingServiceTest` | 8 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 2
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
8 testes  src/test/java/br/com/marealto/service/PricingServiceTest.java
4 testes  src/test/java/br/com/marealto/service/DocumentServiceTest.java
```
