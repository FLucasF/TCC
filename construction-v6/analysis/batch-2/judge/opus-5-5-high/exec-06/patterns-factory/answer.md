# Padrões: Factory

## Visão geral

O projeto usa Javalin, sem contêiner de injeção, e toda a montagem fica no `main`. `Application.main` cria o `Database`, os repositórios, o `TransLogClient`, as calculadoras, os serviços e os controllers, e entrega cada peça pelo construtor (`src/main/java/com/loja/Application.java:26-68`). Nenhuma classe de produção cria as próprias dependências fora dali.

Só os cupons têm um objeto para cada caso. As quatro classes são criadas com `new` num lugar só, o construtor de `CouponService`, que serve de registro por código (`src/main/java/com/loja/service/coupon/CouponService.java:11-20`).

A entrega, a forma de pagamento e o nível do clube não têm objeto por caso. A variação fica num `switch` ou num `if` dentro de uma calculadora única, então nesses três a criação não tem o que concentrar.

Os objetos sem regra (o item, o resumo, o pedido, o aviso, o resultado do frete) são criados direto, por construtor ou por setters, sem nenhuma fábrica ou builder. Também não encontrei estrutura sobrando perto da criação.

## Item a item

**F, como os objetos de cada caso são criados.** Este item só descreve; vou por ponto.

- **Cupons:** as quatro casas são `Welcome10Coupon`, `Minus50Coupon`, `FreeShippingCoupon` e `Buy3Pay2Coupon`. São criadas uma única vez, com `new`, dentro do construtor de `CouponService` (`CouponService.java:12-17`), e registradas pelo `getCode()` de cada uma (`:18-20`). O `CouponService` é criado no `main` (`Application.java:40`).
  - A criação está concentrada, mas no registro e não no `main`. Um cupom novo muda `CouponService.java:12-17`, e não quem usa os cupons.
  - O WELCOME10 precisa saber dos pedidos do cliente, mas não recebe essa dependência. Quem chama calcula `isFirstPurchase` com o `OrderRepository` (`CheckoutService.java:62`) e o passa como parâmetro da interface (`CouponStrategy.java:10`). Por isso nenhum cupom tem dependência para receber, e o `new` sem argumentos do registro dá conta.
  - Isso é uma escolha de desenho: a dependência de um único cupom virou parâmetro de todos. Essa leitura cabe mais ao Strategy do que aqui.
- **Entregas:** não há objeto por entrega. `ShippingCalculator.calculate` faz um `switch` sobre `ShippingMethod` (`ShippingCalculator.java:20-47`), e o `TransLogClient` também nomeia os casos (`TransLogClient.java:30` e `:58-62`). A única dependência, o `TransLogClient`, é criada uma vez no `main` (`Application.java:39`) e entregue ao `ShippingCalculator` (`:41`). Não existe `new TransLogClient(...)` repetido em produção.
- **Formas de pagamento:** não há objeto por caso; a variação é um `switch` em `PaymentAdjustmentCalculator.java:11-34`.
- **Níveis do clube:** não há objeto por caso. `ClubTier` é um enum sem corpo (`domain/ClubTier.java:3-7`), e a variação fica num `switch` e em comparações com `GOLD` (`ClubBenefitsCalculator.java:11-15`, `:19`, `:23`).
- **Montagem repetida:** a mesma montagem do cálculo aparece de novo, peça por peça, no teste (`src/test/java/com/loja/CheckoutServiceTest.java:30-56`), para trocar o `TransLogClient` por um mock. É código de teste montando o próprio cenário, e não o espalhamento de `new` entre fluxos de produção. Mesmo assim, se o `CheckoutService` ganhar uma dependência, os dois lugares mudam juntos.

Resumo: a criação é a montagem à mão num lugar só (o `main`), e o registro de cupons é uma pequena fábrica interna. Nas entregas, pagamentos e níveis não há objeto por caso; a variação fica em `switch`, e isso é assunto do Strategy.
Conclusão: não se aplica

**F-exagero, os objetos sem regra.** Todos são criados direto:
- O item do carrinho tem um construtor simples (`domain/OrderItem.java:10-13`), usado no teste e no repositório (`repository/OrderRepository.java:149`).
- O resumo é preenchido por setters no próprio cálculo (`service/CheckoutService.java:100-111`).
- O pedido é preenchido por setters no registro (`service/OrderService.java:43-56`).
- O aviso nem vira objeto na escrita: `NotificationRepository.create` grava os campos direto no banco (`repository/NotificationRepository.java:20-30`). Na leitura, ele é montado por construtor (`repository/NotificationRepository.java:78`).
- O resultado do frete é um construtor de dois campos (`service/shipping/ShippingResult.java:9-12`).

Um `grep` por `factory`, `builder` e por métodos estáticos `of`/`create` em `src/main` não acha nenhuma classe desse tipo; os únicos builders são os da API do JDK (`HttpClient.newBuilder`, `HttpRequest.newBuilder`, em `TransLogClient.java:23` e `:37`). A resposta tem o tamanho do problema.
Conclusão: contenção

**E, estrutura sem necessidade fora dos pontos.** Perto da criação, não há interface com uma implementação só, nem camada que só repassa, nem fábrica ou builder que ninguém pediu.
- O `TransLogClient` é uma classe concreta, sem interface por cima (`TransLogClient.java:16`). O teste a substitui herdando dela (`CheckoutServiceTest.java:92-104`).
- A única interface de produção, `CouponStrategy`, tem quatro implementações, e o enunciado pede essa variação.
- O `CouponService` guarda o registro e aplica o teto de 30% (`CouponService.java:31-37`); não é uma camada que só repassa.
Conclusão: contenção

## O que mudaria primeiro

1. **Levar a criação dos cupons para a montagem do `main`.** Hoje o `CouponService` cria os cupons por dentro (`CouponService.java:12-17`). O `main` poderia passar a lista ao registro, e o WELCOME10 receberia o `OrderRepository` no construtor, em vez de todos os cupons receberem `isFirstPurchase`. Assim uma dependência nova de um cupom muda só a montagem e aquele cupom, sem mexer na interface de todos.
2. **Tirar a montagem do cálculo do teste.** Hoje ela está duplicada em `CheckoutServiceTest.java:30-56`. Uma classe de composição usada pelo `main` e pelos testes, com o `TransLogClient` como parâmetro, faria uma dependência nova do `CheckoutService` mudar um lugar só. O ganho é pequeno, porque hoje são só dois lugares.

## Dúvidas

- Decidi tratar a criação dos cupons dentro do construtor de `CouponService` como concentrada, mesmo fora do `main`. Ela acontece num lugar só, uma vez, e quem usa os cupons não os cria. A régua aceita "uma fábrica (um registro)" como forma concentrada.
- Não contei a montagem repetida no teste como o "`new` espalhado" que a régua manda registrar. É um teste preparando o próprio cenário, não dois fluxos de produção criando suas peças. Mesmo assim deixei a duplicação descrita no F.
