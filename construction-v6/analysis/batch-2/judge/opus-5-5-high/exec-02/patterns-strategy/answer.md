# Padrões: Strategy

## Visão geral

O agente montou o Strategy nos quatro pontos que pedem o padrão. Há uma interface por ponto (`ShippingStrategy`, `CouponStrategy`, `PaymentMethodStrategy`, `ClubTierStrategy`) e uma classe por caso. As instâncias ficam em quatro `Map` montados em `Main.java`, e o `CheckoutService` acha o caso por consulta ao mapa, sem citar nenhum. As quatro regras cruzadas moram na casa do par: o motoboy dentro do `GoldTier`, a retirada dentro do `FreeShippingCoupon`, o "sem cupom" dentro do `PixPayment` e o histórico de pedidos dentro do `Welcome10Coupon`.

O problema mais sério está no pagamento. A regra de quantas parcelas cada forma aceita escapou das casas: está num `if` que cita `PIX`, `BANK_SLIP` e `CREDIT_CARD` no `ValidationService`, e as classes de pagamento ainda têm a sua própria versão dessa regra, que só ajusta o número em silêncio.

Há problemas menores na entrega:
- `EconomyShipping` e `ExpressShipping` são quase iguais, copiadas uma da outra.
- A interface separa o preço e o prazo em duas chamadas, e por isso o prazo é pedido à TransLog com um CEP e um peso inventados.

A região, que é o controle negativo, ficou como `switch` que só devolve números. Isso é contenção, mas a variação está espalhada por três lugares.

## Item a item

**P1-local, a entrega: onde mora o código de cada caso.** Cada caso tem a sua classe:
- `CourierShipping`: o custo fixo de R$ 18,00, o limite de 5 kg e o prazo 0, em `src/main/java/com/loja/domain/strategies/shipping/CourierShipping.java:8-24`.
- `StorePickupShipping`: grátis, 1 dia, em `StorePickupShipping.java:10-22`.
- `EconomyShipping` e `ExpressShipping`: as duas consultam a TransLog.

Fora das casas, os nomes aparecem em três lugares que não contam:
- como chave do registro (`Main.java:45-50`);
- na tradução para o código da transportadora (`infrastructure/TransLogClient.java:28`, `ECONOMY ? "STD" : "EXP"`), que é trabalho do adaptador;
- no enum `TransLogClient.TransLogService` (`TransLogClient.java:66-69`), que também é do lado da TransLog.

Nenhuma linha do serviço faz algo só para um caso. `ValidationService.validateShippingMethod` (`services/ValidationService.java:108-134`) e `CheckoutService.calculateSummary` (`services/CheckoutService.java:39-41`) só chamam a interface.

Duas ressalvas, que não tiram o código das casas:
- `EconomyShipping.java` e `ExpressShipping.java` são cópias. Mudam só a constante do serviço e o prazo de reserva (7 contra 2, na linha 36 de cada uma). O `getRegionalDelay` é idêntico nas duas (linhas 40-46).
- A interface (`ShippingStrategy.java:8-12`) separa `calculateCost`, `calculateDeliveryDays` e `isAvailable`. Por isso as classes da TransLog consultam a transportadora de novo para o prazo, com CEP `"00000000"` e peso `1.0` (`EconomyShipping.java:31-32`), e de novo para a disponibilidade (`EconomyShipping.java:50-51`). A granularidade da interface atrapalha o caso que depende de uma cotação só. Isso pesa no comportamento, não em onde mora o código.

Conclusão: acerto

**P1-selecao, a entrega: como o caso é escolhido.** A escolha é por consulta: `shippingStrategies.get(request.shippingMethod())` em `CheckoutService.java:39` e em `ValidationService.java:113`. O texto vira constante pelo `ShippingMethod.valueOf` (`api/controllers/ApiController.java:335`), sem citar nenhum caso.

Conclusão: acerto

**P2-local, o cupom: onde mora o código de cada caso.** Cada cupom tem a sua classe, com a conta do desconto e a condição de uso:
- `Welcome10Coupon.java:16-23`: 10% e a primeira compra;
- `Minus50Coupon.java:10-17`: R$ 50 e o mínimo de R$ 300;
- `FreeShippingCoupon.java:10-17`: o valor do frete e a exclusão da retirada;
- `Buy3Pay2Coupon.java:18-43`: uma unidade grátis a cada três.

O teto de 30% fica em `CheckoutService.java:52-55` e vale para todos os cupons sem citar nenhum: é código genérico, não de um caso. `ValidationService.validateCoupon` (`ValidationService.java:136-162`) só pergunta `isApplicable` à estratégia. Fora das casas, os nomes só aparecem como chave do registro (`Main.java:52-57`).

Conclusão: acerto

**P2-selecao, o cupom: como o caso é escolhido.** A escolha é por consulta: `couponStrategies.get(request.coupon())` em `CheckoutService.java:43` e em `ValidationService.java:141`, depois de `CouponCode.valueOf` (`ApiController.java:336`). O `if (couponStrategy != null)` (`CheckoutService.java:45`) trata a falta de cupom, não um cupom em particular.

Conclusão: acerto

**P3-local, a forma de pagamento: onde mora o código de cada caso.** As três classes guardam o ajuste e a recusa:
- `PixPayment.java:8-13`: o desconto de 5%;
- `CreditCardPayment.java:14-54`: a tabela Price a partir de 4x;
- `BankSlipPayment.java:6-27`: a tarifa de R$ 3,49 e o teto de R$ 1.000.

A regra das parcelas permitidas, que o enunciado dá a cada forma ("Pix e boleto só 1; cartão de 1 a 12"), ficou fora das casas, num `if` que cita os três casos em `ValidationService.java:174-183`:
- `if (request.paymentMethod() == PaymentMethod.PIX || request.paymentMethod() == PaymentMethod.BANK_SLIP)` na linha 175;
- `else if (request.paymentMethod() == PaymentMethod.CREDIT_CARD)`, com o `12` fixo, nas linhas 179-180.

Essas linhas fazem coisas só para cada caso. Uma forma de pagamento nova obrigaria a mexer ali. A mesma regra também existe dentro das casas, em outra versão: o `calculateInstallments` corta para 1 no Pix e no boleto (`PixPayment.java:16-18`, `BankSlipPayment.java:15-17`) e limita entre 1 e 12 no cartão (`CreditCardPayment.java:25-33`). A regra está escrita duas vezes, e a versão que recusa é a que está fora da estrutura.

Conclusão: malfeito

**P3-selecao, a forma de pagamento: como o caso é escolhido.** O cálculo escolhe por consulta, `paymentMethodStrategies.get(request.paymentMethod())` (`CheckoutService.java:68`), e não cita nenhuma forma. O `if` que cita as formas (`ValidationService.java:175-183`) só recusa o pedido e não faz conta. Pela régua, ele não muda a forma de escolha (já contou no item anterior).

Conclusão: acerto

**P4-local, o nível do clube: onde mora o código de cada caso.** Cada nível tem a sua classe com as três vantagens: o crédito, o frete e o brinde (`BronzeTier.java:8-20`, `SilverTier.java:8-20`, `GoldTier.java:9-24`). Busquei `credit`, `shipping` e `gift` fora delas e não achei nada que faça algo só para um nível:
- o serviço só chama `clubStrategy.adjustShippingCost`, `calculateCredit` e `hasGift` (`CheckoutService.java:61, 79, 80`);
- o `CreditObserver` (`domain/observers/CreditObserver.java:19-23`) usa o crédito que já saiu no resumo;
- `ValidationService.validateClubTier` (`ValidationService.java:90-94`) só confere se veio.

Os nomes aparecem fora das casas só como chave do registro (`Main.java:59-63`).

Conclusão: acerto

**P4-selecao, o nível do clube: como o caso é escolhido.** A escolha é por consulta, `clubTierStrategies.get(request.clubTier())` (`CheckoutService.java:60`), depois de `ClubTier.valueOf` (`ApiController.java:339`).

Conclusão: acerto

**R1, o GOLD não paga frete, exceto no motoboy.** A regra mora no nível: `if (shippingMethod == ShippingMethod.COURIER) return shippingCost; return Money.zero();` em `GoldTier.java:14-19`. O serviço passa a forma de entrega a qualquer nível sem citar nenhum (`CheckoutService.java:61`). Citar o `COURIER` dentro da casa do par não espalha a regra.

Conclusão: acerto

**R2, o FREESHIPPING não vale na retirada.** A regra mora no cupom: `return shippingMethod != ShippingMethod.STORE_PICKUP;` em `FreeShippingCoupon.java:15-17`. A validação pergunta `isApplicable` de forma genérica (`ValidationService.java:154-160`).

Conclusão: acerto

**R3, o desconto do Pix só sem cupom.** A regra mora no Pix, que recebe o contexto: `if (hasCoupon) return Money.zero();` em `PixPayment.java:9-11`. O serviço passa `request.coupon() != null` a qualquer forma de pagamento, sem citar o Pix (`CheckoutService.java:70-74`). O preço é o parâmetro `hasCoupon` aparecer na assinatura das três formas (`PaymentMethodStrategy.java:6`), mas isso é passar contexto, não um `if` fora do lugar.

Conclusão: acerto

**R4, o WELCOME10 só na primeira compra.** A regra mora no cupom, que consulta os pedidos pelo repositório injetado: `orderRepository.isFirstOrder(customerCpf)` em `Welcome10Coupon.java:21-23`, com o repositório passado em `Main.java:53`. Nem o controller nem o serviço consultam o histórico.

Conclusão: acerto

**P5-forma, a região.** O enum `Region` é só rótulo (`domain/entities/Region.java:3-9`). A variação está em `switch` que devolvem números:
- a porcentagem do seguro em `CheckoutService.java:117-122`;
- o limite (R$ 30,00 ou `null`) em `CheckoutService.java:126-129`;
- os dias a mais do Norte e do Nordeste num `getRegionalDelay` copiado em `EconomyShipping.java:40-46` e em `ExpressShipping.java:40-46`.

Não há classe nem objeto com comportamento por região. A forma é `switch` que devolve número, em três lugares (dois deles iguais).

Conclusão: contenção

**P5-proporcao, a região.** A resposta não cria estrutura por região: os `switch` só devolvem a porcentagem, o limite e os dias, e uma conta só serve a todas as regiões (`CheckoutService.java:124-135`). Pela régua é aceitável e não é exagero. Não é a forma de dado, porém: é condicional, espalhada por três `switch` em dois arquivos. Uma região nova obriga a mexer em quatro lugares, e o `default -> 0` dos atrasos (`EconomyShipping.java:44`) a deixaria passar sem aviso.

Conclusão: contenção

**E, estrutura sem necessidade perto do Strategy.** Não achei estrutura sem necessidade perto deste pattern. Duas observações:
- As quatro interfaces de estratégia têm um método de identificação que ninguém chama: `getTier()`, `getCode()` e `getMethod()` (`ClubTierStrategy.java:12`, `CouponStrategy.java:21`, `PaymentMethodStrategy.java:14`, `ShippingStrategy.java:14`). Busquei e não há nenhuma chamada fora das próprias classes. É código morto, não camada nem fábrica.
- Os repositórios têm interface com uma implementação só (`repositories/ProductRepository.java` e `repositories/impl/ProductRepositoryImpl.java`, e o mesmo para os pedidos, os clientes e os avisos), e nem os testes os substituem: só existe `src/test/java/com/loja/MoneyTest.java`. Ficam longe do Strategy; deixo para a correção do pattern a que estão mais perto.

Conclusão: contenção

## O que mudaria primeiro

1. **Levar a regra das parcelas para dentro de cada forma de pagamento**, por exemplo com um `boolean acceptsInstallments(int n)` na `PaymentMethodStrategy`, e apagar o `if` que cita `PIX`, `BANK_SLIP` e `CREDIT_CARD` em `ValidationService.java:175-183`. Isso também tiraria a duplicação com o `calculateInstallments`, que hoje corta o número em silêncio. É o único lugar em que uma forma nova obrigaria a mexer fora da casa dela.
2. **Pôr os números da região num enum que os carregue**, por exemplo `Region(percentual, limite, diasExtras)` com um método só. Isso apagaria os dois `switch` do `CheckoutService` e o `getRegionalDelay` copiado. Ao mesmo tempo, dá para juntar `EconomyShipping` e `ExpressShipping` numa classe da TransLog que recebe o código do serviço, com uma cotação só para o preço e o prazo.

## Dúvidas

- **O `if` das parcelas.** Ele é validação e não faz conta. Contei como código específico fora da casa no P3-local (malfeito), porque a régua inclui "um `if` que o nomeia numa validação ou na escolha do código de erro". Não o contei contra a escolha no P3-selecao, porque a régua diz que um `if` que só recusa não muda a forma de escolha.
- **O E.** Deixei as interfaces de repositório com uma implementação só fora deste item, porque a régua manda registrar só o que fica perto do pattern corrigido. Se a leitura for de registrar toda estrutura sem necessidade no projeto, elas contariam como exagero.
