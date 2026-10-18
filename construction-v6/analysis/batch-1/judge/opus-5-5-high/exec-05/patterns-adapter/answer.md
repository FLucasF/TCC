# Padrões: Adapter

## Visão geral

O agente isolou a TransLog numa classe só, `TransLogService`. Ela monta a rota `/v2/quote` com `weight_g`, faz a chamada HTTP, lê `price_cents`/`eta_hours`/`ZIP_NOT_SERVED` num record privado e converte tudo para as nossas unidades: gramas, reais e dias. O resto do código recebe um `QuoteResult` com o preço em reais e o prazo em dias. Esse é o ponto forte. O problema mais sério é que a assinatura de `getQuote` pede o código de serviço da TransLog como `String`, então a escolha `"STD"`/`"EXP"` ficou no meio do cálculo do frete, em `CheckoutCalculator`. Por esse buraco o vocabulário da transportadora vaza para fora do lado dela. Não há interface de transportadora. O ponto não exige uma, mas a calculadora depende direto da classe concreta. A retirada e o motoboy ficaram como valores fixos na própria regra da entrega, sem estrutura a mais.

## Item a item

**P6, o vocabulário da transportadora fica do lado dela?**
- **O lado da TransLog é `src/main/java/com/loja/orderservice/service/TransLogService.java`:**
  - a chamada HTTP em `TransLogService.java:31-37`, com a rota `/v2/quote?zip=%s&weight_g=%d&service=%s`;
  - a conversão de quilos para gramas em `TransLogService.java:29`;
  - a leitura de `ZIP_NOT_SERVED` em `TransLogService.java:44`;
  - a conversão de `priceCents` para reais em `TransLogService.java:54-55`;
  - a conversão de `etaHours` para dias, arredondando para cima, em `TransLogService.java:57`;
  - o record da resposta com `@JsonProperty("price_cents")` e `@JsonProperty("eta_hours")` em `TransLogService.java:108-113`. Declarar esses nomes ali não é vazamento.
- **O que sai do adaptador está na nossa língua.** `QuoteResult` (`TransLogService.java:66-106`) traz `price` em reais, `days` em dias e os estados `available`/`served`. A calculadora passa o peso em quilos (`CheckoutCalculator.java:207-212, 227-228`) e só lê `getPrice()`/`getDays()` (`CheckoutCalculator.java:239`). Não aparece `priceCents`, `etaHours`, gramas nem horas fora da classe: o `grep` em `src/main` só os acha em `TransLogService.java`.
- **O vazamento é a escolha do código de serviço.** `CheckoutCalculator.java:225` faz `request.getShippingMethod() == ShippingMethod.ECONOMY ? "STD" : "EXP"` dentro de `calculateShipping` e passa a string para `getQuote(String zipCode, BigDecimal weightKg, String service)` (`TransLogService.java:27`). Traduzir a nossa entrega para o código dela é trabalho do adaptador. Aqui esse trabalho ficou no cálculo do frete, e a própria assinatura do adaptador pede o vocabulário da TransLog. Trocar de transportadora obrigaria a mudar `CheckoutCalculator` também, não só a peça da TransLog.

Esse é o caso que a régua chama de malfeito: o lado da TransLog existe e cuida da chamada, da resposta e das unidades, mas o `"STD"`/`"EXP"` vaza para o cálculo do frete.
Conclusão: malfeito

**P6-exagero, as entregas sem transportadora.** A retirada devolve `new ShippingInfo(ZERO, 1)` (`CheckoutCalculator.java:215-217`). O motoboy confere os 5 kg e devolve `new ShippingInfo(new BigDecimal("18.00"), 0)` (`CheckoutCalculator.java:218-223`). Os dois são o valor e o prazo na própria regra da entrega. Nenhum deles passa por `TransLogService`, e não existe interface de transportadora, "cliente" do motoboy nem adaptador que devolva número fixo: no `grep` por `interface|implements|abstract class` em `src/main` só aparecem os repositórios do Spring Data. O `switch` sobre as entregas é assunto do Strategy (P1), não deste controle.
Conclusão: contenção

**E, estrutura sem necessidade fora dos pontos.** Perto do Adapter não há estrutura que ninguém pediu.
- `QuoteResult` é o tipo que o adaptador devolve, com três fábricas estáticas (`success`, `notServed`, `unavailable`, em `TransLogService.java:79-89`). Ele carrega os três desfechos que o enunciado distingue: cotou, CEP não atendido, fora do ar. Não é uma camada que só repassa.
- As únicas interfaces do projeto são os repositórios JPA (`repository/*.java`).
- Não há builder, fábrica nem interface com implementação única ao redor da TransLog.
- O campo `translogUrl` (`TransLogService.java:15, 21`) é guardado e nunca lido, e o método público `applyGoldBenefits` (`CheckoutCalculator.java:423-428`) não é chamado em lugar nenhum de `src/main`. São sobras, não estrutura.
Conclusão: contenção

## O que mudaria primeiro

1. **Mover a tradução `ECONOMY -> "STD"`, `EXPRESS -> "EXP"` para dentro de `TransLogService`.** O `getQuote` passaria a receber um `ShippingMethod`, ou um nível de serviço nosso, em vez de `String service`. Assim `CheckoutCalculator.java:225` deixa de citar o código da TransLog e a assinatura do adaptador fica só na nossa língua. É o único vazamento, e fechá-lo deixa o ponto como a régua pede.
2. **Opcional: pôr uma porta de transportadora** (uma interface que `TransLogService` implementa) e fazer a calculadora depender dela, não da classe concreta. Não muda a conclusão do P6, mas faria a troca de transportadora prevista no enunciado mexer só no adaptador e na ligação do Spring.

## Dúvidas

- **A assinatura com `String service`:** decidi tratá-la como parte do mesmo vazamento, não como um segundo. O adaptador não cita o nosso enum, mas obriga quem chama a conhecer os códigos dela. O sintoma que a régua nomeia é o `"STD"` no cálculo do frete (`CheckoutCalculator.java:225`), e a assinatura é a causa dele.
- **Os testes:** eles param `getQuote` com `anyString()` (`OrderIntegrationTest.java:51`) e não mostram mais vazamento. Julguei o isolamento só por `src/main`.
