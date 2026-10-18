Li os dois testes e o código que eles exercitam; agora escrevo a correção.

# testes

## Visão geral

O agente escreveu poucos testes: 6 métodos em duas classes (`pit.txt`: `tests=6`). Três deles testam `Money` e três refazem os exemplos 1 a 3 do enunciado, chamando `CheckoutService.calculateSummary`. O ponto forte é a qualidade das asserções. Todas conferem valores do negócio, campo a campo do resumo. Os testes rodam sem banco e sem internet, com H2 em memória e a TransLog substituída por um mock, e não dependem de relógio nem de ordem. O PIT confirma isso: dos erros plantados em linhas que os testes executam, eles pegam 81% (`test_strength=0.811`). O problema mais sério é a cobertura. Fora o cálculo do resumo, nenhum requisito do enunciado é testado: nenhuma recusa, nenhuma transição do pedido, nenhum pagamento repetido, nenhuma concorrência e nenhuma falha da TransLog. Metade das linhas do código principal nunca roda (`line_coverage=0.45`, `no_coverage=145` de 293 mutantes), e por isso o `mutation_score` fica em 0.410. Mesmo dentro do cálculo, ficaram de fora os exemplos que mais pegariam erros: o desconto do Pix, o BUY3PAY2, o FREESHIPPING com o teto de 30%, o limite do seguro e o frete grátis e o brinde do GOLD.

## Item a item

**T1, o que dos requisitos os testes cobrem.**

- **O cálculo: coberto em parte.** `CheckoutServiceTest.example1`, `example2` e `example3` (`src/test/java/com/loja/service/CheckoutServiceTest.java:97-129`, `:132-164` e `:167-195`) conferem os 11 campos do resumo dos exemplos 1, 2 e 3. Eles passam pelo WELCOME10, pelo MINUS50, pelos juros do cartão em 6×, pela tarifa do boleto, pelos dias a mais do Norte, pelo seguro de Norte, Centro-Oeste e Nordeste e pelo crédito SILVER/GOLD. O motoboy para o GOLD, que paga o frete, aparece no exemplo 3. `MoneyTest` (`src/test/java/com/loja/domain/MoneyTest.java:9-15`) confere o arredondamento meio para o par com os números do enunciado (2,995 → 3,00 e 2,985 → 2,98). Ficaram de fora os exemplos 4 a 7, e com eles:
  - o desconto de 5% do Pix sem cupom: no exemplo 1, o único com Pix, há cupom e o ajuste é 0,00;
  - o BUY3PAY2;
  - o FREESHIPPING e o teto de 30% (`CheckoutService.java:258-259`);
  - o limite de R$ 30,00 do seguro;
  - o frete zerado do GOLD;
  - o brinde, que só é conferido como `false`.

  Também não há teste de que o pedido registrado guarda o mesmo resumo da prévia, a exigência que o enunciado mais destaca. Tudo é testado no serviço, nunca pelos endpoints.
- **As recusas: não cobertas.** Nenhum teste confere um código de erro, nem os exemplos 8 a 10. `grep` por `assertThrows` em `src/test` não acha nada.
- **O ciclo do pedido: não coberto.** Nenhum teste chama `OrderService`, que tem 233 linhas, nem `Order.markAsPaid` e as outras transições.
- **O pagamento repetido: não coberto.**
- **A concorrência: não coberta.** Os `@Lock(PESSIMISTIC_WRITE)` de `ProductRepository.java:14` e `CustomerRepository.java:14` não são exercitados por nenhum teste.
- **A transportadora: não coberta.** O mock (`CheckoutServiceTest.java:44`) só devolve cotações de sucesso. Não há teste com `ZIP_NOT_SERVED`, nem com a TransLog fora do ar, nem do `TransLogClient` real. É uma lacuna que esconde erros na leitura do código:
  - `TransLogShipping.isAvailable` engole qualquer exceção e devolve `false` (`src/main/java/com/loja/service/shipping/TransLogShipping.java:30-31`). Assim, a TransLog fora do ar parece virar `SHIPPING_METHOD_UNAVAILABLE` e não `CARRIER_UNAVAILABLE`, e o `catch` de `CheckoutService.java:207-208` nunca dispara.
  - `TransLogClient.QuoteResponse` tem os campos `priceCents` e `etaHours` (`src/main/java/com/loja/integration/TransLogClient.java:43-47`), mas a TransLog responde `price_cents` e `eta_hours`, e não achei configuração de nomes no projeto. Dúvida: não rodei; é o que a leitura indica.
  - O `timeoutMs` é recebido e não é usado (`TransLogClient.java:16-21`).

  Um teste com um servidor falso pegaria os três.

Só uma das seis áreas tem cobertura, e incompleta.
Conclusão: não segue

**T2, as asserções conferem valores.** Todas conferem. Os três testes de checkout fazem 11 asserções cada, com `assertEquals` sobre o `BigDecimal` exato de cada campo e `assertEquals`/`assertFalse` no prazo, nas parcelas e no brinde (`CheckoutServiceTest.java:118-128`, por exemplo `assertEquals(new BigDecimal("412.07"), summary.finalTotal().value())`). Os testes de `MoneyTest` também conferem o valor do resultado (`MoneyTest.java:12`, `:23` e `:30`). Não há nenhum `assertNotNull`, nenhum "não deu exceção" e nenhum status HTTP. O mock da TransLog também exige os argumentos exatos: `getQuote("01310100", 1800, "EXP")` em `CheckoutServiceTest.java:98`. Assim o teste confere de quebra o peso em gramas e o código do serviço, porque um peso errado deixaria o mock sem resposta e o teste falharia. Os números do PIT dizem o mesmo: entre os mutantes que os testes executam, 120 são pegos e 28 passam (`test_strength=0.811`). O que derruba o `mutation_score` é a falta de cobertura, não asserções fracas. A única fraqueza é o brinde: ele só é conferido como `false`, então inverter `GoldClub.hasGift` (`src/main/java/com/loja/service/club/GoldClub.java:31`) para sempre `false` passaria.
Conclusão: segue

**T3, rodam sem banco e sem internet.** O banco é um H2 em memória. `src/test/resources/application.properties:3-9` define `jdbc:h2:mem:testdb`, o `H2Dialect` e `create-drop`, e o H2 entra no `pom.xml` com `scope=test`. Esse arquivo substitui o de `src/main`, que exigiria `DB_URL` e PostgreSQL. A TransLog é um mock do Mockito (`CheckoutServiceTest.java:44`), passado à mão para `EconomyShipping` e `ExpressShipping` (`:58-59`). O bean real recebe `translog.url=http://localhost:8080`, mas nunca é chamado nos testes. Não há Testcontainers, Docker nem PostgreSQL no `pom.xml` de teste. A `situation` é `ok`, e os testes passaram no ambiente do PIT. Uma observação: o `@SpringBootTest` sobe o contexto inteiro só para obter dois repositórios, enquanto as estratégias são montadas com `new` (`CheckoutServiceTest.java:48-77`). O teste confere essa lista feita à mão, não a montagem do Spring. Isso não quebra o requisito.
Conclusão: segue

**T4, dependem de coisas que mudam sozinhas.** Os testes não usam relógio, não fazem `sleep` e não dependem da ordem. Cada um monta o seu catálogo: o `@BeforeEach` apaga e regrava os produtos (`CheckoutServiceTest.java:82-94`), sem depender do `DataInitializer`. Há uma dependência implícita: o `example1` usa WELCOME10, que consulta no banco se o CPF já tem pedido pago (`CheckoutService.java:239-242`). O teste passa porque a tabela de pedidos está vazia, mas nenhum teste a limpa. Hoje nenhum teste grava pedidos, então isso não falha; o primeiro teste que registrar e pagar um pedido com o mesmo CPF pode quebrá-lo, conforme a ordem de execução. Quanto ao tempo, os testes não dependem do relógio porque não testam nada que dependa dele. O código de produção usa `Instant.now()` direto em `Order.java:50` e `Order.java:118`, sem um `Clock` injetável. Por isso a regra das 48 horas, com o caso exato de 48 horas, não poderia ser testada sem esperar ou manipular o relógio. É provavelmente por isso que ficou sem teste.
Conclusão: segue

## O que mudaria primeiro

1. **Testar a TransLog e as recusas.** Um teste com um servidor HTTP falso (MockWebServer ou WireMock em processo) para o `TransLogClient` real, com cotação válida, `ZIP_NOT_SERVED`, erro 500 e demora acima de 2 s. Mais testes de recusa conferindo o código, começando pelos exemplos 8 a 10. Pela leitura, isso pegaria na hora três defeitos: o JSON em snake_case, o timeout ignorado e a falha que vira `SHIPPING_METHOD_UNAVAILABLE`. São as linhas onde hoje nenhum teste passa.
2. **Testar o ciclo do pedido.** Testes de `OrderService` com um `Clock` injetado no lugar de `Instant.now()`, cobrindo as transições permitidas e proibidas, o limite exato de 48 horas, o aviso de pagamento repetido sem baixar o estoque duas vezes e o cancelamento de pedido pago devolvendo o estoque e o crédito. É ali que estão quase todos os 145 mutantes sem cobertura, e as regras de maior risco para o negócio.

## Dúvidas

- Em T2, julguei as asserções que existem, que são boas, e deixei a falta de testes para T1. Por isso T2 e T4 saíram `segue` mesmo com só seis testes.
- Os defeitos da TransLog citados em T1 (JSON em snake_case, timeout, exceção engolida) vêm da leitura, sem rodar o serviço. Entram aqui só como o que a falta de testes deixa passar; quem confirma é a dimensão de comportamento.
