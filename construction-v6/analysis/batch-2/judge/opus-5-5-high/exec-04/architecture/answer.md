# arquitetura

## Visão geral

O agente montou um serviço com três pacotes por camada. A raiz `com.clothing` guarda as rotas Javalin, o `Application` e o `Database`. O pacote `model` tem os records e enums, inclusive os corpos de requisição. O pacote `service` junta o serviço de checkout, a validação, os repositórios JDBC, o cliente da TransLog e o `Money`. O melhor está no cálculo do resumo: ele mora numa classe só, `CheckoutService.calculateSummary`, que os dois fluxos chamam. Também não há SQL nem JDBC nas classes de regra.

Os problemas, do mais grave para o menos grave:

1. **Os controllers fazem o trabalho do negócio.** O `OrderRoutes` decide quase todo o ciclo do pedido: o pagamento sem estoque, o envio, a entrega e todo o cancelamento, com a devolução do estoque e o estorno do crédito. Ele chama os repositórios direto, sem passar por um serviço.
2. **A validação de negócio conhece HTTP.** O `Validation` devolve um `ValidationError` que carrega o código HTTP (422, 503).
3. **Há cópias de partes do cálculo.** O subtotal e o peso estão duplicados no `Validation`. O valor da parcela é refeito no repositório quando o pedido é lido.
4. **Um nome de coluna vaza para fora do repositório.** Quem chama o repositório escolhe o nome da coluna de data que entra no SQL.

O relatório não acusa ciclo entre `model` e `service`. Mas a raiz, que ele deixa de fora, forma um laço com `service` por causa do `Database` (ver A2).

## Item a item

**A1, a regra de negócio não conhece a web?** O relatório não aponta nenhuma classe de fora das rotas usando o Javalin. Só `Application`, `CustomerRoutes`, `OrderRoutes` e `ProductRoutes` usam o Javalin, e as quatro são a camada web e a montagem das rotas.

O código HTTP, porém, entra na regra de negócio. O `Validation` é onde moram as regras do pedido: a ordem das recusas, o motoboy até 5 kg, a condição de cada cupom, o boleto até R$ 1.000. Ele declara `record ValidationError(String code, int httpStatus)` (`src/main/java/com/clothing/service/Validation.java:14`) e preenche o status em cada recusa:
- `new ValidationError("INVALID_ORDER", 422)` (`Validation.java:19`);
- `new ValidationError("CARRIER_UNAVAILABLE", 503)` (`Validation.java:78`);
- `new ValidationError("COUPON_NOT_APPLICABLE", 422)` (`Validation.java:119`, `:126`, `:132`);
- `new ValidationError("PAYMENT_METHOD_UNAVAILABLE", 422)` (`Validation.java:145`).

O controller só copia esse número para a resposta: `ctx.status(validationError.httpStatus())` (`src/main/java/com/clothing/OrderRoutes.java:30`). A tradução de recusa para HTTP, que deveria ficar na web, foi decidida na regra.

Há um ponto secundário, e duvidoso. O `CheckoutService.calculateSummary` recebe o `CheckoutRequest` (`CheckoutService.java:22`), que é o corpo JSON da requisição: ele chega por `ctx.bodyAsClass(CheckoutRequest.class)` (`OrderRoutes.java:27`) e tem anotações Jackson (`model/CheckoutRequest.java:3`). Ele mora em `model`, e não junto das rotas, então não pesei esse ponto.

O `httpStatus` no `Validation` já basta.
Conclusão: não segue

**A2, os ciclos entre pacotes são acoplamento real?** O relatório não traz nenhum grupo em ciclo ("Cycles: 0 group(s), 0 of 2 packages caught in one"). Ele só olha os pacotes abaixo da raiz, e a única dependência entre eles é `service -> model`.

O relatório deixa de fora a raiz `com.clothing`, que tem classes próprias (`Application`, as três `*Routes` e `Database`). No código, a raiz e `service` dependem uma da outra:
- **da raiz para `service`:** o `OrderRoutes` cria e chama `CheckoutService` e os repositórios (`OrderRoutes.java:15-18`);
- **de `service` para a raiz:** `OrderRepository`, `CustomerRepository` e `ProductRepository` importam `com.clothing.Database` e chamam `Database.getConnection()` (`service/OrderRepository.java:3`, `service/CustomerRepository.java:3`, `service/ProductRepository.java:3`).

O laço fecha por uma chamada, e não por um tipo de dado. Mas a classe que o fecha é um provedor de conexões estático, que não depende de nada de `service` (`Database.java:1-12`). Nenhum par de classes se chama nos dois sentidos. Se o `Database` fosse para um pacote próprio, o laço sumiria sem mexer em lógica nenhuma.

Pelo relatório, que é a fonte que a régua manda usar, não há ciclo. Registro o laço da raiz em Dúvidas.
Conclusão: segue

**A3, o controller só traduz?** Não. O `OrderRoutes` concentra o ciclo de vida do pedido.

- **Pagamento** (`OrderRoutes.java:118-181`):
  - decide se a operação é permitida pela situação, `order.status() != OrderStatus.AWAITING_PAYMENT` (`:136`);
  - decide se o aviso é repetido, com `orderRepo.isPaymentAlreadyRecorded` (`:142`);
  - confere o estoque item a item e, se faltar, cancela o pedido e cria o aviso por conta própria (`:150-160`): `orderRepo.updateOrderStatus(orderId, OrderStatus.CANCELLED)` em `:155` e `customerRepo.createNotification(...ORDER_CANCELLED)` em `:156`.

  Só a parte das 48 horas e da baixa fica no `CheckoutService.processPayment` (`CheckoutService.java:222-246`). Mesmo assim, o controller traduz o prazo vencido comparando a mensagem da exceção (`OrderRoutes.java:169`).
- **Envio** (`:201-208`): o controller decide a transição (`status() != OrderStatus.PAID`), grava o rastreio e muda a situação direto no repositório.
- **Entrega** (`:231-237`): o mesmo padrão do envio.
- **Cancelamento** (`:260-280`): o controller tem a regra inteira. Ele decide em quais situações pode cancelar (`:260`), devolve o estoque (`orderRepo.incrementStock`, `:270`), estorna o crédito (`customerRepo.updateCreditBalance(cpf, credit.negate())`, `:275`), muda a situação (`:279`) e gera o aviso (`:280`).
- **Resumo e registro:** a sequência das validações de negócio é montada no controller, e repetida nos dois handlers (`:28-51` e `:65-88`): validação geral, depois o cupom, depois o resumo, depois o limite do boleto sobre o total.

Os handlers de produto e de cliente (`ProductRoutes.java`, `CustomerRoutes.java`) só consultam e traduzem.
Conclusão: não segue

**A4, o acesso ao banco está isolado?** Na maior parte, sim. Todo o SQL e o JDBC estão em classes de acesso a dados: `OrderRepository`, `CustomerRepository` e `ProductRepository`, mais o `Database` para a criação do esquema. O `CheckoutService` e o `Validation` só chamam métodos de repositório (`CheckoutService.java:63`, `:218-219`, `:233-245`; `Validation.java:37`, `:117`).

Há um vazamento: o nome da coluna é escolhido fora do repositório e colado no SQL. O repositório monta `"UPDATE orders SET status = ?, " + timestampColumn + " = ? WHERE id = ?"` (`OrderRepository.java:134`). Quem passa a coluna é a regra de negócio, `updateOrderStatusAndTimestamp(orderId, OrderStatus.PAID, "paid_at")` (`CheckoutService.java:234`), e também o controller (`OrderRoutes.java:208`, `:237`). Assim a regra conhece o desenho físico da tabela.

Há ainda duas classes de dados fora do lugar, que são menores:
- a baixa e a consulta de estoque ficam no `OrderRepository` (`:198-232`), e não no repositório de produtos;
- a consulta de pedidos pagos para o WELCOME10 fica no `CustomerRepository` (`:328-343`).

Além disso, o controller fala direto com os repositórios para mudar estado (ver A3).
Conclusão: em parte

**A5, um cálculo só para o resumo e para o pedido?** O cálculo principal é um só. O resumo chama `checkoutService.calculateSummary(req)` (`OrderRoutes.java:44`). O registro chama `registerOrder` (`OrderRoutes.java:90`), que chama o mesmo `calculateSummary` (`CheckoutService.java:217`).

Há, porém, partes refeitas:
- **A parcela do pedido é recalculada na leitura.** O resumo calcula `Money.divide(finalTotal, installments)` (`CheckoutService.java:43`). O pedido não guarda esse valor: o `OrderRepository.getOrder` refaz a conta a partir de `final_total` e `installments` (`OrderRepository.java:94`). É uma etapa refeita só no fluxo do pedido. Hoje a fórmula é igual, mas qualquer mudança na regra da parcela faria os dois divergirem.
- **O subtotal e o peso estão copiados no `Validation`.** Ele tem `calculateSubtotal` e `calculateTotalWeight` próprios (`Validation.java:155-175`), cópias de `CheckoutService.java:60-69` e `:156-165`. São eles que decidem o MINUS50 e o limite do motoboy. Os dois fluxos usam as cópias igualmente, então não fazem o resumo divergir do pedido. Mas a condição do cupom é avaliada sobre um subtotal que não é o do resumo.
- **A regra do FREESHIPPING na retirada aparece duas vezes**, em `CheckoutService.java:80` e `Validation.java:131`.
- **O valor validado não é o valor gravado.** No registro, o resumo usado para conferir o boleto (`OrderRoutes.java:81-83`) é descartado, e `registerOrder` calcula de novo (`CheckoutService.java:217`), consultando a TransLog outra vez.
Conclusão: em parte

**A6, como os pacotes estão organizados?** A organização é por camada, e incompleta:
- `model` tem os records e enums, com os do domínio misturados aos do corpo da API: `CheckoutRequest`, `PaymentRequest`, `ShipmentRequest`, `ErrorResponse`;
- `service` junta serviço, validação, repositórios JDBC, o cliente HTTP da TransLog e o utilitário de dinheiro;
- a raiz fica com a camada web, a inicialização e a infraestrutura de banco.

Não há divisão por assunto (pedido, catálogo, cliente).
Conclusão: não se aplica

## O que mudaria primeiro

1. **Criar um serviço de pedido com as transições.** Os métodos seriam pagar, enviar, entregar e cancelar, com a recusa por situação, a devolução de estoque, o estorno de crédito e os avisos. Eles sairiam do `OrderRoutes`, e os handlers só leriam o corpo, chamariam o serviço e traduziriam o resultado. Hoje a regra mais sensível do enunciado está na camada web, e não há como reutilizá-la nem testá-la sem HTTP.
2. **Tirar o `httpStatus` do `Validation`.** A regra passaria a devolver só o código da recusa (um enum ou uma exceção de negócio), e o mapeamento para 422, 409 e 503 ficaria nas rotas. No mesmo movimento:
   - o `Validation` passaria a usar o subtotal e o peso do `CheckoutService`, em vez de copiá-los;
   - a parcela passaria a ser gravada com o pedido, em vez de recalculada no repositório.

## Dúvidas

- **O laço da raiz com `service`.** O relatório exclui a raiz `com.clothing` da análise de pacotes e acusa zero ciclos. No código, há um laço entre a raiz e `service`: as rotas chamam os serviços, e os repositórios chamam `Database.getConnection()`. Ele fecha por uma chamada a uma classe de infraestrutura, e não por um tipo de dado. Mas não é um caso de serviços se chamando nos dois sentidos. Segui o relatório e dei `segue`. Se o laço da raiz contar como ciclo de comportamento, a conclusão seria `não segue`.
- **O `CheckoutRequest` como objeto da API.** A régua fala dos objetos da API "quando moram com os controllers". O `CheckoutRequest` é o corpo da requisição, mas mora em `model`. Não o contei como conhecimento da web no A1. A conclusão não muda, porque o `httpStatus` no `Validation` já decide o item.
