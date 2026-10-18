# Padrões: Adapter

## Visão geral

O agente criou um lado da TransLog: o pacote `shipping`, com `TransLogAdapter`, que monta a URL `/v2/quote` e faz a chamada HTTP, `ShippingQuote`, que guarda a resposta, e a interface `ShippingCarrier`. Mas esse lado não traduz nada. Ele só repassa a resposta crua, ainda com os nomes da TransLog, e todo o trabalho de tradução ficou no `CheckoutService`:

- a conversão de quilos para gramas;
- a escolha entre `"STD"` e `"EXP"`;
- a leitura do código `"ZIP_NOT_SERVED"`;
- a leitura de `priceCents` e `etaHours`, com a divisão por 100 e por 24.

A própria interface `ShippingCarrier`, que deveria ser a nossa porta, pede o peso em gramas e devolve centavos e horas. Ela não tem como receber a forma de entrega, então o serviço a contorna e usa a classe concreta `TransLogAdapter`. Trocar de transportadora mexeria no `CheckoutService` todo. Nas entregas sem transportadora o agente foi contido: a retirada e o motoboy são valores fixos dentro da regra do frete. Também não há estrutura sobrando perto deste padrão.

## Item a item

**P6, o vocabulário da transportadora fica do lado dela?** O `prompt.md` desta execução traz o vocabulário em inglês da TransLog (`price_cents`, `eta_hours`, `weight_g`, `ZIP_NOT_SERVED`, `STD`/`EXP`), então o ponto se aplica.

O que fica do lado dela, e está certo ali:
- a rota e os parâmetros (`src/main/java/com/loja/shipping/TransLogAdapter.java:24-25`, `/v2/quote?zip=%s&weight_g=%d&service=%s`);
- o limite de 2 segundos (`TransLogAdapter.java:10`, `:28-29`);
- a leitura da resposta em `ShippingQuote` (`TransLogAdapter.java:34`, campos `priceCents`/`etaHours` em `src/main/java/com/loja/shipping/ShippingQuote.java:4-5`).

O que vaza para fora, tudo em `src/main/java/com/loja/service/CheckoutService.java`:
- **a unidade dela, gramas:** `calculateTotalWeight(request.items) * 1000` nas linhas 165 e 242. A conversão é feita no serviço, que já trabalha com a unidade dela;
- **os códigos de serviço:** `request.shippingMethod == ShippingMethod.ECONOMY ? "STD" : "EXP"` na linha 243. Traduzir a nossa entrega para o código dela é trabalho do adaptador, e foi feito no cálculo do frete;
- **o código de erro dela:** `"ZIP_NOT_SERVED".equals(quote.error)` na linha 169;
- **os campos e as unidades da resposta:** `Money.round(quote.priceCents / 100.0)` na linha 251 e `Math.ceil(quote.etaHours / 24.0)` na linha 252. Quem converte centavos em reais e horas em dias é o serviço, não o adaptador;
- **a configuração dela:** `System.getenv("TRANSLOG_URL")` e `new com.loja.shipping.TransLogAdapter(...)` nas linhas 158-164 e 239-240.

A interface não protege o resto do código. `ShippingCarrier.getQuote(String zipCode, Double weightGrams)` (`src/main/java/com/loja/shipping/ShippingCarrier.java:4`) recebe gramas e devolve um `ShippingQuote` em centavos e horas, ou seja, a porta fala a língua da TransLog. Ela também não tem como receber a forma de entrega. Por isso o adaptador fixa `"STD"` em `getQuote` (`TransLogAdapter.java:19`), e o cálculo do frete deixa a interface de lado e chama o método concreto `getQuoteWithService` (`CheckoutService.java:240`, `:245`).

O lado da TransLog existe e faz a chamada HTTP, mas o vocabulário vaza por vários caminhos. Isso se encaixa em "malfeito", e não em "omissão".
Conclusão: malfeito

**P6-exagero, as entregas sem transportadora.** A retirada e o motoboy são valores fixos dentro da regra do frete:
- `case STORE_PICKUP: return new ShippingInfo(0.0, 1);` e `case COURIER: return new ShippingInfo(18.0, 0);` (`CheckoutService.java:228-231`);
- o limite de 5 kg do motoboy é uma conferência direta (`CheckoutService.java:150-155`).

Nenhuma das duas passa por `ShippingCarrier`. O único implementador da interface é o `TransLogAdapter`, e não existe "cliente" do motoboy nem adaptador para devolver número fixo. O `switch` sobre as entregas é assunto da régua do Strategy (P1), não desta.
Conclusão: contenção

**E, estrutura sem necessidade perto do Adapter.** O que existe perto deste padrão é a interface `ShippingCarrier`, com uma implementação só. Ela não conta como E, porque o P6 pede a troca de transportadora e a régua aceita a interface ali. O problema dela, falar em gramas e ser contornada, já está no P6. `ProductProvider` (`src/main/java/com/loja/strategy/ProductProvider.java`) é uma interface funcional usada como lambda pelos cupons. Ela pertence à área do Strategy e não é uma camada que só repassa para a transportadora. Não encontrei builder, fábrica ou camada de repasse ligados à transportadora.
Conclusão: contenção

## O que mudaria primeiro

1. **Levar toda a tradução para dentro do `TransLogAdapter` e mudar a porta.** A porta passaria a ser algo como `quote(zipCode, weightKg, ShippingMethod)`, que devolve frete em reais e prazo em dias, ou um resultado do tipo "CEP não atendido" ou "transportadora fora do ar". O adaptador ficaria com os gramas, o `STD`/`EXP`, o `ZIP_NOT_SERVED` e a divisão por 100 e por 24. Hoje essas regras estão espalhadas por `CheckoutService.java:165-172` e `:242-252`. Assim, trocar de transportadora mexeria só no pacote `shipping`.
2. **Fazer o serviço depender da interface, não da classe concreta.** O `ShippingCarrier` seria criado uma vez, com a `TRANSLOG_URL`, e passado ao `CheckoutService`, em vez de o serviço ler a variável de ambiente e fazer `new TransLogAdapter` duas vezes (`:158-164`, `:239-240`).

## Dúvidas

- `ShippingCarrier` e `ShippingQuote` estão no pacote `shipping`, não citam os nossos casos e falam a língua da TransLog. Pela definição da régua, os contei como lado dela. Por isso não os chamei de vazamento: tratei-os como o sinal de que a porta não traduz nada.
- Fora desta dimensão, e sem ter rodado o serviço: o `ObjectMapper` está na configuração padrão (`TransLogAdapter.java:11`), e os campos `priceCents`/`etaHours` não têm `@JsonProperty`. Pelo padrão do Jackson, uma resposta com `price_cents` falharia por propriedade desconhecida e cairia em `CARRIER_UNAVAILABLE`. Há outros dois pontos: a validação consulta sempre com `"STD"`, inclusive para `EXPRESS` (`TransLogAdapter.java:19`), e uma queda da TransLog no cálculo vira `SHIPPING_METHOD_UNAVAILABLE` (`CheckoutService.java:247-248`). Quem confere isso é a correção do comportamento. Não pesou aqui.
