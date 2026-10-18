# arquitetura

## Visão geral

O projeto está organizado por camada (`controller`, `service`, `repository`, `entity`, `dto`, `model`, `exception`, `config`), e não há nenhum ciclo entre pacotes. Os controllers só repassam a chamada e não decidem nada do negócio. O banco é acessado só por repositórios Spring Data. O resumo e o registro do pedido chamam o mesmo `CheckoutCalculator.calculate`. O problema sério está no item A1: a regra de negócio conhece a web. As exceções de negócio levam o código HTTP (`BusinessException` guarda um `org.springframework.http.HttpStatus`), e cada serviço já escolhe o 422, o 404, o 409 ou o 503 na hora de recusar. Além disso, os serviços recebem e devolvem os objetos do corpo da API (`CheckoutRequest`, `PaymentRequest`, `ShipmentRequest`, `OrderResponse`, `SummaryResponse`…). O relatório não acusa nada disso, porque procura só as anotações do Spring MVC e o `ResponseEntity`, e o `HttpStatus` não entra nessa conta. Há também um defeito menor: uma cópia morta da regra do frete do GOLD em `CheckoutCalculator.applyGoldBenefits`.

## Item a item

**A1, a regra de negócio não conhece a web?** Não, ela conhece a web, em duas frentes.

- **Os códigos HTTP.** A recusa do negócio carrega a situação HTTP: `exception/BusinessException.java:3` importa `org.springframework.http.HttpStatus`, e o campo `httpStatus` (`:8`) é preenchido pelo construtor (`:10`). Todas as classes de negócio montam essa recusa escolhendo o código HTTP:
  - `service/CheckoutCalculator.java:10` importa `HttpStatus`, e o arquivo tem 24 usos, por exemplo `:87` (`UNPROCESSABLE_ENTITY`) e `:231` (`SERVICE_UNAVAILABLE` para a TransLog fora do ar);
  - `service/OrderService.java:8` importa `HttpStatus`, com 16 usos, por exemplo `:96` (`CONFLICT`) e `:105` (`ORDER_EXPIRED` com `CONFLICT`);
  - `service/CustomerService.java:23`, `service/ProductService.java:23` e `service/NotificationService.java:37,48` usam `NOT_FOUND`.

  Quem decide que "pedido expirado" vira 409 é o serviço, e não a camada web. O `GlobalExceptionHandler` (`exception/GlobalExceptionHandler.java:13-15`) só copia o status que veio na exceção.
- **Os objetos da API.** O relatório registra `service -> dto` com 9 dependências de classe:
  - `service/CheckoutCalculator.java:34` recebe `CheckoutRequest`, que é o corpo da requisição (`dto/CheckoutRequest.java`), e toda a regra lê dele (`request.getCoupon()`, `request.getClubTier()`…);
  - `service/OrderService.java:41,47` recebe `CheckoutRequest` e devolve `SummaryResponse`/`OrderResponse`; `:82` recebe `PaymentRequest` e `:133` recebe `ShipmentRequest`;
  - `CustomerService.java:21`, `ProductService.java:21` e `NotificationService.java:35,48` devolvem `CustomerResponse`, `ProductResponse` e `NotificationResponse`;
  - a montagem da resposta da API fica no próprio serviço (`OrderService.java:227-258`).

O relatório diz "Classes outside the web layer that use the incoming web: none" porque não conta o `HttpStatus`. A régua conta os códigos HTTP, e eles estão nas cinco classes de serviço.
Conclusão: não segue

**A2, os ciclos entre pacotes são acoplamento real?** O relatório não acha nenhum ciclo ("Cycles: 0 group(s), 0 of 8 packages caught in one"). As 14 dependências entre pacotes seguem num só sentido: `controller -> service -> repository -> entity -> model`, com `dto` e `exception` ao lado.
Conclusão: segue

**A3, o controller só traduz?** Sim. Cada método recebe o corpo ou o caminho, chama um serviço e embrulha o resultado. Exemplos: `controller/OrderController.java:19-23` (`createOrder` chama `orderService.createOrder` e devolve 201), `:31-37` (pagamento), `controller/CheckoutController.java:19-23` e `controller/CustomerController.java:26-35`. Nenhum controller faz conta, decide uma recusa, muda a situação do pedido ou mexe no estoque. A tradução da recusa para HTTP é que não está na camada web, mas esse problema já foi contado no A1.
Conclusão: segue

**A4, o acesso ao banco está isolado?** Sim. Os serviços só falam com interfaces `JpaRepository`:
- `repository/ProductRepository.java`, `OrderRepository.java`, `CustomerRepository.java` e `NotificationRepository.java`;
- as duas consultas escritas à mão são JPQL com trava e ficam nos repositórios (`ProductRepository.java:16-18`, `CustomerRepository.java:16-18`).

Uma busca por `JdbcTemplate`, `java.sql`, `EntityManager`, `createNativeQuery` e `nativeQuery` não acha nada no código principal. Um detalhe fora da régua: o BUY3PAY2 busca o produto de novo no repositório (`CheckoutCalculator.java:284`), embora ele já esteja no `productMap`. É uma ineficiência, não um vazamento de camada.
Conclusão: segue

**A5, um cálculo só para o resumo e para o pedido?** Sim. Os dois fluxos chamam o mesmo método:
- o resumo: `service/OrderService.java:42`, `checkoutCalculator.calculate(request)`;
- o registro: `:48`, a mesma chamada, e o resultado vai para o pedido com `order.setSummary(summary)` em `:59`.

As validações, o frete, o cupom, o seguro, o pagamento e o crédito ficam todos dentro de `CheckoutCalculator.calculate` (`:34-81`). Há porém uma cópia parcial morta: `CheckoutCalculator.applyGoldBenefits` (`:423-428`) repete a regra "GOLD não paga frete, exceto no motoboy", que já está em `:47-49`. Nenhum código chama esse método (a busca por `applyGoldBenefits` só acha a definição). Hoje ele não faz os fluxos divergirem, mas é uma segunda versão de uma etapa do cálculo esperando alguém chamá-la.
Conclusão: segue

**A6, como os pacotes estão organizados?** Por camada. São oito pacotes técnicos (`config` 1, `controller` 5, `dto` 9, `entity` 6, `exception` 2, `model` 8, `repository` 4, `service` 6 classes) e nenhum pacote por assunto: pedido, catálogo, cupom e cliente se misturam dentro de `service` e `entity`. Todo o cálculo (frete, cupons, seguro, pagamento, clube) está concentrado numa classe só, `service/CheckoutCalculator.java`, com 453 linhas.
Conclusão: não se aplica

## O que mudaria primeiro

1. **Tirar o HTTP da recusa de negócio.** A `BusinessException` passaria a levar só o código (`ORDER_EXPIRED`, `INSUFFICIENT_STOCK`…), e o `GlobalExceptionHandler` faria a tabela código→status. Com uma só mudança, as cinco classes de serviço deixam de conhecer a web, e o status de cada recusa fica decidido num lugar só.
2. **Dar à regra de negócio tipos próprios de entrada e saída.** Por exemplo, um `CheckoutInput`/`Cart` para o `CheckoutCalculator` e o `OrderService`, e o `Order`/`OrderSummary` como retorno. Os controllers converteriam de e para `CheckoutRequest`, `OrderResponse` etc. Assim, mudar o formato do JSON do site deixa de exigir mexer no cálculo. Junto disso, apagar o `applyGoldBenefits` morto.

## Dúvidas

- **O `dto` como "objeto da API".** A régua cita os records "quando moram com os controllers", e aqui eles ficam num pacote `dto` separado. Contei mesmo assim, porque são exatamente os corpos de requisição e resposta do anexo e não servem a mais nada. O A1 não depende disso: o `HttpStatus` nos serviços já basta para `não segue`.
- **O `TransLogService`.** Ele usa `RestClient` e `@JsonProperty`, mas é a web de saída (chamar a transportadora), não a de entrada. Não contei no A1.
- **A conclusão do A6.** O item é descritivo, e nenhuma das palavras (`segue`, `em parte`, `não segue`) cabe sem fingir um julgamento. Usei `não se aplica` no sentido de "item sem juízo de valor"; a organização em si está descrita no texto.
