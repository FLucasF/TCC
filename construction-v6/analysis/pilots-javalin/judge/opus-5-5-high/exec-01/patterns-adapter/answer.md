# Padrões: Adapter

## Visão geral

A conversa com a TransLog fica numa peça só, `src/main/java/shop/app/TransLogClient.java`. Ela implementa uma porta do domínio, `CarrierGateway` (`src/main/java/shop/domain/CarrierGateway.java:7`). A chamada HTTP, a rota `/v2/quote`, os campos `price_cents` e `eta_hours`, o `ZIP_NOT_SERVED` e as conversões de centavos para reais e de horas para dias ficam todos ali. Esse é o ponto forte. O problema é que a porta foi desenhada no formato da TransLog, e não no nosso. Ela recebe o peso em gramas e um código de serviço em texto (`CarrierGateway.java:19`). Por isso a conversão de quilos para gramas mora no domínio (`ShippingMethod.java:8-10`). Os códigos `"STD"` e `"EXP"` são escolhidos na montagem das regras do domínio (`Rules.java:16-17`). Trocar de transportadora mexeria no domínio, e não só no adaptador. O controle negativo está bem resolvido: a retirada e o motoboy são valores fixos num `ShippingMethod.Fixed` e não passam pela porta de transportadora.

## Item a item

**P6, o vocabulário da transportadora fica do lado dela?** O lado da TransLog existe e é uma peça só, `TransLogClient`. Ele monta a URL com `/v2/quote`, `zip`, `weight_g` e `service` (`src/main/java/shop/app/TransLogClient.java:33-34`). Também aplica o tempo-limite de 2 s (`:20`, `:37`), trata o `ZIP_NOT_SERVED` como "não atende" (`:61-62`) e lê `price_cents` e `eta_hours` (`:66-67`). As unidades dela são convertidas ali mesmo: centavos viram reais com `BigDecimal.valueOf(cents, 2)` (`:72`) e horas viram dias arredondados para cima, `(hours + 23) / 24` (`:73`). Para fora sai um `Quote(cost, days)` em reais e dias (`CarrierGateway.java:8`), e nenhum `priceCents` ou `etaHours` aparece fora do adaptador. Mesmo assim, três coisas do vocabulário dela vazam:
- **a unidade gramas**: a porta do domínio pede `int weightGrams` (`src/main/java/shop/domain/CarrierGateway.java:19`), e a conversão de quilos para gramas fica num record do domínio, `ShippingMethod.Request.weightGrams()` (`src/main/java/shop/domain/ShippingMethod.java:8-10`). Quem a chama é a regra da entrega, em `ShippingMethod.Carrier` (`ShippingMethod.java:49`). É o caso que a régua chama de vazamento: a conversão para gramas feita antes de chamar a transportadora;
- **os códigos de serviço**: `"STD"` e `"EXP"` são escolhidos no domínio, em `Rules.standard` (`src/main/java/shop/domain/Rules.java:16-17`), e passam pela porta como `String service` (`CarrierGateway.java:19`, `ShippingMethod.java:44`). A tradução `EXPRESS -> "EXP"` deveria ficar dentro do adaptador, e ficou fora;
- **o formato da porta**: como a assinatura `quote(zipCode, weightGrams, service)` copia os parâmetros da API da TransLog, outra transportadora teria de aceitar gramas e um código de serviço no padrão da TransLog. Do contrário, a porta e o domínio mudariam junto.

Isso também aparece no dublê de teste: ele implementa a porta como `(zip, grams, service) -> ...` (`src/test/java/shop/TestApp.java:51`). A peça da TransLog existe e isola o protocolo, a resposta e duas das três unidades. As gramas e os códigos `STD` e `EXP` vazam para o domínio.
Conclusão: malfeito

**P6-exagero, as entregas sem transportadora.** A retirada e o motoboy são instâncias de um record genérico com valor e prazo fixos, `ShippingMethod.Fixed` (`src/main/java/shop/domain/ShippingMethod.java:32-41`). Elas são montadas com os próprios números em `Rules.java:18-19`: `Money.ZERO, 1` e `Money.of("18.00"), 0`, com o limite de 5 kg. Não há porta de transportadora, adaptador nem "cliente" do motoboy, e nenhuma das duas passa por `CarrierGateway`. Só `ShippingMethod.Carrier` usa a porta (`ShippingMethod.java:44-56`). O valor e o prazo ficam na própria regra da entrega.
Conclusão: contenção

**E, estrutura sem necessidade fora dos pontos (perto do Adapter).** A única interface com uma implementação de produção perto deste pattern é `CarrierGateway`. Ela é a porta que o P6 pede ("pode ser que um dia a gente troque de transportadora"), e os testes a substituem (`TestApp.java:51`), então não conta aqui. O `Notifier` (`src/main/java/shop/app/Notifier.java:11-15`) é uma classe concreta que grava o aviso no banco, sem interface nem adaptador para um serviço de e-mail que não existe. Há uma duplicação pequena: `CarrierGateway.Quote` (`CarrierGateway.java:8`) e `ShippingMethod.Quote` (`ShippingMethod.java:13`) têm o mesmo formato `(cost, days)`. Mas uma é o resultado da porta e a outra o resultado da regra de entrega, já com os dias de despacho da região (`ShippingMethod.java:54`), e isso não é uma camada que só repassa. Não encontrei estrutura sem necessidade perto deste pattern.
Conclusão: contenção

## O que mudaria primeiro

1. **Mudar a porta para a nossa língua**: `quote(String zipCode, BigDecimal weightKg, ShippingMethod/um tipo nosso de serviço)`. A conversão para gramas (`Math.round` de kg × 1000) e a tradução `ECONOMY -> "STD"` e `EXPRESS -> "EXP"` iriam para dentro de `TransLogClient`. Com isso, o `weightGrams()` sai de `ShippingMethod.Request` e os textos `"STD"` e `"EXP"` saem de `Rules`. Trocar de transportadora passaria a mexer só no adaptador, que é o que o enunciado pede.
2. Tirar o parâmetro `service` em texto da `ShippingMethod.Carrier`. Ela passaria só a própria entrega (ou um enum nosso de nível de serviço), e o adaptador decidiria o código.

## Dúvidas

- Os códigos `"STD"` e `"EXP"` aparecem em `Rules.standard`, que é a configuração das regras e não o cálculo do frete. Um leitor pode achar que é só montagem. Contei como vazamento porque `Rules` está no pacote `shop.domain` e os códigos atravessam a porta como parte do contrato do domínio. Uma segunda transportadora teria de entender `"STD"` e `"EXP"`.
- O adaptador está no pacote `shop.app`, junto dos serviços de aplicação, e não numa pasta própria. Considerei como "lado da TransLog" só o arquivo `TransLogClient.java`. Isso não muda a conclusão: nenhum outro arquivo de `shop.app` cita o vocabulário dela.
