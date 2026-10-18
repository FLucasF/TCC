Terminei de ler o código da TransLog e das entregas. Segue a correção.

# padrões: Adapter

## Visão geral

O agente montou um adaptador de verdade. A chamada HTTP, a rota, os campos da resposta e o `ZIP_NOT_SERVED` ficam todos em `TransLogGateway.java`, que implementa uma porta `CarrierGateway` (`Shipping.java:39-41`). O preço já sai convertido para reais dentro do adaptador (`TransLogGateway.java:184`), e é isso que o desenho tem de melhor. Só que a porta foi recortada na língua da TransLog, não na nossa. Ela recebe o peso em gramas e o código de serviço da TransLog (`Shipping.java:40`) e devolve o prazo em horas (`Shipping.java:30`). Por isso três pedaços do vocabulário dela vazam para o nosso lado:

- a conversão de quilos para gramas fica no contexto da entrega (`Shipping.java:9-10`);
- os códigos `"STD"`/`"EXP"` são escolhidos no registro das formas de entrega (`Shipping.java:111-112`);
- a divisão de horas por 24 fica na estratégia de entrega (`Shipping.java:68`).

A retirada na loja e o motoboy foram resolvidos do jeito simples, com o valor fixo na própria regra.

## Item a item

**P6, o vocabulário da transportadora fica do lado dela?** O lado da TransLog é `TransLogGateway.java`. Lá ficam a montagem de `/v2/quote?zip=…&weight_g=…&service=…` (`TransLogGateway.java:32`), o tempo limite de 2 s (`TransLogGateway.java:18`, `:35`), a leitura de `error`/`ZIP_NOT_SERVED` (`TransLogGateway.java:53-56`), a leitura de `price_cents` e `eta_hours` (`TransLogGateway.java:58-59`) e a conversão de centavos para reais (`BigDecimal.valueOf(price.asLong(), 2)`, `TransLogGateway.java:64`). Nada disso aparece em outro lugar. O resto do cálculo trabalha em quilos (`PricingService.java:43`) e recebe um `ShippingQuote(cost, days)` (`Shipping.java:14`). Os vazamentos:
- **Gramas, a unidade dela:** `ShippingContext.weightGrams()` converte os quilos em gramas (`weightKg × 1000`, arredondado) fora do adaptador (`Shipping.java:9-10`). A porta exige o peso já em gramas (`long weightGrams`, `Shipping.java:40`), e a estratégia passa `ctx.weightGrams()` (`Shipping.java:63`).
- **Horas, a unidade dela:** o resultado da porta, `CarrierQuote(BigDecimal price, int etaHours)`, é declarado em `Shipping.java:30`, no arquivo que conhece os nossos casos, e não do lado da TransLog. `CarrierShippingMethod` lê `q.etaHours()` e faz a conversão `(q.etaHours() + 23) / 24` (`Shipping.java:68`). É justamente o exemplo de vazamento da régua.
- **Os códigos de serviço:** `"STD"` e `"EXP"` são escolhidos em `ShippingMethods.standard` (`Shipping.java:111-112`), guardados na estratégia (`Shipping.java:45`) e passados como `String service` pela porta (`Shipping.java:40`, `:63`). A tradução `EXPRESS -> "EXP"` deveria morar dentro do adaptador.

Existe uma peça só da TransLog, que faz a chamada e lê a resposta. Mas duas unidades e os códigos de serviço dela vazam para a estratégia de entrega e para o contexto.
Conclusão: malfeito

**P6-exagero, as entregas sem transportadora.** A retirada é uma classe com o valor e o prazo fixos, `new ShippingQuote(Money.ZERO, 1)` (`Shipping.java:80-82`). O motoboy faz o mesmo com `new ShippingQuote(new BigDecimal("18.00"), 0)` e o limite de 5 kg na própria regra (`Shipping.java:85-99`). Nenhuma das duas passa pela `CarrierGateway`, e não há um "cliente" do motoboy nem um adaptador que devolva um número fixo. As duas são implementações de `ShippingMethod`, mas essa é a estrutura da variação das entregas (o Strategy), não uma porta de transportadora.
Conclusão: contenção

**E, estrutura sem necessidade perto do Adapter.** A `CarrierGateway` tem uma única implementação em produção (`Main.java:15`), mas os testes a substituem por uma lambda (`src/test/java/shop/TestApp.java:30`), e o enunciado prevê a troca de transportadora (`prompt.md:43`). A interface `Store` (`Store.java:10`) tem duas implementações, `JdbcStore` e `MemoryStore`, e a segunda é usada nos testes (`TestApp.java:25`). O registro `ShippingMethods` (`Shipping.java:102-120`) é a escolha da entrega pelo código e pertence a outro pattern. Não achei camada que só repassa, nem fábrica ou builder desnecessários, perto da integração.
Conclusão: contenção

## O que mudaria primeiro

1. **Recortar a porta na nossa língua.** A porta passaria a ser algo como `quote(String zip, BigDecimal weightKg, ShippingOption option)` e devolveria o frete em reais e o prazo em dias. A conversão para gramas, a tradução `ECONOMY/EXPRESS -> "STD"/"EXP"` e o `ceil(eta_hours / 24)` iriam para dentro de `TransLogGateway`. Hoje, uma transportadora nova que cote em quilos e em dias obrigaria a mexer em `Shipping.java` (`:9-10`, `:68`, `:111-112`), e não só no adaptador.
2. **Levar o record da resposta para o lado da TransLog.** O `CarrierQuote(price, etaHours)` iria para junto do adaptador, ou seria trocado por um `ShippingQuote` já em dias, para que o campo `etaHours` não precise existir fora dele.

## Dúvidas

- Decidi que `Shipping.java` não é do lado da TransLog: todas as classes estão no mesmo pacote `shop`, e esse arquivo conhece os nossos casos (`ECONOMY`, `EXPRESS`, `STORE_PICKUP`, `COURIER`). Com isso, o `CarrierQuote` declarado ali (`Shipping.java:30`) não é o "record da resposta" que a régua permite. E mesmo que fosse, o `etaHours` é lido e convertido na estratégia (`Shipping.java:68`), o que já é vazamento.
- O `prompt.md` desta execução tem o vocabulário em inglês da TransLog (`prompt.md:203-213`), então o ponto se aplica.
