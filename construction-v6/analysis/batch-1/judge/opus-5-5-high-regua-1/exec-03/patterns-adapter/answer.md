Terminei de ler o código da TransLog e quem o chama. Segue a correção.

# Padrões: Adapter

## Visão geral

A conversa com a TransLog fica numa classe só, `TransLogClient`. A metade da resposta está bem isolada: o JSON dela, os centavos, as horas e o `ZIP_NOT_SERVED` são traduzidos ali dentro para reais e dias. A metade do pedido não está. É o `OrderService` que escolhe o código de serviço dela (`"STD"`/`"EXP"`) e converte o peso de quilos para gramas. O próprio método do adaptador pede esses dois dados na língua da TransLog (`int weightGrams, String service`). Por isso, trocar de transportadora mexeria também no `OrderService`. Esse é o problema mais sério. Há um resquício menor: uma conversão de horas para dias num utilitário genérico, que nenhum código de produção usa. A retirada e o motoboy estão do tamanho certo: valores fixos, sem porta nem adaptador.

## Item a item

**P6, o vocabulário da transportadora fica do lado dela?** O lado da TransLog é `src/main/java/com/loja/order/service/TransLogClient.java`, com o record `QuoteResponse` e as exceções aninhadas. Os outros arquivos da pasta (`OrderService`, `PricingService`) conhecem os nossos casos, então ficam fora.

O que fica do lado dela:
- a rota e os parâmetros (`/v2/quote?zip=%s&weight_g=%d&service=%s`, `TransLogClient.java:38`);
- a leitura de `error`, `price_cents` e `eta_hours` (`TransLogClient.java:56-65`);
- a conversão de centavos para reais (`TransLogClient.java:67-71`) e de horas para dias, arredondando para cima (`TransLogClient.java:73`);
- a resposta já na nossa língua: `QuoteResponse(BigDecimal price, int etaDays)` (`TransLogClient.java:31`). Quem chama lê só `quote.price()` e `quote.etaDays()` (`OrderService.java:86-87`).

O que vaza, e por onde:
- **O código de serviço dela fora do adaptador.** `String service = shippingMethod == ShippingMethod.ECONOMY ? "STD" : "EXP";` (`OrderService.java:80`). A tradução `ECONOMY -> "STD"` é trabalho do adaptador, e está no serviço de pedidos.
- **A unidade dela fora do adaptador.** `totalWeight.intValue() * 1000` (`OrderService.java:83`) converte os quilos em gramas antes de chamar a transportadora. É exatamente o caso que a régua dá como vazamento. A conta ainda trunca os quilos antes de multiplicar (1,80 kg vira 1000 g), mas isso é assunto da dimensão do comportamento.
- **A fronteira desenhada na língua dela.** A assinatura `quote(String zipCode, int weightGrams, String service)` (`TransLogClient.java:33`) obriga quem chama a falar gramas e códigos de serviço. Não há como chamar o adaptador com o peso em quilos e a nossa forma de entrega.
- **Menor:** `RoundingUtil.ceilDays(double hours)`, que faz `hours / 24.0` (`util/RoundingUtil.java:11-12`), é a unidade dela num utilitário genérico. Nenhum código de produção chama esse método: só `RoundingUtil.java:11` e o teste `RoundingUtilTest.java:19-23`. É código morto, sem efeito no acoplamento.

O nome do lado da TransLog também aparece em quem chama: o campo `TransLogClient transLogClient` (`OrderService.java:22`) e os `catch` de `TransLogClient.ZipCodeNotServedException` e `TransLogClient.TransLogUnavailableException` (`OrderService.java:88-90`). A falta de uma interface, sozinha, a régua não cobra. Aqui ela pesa porque junta forças com os vazamentos acima: a classe concreta e a língua dela chegam até o serviço de pedidos.
Conclusão: omissão

**P6-exagero, as entregas sem transportadora.** A retirada (grátis, 1 dia) e o motoboy (R$ 18,00, 0 dias) são valores fixos escritos direto onde se calcula a entrega (`OrderService.java:73-77`). Não há porta de transportadora, nem "cliente" do motoboy, e a retirada não passa pelo `TransLogClient`, que só é chamado no `else` de ECONOMY e EXPRESS (`OrderService.java:78-85`). O `if`/`else` sobre as formas de entrega é assunto do Strategy, não deste controle.
Conclusão: contenção

**E, estrutura sem necessidade fora dos pontos (perto do Adapter).** Perto da integração não há estrutura a mais: nenhuma interface com uma implementação só, nenhuma fábrica de cliente HTTP, nenhuma camada que só repassa. O `TransLogClient` é uma classe concreta só (`TransLogClient.java:14-15`). O `JacksonConfig` (`config/JacksonConfig.java:10-15`) só configura o `ObjectMapper`, e não é uma camada.
Conclusão: contenção

## O que mudaria primeiro

1. **Mudar a assinatura do adaptador para a nossa língua:** algo como `quote(String zipCode, BigDecimal weightKg, ShippingMethod method)`, com a conversão para gramas e a tradução `ECONOMY/EXPRESS -> STD/EXP` dentro do `TransLogClient`. Só assim, trocar de transportadora deixa de mexer no `OrderService:80-83`.
2. **Traduzir as exceções para termos nossos,** por exemplo "entrega indisponível" e "transportadora fora do ar", declaradas fora da classe da TransLog. De preferência, colocar uma porta de transportadora na frente. Assim, o `OrderService` deixa de citar `TransLogClient` nos `catch` e no campo.

## Dúvidas

- **Omissão ou malfeito no P6.** O adaptador existe e isola toda a resposta. Lido pela definição geral, o caso parece **malfeito**: a estrutura existe, mas não cobre tudo. Fiquei com **omissão** porque a régua do Adapter diz que qualquer termo ou unidade da TransLog fora do lado dela é vazamento, e que o vazamento é a omissão deste ponto. Aqui vazam duas coisas, o código de serviço e as gramas.
- **`ZipCodeNotServedException`.** O nome imita o `ZIP_NOT_SERVED` da TransLog, mas "CEP não atendido" também é um conceito do nosso negócio. Ele está declarado do lado dela (`TransLogClient.java:82`), então não o contei como vazamento. O que pesa é o `catch` dele no `OrderService:88`, já citado como acoplamento.
- **`RoundingUtil.ceilDays`.** É a unidade dela fora do lado dela, mas não tem uso em produção. Registrei como resquício, e não como a base da conclusão.
