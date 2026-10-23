# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 10 testes rodaram (0 falhas, 0 erros, 0 pulados); 10 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 96 de 868 (11.1%)
- ramos (if/switch): 19 de 190 (10.0%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `com.pousada.mare_alta` | 0 de 20 | 0 de 8 |
| `com.pousada.mare_alta.database` | 0 de 17 | 0 de 0 |
| `com.pousada.mare_alta.document` | 51 de 117 | 5 de 26 |
| `com.pousada.mare_alta.http` | 0 de 157 | 0 de 46 |
| `com.pousada.mare_alta.http.request` | 0 de 1 | 0 de 0 |
| `com.pousada.mare_alta.http.response` | 0 de 63 | 0 de 2 |
| `com.pousada.mare_alta.model` | 23 de 139 | 0 de 12 |
| `com.pousada.mare_alta.repository` | 0 de 152 | 0 de 26 |
| `com.pousada.mare_alta.service` | 0 de 172 | 0 de 52 |
| `com.pousada.mare_alta.util` | 22 de 30 | 14 de 18 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `com.pousada.mare_alta.document.CpfDocumentFormatterTest` | 2 | 0 | 0 |
| `com.pousada.mare_alta.util.DateUtilTest` | 5 | 0 | 0 |
| `com.pousada.mare_alta.util.NumberFormatterTest` | 3 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 3
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
5 testes  src/test/java/com/pousada/mare_alta/util/DateUtilTest.java
3 testes  src/test/java/com/pousada/mare_alta/util/NumberFormatterTest.java
2 testes  src/test/java/com/pousada/mare_alta/document/CpfDocumentFormatterTest.java
```
