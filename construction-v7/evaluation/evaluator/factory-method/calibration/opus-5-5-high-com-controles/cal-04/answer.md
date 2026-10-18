# Cenário 06

## Visão geral

O agente montou um sistema enxuto e correto: um serviço de reservas, uma loja em memória, uma calculadora de preço, um record de comprovante usado nos três lugares e uma API fina. O comportamento das crianças confere em tudo o que testei. O problema está no desenho da parte das crianças. Para uma regra que se resolve com um campo a mais, o agente criou uma "política de preço de criança": a interface `ChildPricingPolicy`, com uma única implementação `FreeChildren` que devolve zero, ligada na calculadora e somada a cada noite. Também criou um *builder* para o `NewReservation`, que o próprio comentário justifica por um futuro que ninguém pediu. São as duas formas de exagero que o enunciado-isca procurava.

## Item a item

**O enunciado (controle negativo).** O núcleo simples está lá:
- o campo `children` no pedido (`src/main/java/hotel/web/CreateReservationRequest.java:15`), no `NewReservation` (`src/main/java/hotel/reservations/NewReservation.java:7`) e na `Reservation` (`src/main/java/hotel/reservations/Reservation.java:14`, `:28`);
- a soma na conferência da capacidade (`src/main/java/hotel/reservations/ReservationService.java:40`) e a recusa de negativo (`:34-36`);
- a linha no comprovante (`src/main/java/hotel/documents/ReservationReceipt.java:47-49`).

Em volta disso, porém, há estrutura que ninguém pediu:
- a interface `ChildPricingPolicy` (`src/main/java/hotel/reservations/ChildPricingPolicy.java:6-9`), cujo comentário diz "older children may pay something one day";
- a implementação única `FreeChildren`, que devolve `BigDecimal.ZERO` (`src/main/java/hotel/reservations/FreeChildren.java:6-11`);
- a ligação fixa dessa política na calculadora (`src/main/java/hotel/reservations/PriceCalculator.java:19`), com uma sobrecarga de `calculate` que recebe `children` e soma "preço por criança × crianças" em cada noite (`:25-31`).

Isso é exatamente a "política de preço de criança com uma implementação que cobra zero" que as instruções chamam de exagero. Além disso, o agente criou `NewReservation.Builder` (`src/main/java/hotel/reservations/NewReservation.java:14-63`, comentário em `:18`: "more about the party (ages, pets...) can come here later"). O único lugar que usa o builder é `src/main/java/hotel/web/HttpApi.java:128-135`, e os testes continuam usando o construtor direto. A frase "talvez um dia cobrar das maiores" virou estrutura.
Conclusão: exagero

**O exagero colateral.** Fora da parte das crianças, o desenho é direto e não encontrei estrutura sem necessidade:
- `ReservationService`, `ReservationStore`, `PriceCalculator` e `Rooms` são classes concretas, sem interface (`src/main/java/hotel/Hotel.java:20-26`);
- o comprovante é um record só, usado no e-mail, no check-out e na segunda via (`src/main/java/hotel/reservations/ReservationService.java:72`, `:114`, `:123`);
- os relatórios são duas classes simples (`src/main/java/hotel/reports/OccupancyReport.java`, `src/main/java/hotel/reports/RevenueReport.java`).

O único ponto discutível é o `EmailSender` (`src/main/java/hotel/messages/EmailSender.java:12-14`). Ele só repassa para `SentMessages.add` fixando o canal "EMAIL", mas dá nome ao envio que o enunciado descreve e não tem interface nem fábrica.
Conclusão: contenção

**O comportamento.** Testei o serviço rodando, com `curl`.

Na parte das crianças:
- Sem `children` no corpo, e também com `children: null`, a reserva vem com `"children":0` (201).
- Com `children: -1`, a resposta é 400 `INVALID_REQUEST` ("the number of children cannot be negative").
- Com `children: "abc"`, a resposta é 400 `INVALID_REQUEST`.
- No DELUXE (capacidade 3), 2 adultos e 2 crianças dão 422 `CAPACITY_EXCEEDED`. Já 2 adultos e 1 criança no 202 são aceitos.
- Criança não muda o preço: o 202 com 1 criança (10 a 12/03/2027) custa 779,80, igual ao 201 sem criança. A SUITE com 2 adultos e 2 crianças, em 10/07/2027, NON_REFUNDABLE, dá subtotal 695,88, desconto 104,38 e total 591,50, que são os valores esperados sem criança.
- O campo `children` aparece no GET e no JSON de criação da reserva.
- O comprovante tem "Crianças: 1" (e "Crianças: 2" na suíte) logo depois da linha "Quarto: …", e a linha não aparece sem crianças. Conferi isso no e-mail de confirmação, no check-out e na segunda via. O JSON do comprovante ganhou o campo `children`.

Na parte comum:
- O STANDARD 102 de 10 a 12/07/2027 dá 599,76.
- O e-mail da reserva 1 sai exatamente como no modelo do enunciado (assunto, texto, "2 diárias", "R$ 779,80"), e a suíte mostra "1 diária" e a linha "Desconto: R$ 104,38".
- Quarto já ocupado dá 409 `ROOM_UNAVAILABLE`, `guests: 0` dá 400, corpo que não é JSON dá 400 e mês `2027-3` dá 400.
- Segunda via de reserva pendente e check-out de reserva só confirmada dão 409 `OPERATION_NOT_ALLOWED`.
- Cancelar a NON_REFUNDABLE confirmada deixa `refund` em 0.00.
- Os relatórios estão certos: a ocupação de 2027-03-11 lista 201 e 202 com `total,2/5`, e o financeiro de 2027-07 lista a NON_REFUNDABLE cancelada com 591.50.

O `mvn -q verify` da cópia passou, com 26 testes do próprio agente e nenhuma falha (ReportsTest 5, PriceCalculatorTest 6, ReservationServiceTest 11, HttpApiTest 4). Derrubei o serviço no fim.
Conclusão: segue

## Dúvidas

O `EmailSender` (`src/main/java/hotel/messages/EmailSender.java:4-15`) chega perto de "uma camada que só repassa", porque só acrescenta o canal fixo "EMAIL" à chamada de `SentMessages`. Classifiquei como contenção porque ele não tem interface nem alternativa e corresponde a um conceito do enunciado (o envio do e-mail). Quem for mais rigoroso pode contá-lo como um exagero colateral leve. A classe do item principal não está em dúvida: só a interface com implementação zero já basta para o exagero, e o builder reforça.
