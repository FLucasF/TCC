# Cenário 01

## Visão geral

O agente montou os cinco planos de tarifa como um Strategy. Há uma interface `RatePlanPolicy` com três métodos: validação, preço e devolução. Cada um dos cinco planos tem a sua classe, e o normal e o não reembolsável também têm. O plano é escolhido num único `EnumMap`. O cálculo do preço e o cancelamento pedem ao plano o que precisam e não citam nenhum plano pelo nome. O resto do sistema é enxuto: serviço, store em memória, relatórios e API, sem interfaces ou fábricas sobrando. Não achei problema sério. Todas as regras do fim do enunciado e os exemplos do financeiro bateram nos testes pela API.

## Item a item

**O enunciado (pede o pattern).** A interface `reservations/RatePlanPolicy.java:7-15` declara `validate`, `price` e `refund`. Cada plano tem a sua implementação, com as próprias regras dentro dela:
- `reservations/StandardPlan.java:6-21`: devolve o que foi pago.
- `reservations/NonRefundablePlan.java:6-23`: 15% de desconto e devolução zero.
- `reservations/CorporatePlan.java:8-32`: o mapa das empresas fica nas linhas 10-13, a recusa do código nas linhas 16-21 e o desconto da empresa na linha 25.
- `reservations/WeekendPackagePlan.java:10-38`: valida sexta a domingo nas linhas 19-26, usa o preço fechado por tipo de quarto nas linhas 12-15 e 29-32 e devolve metade nas linhas 16 e 36.
- `reservations/LongStayPlan.java:8-29`: exige 7 noites ou mais nas linhas 14-18 e dá 10% de desconto na linha 22.

A escolha do plano acontece num ponto só, o `EnumMap` de `reservations/RatePlans.java:9-21`. Quem usa os planos só delega:
- `reservations/PriceCalculator.java:25-29` chama `validate` e depois `price` no plano escolhido. A parte comum (noites e temporada) fica em `PriceCalculator.java:31-56` e não pergunta qual é o plano.
- O cancelamento faz `ratePlans.policyFor(...).refund(reservation)` em `reservations/ReservationService.java:88`.

O `enum` `reservations/RatePlan.java:3-14` só tem os nomes. Não há `switch` nem `if` sobre o plano em lugar nenhum: procurei os nomes dos planos no código e eles só aparecem nas próprias classes, no `enum` e no registro. O `PercentOff` em `reservations/PercentOff.java:10-13` é só uma ajuda de cálculo com a taxa recebida como parâmetro e não decide nada pelo plano. Um plano novo é uma classe, uma constante no `enum` e uma linha no mapa.
Conclusão: acerto

**O exagero colateral.** Não há. Fora dos planos, as classes são concretas e cada uma tem trabalho de verdade: `Rooms`, `ReservationStore`, `ReservationService`, os dois relatórios e `HttpApi`, montadas à mão em `Hotel.java:313-323`. Não achei interface com implementação única, fábrica ou builder próprio. O `JsonMapper.builder()` em `web/HttpApi.java:45` é do Jackson. O mais perto de uma camada que só repassa é o `EmailSender` em `messages/EmailSender.java:199-201`, que chama `SentMessages.add` e apenas fixa o canal `"EMAIL"`. Ele é pequeno e tem um papel no domínio, então não conta como exagero (ver Dúvidas).
Conclusão: contenção

**O comportamento.** Testei numa cópia em `/tmp`, com o jar `target/hotel.jar` na porta 7070, chamando a API com `curl`:

- **Convênio:**
  - Tecnosul no deluxe de 10 a 12/03/2027 dá 779.80 / 93.58 / 686.22, como no enunciado.
  - Agrovale dá desconto de 140.36 e Farmavida (suíte) de 289.95. As duas contas conferem (18% e 25%).
  - Sem `companyCode`, com `ACME` ou com `tecnosul` minúsculo: 400 `INVALID_REQUEST`.
  - Cancelado depois de pago, devolveu 686.22, ou seja, tudo.
- **Pacote de fim de semana:**
  - Deluxe de 12 a 14/03/2027 sai a 820.00 com desconto 0.00.
  - Standard em julho, que é alta temporada, sai a 520.00. A suíte em julho sai a 1200.00. A temporada não entrou, como pedido.
  - Entrada num sábado e sexta com 3 noites: 400 `INVALID_REQUEST`.
  - Pago e cancelado, devolveu 410.00. Pendente e cancelado, devolveu 0.00.
- **Estadia longa:**
  - Standard de 01 a 08/03/2027 dá 1749.30 / 174.93 / 1574.37, como no enunciado.
  - 7 noites em julho dão 2099.16 / 209.92 / 1889.24, com a temporada valendo.
  - 6 noites: 400.
  - Cancelada depois de paga, devolveu tudo.
- **Normal e não reembolsável:**
  - O standard de 10 a 12/07/2027 dá 599.76, o exemplo do enunciado.
  - O não reembolsável num deluxe dá 15% de desconto (116.97) e devolveu 0.00 ao ser cancelado depois de pago. O normal devolveu tudo.
  - Um `companyCode` no plano normal foi ignorado.
- **Resto do sistema:**
  - O e-mail de confirmação tem o assunto e o texto pedidos, com a linha "Desconto" quando há desconto.
  - A segunda via de uma reserva pendente dá 409 `OPERATION_NOT_ALLOWED`. Cancelar duas vezes também dá 409.
  - Os relatórios saem em `text/csv; charset=utf-8`. No financeiro de março, o pacote cancelado conta com 410.00. Em maio, o não reembolsável cancelado conta com 662.83. A ocupação dá `total,1/5`.
  - Os erros vieram certos: corpo inválido e plano desconhecido dão 400, quarto inexistente dá 404 `ROOM_NOT_FOUND`, saída antes da entrada dá 400 e mês inválido dá 400.

O `mvn verify` da cópia passou: 30 testes, nenhuma falha, BUILD SUCCESS. No fim derrubei o serviço.
Conclusão: segue

## Dúvidas

O `EmailSender` (`messages/EmailSender.java:191-202`) quase só repassa para `SentMessages`, mas fixa o canal e dá nome à operação de mandar e-mail. Como é uma classe concreta e não uma abstração, não a contei como exagero. Fora isso, nenhuma.
