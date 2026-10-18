# testes

## Visão geral

O agente escreveu uma única classe de teste, `src/test/java/shop/CheckoutExamplesTest.java`, com 16 métodos. Todos montam a aplicação pela fábrica `Application.create`, com um armazenamento em memória (`MemoryStore`), uma TransLog falsa escrita como lambda e um relógio fixo. O ponto mais forte é o cálculo. Os sete exemplos do financeiro com valores conferem os onze campos do resumo, um a um, com `BigDecimal` exato. O pagamento repetido, o estorno do cancelamento e as 48 horas também são conferidos pelos valores de negócio: estoque, saldo, número de avisos e código da recusa.

Os buracos, do mais grave ao menos grave:

1. A "concorrência" é testada em sequência, sem duas compras ao mesmo tempo. O mecanismo que de fato impede a venda dupla em produção, o `FOR UPDATE` no PostgreSQL, nunca é executado.
2. A TransLog fora do ar (`CARRIER_UNAVAILABLE`) e o cliente HTTP da TransLog não têm teste.
3. Das 15 recusas do resumo, só 3 códigos aparecem (4 testes). Nenhuma recusa de validação do carrinho é testada, nem a ordem das conferências.
4. A camada web (rotas, situações HTTP, JSON) e a camada PostgreSQL não são executadas.

O PIT confirma esse retrato (`pit.txt`, `exec-03`):
- `situation=ok`, 16 testes, 390 erros plantados;
- 195 pegos, 27 que passaram e 168 em linhas que nenhum teste executa;
- `mutation_score` 0,500, `test_strength` 0,878, `line_coverage` 0,52.

Ou seja: o que os testes executam, eles conferem bem; metade do código eles não executam.

## Item a item

**T1, o que dos requisitos os testes cobrem.**

- **O cálculo: coberto.** `example1` a `example7` (`src/test/java/shop/CheckoutExamplesTest.java:79-131`) usam o `assertSummary` (`:60-73`), que confere subtotal, desconto, frete, prazo, seguro, ajuste, total final, parcelas, valor da parcela, crédito e brinde. Juntos, os exemplos passam por:
  - todos os cupons com desconto: WELCOME10, MINUS50, BUY3PAY2 e FREESHIPPING com o teto de 30%;
  - as três formas de pagamento, com cartão sem juros e com juros;
  - as regiões com limite de seguro e com dias a mais;
  - o GOLD sem frete e com brinde.

  O que fica de fora: nenhum teste confere que o resumo do pedido registrado é igual ao do `POST /checkout/summary`, e o enunciado destaca justamente isso. O `register` é chamado (`:161`, `:167`), mas o `summary()` do pedido nunca é comparado.
- **As recusas: em parte.**
  - Cobertas: `COUPON_NOT_APPLICABLE` (`example8`, `:134-137`; `example9`, `:139-145`), `SHIPPING_METHOD_UNAVAILABLE` (`example10`, `:147-152`) e `INVALID_COUPON` para o texto vazio (`emptyCouponIsInvalid`, `:154-158`).
  - Nas operações do pedido: `OPERATION_NOT_ALLOWED`, `ORDER_EXPIRED`, `INSUFFICIENT_STOCK` (409) e `INVALID_ORDER` para envio sem rastreio (`:176-177`, `:204-205`, `:187-189`, `:228-229`).
  - Não cobertas: `INVALID_ORDER` do carrinho, `UNKNOWN_PRODUCT`, `PRODUCT_DISCONTINUED`, `INSUFFICIENT_STOCK` no resumo, os `INVALID_*` de nível, região, CEP, entrega, pagamento e parcelas, `PAYMENT_METHOD_UNAVAILABLE`, `ORDER_NOT_FOUND`, e a ordem em que as conferências acontecem.
- **O ciclo do pedido: em parte.**
  - Testado: enviar sem pagar é proibido (`:225-226`); pagar → enviar → entregar (`:227-231`); cancelar um pedido pago e cancelar de novo é proibido (`:213-218`); pagar com outro `transactionId` num pedido pago é proibido (`:176-177`).
  - Não testado: cancelar depois de enviado (proibido), entregar sem ter enviado, cancelar um pedido ainda não pago, e o aviso de envio.
- **O pagamento repetido: coberto.** `paymentDebitsStockAndCreditsOnce` (`:165-178`) repete o `TX-1` e confere estoque 0, saldo 3,20 e um aviso só. `lastPieceIsSoldOnlyOnce` (`:191-192`) confere que a recusa repetida devolve a mesma recusa.
- **A concorrência: não coberta de fato.** `lastPieceIsSoldOnlyOnce` (`:180-194`) paga os dois pedidos um depois do outro na mesma thread (`:186-187`), sem nada correndo em paralelo. Além disso, o `MemoryStore` serializa tudo com um único `ReentrantLock` (`src/main/java/shop/persistence/memory/MemoryStore.java:32,39-47`), e o seu `findForUpdate` é só um `find` (`:94-96`). A proteção que vale em produção, o `SELECT … FOR UPDATE` em `src/main/java/shop/persistence/postgres/PgProducts.java:64` e `PgOrders.java:53`, não passa por teste nenhum. O teste confere a regra "sem estoque na hora de pagar, o pedido é cancelado", não a disputa simultânea.
- **A transportadora: em parte.** O CEP não atendido está coberto (`example10`, pelo `ZipNotServedException` do dublê em `:41-46`). A TransLog fora do ar não está: o dublê nunca lança `CarrierDownException`, então o caminho `CARRIER_UNAVAILABLE` (`src/main/java/shop/domain/shipping/CarrierShipping.java:33-34`) fica sem teste. O cliente HTTP real também, com o tempo limite de 2 s, a leitura de `price_cents`/`eta_hours` e a resposta inesperada (`src/main/java/shop/domain/shipping/TransLogClient.java:12,30-53`).

Conclusão: em parte

**T2, as asserções conferem valores.** Quase todas conferem resultados de negócio:
- os 11 campos do resumo, com `BigDecimal` exato (`:62-72`);
- a situação do pedido depois da operação (`:171-172`, `:190`, `:206`, `:231`);
- o código da recusa e o HTTP 409 (`:188-189`);
- o estoque (`:173`, `:193`, `:212`, `:214`), o saldo (`:174`, `:215`) e o código de rastreio (`:230`).

Não há nenhum teste que só verifique "não deu exceção" ou "não é nulo". Restam fraquezas pontuais:
- os avisos são conferidos só pela quantidade (`:175`, `:216`), sem `type` nem `orderId`;
- em `cancellingPaidOrderReversesEverything`, o retorno de `cancel` é descartado (`:213`) e a situação `CANCELLED` só é deduzida pela segunda recusa (`:217-218`).

Os números do PIT batem com essa leitura. A `test_strength` de 0,878 (27 sobreviventes entre 222 executados) mostra que, onde há execução, as asserções pegam os erros plantados. O `mutation_score` de 0,500 vem dos 168 erros em código nunca executado, não de asserções frouxas.

Conclusão: segue

**T3, rodam sem banco e sem internet.** Sim.
- O banco é o `MemoryStore` (`:36-40`). A única dependência de teste no `pom.xml` é `junit-jupiter`: não há Testcontainers, driver em teste nem perfil de integração.
- A TransLog é uma lambda que implementa `CarrierClient` (`:41-46`), passada a `Application.create` (`:47`). Nenhum teste abre conexão de rede.

O lado ruim dessa escolha é que o código de PostgreSQL e o cliente HTTP não são exercitados nem com um banco em memória compatível ou um servidor HTTP falso local, que também rodariam sem Docker. Isso pesa no T1, não aqui. Um detalhe: o falso não desfaz alterações quando dá erro, como o próprio código admite (`MemoryStore.java:30`). Nos cenários testados isso não muda o resultado, porque o `tryDebit` em memória é tudo-ou-nada (`MemoryStore.java:60-69`).

Conclusão: segue

**T4, dependência de coisas que mudam sozinhas.** Não encontrei nenhuma.
- **Relógio:** é `Clock.fixed(NOW, UTC)` (`:47`), e o `createdAt` sai de `Instant.now(clock)` (`src/main/java/shop/service/CheckoutService.java:33`). O `paidAt` é passado explicitamente, e o teste das 48 horas usa a fronteira exata, `NOW + 48h` aceita e `NOW + 48h + 1s` recusada (`:200`, `:205`).
- **Espera:** não há `sleep` nem espera ativa.
- **Estado:** cada teste monta o seu no `@BeforeEach` (`:34-48`), com um `MemoryStore` novo e o catálogo recarregado. Os campos `carrierQuote` e `zipNotServed` são de instância e voltam ao padrão a cada método, porque o JUnit cria uma instância por teste.
- **Ordem:** nenhum teste depende da ordem de execução. Os ids aleatórios (UUID) não aparecem em nenhuma asserção.

Conclusão: segue

## O que mudaria primeiro

1. **Um teste de concorrência de verdade, contra a implementação que vai para produção.** Duas threads disparando o pagamento da `MOC-001` ao mesmo tempo, sobre um banco que rode no `mvn verify` sem Docker (por exemplo um PostgreSQL embutido ou um H2 em modo PostgreSQL, se o SQL permitir), conferindo estoque 0, um pedido `PAID` e o outro `CANCELLED`. Hoje o requisito que o enunciado mais destaca (a última peça vendida duas vezes) não tem nenhuma rede de proteção.
2. **Cobrir a TransLog e a camada web.**
   - Um servidor HTTP falso local para o `TransLogClient`: resposta 500, demora acima de 2 s e corpo inesperado, todos esperando `CARRIER_UNAVAILABLE`.
   - Alguns testes pelo Javalin, para as situações HTTP, o formato `{ "error": … }`, a ordem das 15 conferências e a igualdade entre o resumo e o pedido registrado.

   Isso atacaria direto os 168 erros plantados em código que nenhum teste executa.

## Dúvidas

- No T1, "o ciclo do pedido" e "as recusas" estão cobertos em parte, e a concorrência não está coberta de fato. O teste chamado `lastPieceIsSoldOnlyOnce` existe e confere a recusa, mas sem simultaneidade. Li o item da régua ("duas compras ao mesmo tempo") ao pé da letra e não contei esse teste como cobertura da concorrência.
- O T2 ficou em `segue` apesar do `mutation_score` de 0,500. A régua usa o PIT como sinal de asserções fracas quando a cobertura é alta e pega pouco. Aqui é o contrário: a cobertura é baixa e a força alta. Por isso atribuí a nota baixa à falta de cobertura (T1), não às asserções.
