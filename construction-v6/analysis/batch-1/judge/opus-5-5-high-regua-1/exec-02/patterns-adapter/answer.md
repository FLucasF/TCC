Já li o enunciado, o `TransLogClient` e a parte do `CheckoutService` que calcula o frete. Segue a correção.

# padrões: Adapter

## Visão geral

O agente juntou a chamada HTTP à TransLog numa classe só, `integration/TransLogClient.java`. A URL, a rota `/v2/quote`, os parâmetros e a leitura de `price_cents`/`eta_hours`/`ZIP_NOT_SERVED` ficam ali dentro. Isso é o começo de um Adapter, mas a classe não traduz nada. Ela recebe o peso em gramas e o código de serviço da TransLog, e devolve centavos e horas num tipo com os nomes dela. Por isso o trabalho de tradução foi parar no `CheckoutService`. É lá que a nossa entrega vira `"STD"`/`"EXP"`, os quilos viram gramas, os centavos viram reais e as horas viram dias. Trocar de transportadora obrigaria a mexer no serviço de checkout, que é exatamente o que o ponto P6 quer evitar. O problema mais sério é esse vazamento. Na parte boa, a retirada e o motoboy ficaram com valores fixos e não passam por nenhuma estrutura de transportadora (contenção), e o projeto não criou nenhuma abstração desnecessária perto desse ponto.

## Item a item

**P6, o vocabulário da transportadora fica do lado dela?** O lado da TransLog é `src/main/java/com/loja/orderservice/integration/TransLogClient.java`, o único arquivo da pasta `integration`. Ali estão, como deve ser, a rota e os parâmetros (`TransLogClient.java:26`, `"%s/v2/quote?zip=%s&weight_g=%d&service=%s"`), a comparação com `"ZIP_NOT_SERVED"` (`:46`), a leitura de `"price_cents"` e `"eta_hours"` (`:52-53`) e o tipo aninhado da resposta com `priceCents`/`etaHours` (`:71-74`). Até aqui não há vazamento: são declarações do lado dela.

O vazamento começa pela assinatura pública do cliente. `getQuote(String zipCode, Integer weightGrams, String service)` (`:24`) pede gramas e o código de serviço dela, e devolve `TransLogQuoteResponse` em centavos e horas. Quem chama tem de falar a língua da TransLog. Em `src/main/java/com/loja/orderservice/service/CheckoutService.java` aparecem quatro vazamentos:
- `CheckoutService.java:273`: `String service = method == ShippingMethod.ECONOMY ? "STD" : "EXP";`. A tradução da nossa entrega para o código de serviço dela é trabalho do adaptador e está fora dele.
- `CheckoutService.java:274-275`: o peso em quilos convertido em gramas (`multiply(new BigDecimal("1000")).setScale(0, RoundingMode.DOWN)`), na variável `weightGrams`. É conversão de unidade fora do adaptador.
- `CheckoutService.java:277` e `:284`: o serviço usa diretamente o tipo `TransLogClient.TransLogQuoteResponse`, importado em `:11`, e o campo `transLogClient` em `:35`.
- `CheckoutService.java:285`: `response.getPriceCents()` dividido por 100, a conversão de centavos em reais. `CheckoutService.java:286`: `Math.ceil(response.getEtaHours() / 24.0)`, a conversão de horas em dias. Esse é o exemplo de vazamento que a régua cita.

Não existe porta nem interface de transportadora: o `CheckoutService` depende da classe concreta. A régua não cobra isso por si só, mas aqui a falta de porta vem junto com a falta de tradução.

Uma observação à parte, que não muda a conclusão: o adaptador junta "CEP não atendido", "resposta estranha" e "fora do ar/timeout" num mesmo `Optional.empty()` (`TransLogClient.java:47`, `:49`, `:61`, `:63`). O `if` da linha 46 nem chega a separar os casos. Assim o cliente não entrega ao resto do código, na nossa língua, a diferença entre indisponibilidade da transportadora e entrega indisponível, e `CheckoutService.java:279-281` trata tudo como `SHIPPING_METHOD_UNAVAILABLE`. Se isso dá a resposta certa é com a dimensão do comportamento; aqui fica só como sinal de uma fronteira que não traduz.

Resumo: a chamada está isolada, mas o vocabulário vaza por quatro pontos (código de serviço, gramas, centavos e horas), todos dentro do serviço de checkout.
Conclusão: omissão

**P6-exagero, as entregas sem transportadora.** O motoboy e a retirada são constantes do próprio serviço: `COURIER_COST = 18.00` (`CheckoutService.java:25`), `COURIER_DELIVERY_DAYS = 0` (`:32`), `STORE_PICKUP_DAYS = 1` (`:31`). São devolvidas direto em `calculateShipping`, sem chamada externa (`CheckoutService.java:265-271`; para a retirada também em `:61-62`). Não há interface de transportadora, nem "cliente" do motoboy, e a retirada não passa pelo `TransLogClient`. Que essas regras vivam num `if/else` sobre a entrega é assunto do Strategy (P1), não deste controle negativo. Aqui a resposta é do tamanho do problema.
Conclusão: contenção

**E, estrutura sem necessidade fora dos pontos (perto do Adapter).** Na pasta `integration` e nas classes que a usam não há interface com uma só implementação, camada de repasse nem fábrica. A única abstração do projeto são os repositórios Spring Data (`repository/*Repository.java`), que são o mecanismo normal do framework. Os `@Builder` do Lombok (nas entidades, nos DTOs e em `TransLogClient.java:70`) são gerados por anotação e não acrescentam nenhuma camada. Não achei estrutura sobrando.
Conclusão: contenção

## O que mudaria primeiro

1. Fazer o `TransLogClient` falar a nossa língua. Ele passaria a receber a `ShippingMethod` (ou um "nível de serviço" nosso), o CEP e o peso em quilos (`BigDecimal`), e a devolver uma cotação nossa com o frete em reais e o prazo em dias. A tradução `ECONOMY/EXPRESS -> STD/EXP`, a conversão para gramas, `/100` e `ceil(/24)` sairiam de `CheckoutService.java:273-286` e iriam para dentro do adaptador. O `TransLogQuoteResponse` deixaria de ser público. Assim, trocar de transportadora mexeria numa peça só.
2. Na mesma fronteira, devolver resultados distintos para "CEP não atendido" e "transportadora fora do ar" (por exemplo, duas exceções nossas), em vez de um `Optional` vazio para tudo. Depois disso, uma interface de transportadora implementada pelo adaptador facilitaria a troca, mas é secundária em relação ao isolamento.

## Dúvidas

- Considerei o tipo aninhado `TransLogClient.TransLogQuoteResponse` como "declarado do lado dela", porque fica dentro do arquivo do adaptador. O vazamento que contei é o uso desse tipo e dos campos dele no `CheckoutService`, não a declaração.
- A junção de erros dentro do adaptador (`Optional.empty()` para tudo) entrou como observação no P6, sem pesar na conclusão: a régua do ponto mede o isolamento do vocabulário, e as consequências de comportamento ficam com a outra dimensão.
