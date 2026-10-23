# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 12 testes rodaram (0 falhas, 0 erros, 0 pulados); 12 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 130 de 902 (14.4%)
- ramos (if/switch): 22 de 176 (12.5%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `br.com.pousada.maralta` | 0 de 43 | 0 de 8 |
| `br.com.pousada.maralta.controller` | 0 de 226 | 0 de 28 |
| `br.com.pousada.maralta.database` | 0 de 29 | 0 de 2 |
| `br.com.pousada.maralta.domain` | 32 de 123 | 0 de 16 |
| `br.com.pousada.maralta.guesttype` | 71 de 117 | 8 de 40 |
| `br.com.pousada.maralta.repository` | 0 de 194 | 0 de 36 |
| `br.com.pousada.maralta.service` | 27 de 170 | 14 de 46 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `br.com.pousada.maralta.GuestTypeTest` | 4 | 0 | 0 |
| `br.com.pousada.maralta.PricingServiceTest` | 8 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 2
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
8 testes  src/test/java/br/com/pousada/maralta/PricingServiceTest.java
4 testes  src/test/java/br/com/pousada/maralta/GuestTypeTest.java
```
