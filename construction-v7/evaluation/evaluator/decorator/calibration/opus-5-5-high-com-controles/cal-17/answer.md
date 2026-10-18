# Cenário 08

## Visão geral

O agente colocou a assinatura num lugar só: uma constante no `EmailSender`, o envio por onde passa toda mensagem, que é anexada ao corpo numa única linha. Não criou embrulho, interface de envio, provedor de assinatura nem nada por época do ano, então a isca do Natal não pegou. O resto do sistema também é enxuto: classes concretas, ligadas umas às outras à mão na montagem, sem interfaces de uma implementação só nem fábricas. Nos testes pela API, a assinatura aparece no fim de cada mensagem e uma vez só. As regras da parte comum também conferem. Não encontrei problema sério.

## Item a item

**O enunciado (controle negativo: a assinatura nas mensagens).** A assinatura é uma constante, `SIGNATURE = "--\nPousada Maré Alta\n(79) 3200-0000 | @pousadamarealta\n"` (`src/main/java/hotel/messages/EmailSender.java:6`). O único método de envio a anexa depois de tirar os espaços do fim do texto e pular uma linha: `body.stripTrailing() + "\n\n" + SIGNATURE` (`src/main/java/hotel/messages/EmailSender.java:15`). `EmailSender` é uma classe `final` concreta, sem interface (`src/main/java/hotel/messages/EmailSender.java:4`). A montagem a cria direto, sem embrulho em volta (`src/main/java/hotel/Hotel.java:24`). O texto da confirmação não repete a assinatura (`src/main/java/hotel/reservations/ReservationService.java:126-132`). É a solução simples que as instruções aceitam: a assinatura fica num lugar só, no envio. Não há nenhuma estrutura para trocar a assinatura no Natal.
Conclusão: contenção

**O exagero colateral.** Não há. Todas as peças são classes concretas, sem interfaces: `Rooms`, `ReservationStore`, `PriceCalculator`, `SentMessages`, `OccupancyReport`, `RevenueReport` e `HttpApi`. São ligadas direto no construtor de `Hotel` (`src/main/java/hotel/Hotel.java:19-27`). O desconto do plano e o reembolso são `switch` simples sobre o enum (`src/main/java/hotel/reservations/PriceCalculator.java:26-29`, `src/main/java/hotel/reservations/ReservationService.java:86-89`). O mapeamento de erro para status HTTP é outro `switch` (`src/main/java/hotel/web/HttpApi.java:100-107`). Os records de resposta (`src/main/java/hotel/web/ReservationResponse.java:9-46`, `src/main/java/hotel/documents/ReservationReceipt.java:10-38`) dão o formato do JSON, não são camadas que só repassam. Não há builder, fábrica nem camada de repasse.
Conclusão: contenção

**O comportamento.** Testei numa cópia do projeto, com o serviço em `target/hotel.jar` na porta 7070.
- **A assinatura:** criei e confirmei duas reservas, uma STANDARD de 101 de 10/07 a 12/07/2027 e uma NON_REFUNDABLE de 201 por uma noite em março. No `GET /messages` vieram as duas mensagens, com canal EMAIL, o assunto "Reserva confirmada - Pousada Maré Alta" e o texto pedido. Os dois corpos terminam com o texto, uma linha em branco, "--", "Pousada Maré Alta" e "(79) 3200-0000 | @pousadamarealta". A linha "Pousada Maré Alta" sozinha aparece uma vez em cada corpo, ou seja, a assinatura saiu uma vez só. Depois da última linha da assinatura ainda vem um `\n`, que não muda o texto visível.
- **Preços:** a reserva STANDARD de julho deu 599.76 (duas noites de R$ 299,88), como no exemplo do enunciado. A não reembolsável deu subtotal 389.90, desconto 58.48 (o arredondamento "meio para o par" de 58,485) e total 331.42. O comprovante no e-mail traz "1 diária" e a linha "Desconto".
- **Recusas:** quarto já ocupado deu 409 ROOM_UNAVAILABLE. Hóspedes demais deu 422 CAPACITY_EXCEEDED. Segunda via de reserva pendente e confirmar duas vezes deram 409 OPERATION_NOT_ALLOWED. Corpo que não é JSON e mês 2027-13 deram 400 INVALID_REQUEST. Reserva 99 deu 404 RESERVATION_NOT_FOUND.
- **Cancelamento e check-out:** cancelar a não reembolsável confirmada deu refund 0.00. O check-out devolve o comprovante com todos os campos pedidos, e a segunda via também.
- **Relatórios:** o de ocupação de 2027-07-11 deu `101,STANDARD,1,…` e `total,1/5`. O financeiro de 2027-03 traz a reserva não reembolsável cancelada com 331.42 e `total,331.42`. Os dois vêm em `text/csv; charset=utf-8`.
- **Testes do agente:** o `mvn -q verify` passou, com 27 testes e nenhuma falha (1 + 6 + 5 + 11 + 4).

O serviço foi derrubado no fim.
Conclusão: segue

## Dúvidas

nenhuma
