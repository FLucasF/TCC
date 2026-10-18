# Padrões: Adapter

## Visão geral

O agente isolou a conversa com a TransLog numa classe só, `TransLogClient`. Ela monta a rota `/v2/quote`, converte quilos em gramas, traduz `ECONOMY`/`EXPRESS` para `"STD"`/`"EXP"`, lê `price_cents`/`eta_hours` e devolve um `ShippingResult` em reais e dias. Nenhum termo, campo ou unidade da TransLog aparece fora dessa classe. O resto do código pede a cotação com o peso em quilos e recebe reais e dias, então o ponto que pede o padrão está resolvido.

Os problemas vêm no sentido contrário, com a nossa regra entrando no adaptador, e a régua não chama isso de vazamento:
- **O mais sério:** os dias a mais do Norte e do Nordeste, uma regra da loja, ficam dentro do adaptador, num `switch` sobre a região. Quem trocar de transportadora vai ter de copiar essa regra para a peça nova. O próprio dublê do teste precisa repetir a regra.
- **Os dois sinais são frágeis:** o adaptador avisa "CEP não atendido" devolvendo `null` e avisa "fora do ar" com uma `RuntimeException` cuja mensagem é uma string. O `ShippingCalculator` compara essa string para saber o que houve.
- **Não há interface de transportadora.** A régua aceita isso, mas o teste só consegue trocar a TransLog herdando da classe concreta.

As entregas sem transportadora ficam como valores fixos no próprio cálculo, sem estrutura nenhuma. É a resposta do tamanho do problema.

## Item a item

**P6, o vocabulário da transportadora fica do lado dela?** O lado da TransLog é `src/main/java/com/loja/service/shipping/TransLogClient.java`. Tudo o que é dela está lá:
- a tradução do nosso caso para o código de serviço, `method == ShippingMethod.ECONOMY ? "STD" : "EXP"` (`TransLogClient.java:30`), que é trabalho do adaptador;
- a conversão de quilos para gramas (`:31`);
- a rota e os parâmetros `/v2/quote?zip=%s&weight_g=%d&service=%s` (`:33`);
- a leitura de `"error"`, `price_cents` e `eta_hours` (`:48`, `:52-53`);
- a conversão de centavos para reais (`:55`) e de horas para dias arredondando para cima (`:56`).

A interface pública é `quote(String zipCode, BigDecimal weightKg, ShippingMethod method, Region region)` e devolve `ShippingResult(cost, deliveryDays)` (`ShippingResult.java:4-5`). As duas falam a nossa língua: quilos na entrada, reais e dias na saída.

Fora do adaptador não há vazamento:
- O `ShippingCalculator` calcula o peso em quilos (`ShippingCalculator.java:50-57`) e repassa sem converter (`:30-35`).
- O `CheckoutService` só usa `getCost()` e `getDeliveryDays()` (`CheckoutService.java:75`, `:84-85`).
- A busca por `price_cents`/`priceCents`, `eta_hours`/`etaHours`, `weight_g`, `ZIP_NOT_SERVED`, `"STD"`, `"EXP"`, `/ 24` e `1000` fora do `TransLogClient` só achou o `1000.00` do boleto (`CheckoutService.java:228`), que não tem nada a ver com a TransLog.
- O `ZIP_NOT_SERVED` nem aparece por nome: o adaptador olha `json.has("error")` (`:48`).

Há três ressalvas, nenhuma de vazamento:
1. **A regra das regiões está dentro do adaptador.** O `switch (region) { case NORTH -> 3; case NORTHEAST -> 2; ... }` fica em `TransLogClient.java:58-62`. Isso põe uma regra de negócio da loja dentro da peça que deveria só traduzir. Trocar de transportadora deixaria de "mexer só nessa peça", porque a regra teria de ser copiada. A prova está no dublê do teste, `MockTransLogClient`, que repete a regra: `2 + (region == Region.NORTH ? 3 : 0)` (`src/test/java/com/loja/CheckoutServiceTest.java:101`).
2. **Os sinais de "CEP não atendido" e "fora do ar" são frágeis.** O adaptador devolve `null` (`TransLogClient.java:49`) ou lança `RuntimeException("CARRIER_UNAVAILABLE")` (`:69`), e o chamador decide pela mensagem com `e.getMessage().equals("CARRIER_UNAVAILABLE")` (`ShippingCalculator.java:41`). Os textos são nossos, não da TransLog. Mesmo assim, é um contrato fraco entre as duas peças.
3. **Não há interface.** O `ShippingCalculator` depende da classe concreta (`ShippingCalculator.java:11`), e o teste troca a TransLog por herança (`CheckoutServiceTest.java:93`). A régua diz que o ponto mede o isolamento do vocabulário, com ou sem interface.

Pelo critério da régua, nada do vocabulário da TransLog sai do lado dela.
Conclusão: acerto

**P6-exagero, as entregas sem transportadora.** A retirada é uma linha, `case STORE_PICKUP -> new ShippingResult(BigDecimal.ZERO, 1)` (`ShippingCalculator.java:21`). O motoboy é um `case` com o limite de 5 kg e `new ShippingResult(new BigDecimal("18.00"), 0)` (`:22-27`). Não existe interface de transportadora, nem "cliente" do motoboy, e a retirada não passa pela TransLog. Os dois valores ficam na própria regra da entrega. O `switch` sobre as quatro entregas (`:20`) é assunto da Strategy (P1), não deste item.
Conclusão: contenção

**E, estrutura sem necessidade fora dos pontos.** O pacote de entrega tem só `ShippingCalculator`, `ShippingResult` e `TransLogClient`. A busca por `interface`, `abstract class`, `extends` e `implements` no código de produção só achou a `CouponStrategy` com quatro implementações (o ponto dos cupons, de outro pattern) e a `ApiException extends RuntimeException`. Perto do Adapter não há interface com uma implementação só, nem camada que só repassa, nem fábrica.
Conclusão: contenção

## O que mudaria primeiro

1. **Tirar a regra das regiões do `TransLogClient`.** Ela iria para o `ShippingCalculator`, aplicada à econômica e à expressa depois da cotação, e o adaptador ficaria só com a tradução. Assim, trocar de transportadora mexeria só no adaptador, e o dublê do teste deixaria de repetir uma regra de negócio.
2. **Trocar o `null` e a `RuntimeException` com string por sinais tipados**, como duas exceções próprias ou um resultado que diga "não atendido" e "fora do ar". Uma interface `Carrier` com o `TransLogClient` como implementação ajudaria o teste, que hoje herda da classe concreta.

## Dúvidas

- **A regra das regiões dentro do adaptador.** A régua define o malfeito só como o vocabulário da TransLog saindo do lado dela. O caso aqui é o contrário: a regra da loja entra no adaptador. Considerei que isso não é vazamento e mantive o acerto, deixando o problema registrado no texto. Uma leitura mais estrita, de que o adaptador também precisa ser só tradução, puxaria o item para malfeito.
- **O enunciado.** O `prompt.md` desta execução traz o vocabulário em inglês (`price_cents`, `eta_hours`, `weight_g`, `STD`/`EXP`, `ZIP_NOT_SERVED`), então o ponto mede o que deve medir.
