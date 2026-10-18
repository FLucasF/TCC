# a arquitetura

## Visão geral

O agente montou um serviço em camadas: `api` (um controller Javalin e DTOs), `services` (checkout, pedido, validação), `domain` (entidades, eventos, observers e strategies), `repositories` (interfaces e implementações JDBC) e `infrastructure` (conexão com o banco e cliente da TransLog). A separação mais importante está certa. A regra de negócio não conhece o Javalin nem os DTOs da API, o SQL fica nos repositórios, e o resumo e o pedido usam o mesmo cálculo. Os problemas, do mais grave para o mais leve:

1. O controller orquestra o registro do pedido (valida, calcula e só depois manda gravar) e muda sozinho a situação de um aviso.
2. O `OrderService` faz cast para a implementação JDBC do repositório para gravar a transação de pagamento.
3. Há um ciclo `domain` ↔ `repositories`. Ele fecha pelas entidades, porque as interfaces de repositório estão fora do pacote do domínio que as usa.
4. O `ValidationService` refaz partes do cálculo (subtotal, peso, frete) por conta própria.

## Item a item

**A1, a regra de negócio não conhece a web?** O relatório não acha nenhuma classe de fora da web que dependa de um controller ou da API web de entrada. Só duas classes usam o Javalin: `Main` e `api.controllers.ApiController`. As duas são a camada web e a montagem, não negócio.

Também não há dependência de `domain` ou `services` para `api` (o relatório lista só `api -> domain`, `api -> services` e `api -> repositories`). Os serviços recebem tipos próprios:
- `CheckoutService.CheckoutRequest` é um record do pacote `services` (`src/main/java/com/loja/services/CheckoutService.java:138`), montado pelo controller a partir do `CheckoutRequestDto` (`src/main/java/com/loja/api/controllers/ApiController.java:325-341`).
- Os serviços devolvem entidades do domínio (`OrderSummary`, `Order`). A conversão para os DTOs fica no controller (`ApiController.java:343-375`).
- As recusas saem como exceções com o código do negócio (`ValidationService.ValidationException`). A tradução para 422 ou 503 está no controller (`ApiController.java:377-383`, `:77-78`).

Nenhuma classe de negócio usa código HTTP. O `TransLogClient` usa `java.net.http`, mas é um cliente de saída na `infrastructure`, não a web de entrada.
Conclusão: segue

**A2, os ciclos entre pacotes são acoplamento real?** O relatório tem um grupo, `domain` ↔ `repositories`.

- **De `repositories` para `domain`:** todas as dependências são de tipos de dado. São as entidades `Customer`, `Order`, `OrderItem`, `OrderSummary`, `Product`, `Notification`, `Money` e os enums `OrderStatus`, `NotificationStatus` e `NotificationType`, usados pelas interfaces e pelas implementações JDBC para mapear linhas.
- **De `domain` para `repositories`:** as dependências vêm de classes com comportamento que chamam as interfaces de repositório:
  - `CreditObserver` chama `customerRepository.update` (`src/main/java/com/loja/domain/observers/CreditObserver.java:24`);
  - `StockObserver` chama `findBySku` e `update` (`StockObserver.java:21,28,30`);
  - `NotificationObserver` chama `save` (`NotificationObserver.java:28`);
  - `Welcome10Coupon` chama `isFirstOrder` (`src/main/java/com/loja/domain/strategies/coupon/Welcome10Coupon.java:22`);
  - `Buy3Pay2Coupon` chama `findBySku` (`Buy3Pay2Coupon.java:30`).

As chamadas de comportamento só vão num sentido: regras do domínio chamam repositórios. Os repositórios não chamam regra nenhuma, só usam as entidades como dados. O círculo fecha porque as entidades e as regras moram no mesmo pacote de topo (`domain`), e as interfaces de repositório que essas regras usam ficam fora dele. Se as entidades fossem para um pacote próprio, ou as interfaces para dentro de `domain`, o ciclo sumiria sem mudar o desenho. Por isso li o ciclo como fechado por tipo de dado, que é o caso leve.
Conclusão: em parte

**A3, o controller só traduz?** Na maior parte, sim. Ele não faz conta, não decide recusa do negócio, não muda a situação do pedido e não mexe no estoque. Os envios, entregas, cancelamentos e pagamentos vão para o `OrderService` (`ApiController.java:143`, `:175`, `:192`, `:209`). Mas há dois pontos em que ele faz mais do que traduzir:

- **O registro do pedido é orquestrado no controller.** O `createOrder` chama `validationService.validateCheckoutRequest`, depois `checkoutService.calculateSummary`, monta os itens e só então passa o resumo pronto para `orderService.createOrder` (`ApiController.java:89-101`). Quem garante que o pedido foi validado e que o resumo vem do cálculo é o controller. O `OrderService.createOrder` grava qualquer `OrderSummary` que receber, sem validar (`src/main/java/com/loja/services/OrderService.java:38-57`). O resumo segue o mesmo roteiro, também no controller (`ApiController.java:70-71`).
- **Marcar um aviso como lido é feito inteiro no controller:** `notification.markAsRead()` e depois `notificationRepository.update(notification)` (`ApiController.java:307-308`). Essa mudança de situação do aviso não passa por nenhum serviço.
- Pontos menores:
  - a regra "avisos de um CPF sem pedido são `CUSTOMER_NOT_FOUND`" é decidida no controller (`ApiController.java:273`);
  - as consultas de pedido, produto e cliente vão direto ao repositório (`:117`, `:226`, `:251`). São só leituras e traduções, aceitáveis.
- A tradução das recusas depende do texto da mensagem da exceção (`e.getMessage().contains("not found")`, `ApiController.java:153-159`, `:179`, `:196`, `:213`). Continua sendo tradução, mas é frágil.
Conclusão: em parte

**A4, o acesso ao banco está isolado?** O SQL e o JDBC estão só em `repositories/impl/*RepositoryImpl.java` (por exemplo, `src/main/java/com/loja/repositories/impl/ProductRepositoryImpl.java:48`) e em `infrastructure/Database.java` (a conexão e a criação das tabelas). Nenhuma classe de `services` ou `domain` importa `java.sql` ou monta SQL (conferi com busca). As regras de negócio falam com interfaces próprias (`repositories/OrderRepository.java`, `ProductRepository.java` etc.).

Há uma quebra da abstração: o `OrderService` importa a implementação JDBC (`OrderService.java:11`) e faz cast `((OrderRepositoryImpl) orderRepository).savePaymentTransaction(...)` em três lugares (`OrderService.java:76`, `:88`, `:95`), porque esse método não está na interface. Não há SQL no serviço, mas o serviço fica preso à classe JDBC concreta, e o relatório mostra `services -> repositories` incluindo `impl`. Como a pergunta é se o SQL e o JDBC estão isolados, e estão, julguei que segue, com essa ressalva.
Conclusão: segue

**A5, um cálculo só para o resumo e para o pedido?** Os dois fluxos chamam o mesmo `checkoutService.calculateSummary(request)`: o resumo em `ApiController.java:71` e o registro em `ApiController.java:90`. O pedido grava esse mesmo `OrderSummary` (`OrderService.java:45-50`). Não existe uma segunda cópia do cálculo exclusiva de um dos fluxos.

O que existe é uma duplicação paralela no `ValidationService`, que vale igualmente para os dois fluxos:
- ele tem suas próprias cópias de `calculateProductsSubtotal` e `calculateTotalWeight` (`src/main/java/com/loja/services/ValidationService.java:191-207`, iguais às de `CheckoutService.java:97-114`);
- ele consulta o frete de novo (`ValidationService.java:121-122`, `:150`);
- ele decide se o boleto está disponível pelo subtotal recalculado, não pelo total do resumo (`ValidationService.java:185-186`).

Isso não faz o resumo e o pedido divergirem entre si. Mas é uma segunda versão de etapas do cálculo que pode divergir do `CheckoutService`.
Conclusão: segue

**A6, como os pacotes estão organizados?** Por camada: `api` (`controllers`, `dto`), `services`, `domain`, `repositories` (com `impl`) e `infrastructure`, com o `Main` na raiz fazendo a montagem à mão (`src/main/java/com/loja/Main.java:36-112`). Dentro de `domain`, a divisão é por tipo de construção (`entities`, `events`, `observers`, `strategies/{club,coupon,payment,shipping}`), não por assunto do negócio.
Conclusão: não se aplica

## O que mudaria primeiro

1. **Levar a orquestração do registro para dentro de um serviço.** O caso de uso "registrar pedido" (validar, calcular o resumo, criar o cliente, gravar) deveria ficar num método só do serviço, chamado pelo controller com o `CheckoutRequest`. Assim o controller só traduz, e não dá para gravar um pedido com um resumo que não veio do cálculo validado. Marcar um aviso como lido também deveria ir para um serviço.
2. **Pôr `savePaymentTransaction` na interface `OrderRepository` e tirar os casts.** Junto com isso, mover as interfaces de repositório para dentro do domínio, ou as entidades para um pacote próprio. Isso desfaz o ciclo `domain` ↔ `repositories` e deixa as regras dependerem só de abstrações.

## Dúvidas

- **A2:** decidir se o ciclo fecha por tipo de dado ou por comportamento depende de qual aresta se considera a que "fecha" o círculo. A aresta `domain -> repositories` é de comportamento (observers e cupons chamando repositórios), mas tem um sentido só. A volta (`repositories -> domain`) é só de entidades. Como a régua pesa o comportamento "nos dois sentidos" e o ciclo some só de mudar tipos de pacote, li como leve (`em parte`). Uma leitura mais rígida, em que a regra de negócio dependendo de um pacote de persistência já é o laço de comportamento, daria `não segue`.
- **A4:** contei o cast para `OrderRepositoryImpl` como ressalva, não como falha. A régua define o problema como SQL ou JDBC na regra de negócio, e isso não acontece.
- **A6:** é descritivo e a régua não dá conclusão para ele. Usei `não se aplica`, porque a lista de palavras permitidas não tem uma neutra.
