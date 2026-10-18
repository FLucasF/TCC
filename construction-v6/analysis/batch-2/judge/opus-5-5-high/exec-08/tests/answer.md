# testes

## Visão geral

O agente escreveu 10 métodos de teste em três classes. Todos sobem o Javalin de verdade numa porta livre, com um armazenamento em memória (`MemoryStore`) e uma transportadora falsa, e fazem chamadas HTTP ao serviço. Uma quarta classe testa o cliente da TransLog contra um servidor HTTP local. O ponto mais forte é o cálculo: os sete exemplos do financeiro estão conferidos campo a campo. O pagamento repetido e a fronteira das 48 horas também estão bem testados. Na medida por mutação (`pit.txt`, `situation=ok`), os testes pegam 0,917 dos erros que executam (`test_strength`), o que mostra asserções sobre valores. O problema mais sério é que o armazenamento de produção, `JdbcStore` (431 das 1737 linhas do código principal), não é executado por teste nenhum. Por isso a concorrência, que ali depende de `SELECT ... FOR UPDATE`, nunca é exercida. O teste da "última peça" é sequencial e roda sobre um armazenamento que só aceita uma transação por vez. Isso ajuda a explicar a cobertura de linhas de só 0,64 e os 109 erros plantados em linhas que nenhum teste executa. Em segundo lugar: metade das recusas do resumo não tem teste, e alguns testes conferem só o status 409, sem o código do erro.

## Item a item

**T1, o que dos requisitos do enunciado os testes cobrem?**
- **O cálculo: coberto.** `CheckoutExamplesTest.java:39-75` roda os exemplos 1 a 7 em `POST /checkout/summary` e confere os 11 campos do resumo em cada um (`check`, linhas 19-35). Isso inclui o teto de 30% do cupom (exemplos 5 e 6), os dias a mais no Norte (prazos 5 e 6) e o seguro no limite (30,00). Ficam de fora:
  - nenhum teste confere que o `summary` do pedido registrado dá os mesmos valores do resumo, uma exigência explícita do enunciado. `OrderLifecycleTest.java:25-29` só olha `status` e `createdAt`;
  - o desconto do WELCOME10 na primeira compra nunca é conferido em valor: `OrderLifecycleTest.java:102` só olha o status.
- **As recusas: em parte.** `CheckoutExamplesTest.java:77-112` confere pelo código COUPON_NOT_APPLICABLE (exemplo 8), SHIPPING_METHOD_UNAVAILABLE (exemplo 10), CARRIER_UNAVAILABLE com 503, PRODUCT_DISCONTINUED, INSUFFICIENT_STOCK, INVALID_COUPON (o texto vazio) e INVALID_ORDER. Dois casos conferem a ordem das verificações: produto fora de linha com cupom vazio dá PRODUCT_DISCONTINUED (linhas 97-99), e falta de estoque com cupom vazio dá INSUFFICIENT_STOCK (linhas 101-103). `OrderLifecycleTest.java:99-106` cobre o exemplo 9. `OrderLifecycleTest.java:110-121` cobre ORDER_NOT_FOUND, PRODUCT_NOT_FOUND, CUSTOMER_NOT_FOUND, NOTIFICATION_NOT_FOUND, INVALID_ORDER no pagamento e OPERATION_NOT_ALLOWED. Não têm teste: UNKNOWN_PRODUCT, INVALID_CLUB_TIER, INVALID_REGION, INVALID_ZIP_CODE, INVALID_SHIPPING_METHOD, o motoboy acima de 5 kg, o MINUS50 abaixo de R$ 300, INVALID_PAYMENT_METHOD, INVALID_INSTALLMENTS, o boleto acima de R$ 1.000 e o envio sem `trackingCode`.
- **O ciclo do pedido: coberto.**
  - O caminho todo, pagar, enviar e entregar, com o código de rastreio, está em `OrderLifecycleTest.java:32-50`.
  - Transições proibidas:
    - entregar um pedido pago (linha 45);
    - cancelar um pedido enviado (linha 49);
    - enviar sem pagar (linha 119);
    - pagar de novo com outro `transactionId` (linhas 41-43).
  - Cancelar um pedido pago devolve o estoque e tira o crédito (linhas 55-66).
  - A expiração está testada dos dois lados: aceita com exatamente 48 h (linha 32, pago em `2026-10-16T15:30:00Z` sobre `createdAt` `2026-10-14T15:30:00Z`) e recusa com 48 h e 1 s (linhas 73-76).
  - Falta só o cancelamento direto de um pedido ainda não pago.
- **O pagamento repetido: coberto.** `OrderLifecycleTest.java:35-39` repete o aviso `TX-1` e confere que o estoque ficou em 3 (5 − 2, baixado uma vez só), que o saldo é 19,99 (um crédito só) e que há um único aviso. As linhas 77-79 conferem que repetir um aviso recusado devolve a mesma recusa, ORDER_EXPIRED, e não mexe no estoque.
- **A concorrência: não coberta.** `lastPieceIsSoldOnlyOnce` (`OrderLifecycleTest.java:84-95`) paga os dois pedidos da última mochila **um depois do outro**. O teste confere a falta de estoque na hora do pagamento, mas não duas compras ao mesmo tempo. Mesmo que fossem simultâneas, o `MemoryStore` serializa tudo com uma trava global (`MemoryStore.java:28-35`), e `lockProducts` e `lockOrder` não travam nada (linhas 42 e 61). A proteção de verdade está no `JdbcStore` (`FOR UPDATE` em `JdbcStore.java:196` e `:222`), e nenhum teste executa essa classe.
- **A transportadora: coberta.** `TransLogGatewayTest.java` sobe um servidor HTTP local e testa:
  - a cotação convertida para reais (33,10 e 41 h, linhas 39-41);
  - ZIP_NOT_SERVED dando vazio (linha 42);
  - status 500, outro `error`, resposta lenta além dos 2 s e URL ausente dando "fora do ar" (linhas 55-58).
  - A ponta da API está em `CheckoutExamplesTest.java:85-95`.
  - Fica de fora a consulta que o serviço manda: o servidor falso ignora a requisição (`TransLogGatewayTest.java:16-25`), e o dublê da API ignora os argumentos (`TestApp.java:30-33`). Nenhum teste confere `weight_g` em gramas nem `service=STD/EXP`.

Estão cobertos o cálculo, o ciclo, o pagamento repetido e a transportadora. As recusas estão cobertas em parte, e a concorrência não está.
Conclusão: em parte

**T2, as asserções conferem valores?** Na grande maioria, sim:
- os valores do resumo, com `toPlainString()`, o que confere também as duas casas decimais (`CheckoutExamplesTest.java:24-34`);
- o estoque e o saldo depois das operações (`OrderLifecycleTest.java:37-38, 60-61, 79, 92`);
- a situação do pedido depois de cada passo (linhas 34, 47, 50, 76, 93);
- os códigos das recusas.

Os números confirmam: `test_strength` 0,917, com 26 erros que sobreviveram contra 286 pegos entre os executados. Isso não combina com testes que só executam código. Há alguns pontos fracos:
- `OrderLifecycleTest.java:30` (`assertEquals(200, ... status() == 200 ? 200 : 0)`) e `:102` (`assertEquals(201, ... == 200 ? 201 : 0)`) só conferem status, e de um jeito torto. A linha 102 deveria conferir o desconto do WELCOME10;
- `:94` usa `assertTrue(size >= 2)` para os avisos depois do cancelamento por falta de estoque, sem conferir que existe um ORDER_CANCELLED;
- `:45` e `:49` conferem só o 409, sem o código;
- `CheckoutExamplesTest.java:97-110` confere o código, mas não o HTTP 422.

São poucas e não pesam no conjunto.
Conclusão: segue

**T3, os testes rodam sem banco e sem internet, como o enunciado pede?** Sim, todos rodam sem banco e sem internet:
- o banco é resolvido com o `MemoryStore` (`TestApp.java:25`; a classe está em `src/main`, `MemoryStore.java:15`, com o comentário "usado nos testes (sem banco)");
- a TransLog na API é uma lambda que implementa `CarrierGateway` (`TestApp.java:30-33`);
- o cliente HTTP da TransLog é testado contra um `com.sun.net.httpserver.HttpServer` em `localhost`, porta 0 (`TransLogGatewayTest.java:14-28`);
- o `pom.xml` não tem PostgreSQL de teste, H2 nem Testcontainers, e nenhum teste lê `DB_URL` ou `TRANSLOG_URL`.

A consequência, já contada em T1, é que o SQL de produção fica sem teste. Isso é lacuna de cobertura, não violação deste item.
Conclusão: segue

**T4, os testes dependem de coisas que mudam sozinhas?**
- **O tempo é controlado.** `TestApp.java:34` passa `Clock.fixed(now, ZoneOffset.UTC)` com o `NOW` constante, e o código principal só lê a hora pelo `Clock` injetado (`OrderService.java:35`; o `Clock.systemUTC()` aparece só em `Main.java:15`). Os `paidAt` são literais, de modo que as 48 horas são exatas e reproduzíveis.
- **Cada teste monta o seu estado.** Cada método cria um `TestApp` novo, com um `MemoryStore` novo e porta 0 (`try (TestApp app = new TestApp(NOW))` em todos os testes). Os ids de aviso recomeçam em 1 a cada armazenamento, então `/notifications/9/read` dá NOTIFICATION_NOT_FOUND independentemente da ordem dos testes.
- **Um `sleep` de verdade.** `TransLogGatewayTest.java:18` tem um `Thread.sleep(delayMs)` no servidor falso, com 2600 ms (linha 53), para simular a TransLog lenta contra o limite de 2 s. Não é um `sleep` para esperar um resultado, e o desfecho não oscila: a resposta nunca chega antes dos 2 s. Mas o teste depende do relógio de verdade e custa uns 2,6 s.
- **Estado mutável compartilhado.** `carrierAnswer` e `carrierDown` mudam ao longo de um mesmo teste (`CheckoutExamplesTest.java:42-69, 85-91`), sempre de forma sequencial, sem risco.

Conclusão: segue

## O que mudaria primeiro

1. **Testar o `JdbcStore` e uma disputa de verdade pela última peça.** Rodar a mesma bateria contra o armazenamento JDBC num banco em memória compatível (H2 em modo PostgreSQL, por exemplo, se o SQL permitir) e disparar dois pagamentos da `MOC-001` em threads ao mesmo tempo. Hoje a parte mais arriscada do serviço não tem teste nenhum: as travas `FOR UPDATE`, o `ON CONFLICT` da idempotência e o `CHECK (stock >= 0)`. O teste de concorrência atual passaria até com um código sem trava nenhuma.
2. **Completar as recusas do resumo e conferir o pedido registrado.** Um teste parametrizado com uma linha por código da tabela (as 15 situações, com o HTTP) e um teste que compara o `summary` de `POST /orders` com o de `POST /checkout/summary`. Seria o jeito barato de cobrir as faltas listadas em T1, de proteger a ordem das verificações e de proteger a exigência de que os dois resumos sejam iguais.

## Dúvidas

- `pit.txt` só traz os totais. A atribuição dos 109 erros sem cobertura e da linha de 0,64 principalmente ao `JdbcStore` e ao `Main` é uma leitura minha, pelo tamanho das classes e pelo fato de nenhum teste instanciá-las. Não é um dado da medida.
- Na concorrência, a régua pede "duas compras ao mesmo tempo". Como o teste é sequencial, li a área como não coberta, e não como coberta em parte, apesar de ele conferir a regra da falta de estoque no pagamento.
- O `sleep` do servidor falso simula a lentidão; ele não espera um resultado. Contei-o como observação em T4, sem rebaixar a conclusão.
