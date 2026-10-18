# Padrões: Strategy

## Visão geral

O serviço não usa Strategy em nenhum dos quatro pontos que pedem o padrão. Os enums `ShippingMethod`, `CouponCode`, `PaymentMethod` e `ClubTier` são só rótulos, sem corpo nem dados. O Jackson converte o texto da requisição para a constante, e um serviço por ponto (`ShippingService`, `CouponService`, `PaymentService`, `ClubService`) faz a conta num `switch` que nomeia os casos. Quando um caso só aparece num `if`, ele é comparado com `==`.

O problema mais sério é que o código de cada caso está espalhado. O `CheckoutService` acumula regras de casos isolados:
- o limite de 5 kg do motoboy;
- a exceção do GOLD no motoboy;
- a regra de parcelas de cada forma de pagamento;
- o teto do boleto;
- a consulta do histórico do WELCOME10.

Boa parte disso aparece duas vezes, porque `validateRequest` refaz o cálculo inteiro só para o boleto (`CheckoutService.java:190-223`). Um caso novo em qualquer ponto mexe no enum, no `switch` do serviço do ponto e, muitas vezes, no `CheckoutService`.

Há dois acertos menores. Duas regras cruzadas (FREESHIPPING na retirada e Pix sem cupom) ficaram no código do próprio ponto, e não num terceiro lugar. A região, o controle negativo, foi resolvida sem estrutura, só com `switch` que devolve números. Não há estrutura sem necessidade.

## Item a item

**P1-local, a entrega.** `ShippingMethod` é um enum sem corpo (`src/main/java/com/shop/model/enums/ShippingMethod.java:3-8`), então nenhum caso tem casa e todo código específico de uma entrega está fora de casa:
- a conta de cada opção no `switch` de `src/main/java/com/shop/service/ShippingService.java:41-46` (retirada `ZERO, 1` e motoboy `18.00, 0` escritos ali mesmo);
- a limitação do motoboy (acima de 5 kg) em `src/main/java/com/shop/service/CheckoutService.java:164-167`, no serviço geral;
- o `COURIER` nomeado no cálculo do frete do GOLD em `CheckoutService.java:60` e `:196` (ver R1);
- o `STORE_PICKUP` dentro do ramo do FREESHIPPING em `src/main/java/com/shop/service/CouponService.java:34` (ver R2).

A tradução `"STD"`/`"EXP"` (`ShippingService.java:42-43`) é trabalho do lado da TransLog e não espalha a entrega. Os dias a mais do Norte e do Nordeste estão em `calculateTranslog` (`ShippingService.java:81-85`), que é comum às duas opções da TransLog. O parâmetro `baseDays` (7 e 0) é passado nas linhas 42-43 e nunca é lido (`ShippingService.java:49`). Não muda a conta, mas deixa números por caso soltos na chamada.
Conclusão: omissão

**P1-selecao, a entrega.** O texto `shippingMethod` vira a constante pelo Jackson (`src/main/java/com/shop/dto/CheckoutRequest.java:16`). Depois, um `switch (method)` nomeia os quatro casos e faz a conta no mesmo lugar (`ShippingService.java:41-46`). É uma condicional no cálculo.
Conclusão: omissão

**P2-local, o cupom.** `CouponCode` é um enum sem corpo (`CouponCode.java:3-8`), então nenhum cupom tem casa. A conta e a condição de cada cupom estão nos braços de um `switch` em `CouponService.java:19-40`. O BUY3PAY2 tem um método privado na mesma classe (`CouponService.java:56-71`), mas um método privado do serviço não é casa.

Fora do `CouponService`, o histórico que só o WELCOME10 usa é consultado no serviço geral, duas vezes: `CheckoutService.java:64-68` e `:200-204`. Ele chega a todos os cupons como o parâmetro `isFirstPurchase` (`CouponService.java:18`). O teto de 30% (`CouponService.java:46-51`) é comum a todos e não conta como código de um caso.
Conclusão: omissão

**P2-selecao, o cupom.** A constante vem do Jackson (`CheckoutRequest.java:17`) e é ramificada no `switch (coupon)`, que calcula o desconto de cada caso ali mesmo (`CouponService.java:19-40`). É uma condicional no cálculo. A recusa genérica `COUPON_NOT_APPLICABLE` (`CheckoutService.java:76-77`) não nomeia caso.
Conclusão: omissão

**P3-local, a forma de pagamento.** `PaymentMethod` é um enum sem corpo (`PaymentMethod.java:3-7`). O ajuste de cada forma está em métodos privados de `PaymentService` (`PaymentService.java:36-74`), escolhidos por `switch` (`:29-33`). As outras regras de cada forma estão no `CheckoutService`:
- as parcelas permitidas, num segundo `switch` que nomeia `PIX, BANK_SLIP` e `CREDIT_CARD` (`CheckoutService.java:177-188`);
- a recusa do boleto acima de R$ 1.000,00, num `if (... == PaymentMethod.BANK_SLIP)` que refaz frete, cupom e seguro para chegar ao total (`CheckoutService.java:190-223`).

Uma forma nova mexe em três lugares.
Conclusão: omissão

**P3-selecao, a forma de pagamento.** O `switch (method)` em `PaymentService.java:29-33` nomeia os casos. Cada braço chama um método privado com a conta do caso, mas esses métodos são da mesma classe e o `switch` está dentro do cálculo do pagamento: não é uma fábrica que devolve um objeto. É uma condicional no cálculo. Além disso, o `switch` das parcelas (`CheckoutService.java:177`) e o `if` do boleto (`:190`) escolhem o caso de novo.
Conclusão: omissão

**P4-local, o nível do clube.** `ClubTier` é um enum sem corpo (`ClubTier.java:3-7`), e as vantagens são todas comparações de caso em `ClubService`:
- a porcentagem do crédito num `switch` (`ClubService.java:11-15`);
- o brinde com `tier == ClubTier.GOLD` (`:22`);
- o frete grátis com `tier == ClubTier.GOLD` (`:26`).

Fora do `ClubService`, a exceção do frete do GOLD no motoboy está no serviço geral, duas vezes (`CheckoutService.java:59-62` e `:195-198`). Um nível novo com frete grátis ou brinde mexeria em `ClubService` linha a linha.
Conclusão: omissão

**P4-selecao, o nível do clube.** O enum vem do Jackson (`CheckoutRequest.java:20`) e cada vantagem nomeia os casos onde a conta é feita: o `switch` de `ClubService.java:11` e os `==` de `:22` e `:26`. É uma condicional no cálculo. Os casos diferem só em dados (porcentagem, frete grátis, limiar do brinde), e um enum com esses dados seria a resposta do tamanho do problema. Mas aqui os dados estão em condicionais, não no enum.
Conclusão: omissão

**R1, o GOLD não paga frete, exceto no motoboy.** A regra está no serviço geral, e não no nível: `if (clubService.hasFreeShipping(request.getClubTier()) && request.getShippingMethod() != ShippingMethod.COURIER)` zera o frete em `CheckoutService.java:59-62`. A mesma linha está copiada em `:195-198`. É o remendo descrito na régua, só que com o GOLD escondido atrás de `hasFreeShipping`, que por sua vez nomeia o GOLD (`ClubService.java:26`). Como não existe estrutura de nível, chamo de omissão, e não de malfeito.
Conclusão: omissão

**R2, o FREESHIPPING não vale na retirada.** A regra fica no braço do FREESHIPPING dentro de `CouponService` (`CouponService.java:33-36`, `if (shippingMethod == ShippingMethod.STORE_PICKUP) yield null`). Ela não espalha para um terceiro lugar: o `CheckoutService` só converte o `null` em recusa genérica (`:76-77`). Mas não há casa do FREESHIPPING; a regra é um braço do `switch` que já é a omissão do P2.
Conclusão: omissão

**R3, o desconto do Pix só sem cupom.** A regra fica em `calculatePix`, que recebe o cupom e o desconto como contexto (`PaymentService.java:36-39`). O `switch` repassa esse contexto só ao Pix (`:30`). Não está no serviço geral. Mas, como no R2, mora num método privado do `PaymentService` escolhido por `switch`, e não numa casa do Pix. À parte, e fora desta dimensão: a condição `couponDiscount > 0` (`:37`) dá o desconto a quem usou um cupom de desconto zero, ao contrário do que o enunciado pede.
Conclusão: omissão

**R4, o WELCOME10 só na primeira compra.** A decisão está no braço do WELCOME10 (`CouponService.java:20-23`), mas a consulta do histórico está no serviço geral: `orderRepository.countPaidOrdersByCpf` em `CheckoutService.java:64-68`, repetida em `:200-204`. Ela roda para qualquer cupom e chega pronta como `boolean isFirstPurchase`. É o remendo descrito na régua: o cupom não consulta os pedidos, o serviço consulta por ele. À parte, e fora desta dimensão: o resumo passa `conn = null` (`src/main/java/com/shop/controller/CheckoutController.java:20`), e com isso `isFirstPurchase` fica `false` no resumo.
Conclusão: omissão

**P5-forma, a região.** `Region` é um enum sem corpo (`Region.java:3-9`). A variação está em três `switch` que só devolvem números:
- a porcentagem do seguro (`CheckoutService.java:250-255`);
- o limite do seguro (`:260-263`);
- os dias a mais (`ShippingService.java:81-85`).

Nenhum devolve objeto com comportamento. A conta é uma só para todas as regiões (`CheckoutService.java:257-269`).
Conclusão: contenção

**P5-proporcao, a região.** Não há classe por região, constante com corpo nem mapa de objetos: a resposta tem o tamanho do problema. É condicional, e não dado. Os números de uma região estão espalhados em três `switch` de duas classes, e uma região nova mexe nos três. Um enum com `percentage`, `cap` e `extraDays` seria mais direto, mas a forma atual é aceitável e não é exagero.
Conclusão: contenção

**E, estrutura sem necessidade fora dos pontos.** Perto deste pattern não há nada criado sem necessidade. O projeto não tem nenhuma interface, classe abstrata nem `implements`; a única herança é `BusinessException extends RuntimeException`. Os serviços são classes concretas montadas à mão em `src/main/java/com/shop/Main.java:107-114`. `ShippingQuote` e `PaymentCalculation` são só portadores de resultado.
Conclusão: contenção

## O que mudaria primeiro

1. **Dar casa a cada caso de cupom e de entrega**, os dois pontos que o enunciado diz que crescem toda semana. Por exemplo, uma interface `Coupon` com `discount(contexto)` e uma implementação por código, achadas num `Map`. O WELCOME10 consultaria o histórico pelo contexto, e o FREESHIPPING recusaria a retirada dentro da própria classe. Para a entrega, uma interface `ShippingOption` com `quote(...)` e `isAvailable(...)`, para que o limite de 5 kg saia de `CheckoutService.java:164`.
2. **Tirar do `CheckoutService` as regras de casos isolados e a cópia do cálculo em `validateRequest`** (`CheckoutService.java:177-223`). Parcelas e teto do boleto iriam para cada forma de pagamento. O frete grátis do clube, com a exceção do motoboy, iria para o nível, num `ClubTier` que carregue os próprios dados ou o próprio comportamento. Hoje a mesma regra existe em dois lugares do mesmo arquivo, que podem divergir.

## Dúvidas

- **R2 e R3**: as duas regras não espalham para um terceiro lugar; ficam no código do próprio ponto. Mas também não estão numa casa, porque o ponto não tem estrutura. Como `acerto` exige o padrão aplicado e `malfeito` exige a estrutura existir, escolhi `omissão`, a mesma conclusão do ponto onde a regra mora. Deixei claro no texto que não é o remendo do serviço geral.
- **Métodos privados por caso** (`PaymentService.calculatePix` e outros, `CouponService.calculateBuy3Pay2Discount`): não contei como casa, porque a régua define casa como classe, constante de enum com corpo ou entrada de mapa com função própria. Também não contei o `switch` que os chama como fábrica, porque ele não devolve um objeto para outro calcular.
