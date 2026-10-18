# arquitetura

## Visão geral

O projeto é organizado por camada (`controller`, `service`, `repository`, `model`, `dto`). O cálculo do resumo está num único método, `CheckoutService.calculateSummary`, e o resumo e o pedido passam os dois por ele. O SQL ficou todo em repositórios, as regras pequenas (cupom, clube, pagamento, frete) estão em serviços separados, e os controllers dos pedidos ficam finos. O problema mais sério está em A1: a regra de negócio conhece a web. Os serviços lançam `BusinessException` com o código HTTP (422, 409, 404, 503) em 38 lugares. Além disso, `CheckoutService` e `OrderService` recebem o objeto do corpo da requisição (`dto.CheckoutRequest`). Em segundo lugar vem A5: o mesmo cálculo faz o passo "primeira compra" só no fluxo do pedido, porque depende de receber ou não uma `Connection`. Dentro da validação há ainda uma segunda cópia parcial do cálculo do total. Em terceiro, o JDBC aparece nos serviços: `Connection`, `commit`/`rollback`, e um `SQLException` usado para dizer "sem estoque". Por fim, os controllers de cliente, aviso e produto pulam a camada de serviço e chamam os repositórios direto. Há um único ciclo entre pacotes (`dto` ↔ `model`), e ele fecha por um tipo de dado.

## Item a item

**A1, a regra de negócio não conhece a web?** O relatório não acha tipos do Javalin fora dos controllers e do `Main` ("Classes that use Javalin": só `Main` e os quatro `controller.*`). Mesmo assim, a regra de negócio conhece a web de duas formas:
- **Os códigos HTTP.** `exception/BusinessException.java:5-10` guarda um `httpStatus`, e os serviços escolhem o código ao recusar: `service/CheckoutService.java:77` (422), `:115`, `:137`, `:166`, `:221`; `service/OrderService.java:88` (409), `:99` (404), `:103`, `:117`, `:134`; `service/ShippingService.java:64` e `:92` (503), `:70` (422). Os controllers só repassam esse código: `controller/OrderController.java:23` usa `ctx.status(e.getHttpStatus())`.
- **O objeto de entrada da API.** `service/CheckoutService.java:40` e `service/OrderService.java:56` recebem `dto.CheckoutRequest`, a classe em que o Javalin converte o corpo JSON (`controller/CheckoutController.java:19`, `controller/OrderController.java:18`). Ela traz até o `CustomerInfo` aninhado, que é o formato do JSON. O relatório mostra a dependência `service -> dto (6 class dependencies)`.

O resumo devolvido também é `dto.CheckoutSummary`. É o mesmo objeto que o controller serializa e que a entidade `model.Order` guarda (`model/Order.java:13`).

As classes `CouponService`, `ClubService` e `PaymentService` estão limpas: recebem e devolvem tipos próprios ou `BigDecimal`. Mas `CheckoutService`, `OrderService` e `ShippingService` são classes de negócio que conhecem a web, e uma só já basta.
Conclusão: não segue

**A2, os ciclos entre pacotes são acoplamento real?** O relatório tem um grupo só: `dto, model`.
- No sentido `dto -> model`, as dependências são de DTOs para enums (`ClubTier`, `Region`, `OrderStatus` etc.) e para `model.OrderItem`. São todas tipos de dado.
- O círculo fecha na volta, `model -> dto`, por uma única dependência: `model.Order -> dto.CheckoutSummary` (`model/Order.java:3`, `:13`). `CheckoutSummary` é uma classe só de dados, com campos, getters e setters (`dto/CheckoutSummary.java:5-52`).

O ciclo fecha por um tipo de dado. Bastaria mover o resumo para `model` sem mudar o desenho. Nenhum laço fecha por comportamento.
Conclusão: em parte

**A3, o controller só traduz?** `CheckoutController` e quase todo o `OrderController` só traduzem: leem o corpo, chamam o serviço e convertem o resultado ou a exceção para HTTP (`controller/OrderController.java:16-26`, `:82-108`). Mas há pontos em que o controller faz mais do que traduzir:
- **`CustomerController` e `ProductController` não têm serviço.** Falam com os repositórios direto e decidem a recusa ali: `controller/CustomerController.java:28-32` (`CUSTOMER_NOT_FOUND`), `controller/ProductController.java:20-24` (`PRODUCT_NOT_FOUND`).
- **A regra "os avisos de um CPF que nunca registrou pedido são `CUSTOMER_NOT_FOUND`"** está em `controller/CustomerController.java:46-50`.
- **O caso de uso "marcar aviso como lido"** também está no controller: busca, troca a situação e busca de novo (`controller/CustomerController.java:68-75`). É uma mudança de situação de uma entidade feita fora de um serviço.
- **A presença de `transactionId`/`paidAt` e de `trackingCode`** é conferida no controller (`controller/OrderController.java:47-49`, `:67-69`). Conferir o formato da requisição é papel aceitável da camada web. O efeito colateral é que essa recusa passa na frente do `ORDER_NOT_FOUND`, que fica no serviço.

Nenhum controller calcula valores nem mexe na situação do pedido ou no estoque. Mas três casos de uso pequenos moram na camada web.
Conclusão: em parte

**A4, o acesso ao banco está isolado?** Todo o SQL está nos repositórios: `repository/*Repository.java` e a criação do esquema em `config/Database.java:42-124`. A busca por `SELECT|UPDATE|INSERT|PreparedStatement` em `service/` não acha nada. O JDBC, porém, aparece nos serviços:
- **A transação é controlada à mão no serviço.** `service/OrderService.java:57-58` (`database.getConnection()`, `setAutoCommit(false)`), `:66`, `:70`, `:73` (`commit`/`rollback`), e o mesmo padrão em `:94-163`, `:167-195`, `:199-224`, `:228-270`.
- **`CheckoutService` recebe e repassa um `java.sql.Connection`.** Também declara `throws SQLException` (`service/CheckoutService.java:17-18`, `:40`, `:112`).
- **A falta de estoque chega à regra de negócio como `SQLException`.** `repository/ProductRepository.java:51-52` lança `SQLException("Insufficient stock")`, e `service/OrderService.java:124` captura essa exceção técnica para decidir que o pedido é cancelado. Como o `UPDATE` que não afeta linha nenhuma não aborta a transação, o `commit` em `:128` grava as baixas feitas antes nos outros itens. Isso é comportamento, de outra dimensão, mas vem direto desse desenho.

O SQL está isolado, mas os serviços de regra usam JDBC direto para a transação e para o sinal de estoque.
Conclusão: em parte

**A5, um cálculo só para o resumo e para o pedido?** Os dois fluxos chamam o mesmo método: `controller/CheckoutController.java:20` chama `checkoutService.calculateSummary(request, null)`, e `service/OrderService.java:60` chama `checkoutService.calculateSummary(request, conn)`. O pedido grava o resumo devolvido (`service/OrderService.java:64`). Há, porém, dois desvios:
- **Um passo só acontece no fluxo do pedido.** A consulta de primeira compra só roda quando há conexão (`service/CheckoutService.java:64-68`, e de novo em `:200-204`). No resumo, que recebe `null`, `isFirstPurchase` fica sempre `false`. Pela leitura do código, o `WELCOME10` então sai `COUPON_NOT_APPLICABLE` no resumo e é aceito no pedido. O mesmo cálculo dá resultados diferentes conforme o fluxo, que é justamente o problema que o enunciado quer evitar.
- **O total é recalculado dentro da validação.** Para o limite do boleto, `service/CheckoutService.java:190-222` refaz subtotal, frete (com uma nova chamada à TransLog), frete grátis do GOLD, primeira compra, cupom, seguro e total. É a mesma conta que `:49-85` faz. Esta cópia já diverge: ignora o cupom não aplicável (`:212`, `discount != null`). E as duas cotações da TransLog podem não ser iguais.

As duas cópias rodam nos dois fluxos, então a diferença entre resumo e pedido vem só do primeiro desvio. A estrutura é de um cálculo único, com uma etapa que depende do fluxo e uma cópia parcial do total.
Conclusão: em parte

**A6, como os pacotes estão organizados?** Por camada. São sete pacotes técnicos (`config` 1, `controller` 4, `dto` 10, `exception` 1, `model` 13, `repository` 4, `service` 6, segundo o relatório), mais o `Main` na raiz. Não há pacote por assunto: pedido, catálogo e cupom se espalham por todas as camadas.
Conclusão: não se aplica

## O que mudaria primeiro

1. **Tirar a web da regra de negócio.**
   - A `BusinessException` passaria a carregar só o código do negócio, e a tabela código → HTTP iria para a camada web (um `app.exception(...)` no `Main` ou um mapeador nos controllers).
   - Os serviços receberiam um comando próprio do negócio em vez do `CheckoutRequest`.
   - O `CheckoutSummary` iria para `model`, o que também desfaz o ciclo `dto` ↔ `model`.

   Isso resolve A1 e A2 de uma vez.
2. **Deixar o cálculo realmente único.** A primeira compra seria um dado de entrada que os dois fluxos buscam do mesmo jeito, em vez de depender de chegar uma `Connection`. O limite do boleto seria conferido sobre o total já calculado, sem a segunda cópia em `validateRequest`. Assim o resumo e o pedido não podem mais divergir.

## Dúvidas

- **A1, o `dto.CheckoutRequest`.** A régua fala dos objetos da API "quando moram com os controllers". Aqui eles estão num pacote próprio, `dto`, fora de `controller`. Contei mesmo assim como objeto da API: é a classe em que o corpo JSON é convertido, e `dto` também guarda as respostas e o `ErrorResponse`. A conclusão de A1 não depende dessa leitura, porque os códigos HTTP nos serviços já bastam.
- **A5, a classificação dos desvios.** A divergência do `WELCOME10` não vem de uma cópia do cálculo, e sim de um ramo dentro do mesmo método que muda conforme o fluxo. Tratei como "uma etapa feita só num dos fluxos" e dei `em parte`, não `não segue`, porque os dois fluxos de fato chamam o mesmo cálculo. Conferi esse efeito só lendo o código, sem rodar.
- **A6, a linha de conclusão.** O item é descritivo e a régua não dá uma conclusão para ele. Usei `não se aplica`, porque nenhuma das outras palavras cabe num item que não é avaliado.
