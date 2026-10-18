# Padrões: Adapter

## Visão geral

O agente isolou a conversa HTTP com a TransLog numa peça só, `TranslogClient`, atrás de uma porta, `CarrierGateway`. A peça cuida do formato da API: a rota `/v2/quote`, o parâmetro `weight_g`, os campos `price_cents` e `eta_hours`, o erro `ZIP_NOT_SERVED` e os códigos `STD`/`EXP`. Também já entrega o preço convertido em reais. O isolamento, porém, fica pela metade nas unidades. A própria porta fala em gramas (`weightGrams`) e em horas (`etaHours`). Por isso a estratégia das entregas por transportadora, `CarrierShippingOption`, faz duas conversões que eram trabalho do adaptador: quilos para gramas e horas para dias. Trocar de transportadora mexeria nessa estratégia e na porta, e não só no adaptador. Isso é o problema mais sério. As entregas internas (retirada e motoboy) ficaram com valores fixos na própria regra, sem passar pela porta da transportadora, o que é a resposta do tamanho do problema.

## Item a item

**P6, o vocabulário da TransLog fica do lado dela?**

O que fica do lado da TransLog, todo em `src/main/java/br/com/loja/pedidos/shipping/TranslogClient.java`:
- a tradução do nosso nível de serviço para o código dela (`"EXP"`/`"STD"`, linha 43);
- a rota `/v2/quote` e os parâmetros `zip`, `weight_g` e `service` (linhas 44–45);
- a interpretação de `ZIP_NOT_SERVED` (linha 71);
- a leitura de `price_cents` e `eta_hours` (linha 73);
- a conversão dos centavos para reais, `BigDecimal.valueOf(cents.longValue(), 2)` (linha 75).

O código do lado de cá também não usa os nomes estrangeiros. A busca por `price_cents`, `eta_hours`, `ZIP_NOT_SERVED`, `"STD"`, `"EXP"` e `weight_g` em `src/main` só os encontra nesse arquivo. O `CheckoutService` pede a cotação em quilos e recebe o frete em reais e o prazo em dias (`checkout/CheckoutService.java:59-61`).

O que vaza, e por onde:
- **A porta fala as unidades da TransLog.** A assinatura é `quote(String zipCode, long weightGrams, ServiceLevel level)` (`shipping/CarrierGateway.java:8`) e o resultado é `Quoted(BigDecimal price, long etaHours)` (`shipping/CarrierGateway.java:13`). Assim, quem usa a porta precisa trabalhar em gramas e em horas, que são as unidades da TransLog, e não em quilos e dias.
- **A conversão dos quilos em gramas acontece fora do adaptador.** Em `shipping/CarrierShippingOption.java:27-28`, `request.weightKg().multiply(BigDecimal.valueOf(1000)).setScale(0, RoundingMode.CEILING)`. A régua cita este caso pelo nome como vazamento.
- **A conversão das horas em dias também acontece fora do adaptador.** Em `shipping/CarrierShippingOption.java:31`, `(q.etaHours() + 23) / 24`: a estratégia de entrega lê o campo `etaHours` e faz a divisão por 24 que a régua cita como exemplo de vazamento.

A metade do formato da API está bem isolada. A metade das unidades está do lado errado da porta. Há um vazamento real, por dois pontos, numa estratégia de entrega.

Conclusão: omissão

**P6-exagero, as entregas sem transportadora.**
- A retirada devolve o valor fixo direto na sua regra: `new ShippingQuote(Money.ZERO, 1)` (`shipping/StorePickupShipping.java:16`).
- O motoboy faz o mesmo, com o limite de 5 kg na própria classe: `new ShippingQuote(Money.of("18.00"), 0)` (`shipping/CourierShipping.java:20-23`).
- As duas classes implementam `ShippingOption`, a interface comum das entregas, e não recebem nem implementam `CarrierGateway`. Também não existe um "cliente" do motoboy nem uma porta de transportadora para a retirada.
- Só as duas entregas da TransLog são ligadas à porta (`shipping/ShippingConfig.java:10-17`).

Conclusão: contenção

**E, estrutura sem necessidade perto do Adapter.**
- `CarrierGateway` tem uma implementação só (`TranslogClient`). É a porta que o P6 justifica (a troca de transportadora), então não conta aqui.
- Os resultados selados `Quoted`/`NotServed`/`Unavailable` (`shipping/CarrierGateway.java:10-20`) traduzem as três respostas da TransLog para a nossa língua. Têm uso.
- O enum `ServiceLevel` (`shipping/ServiceLevel.java:3-5`) é o nível neutro que o adaptador traduz para `STD`/`EXP`. Também tem uso.
- `ShippingConfig` só monta os dois beans das entregas da TransLog.

Não achei, perto deste pattern, interface, camada ou fábrica que ninguém pedisse.

Conclusão: contenção

## O que mudaria primeiro

1. **Fazer a porta falar a nossa língua.** A assinatura passaria a ser `CarrierGateway.quote(String zipCode, BigDecimal weightKg, ServiceLevel level)`, e o resultado traria o prazo já em dias (`Quoted(BigDecimal price, int days)`). As duas conversões, quilos em gramas com arredondamento para cima e horas em dias com arredondamento para cima, passariam para dentro de `TranslogClient`. Assim, uma transportadora nova que cote em quilos e em dias, ou em libras e em minutos, mexe só no adaptador dela.
2. Com isso, `CarrierShippingOption` fica só com o que é nosso: somar os dias a mais da região (`request.region().dispatchDelayDays()`) e transformar "não atende" e "fora do ar" nos nossos erros.

## Dúvidas

- **Se a porta `CarrierGateway` é do lado da TransLog.** Ela está na mesma pasta do adaptador e não cita as nossas entregas. Pela letra da régua ("os da mesma pasta que não conhecem os nossos casos"), poderia contar como lado da TransLog. Só que, por essa letra, a pasta `shipping` inteira, incluindo `CarrierShippingOption`, também contaria, e a própria régua dá a estratégia de entrega que faz `etaHours / 24` como vazamento. Por isso li a porta como o lado de cá: é o contrato que o resto do código consome. Mesmo na leitura contrária, a conclusão é a mesma, porque as duas conversões de unidade estão na estratégia (`CarrierShippingOption.java:27-28` e `:31`), que é do lado de cá em qualquer leitura.
- **A conversão dos centavos.** Ela está corretamente dentro do adaptador (`TranslogClient.java:75`), então o frete não vaza. Só o peso e o prazo vazam.
