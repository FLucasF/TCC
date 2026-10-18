# Padrões: Strategy

## Visão geral

O agente não usou o Strategy em nenhum dos quatro pontos que pedem o padrão. Os seis enums de `model/` (`ShippingMethod`, `Coupon`, `PaymentMethod`, `ClubTier`, `Region`, `OrderStatus`) são só rótulos, sem corpo e sem dados. O Jackson converte o texto da requisição direto nesses enums (`CheckoutRequest.java:11-16`), e todo o comportamento de cada caso fica numa única classe de 453 linhas, `CheckoutCalculator`, escrito em `switch` que nomeiam os casos e fazem a conta ali mesmo. Seguem os problemas, do mais grave ao menos grave:

- **Entrega (P1) e clube (P4).** São os dois pontos em que o enunciado avisa que vão crescer, e os dois estão espalhados. As regras do GOLD aparecem em três métodos diferentes, e há ainda uma cópia que nada chama.
- **Regras cruzadas (R1 a R4).** Como não existe casa para os casos, todas ficaram em `if` dentro do cálculo geral.
- **Pagamento (P3).** Os casos são nomeados em dois `switch` separados, um para as parcelas e outro para a conta.
- **Região (P5, o controle negativo).** Aqui a resposta tem o tamanho certo: dois `switch` que só devolvem números, sem nenhuma estrutura por região.
- **Estrutura desnecessária (E).** Não há.

## Item a item

**P1-local, a entrega: onde mora o código de cada caso.** `ShippingMethod` não tem corpo (`src/main/java/com/loja/orderservice/model/ShippingMethod.java:3-8`), então nenhum caso tem casa. Todo o código específico de cada entrega está fora:
- **STORE_PICKUP:** grátis e 1 dia (`src/main/java/com/loja/orderservice/service/CheckoutCalculator.java:215-216`).
- **COURIER:** o limite de 5 kg, os R$ 18,00 e os 0 dias (`CheckoutCalculator.java:218-222`).
- **ECONOMY e EXPRESS:**
  - a tradução para `STD`/`EXP` está no calculador, e não do lado da TransLog (`CheckoutCalculator.java:225`);
  - a chamada à TransLog e o tratamento das respostas estão em `CheckoutCalculator.java:227-236`;
  - os dias a mais por região estão em `CheckoutCalculator.java:238`.

Uma entrega nova obriga a mexer nesse `switch`. Os `if` das regras cruzadas (R1, R2) também nomeiam entregas no cálculo, e o caso da linha 416 está no P4.
Conclusão: omissão

**P1-seleção, a entrega: como o caso é escolhido.** O `shippingMethod` vira um enum-rótulo, e `switch (request.getShippingMethod())` decide e calcula no mesmo lugar (`CheckoutCalculator.java:214-242`). É uma condicional no cálculo. O `default` da linha 241 nunca é alcançado.
Conclusão: omissão

**P2-local, o cupom: onde mora o código de cada caso.** `Coupon` é um enum sem corpo (`src/main/java/com/loja/orderservice/model/Coupon.java:3-8`). As quatro regras ficam nos ramos de um `switch` do calculador:
- WELCOME10: a checagem da primeira compra e os 10% (`CheckoutCalculator.java:262-267`), mais `isFirstPurchase` (`:306-310`);
- MINUS50: o mínimo de R$ 300 e os R$ 50 (`:268-273`);
- FREESHIPPING: o desconto igual ao frete, mais a recusa na retirada (`:274-279`);
- BUY3PAY2: a conta por unidade, com uma consulta ao repositório dentro dela (`:280-291`).

O teto de 30% (`:296-301`) é genérico, não nomeia nenhum cupom, e não conta. A "validação" das linhas 120-126 (`Coupon.valueOf(request.getCoupon().name())`) só confere se o código existe, e também não conta.
Conclusão: omissão

**P2-seleção, o cupom: como o caso é escolhido.** O enum-rótulo vem do Jackson, e `switch (request.getCoupon())` faz a conta de cada cupom (`CheckoutCalculator.java:261-292`). É uma condicional no cálculo.
Conclusão: omissão

**P3-local, a forma de pagamento: onde mora o código de cada caso.** `PaymentMethod` não tem corpo (`src/main/java/com/loja/orderservice/model/PaymentMethod.java:3-7`). O código de cada forma está repartido em dois `switch`:
- **As parcelas permitidas:** um para Pix e boleto, de 1 a 12 no cartão (`CheckoutCalculator.java:136-149`).
- **O ajuste de cada forma:**
  - Pix: o desconto de 5% (`:356-367`);
  - boleto: a recusa acima de R$ 1.000 e a tarifa de R$ 3,49 (`:368-376`);
  - cartão: sem juros até 3x, e a tabela Price a partir de 4x (`:377-394`).

Cada forma de pagamento tem código em dois lugares do calculador.
Conclusão: omissão

**P3-seleção, a forma de pagamento: como o caso é escolhido.** São duas condicionais que nomeiam os casos: `switch (paymentMethod)` na validação (`CheckoutCalculator.java:137`) e `switch (request.getPaymentMethod())` na conta (`:355`). A segunda é condicional no cálculo.
Conclusão: omissão

**P4-local, o nível do clube: onde mora o código de cada caso.** `ClubTier` é um enum sem corpo e sem dados (`src/main/java/com/loja/orderservice/model/ClubTier.java:3-7`). As vantagens de cada nível estão em três métodos do calculador, mais uma cópia que ninguém usa:
- **O crédito:** um `switch` com a porcentagem de cada nível (`CheckoutCalculator.java:402-406`). São só números, mas ficam numa condicional do calculador, e não no nível.
- **O frete grátis do GOLD:** `CheckoutCalculator.java:47-49`.
- **O brinde do GOLD:** `CheckoutCalculator.java:411-421`. Além do mínimo de R$ 500, ele também nomeia o `COURIER` (linha 416), uma restrição que o enunciado não liga ao brinde. Saber se isso está certo é com a dimensão do comportamento; aqui o que importa é que é mais código do GOLD fora de qualquer casa.
- **A cópia sem uso:** `applyGoldBenefits` (`CheckoutCalculator.java:423-428`) repete o `if` do frete. Nenhuma classe a chama: o `grep` em `src` só acha a declaração.

Um nível novo obriga a mexer em pelo menos três métodos.
Conclusão: omissão

**P4-seleção, o nível do clube: como o caso é escolhido.** Não há consulta. São três condicionais que nomeiam os níveis no cálculo: o `switch (tier)` (`CheckoutCalculator.java:402`), o `request.getClubTier() == ClubTier.GOLD` (`:47`) e o `tier != ClubTier.GOLD` (`:412`).
Conclusão: omissão

**R1, o GOLD não paga frete, exceto no motoboy.** A regra está no cálculo geral, exatamente como o remendo da régua: `if (request.getClubTier() == ClubTier.GOLD && request.getShippingMethod() != ShippingMethod.COURIER)` (`CheckoutCalculator.java:47`). Ela se repete em `applyGoldBenefits` (`:424`), que ninguém chama, e o mesmo par GOLD/COURIER volta no brinde (`:412-417`). Não há casa do nível nem da entrega onde a regra pudesse morar.
Conclusão: omissão

**R2, o FREESHIPPING não vale na retirada.** A regra está no ramo FREESHIPPING do `switch` de cupons, dentro do calculador: `if (request.getShippingMethod() == ShippingMethod.STORE_PICKUP)` (`CheckoutCalculator.java:275-277`). Fica junto do código desse cupom, mas como o cupom não tem casa, a regra está no cálculo geral.
Conclusão: omissão

**R3, o desconto do Pix só sem cupom.** A regra está no ramo PIX do `switch` de pagamento: `if (request.getCoupon() == null)` (`CheckoutCalculator.java:357`). O Pix não é um objeto que receba o contexto, então a condição fica no cálculo. Pelo menos ela olha a presença do cupom, não o valor do desconto, como o enunciado pede.
Conclusão: omissão

**R4, o WELCOME10 só na primeira compra.** A checagem não está no controller. Ela está no ramo WELCOME10 do `switch` (`CheckoutCalculator.java:263-265`), que chama o método privado `isFirstPurchase`, e este consulta `orderRepository.countByCustomerCpfAndStatusIn` (`:306-310`). Fica no serviço de cálculo, e não num cupom que consulte os pedidos.
Conclusão: omissão

**P5-forma, a região: que forma a variação tem.** `Region` é um enum-rótulo (`src/main/java/com/loja/orderservice/model/Region.java:3-9`). Há dois `switch` no calculador que só devolvem números:
- `calculateInsurance` atribui a taxa e o limite de cada região, e depois faz uma conta única para todas (`CheckoutCalculator.java:316-345`). O `default` com taxa zero (`:333-336`) nunca é alcançado.
- `getExtraDays` devolve 3, 2 ou 0 dias (`:245-251`).

Conclusão: não se aplica

**P5-proporção, a região: a resposta tem o tamanho do problema.** Não há classe por região, nem constante com corpo, nem objeto com comportamento. As regiões diferem só em números, e a conta é uma só. Pela régua, um `switch` que só devolve o número é aceitável: é uma condicional, não um dado (um enum com taxa, limite e dias seria mais limpo), mas não é exagero.
Conclusão: contenção

**E, estrutura sem necessidade fora dos pontos.** Não encontrei nenhuma. As únicas interfaces do projeto são os repositórios do Spring Data (`repository/*.java`). `TransLogService` é uma classe concreta, sem interface. `QuoteResult` (`TransLogService.java:66-106`), `ShippingInfo` e `PaymentInfo` (`CheckoutCalculator.java:430-452`) são pequenos objetos que só carregam valores, não camadas ou fábricas desnecessárias. Os serviços de produto e de cliente fazem a busca e o mapeamento, não só repassam.
Conclusão: contenção

## O que mudaria primeiro

1. **A entrega como estratégia.** Uma interface (por exemplo, `ShippingOption.quote(contexto)`, devolvendo o custo e o prazo ou a indisponibilidade), com um bean por opção, achado num `Map` pelo código. O limite de 5 kg ficaria no motoboy, e a tradução `STD`/`EXP` com a chamada à TransLog ficaria nas duas opções da transportadora. É o ponto em que o enunciado diz que "quase toda semana entra uma opção nova", e hoje cada opção nova mexe no `switch` de `CheckoutCalculator.java:214`.
2. **O nível do clube carregando as próprias vantagens.** Um enum com a taxa de crédito, um `freeShipping(entrega)` e um `gift(subtotal)`, ou uma interface por nível. Isso acabaria com os três lugares que nomeiam o GOLD (`:47`, `:402-406`, `:411-421`) e com a cópia morta (`:423-428`), e a R1 passaria a morar no próprio GOLD. Os cupons e as formas de pagamento seguiriam o mesmo caminho, levando a R2, a R3 e a R4 para as casas certas.

## Dúvidas

- **Que conclusão dar às regras cruzadas.** "Malfeito" pressupõe uma estrutura da qual a regra escapou, e aqui não existe estrutura nenhuma. Dei `omissão`: as quatro regras estão no cálculo geral, como a variação toda.
- **A conclusão de `P5-forma`.** A régua pede só uma descrição, e o formato da resposta não tem uma palavra para isso. Usei `não se aplica` na linha e deixei a avaliação para `P5-proporção`.
- **A restrição do motoboy no brinde** (`CheckoutCalculator.java:416`). Não está no enunciado. Contei só como código do GOLD fora de casa; se é um erro, decide a dimensão do comportamento.
- **O método `applyGoldBenefits`, que ninguém chama** (`:423-428`). Usei como evidência de que a regra se espalhou, mesmo que não afete o resultado.
