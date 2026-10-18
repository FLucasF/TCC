# testes

## Visão geral

O agente escreveu uma única classe de teste de integração, `src/test/java/br/com/loja/pedidos/PedidosApiTest.java`, com 14 métodos `@Test`. Ela sobe o contexto Spring inteiro sobre um H2 em memória e usa um servidor HTTP falso no lugar da TransLog. Todas as chamadas passam pela API, com MockMvc.

O ponto mais forte é que os sete exemplos do financeiro estão nos testes, cada um conferindo os onze campos do resumo. O ciclo do pedido, o pagamento repetido e os casos da TransLog que respondem (CEP não atendido e erro HTTP) também estão cobertos. Os números do PIT batem com essa leitura: `situation=ok`, 263 erros plantados, 251 pegos, `mutation_score` 0,954, `test_strength` 0,969, `line_coverage` 0,95 (`pit.txt`, linha `exec-06`).

Os problemas, do mais grave para o menos grave:
1. **A concorrência não tem teste.** A disputa pela última peça só é testada em sequência.
2. **O pedido registrado nunca é comparado ao resumo.** É justamente a divergência que o enunciado pede para evitar.
3. **O estado é compartilhado entre os testes.** O banco H2 nunca é zerado, e a TransLog falsa é configurada por variáveis estáticas. Hoje isso funciona, mas é frágil.
4. **Algumas asserções conferem só o status HTTP**, ou só o código de erro sem o status.

## Item a item

**T1, o que dos requisitos do enunciado os testes cobrem?**

- **O cálculo está coberto.** Os testes `example1` a `example7` (`src/test/java/br/com/loja/pedidos/PedidosApiTest.java:123-177`) chamam `POST /checkout/summary` com os dados exatos dos exemplos 1 a 7. O `expectSummary` (`:104-119`) confere os 11 campos: subtotal, cupom, frete, prazo, seguro, ajuste, total final, parcelas, valor da parcela, crédito e brinde.
  - Entram assim os quatro cupons, as três formas de pagamento (com juros em `example2`), o limite de 30% (`example5`, `example6`), o seguro com teto (`example7`) e os dias a mais do Norte (`example1`, `example7`).
  - Os dias a mais do Nordeste não estão cobertos: só aparecem em `example3`, que é motoboy, onde esses dias não se aplicam.
  - Esse cálculo só é conferido no resumo. Em `POST /orders`, o `register` (`:93-97`) confere só o 201 e lê o `id`; o `summary` do pedido gravado não é comparado com o do resumo em nenhum teste.
- **As recusas estão cobertas.** Os testes `example8`, `example9AndPaidOrderKillsWelcomeCoupon`, `example10AndCarrierDown` e `validationOrder` (`:179-232`) cobrem 10 das 15 recusas do resumo: `INVALID_ORDER`, `UNKNOWN_PRODUCT`, `PRODUCT_DISCONTINUED`, `INSUFFICIENT_STOCK`, `INVALID_COUPON` (com o texto vazio, `:224`), `INVALID_INSTALLMENTS`, `PAYMENT_METHOD_UNAVAILABLE`, `SHIPPING_METHOD_UNAVAILABLE` (motoboy acima de 5 kg, `:230`), `COUPON_NOT_APPLICABLE` e `CARRIER_UNAVAILABLE`.
  - As linhas `:216-223` também mandam `"PIX"` como forma de entrega. Com isso, sem dizer, conferem a prioridade dos itens 1 a 4 sobre o item 8.
  - Não têm teste: `INVALID_CLUB_TIER`, `INVALID_REGION`, `INVALID_ZIP_CODE`, `INVALID_SHIPPING_METHOD` e `INVALID_PAYMENT_METHOD`.
  - Nas operações do pedido há `ORDER_EXPIRED`, `OPERATION_NOT_ALLOWED`, `INSUFFICIENT_STOCK` 409 e `ORDER_NOT_FOUND`. Há ainda `NOTIFICATION_NOT_FOUND`, `CUSTOMER_NOT_FOUND` e `PRODUCT_NOT_FOUND` (`:314-318`).
- **O ciclo do pedido está coberto**, com lacunas pequenas. O teste `expirationAndShipping` (`:274-301`) confere:
  - enviar antes de pagar dá 409 (`:282`);
  - pagar com 48 h + 1 s dá `ORDER_EXPIRED`, e o pedido fica `CANCELLED` (`:283-286`);
  - pagar com exatamente 48 h é aceito (`:291-292`);
  - enviar sem `trackingCode` dá 422 (`:293`);
  - o envio grava o rastreio (`:294-295`);
  - cancelar depois do envio dá 409 (`:296`);
  - a entrega deixa o pedido em `DELIVERED` (`:297-298`).

  O teste `paymentLifecycleIsIdempotentAndStockNeverGoesNegative` cobre o cancelamento de um pedido pago, com o estoque e o crédito voltando (`:262-265`). O `notificationsCanBeReadAndUnknownResourcesAre404` cobre o cancelamento de um pedido não pago (`:308`). Ficam de fora: entregar sem ter enviado, pagar um pedido cancelado e o próprio status `SHIPPED`.
- **O pagamento repetido está coberto.** Em `:244-251`, o mesmo `TX-A` é enviado duas vezes e as duas respostas são 200 com `PAID`. O estoque da `MOC-001` fica em 0, o saldo em 3,20 (2% de 159,90, uma vez só), e um `TX-B` diferente dá `OPERATION_NOT_ALLOWED`. A repetição de uma recusa devolve a mesma recusa (`:255-258`).
- **A concorrência não está coberta.** O comentário em `:253` fala em "última peça", mas os dois pagamentos (`first` e `second`) são feitos um depois do outro, na mesma thread. Nenhum teste dispara duas compras ao mesmo tempo. Além disso, o banco dos testes é o H2, cuja trava com `FOR UPDATE` (`ProductRepository.java:27`, `OrderRepository.java:52`) não se comporta como a do PostgreSQL.
- **A transportadora está coberta.** O teste `example10AndCarrierDown` (`:201-212`) confere o `ZIP_NOT_SERVED` (422 `SHIPPING_METHOD_UNAVAILABLE`) e a resposta HTTP 500 (503 `CARRIER_UNAVAILABLE`). O limite de 2 segundos e uma resposta malformada não têm teste. O servidor falso responde igual para qualquer consulta (`:40-45`), então nenhum teste confere o `zip`, o `weight_g` ou o `service` (`STD`/`EXP`) que o serviço envia.

Cinco das seis áreas estão cobertas, e quatro delas bem. A concorrência, que é um requisito central do enunciado, não tem teste, e falta comparar o pedido registrado com o resumo.
Conclusão: em parte

**T2, as asserções conferem valores?** Sim, na grande maioria das vezes.
- Os sete testes de cálculo comparam valores exatos dos 11 campos (`:107-118`).
- Os testes do ciclo conferem o que mudou no negócio: o `status` do pedido (`:245`, `:259`, `:263`, `:286`, `:298`), o estoque em números (`:250`, `:264`), o saldo de crédito (`:251`, `:265`), os avisos com quantidade, tipo e status (`:266-270`) e o rastreio (`:295`).
- As recusas são conferidas pelo campo `error`.

Os pontos fracos são secundários:
- **Só o status HTTP é conferido** em `:194` (resumo aceito), `:282`, `:293` e `:296` (409 ou 422 sem o código do erro) e `:308` (cancelamento sem conferir a situação).
- **Só o código de erro é conferido, sem o status HTTP**, em `:218-231`.
- **A comparação com `Double.parseDouble`** (`:108`) não distingue `3.2` de `3.20`. Por isso, o formato de duas casas decimais pedido no enunciado não é conferido.

Os números do PIT confirmam que os testes conferem e não só executam: a cobertura de linhas é 0,95 e a taxa de erros pegos é 0,954, com 0,969 entre os erros em linhas executadas. Sobram só 8 erros que passaram e 4 em linhas que nenhum teste executa.
Conclusão: segue

**T3, os testes rodam sem banco e sem internet, como o enunciado pede?** Sim.
- **O banco** é trocado por um H2 em memória, em modo PostgreSQL, pelo `@DynamicPropertySource` (`:61-63`). O H2 entra no `pom.xml` com escopo `test`, e as migrações do Flyway rodam sobre ele.
- **A TransLog** é um `com.sun.net.httpserver.HttpServer` que escuta em `127.0.0.1`, numa porta livre (`:39-46`). O endereço dele é injetado em `translog.url` (`:60`), e o teste passa pelo cliente HTTP de verdade (`HttpTransLogClient`).
- Nenhum teste usa PostgreSQL, Docker, Testcontainers ou a internet. Também não há outros arquivos em `src/test/resources`.

Um custo dessa escolha: o SQL e as travas são exercitados num banco diferente do de produção. Isso não fere o item, mas explica por que a concorrência não poderia ser medida com fidelidade nesse arranjo.
Conclusão: segue

**T4, os testes dependem de coisas que mudam sozinhas?**
- **O relógio está bem resolvido, por um caminho indireto.** O código principal injeta um `Clock` (`OrderService.java:36`, `PedidosApplication.java:17-18`), mas os testes não o trocam. Isso não importa aqui, porque a regra das 48 horas compara o `paidAt` recebido com o `createdAt` gravado (`OrderService.java:80`), e o teste lê o `createdAt` devolvido pela API para montar `createdAt + 48h` e `createdAt + 48h + 1s` (`:278-291`). A fronteira fica, portanto, determinística.
- **O relógio de verdade aparece só onde não pesa.** O `pay` (`:100`) e o teste de pagamento (`:241`) usam `Instant.now()` como `paidAt`, mas ele sempre cai dentro da janela de um pedido que acabou de ser criado.
- **Não há `sleep`.**
- **O estado é compartilhado:**
  - **O banco não é zerado.** Os 14 testes usam um único contexto e um único H2 nomeado, com `DB_CLOSE_DELAY=-1` (`:61`), que nunca é limpo. O catálogo vai mudando ao longo da execução: `example9…` paga duas `CAM-001` (`:195`), e `expirationAndShipping` paga uma `MEI-001` (`:291`). O teste de pagamento confere o estoque da `MOC-001` em números absolutos, 0 e 1 (`:250`, `:264`). Isso só funciona porque nenhum outro teste mexe nesse produto. Na mesma linha, a recusa de `PAYMENT_METHOD_UNAVAILABLE` com três `JAQ-001` (`:228`) depende de ninguém ter pago uma jaqueta antes.
  - **A TransLog falsa depende de variáveis estáticas que podem mudar** (`translogBody` e `translogStatus`, `:33-34`). O `example10AndCarrierDown` põe o status em 500 e só o devolve a 200 na última linha (`:208-211`), sem `finally`. Se uma asserção do meio falhar, o 500 fica para os testes seguintes, embora o `carrier()` (`:76`) limpe o status nos testes que o chamam.
- **O que é bom:** cada teste cria um CPF novo por meio de um contador (`:35`, `:71-73`), o que isola clientes, crédito, avisos e o `WELCOME10`.

Pela leitura, hoje nenhum teste depende da ordem em que os testes rodam, mas os estoques são de todos. Um teste novo que pague `MOC-001`, `JAQ-001` ou `TEN-001` quebraria outro teste sem ter relação com ele.
Conclusão: em parte

## O que mudaria primeiro

1. **Testar a concorrência e comparar o pedido com o resumo.**
   - Um teste dispararia dois pagamentos ao mesmo tempo, em duas threads, para a última `MOC-001`, e conferiria que um sai `PAID`, o outro `INSUFFICIENT_STOCK`, e o estoque termina em 0. O ideal seria rodá-lo num banco com as travas do PostgreSQL; sem Docker, isso exige ao menos documentar a limitação do H2.
   - Outro teste registraria um pedido e compararia o `summary` dele, campo a campo, com o do `POST /checkout/summary`.

   São os dois requisitos que o próprio dono da loja diz terem falhado no sistema antigo, e hoje nenhum teste os protege.
2. **Isolar o estado.**
   - Restaurar o catálogo antes de cada teste, ou fazer as asserções de estoque relativas ao valor lido antes.
   - Pôr a troca da TransLog para "fora do ar" num `try/finally`, ou reiniciar a TransLog falsa num `@BeforeEach`.

   Isso tira a fragilidade de testes que hoje só passam porque ninguém mais toca nos mesmos produtos.

## Dúvidas

- Em T1, a transportadora fora do ar foi testada só com HTTP 500, sem o limite de 2 segundos. Dei a área como coberta, porque a régua pede "a TransLog fora do ar **ou** o CEP que ela não atende", e os dois casos estão lá.
- Em T4, não ter um relógio controlado (o `Clock` injetado nunca é trocado nos testes) não contou como problema. A regra das 48 horas não depende do "agora" do servidor, e o teste monta a fronteira a partir do `createdAt` devolvido. Nessa leitura, o tempo está, de fato, controlado.
