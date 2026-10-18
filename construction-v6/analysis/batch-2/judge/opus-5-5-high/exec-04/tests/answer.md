Já li o enunciado, o projeto inteiro, o único arquivo de teste e os números do PIT. Segue a correção.

# Testes

## Visão geral

O agente escreveu um único arquivo de teste, `src/test/java/com/clothing/service/CheckoutServiceTest.java`, com um método só, `testMoneyRounding` (linhas 28–35). Ele confere o arredondamento "meio para o par" de `Money.round`. As quatro asserções conferem valores exatos e estão certas. Fora isso, não há teste. O resumo, as recusas, o ciclo do pedido, o pagamento repetido, a concorrência e a TransLog ficam todos sem teste. O `setUp` (linhas 20–26) cria `ProductRepository`, `CustomerRepository`, `OrderRepository` e `CheckoutService`, e nenhum teste os usa. Os campos se chamam `mockProductRepo` e similares, mas são as classes reais, que chamam o estático `Database.getConnection()` (por exemplo `service/ProductRepository.java:10`). Pela regra 2 o nome não conta: não existe dublê nenhum. Os números do PIT (`pit.txt`, linha `exec-04`) confirmam a leitura: `situation` ok, `tests` 1, `line_coverage` 0.01, 320 erros plantados, 2 pegos, 0 que passaram e 318 em linhas que nenhum teste executa. Isso dá `mutation_score` 0.006 e `test_strength` 1.000. O problema mais sério, de longe, é que quase nada é coberto (T1). As asserções são boas (T2) e não dependem de banco, internet nem relógio (T3, T4), mas só porque o único teste não encosta em nada disso. O código também não oferece por onde trocar o banco por um dublê.

## Item a item

**T1, o que dos requisitos do enunciado os testes cobrem?**

- **O cálculo:** não há cobertura. Nenhum teste chama `CheckoutService.calculateSummary` (`service/CheckoutService.java:22`). Nenhum confere subtotal, desconto, frete, seguro, total final, parcela ou crédito, nem algum dos dez exemplos do enunciado. O único teste confere o arredondamento de `Money.round` (`CheckoutServiceTest.java:31-34`), que é peça do cálculo mas não é o resumo.
- **As recusas:** não há teste de nenhum código de erro (`INVALID_ORDER`, `COUPON_NOT_APPLICABLE` etc.).
- **O ciclo do pedido:** não há teste de pagar, enviar, entregar ou cancelar, nem das transições proibidas.
- **O pagamento repetido:** não há teste. `OrderRepository.isPaymentAlreadyRecorded` e o `processPayment` (`service/CheckoutService.java:222`) não são executados.
- **A concorrência:** não há teste da última peça.
- **A transportadora:** não há teste de `TransLogClient`, nem de TransLog fora do ar ou de `ZIP_NOT_SERVED`.

O PIT diz o mesmo: 318 dos 320 erros plantados estão em linhas que nenhum teste executa, e a cobertura de linhas é de 1%. Nenhuma das seis áreas está coberta.

Conclusão: não segue

**T2, as asserções conferem valores?** Sim. As quatro asserções do único teste são `assertEquals` contra um `BigDecimal` exato, com a escala junto: `2.995` vira `3.00`, `2.985` vira `2.98`, `2.004` vira `2.00` e `1.996` vira `2.00` (`CheckoutServiceTest.java:31-34`). Nenhuma é do tipo fraco ("não deu exceção", "não é nulo", "status 200"). O PIT concorda: `test_strength` 1.000, ou seja, os 2 erros plantados nas linhas executadas foram pegos e nenhum passou (`survived` 0). O padrão é bom, mas foi aplicado a uma única função de três linhas (`service/Money.java:7-10`). A falta de abrangência foi julgada no T1, não aqui.

Conclusão: segue

**T3, os testes rodam sem banco e sem internet, como o enunciado pede?** Rodam. O PIT executou com `situation` ok, e o único teste só chama `Money.round`, que não toca banco nem rede. Os repositórios criados no `setUp` (`CheckoutServiceTest.java:22-25`) não abrem conexão no construtor. Só os métodos chamam `Database.getConnection()`, e o teste nunca os chama. Não há Testcontainers, Docker nem PostgreSQL nas dependências de teste (`pom.xml`: só `junit:4.13.2`). Ressalva: o projeto não resolve o banco nem a TransLog, só evita os dois. Não existe banco em memória, repositório falso ou servidor falso da TransLog. Os repositórios são classes concretas presas ao estático `Database` (`Database.java:152`), e `TransLogClient` lê `TRANSLOG_URL` direto do ambiente (`service/TransLogClient.java:24`) com um `HttpClient` estático (`:19`). Hoje nenhum teste depende de infraestrutura externa, então o item segue. Mas qualquer teste que exercitasse o `CheckoutService` precisaria de PostgreSQL de verdade. O `TESTING.md` só descreve testes manuais, com `docker-compose`, e não conta como teste.

Conclusão: segue

**T4, os testes dependem de coisas que mudam sozinhas?** Não. O único teste não usa relógio, não tem `sleep`, não depende da ordem de execução e não divide estado com outro teste: o `@Before` monta tudo de novo e é o único método. A regra das 48 horas não é testada. No código principal, o registro do pedido grava `Instant.now()` sem relógio controlável (`service/OrderRepository.java:45`), o que dificultaria um teste futuro dessa regra. A comparação em si usa `createdAt` e o `paidAt` recebido (`service/CheckoutService.java:223-225`), e não o relógio. Isso é observação sobre o código principal, não sobre os testes que existem.

Conclusão: segue

## O que mudaria primeiro

1. **Testar o cálculo do resumo com os exemplos do enunciado.** Seriam os exemplos 1 a 7, mais as recusas dos exemplos 8 a 10, conferindo cada campo do resumo e cada código de erro. Para isso, o `CheckoutService` precisa receber os repositórios e a TransLog por interface, com dublês em memória e um dublê da cotação. É aí que está quase todo o risco de negócio, e hoje 318 dos 320 erros plantados passam despercebidos por falta de execução.
2. **Testar o ciclo do pedido e o pagamento**, também com repositórios em memória: as transições permitidas e proibidas, a expiração com 48 h exatas e com 48 h e 1 s, e o aviso repetido com o mesmo `transactionId` sem baixar o estoque nem dar o crédito duas vezes.

## Dúvidas

- **T3 e T4:** os dois itens seguem a prática no sentido literal, porque nenhum teste depende de banco, internet ou relógio. Mas isso acontece porque o único teste não exercita nada que dependa deles, e não porque o agente tenha resolvido o banco ou a TransLog com dublês. Mantive "segue", porque a pergunta dos itens é sobre a dependência dos testes que existem, e registrei a ressalva no texto. A pobreza da cobertura foi julgada no T1.
- **T2:** dei "segue" porque todas as asserções existentes conferem valores exatos e o `test_strength` é 1.000, mesmo sendo só quatro asserções sobre uma única função.
