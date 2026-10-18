# Padrões: Strategy

## Visão geral

O agente montou o Strategy nos quatro pontos que pedem o padrão, e o serviço já nasceu assim. A entrega, o cupom e a forma de pagamento têm cada um uma interface (`ShippingOption`, `Coupon`, `PaymentMethod`) e uma classe por caso. O caso é achado por consulta num `Map` indexado pelo código (`ShippingOptions`, `Coupons`, `PaymentMethods`). O nível do clube é um enum que só carrega os dados de cada nível, com um método só para todos. O `CheckoutCalculator` não nomeia nenhum caso: só pergunta às estratégias (`quote`, `discount`, `charge`, `waivesShipping`, `credit`, `gift`). Uma busca pelos nomes dos casos em `src/main` só encontra as casas e as listas de registro.

As quatro regras cruzadas moram na estrutura. Em duas delas o código nem nomeia o caso do outro ponto: o cupom pergunta `chargesFreight()` à entrega, e o nível pergunta `clubWaivable()` à entrega. A região, o controle negativo, foi resolvida como dado.

Não achei nenhum problema sério. Há três ressalvas menores, sem efeito nas conclusões:
- o histórico de compras é consultado pelo calculador e entregue pronto ao cupom, em vez de o cupom consultar;
- o "cupom informado" chega à forma de pagamento como um `boolean` solto na assinatura;
- a tradução para o código da TransLog (`STD`/`EXP`) está na lista de registro das entregas, e não no adaptador.

## Item a item

**P1-local, a entrega: onde mora o código de cada caso.** `STORE_PICKUP` tem a sua classe (`src/main/java/shop/domain/shipping/StorePickup.java:5-25`), com o frete zero e o prazo de 1 dia. `COURIER` também (`src/main/java/shop/domain/shipping/Courier.java:8-35`), com o R$ 18,00, o prazo 0, o limite de 5 kg (`Courier.java:19-21`) e a exceção do clube (`Courier.java:31-34`).

`ECONOMY` e `EXPRESS` dividem a classe `CarrierShipping` (`src/main/java/shop/domain/shipping/CarrierShipping.java:8`). Elas diferem só num dado, o serviço da transportadora, passado no construtor. As instâncias são criadas em `ShippingOptions.java:18-19`, que funciona como uma constante de enum que recebe os seus dados. Os dias a mais do Norte e do Nordeste valem só para essas duas, e ficam dentro de `CarrierShipping.java:29` (`request.region().extraCarrierDays()`), sem nomear região. Os códigos de erro da opção indisponível também ficam nas casas (`CarrierShipping.java:31-34`, `Courier.java:20`).

Fora das casas, nenhuma linha faz algo só para uma entrega. O calculador só chama `shipping.quote(...)` e `tier.waivesShipping(shipping)` (`src/main/java/shop/service/CheckoutCalculator.java:55-56`).

Um detalhe: a tradução `"ECONOMY" -> "STD"` e `"EXPRESS" -> "EXP"` está na lista de registro (`ShippingOptions.java:18-19`), e não do lado da TransLog (`TransLogClient.java`). Isso não espalha a entrega, mas trocar de transportadora mexeria nessa lista. Isso é assunto do Adapter, e não pesa aqui.
Conclusão: acerto

**P1-selecao, a entrega: como o caso é escolhido.** Por consulta. `shippingOptions.find(req.shippingMethod())` busca no mapa `byCode` (`ShippingOptions.java:9`, `24-26`; `CheckoutCalculator.java:50-51`). O único `orElseThrow` é uma recusa (`INVALID_SHIPPING_METHOD`). Uma entrega nova é uma classe nova mais uma linha em `ShippingOptions.standard`.
Conclusão: acerto

**P2-local, o cupom: onde mora o código de cada caso.** Cada cupom tem a sua classe com a sua conta e a sua condição:
- o 10% e a primeira compra em `src/main/java/shop/domain/coupon/Welcome10.java:15-20`;
- o mínimo de R$ 300 e os R$ 50 em `Minus50.java:17-22`;
- o frete e a recusa na retirada em `FreeShipping.java:14-19`;
- as unidades a cada 3 em `Buy3Pay2.java:13-19`.

O teto de 30% é uma regra de todos os cupons, numa função genérica (`Coupons.java:29-32`). O calculador só testa se veio cupom (`CheckoutCalculator.java:59`) e chama `coupon.discount(...)` e `Coupons.cap(...)` (`CheckoutCalculator.java:63-64`). Nenhuma linha fora das casas nomeia ou trata um cupom específico.

Há uma ressalva, que volta no R4: `firstPurchase` é calculado pelo calculador para qualquer cupom (`CheckoutCalculator.java:62`) e só o WELCOME10 usa. Pela régua, isso não conta como código de um caso: é uma consulta genérica que não cita o cupom.
Conclusão: acerto

**P2-selecao, o cupom: como o caso é escolhido.** Por consulta. `coupons.find(req.coupon())` busca no mapa `byCode` (`Coupons.java:14`, `25-27`; `CheckoutCalculator.java:60-61`), com recusa por `INVALID_COUPON`. O `if (req.coupon() != null)` (`CheckoutCalculator.java:59`) só testa se o cupom veio, sem nomear caso.
Conclusão: acerto

**P3-local, a forma de pagamento: onde mora o código de cada caso.** Cada forma tem a sua classe, com as parcelas aceitas, a recusa e o ajuste:
- Pix: só à vista e o desconto de 5% sem cupom (`src/main/java/shop/domain/payment/Pix.java:15-28`);
- cartão: de 1 a 12 parcelas, sem juros até 3x, tabela Price acima disso (`CreditCard.java:20-40`);
- boleto: só à vista, o limite de R$ 1.000 em `availableFor` e a tarifa de R$ 3,49 (`BankSlip.java:16-29`).

O calculador só pergunta: `acceptsInstallments`, `availableFor` e `charge` (`CheckoutCalculator.java:69-77`). A escolha dos códigos de erro (`INVALID_INSTALLMENTS`, `PAYMENT_METHOD_UNAVAILABLE`) fica no calculador, mas segue a resposta da forma, sem nomear caso. Nenhuma linha fora das casas faz algo só para uma forma.
Conclusão: acerto

**P3-selecao, a forma de pagamento: como o caso é escolhido.** Por consulta. `paymentMethods.find(req.paymentMethod())` busca no mapa `byCode` (`PaymentMethods.java:9`, `20-22`; `CheckoutCalculator.java:67-68`), com recusa por `INVALID_PAYMENT_METHOD`.
Conclusão: acerto

**P4-local, o nível do clube: onde mora o código de cada caso.** `ClubTier` é um enum que carrega três dados por nível: a porcentagem do crédito, se tem frete grátis e o valor acima do qual vai brinde (`src/main/java/shop/domain/ClubTier.java:8-10`). Os métodos são um só para todos: `credit` (`ClubTier.java:30-32`), `waivesShipping` (`ClubTier.java:34-36`) e `gift` (`ClubTier.java:38-40`). Hoje os três níveis diferem só nesses dados, o que a régua aceita como acerto.

Busquei `credit`, `gift`, `freeShipping` e `waives` fora do enum. Só há chamadas genéricas (`CheckoutCalculator.java:56`, `81`) e o uso do crédito já calculado no resumo (`src/main/java/shop/service/CreditListener.java:9`, `15`). Nenhum `GOLD` ou `SILVER` aparece fora das constantes.

Fica uma dúvida para o futuro, não um defeito: um nível com uma vantagem de outro tipo (diferente de crédito, frete ou brinde por valor) pediria um campo novo para todos os níveis, ou corpo próprio na constante.
Conclusão: acerto

**P4-selecao, o nível do clube: como o caso é escolhido.** Por consulta. `ClubTier.parse` faz `ClubTier.valueOf(value)` (`ClubTier.java:22-28`; `CheckoutCalculator.java:45`), e o enum carrega o comportamento por meio dos dados. O `catch` só recusa (`INVALID_CLUB_TIER`).
Conclusão: acerto

**R1, o GOLD não paga frete, exceto no motoboy.** A regra fica entre o nível e a entrega, sem nomear caso nenhum. O nível diz se tem frete grátis (`ClubTier.java:10`, `35`: `freeShipping && option.clubWaivable()`). O motoboy diz que o benefício do clube não vale nele (`Courier.java:30-34`, `clubWaivable()` devolve `false`). As outras entregas devolvem `true` (`CarrierShipping.java:43-46`, `StorePickup.java:21-24`).

O calculador aplica a resposta sem condicional sobre os casos (`CheckoutCalculator.java:56`). Não há `if (tier == GOLD && method != COURIER)` em lugar nenhum. É a regra na casa do par, só que sem nem citar o par.
Conclusão: acerto

**R2, o FREESHIPPING não vale na retirada.** A regra fica no cupom (`FreeShipping.java:15-17`). Ele pergunta à entrega `c.shipping().chargesFreight()` e recusa com `COUPON_NOT_APPLICABLE` se ela não cobra frete. A retirada responde `false` (`StorePickup.java:16-19`). Não há `STORE_PICKUP` citado no serviço nem no controller.
Conclusão: acerto

**R3, o desconto do Pix só sem cupom.** A regra fica no Pix (`Pix.java:26`: `couponInformed ? orderTotal : orderTotal.subtract(...)`), que recebe o contexto. O calculador passa `req.coupon() != null` a qualquer forma (`CheckoutCalculator.java:77`), sem nomear o Pix. O cartão e o boleto ignoram o parâmetro (`CreditCard.java:30`, `BankSlip.java:26`).

Ressalva: o contexto é um `boolean` solto na assinatura da interface (`PaymentMethod.java:14`). Uma próxima regra cruzada desse tipo aumentaria a assinatura de todas as formas. Um objeto de contexto, como o `CouponContext` dos cupons, cresceria melhor. A regra mora certo.
Conclusão: acerto

**R4, o WELCOME10 só na primeira compra.** Quem decide é o cupom (`Welcome10.java:16-18`), que recusa quando `!c.firstPurchase()`. A consulta ao histórico, porém, é feita pelo calculador (`CheckoutCalculator.java:62`: `!tx.orders().hasPurchased(req.cpf())`) e entregue pronta no `CouponContext` (`src/main/java/shop/domain/coupon/CouponContext.java:8-9`).

A linha não nomeia o WELCOME10 e roda para qualquer cupom. Pela régua, é uma função genérica que não cita o caso, e não um remendo. Não há checagem no controller (`src/main/java/shop/web/` não toca no histórico).

Ainda assim, fica um pouco abaixo do ideal da régua ("no cupom, que consulta os pedidos"). O serviço sabe que algum cupom precisa saber da primeira compra. Um cupom novo que dependesse de outra informação do histórico obrigaria a mexer no calculador e no `CouponContext`. Não há `if` sobre o cupom, então fico com acerto, com essa ressalva.
Conclusão: acerto

**P5-forma, a região.** Um enum que só carrega números: a porcentagem do seguro, o limite (ou `null`) e os dias a mais na transportadora (`src/main/java/shop/domain/Region.java:6-10`). Os métodos são um só para todas as regiões: `insurance` (`Region.java:30-33`) e `extraCarrierDays` (`Region.java:36-38`). Nenhuma constante tem corpo próprio.
Conclusão: contenção

**P5-proporcao, a região: a resposta tem o tamanho do problema.** Tem. A variação é dado puro, numa tabela em forma de enum. Não há classe por região, constante com corpo, nem `switch`. Os dias a mais também são dado, lidos por `CarrierShipping.java:29` sem nomear região.
Conclusão: contenção

**E, estrutura sem necessidade fora dos pontos.** Perto deste pattern, não há. Os três registros (`ShippingOptions`, `Coupons`, `PaymentMethods`) são o mecanismo de seleção, cada um com um mapa e um `find`. `ShippingRequest`, `ShippingQuote`, `CouponContext` e `PaymentCharge` são records de entrada e saída das estratégias. Não há interface com uma implementação só nem camada de repasse em torno do cálculo.

Fora do alcance do Strategy, há duas estruturas, que deixo para as correções certas. O `CarrierClient` tem uma implementação só, mas o enunciado prevê trocar de transportadora: é o ponto do Adapter. O `Store`/`Tx` tem `MemoryStore` em `src/main`, usado só pelos testes (`src/test/java/shop/CheckoutExamplesTest.java:36`): é assunto de outra dimensão.
Conclusão: contenção

## O que mudaria primeiro

1. **Levar a consulta da primeira compra para dentro do cupom.** O `CouponContext` passaria a levar acesso ao histórico do CPF (por exemplo, o repositório de pedidos ou uma função `cpf -> boolean`), e não o `boolean` pronto. Assim, um cupom novo que dependa do histórico não mexe no `CheckoutCalculator`, e a consulta só roda quando o WELCOME10 é usado.
2. **Trocar o `boolean couponInformed` de `PaymentMethod.charge` por um objeto de contexto**, como já foi feito nos cupons. A próxima regra cruzada de pagamento entraria no contexto, sem mudar a assinatura das três formas.

## Dúvidas

- **ECONOMY e EXPRESS numa só classe.** Elas dividem `CarrierShipping` e diferem só no serviço passado no construtor. Contei cada instância registrada como a casa do seu caso, como uma constante de enum que recebe dados, porque as duas diferem só nesse dado e na mesma conta da transportadora.
- **A consulta da primeira compra no calculador (R4).** A régua manda não contar uma função genérica que não cita o caso, mesmo que só um caso a use. Por isso a consulta em `CheckoutCalculator.java:62` não virou malfeito. Registrei a ressalva no texto.
