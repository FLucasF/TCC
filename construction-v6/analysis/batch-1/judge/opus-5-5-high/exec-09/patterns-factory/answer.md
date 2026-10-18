# padrões: Factory

## Visão geral

Todos os objetos de cada caso (as 4 entregas, os 4 cupons, as 3 formas de pagamento e os 3 níveis) são classes `@Component`. O contêiner do Spring cria essas classes, e o `CheckoutService` recebe cada família como `List<...>` e a guarda num mapa indexado por `getCode()` (`service/CheckoutService.java:39-48`). Não há `new` de caso fora disso, e as únicas peças que dependem de algo (a econômica e a expressa) recebem o `TransLogClient` pelo construtor. A criação está concentrada: um caso novo é uma classe nova, e nenhum ponto de uso precisa mudar para criá-lo. Uma ressalva fica fora dos quatro pontos do F: as situações do pedido são criadas por dois caminhos. O contêiner cria as situações e o `OrderService` as registra num mapa (`service/OrderService.java:47`), mas as próprias situações também fazem `new CancelledState()`, `new PaidState()` etc. nas transições. Essas instâncias vão para um campo `@Transient` que ninguém lê. Os objetos sem regra (item do carrinho, aviso, resumo) são criados direto, por construtor ou setters, sem fábrica nem builder: é contenção. O único defeito é repetição: o mesmo mapeamento de `ItemRequest` aparece copiado cinco vezes. Não há estrutura criacional fora dos pontos.

## Item a item

**F, a criação dos objetos de cada caso.**
- **Forma.** As 14 classes de caso têm `@Component`:
  - entregas: `strategy/shipping/EconomyShipping.java:8`, `ExpressShipping.java:8`, `StorePickupShipping.java:7`, `CourierShipping.java:7`;
  - cupons: `strategy/coupon/Welcome10Coupon.java:11`, `Minus50Coupon.java:11`, `FreeShippingCoupon.java:10`, `Buy3Pay2Coupon.java:12`;
  - pagamentos: `strategy/payment/PixPayment.java:8`, `CreditCardPayment.java:9`, `BankSlipPayment.java:7`;
  - níveis: `strategy/club/BronzeTier.java:7`, `SilverTier.java:7`, `GoldTier.java:7`.
- **Registro.** O registro é montado uma única vez no construtor do `CheckoutService` (`service/CheckoutService.java:45-48`, `Collectors.toMap(XStrategy::getCode, ...)`). Quem usa só consulta o mapa: `shippingMethods.get(...)` (`:197`), `coupons.get(...)` (`:223`), `paymentMethods.get(...)` (`:284`), `clubTiers.get(...)` (`:305`).
- **Dependências.** A econômica e a expressa recebem o `TransLogClient` injetado (`EconomyShipping.java:12-14`, `ExpressShipping.java:12-14`). Nenhuma das duas faz `new TransLogClient`. O cliente também é um `@Component` que lê a URL por `@Value` (`external/TransLogClient.java:11,16`). Ele monta o próprio `RestTemplate` com os timeouts (`TransLogClient.java:18-21`) em vez de recebê-lo injetado, mas isso acontece num lugar só e não se repete.
- **WELCOME10.** O cupom não recebe os pedidos do cliente por dependência. Quem busca é o `CheckoutService` (`:225-230`), que passa a lista de situações como parâmetro de `isApplicable` (`Welcome10Coupon.java:24`). A peça fica sem dependência própria. Isso é questão de desenho do Strategy, não de criação.
- **`new` espalhado.** Não há `new` de nenhum caso das quatro famílias, nem de suas dependências, em nenhum fluxo. O `grep` por `new ` em `src/main` só acha objetos de valor e DTOs.
- **Ressalva, fora das quatro famílias do F.** As situações do pedido (P7) também têm `@Component` e são registradas por `getStatusCode` (`OrderService.java:47`); cada operação pega a situação pelo mapa (`OrderService.java:124,151,174,197`). Mesmo assim, as transições criam instâncias à mão: `order.setState(new CancelledState())` em `state/AwaitingPaymentState.java:30,38,68` e `state/PaidState.java:47`, `new PaidState()` em `AwaitingPaymentState.java:51`, `new ShippedState()` em `PaidState.java:31`, `new DeliveredState()` em `ShippedState.java:35`. O campo é `@Transient` (`domain/Order.java:73-74`) e `getState()` (`Order.java:148`) não é chamado em lugar nenhum. São dois caminhos de criação para os mesmos objetos, e o caminho do `new` não tem efeito. Hoje é inofensivo porque as situações não têm dependências. Se uma ganhar dependência injetada, essas instâncias soltas viriam sem ela.
- **Leitura.** Forma: o contêiner do Spring mais um registro por código montado no serviço. Criação concentrada nas quatro famílias. Item descritivo.

Conclusão: não se aplica

**F-exagero, os objetos sem regra.**
- **Construtor direto, sem fábrica nem builder:**
  - o aviso: `new Notification(cpf, type, orderId)` (`service/OrderService.java:278`). O construtor só preenche os campos e fixa `status = "NEW"` e `createdAt` (`domain/Notification.java:28-34`), sem decisão;
  - o item do pedido: `new OrderItem(orderId, sku, quantity)` (`OrderService.java:91`);
  - os resultados das estratégias: `new ShippingResult(...)`, `new PaymentResult(...)` e `new ClubBenefits(...)` (por exemplo `strategy/shipping/CourierShipping.java:16`, `strategy/payment/PixPayment.java:29`, `strategy/club/GoldTier.java:19`).
- **Construtor vazio e setters:**
  - o resumo: `new SummaryResponse()` (`service/CheckoutService.java:86-97`);
  - o item do carrinho: `new ItemRequest()` (`OrderService.java:105,132,159,182,205`), que é só um POJO com setters (`dto/ItemRequest.java`).
- **Nenhuma estrutura criacional.** O `grep` por `Builder`, `Factory`, `@Bean` e `@Configuration` em `src/main` não acha nada (o único `factory` é o `SimpleClientHttpRequestFactory` do Spring em `TransLogClient.java:18`).
- **Repetição, que não é exagero.** O bloco que transforma `OrderItem` em `ItemRequest` está copiado literalmente nas cinco operações (`OrderService.java:104-110, 131-137, 158-164, 181-187, 204-210`). A montagem do `SummaryResponse` campo a campo aparece duas vezes, uma a partir do cálculo (`CheckoutService.java:86-97`) e outra a partir do pedido gravado (`OrderService.java:320-331`). Um método de conversão simples resolveria; uma fábrica continuaria sendo exagero.

A resposta é do tamanho do problema.

Conclusão: contenção

**E, estrutura sem necessidade fora dos pontos.** Perto da criação de objetos não há fábrica, builder, configuração `@Bean` nem camada de criação que ninguém pediu. O `OrderStateContext` é uma interface com uma única implementação, o próprio `OrderService` (`state/OrderStateContext.java:8`, `service/OrderService.java:21`). Isso pertence ao desenho do State e fica para aquela correção. Os duplos `new` das situações, descritos no F, são criação redundante, não estrutura nova.

Conclusão: contenção

## O que mudaria primeiro

1. **Tirar os `new XState()` das transições.** Basta que cada situação só grave o novo código (`order.setStatus(...)`) e deixar a resolução para o mapa que o `OrderService` já monta, ou então remover o campo `@Transient state`. Hoje existem dois caminhos de criação para as mesmas peças, e um deles não serve para nada. Se uma situação ganhar uma dependência (por exemplo, a futura "em separação" precisando de um serviço do estoque), a instância criada à mão viria sem ela.
2. **Juntar a conversão `OrderItem → ItemRequest` num método só**, ou num construtor ou record `ItemRequest(sku, quantity)`. O bloco copiado cinco vezes em `OrderService.java` é a única repetição de criação do projeto.

## Dúvidas

- O F lista as entregas, os cupons, as formas de pagamento e os níveis. As situações do pedido não estão nessa lista. Registrei a criação delas como ressalva no texto do F, sem que ela pese na leitura das quatro famílias. A forma de cada transição é assunto do State.
- O WELCOME10 não recebe o acesso aos pedidos como dependência: o `CheckoutService` busca os pedidos e passa a lista como parâmetro. Li isso como escolha de interface da estratégia (Strategy), não como criação espalhada, porque nenhum `new` aparece por causa disso.
