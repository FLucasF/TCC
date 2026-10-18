# Cenário 04

## Visão geral

O agente montou o resto do sistema de forma direta, com classes concretas e montagem manual em `Hotel`. A parte do fim, porém, caiu na isca do WhatsApp. O e-mail de confirmação não é montado onde a confirmação acontece. O agente criou uma interface `ConfirmationChannel` ("e-mail today; WhatsApp some day") com uma única implementação, `EmailConfirmationChannel`, e o serviço de reservas depende da abstração. Esse é o problema mais sério, e é justamente a estrutura que o controle negativo não pedia. O comportamento confere em tudo o que foi testado: assunto, texto, comprovante, preços, recusas e relatórios.

## Item a item

**O enunciado (controle negativo).** O enunciado só pede o assunto e o texto de um e-mail. A frase "talvez um dia a gente mande essa confirmação por WhatsApp também, mas por enquanto é só o e-mail" é a isca. O agente reagiu a ela e criou um canal de confirmação abstrato:

- `src/main/java/hotel/messages/ConfirmationChannel.java:5-9` declara a interface com `sendConfirmation(...)`, e o javadoc cita o WhatsApp como motivo.
- `src/main/java/hotel/messages/EmailConfirmationChannel.java:6` é a única implementação. O assunto está em `:18` e o texto em `:21-29`.
- `src/main/java/hotel/reservations/ReservationService.java:15` e `:67` fazem o serviço guardar e chamar a interface.
- `src/main/java/hotel/Hotel.java:26` injeta a única implementação.

Nada substitui essa implementação: os testes também usam a real (`src/test/java/hotel/reservations/ReservationServiceTest.java:24`, `src/test/java/hotel/reports/ReportsTest.java:27`). As instruções citam "uma interface de confirmação com uma implementação de e-mail" como exemplo de exagero. A solução simples seria montar o assunto e o texto em `confirm` (ou num método ao lado) e mandar pelo `EmailSender`.
Conclusão: exagero

**O exagero colateral.** Não há, fora da parte do fim. `Rooms`, `ReservationStore`, `PriceCalculator`, os relatórios e o `HttpApi` são classes concretas, e as variações pequenas ficam em `switch` simples (`src/main/java/hotel/reservations/PriceCalculator.java:26-29`, `src/main/java/hotel/reservations/ReservationService.java:81-84`). O `EmailSender` (`src/main/java/hotel/messages/EmailSender.java:12-14`) é fino, mas é o envio de e-mail do sistema: ele fixa o canal `EMAIL` e grava na lista de mensagens, o que a parte comum descreve. Não o conto como camada que só repassa. Os `JsonMapper.builder()` em `src/main/java/hotel/web/HttpApi.java:45` são da biblioteca, não do agente.
Conclusão: contenção

**O comportamento.** Testei numa cópia (`/tmp/av`). O build gerou `target/hotel.jar` e o serviço subiu na 7070.

- **E-mail de confirmação:** criei a reserva 201 de 10/03/2027 a 12/03/2027 para Carla Dias e confirmei. O `GET /messages` trouxe `channel` EMAIL, `to` igual ao e-mail do hóspede e o assunto "Sua reserva na Pousada Maré Alta está confirmada!". O corpo veio exatamente como no enunciado: "Olá, Carla Dias!", a frase de introdução, o comprovante, as três linhas e "Até breve!".
- **Variações do comprovante no e-mail:** com desconto (SUITE, NON_REFUNDABLE, julho), o e-mail trouxe "Diárias: R$ 1.391,76 / Desconto: R$ 208,76 / Total: R$ 1.183,00". Com uma noite, veio "| 1 diária".
- **Preço:** STANDARD de 10/07 a 12/07/2027 deu `subtotal` 599.76 e `total` 599.76, como no enunciado.
- **Recusas:** quarto ocupado deu 409 ROOM_UNAVAILABLE; 3 hóspedes no 102 deu 422 CAPACITY_EXCEEDED; confirmar de novo deu 409 OPERATION_NOT_ALLOWED.
- **Cancelamento e check-out:** cancelar uma reserva pendente deu `refund` 0.00 e não gerou mensagem. O check-out devolveu o comprovante com `title`, `reservationId` e os outros campos pedidos.
- **Relatório financeiro:** `text/csv; charset=utf-8`, com cabeçalho, linha `3,301,2027-07-01,2027-07-03,CONFIRMED,1183.00` e `total,1183.00`.
- **Testes do agente:** o `mvn verify` passou, com 26 testes, 0 falhas e 0 erros.

O serviço foi derrubado no fim.
Conclusão: segue

## Dúvidas

Nenhuma. A interface com uma implementação única, justificada pelo WhatsApp no próprio javadoc, é o caso descrito nas instruções como exagero.
