Já li o enunciado, o único arquivo de teste e o código que ele exercita. Segue a correção.

# Testes

## Visão geral

O agente escreveu **um único teste**: `CheckoutServiceTest.testBasicCheckout`. Ele calcula o resumo de uma compra simples (2 camisetas, retirada na loja, Pix, BRONZE, Sudeste, sem cupom) e confere três campos. O teste resolve o banco com subclasses falsas dos repositórios, por isso roda sem PostgreSQL. As asserções conferem valores de negócio, não só a ausência de exceção.

O problema mais sério é a cobertura. Das seis áreas do enunciado, só o cálculo aparece, e mesmo assim só em parte: nenhuma recusa, nenhuma transição do pedido, nada do pagamento repetido, da concorrência ou da TransLog. Os números do PIT (`pit.txt`, `exec-05`, situação `ok`) confirmam isso:

- 1 teste;
- cobertura de linhas de 0,16;
- *mutation score* de 0,121 (39 erros pegos de 322 plantados);
- 248 erros em linhas que nenhum teste executa;
- *test strength* de 0,527: dos 74 erros em linhas executadas, 35 passam.

O segundo problema é que, mesmo dentro do cenário testado, ficam sem conferir 8 dos 11 campos do resumo, entre eles o total final.

## Item a item

**T1, o que dos requisitos os testes cobrem.**
- **O cálculo**, em parte. `testBasicCheckout` confere `productsSubtotal` = 159.80, `couponDiscount` = 0.0 e `shippingCost` = 0.0 (`src/test/java/com/loja/service/CheckoutServiceTest.java:38-40`). Não confere seguro, ajuste do pagamento, total final, parcela, crédito, brinde nem prazo. Nenhum dos 10 exemplos do financeiro é reproduzido. Também não há teste de cupom, de clube, de cartão parcelado, de boleto, de região com limite de seguro nem do arredondamento meio-para-o-par (`Money.round`, `src/main/java/com/loja/util/Money.java:7-11`).
- **As recusas**: nenhum teste. As validações de `validateRequest` (`src/main/java/com/loja/service/CheckoutService.java:71-138`) só são executadas no caminho feliz.
- **O ciclo do pedido**: nenhum teste. `OrderService.processPayment` e `shipOrder` (`src/main/java/com/loja/service/OrderService.java:51-140`) não são chamados. O falso `MockOrderRepository.findById` sempre devolve `null` (`CheckoutServiceTest.java:71-74`), então nem dá para montar um pedido.
- **O pagamento repetido**: nenhum teste. O mapa `paymentTracking` (`OrderService.java:65-71`) não é exercitado.
- **A concorrência**: nenhum teste.
- **A transportadora**: nenhum teste. O único cenário usa `STORE_PICKUP` (`CheckoutServiceTest.java:32`), justamente a entrega que não passa pela TransLog.

Conclusão: não segue

**T2, as asserções conferem valores.** As três asserções que existem (3 de 3) conferem valores de negócio exatos com `isEqualTo` (`CheckoutServiceTest.java:38-40`), não `isNotNull` nem status HTTP. Esse é o lado bom.

O lado fraco tem duas partes:
- Dois dos três valores são zeros que o cenário já garante por construção: sem cupom, o desconto é 0; na retirada, o frete é fixo em 0.0 (`CheckoutService.java:228-229`).
- Os campos com conta de verdade ficam de fora. Nesse cenário, o seguro (1% de 159,80), o ajuste do Pix (−5%) e o total final são calculados, mas não conferidos.

A *test strength* de 0,527 mostra o efeito: quase metade dos erros plantados em linhas executadas passa pelo teste. É o padrão de código executado sem ser conferido, por exemplo em `calculateInsurance`, `calculatePaymentAdjustment` e `calculateInstallmentAmount` (`CheckoutService.java:268-314`).

Conclusão: em parte

**T3, rodam sem banco e sem internet.**
- **O banco.** O teste estende cada repositório com `super(null)` e sobrescreve só os métodos que o caminho usa (`CheckoutServiceTest.java:44-103`), então nunca abre conexão. Não há PostgreSQL, Docker nem Testcontainers no `pom.xml`: as dependências de teste são só `junit-jupiter` e `assertj-core`.
- **A TransLog.** O teste não chega a ela, porque usa retirada na loja. Mas ela não teria como ser substituída por um dublê: `CheckoutService` cria `new TransLogAdapter(System.getenv("TRANSLOG_URL"))` dentro do próprio método (`CheckoutService.java:158-164` e `239-240`), em vez de receber o `ShippingCarrier` de fora. Um teste com `ECONOMY`/`EXPRESS` daria `CARRIER_UNAVAILABLE` (variável ausente) ou bateria no site de verdade.

O que existe roda isolado, como o enunciado pede. A lacuna é de cobertura (T1), não de isolamento.

Conclusão: segue

**T4, dependem de coisas que mudam sozinhas.** O único teste não usa relógio, `sleep` nem ordem de execução, e monta todo o seu estado no `@BeforeEach`, com repositórios e serviços novos a cada execução (`CheckoutServiceTest.java:16-23`). Isso segue a prática.

A ressalva é que ele não toca em nada que dependa de tempo. O código de produção registra o pedido com `Instant.now()` direto (`OrderService.java:36`), sem um relógio injetável, então um teste da regra das 48 horas (`OrderService.java:73-76`) teria de usar o relógio de verdade. Como esse teste não existe, isso não pesa na conclusão; fica registrado em Dúvidas.

Conclusão: segue

## O que mudaria primeiro

1. **Escrever testes para os 10 exemplos do financeiro**, de preferência um teste parametrizado, conferindo os 11 campos do resumo e as recusas dos exemplos 8 a 10. Isso cobriria cupons, clube, seguro com limite, Pix, cartão com juros, boleto e arredondamento, e atacaria a maior parte dos 248 erros sem cobertura. Exige primeiro que `CheckoutService` receba o `ShippingCarrier` pelo construtor, para que um dublê responda como a TransLog nos exemplos.
2. **Testar o ciclo do pedido com um repositório em memória que guarde estado** e um relógio controlado: pagar, enviar, entregar, cancelar, as transições proibidas (409), as 48 horas exatas e a 48h+1s, e o aviso de pagamento repetido sem baixar o estoque nem dar o crédito duas vezes. Hoje nada disso é exercitado.

## Dúvidas

- **T3**: avaliei pelo que os testes existentes fazem (rodam sem banco e sem internet) e deixei a impossibilidade de usar um dublê da TransLog como observação, não como reprovação, porque nenhum teste existente a viola.
- **T4**: concluí "segue" porque o único teste não depende de nada que mude sozinho. A conformidade é fácil porque o teste não toca no tempo, e um teste das 48 horas não teria como controlá-lo no código atual.
