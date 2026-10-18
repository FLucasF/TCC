# a arquitetura

## Visão geral

O agente montou um projeto em camadas técnicas: `api` com os handlers do Javalin, `service` com `CheckoutService` e `OrderService`, `repository` com JDBC puro, `model` com as entidades e os DTOs, e mais `strategy`, `shipping` e `util`. O relatório não mostra nenhum ciclo entre pacotes. Só `App` e as classes de `api` usam o Javalin. Todo o SQL fica nos repositórios. O resumo e o registro do pedido passam pelo mesmo `calculateSummary`.

O problema mais sério está no `OrderService`: ele decide os códigos HTTP das recusas (404, 409, 422), que viajam dentro da `OrderException`. Com isso, a regra do ciclo do pedido conhece a web. Em segundo lugar, o `NotificationEndpoint` toma decisões que caberiam a um serviço: ele resolve que o cliente não existe porque a lista de avisos veio vazia, e ele mesmo muda o aviso para lido. Há também dois defeitos de desenho menores: `CheckoutService` e `OrderService` dependem um do outro, e existem duas instâncias do `CheckoutService`.

## Item a item

**A1, a regra de negócio não conhece a web?** O relatório diz que nenhuma classe fora da camada web depende de controller nem usa a API web de entrada. Só `App` e as classes de `api` usam o Javalin (`architecture/architecture-report.txt`, a lista "Classes that use Javalin"). Os tipos do framework, portanto, estão contidos. Os códigos HTTP, não:
- `OrderException` guarda um `httpStatus` (`src/main/java/com/loja/service/OrderException.java:5`).
- O `OrderService` escolhe esse código em cada recusa: `new OrderException("ORDER_NOT_FOUND", 404)` (`src/main/java/com/loja/service/OrderService.java:54`), `("INVALID_ORDER", 422)` (`:58`), `("OPERATION_NOT_ALLOWED", 409)` (`:62`, `:122`, `:150`, `:166`, `:170`), `("ORDER_EXPIRED", 409)` (`:86`) e `("INSUFFICIENT_STOCK", 409)` (`:208`).
- O controller só repassa o valor: `ctx.status(e.httpStatus)` (`src/main/java/com/loja/api/OrderEndpoint.java:46`, `:56`, `:65`, `:74`).

O ciclo do pedido, que é regra de negócio, decide qual é a resposta HTTP. A tradução deveria ficar na camada web. No checkout a tradução está certa: a `CheckoutException` leva só o código, e o mapeamento para 422/503 fica em `OrderEndpoint.getHttpStatus` (`src/main/java/com/loja/api/OrderEndpoint.java:79-84`).

Há um ponto secundário, que deixo em dúvida (ver Dúvidas). Os serviços recebem o `CheckoutRequest`, que é a classe em que o Jackson desserializa o corpo HTTP (`src/main/java/com/loja/api/CheckoutEndpoint.java:18`, `src/main/java/com/loja/service/CheckoutService.java:19`, `src/main/java/com/loja/service/OrderService.java:27`). O `processPayment` recebe o `paidAt` como o texto cru do JSON e o converte ele mesmo (`src/main/java/com/loja/service/OrderService.java:51`, `:73`). Esses DTOs moram em `model`, não junto dos controllers, então não os conto como violação. Só as recusas com o código HTTP já bastam: uma classe de negócio, `OrderService`, conhece a web.
Conclusão: não segue

**A2, os ciclos entre pacotes são acoplamento real?** O relatório informa "Cycles: 0 group(s), 0 of 7 packages caught in one", e as 12 dependências entre pacotes vão todas num sentido só (api → service → strategy/repository → model). Não existe ciclo entre pacotes. Dentro do pacote `service`, porém, há um laço por comportamento, que o relatório não mostra porque ele só olha pacotes:
- `OrderService` cria um `CheckoutService` passando `this` (`src/main/java/com/loja/service/OrderService.java:24`) e chama `calculateSummary` (`:28`).
- `CheckoutService` volta ao `OrderService` por `orderService.getOrderRepo()` (`src/main/java/com/loja/service/CheckoutService.java:142`, `:198`), só para pegar o repositório.

É um acoplamento ruim, mas não é ciclo entre pacotes, que é o que o item pergunta.
Conclusão: segue

**A3, o controller só traduz?** O `CheckoutEndpoint` e o `OrderEndpoint` recebem, chamam o serviço e traduzem: o `null` do `findOrder` vira 404 (`src/main/java/com/loja/api/OrderEndpoint.java:28-32`), e não há conta, estoque nem mudança de situação ali. O `NotificationEndpoint` vai além:
- Em `GET /customers/{cpf}/notifications`, ele decide que o cliente não existe porque a lista de avisos está vazia (`src/main/java/com/loja/api/NotificationEndpoint.java:18-22`). Isso é uma regra do negócio ("CPF que nunca registrou pedido"), decidida no controller e com o critério errado: o cliente que tem pedido, mas ainda nenhum aviso, recebe 404, quando a fonte certa seria o `CustomerRepository`.
- Em `POST /notifications/{id}/read`, o próprio controller muda a situação do aviso: chama `notificationRepo.markAsRead` e põe `notification.status = NotificationStatus.READ` (`:32-33`). Nenhum serviço participa.

O `ProductEndpoint` e o `CustomerEndpoint` consultam o repositório direto (`src/main/java/com/loja/api/ProductEndpoint.java:16`, `src/main/java/com/loja/api/CustomerEndpoint.java:16`). São leituras simples, então aceito. O problema fica restrito aos avisos: não toca no pedido nem no estoque, mas são decisões que não cabem a um controller.
Conclusão: em parte

**A4, o acesso ao banco está isolado?** O `java.sql` aparece só no pacote `repository`: `Database`, `ProductRepository`, `OrderRepository`, `CustomerRepository` e `NotificationRepository` (todos com `import java.sql.*`). Os serviços e as estratégias falam com os dados só por métodos de repositório:
- `productRepo.updateStock` (`src/main/java/com/loja/service/OrderService.java:215`);
- `customerRepo.addCredit` (`:226`);
- `orderRepo.hasCompletedOrder` (`src/main/java/com/loja/strategy/Welcome10Coupon.java:23`).

Nenhuma classe de negócio monta SQL. Um detalhe vai no sentido contrário: a definição de "primeira compra" (as situações que contam) está escrita no SQL do repositório, `status IN ('PAID', 'SHIPPED', 'DELIVERED')` (`src/main/java/com/loja/repository/OrderRepository.java:109`). Isso é regra que vazou para a camada de dados, não SQL na regra de negócio, então não muda a conclusão.
Conclusão: segue

**A5, um cálculo só para o resumo e para o pedido?** Os dois fluxos chamam o mesmo método:
- o resumo: `checkoutService.calculateSummary(request)` (`src/main/java/com/loja/api/CheckoutEndpoint.java:19`);
- o registro: `checkoutService.calculateSummary(request)` (`src/main/java/com/loja/service/OrderService.java:28`), com o resultado gravado como está (`:37`).

As instâncias são diferentes: uma é criada em `src/main/java/com/loja/App.java:23` e a outra em `src/main/java/com/loja/service/OrderService.java:24`. Mas a classe não guarda estado além dos repositórios, que são os mesmos, então o cálculo é um só. Dentro dele há repetição, só que ela vale para os dois fluxos e por isso não os separa:
- a TransLog é consultada duas vezes, uma na validação e sempre com `STD` (`src/main/java/com/loja/service/CheckoutService.java:164-166`, via `src/main/java/com/loja/shipping/TransLogAdapter.java:19`) e outra no cálculo (`src/main/java/com/loja/service/CheckoutService.java:240-245`);
- a checagem de frete grátis do clube aparece duas vezes (`:30`, `:40`).
Conclusão: segue

**A6, como os pacotes estão organizados?** Os pacotes seguem a camada e o tipo técnico, não o assunto:
- camadas: `api` (5 classes), `service` (4), `repository` (5) e `model` (18, que junta entidades, enums e os DTOs de entrada e saída);
- tipo técnico: `strategy` (17, com cupons, pagamentos e clube misturados), `shipping` (3) e `util` (1);
- `App` fica no pacote raiz (`architecture/architecture-report.txt`, a lista "Packages").

É organização por camada, com pacotes extras nomeados pelo padrão de projeto (`strategy`) em vez do assunto.
Conclusão: não se aplica

## O que mudaria primeiro

1. **Tirar o código HTTP do `OrderService`.** A `OrderException` passaria a levar só o código do negócio, como a `CheckoutException` já faz, e o mapeamento para 404/409/422 ficaria no `OrderEndpoint`, igual ao `getHttpStatus` do checkout. Isso resolve o único motivo do `não segue` em A1 e deixa as duas exceções coerentes.
2. **Criar um serviço de clientes e avisos.** Ele cuidaria de "o cliente existe?", consultando o `CustomerRepository`, e de "marcar como lido". O `NotificationEndpoint` deixaria de decidir regra e de mudar estado. Junto, eu desfaria o laço `OrderService` ↔ `CheckoutService`: o `CheckoutService` receberia o `OrderRepository` no construtor, e o `OrderService` receberia o `CheckoutService` pronto do `App`.

## Dúvidas

- **A1, os DTOs de entrada.** `CheckoutRequest`, `PaymentRequest`, `ShipmentRequest` e `CustomerRequest` são o formato do corpo HTTP, mas moram em `model`, não junto dos handlers. A régua cita os objetos da API "quando moram com os controllers", então não os contei. Os serviços receberem esse DTO mutável, e até alterá-lo (`request.installments = 1`, `src/main/java/com/loja/service/CheckoutService.java:128`), é um acoplamento à forma da API. Ele não decide a conclusão, que já é `não segue` pelos códigos HTTP.
- **A2, o laço dentro do pacote.** O laço entre `CheckoutService` e `OrderService` é por comportamento, mas fica dentro de um pacote só. Li o item como restrito aos ciclos entre pacotes do relatório e concluí `segue`, deixando o laço registrado no texto.
- **A6.** É descritivo, sem juízo de valor, e o formato só permite as quatro palavras das outras dimensões. Usei `não se aplica` para não sugerir que a organização segue ou não segue alguma prática.
