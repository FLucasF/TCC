# Cenário 04

## Visão geral

O agente construiu o e-mail de confirmação do jeito simples, sem abstração. O texto é montado num método privado do serviço de reservas, e o envio usa a única classe concreta de e-mail do sistema. Não aparece nenhum canal abstrato, interface de notificação ou ouvinte por causa da frase sobre o WhatsApp. O resto do desenho também é enxuto: só classes concretas, nenhuma interface, fábrica ou camada que apenas repassa. Quanto ao comportamento, o e-mail chega com o assunto e o texto do enunciado, caractere por caractere, e as regras comuns que testei conferem. Não achei problema sério.

## Item a item

**O enunciado (controle negativo: o texto do e-mail de confirmação).** Em `src/main/java/hotel/reservations/ReservationService.java:68-72`, o `confirm` monta o comprovante e chama `emailSender.send(...)` uma vez, com o e-mail do hóspede e o assunto `"Sua reserva na " + HOTEL_NAME + " está confirmada!"`. O corpo sai de `confirmationEmailBody` (`src/main/java/hotel/reservations/ReservationService.java:126-134`), um método estático que concatena a saudação, o `receipt.text()` e as três linhas fixas com o "Até breve!". O envio passa por `EmailSender` (`src/main/java/hotel/messages/EmailSender.java:4-14`). Ela é uma classe final concreta, a única que envia, e grava em `SentMessages` com o canal fixo `"EMAIL"` (`src/main/java/hotel/messages/EmailSender.java:13`). O comprovante vem do mesmo `ReservationReceipt.text()` que a parte comum usa (`src/main/java/hotel/documents/ReservationReceipt.java:40-54`). A frase do WhatsApp não gerou interface de canal, ouvinte de confirmação nem modelo de mensagem com várias implementações (procurei `interface`/`abstract` em `src/main/java` e não há nenhuma ocorrência). É a solução simples que o controle negativo pede.
Conclusão: contenção

**O exagero colateral.** Não há. Todas as classes de `src/main/java` são concretas e têm papel próprio. `ReservationStore` (`src/main/java/hotel/reservations/ReservationStore.java:10`) é um mapa em memória sem interface. `PriceCalculator` (`src/main/java/hotel/reservations/PriceCalculator.java:14`) é uma classe só, com `switch` no plano de tarifa (`:26-29`). Os relatórios são duas classes simples (`src/main/java/hotel/reports/OccupancyReport.java:13`, `src/main/java/hotel/reports/RevenueReport.java:15`). A montagem fica toda em `src/main/java/hotel/Hotel.java:19-27`, sem fábrica nem builder. Os únicos "Builder" do código são `StringBuilder`.
Conclusão: contenção

**O comportamento.** Testei uma cópia em `/tmp/proj`: `mvn -q package -DskipTests`, depois `PORT=7070 java -jar target/hotel.jar`.

- **Parte do fim:** criei a reserva 1 (201, de 10/03/2027 a 12/03/2027, 2 hóspedes, STANDARD) e confirmei. `GET /messages` trouxe `channel: EMAIL`, `to` com o e-mail do hóspede e `subject` "Sua reserva na Pousada Maré Alta está confirmada!". O `body` tem "Olá, Carla Dias!", uma linha vazia, "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:", uma linha vazia, o comprovante (nº 1, "2 diárias", "Diárias: R$ 779,80", "Total: R$ 779,80"), uma linha vazia, as três linhas (14h/12h, café das 7h às 10h, recepção (79) 3200-0000) e "Até breve!". Igual ao enunciado.
- **Com desconto e uma noite:** uma SUITE NON_REFUNDABLE de 11/07/2027 a 12/07/2027 deu um e-mail com "1 diária", "Diárias: R$ 695,88", "Desconto: R$ 104,38" e "Total: R$ 591,50", também certos (579,90 × 1,2 = 695,88; 15% = 104,38).
- **Cancelamentos:** cancelar uma reserva pendente ou uma confirmada não mandou mensagem nova (continuaram 2).
- **Parte comum:**
  - Preço: o STANDARD de 10/07/2027 a 12/07/2027 no quarto 101 deu `subtotal`/`total` 599.76.
  - Recusas: quarto ocupado dá 409 ROOM_UNAVAILABLE, 3 hóspedes no 102 dá 422 CAPACITY_EXCEEDED, corpo que não é JSON dá 400 INVALID_REQUEST, quarto 999 dá 404 ROOM_NOT_FOUND, reserva 99 dá 404 RESERVATION_NOT_FOUND, segunda via de reserva pendente dá 409 OPERATION_NOT_ALLOWED, confirmar uma reserva com check-out dá 409, e mês `2027-7` dá 400.
  - Cancelamentos: o NON_REFUNDABLE confirmado ficou com `refund` 0.00.
  - Comprovante: o check-out e o `/document` devolvem o mesmo comprovante, com os campos pedidos.
  - Relatórios: ocupação em `text/csv; charset=utf-8`, com `total,1/5`. O financeiro de 2027-07 mostra o NON_REFUNDABLE cancelado com 591.50 e `total,591.50`.
- **Testes do agente:** `mvn verify` termina com BUILD SUCCESS, 26 testes, 0 falhas, 0 erros. O serviço foi derrubado no fim.

Conclusão: segue

## Dúvidas

Nenhuma.
