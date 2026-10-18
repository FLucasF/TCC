# Padrões: Adapter

## Visão geral

O agente criou um lado da TransLog: `integration/TransLogClient.java` monta a chamada `GET /v2/quote` e lê a resposta, inclusive o `error`. Também tem uma exceção própria para quando ela sai do ar. As duas entregas internas ficaram simples, com o valor fixo na própria regra. O problema mais sério é que o cliente não traduz nada. Ele recebe o peso já em gramas e o código de serviço (`"STD"`/`"EXP"`) já escolhido, e devolve um `QuoteResult(priceCents, etaHours)` na unidade dela. Por isso as conversões ficam fora do lado da TransLog. A troca de quilos por gramas mora no nosso domínio (`Weight.toGrams()`). A escolha do código de serviço fica em cada estratégia de entrega. A leitura de `priceCents`, a troca de centavos por reais e o `etaHours / 24` ficam na classe-base das estratégias. Em segundo plano, o nome da transportadora chega ao serviço de checkout e ao tratador de erros pela `TransLogUnavailableException`. Trocar de transportadora mexeria em pelo menos sete arquivos fora de `integration/`.

## Item a item

**P6, o vocabulário da transportadora fica do lado dela?** O enunciado desta execução tem o vocabulário estrangeiro (`prompt.md:205-213`: `/v2/quote`, `weight_g`, `STD`/`EXP`, `price_cents`, `eta_hours`, `ZIP_NOT_SERVED`), então o ponto se aplica.

O que fica do lado da TransLog:
- a rota e os parâmetros (`src/main/java/com/loja/integration/TransLogClient.java:25-26`);
- a leitura da resposta e do `error`, que vira `null` (`TransLogClient.java:28-35`);
- o record da resposta (`TransLogClient.java:43-47`);
- a exceção de site fora do ar (`TransLogClient.java:38-39`).

O que vaza, e por onde:
- **as gramas no domínio e nas estratégias.** `Weight.toGrams()` faz `kilograms × 1000` (`src/main/java/com/loja/domain/Weight.java:24-26`). Quem chama é `TransLogShipping`, que passa o resultado ao cliente (`src/main/java/com/loja/service/shipping/TransLogShipping.java:26` e `:39`). A assinatura do cliente já pede gramas: `getQuote(String zipCode, int weightGrams, String service)` (`TransLogClient.java:23`). A conversão da nossa unidade para a dela acontece fora do adaptador.
- **os códigos de serviço nas estratégias.** `EconomyShipping.getServiceCode()` devolve `"STD"` (`service/shipping/EconomyShipping.java:21`), e `ExpressShipping` devolve `"EXP"` (`service/shipping/ExpressShipping.java:21`). A tradução `ECONOMY -> "STD"` deveria ser trabalho do adaptador.
- **os campos e as unidades da resposta nas estratégias.** O cliente expõe `public record QuoteResult(Integer priceCents, Integer etaHours)` (`TransLogClient.java:49`). Declarar esse record ali não é vazamento; o vazamento é o uso de fora. `TransLogShipping` lê `result.priceCents()` (`TransLogShipping.java:29`) e divide por 100 para ter reais (`TransLogShipping.java:43`). Também faz `Math.ceil(result.etaHours() / 24.0)` (`TransLogShipping.java:44`). É exatamente o caso que a régua dá como exemplo: a estratégia de entrega converte horas em dias.

`TransLogShipping` conta como fora do lado da TransLog. O nome é "TransLog", mas a classe está em outra pasta (`service/shipping`). Ela implementa a nossa `ShippingStrategy` e conhece os nossos casos, como o `switch` sobre `NORTH`/`NORTHEAST` (`TransLogShipping.java:50-55`).

Há ainda um acoplamento que não está na lista do vocabulário (é o nome da transportadora, não a API dela). `CheckoutService` importa e captura `TransLogUnavailableException` (`service/CheckoutService.java:6`, `:207`). `GlobalExceptionHandler` faz o mesmo (`exception/GlobalExceptionHandler.java:3`, `:19-20`). Trocar de transportadora também mexeria nesses dois arquivos.

Como o lado da TransLog existe e faz a chamada e a leitura, mas o peso em gramas, o código de serviço, os centavos e as horas vazam, o ponto é malfeito.
Conclusão: malfeito

**P6-exagero, as entregas sem transportadora.** A retirada devolve `new ShippingResult(Money.zero(), 1)` direto na sua regra (`service/shipping/StorePickupShipping.java:22`). O motoboy devolve `new ShippingResult(new Money("18.00"), 0)` com o limite de 5 kg numa constante (`service/shipping/CourierShipping.java:11`, `:20`, `:25`). Nenhuma das duas recebe o `TransLogClient` ou estende `TransLogShipping`. Não há um "cliente" do motoboy nem uma porta de transportadora por onde a retirada passe. Os valores fixos estão na própria regra da entrega.
Conclusão: contenção

**E, estrutura sem necessidade perto do Adapter.** Não há interface de transportadora com uma implementação só: `TransLogClient` é uma classe concreta. A classe-base `TransLogShipping` é dividida por duas estratégias reais (econômica e expressa), e não é uma camada que só repassa. A `TransLogUnavailableException` é uma exceção só, usada para o caso de site fora do ar. Não achei builder, fábrica ou camada extra em volta da integração.
Conclusão: contenção

## O que mudaria primeiro

1. **Fazer o cliente da TransLog falar a nossa língua.** A assinatura seria algo como `quote(String zipCode, Weight weight, ShippingMethod method)`, devolvendo o frete em `Money` e o prazo em dias. As conversões passariam para dentro de `integration/`:
   - quilos para gramas, com `Weight.toGrams()` saindo do domínio;
   - `ECONOMY`/`EXPRESS` para `"STD"`/`"EXP"`;
   - centavos para reais;
   - `ceil(horas / 24)`.

   Assim `EconomyShipping`, `ExpressShipping` e `TransLogShipping` não conheceriam mais nenhum termo dela. Isso fecha os três vazamentos de uma vez.
2. **Trocar a `TransLogUnavailableException` por uma exceção neutra** (algo como "transportadora indisponível"). Com isso `CheckoutService` e `GlobalExceptionHandler` não dependeriam do nome da transportadora, e a troca ficaria só em `integration/`.

## Dúvidas

- **Se `TransLogShipping` é do lado da TransLog.** O nome sugere que sim. Mas pela definição da régua (a pasta de quem faz a chamada HTTP, sem conhecer os nossos casos) ela fica de fora: está em `service/shipping`, implementa a nossa estratégia e decide o prazo extra por região. Por isso contei como vazamento o que ela faz com `priceCents` e `etaHours`.
- **Se a exceção com o nome da transportadora conta como vazamento.** A lista da régua é o formato da API, os campos, os códigos de serviço e as unidades, e o nome da empresa não está nela. Registrei como acoplamento à parte, sem pesar na conclusão, que já é `malfeito` pelos outros vazamentos.
- **Dois problemas de funcionamento que não entram nesta dimensão**, deixados para o comportamento (não compilei nem rodei):
  - `QuoteResponse` declara `priceCents`/`etaHours` sem `@JsonProperty`, e não achei configuração de snake_case em `application.properties`. A leitura de `price_cents` parece que daria `null`.
  - `TransLogShipping.isAvailable` captura qualquer `Exception` e devolve `false` (`TransLogShipping.java:30-31`). Assim o site fora do ar parece sair como `SHIPPING_METHOD_UNAVAILABLE`, e o `catch` de `CheckoutService.java:207` não seria alcançado.
