Já li os 20 testes, o enunciado e os trechos do código que os testes tocam. Segue a correção.

# Os testes

## Visão geral

O agente escreveu uma única classe de testes de integração, `PedidosIntegrationTest`, com 20 métodos `@Test`. Ela sobe o serviço inteiro numa porta aleatória, troca o PostgreSQL por H2 em memória e a TransLog por um servidor HTTP falso do próprio JDK, e conversa com o serviço por HTTP de verdade. O ponto mais forte são as asserções. Os sete exemplos do financeiro conferem os onze campos do resumo, valor por valor. As recusas conferem o código do erro. O ciclo do pedido confere a situação, o estoque, o saldo e os avisos depois de cada operação. Os números do PIT confirmam: 94% das linhas executadas, 77,9% dos erros plantados pegos e força de 0,857 (`pit.txt`). Os problemas, do mais sério para o menos:
1. **A concorrência não é testada.** O teste da "última peça" faz os dois pagamentos um depois do outro.
2. **O tempo depende do relógio de verdade.** A janela de 48 horas foi bem resolvida, mas os pagamentos comuns usam uma data fixa, anterior ao registro do pedido.
3. **Sobra estado entre testes.** O estoque da jaqueta (`JAQ-001`) alterado num teste nunca é restaurado.
4. **Lacunas menores.** A TransLog que demora mais de 2 segundos ou responde lixo não é testada, nem a precedência entre recusas.

## Item a item

**T1, o que dos requisitos do enunciado os testes cobrem?**
- **O cálculo: coberto.** `example1` a `example7` (`src/test/java/br/com/loja/pedidos/PedidosIntegrationTest.java:134-185`) refazem os sete exemplos do enunciado e conferem os onze campos pelo `assertSummary` (`:107-123`). Assim ficam cobertos:
  - o teto de 30%, no BUY3PAY2 do exemplo 5 e no FREESHIPPING do exemplo 6;
  - os juros da tabela Price (exemplo 2);
  - o GOLD que paga o motoboy (exemplo 3);
  - o brinde, o limite do seguro e o desconto do Pix (exemplo 7);
  - os dias a mais no Norte (exemplos 1 e 7).

  Além disso, `registeredSummaryEqualsPreview` (`:426-434`) confere que o resumo do pedido registrado, e depois consultado, é igual ao resumo prévio. Não há teste no limite exato de 5 kg do motoboy. A única compra para o Nordeste com transportadora (`:428`) é comparada só com o resumo prévio, não com valores esperados.
- **As recusas: cobertas.** São três grupos:
  - `example8`, `example9` e `example10` (`:187-216`) conferem o código e o HTTP 422.
  - `validationOrder` (`:229-265`) passa por quase toda a tabela: `INVALID_ORDER`, `UNKNOWN_PRODUCT`, `PRODUCT_DISCONTINUED`, `INSUFFICIENT_STOCK`, `INVALID_CLUB_TIER`, `INVALID_REGION`, `INVALID_ZIP_CODE`, `INVALID_SHIPPING_METHOD`, `INVALID_COUPON` (vazio e minúsculo), `INVALID_PAYMENT_METHOD`, `INVALID_INSTALLMENTS` (Pix em 2× e cartão em 13×), `PAYMENT_METHOD_UNAVAILABLE` e motoboy acima de 5 kg.
  - `unknownResources` (`:412-424`) cobre os 404.

  O nome `validationOrder` promete testar a ordem das recusas, mas cada chamada tem um defeito só. A única com dois defeitos (`:233-235`, SKU repetido e CPF vazio) cai no mesmo código. Logo, a precedência entre as regras não é testada. O `COUPON_NOT_APPLICABLE` do MINUS50 abaixo de R$ 300,00 também não.
- **O ciclo do pedido: coberto.** `fullLifeCycleWithCreditAndNotifications` (`:292-325`) confere as transições permitidas (pagar, enviar, entregar) e as proibidas: enviar sem pagar (`:298`), entregar sem enviar (`:317`), cancelar depois de enviado (`:321`) e um outro `transactionId` num pedido já pago (`:314-315`). `cancellingPaidOrderGivesEverythingBack` (`:356-367`) e `cancellingUnpaidOrderTouchesNothing` (`:369-377`) conferem a devolução do estoque e do crédito e o cancelamento repetido (409). `paymentWindowIs48HoursInclusive` (`:379-396`) confere exatamente 48 h (aceito) e 48 h + 1 s (`ORDER_EXPIRED`, pedido `CANCELLED`).
- **O pagamento repetido: coberto.** As linhas `:309-312` repetem o mesmo aviso e conferem a resposta 200, o estoque ainda em 48 e o saldo ainda em 3,20. As recusas repetidas também são conferidas: `ORDER_EXPIRED` em `:393-394` e `INSUFFICIENT_STOCK` em `:408-409`.
- **A concorrência: não coberta.** `lastPieceIsSoldOnlyOnce` (`:398-410`) registra dois pedidos da `MOC-001` e paga um **depois** do outro (`:402-403`), na mesma thread. O teste prova que o segundo pagamento é recusado por falta de estoque, não que duas compras ao mesmo tempo não vendem a peça duas vezes. Uma versão do serviço sem o `FOR UPDATE` (`src/main/java/br/com/loja/pedidos/catalog/ProductRepository.java:37`), ou seja sem a trava, passaria do mesmo jeito.
- **A transportadora: coberta.** `carrierDownIs503` (`:220-227`) põe o servidor falso para responder 500 e confere `CARRIER_UNAVAILABLE` com 503. `example10` (`:209-216`) confere o `ZIP_NOT_SERVED`. Ficam de fora a TransLog que não responde em 2 segundos e a que responde 200 com um corpo inválido, dois caminhos que o código trata (`src/main/java/br/com/loja/pedidos/shipping/TranslogClient.java:48`, `:56-59`, `:71-77`).

Cinco das seis áreas estão bem cobertas. A concorrência aparece só em versão sequencial.
Conclusão: em parte

**T2, as asserções conferem valores?** Em quase todos os testes, sim:
- O `assertSummary` compara cada valor em dinheiro com `isEqualByComparingTo` (`:112-121`) e confere também o prazo, as parcelas e o brinde.
- O ciclo confere a situação depois de cada operação (`:303`, `:320`, `:322`, `:363`, `:391`, `:406`), o estoque pelo endpoint do produto (`:304`, `:311`, `:364`, `:395`, `:407`), o saldo do cliente (`:306`, `:312`, `:365`) e a sequência exata dos tipos de aviso (`:324`, `:376`).
- `notificationsAreListedAndMarkedRead` confere o tipo, a situação, o `orderId` e a marcação como lido repetida (`:338-349`).

Os pontos fracos são poucos:
- o `assertSummary` aceita tanto 200 quanto 201 (`:110`);
- `validationOrder` confere só o código do erro, não o HTTP 422 (`:232-264`);
- as linhas `:331-332` pagam e cancelam sem conferir a resposta, e só depois conferem os avisos.

Os números do PIT batem com essa leitura (`pit.txt`): cobertura de linhas de 0,94, `mutation_score` de 0,779 e `test_strength` de 0,857. Dos 279 erros plantados em linhas executadas, 239 são pegos. Não é o padrão de testes que executam o código sem conferir nada. Os 40 que sobrevivem e os 28 sem cobertura se explicam em boa parte pelas lacunas do T1, mas o PIT não diz quais são, então isso fica como hipótese.
Conclusão: segue

**T3, os testes rodam sem banco e sem internet?** Sim. O `@DynamicPropertySource` (`:49-55`) aponta o banco para `jdbc:h2:mem:pedidos;MODE=PostgreSQL;DB_CLOSE_DELAY=-1`, com o H2 só no escopo de teste (`pom.xml`, a dependência `com.h2database:h2` com `<scope>test</scope>`). A TransLog é um `com.sun.net.httpserver.HttpServer` em `127.0.0.1`, numa porta livre (`:33-47`), passado ao serviço por `translog.url` (`:54`). Nenhum teste usa PostgreSQL, Docker, Testcontainers ou a TransLog de verdade, e o `pom.xml` não tem nenhuma dessas dependências. Uma ressalva que não é defeito do item: com o H2, o `FOR UPDATE` e a migração `V1__init.sql` rodam num banco diferente do de produção. Isso é consequência da restrição do enunciado, não um problema.
Conclusão: segue

**T4, os testes dependem de coisas que mudam sozinhas?**
- **Sleep:** nenhum.
- **Estado entre testes:** cada teste começa de um estado limpo. O `@BeforeEach` (`:66-77`) apaga pedidos, itens, avisos, eventos de pagamento e clientes, e restaura o estoque da `CAM-001` e da `MOC-001` e a resposta da TransLog falsa. A exceção é `example7`, que muda o estoque da `JAQ-001` para 10 (`:180`) e ninguém restaura. Hoje não quebra nada: `validationOrder` usa `JAQ-001` × 3 (`:260`), que passa com 3 ou com 10. Mas é estado que vaza de um teste para outro. A resposta da TransLog falsa fica em campos `static volatile` mutáveis (`:29-30`). Isso funciona porque o JUnit roda os testes em sequência por padrão, e quebraria se alguém ligasse a execução em paralelo.
- **Relógio:** o serviço tem um `Clock` injetável (`src/main/java/br/com/loja/pedidos/PedidosApplication.java:16-17`, usado em `src/main/java/br/com/loja/pedidos/order/OrderService.java:57`), mas os testes não o substituem. O teste das 48 horas contorna isso bem: calcula o `paidAt` a partir do `createdAt` devolvido pelo próprio serviço (`:287-290`), e assim não depende da data em que roda. Já os outros pagamentos usam um `paidAt` fixo, `2026-10-14T16:00:00Z` (`:276`, e também `:202`), num pedido registrado com a hora real. Esses testes só passam porque o pagamento "anterior ao registro" nunca é considerado tardio (`OrderService.java:123`). Rodados antes de 12/10/2026, o pagamento cairia mais de 48 h depois do registro e daria `ORDER_EXPIRED`. Hoje (18/10/2026) passam e vão continuar passando, mas o resultado depende do relógio de verdade, e os testes pagam pedidos antes de eles existirem.
Conclusão: em parte

## O que mudaria primeiro

1. **Um teste de concorrência de verdade para a última peça.** Disparar os dois pagamentos da `MOC-001` em duas threads, liberadas juntas por um `CountDownLatch`, e conferir que exatamente um dá 200, o outro dá `INSUFFICIENT_STOCK` e o estoque termina em 0. É o requisito que o enunciado mais destaca ("já aconteceu de vender o que não tinha"), e hoje nada protege a trava no estoque.
2. **Controlar o tempo e fechar o estado.** Fornecer nos testes um `Clock` fixo, por exemplo um `@TestConfiguration` com `Clock.fixed`, e calcular todo `paidAt` a partir dele. Restaurar no `@BeforeEach` o estoque de todo o catálogo, não só de dois produtos. Assim nenhum teste depende do dia em que roda nem do que o teste anterior deixou.

## Dúvidas

- Em T1, a área "concorrência" contou como não coberta, apesar de `lastPieceIsSoldOnlyOnce` tratar da última peça: o item pede "duas compras ao mesmo tempo", e o teste é sequencial. Com cinco de seis áreas cobertas, escolhi "em parte".
- Em T4, o `paidAt` fixo e o vazamento da `JAQ-001` não quebram nenhum teste hoje. Contei como dependência porque o item pergunta se os testes dependem de coisas que mudam sozinhas, não se falham agora.
