# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 7 testes rodaram (0 falhas, 0 erros, 0 pulados); 7 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 30 de 921 (3.3%)
- ramos (if/switch): 14 de 142 (9.9%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `com.pousadamareal` | 0 de 39 | 0 de 2 |
| `com.pousadamareal.api` | 0 de 131 | 0 de 0 |
| `com.pousadamareal.controller` | 0 de 157 | 0 de 36 |
| `com.pousadamareal.model` | 7 de 231 | 0 de 24 |
| `com.pousadamareal.repository` | 0 de 217 | 0 de 46 |
| `com.pousadamareal.service` | 23 de 146 | 14 de 34 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `com.pousadamareal.PricingServiceTest` | 7 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 1
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
7 testes  src/test/java/com/pousadamareal/PricingServiceTest.java
```
