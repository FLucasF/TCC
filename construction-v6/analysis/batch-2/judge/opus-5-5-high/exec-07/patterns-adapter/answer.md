# padrões: Adapter

## Visão geral

Há um lado da TransLog. `TransLogGateway` (`src/main/java/shop/shipping/TransLogGateway.java`) é a única classe que faz a chamada HTTP, monta a rota `/v2/quote`, lê `price_cents` e `eta_hours`, trata o `ZIP_NOT_SERVED` e passa o preço de centavos para reais. Ela fica atrás de uma porta, `CarrierGateway`, que o teste troca por um falso. A retirada e o motoboy não passam por essa porta. Mesmo assim, três partes do vocabulário da TransLog ficam do nosso lado:

- **As horas viram dias fora do adaptador.** A conta `(etaHours + 23) / 24` está na estratégia de entrega `CarrierShipping`.
- **Os quilos viram gramas fora do adaptador.** A conversão está em `ShippingContext.weightGrams()`, e a porta pede o peso em gramas.
- **O código de serviço é escolhido fora do adaptador.** `"STD"` e `"EXP"` são escolhidos na montagem do serviço (`App`) e entram pela porta como um texto "específico da transportadora".

Isso deixa a porta com as unidades e os códigos da TransLog. Trocar de transportadora mexeria na porta, na estratégia de entrega, no contexto e na montagem. O controle negativo está bem resolvido.

## Item a item

**P6, o vocabulário da transportadora fica do lado dela?**

O lado da TransLog existe e é uma peça só:
- `TransLogGateway.java:33` monta a rota `/v2/quote` com o parâmetro `weight_g`.
- `:35-38` faz a chamada e trata a falha (um código HTTP diferente de 200 vira `CARRIER_UNAVAILABLE`).
- `:53-54` traduz o `ZIP_NOT_SERVED` para `SHIPPING_METHOD_UNAVAILABLE`.
- `:58-59` lê `price_cents` e `eta_hours`.
- `:64` passa os centavos para reais: `BigDecimal.valueOf(price.asLong(), 2)`.

Nada disso aparece fora dessa classe. O vazamento está em outros pontos:

- **O prazo em horas sai do adaptador.**
  - O record de retorno, `CarrierQuote.java:5`, guarda `int etaHours`.
  - A estratégia de entrega converte as horas em dias em `CarrierShipping.java:23`: `int days = (quote.etaHours() + 23) / 24 + ...`. É o caso que a régua dá como exemplo de vazamento: uma estratégia trabalhando com a unidade da transportadora.
  - O preço, ao contrário, já sai em reais (`CarrierQuote.price`), e esse lado está certo.
- **O peso em gramas.**
  - A porta pede gramas: `CarrierGateway.java:10`, `quote(String zipCode, int weightGrams, String service)`.
  - A conversão de quilos para gramas fica no contexto da entrega, que é compartilhado por todas as entregas: `ShippingContext.java:9-11`, `weightKg.movePointRight(3).setScale(0, RoundingMode.CEILING)`.
  - Quem chama essa conversão é a estratégia: `CarrierShipping.java:22`, `context.weightGrams()`.
  - Pela régua, converter os quilos em gramas antes de chamar a transportadora é vazamento.
- **Os códigos de serviço.**
  - `"STD"` e `"EXP"` são escritos fora do adaptador, na montagem: `App.java:39-40`, `new CarrierShipping("ECONOMY", "STD", translog)` e `new CarrierShipping("EXPRESS", "EXP", translog)`.
  - Eles passam pela estratégia (`CarrierShipping.java:6,22`) até a porta, que documenta o parâmetro como "carrier-specific service code" (`CarrierGateway.java:6`).
  - A tradução `EXPRESS -> "EXP"` deveria estar dentro do adaptador. Do jeito que está, outra transportadora com outros códigos obrigaria a mudar `App`.

A porta (`CarrierGateway`) e o fato de a chamada HTTP estar isolada são pontos fortes. Mas a régua mede o isolamento do vocabulário, e três termos ou unidades da TransLog estão do nosso lado.
Conclusão: malfeito

**P6-exagero, as entregas sem transportadora.**
- A retirada devolve o valor fixo na própria classe: `StorePickupShipping.java:13`, `new ShippingQuote(Money.ZERO, 1)`.
- O motoboy faz o mesmo, com o limite de peso na própria regra: `CourierShipping.java:18-21`, `Money.of("18.00"), 0`.
- Nenhuma das duas recebe um `CarrierGateway` (`App.java:41-42`). Não há "cliente" do motoboy nem uma transportadora falsa para a retirada.
- As duas classes são as casas desses casos da entrega, que é o ponto P1 (Strategy). Não são uma estrutura de transportadora.

Conclusão: contenção

**E, estrutura sem necessidade perto do Adapter.**
- `CarrierGateway` tem uma implementação só em `src/main`, mas é substituída por um falso nos testes: `ServiceTest.java:36`, `CarrierGateway gateway = (zip, grams, service) -> ...`. Além disso, o enunciado prevê a troca de transportadora. Não é uma interface sem uso.
- `CarrierQuote` e `ShippingQuote` são dois records parecidos, mas um é o retorno da porta e o outro é o da entrega. Cada um tem o seu papel, e nenhum só repassa.
- Não achei outra camada, fábrica ou builder perto da transportadora.

Conclusão: contenção

## O que mudaria primeiro

1. **Fazer a porta falar só a nossa língua.** A porta ficaria `quote(String zipCode, BigDecimal weightKg, <a nossa entrega>)` e devolveria o frete em reais e o prazo em dias. A conversão para gramas (hoje em `ShippingContext.weightGrams()`), a conta `/24` (hoje em `CarrierShipping:23`) e a escolha entre `STD` e `EXP` (hoje em `App:39-40`) passariam para dentro de `TransLogGateway`. Com isso, trocar de transportadora mexeria só no adaptador.
2. **Trocar `etaHours` em `CarrierQuote` por um prazo em dias.** Assim nenhum nome de campo da TransLog passa da porta.

## Dúvidas

- **De que lado ficam `CarrierQuote` e `CarrierGateway`.** Os dois estão na pasta `shipping`, junto com a classe que faz a chamada, e não citam os nossos casos. Pela régua, poderiam contar como "lado da TransLog". Tratei-os como a porta, o nosso lado da conversa, porque a estratégia `CarrierShipping` os usa diretamente. O vazamento fica comprovado onde o vocabulário é *usado* fora do adaptador (`CarrierShipping.java:22-23`, `ShippingContext.java:9-11`, `App.java:39-40`), e isso vale qualquer que seja o lado em que se ponham os dois arquivos.
- **Se `"STD"` e `"EXP"` em `App` contam como vazamento.** `App` é só a montagem do serviço, mas escreve os códigos de serviço da TransLog fora do adaptador. Contei como vazamento.
