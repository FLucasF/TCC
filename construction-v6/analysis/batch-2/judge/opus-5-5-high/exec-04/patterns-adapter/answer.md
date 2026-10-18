# Padrões: Adapter

## Visão geral

O agente isolou a conversa com a TransLog numa classe só, `TransLogClient` (`src/main/java/com/clothing/service/TransLogClient.java`). Ela monta a rota `/v2/quote` com `weight_g`, faz a chamada HTTP com o limite de 2 segundos, lê `price_cents`, `eta_hours` e `error`, e já devolve um `ShippingQuote(price, deliveryDays)` em reais e em dias. Recebe o peso em quilos e converte para gramas por dentro. As três unidades da TransLog (centavos, horas e gramas) ficam do lado dela, e esse é o ponto forte.

O isolamento tem um furo, e ele se repete três vezes. A tradução da nossa entrega para o código de serviço dela (`ECONOMY -> "STD"`, `EXPRESS -> "EXP"`) não está no adaptador: quem chama é que escolhe e passa a string. Isso aparece em `CheckoutService.java:122`, `CheckoutService.java:142` e `Validation.java:72`. Trocar de transportadora obrigaria a mexer nesses três lugares, e não só na peça da TransLog. Por isso o P6 fica como malfeito.

As entregas sem transportadora (a retirada e o motoboy) estão resolvidas com valores fixos na regra de entrega, sem porta nem adaptador. Não há estrutura desnecessária por perto: nenhuma interface no projeto.

## Item a item

**P6, o vocabulário da transportadora fica do lado dela?**

O que fica do lado da TransLog, tudo em `TransLogClient.java`:
- a variável `TRANSLOG_URL` (linha 24);
- a conversão de quilos para gramas, `(int) Math.round(weightKg * 1000)` (linha 29);
- a rota `/v2/quote?zip=%s&weight_g=%d&service=%s` (linha 30) e o limite de 2 segundos (linha 35);
- a recusa de status diferente de 200 (linhas 41–43);
- a leitura de `error` (linha 48) e a conferência de `price_cents` e `eta_hours` (linha 52);
- a conversão de centavos para reais (linhas 56–57) e de horas para dias, arredondando para cima (linhas 59–60);
- a resposta no nosso vocabulário, `ShippingQuote(BigDecimal price, int deliveryDays)` (linhas 21 e 62).

Quem chama passa o peso em quilos (`double weightKg`, linha 23) e recebe o frete em reais e o prazo em dias. Nenhuma estratégia ou regra de fora converte unidade. Busquei `cents`, `hours`, `weight_g`, `grams`, `price_cents`, `eta_hours` e `ZIP_NOT_SERVED` no resto de `src/main` e não achei nada.

O que vaza são os códigos de serviço dela, `"STD"` e `"EXP"`, escolhidos fora do adaptador nos três lugares que o chamam:
- `CheckoutService.java:122`, no cálculo do frete: `String service = req.shippingMethod() == ShippingMethod.ECONOMY ? "STD" : "EXP";`;
- `CheckoutService.java:142`, a mesma linha copiada no cálculo do prazo;
- `Validation.java:72`, a mesma linha na validação da entrega.

O parâmetro `String service` da assinatura (`TransLogClient.java:23`) já é o vocabulário dela exposto na interface do adaptador. O certo seria receber o nosso `ShippingMethod` e traduzir por dentro.

A falta de atendimento do CEP volta como `null` (linha 49) e a queda como `CarrierUnavailableException`. Isso não é vocabulário da TransLog, é só o jeito como o adaptador sinaliza, e não conto como vazamento.

O lado da TransLog existe e cobre as unidades e o formato da API, mas os códigos de serviço dela vazam em três pontos de fora.
Conclusão: malfeito

**P6-exagero, as entregas sem transportadora.** A retirada e o motoboy são valores fixos, escritos direto na regra da entrega:
- frete: `case STORE_PICKUP -> BigDecimal.ZERO;` e `case COURIER -> new BigDecimal("18.00");` (`CheckoutService.java:117-118`);
- prazo: `case STORE_PICKUP -> 1;` e `case COURIER -> 0;` (`CheckoutService.java:137-138`);
- limite de 5 kg do motoboy: um `if` na validação (`Validation.java:66`).

Não há interface de transportadora, nem "cliente" do motoboy, e a retirada não passa pela porta da TransLog. Esses casos nem passam pelo `TransLogClient`. Isso é contenção.

À parte, porque é da dimensão do comportamento e não desta: o adicional de prazo da região (`CheckoutService.java:153`) é somado também à retirada e ao motoboy. O enunciado diz que nessas duas opções o prazo é o da própria opção.
Conclusão: contenção

**E, estrutura sem necessidade fora dos pontos.** Perto do Adapter não há estrutura que nenhum ponto peça. A busca por `interface`, `abstract class`, `implements`, `Builder` e `Factory` em `src/main` não acha nada além do `HttpRequest.newBuilder()` da biblioteca. O `TransLogClient` é uma classe concreta com um método estático, e os repositórios (`ProductRepository`, `CustomerRepository`, `OrderRepository`) também são classes concretas, sem interface de implementação única.
Conclusão: contenção

## O que mudaria primeiro

1. **Fazer o adaptador receber a nossa entrega e traduzir por dentro.** A assinatura passaria a ser algo como `quote(String zipCode, double weightKg, ShippingMethod method)`, e o mapeamento `ECONOMY -> "STD"`, `EXPRESS -> "EXP"` ficaria dentro de `TransLogClient`. Isso fecha o único vazamento e tira três cópias da mesma linha (`CheckoutService.java:122`, `CheckoutService.java:142`, `Validation.java:72`). Com isso, trocar de transportadora mexe só na peça da TransLog.
2. **Fazer uma cotação só por pedido.** Hoje a mesma cotação é pedida até quatro vezes a cada resumo: na validação, no frete, no prazo e de novo no frete cru do FREESHIPPING (`CheckoutService.java:83`). Calcular uma vez e passar o `ShippingQuote` adiante reduziria o acoplamento do cálculo com o adaptador. Isso já beira o desenho do Strategy, mas é aqui que o custo aparece.

## Dúvidas

- O `"STD"`/`"EXP"` entra na régua como exemplo de vazamento ("o `"STD"` escolhido no cálculo do frete"). Dois dos três lugares onde ele aparece são exatamente o cálculo do frete e do prazo. Por isso li como malfeito, mesmo com todas as unidades bem isoladas. Seria acerto se a régua só medisse unidades e campos da resposta, mas ela também cita os códigos de serviço.
- O `null` como resposta de "CEP não atendido" (`TransLogClient.java:49`) é um contrato pobre, mas não carrega termo da TransLog. Não contei como vazamento.
