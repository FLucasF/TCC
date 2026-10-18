Já li o código da entrega e da TransLog; segue a correção.

# Padrões: Adapter

## Visão geral

O agente criou um lado da TransLog de verdade. `TransLogGateway` é a única classe que faz a chamada HTTP. Ela lê `price_cents`, `eta_hours` e `ZIP_NOT_SERVED`, converte centavos em reais e horas em dias, e transforma as respostas em exceções próprias, atrás de uma porta (`CarrierGateway`). As entregas sem transportadora ficaram simples, com o valor fixo na própria regra. O problema é que a porta foi desenhada na língua da TransLog: ela recebe o peso em **gramas** e o **código de serviço dela**. Por isso duas coisas vazam para o nosso lado. A estratégia de entrega converte quilos em gramas, e a configuração das entregas escolhe `"STD"`/`"EXP"`. Trocar de transportadora mexeria nessas duas peças além do adaptador.

## Item a item

**P6, o vocabulário da transportadora fica do lado dela?**

O que fica do lado da TransLog, em `src/main/java/br/com/loja/shipping/TransLogGateway.java`:
- a rota `/v2/quote` e os parâmetros `zip`/`weight_g`/`service` (`:34`);
- o timeout de 2 s (`:19`, `:22`, `:36`);
- a leitura de `error`/`ZIP_NOT_SERVED` (`:50-54`) e de `price_cents`/`eta_hours` (`:56-61`);
- a conversão de centavos em reais (`:62`) e de horas em dias, arredondando para cima (`:63`).

O que sai dali para o resto do código é `CarrierQuote(BigDecimal price, int days)` (`CarrierGateway.java:14`), em reais e em dias. Até aqui, segue o padrão.

O que vaza, e por onde:
- **A unidade do peso.** A porta pede `long weightGrams` (`CarrierGateway.java:12`). Por isso `CarrierShipping`, a estratégia de ECONOMY/EXPRESS (ela implementa `ShippingOption` e soma os dias da região), converte quilos em gramas antes de chamar a transportadora: `request.weightKg().movePointRight(3)...` (`CarrierShipping.java:28`). Esse é o caso que a régua dá como exemplo de vazamento.
- **Os códigos de serviço.** A tradução da nossa entrega para o código dela fica fora do adaptador, na configuração que conhece os nossos casos: `new CarrierShipping("ECONOMY", "STD", gateway)` e `new CarrierShipping("EXPRESS", "EXP", gateway)` (`ShippingConfig.java:11`, `:16`). A porta recebe esse `String service` pronto (`CarrierGateway.java:12`). Dentro do adaptador só sobra uma falsa tradução, `service.equals("STD") ? "STD" : "EXP"` (`TransLogGateway.java:35`). Ela devolve o mesmo código e manda como `EXP` qualquer valor que não seja `STD`.

O tratamento de erro está do lado certo. `ZipNotServedException` e `CarrierDownException` são declaradas na porta (`CarrierGateway.java:17-24`), e a estratégia só as traduz para os nossos códigos de erro (`CarrierShipping.java:32-35`).

O lado da TransLog existe e está bem delimitado, mas a unidade (gramas) e os códigos de serviço dela vazam para a estratégia e para a configuração.
Conclusão: malfeito

**P6-exagero, as entregas sem transportadora.** A retirada devolve `new ShippingQuote(Money.ZERO, 1)` direto na própria regra (`StorePickupShipping.java:16-18`). O motoboy confere o limite de 5 kg e devolve `new ShippingQuote(Money.of("18.00"), 0)` (`CourierShipping.java:22-27`). Nenhum dos dois recebe nem usa `CarrierGateway`. Os dois implementam só `ShippingOption`, que é a estratégia das entregas (P1), e não uma porta de transportadora. Não há "cliente" do motoboy, e a retirada não passa pela porta da TransLog.
Conclusão: contenção

**E, estrutura sem necessidade perto do Adapter.** `CarrierGateway` tem uma implementação só (`TransLogGateway.java:17`; nos testes há um stub em `LojaIntegrationTest.java:36`). Mesmo assim, ela é a porta que o próprio P6 justifica ("pode ser que um dia a gente troque de transportadora"), então não é estrutura sem necessidade. `ShippingConfig` (`ShippingConfig.java:9-17`) só cria os dois beans que reaproveitam `CarrierShipping`, e não é uma camada que só repassa. `CarrierQuote` (`CarrierGateway.java:14`) e `ShippingQuote` (`ShippingQuote.java:5`) quase se repetem (valor e dias), mas cada um fica de um lado da porta. Não achei builder, fábrica ou camada sem necessidade perto deste pattern.
Conclusão: contenção

## O que mudaria primeiro

1. **Fazer a porta falar a nossa língua.** A assinatura passaria a ser algo como `quote(String zipCode, BigDecimal weightKg, Region/ShippingMethod ou um enum nosso de nível de serviço)`. A conversão para gramas (`CarrierShipping.java:28`) e a tradução `ECONOMY → "STD"`, `EXPRESS → "EXP"` (`ShippingConfig.java:11,16`) iriam para dentro do `TransLogGateway`. Assim, trocar de transportadora mexe só no adaptador, que é o que o enunciado pede.
2. **Tirar a falsa tradução** de `TransLogGateway.java:35`. Com a tradução de verdade dentro do adaptador, um serviço desconhecido deixa de virar `EXP` sem ninguém perceber.

## Dúvidas

- **De que lado fica cada arquivo.** O código da TransLog divide a pasta `shipping` com as estratégias, e a regra da pasta ("os da mesma pasta que não conhecem os nossos casos") não decide sozinha. `CarrierShipping` não escreve `ECONOMY`/`EXPRESS`, porque recebe o código por parâmetro. Mesmo assim, contei a classe do nosso lado: ela implementa `ShippingOption`, aplica os dias extras da região, e é a "estratégia de entrega" do exemplo da régua. `CarrierGateway` também não cita os nossos casos. Tratei a porta como a fronteira, e a assinatura dela (`weightGrams`, `service`) como a origem do vazamento. Se ela contasse como lado da TransLog, os vazamentos de `CarrierShipping.java:28` e `ShippingConfig.java:11,16` continuariam, e a conclusão seria a mesma.
- **O nome `ZipNotServedException`** (`CarrierGateway.java:17`) lembra o código `ZIP_NOT_SERVED` da TransLog. Não contei como vazamento: "CEP não atendido" é um conceito que o próprio enunciado usa como nosso (o erro `SHIPPING_METHOD_UNAVAILABLE`), e o texto do código dela só é lido dentro do adaptador.
