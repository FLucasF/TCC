# Cenário 01

## Visão geral

O agente montou um sistema simples e bem dividido: quartos, reservas, comprovante, mensagens e relatórios, cada um no seu pacote, sem camadas a mais. Tudo o que testei funciona como o enunciado pede. O problema está no desenho dos planos de tarifa, que não usa o Strategy. `RatePlan` é um `enum` só com os nomes. O preço de cada plano é decidido por `if`/`switch` no `PriceCalculator`, a devolução por um `switch` no `ReservationService.cancel`, e a regra de quem pode usar cada plano por três `if` no `ReservationService.create`. Um plano novo, como o da melhor idade ou o de 60 dias de antecedência, obriga a mexer em pelo menos três lugares, em duas classes.

## Item a item

**O enunciado (pede o pattern).** Não existe uma estrutura por plano.
- `src/main/java/hotel/reservations/RatePlan.java:3-14` é um `enum` com cinco constantes sem corpo; só os comentários as descrevem.
- No preço, `src/main/java/hotel/reservations/PriceCalculator.java:32-39` testa `ratePlan == RatePlan.WEEKEND_PACKAGE` e, dentro desse `if`, um `switch (room.type())` dá o preço fechado. Em seguida, `PriceCalculator.java:40-46` faz um `switch (ratePlan)` sobre os cinco planos para calcular o desconto. As taxas e a lista de empresas ficam como constantes soltas do calculador (`PriceCalculator.java:18-23`).
- Na devolução, `src/main/java/hotel/reservations/ReservationService.java:105-109` faz outro `switch (reservation.ratePlan())`.
- Nas regras de quem pode usar o plano, `ReservationService.java:51-56` (código da empresa), `ReservationService.java:57-62` (sexta a domingo) e `ReservationService.java:63-66` (7 noites) são `if` sobre o plano dentro do serviço.

O comportamento dos cinco planos, o normal e o não reembolsável inclusive, fica decidido por `switch`/`if` em três lugares. É exatamente o caso que as instruções chamam de omissão.
Conclusão: omissão

**O exagero colateral.** Não encontrei interface com uma implementação só, fábrica, builder nem camada que só repassa a chamada.
- `Rooms`, `ReservationStore`, `SentMessages`, `OccupancyReport` e `RevenueReport` são classes concretas com trabalho de verdade.
- `Hotel.java:19-28` monta as peças à mão.
- O único ponto que chega perto é `src/main/java/hotel/messages/EmailSender.java:12-14`, que só chama `sentMessages.add("EMAIL", ...)`. Mas ele fixa o canal e dá nome ao envio de e-mail, que o enunciado diz que um dia vai sair de verdade. É uma classe concreta e pequena, não uma camada à toa (ver Dúvidas).
Conclusão: contenção

**O comportamento.** Testei na cópia, com o serviço em `PORT=7070 java -jar target/hotel.jar` (o jar com `Main-Class: hotel.Hotel`), chamando a API com `curl`:

| O que testei | O que veio |
|---|---|
| Convênio Tecnosul, deluxe de 10/03/2027 a 12/03/2027 | 779.80 / 93.58 / 686.22 ✔ |
| Convênio Agrovale, deluxe, mesmas datas | desconto 140.36 (18%) ✔ |
| Convênio Farmavida, suíte, mesmas datas | desconto 289.95 (25%) ✔ |
| Convênio sem código, com `ACME` e com `tecnosul` minúsculo | 400 `INVALID_REQUEST` ✔ |
| Pacote deluxe de 12/03 a 14/03 | 820.00, desconto 0.00 ✔ |
| Pacote standard em julho (alta temporada) | 520.00 ✔ |
| Pacote na suíte | 1200.00 ✔ |
| Pacote começando no sábado, ou de 3 noites | 400 ✔ |
| Pacote pago e cancelado | refund 410.00 ✔ |
| Pacote pendente e cancelado | refund 0.00 ✔ |
| Estadia longa standard de 01/03 a 08/03 | 1749.30 / 174.93 / 1574.37 ✔ |
| Estadia longa de 25/02 a 04/03 (4 noites de temporada) | 1949.22 / 194.92 / 1754.30 ✔ |
| Estadia longa de 6 noites | 400 ✔ |
| Convênio e estadia longa cancelados depois de pagos | devolvem tudo ✔ |
| Não reembolsável cancelado depois de pago | devolve 0.00 ✔ e continua no financeiro de abril (662.83) ✔ |
| `companyCode` num plano STANDARD | ignorado ✔ |

Na parte comum:
- STANDARD de 10/07 a 12/07 dá 599.76 ✔.
- O e-mail de confirmação sai com o assunto e o texto pedidos e mostra a linha "Desconto" quando há desconto ✔.
- O comprovante sai no check-out e a segunda via só para reserva paga; uma reserva pendente recebe 409 ✔.
- A ocupação vem em `text/csv; charset=utf-8` e fecha com `total,1/5` ✔; o financeiro de março conta só os R$ 410,00 do pacote cancelado ✔.
- Os erros 404, 409 (quarto ocupado), 422, 400 (plano desconhecido) e 400 (mês inválido) conferem ✔.

O `mvn verify` da cópia passou com 26 testes, 0 falhas e 0 erros (BUILD SUCCESS).
Conclusão: segue

## Dúvidas

- A ordem das validações em `ReservationService.create` faz a disponibilidade do quarto (`ReservationService.java:44-50`) ser checada antes das regras do plano (`ReservationService.java:51-66`). Um pedido de pacote em data errada, para um quarto já ocupado, recebe 409 `ROOM_UNAVAILABLE` em vez de 400. O enunciado não diz qual erro vem primeiro, então não contei isso contra o comportamento.
- `EmailSender` (`messages/EmailSender.java:12-14`) quase só repassa para `SentMessages`. Considerei que ele tem razão de existir (fixa o canal e é o ponto em que o envio real vai entrar) e por isso não o contei como exagero, mas é uma leitura discutível.
