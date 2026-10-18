Já li o enunciado, as quatro famílias de estratégia e o `CheckoutService`, e busquei os nomes dos casos no projeto todo. Agora escrevo a correção.

# Padrões: Strategy

## Visão geral

O agente montou o Strategy nos quatro pontos que pedem o padrão: entrega, cupom, pagamento e clube. Cada um tem uma interface com uma classe `@Component` por caso, e o `CheckoutService` acha o caso num `Map` montado a partir dos beans pelo `getCode()`. A escolha do caso é por consulta nos quatro. O pagamento e o clube estão limpos. Três das quatro regras cruzadas moram onde devem: o GOLD que paga o motoboy fica no `GoldTier`, o FREESHIPPING que não vale na retirada fica no `FreeShippingCoupon` e o Pix sem cupom fica no `PixPayment`.

Dois casos escaparam da estrutura, ambos no serviço, em ordem de gravidade:
1. **A condição do MINUS50** (produtos a partir de R$ 300,00) está num `if` que nomeia o cupom dentro do `CheckoutService`. O `Minus50Coupon.isApplicable` responde `true` sempre.
2. **A escolha do código de erro da entrega** é feita por um `if` que nomeia `ECONOMY` e `EXPRESS`. A estratégia só devolve um `boolean`, que não diz por que a entrega não está disponível, e o serviço completa a resposta pelo nome do caso.

A região, que é o controle negativo, está como um `switch` que só devolve números, sem estrutura por região. Os dias a mais do Norte e do Nordeste, porém, estão copiados nas duas entregas da TransLog.

## Item a item

**P1-local, a entrega: onde mora o código de cada caso.** Cada entrega tem a sua classe:
- `CourierShipping`: R$ 18,00, 0 dias e o limite de 5 kg (`src/main/java/com/loja/strategy/shipping/CourierShipping.java:16,21`);
- `StorePickupShipping`: grátis e 1 dia (`StorePickupShipping.java:16`);
- `EconomyShipping` e `ExpressShipping`: a cotação da TransLog com `"STD"` e `"EXP"` e os dias a mais por região (`EconomyShipping.java:23-35`, `ExpressShipping.java:23-35`).

Um caso escapa da casa: `src/main/java/com/loja/service/CheckoutService.java:200`, `if (request.getShippingMethod().equals("ECONOMY") || request.getShippingMethod().equals("EXPRESS"))`. Ele decide se a recusa é `CARRIER_UNAVAILABLE` ou `SHIPPING_METHOD_UNAVAILABLE` pelo nome da entrega. É código específico desses dois casos fora da casa deles, exatamente o "if que o nomeia na escolha do código de erro".

A causa está no desenho: `ShippingStrategy.isAvailable` devolve só um `boolean` (`ShippingStrategy.java:9`), e o `TransLogClient` devolve `available=false` tanto para o `ZIP_NOT_SERVED` (`TransLogClient.java:36`) quanto para a TransLog fora do ar (`TransLogClient.java:53`). Sem saber o motivo, o serviço adivinha pelo nome. Pela leitura do código, um CEP não atendido na econômica acaba em `CARRIER_UNAVAILABLE`. Se isso dá a resposta errada é a dimensão do comportamento que mede, não esta.

Dois pontos menores:
- `EconomyShipping` e `ExpressShipping` são cópias quase linha a linha, inclusive o `switch` dos dias a mais. É duplicação entre duas casas, não código fora delas.
- A tradução `"STD"`/`"EXP"` fica dentro da casa de cada entrega, o que é aceitável.

Conclusão: malfeito

**P1-selecao, a entrega: como o caso é escolhido.** Por consulta. O mapa é montado em `CheckoutService.java:46` (`Collectors.toMap(ShippingStrategy::getCode, ...)`), conferido em `:193` e consultado em `:197`. Depois a conta é sempre `shipping.calculate(...)` (`:207`), sem nenhum `if` que nomeie um caso. O `if` da linha 200 só escolhe a recusa e não calcula; ele foi cobrado no P1-local.
Conclusão: acerto

**P2-local, o cupom: onde mora o código de cada caso.** Cada cupom tem a sua classe com `calculateDiscount` e `isApplicable`:
- `Welcome10Coupon`: 10% e a checagem da primeira compra (`Welcome10Coupon.java:20,25-26`);
- `FreeShippingCoupon`: o valor do frete e a recusa na retirada (`FreeShippingCoupon.java:19,24`);
- `Buy3Pay2Coupon`: uma unidade grátis a cada três (`Buy3Pay2Coupon.java:23-31`);
- `Minus50Coupon`: o desconto de R$ 50,00 (`Minus50Coupon.java:20`).

A condição do MINUS50, porém, não está na casa dele. `Minus50Coupon.isApplicable` devolve `true` (`Minus50Coupon.java:24-26`), e a regra "a partir de R$ 300,00" está no serviço: `CheckoutService.java:238-242`, `if (request.getCoupon().equals("MINUS50")) { if (productsSubtotal.compareTo(new BigDecimal("300.00")) < 0) throw ... COUPON_NOT_APPLICABLE }`. É uma regra de um caso no serviço, nomeando o caso.

O teto de 30% (`CheckoutService.java:244-247`) vale para todos os cupons e não nomeia nenhum, então está bem no serviço.
Conclusão: malfeito

**P2-selecao, o cupom: como o caso é escolhido.** Por consulta. O mapa é montado em `CheckoutService.java:45` e consultado em `:219` e `:223`. A conta é `coupon.calculateDiscount(...)` (`:236`) para qualquer cupom. O `if` do MINUS50 em `:238` só recusa e não calcula; ele foi cobrado no P2-local.
Conclusão: acerto

**P3-local, a forma de pagamento: onde mora o código de cada caso.** Tudo o que é de cada forma está na casa dela:
- `PixPayment`: o desconto de 5% condicionado a não ter cupom e só 1 parcela (`PixPayment.java:20-27,39`);
- `CreditCardPayment`: até 3x sem juros, a tabela Price de 4x a 12x e o limite de 1 a 12 parcelas (`CreditCardPayment.java:23-35,49`);
- `BankSlipPayment`: a tarifa de R$ 3,49, a recusa acima de R$ 1.000,00 e só 1 parcela (`BankSlipPayment.java:16-17,24,29`).

O serviço só chama `isValidInstallments`, `isAvailable` e `calculate` (`CheckoutService.java:288,292,298`), sem nomear nenhuma forma. Uma busca por `"PIX"`, `"CREDIT_CARD"` e `"BANK_SLIP"` fora de `strategy/payment` não acha nada. O padrão "parcelas = 1 quando não vem" aparece duas vezes (`CheckoutService.java:286` e `CreditCardPayment.java:18`), mas é genérico e não é de um caso.
Conclusão: acerto

**P3-selecao, a forma de pagamento: como o caso é escolhido.** Por consulta. O mapa é montado em `CheckoutService.java:47` e consultado em `:280` e `:284`. A conta é `payment.calculate(request, orderTotal, hasCoupon)` (`:298`).
Conclusão: acerto

**P4-local, o nível do clube: onde mora o código de cada caso.** Cada nível devolve os seus dados num `ClubBenefits` (a porcentagem do crédito, o frete grátis e o limite do brinde):
- `BronzeTier.java:16`: `0`, `false`, `null`;
- `SilverTier.java:16`: `0.02`, `false`, `null`;
- `GoldTier.java:16-19`: `0.05`, frete grátis fora do motoboy, `500.00`.

O serviço usa esses dados de forma genérica: `clubBenefits.isFreeShipping()` (`CheckoutService.java:65`), `getCreditPercentage()` (`:79-81`) e `getGiftThreshold()` (`:83-84`). Uma busca por `credit`, `gift` e `freeShipping` fora do pacote `strategy/club` não acha nenhuma linha que pergunte qual é o nível. Os níveis diferem só nesses dados, e um nível novo responde sozinho.
Conclusão: acerto

**P4-selecao, o nível do clube: como o caso é escolhido.** Por consulta. O mapa é montado em `CheckoutService.java:48`, conferido em `:177` e consultado em `getClubTier` (`:301-305`). A conta é `clubTier.getBenefits(...)` (`:63`).
Conclusão: acerto

**R1, o GOLD não paga frete, exceto no motoboy.** A regra fica na casa do GOLD: `GoldTier.java:16`, `boolean freeShipping = !request.getShippingMethod().equals("COURIER")`. O serviço só lê `isFreeShipping()` (`CheckoutService.java:65`). Citar o `COURIER` dentro do nível é onde a regra deve morar.
Conclusão: acerto

**R2, o FREESHIPPING não vale na retirada.** A regra fica na casa do cupom: `FreeShippingCoupon.java:24`, `return !request.getShippingMethod().equals("STORE_PICKUP")`. O serviço só chama `coupon.isApplicable(...)` (`CheckoutService.java:232`).
Conclusão: acerto

**R3, o desconto do Pix só sem cupom.** A regra fica no Pix, que recebe o contexto: o serviço calcula `hasCoupon = request.getCoupon() != null` sem nomear forma nenhuma (`CheckoutService.java:296`) e passa para qualquer estratégia (`:298`). Quem decide é `PixPayment.java:20`, `if (!hasCoupon)`. O parâmetro `hasCoupon` está na interface só por causa do Pix (`PaymentStrategy.java:8`), mas é contexto genérico e não um `if` sobre o caso.
Conclusão: acerto

**R4, o WELCOME10 só na primeira compra.** A decisão fica no cupom: `Welcome10Coupon.java:24-26` recusa quando há pedido `PAID`, `SHIPPED` ou `DELIVERED`. A consulta ao histórico, porém, é feita no serviço: `CheckoutService.java:225-230` busca os pedidos do CPF nessas três situações e passa a lista para `isApplicable` de qualquer cupom. Essa linha não nomeia o WELCOME10, e pela régua uma função genérica que não cita o caso não conta como código dele fora da casa. Mesmo assim, a definição de "pedido que conta" (as três situações) aparece duas vezes, em `:227` e em `Welcome10Coupon.java:26`, e o histórico é buscado para qualquer cupom, MINUS50 incluído. A regra mora no cupom, mas os dados vêm já filtrados pelo serviço.
Conclusão: acerto

**P5-forma, a região: que forma a variação tem.** A região é um `String`, sem enum. A variação aparece em três lugares:
- um `switch` no serviço que devolve a porcentagem e o limite do seguro, com a conta única logo depois (`CheckoutService.java:256-276`);
- um `switch` que devolve os dias a mais (`NORTH -> 3`, `NORTHEAST -> 2`), copiado em `EconomyShipping.java:29-33` e em `ExpressShipping.java:29-33`;
- a lista de regiões válidas (`CheckoutService.java:185`), que só confere se o código existe e não conta.

Não há classe, objeto nem corpo por região.
Conclusão: contenção

**P5-proporcao, a região: a resposta tem o tamanho do problema.** Os dois `switch` só devolvem números, e a conta do seguro é uma só para todas as regiões (`CheckoutService.java:270-274`). Isso é aceitável: não há estrutura por região, mas é uma condicional e não um dado. Os números da região também estão espalhados: o seguro no serviço e os dias a mais duplicados nas duas entregas. Uma região nova mexe em quatro lugares (`:185`, `:256`, `EconomyShipping.java:29` e `ExpressShipping.java:29`), quando uma tabela única (um enum só com os números ou um mapa) resolveria em um.
Conclusão: contenção

**E, estrutura sem necessidade fora dos pontos.** Perto do Strategy não há estrutura que ninguém pediu:
- `ShippingResult`, `PaymentResult` e `ClubBenefits` são objetos de valor usados de fato pelo cálculo;
- `MoneyRounding` é um utilitário estático de uma linha;
- os controllers chamam os serviços direto, sem camada que só repassa;
- não há fábrica nem builder em volta das estratégias, porque a escolha é o próprio `Map`.

Conclusão: contenção

## O que mudaria primeiro

1. **Levar a condição do MINUS50 para a casa dele.** `Minus50Coupon.isApplicable` deve conferir `productsSubtotal >= 300.00` (a interface pode receber o subtotal), e o `if` de `CheckoutService.java:238-242` sai. Hoje um cupom novo com regra de valor mínimo tende a ganhar outro `if` ali, e o próximo leitor de `Minus50Coupon` acha que ele vale sempre.
2. **Fazer a entrega dizer por que não atende.** No lugar do `boolean isAvailable`, a estratégia pode lançar ou devolver o motivo (não atende, ou transportadora fora do ar), e o `TransLogClient` pode separar `ZIP_NOT_SERVED` de falha. Assim o `if` que nomeia `ECONOMY`/`EXPRESS` em `CheckoutService.java:200` sai, e uma nova entrega de transportadora não precisa ser acrescentada ali. De quebra, some a segunda chamada à TransLog: hoje ela é chamada em `isAvailable` e de novo em `calculate` (`EconomyShipping.java:23,40`).

## Dúvidas

- **R4:** a consulta do histórico está no serviço (`CheckoutService.java:225-230`), mas sem nomear o cupom, e a decisão está no `Welcome10Coupon`. Li isso como a regra morando no cupom (acerto), já que a régua não conta uma função genérica que não cita o caso. A outra leitura possível é que buscar e filtrar os pedidos no serviço já é "a checagem do histórico no serviço", o remendo. Fiquei com a primeira porque quem recusa é o cupom, mas o filtro das três situações duplicado em `:227` deixa a fronteira borrada.
- **P1-local:** os dias a mais por região estão dentro das casas da econômica e da expressa. Para a entrega, tratei isso como código no lugar certo (a regra é só dessas duas opções), e a duplicação entre elas não pesou na conclusão. Para a região, contei como número espalhado (P5-proporcao).
