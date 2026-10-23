# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 21 testes rodaram (0 falhas, 0 erros, 0 pulados); 21 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 148 de 884 (16.7%)
- ramos (if/switch): 47 de 172 (27.3%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `com.pousada` | 0 de 46 | 0 de 4 |
| `com.pousada.api` | 0 de 183 | 0 de 14 |
| `com.pousada.config` | 0 de 23 | 0 de 2 |
| `com.pousada.domain` | 71 de 185 | 21 de 52 |
| `com.pousada.repository` | 0 de 204 | 0 de 38 |
| `com.pousada.service` | 0 de 163 | 0 de 32 |
| `com.pousada.service.messaging` | 77 de 80 | 26 de 30 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `com.pousada.domain.ReservationTest` | 8 | 0 | 0 |
| `com.pousada.service.messaging.ArchiveCopyRuleTest` | 2 | 0 | 0 |
| `com.pousada.service.messaging.EmailValidationRuleTest` | 6 | 0 | 0 |
| `com.pousada.service.messaging.MessageProcessorTest` | 3 | 0 | 0 |
| `com.pousada.service.messaging.TestModeRuleTest` | 2 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 5
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
8 testes  src/test/java/com/pousada/domain/ReservationTest.java
6 testes  src/test/java/com/pousada/service/messaging/EmailValidationRuleTest.java
3 testes  src/test/java/com/pousada/service/messaging/MessageProcessorTest.java
2 testes  src/test/java/com/pousada/service/messaging/TestModeRuleTest.java
2 testes  src/test/java/com/pousada/service/messaging/ArchiveCopyRuleTest.java
```
