# A cobertura e a qualidade dos testes

## Visão geral

O agente escreveu um teste só: `OrderSummaryCalculatorTest.testSimpleCalculation` (`src/test/java/com/example/OrderSummaryCalculatorTest.java:36-57`). É um teste de unidade com Mockito, e o caminho que ele percorre é o mais simples possível: um item, `STORE_PICKUP`, sem cupom, `PIX`, `BRONZE`, `SOUTHEAST`. Ele só confere o `productsSubtotal`. O PIT confirma o que a leitura mostra (`pit.txt`, `exec-07`): `situation` `ok`, `tests` 1, `line_coverage` 0,15 e `mutation_score` 0,071, ou seja, 23 dos 324 erros plantados foram pegos. Dos 324, 290 caem em linhas que nenhum teste executa (`no_coverage`).

Os problemas, do mais grave ao menos grave:

1. **Quase nada dos requisitos é testado.** Não há teste das recusas, do ciclo do pedido, do pagamento repetido, da concorrência nem da TransLog. Do cálculo, só o subtotal.
2. **A asserção é estreita.** O teste calcula o resumo inteiro mas não confere o desconto do Pix, o seguro, o total final nem as parcelas.

O ponto forte: o teste roda sem banco, sem Docker e sem internet, e não depende do relógio.

## Item a item

**T1, o que dos requisitos do enunciado os testes cobrem?** Área por área:
- **O cálculo: em parte, mínimo.** O único teste confere o subtotal, `assertEquals(new BigDecimal("79.90"), summary.getProductsSubtotal())` (`OrderSummaryCalculatorTest.java:56`). Ele não confere o desconto do cupom, o frete, o seguro, o ajuste do pagamento, o total final, as parcelas, o crédito nem o brinde. Nenhum dos dez exemplos do financeiro foi testado. A entrada do teste nem passa pela TransLog, pelos cupons, pelo cartão parcelado, pelo boleto ou pelos níveis SILVER e GOLD.
- **As recusas: não coberta.** Nenhum teste confere um código de erro. `ValidationService`, `CouponValidator` e `GlobalExceptionHandler` não aparecem em `src/test`.
- **O ciclo do pedido: não coberto.** `OrderService` e `Order` não aparecem em nenhum teste.
- **O pagamento repetido: não coberto.**
- **A concorrência na última peça: não coberta.**
- **A transportadora: não coberta.** `TransLogService` é um mock no teste (`OrderSummaryCalculatorTest.java:26-27`) e nunca é chamado, porque a entrega é `STORE_PICKUP`. Nenhum teste simula a TransLog fora do ar ou o `ZIP_NOT_SERVED`.

Das seis áreas, só a do cálculo tem algum teste, e mesmo esse cobre um campo de onze. Os 290 erros plantados em linhas não executadas (`pit.txt`) mostram a mesma coisa.
Conclusão: não segue

**T2, as asserções conferem valores?** As duas asserções do teste estão em `OrderSummaryCalculatorTest.java:55-56`. A primeira, `assertNotNull(summary)`, é do tipo fraco. A segunda confere um valor do negócio, o subtotal 79.90. O cálculo, porém, também produz desconto do Pix, seguro, total final, parcela e crédito (`OrderSummaryCalculator.java:46-73`), e nada disso é conferido. Com essa entrada, o teste deveria esperar ajuste −4,04, seguro 0,80, total final 76,66 e 1× de 76,66, mas nenhum desses valores aparece. Os números do PIT confirmam: dos 34 erros plantados em linhas executadas, 11 passaram (`survived` 11, `test_strength` 0,676). São erros que o teste executa e não percebe, como trocar a porcentagem do seguro ou o sinal do desconto do Pix. Metade das asserções confere valor de verdade, mas ela cobre só uma pequena parte do que o teste executa.
Conclusão: em parte

**T3, os testes rodam sem banco e sem internet, como o enunciado pede?** Sim. O teste usa só `@ExtendWith(MockitoExtension.class)` (`OrderSummaryCalculatorTest.java:20`), sem `@SpringBootTest`, sem contexto Spring e sem repositório. `ProductService` e `TransLogService` são mocks (`:23-27`), e o calculador é montado à mão (`:33`). Nenhum teste precisa de PostgreSQL, de Docker ou de Testcontainers (o `pom.xml` não tem essas dependências), nem da TransLog de verdade. O PIT rodou com `situation` `ok`, o que mostra que o teste passa nesse ambiente. Uma ressalva: como não existe teste com contexto Spring nem de repositório, o projeto nunca precisou resolver o banco em memória. A pergunta foi respondida pela ausência desses testes, não por uma solução.
Conclusão: segue

**T4, os testes dependem de coisas que mudam sozinhas?** O único teste não depende do relógio, não usa `sleep`, não depende da ordem de execução e não compartilha estado: o `@BeforeEach` cria um calculador novo (`OrderSummaryCalculatorTest.java:31-34`), e o produto e a requisição são montados dentro do próprio teste (`:38-51`). Isso só vale para o que existe. A regra das 48 horas não é testada. O código principal compara `paidAt` com `createdAt` (`domain/entity/Order.java:120-126`), mas `createdAt` vem de `Instant.now()` no construtor, sem um relógio injetável (`domain/entity/Order.java:56`; o mesmo em `Notification.java:41`). Um teste futuro das 48 horas teria de contornar isso. Não é uma dependência dos testes atuais, só uma dificuldade que o código deixa para os próximos.
Conclusão: segue

## O que mudaria primeiro

1. **Testar os dez exemplos do financeiro contra o resumo inteiro.** Um teste parametrizado, com a TransLog substituída por um dublê que devolve as respostas dos exemplos, conferindo os onze campos e as recusas dos exemplos 8 a 10. É o requisito central do enunciado, e hoje 11 dos 34 erros plantados nas linhas já executadas passam sem ser percebidos.
2. **Testar o ciclo do pedido no `OrderService`.** Com repositórios falsos ou um banco em memória, e com o `createdAt` controlado: as transições permitidas e as proibidas, o limite exato das 48 horas, o aviso repetido com o mesmo `transactionId` (sem baixar o estoque nem dar o crédito duas vezes) e a falta de estoque na hora do pagamento. Hoje nada disso tem teste.

## Dúvidas

- No T3, o teste roda sem banco e sem internet, mas porque o agente nunca testou nada que dependesse deles, e não porque montou um ambiente substituto. A régua pergunta se os testes rodam nessas condições; como rodam, a conclusão foi "segue", com a ressalva escrita no item.
- No T4, a mesma situação: com um teste só e nenhum envolvendo tempo, a régua é cumprida pelo que existe. Registrei à parte o `Instant.now()` fixo do código principal, que pesa só para os testes que ainda faltam.
