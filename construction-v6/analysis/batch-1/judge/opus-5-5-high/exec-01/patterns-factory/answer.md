# Padrões: Factory

## Visão geral

O agente deixou toda a criação dos objetos de cada caso com o contêiner do Spring. Cada cupom, forma de pagamento e nível do clube é uma classe `@Component`, e a retirada e o motoboy também. As duas entregas da TransLog usam a mesma classe, `CarrierShipping`, com parâmetros diferentes. Elas são montadas em dois métodos `@Bean` de uma configuração, e é ali que o cliente da TransLog é injetado. O `CheckoutService` recebe as listas de cada tipo no construtor e monta, uma única vez, um registro por código. Nenhum outro lugar do código de produção faz `new` de um caso ou das dependências dele. O cálculo pega as peças prontas, e o pedido usa o mesmo `CheckoutService`.

Os objetos sem regra são criados direto: records com construtor e um `of` estático simples. Não há builder nem fábrica para eles. Também não achei estrutura sobrando perto da criação.

Um detalhe, que pesa mais no Strategy do que aqui: o WELCOME10 não recebe a dependência dos pedidos. Quem consulta o `OrderRepository` é o `CheckoutService`, que entrega ao cupom só um booleano, `firstPurchase`.

## Item a item

**F, a criação dos objetos de cada caso.**
- **Entregas.** `StorePickupShipping` (`src/main/java/br/com/loja/shipping/StorePickupShipping.java:7`) e `CourierShipping` (`shipping/CourierShipping.java:11`) são `@Component`. A econômica e a expressa são duas instâncias de `CarrierShipping`, criadas uma vez cada nos métodos `@Bean` de `shipping/ShippingConfig.java:9-17` (`new CarrierShipping("ECONOMY", "STD", gateway)` e `new CarrierShipping("EXPRESS", "EXP", gateway)`). O `CarrierGateway` chega por injeção, e a implementação dele é `TransLogGateway`, um `@Component` (`shipping/TransLogGateway.java:16`). Trocar de transportadora muda só o bean do gateway. Criar uma opção nova é criar um bean novo.
- **Cupons.** `Welcome10Coupon`, `Minus50Coupon`, `FreeShippingCoupon` e `Buy3Pay2Coupon` são `@Component` sem dependências (`coupon/Welcome10Coupon.java:11` e equivalentes). A dependência do WELCOME10 com os pedidos não fica no objeto do cupom. O `CheckoutService` calcula `firstPurchase` a partir de `orders.hasPurchaseByCustomer(...)` e passa no `CouponContext` (`checkout/CheckoutService.java:77-78`). A consulta roda para qualquer cupom informado, e não só para o WELCOME10. Isso não espalha a criação, mas tira do cupom uma dependência que é só dele.
- **Formas de pagamento.** `PixPayment`, `CreditCardPayment` e `BankSlipPayment` são `@Component` (`payment/PixPayment.java:7`, `payment/CreditCardPayment.java:13`, `payment/BankSlipPayment.java:11`). Pix e boleto estendem uma base sem anotação, `CashPayment` (`payment/CashPayments.java:7`).
- **Níveis.** `BronzeTier`, `SilverTier` e `GoldTier` são `@Component` (`club/BronzeTier.java:9`, `club/SilverTier.java:9`, `club/GoldTier.java:9`).
- **Regiões.** São constantes de enum que só carregam dados (`shipping/Region.java:10-14`). Nada é montado em tempo de execução.
- **Quem usa.** O `CheckoutService` recebe `List<ShippingOption>`, `List<Coupon>`, `List<ClubTier>` e `List<PaymentOption>` por injeção e indexa cada lista por `code()` (`checkout/CheckoutService.java:44-52`, `index` em `:138-142`). A escolha do caso é uma busca no mapa (`find`, `:130-136`). O `OrderService` não cria peça nenhuma: ele chama `checkout.summarize(request)` (`order/OrderService.java:51`).

Busquei `new` dos casos no código inteiro, testes incluídos. Só aparecem as duas linhas de `ShippingConfig`. Não há `new` repetido de casos nem das dependências deles.

A forma, portanto, é o contêiner do Spring, com dois métodos `@Bean` para as entregas que dividem uma classe, e a criação está concentrada.
Conclusão: não se aplica

**F-exagero, os objetos sem regra.**
- O item do carrinho é o record `CartLine`, criado com construtor (`checkout/CheckoutService.java:123`). O item do pedido é o record `OrderItem`, também com construtor (`order/OrderService.java:53`).
- O resumo é o record `OrderSummary`. O cálculo o cria com construtor (`checkout/CheckoutService.java:91-93`), e o repositório faz o mesmo ao ler do banco (`order/OrderRepository.java:67`).
- O aviso é o record `Notification` (`notification/Notification.java:3`), montado no mapeamento da linha (`notification/NotificationRepository.java:35`). A gravação é um `INSERT` direto no repositório (`notification/NotificationRepository.java:18-21`, chamado em `notification/CustomerNotificationListener.java:20-30`).
- A resposta HTTP do pedido usa um método estático simples, `OrderResponse.of(Order)`, que só copia campos (`order/OrderController.java:22-24`).
- Os objetos de parâmetro (`ShippingRequest`, `CouponContext`, `PaymentContext`, `PaymentCharge`, `ShippingQuote`) são todos records criados com construtor.

Os únicos `builder()` do código são da biblioteca: `HttpClient.newBuilder()`, `JsonMapper.builder()` e `HttpRequest.newBuilder` (`shipping/TransLogGateway.java:22-23,33`). Não há builder nem fábrica para objeto que só junta campos.
Conclusão: contenção

**E, estrutura sem necessidade fora dos pontos (perto da criação).** `ShippingConfig` existe porque as duas entregas da TransLog dividem uma classe e precisam de parâmetros. Ela não é uma camada de repasse. `CarrierGateway`, que tem só a implementação `TransLogGateway`, responde a um ponto do enunciado ("pode ser que um dia a gente troque de transportadora"), que fica para o Adapter. `OrderEventListener` responde ao SMS e ao cashback do futuro. O único `@Bean` fora dos casos é o `Clock` (`LojaApplication.java:16-19`), que serve para injetar o relógio e não acrescenta camada. Não achei interface, fábrica ou builder que ninguém peça.
Conclusão: contenção

## O que mudaria primeiro

Passaria a dependência de "primeira compra" para dentro do WELCOME10. O `Welcome10Coupon` receberia o `OrderRepository` por injeção (ou uma porta mais estreita), e o contexto passaria só o CPF. Assim a consulta deixaria de rodar para todos os cupons (`checkout/CheckoutService.java:77`). E um cupom novo com dependência própria (pedidos, cadastro, parceiro) seria só um bean com aquela dependência, sem mexer no `CheckoutService` nem no `CouponContext`. Fora isso, a criação já está concentrada, e não há o que mudar com urgência.

## Dúvidas

O guia diz que "o WELCOME10 precisa dos pedidos do cliente". Aqui o objeto do cupom não tem essa dependência: quem a tem é o `CheckoutService`, que entrega o resultado pronto. Li isso como uma escolha de desenho que não espalha a criação (o objeto continua vindo do contêiner, e a consulta acontece num lugar só). Por isso só descrevi o caso no F, e o efeito no desenho fica para o Strategy.
