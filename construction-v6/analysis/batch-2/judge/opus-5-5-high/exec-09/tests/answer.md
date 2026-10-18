# tests

## Visão geral

O agente escreveu uma suíte quase vazia. Há um único arquivo, `src/test/java/com/loja/CheckoutServiceTest.java`, com três métodos JUnit 4. Só um deles exercita código de produção: `testMoneyRounding` chama `MoneyUtils.round` e confere o arredondamento "meio para o par" com os exemplos do enunciado. Os outros dois só montam um `CheckoutRequest`, que é um DTO de campos públicos sem lógica, e conferem os valores que eles mesmos acabaram de atribuir.

O PIT confirma: `situation=ok`, `tests=3`, `mutants=397`, `killed=1`, `survived=0`, `no_coverage=396`, `mutation_score=0.003`, `line_coverage=0.00` (`pit.txt`, linha `exec-09`). O `test_strength=1.000` engana, porque vem de um só mutante executado em 397.

O problema mais sério é que nenhuma regra de negócio é testada: nem o cálculo do resumo, nem as recusas, nem o ciclo do pedido, nem o pagamento repetido, nem a concorrência, nem a TransLog. Em segundo lugar, dois dos três testes são tautológicos. O único ponto positivo é que a suíte roda sem banco e sem internet, mas só porque não toca em nada que dependa deles.

## Item a item

**T1, o que dos requisitos do enunciado os testes cobrem.** Área por área:
- **O cálculo:** nenhum teste confere subtotal, desconto, frete, seguro, ajuste ou total. `CheckoutService.calculateSummary` (`src/main/java/com/loja/services/CheckoutService.java:199`) nunca é chamado. O que existe é o teste do auxiliar de arredondamento (`CheckoutServiceTest.java:13-17`), que confere uma regra transversal do cálculo, mas não os valores do resumo.
- **As recusas:** nada. `testInvalidRequest` (`CheckoutServiceTest.java:20-25`) tem o nome de uma recusa, mas só confere que uma lista vazia recém-criada está vazia. Não chama `validateCheckoutRequest` (`CheckoutService.java:17`) e não confere nenhum código de erro.
- **O ciclo do pedido:** nada. `PaymentService` e `OrderService` não aparecem nos testes.
- **O pagamento repetido:** nada.
- **A concorrência:** nada.
- **A transportadora:** nada. `queryTranslog` (`CheckoutService.java:388`) não é exercitado.

O PIT bate com isso: 396 dos 397 mutantes estão em linhas que nenhum teste executa.
Conclusão: não segue

**T2, as asserções conferem valores.** Das três, uma é boa:
- `testMoneyRounding` confere valores de negócio, `assertEquals(3.00, MoneyUtils.round(2.995), 0.001)` e `assertEquals(2.98, MoneyUtils.round(2.985), 0.001)` (`CheckoutServiceTest.java:14-15`), que são os exemplos do enunciado. É o teste que mata o único mutante pego.
- `testInvalidRequest` faz `assertEquals(..., true, req.items.isEmpty())` sobre a lista que a linha 22 acabou de criar vazia.
- `testValidCheckoutRequestStructure` faz `assertNotNull(req.customer)`, `assertEquals(1, req.items.size())` e `assertEquals("BRONZE", req.clubTier)` (`CheckoutServiceTest.java:46-48`), todos sobre valores que o próprio teste atribuiu nas linhas 30-44.

Esses dois últimos não passam por nenhum código de produção. São piores que o caso fraco que a régua descreve: não deixam passar um erro na conta, porque nem chegam a fazer conta. Um terço das asserções confere valor real, e só no auxiliar de arredondamento. O padrão do PIT é o inverso do sinal descrito na régua (cobertura alta, poucos erros pegos). Aqui a cobertura é praticamente nula, então o problema começa antes da qualidade das asserções: falta o que conferir.
Conclusão: em parte

**T3, os testes rodam sem banco e sem internet.** Rodam. Nenhum dos três abre conexão, chama `Database.init` ou faz HTTP. Mas isso acontece porque os testes evitam a dificuldade, não porque a resolvem. Não há banco em memória, repositório falso nem dublê da TransLog. O código principal também dificulta isso: o pool é um campo estático (`src/main/java/com/loja/database/Database.java:11`), os serviços são métodos estáticos (`CheckoutService.java:17`, `:199`; `OrderService.java:12`), e a URL da TransLog vem de `System.getenv("TRANSLOG_URL")` (`CheckoutService.java:133`, `:357`). Não há costura para trocar o banco ou a TransLog.

Vale registrar também que o `pom.xml` declara `org.testcontainers:testcontainers` e `org.testcontainers:postgresql` com escopo `test` (`pom.xml:59-71`), e que o `ARQUITETURA.md:237` menciona "testes integrados com testcontainers". Nenhum teste usa essas dependências (`grep` por `Testcontainers`/`PostgreSQLContainer` em `src/` não encontra nada). Por isso o `mvn verify` não precisa de Docker. São só dependências mortas, que o Maven baixa normalmente.
Conclusão: segue

**T4, os testes dependem de coisas que mudam sozinhas.** Nenhum dos três testes usa relógio, `sleep`, ordem de execução ou estado compartilhado: cada um cria seus próprios objetos (`CheckoutServiceTest.java:21`, `:29`). O código principal não oferece relógio controlável: `OrderService.createOrder` usa `Instant.now()` (`src/main/java/com/loja/services/OrderService.java:13`) e `NotificationService` também (`NotificationService.java:22`). Já a regra das 48 horas compara o `paidAt` que vem no aviso com o `createdAt` do pedido (`PaymentService.java:50-52`). Um teste das 48 horas, se existisse, poderia controlar o tempo pelo `paidAt` em vez do relógio. Mas esse teste não existe, então não há o que avaliar além da ausência de dependência.
Conclusão: segue

## O que mudaria primeiro

1. **Testar o cálculo do resumo com os exemplos do enunciado.** O enunciado traz casos com os valores esperados de cada parte, por exemplo "subtotal 1.419,60 · … · total final 1.377,12" (`prompt.md:274`). Isso pede uma lógica de cálculo pura, separada do JDBC estático, que receba produtos e parâmetros e devolva o resumo, com testes que conferem cada campo. É onde está a maior parte do valor e dos 396 mutantes não cobertos.
2. **Pôr uma costura no acesso ao banco e na TransLog** (interfaces de repositório e de transportadora, ou pelo menos um banco em memória e um servidor HTTP falso). Com isso dá para testar o ciclo do pedido, o pagamento repetido, as 48 horas e a TransLog fora do ar sem PostgreSQL. Na mesma passada, apagar os dois testes tautológicos e as dependências de Testcontainers que não são usadas.

## Dúvidas

- **No T1, o teste do arredondamento conta ou não como cobertura do "cálculo"?** Li que não, porque a área pede que um teste confira os valores do resumo, e o teste só confere o auxiliar isolado. Registrei o teste no texto mesmo assim.
- **No T3 e no T4, por que "segue" se os testes não fazem quase nada?** As duas perguntas são sobre depender de banco, internet, relógio ou ordem, e a suíte de fato não depende. Por isso marquei "segue", deixando claro no texto que é por ausência de testes que toquem nessas partes, e não por uma solução de isolamento.
