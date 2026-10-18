# testes

## Visão geral

O agente escreveu 11 métodos de teste em dois arquivos. A maior parte é teste de ponta a ponta em `src/test/java/shop/ShopTest.java`, que sobe o serviço de verdade (`Main.wire`) sobre um H2 em memória no modo PostgreSQL e usa um dublê da TransLog com o relógio fixo (`TestApp.java`). O cliente HTTP da TransLog tem um teste próprio contra um servidor falso local (`TransLogClientTest.java`). A medida por mutação rodou (`pit.txt`: `situation` = `ok`). Os números são bons: 0,91 de cobertura de linhas, 0,853 de mutation score (279 dos 327 erros plantados foram pegos) e 0,924 de test strength. Os pontos fortes:

- os sete exemplos do financeiro são conferidos campo a campo;
- o pagamento repetido é testado em sequência e também ao mesmo tempo;
- há um teste de corrida pela última peça;
- o tempo é controlado e cada teste tem o seu próprio banco.

Os problemas, do mais grave ao menos grave:

1. Nenhum teste confere que o resumo e o pedido registrado dão os mesmos valores, uma exigência explícita do enunciado.
2. Faltam cinco das recusas de validação (região, nível, CEP, entrega e pagamento inválidos) e algumas transições proibidas.
3. Os testes de concorrência provam o travamento no H2, não no PostgreSQL. Os caminhos de código só do PostgreSQL nunca rodam nos testes.
4. Há dois testes fracos: um que confere a própria JDK e outro de "reinício" que não guarda nada antes de reiniciar.

## Item a item

**T1, o que dos requisitos do enunciado os testes cobrem?**

- **O cálculo está coberto.** `ShopTest.java:67-90` (`examples`) roda os exemplos 1 a 7 e compara o JSON inteiro do resumo (`assertSummary`, `ShopTest.java:54-61`). Isso inclui o teto de 30% do cupom (exemplos 5 e 6), os dias a mais do Norte (exemplos 1 e 7), o GOLD sem frete com brinde (exemplo 7), os juros Price (exemplo 2) e a tarifa do boleto (exemplos 3 e 6). A lacuna: o cálculo só é conferido em `POST /checkout/summary`. O registro (`register`, `ShopTest.java:132-138`) confere só o 201 e o `AWAITING_PAYMENT`, nunca o `summary` do pedido. Por isso, nada pega o defeito que o enunciado cita do sistema antigo (o resumo mostrar um valor e o pedido registrar outro).
- **As recusas estão cobertas em parte.** `refusals` (`ShopTest.java:97-130`) confere o corpo exato `{"error":...}` e o HTTP de:
  - `COUPON_NOT_APPLICABLE` (o exemplo 8);
  - `SHIPPING_METHOD_UNAVAILABLE`, tanto pelo CEP quanto pelo motoboy acima de 5 kg;
  - `CARRIER_UNAVAILABLE`;
  - `INVALID_COUPON` com o texto vazio;
  - `PAYMENT_METHOD_UNAVAILABLE`, `INSUFFICIENT_STOCK`, `UNKNOWN_PRODUCT`, `PRODUCT_DISCONTINUED`, `INVALID_ORDER` (SKU repetido e corpo inválido) e `INVALID_INSTALLMENTS`;
  - os quatro 404.

  Alguns casos conferem a ordem entre as recusas: `REL-001`+`XXX` → `UNKNOWN_PRODUCT` antes de `PRODUCT_DISCONTINUED`, na linha 117, e o estoque antes do peso do motoboy, na linha 113. `welcomeCouponOnlyForFirstPurchase` (`ShopTest.java:205-213`) cobre o exemplo 9. Não há teste para `INVALID_CLUB_TIER`, `INVALID_REGION`, `INVALID_ZIP_CODE`, `INVALID_SHIPPING_METHOD` e `INVALID_PAYMENT_METHOD`, nem para o MINUS50 abaixo de R$ 300,00 (o `grep` por esses códigos no `src/test` não acha nada).
- **O ciclo do pedido está coberto.** `paymentLifecycleAndIdempotency` (`ShopTest.java:152-172`) leva o pedido de pago a enviado e depois a entregue. No caminho, confere o `trackingCode`, o envio sem código (→ 422) e o cancelamento depois do envio (→ 409, na linha 170). `cancelPaidOrderGivesEverythingBack` (`ShopTest.java:174-189`) confere a devolução do estoque (48 → 50) e do crédito (7,99 → 0,00) e os avisos. `expiryBoundaryAndReplayOfRefusal` (`ShopTest.java:191-203`) confere o limite de exatamente 48 horas (aceito), 48 horas e 1 segundo (`ORDER_EXPIRED`) e o pagamento num pedido cancelado (409). Faltam o envio de um pedido não pago, a entrega de um pedido não enviado e o cancelamento manual de um pedido que ainda aguarda o pagamento.
- **O pagamento repetido está coberto, e bem.** Em sequência, `ShopTest.java:159-162` confere a mesma resposta byte a byte, o estoque 48 e o crédito 3,20. O outro `transactionId` dá 409 (linha 163). Ao mesmo tempo, `duplicateNoticesConcurrentlyApplyOnce` (`ShopTest.java:241-255`) manda cinco avisos iguais em paralelo e confere estoque 49 e crédito 1,60. A repetição de um pagamento recusado também é testada (`ShopTest.java:196-197`).
- **A concorrência está coberta.** `lastPieceIsSoldOnlyOnce` (`ShopTest.java:215-239`) faz seis pagamentos em paralelo pela `MOC-001`, que tem estoque 1. O teste exige exatamente um 200, que os outros sejam `409 INSUFFICIENT_STOCK` e que o estoque termine em 0.
- **A transportadora está coberta.** No serviço, o dublê responde vazio (CEP não atendido → 422) ou lança `UnavailableException` (→ 503) (`ShopTest.java:101-108`). No cliente, `TransLogClientTest.java:105-148` confere:
  - a montagem da URL (`zip=01310100&weight_g=1800&service=EXP`);
  - a conversão dos centavos e o arredondamento das horas para cima;
  - o `ZIP_NOT_SERVED`, outro erro, um campo faltando e o HTTP 500;
  - o tempo esgotado.

As seis áreas têm teste que confere o resultado. As lacunas ficam dentro delas, e a mais séria é a igualdade entre o resumo e o pedido.
Conclusão: segue

**T2, as asserções conferem valores?** Em geral, sim:

- os resumos são comparados como o JSON inteiro, o que pega qualquer centavo errado (`ShopTest.java:57-60`);
- as recusas conferem o corpo exato e o HTTP (`ShopTest.java:92-95`);
- os efeitos do pagamento e do cancelamento conferem números: o estoque em `ShopTest.java:161, 178, 181, 238, 253` e o saldo em `162, 179, 182, 254`;
- as situações e os avisos conferem o tipo e a ordem (`ShopTest.java:183-185`).

Há algumas asserções fracas:

- `ShopTest.java:209` confere só o status 200 do resumo com WELCOME10, sem os valores;
- `ShopTest.java:194` confere só o 200 do pagamento no limite exato das 48 horas;
- `catalogSurvivesRestartOnSameDatabase` (`ShopTest.java:257-264`) manda um `POST /orders` com `{}`, que é recusado e não grava nada, e depois só confere o estoque inicial. Ele prova que o `Schema.init` pode rodar de novo sem quebrar, não que algo gravado sobrevive ao reinício;
- `sanityInstant` (`ShopTest.java:266-269`) confere o `Instant.parse` da JDK e não toca no código do agente. Ele infla a contagem de testes de 10 para 11.

A proporção fica em cerca de 9 dos 11 métodos conferindo valores do negócio. O PIT confirma a leitura: a cobertura de linhas (0,91) e o mutation score (0,853) andam juntos, e a test strength é de 0,924. Dos 302 erros plantados em linhas executadas, só 23 passaram, ou seja, quem executa o código também confere o resultado.
Conclusão: segue

**T3, os testes rodam sem banco e sem internet, como o enunciado pede?** Sim:

- **O banco** é o H2 em memória no modo PostgreSQL, com um nome aleatório (`TestApp.java:42`), sobre o qual roda o próprio `Schema.init` (`TestApp.java:45`). O H2 tem escopo `test` no `pom.xml`. Não há Testcontainers, Docker nem PostgreSQL em lugar nenhum do `src/test`.
- **A TransLog** é um lambda que implementa `CarrierGateway` dentro do serviço (`TestApp.java:51`). O teste do cliente usa um `com.sun.net.httpserver.HttpServer` local na porta 0 (`TransLogClientTest.java:109-125`).

Há uma ressalva, que não fere o item mas limita o que os testes provam: o código de produção tem caminhos diferentes para o PostgreSQL e para o H2. É o caso do `pg_advisory_lock` (`src/main/java/shop/db/Schema.java:120-139`) e do `ON CONFLICT` (`src/main/java/shop/db/Dao.java:93-102`). Os caminhos do PostgreSQL nunca rodam nos testes. Além disso, as garantias de concorrência (o `FOR UPDATE` em `Dao.java:64` e `Dao.java:166`) são verificadas com os travamentos do H2, não com os do PostgreSQL de produção.
Conclusão: segue

**T4, os testes dependem de coisas que mudam sozinhas?** Em geral, não:

- **O relógio** é um `Clock` próprio que devolve um `Instant` fixo (`TestApp.java:32, 52-56`). O código principal não usa `Instant.now()` (o `grep` só acha o `currentTimeMillis` do retry de subida em `Main.java:52, 58`). O limite das 48 horas é testado mudando o `paidAt` contra o `createdAt` fixo, de forma determinística (`ShopTest.java:194-196`).
- **O estado** é isolado: cada teste cria um app novo com um banco H2 novo (`@BeforeEach`, `ShopTest.java:21-24`, mais o UUID em `TestApp.java:42`). Os CPFs também são distintos entre os testes. Não há dependência de ordem.

Há dois pontos menores:

- `TransLogClientTest.java:113-114` usa `Thread.sleep(3000)` no servidor falso para provocar o limite de 2 segundos do cliente. Isso amarra o teste ao tempo de verdade (ele leva cerca de 2 segundos), mas a folga de 1 segundo torna improvável que ele falhe por acaso. Para testar um timeout, é o meio usual.
- Os dois testes de concorrência dependem do escalonamento real das threads (`ShopTest.java:221-226, 244-248`). As asserções são invariantes (um sucesso só, estoque 0), então não oscilam. Mas nada garante que as requisições se sobreponham de fato, e numa execução em que elas se serializem o teste passa sem exercitar a corrida.
Conclusão: segue

## O que mudaria primeiro

1. **Registrar o pedido e conferir o `summary`.** Basta, em `examples`, registrar o pedido com cada um dos sete exemplos via `POST /orders` e comparar o `summary` do pedido com o do `POST /checkout/summary`. É a exigência mais explícita do enunciado sobre o cálculo ("já aconteceu de o resumo mostrar um valor e o pedido registrar outro") e hoje não tem teste nenhum.
2. **Completar as recusas e as transições que faltam.** As recusas são `INVALID_CLUB_TIER`, `INVALID_REGION`, `INVALID_ZIP_CODE`, `INVALID_SHIPPING_METHOD`, `INVALID_PAYMENT_METHOD` e o MINUS50 abaixo de R$ 300,00, de preferência com casos que fixem a ordem entre elas. As transições são o envio sem pagar, a entrega sem enviar e o cancelamento de um pedido aguardando o pagamento. O `sanityInstant` deveria ser trocado por algo útil, e o teste de reinício deveria gravar um pedido pago antes de subir a segunda instância.

## Dúvidas

- No T1, as seis áreas têm teste que confere o resultado, mas há lacunas dentro de duas delas (as recusas e as transições proibidas) e fora da lista (o resumo igual ao pedido). Li o item como "as áreas estão cobertas?" e concluí `segue`, registrando as lacunas no texto. Uma leitura mais exigente, que pedisse todas as recusas da tabela, daria `em parte`.
- No T3, a régua pede que os testes rodem sem PostgreSQL e sem Docker, e isso eles cumprem. Contei a troca pelo H2, com caminhos de código diferentes para cada banco, como uma ressalva e não como uma falha do item.
