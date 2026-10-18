# Padrões: Adapter

## Visão geral

O agente pôs a conversa com a TransLog numa classe concreta só, `TransLogService`. Não criou interface de transportadora. Quase todo o vocabulário dela fica nessa classe: a rota `/v2/quote`, o parâmetro `weight_g`, os campos `price_cents`/`eta_hours`, o erro `ZIP_NOT_SERVED` e as três conversões de unidade (quilos para gramas, centavos para reais, horas para dias). Quem chama passa o peso em quilos e recebe reais e dias. O problema mais sério é que o contrato do adaptador pede o código de serviço da TransLog como `String service`. Por isso a tradução `ECONOMY → "STD"` / `EXPRESS → "EXP"` fica no `CheckoutCalculator`, fora da peça. Se a transportadora for trocada, esse arquivo também precisa mudar. A retirada e o motoboy ficam como valores fixos na regra da entrega, sem passar por nenhuma porta de transportadora: isso é contenção. Fora dos pontos, não há estrutura desnecessária perto deste pattern.

## Item a item

**P6, o vocabulário da transportadora fica do lado dela?** O lado da TransLog é só `src/main/java/com/loja/orderservice/service/TransLogService.java`. É o único arquivo que faz a chamada HTTP. Os outros arquivos da pasta `service` conhecem os nossos casos, e o `CheckoutCalculator` é um deles.

O que está do lado dela:
- a conversão de quilos para gramas, `weightKg.multiply(new BigDecimal("1000"))` (`TransLogService.java:29`);
- a rota e os parâmetros `/v2/quote?zip=%s&weight_g=%d&service=%s` (`TransLogService.java:31`);
- o erro `"ZIP_NOT_SERVED"` (`TransLogService.java:44`);
- a conversão de centavos para reais (`TransLogService.java:54-55`);
- a conversão de horas para dias, `Math.ceil(response.etaHours / 24.0)` (`TransLogService.java:57`);
- os campos `priceCents`/`etaHours` com `@JsonProperty`, no record privado da resposta (`TransLogService.java:108-113`).

A classe devolve um `QuoteResult` com o nosso vocabulário: preço em reais, prazo em dias, `isServed`/`isAvailable` (`TransLogService.java:66-106`). Quem chama passa `totalWeight` em quilos (`CheckoutCalculator.java:227-228`) e não converte unidade nenhuma. Os dias a mais da região são somados sobre os dias já traduzidos (`CheckoutCalculator.java:238-239`).

O que vaza: os códigos de serviço. `String service = request.getShippingMethod() == ShippingMethod.ECONOMY ? "STD" : "EXP";` está em `CheckoutCalculator.java:225` e é passado ao adaptador na linha 227. Não é um descuido isolado. A própria assinatura `getQuote(String zipCode, BigDecimal weightKg, String service)` (`TransLogService.java:27`) obriga quem chama a falar a língua da TransLog. A régua diz que essa tradução é trabalho do adaptador. Com outra transportadora, que tenha outros códigos, o `CheckoutCalculator` teria de mudar. Unidades, campos e rota ficam isolados, mas os códigos de serviço vazam pelo contrato do adaptador. A régua chama isso de vazamento, a omissão deste ponto.
Conclusão: omissão

**P6-exagero, as entregas sem transportadora.** A retirada devolve `new ShippingInfo(ZERO, 1)` (`CheckoutCalculator.java:215-216`). O motoboy devolve `new ShippingInfo(new BigDecimal("18.00"), 0)`, depois de conferir os 5 kg (`CheckoutCalculator.java:218-222`). São valores fixos na própria regra da entrega. Não há interface de transportadora, nem "cliente" do motoboy, e nenhuma das duas passa pelo `TransLogService`. O fato de as quatro entregas estarem num `switch` é assunto do Strategy, não deste item.
Conclusão: contenção

**E, estrutura sem necessidade fora dos pontos.** O código principal não tem interface além dos repositórios do Spring Data (`repository/*Repository.java`, que estendem `JpaRepository`). Também não há builder, fábrica ou camada que só repasse. Os métodos estáticos `success`/`notServed`/`unavailable` do `QuoteResult` (`TransLogService.java:79-89`) só dão nome aos três resultados de um objeto de valor pequeno. Não são uma fábrica que ninguém pediu. Perto do Adapter, nada a registrar.
Conclusão: contenção

## O que mudaria primeiro

Mudaria a assinatura do adaptador para receber a nossa entrega, por exemplo `getQuote(String zipCode, BigDecimal weightKg, ShippingMethod method)`, ou um valor neutro como "padrão/expressa". A tradução para `"STD"`/`"EXP"` passaria para dentro do `TransLogService`. Com isso, `CheckoutCalculator.java:225` não fala mais a língua da TransLog, e trocar de transportadora mexe numa peça só, que é o que o enunciado pede. Uma interface de transportadora implementada pelo `TransLogService` facilitaria a troca, mas é secundária: o que falta medir aqui é o isolamento do vocabulário.

## Dúvidas

- **Omissão ou malfeito.** O adaptador existe e isola quase todo o vocabulário, e só os códigos de serviço ficam fora. Pela definição geral, isso poderia ser lido como malfeito: a estrutura existe, mas não cobre tudo. Segui o texto próprio do P6, que chama qualquer termo da TransLog fora do lado dela de "vazamento (a omissão deste ponto)". Também pesou que o vazamento está no próprio contrato do adaptador, e não numa linha solta.
- **O lado da TransLog.** Considerei só `TransLogService.java` como lado da TransLog. Os outros arquivos da pasta `service` conhecem os nossos casos. Pela régua, isso os deixa fora do lado da transportadora, mesmo estando na mesma pasta.
