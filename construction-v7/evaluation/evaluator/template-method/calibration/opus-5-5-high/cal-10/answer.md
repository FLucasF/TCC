# Cenário 10

## Visão geral

O agente montou o sistema com classes concretas, uma por responsabilidade (preço, ciclo de vida da reserva, comprovante, mensagens, dois relatórios), sem interfaces nem camadas que só repassam. A coluna `guest` foi posta direto no `OccupancyReport`, com a troca da vírgula por espaço numa linha só. O `RevenueReport` ficou como estava, e não surgiu esqueleto de relatório nem sistema de colunas por causa da frase sobre o telefone. Essa é a resposta simples que o controle negativo pede. Nos testes pela API, tudo o que conferi saiu como o enunciado manda. Não achei problema sério. O único ponto fraco é que os testes do próprio agente não cobrem um nome com vírgula.

## Item a item

**O enunciado (controle negativo: o nome do hóspede no relatório de ocupação).** A coluna está no cabeçalho, logo depois de `reservation`: `room,type,reservation,guest,check_in,check_out` (`src/main/java/hotel/reports/OccupancyReport.java:34`). O valor sai do próprio relatório, com `reservation.guest().name().replace(',', ' ')` (`src/main/java/hotel/reports/OccupancyReport.java:40`), entre as colunas da reserva e da entrada (`:39-41`). O relatório financeiro continua uma classe à parte, escrita inteira e sem mudança (`src/main/java/hotel/reports/RevenueReport.java:23-48`). Não há classe base de relatório, interface, objeto-coluna nem nada guardado para o telefone: `Guest` é só um record com nome, e-mail e CPF (`src/main/java/hotel/reservations/Guest.java:3`). A frase "talvez um dia" não gerou estrutura nenhuma.
Conclusão: contenção

**O exagero colateral.** Não há. As peças são classes `final` concretas, cada uma com uma implementação só e sem interface na frente:
- `PriceCalculator` (`src/main/java/hotel/reservations/PriceCalculator.java:14`);
- `EmailSender`, que só grava em `SentMessages` (`src/main/java/hotel/messages/EmailSender.java:4-14`);
- `ReservationStore` (`src/main/java/hotel/reservations/ReservationStore.java:10`).

A ligação entre elas é feita à mão, num construtor só (`src/main/java/hotel/Hotel.java:19-27`). O plano de tarifa se resolve com dois `switch` pequenos sobre o enum, um no desconto (`src/main/java/hotel/reservations/PriceCalculator.java:26-29`) e outro na devolução (`src/main/java/hotel/reservations/ReservationService.java:86-89`), o que é adequado para dois casos fixos. `EmailSender` é um embrulho fino, mas tem papel de domínio: fixa o canal `EMAIL` (`src/main/java/hotel/messages/EmailSender.java:13`). Não é uma camada vazia.
Conclusão: contenção

**O comportamento.** Testei uma cópia do projeto (`hotel.jar`, porta 7070) com cinco reservas em julho de 2027.

Relatório de ocupação:
- `GET /reports/occupancy?date=2027-07-10` voltou com 200 e `text/csv; charset=utf-8`. Trouxe o cabeçalho com `guest` depois de `reservation`, as linhas `101,STANDARD,1,Silva  João,2027-07-10,2027-07-12` (o nome era "Silva, João") e `201,DELUXE,2,Carla Dias,...`, e no fim `total,2/5`.
- Ficaram de fora a suíte que sai nesse dia, a reserva pendente e a não reembolsável cancelada, como o enunciado manda.
- Em `date=2027-07-09`, o nome "Ana, Maria, Souza" saiu como `Ana  Maria  Souza`. A ordem por quarto está certa.
- O espaço duplo vem do espaço que já seguia a vírgula. É o que o enunciado pede ao pé da letra: trocar a vírgula por um espaço.

Relatório financeiro:
- `GET /reports/revenue?month=2027-07` saiu na ordem de data de entrada, com a não reembolsável cancelada contando `2548.98`, e `total,5335.90`, que confere com a soma.

Preço:
- O STANDARD do 101, de 10/07 a 12/07, deu `subtotal 599.76`, `total 599.76`, igual ao exemplo do enunciado.
- O NON_REFUNDABLE do 201 deu `935.76 / 140.36 / 795.40`, que confere com 389,90 × 1,2 × 2 e 15% de desconto arredondado meio para o par.

Recusas, todas com o código certo:

| Caso | Resposta |
|---|---|
| Quarto já ocupado | 409 `ROOM_UNAVAILABLE` |
| Hóspedes demais | 422 `CAPACITY_EXCEEDED` |
| Corpo que não é JSON | 400 `INVALID_REQUEST` |
| Campo faltando | 400 `INVALID_REQUEST` |
| Saída igual à entrada | 400 `INVALID_REQUEST` |
| Data ou mês fora do formato | 400 `INVALID_REQUEST` |
| Quarto inexistente | 404 `ROOM_NOT_FOUND` |
| Reserva inexistente | 404 `RESERVATION_NOT_FOUND` |
| Check-in de reserva pendente | 409 `OPERATION_NOT_ALLOWED` |
| Segunda via de reserva pendente | 409 `OPERATION_NOT_ALLOWED` |

Comprovante e e-mail:
- A segunda via e o check-out devolvem o comprovante com todos os campos.
- Em `GET /messages`, o e-mail tem canal `EMAIL`, o assunto certo e o texto com o comprovante, com a linha "Desconto" e o valor escrito como `R$ 1.391,76`.

O `mvn -q verify` da cópia passou: 26 testes, 0 falhas, 0 erros (ReportsTest 5, ReservationServiceTest 11, HttpApiTest 4, PriceCalculatorTest 6). Nenhum desses testes usa nome com vírgula: o hóspede em `src/test/java/hotel/reports/ReportsTest.java:28` é "Bruno Lima". A troca da vírgula só ficou confirmada pelo meu teste manual. Derrubei o serviço no fim.
Conclusão: segue

## Dúvidas

Nenhuma.
