# testes

## Visão geral

O agente quase não escreveu testes. Há um único método de teste, `contextLoads()` (`src/test/java/com/loja/LojaPedidosApplicationTest.java:11-13`). Ele sobe o contexto do Spring com H2 em memória e não confere nada: o corpo está vazio. Os números do PIT (`pit.txt`, linha `exec-09`) mostram isso. A `situation` é `ok` e `tests` é 1. Dos 468 erros plantados, 0 foram pegos, 26 sobreviveram e 442 caíram em linhas que nenhum teste executa. O `mutation_score` e o `test_strength` são 0,000 e o `line_coverage` é 0,10.

O problema mais sério é que nenhum requisito do enunciado é testado: o cálculo, as recusas, o ciclo do pedido, a idempotência do pagamento, a concorrência e a TransLog ficam sem teste. Em seguida vem a ausência de asserções. O único ponto positivo é a configuração de teste: o `mvn verify` roda sem PostgreSQL, sem Docker e sem internet, porque o banco é um H2 com schema próprio.

## Item a item

**T1, o que dos requisitos do enunciado os testes cobrem?** Nenhuma das seis áreas tem teste. O `src/test/java` tem um único arquivo e um único método vazio (`LojaPedidosApplicationTest.java:11-13`).
- **O cálculo**: nenhum teste chama `CheckoutService` nem `POST /checkout/summary`. Nenhum dos 7 exemplos do financeiro é conferido.
- **As recusas**: nenhum teste confere um código de erro (`INVALID_ORDER`, `COUPON_NOT_APPLICABLE` etc.).
- **O ciclo do pedido**: as classes de `com/loja/state/` não são exercitadas por nenhum teste.
- **O pagamento repetido**: não há teste.
- **A concorrência**: não há teste.
- **A transportadora**: não há teste de TransLog fora do ar nem de `ZIP_NOT_SERVED`. A URL de teste (`application-test.properties:11`, `http://localhost:9999`) nunca chega a ser chamada.

Os 10% de linhas cobertas são só o que roda na subida do contexto, e nenhuma delas pegou um erro plantado (`killed` = 0).
Conclusão: não segue

**T2, as asserções conferem valores?** Não há asserção nenhuma. `contextLoads()` só garante que o contexto sobe (`LojaPedidosApplicationTest.java:12-13`). Ele não confere sequer um status HTTP ou um valor não nulo, que já seria o caso fraco que a régua descreve. Os números confirmam: das linhas executadas, 26 erros plantados sobreviveram e nenhum foi pego (`test_strength` 0,000 em `pit.txt`). Quando um teste executa código e não pega nenhum erro, ele não confere nada.
Conclusão: não segue

**T3, os testes rodam sem banco e sem internet?** Sim.
- **O banco**: é um H2 em memória. O H2 entra como dependência `test` (`pom.xml:42-46`) e o perfil `test` aponta para `jdbc:h2:mem:testdb` (`application-test.properties:1-4`), ativado por `@ActiveProfiles("test")` (`LojaPedidosApplicationTest.java:8`). Não há PostgreSQL, Docker nem Testcontainers no `pom.xml`.
- **A TransLog**: a URL de teste é `http://localhost:9999` (`application-test.properties:11`), mas nada a chama na subida do contexto, então o teste não depende de rede. Por outro lado, também não existe um servidor falso nem um dublê de `TransLogClient`.

Uma ressalva: o teste usa um schema próprio, `src/test/resources/schema-h2.sql`, copiado à mão de `src/main/resources/schema.sql`. Hoje os dois diferem só no dialeto: `DECIMAL` em vez de `NUMERIC`, `AUTO_INCREMENT` em vez de `BIGSERIAL` e `MERGE ... KEY(sku)` em vez de `INSERT ... ON CONFLICT` (linhas 80-89). O risco é que o `ddl-auto=validate` do teste confere as entidades contra a cópia, não contra o schema de produção, e as duas podem divergir sem que nenhum teste perceba.
Conclusão: segue

**T4, os testes dependem de coisas que mudam sozinhas?** O único teste não depende do relógio, de `sleep`, da ordem de execução nem de estado deixado por outro teste. Isso vale só porque ele não faz nada. O código principal não deixa o tempo ser controlado: `OrderService.java:86` usa `LocalDateTime.now()` e `Notification.java:33` também, ambos sem um `Clock` injetado. Assim, um teste das 48 horas (`OrderService.java:299-300`) teria de depender do relógio real ou montar o `paidAt` em volta dele. Isso é uma limitação de testabilidade, não uma dependência dos testes que existem hoje.
Conclusão: segue

## O que mudaria primeiro

1. **Testes de unidade do cálculo do resumo com os exemplos 1 a 7 do financeiro**, conferindo cada campo (subtotal, cupom, frete, prazo, seguro, ajuste, total final, parcela, crédito, brinde), com um dublê de `TransLogClient` que devolva as respostas dadas nos exemplos. É a parte do enunciado com mais regras e mais arredondamentos, e hoje nenhum erro plantado nela é pego.
2. **Testes do ciclo do pedido sobre o H2 que já está configurado**: as transições permitidas e as proibidas, os limites de 48 horas e de 48 horas mais um segundo, e o aviso de pagamento repetido sem baixar o estoque nem dar o crédito duas vezes. Para isso, antes, injetar um `Clock` no lugar de `LocalDateTime.now()` (`OrderService.java:86`).

## Dúvidas

No T4, a régua pergunta se os testes *dependem* de coisas que mudam sozinhas. O único teste não depende, então concluí "segue". Mas o resultado vem só de ele não testar nada. Registrei a falta de um relógio controlável no código principal como observação, sem rebaixar a conclusão.

No T3 fiz a mesma escolha: concluí "segue" porque a pergunta é se os testes rodam sem banco e sem internet, e rodam. A ausência de um dublê da TransLog e o schema de teste duplicado ficaram como ressalva.
