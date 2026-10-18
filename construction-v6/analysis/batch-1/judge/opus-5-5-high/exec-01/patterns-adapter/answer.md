Já li o código de entrega e conferi onde aparece o vocabulário da TransLog; agora escrevo a correção.

# Padrões: Adapter

## Visão geral

O agente isolou a conversa HTTP com a TransLog numa peça só, `TransLogGateway`, que implementa uma porta própria, `CarrierGateway`. A rota `/v2/quote`, os campos `price_cents` e `eta_hours`, o erro `ZIP_NOT_SERVED` e as conversões de centavos para reais e de horas para dias ficam dentro do adaptador. A resposta já sai na nossa língua: `CarrierQuote(BigDecimal price, int days)`, além de duas exceções nossas. É o ponto forte.

O problema mais sério está no caminho de ida. A porta foi desenhada com a entrada da TransLog, `quote(String zipCode, long weightGrams, String service)`. Por isso duas coisas dela ficam fora do adaptador. A estratégia de entrega converte os quilos em gramas antes de chamar, e a configuração das entregas escreve os códigos de serviço `"STD"` e `"EXP"`. Trocar de transportadora exigiria mexer na porta, na estratégia e na configuração, não só no adaptador.

No controle negativo, a retirada e o motoboy são valores fixos na própria regra da entrega, sem passar pela porta da transportadora. Isso é contenção.

## Item a item

**P6, o vocabulário da transportadora fica do lado dela?**

O lado da TransLog é `shipping/TransLogGateway.java`, o único arquivo que faz a chamada HTTP. Dentro dele ficam:
- a rota e os parâmetros (`TransLogGateway.java:34`): `/v2/quote`, `zip`, `weight_g`, `service`;
- o erro `ZIP_NOT_SERVED`, traduzido para a nossa `ZipNotServedException` (`TransLogGateway.java:50-52`);
- os campos `price_cents` e `eta_hours` (`TransLogGateway.java:56-57`);
- as conversões das unidades da resposta: centavos para reais em `BigDecimal.valueOf(cents.longValue(), 2)` (`TransLogGateway.java:62`) e horas para dias, arredondando para cima, em `(hours + 23) / 24` (`TransLogGateway.java:63`).

Nada disso aparece fora do adaptador no código de produção. O `grep` por `price_cents`, `eta_hours`, `ZIP_NOT_SERVED`, `v2/quote` e `weight_g` só encontra esse arquivo e os testes. O `CheckoutService` fala só a nossa língua: soma o peso em quilos (`checkout/CheckoutService.java:67-68`), pede `ShippingRequest(zip, region, weight)` (`checkout/CheckoutService.java:70`) e recebe `ShippingQuote(cost, days)`. Os dias a mais do Norte e do Nordeste são somados sobre dias já nossos (`shipping/CarrierShipping.java:31`). A volta está bem isolada.

A ida vaza, por dois lugares:
- **Gramas.** A porta pede o peso na unidade da TransLog, `long weightGrams` (`shipping/CarrierGateway.java:12`). Quem converte é a estratégia de entrega, `CarrierShipping`, uma `ShippingOption` do nosso lado: `request.weightKg().movePointRight(3).setScale(0, RoundingMode.HALF_UP)` (`shipping/CarrierShipping.java:28`). Esse é o caso que a régua dá como vazamento: converter os quilos em gramas antes de chamar a transportadora. A regra de arredondamento para gramas inteiros, que é exigência da TransLog, também ficou fora do adaptador.
- **Códigos de serviço.** `"STD"` e `"EXP"` são escritos na configuração das nossas entregas, `new CarrierShipping("ECONOMY", "STD", gateway)` e `new CarrierShipping("EXPRESS", "EXP", gateway)` (`shipping/ShippingConfig.java:11,16`). Esse arquivo conhece os nossos casos e guarda o código num campo da estratégia, `carrierService` (`shipping/CarrierShipping.java:12,30`). A tradução `ECONOMY -> "STD"` / `EXPRESS -> "EXP"` deveria morar no adaptador. Lá só sobrou um repasse que não traduz nada: `service.equals("STD") ? "STD" : "EXP"` (`TransLogGateway.java:35`), que ainda transforma qualquer outro texto em `EXP`.

Fica uma dúvida de fronteira. Pela definição da régua, `CarrierGateway.java` está na mesma pasta e não conhece os nossos casos, então poderia contar como lado da TransLog. Mas ela é a porta que a estratégia usa: é o "nosso lado" da conversa. Contando de um jeito ou de outro, a conclusão é a mesma, porque a conversão para gramas (`CarrierShipping.java:28`) e os códigos `STD`/`EXP` (`ShippingConfig.java:11,16`) estão em arquivos que são claramente do nosso lado.

A parte da resposta é acerto: preço em reais, prazo em dias e erros traduzidos. Mas a régua mede se *qualquer* termo ou unidade da TransLog aparece fora do lado dela, e dois aparecem. Por isso o ponto fica como vazamento (a omissão do ponto), mesmo com metade bem feita.
Conclusão: omissão

**P6-exagero, as entregas sem transportadora.** A retirada devolve o valor fixo na própria classe, `new ShippingQuote(Money.ZERO, 1)` (`shipping/StorePickupShipping.java:17`). O motoboy faz o mesmo com `new ShippingQuote(Money.of("18.00"), 0)` e confere ali mesmo o limite de 5 kg (`shipping/CourierShipping.java:14,23-26`). Nenhuma das duas recebe nem chama `CarrierGateway`, e não há "cliente" do motoboy nem adaptador que devolva um número fixo. A porta de transportadora só é injetada nas duas `CarrierShipping` (`shipping/ShippingConfig.java:10-16`). É a resposta do tamanho do problema.
Conclusão: contenção

**E, estrutura sem necessidade fora dos pontos (perto do Adapter).** `CarrierGateway` tem uma implementação só (`TransLogGateway`), mas é justamente o que o P6 pede ("Pode ser que um dia a gente troque de transportadora"), então não conta aqui. `ShippingConfig` só cria dois beans da mesma classe com parâmetros diferentes (`shipping/ShippingConfig.java:9-17`), o que é configuração e não camada extra. Não achei, perto do Adapter, interface, camada de repasse ou fábrica que nenhum ponto peça.
Conclusão: contenção

## O que mudaria primeiro

1. **Mudar a porta para a nossa língua:** `quote(String zipCode, BigDecimal weightKg, <a nossa entrega, ECONOMY ou EXPRESS>)`. A conversão para gramas inteiros e a tradução para `STD`/`EXP` passariam para dentro do `TransLogGateway`. Com isso, `CarrierShipping.java:28` e `ShippingConfig.java:11,16` deixariam de saber como a TransLog quer o peso e o serviço. Trocar de transportadora passaria a ser só escrever outro adaptador, que é a promessa da própria interface.
2. Na mesma mudança, trocar o repasse `service.equals("STD") ? "STD" : "EXP"` (`TransLogGateway.java:35`) por uma tradução de verdade, que recuse um caso desconhecido em vez de mandá-lo como `EXP`.

## Dúvidas

- **Onde fica a fronteira de `CarrierGateway.java`:** a definição da régua ("mesma pasta, não conhece os nossos casos") a põe do lado da TransLog, e o papel dela no código a põe do nosso. Tratei a interface como a porta, do nosso lado. Isso não muda a conclusão, porque o vazamento também aparece em `CarrierShipping` e `ShippingConfig`.
- **Os testes:** `LojaIntegrationTest.java:70-71` monta o stub com `cents`/`hours` e refaz a conta `(hours + 23) / 24`. É código de teste, não do serviço, e não contei como vazamento.
