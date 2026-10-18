# arquitetura

## Visão geral

O agente organizou o serviço em camadas clássicas (controller, service, repository, domain, util). Ligou tudo à mão em `Application.java`, e o cálculo ficou dividido em calculadoras pequenas (cupom, frete, seguro, pagamento, clube). Os pontos fortes são três: não há ciclo entre pacotes, todo o SQL mora nos repositórios, e os dois fluxos (resumo e pedido) passam pelo mesmo método de cálculo.

Os problemas, do mais grave para o menos grave:

1. A regra de negócio conhece a web. `CheckoutService`, `OrderService` e `ShippingCalculator` lançam `ApiException` com o código HTTP embutido (422, 404, 409, 503).
2. O cálculo existe em duas cópias dentro do `CheckoutService`. O `validateRequest` refaz a conta inteira para testar o boleto. Além disso, a etapa "primeira compra" do WELCOME10 é decidida de um jeito no resumo e de outro no pedido.
3. Os serviços cuidam da transação com JDBC cru (`Connection`, `commit`, `rollback`, `SQLException`). A recusa por falta de estoque chega a passar pela mensagem de uma `SQLException`.
4. Dois controllers falam direto com os repositórios, e um deles muda o estado do aviso.

## Item a item

**A1, a regra de negócio não conhece a web?** O relatório não aponta nenhuma classe fora da web que use o Javalin ou dependa de controller ("Classes outside the web layer that use the incoming web: none"). Só os quatro controllers e o `Application` usam o Javalin. O código HTTP, porém, está dentro da regra de negócio. A `ApiException` (`util/ApiException.java:7`) recebe `(String errorCode, int httpStatus)`, e as classes de negócio escolhem o status:
- `service/CheckoutService.java:69` (`COUPON_NOT_APPLICABLE`, 422), `:118` a `:191` (todas as validações com 422) e `:229` (`PAYMENT_METHOD_UNAVAILABLE`, 422);
- `service/OrderService.java:76` e `:86` (404), `:93` (422), `:105`, `:114` e `:130` (409), além de `:167`, `:174`, `:179`, `:210`, `:217`, `:244` e `:251`;
- `service/shipping/ShippingCalculator.java:24` e `:37` (422) e `:42` (`CARRIER_UNAVAILABLE`, 503).

Os controllers só repassam `e.getHttpStatus()` (por exemplo `controller/OrderController.java:25`). Ou seja, quem traduz o negócio para HTTP é o serviço, não a camada web.

Há também um ponto secundário. O corpo da requisição, `CheckoutRequest` (`domain/CheckoutRequest.java`), é o mesmo objeto que o controller lê com `ctx.bodyAsClass` (`controller/CheckoutController.java:21`) e que a regra recebe (`service/coupon/CouponStrategy.java:11`, `service/shipping/ShippingCalculator.java:17`). Ele mora em `domain` e não com os controllers, então não o conto como violação (ver Dúvidas). Os códigos HTTP sozinhos já decidem o item.
Conclusão: não segue

**A2, os ciclos entre pacotes são acoplamento real?** O relatório não tem nenhum grupo de ciclo ("Cycles: 0 group(s), 0 of 5 packages caught in one"). Todas as dependências vão num sentido só: controller → service/repository/domain/util, service → repository/domain/util e repository → domain. Os subpacotes de `service` (coupon, shipping etc.) importam só `domain` e `util`, e nunca `service` (`service/coupon/CouponStrategy.java:3`, `service/shipping/ShippingCalculator.java:3-4`).
Conclusão: segue

**A3, o controller só traduz?** `CheckoutController` e `OrderController` só leem a requisição, chamam o serviço e escrevem a resposta (`controller/OrderController.java:21-23`, `:41-48`). Não fazem conta, não mudam a situação do pedido e não mexem no estoque.

`CustomerController` e `ProductController`, porém, pulam a camada de serviço e chamam os repositórios direto (relatório: `controller -> repository`, 3 dependências). Neles ficam duas regras pequenas do negócio:
- A recusa "avisos de CPF que nunca registrou pedido são `CUSTOMER_NOT_FOUND`" é decidida no controller (`controller/CustomerController.java:36-37`).
- Marcar o aviso como lido é uma mudança de estado feita pelo controller: ele chama `notificationRepository.markAsRead` e depois `notification.setStatus(READ)` (`controller/CustomerController.java:52-53`).

As regras são triviais e não tocam no pedido nem no estoque, mas ficam fora da camada de negócio.
Conclusão: em parte

**A4, o acesso ao banco está isolado?** Todo o SQL está nos repositórios (por exemplo `repository/ProductRepository.java:99`, `repository/OrderRepository.java:121`), e nenhuma classe de serviço monta SQL. Mesmo assim, os serviços usam JDBC direto para a transação:
- `OrderService` recebe o `Database` e faz `conn.setAutoCommit(false)`, `commit` e `rollback` em todos os métodos (`service/OrderService.java:36-37`, `:60`, `:63`, `:81-155` e seguintes).
- `CheckoutService` recebe e repassa uma `java.sql.Connection` (`service/CheckoutService.java:50`) e declara `throws SQLException` (`:46`).
- O pior caso: o repositório sinaliza a falta de estoque com `throw new SQLException("INSUFFICIENT_STOCK")` (`repository/ProductRepository.java:107`), e o serviço decide a recusa comparando a mensagem da exceção (`service/OrderService.java:124-130`).

A consulta em si está isolada, mas a fronteira da transação e parte do controle de fluxo vazam o JDBC para a regra.
Conclusão: em parte

**A5, um cálculo só para o resumo e para o pedido?** Os dois fluxos chamam o mesmo método: o resumo via `checkoutService.calculateSummary(request)`, que delega a `calculateSummary(request, null)` (`controller/CheckoutController.java:22`, `service/CheckoutService.java:47`), e o pedido via `calculateSummary(request, conn)` (`service/OrderService.java:39`). Dentro do serviço, porém, há uma segunda cópia do cálculo. O `validateRequest` refaz subtotal, primeira compra, cupom com teto, frete com benefício do clube, seguro, total e ajuste do pagamento (`service/CheckoutService.java:195-226`), só para testar o boleto acima de R$ 1.000,00 (`:228`). Essa cópia já diverge da principal: quando o cupom não se aplica, ela sai com `return` (`:203-204`) em vez de recusar.

Além disso, a etapa de elegibilidade do WELCOME10 depende do fluxo. A linha `isFirstPurchase = conn == null || !orderRepository.hasCompletedOrders(...)` (`:62` e `:198`) faz o resumo sempre tratar o cliente como primeira compra, e só o registro consulta o histórico. É uma etapa feita de fato só num dos fluxos, e é exatamente o tipo de diferença entre resumo e pedido que o enunciado proíbe.

Há também cópias menores:
- `Minus50Coupon.isApplicable` recalcula a soma dos produtos por conta própria (`service/coupon/Minus50Coupon.java:9-14`).
- O frete é calculado duas vezes no mesmo método (`service/CheckoutService.java:72` e `:81`).
Conclusão: em parte

**A6, como os pacotes estão organizados?** No primeiro nível, por camada: `controller` (4 classes), `service` (14), `repository` (5), `domain` (14) e `util` (2), com o `Application` na raiz. Dentro de `service` há subpacotes por assunto do cálculo (`club`, `coupon`, `insurance`, `payment`, `shipping`). O desenho é, portanto, por camada, com uma divisão por assunto só dentro dos serviços.
Conclusão: não se aplica

## O que mudaria primeiro

1. **Tirar o HTTP da regra de negócio.** Os serviços lançariam exceções de negócio com só o código do erro (ou um tipo por categoria: não encontrado, inválido, conflito, transportadora indisponível), e a camada web teria um único tradutor para o status HTTP. Hoje cada `throw` nos serviços fixa o protocolo, e os controllers repetem o mesmo `catch` em todos os métodos.
2. **Deixar um único caminho de cálculo.** O `validateRequest` ficaria só com as validações de entrada. A checagem do boleto passaria para dentro do `calculateSummary`, logo depois do total do pedido. A primeira compra seria consultada nos dois fluxos (sem o atalho `conn == null`). Assim o resumo e o pedido passariam a fazer exatamente as mesmas etapas.

## Dúvidas

- **`CheckoutRequest` em `domain`.** A régua conta os objetos da API "quando moram com os controllers". Aqui o corpo da requisição mora em `domain` e é usado como entrada da regra (cupons, frete). Decidi não contá-lo como dependência da web, porque o pacote não é web e o relatório não aponta nada. Mas é, na prática, o formato do JSON circulando pela regra. O mesmo vale para `Order`, serializado direto como resposta (`controller/OrderController.java:23`). O item A1 não depende dessa decisão, porque os códigos HTTP já o decidem.
- **`ApiException` em `util`.** A classe não é tipo de framework nem mora com os controllers, mas carrega o status HTTP. Li "os códigos HTTP" da régua como cobrindo esse caso.
- **A3 com a marcação do aviso.** Marcar um aviso como lido no controller é uma mudança de estado, mas não do pedido nem do estoque, que são os exemplos da régua. Pesei isso, junto com a recusa `CUSTOMER_NOT_FOUND` decidida no controller, como `em parte`, e não como `não segue`.
