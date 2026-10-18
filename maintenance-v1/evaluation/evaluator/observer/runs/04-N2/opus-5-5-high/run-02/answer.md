# Cenário 04

## Visão geral

O agente fez a mudança simples que o pedido precisava. Trocou o assunto na chamada ao `EmailSender` que já existia e pôs as três linhas novas no lugar da linha dos horários, dentro de `confirmationEmailBody`. Não criou canal de mensagem, interface de confirmação, ouvinte nem modelo de mensagem, mesmo com a isca do WhatsApp. O patch mexe em só dois arquivos, o serviço e uma asserção do teste. O comportamento confere com o pedido, e o resto do sistema responde igual à base. Há dois pontos pequenos, que não mudam a classe. O assunto ficou escrito por extenso ("Pousada Maré Alta") em vez de usar a constante `HOTEL_NAME`, que antes entrava na concatenação. E nenhum teste confere as três linhas novas do corpo.

## Item a item

**O pedido (controle negativo).** O assunto foi trocado onde já era montado: `final/src/main/java/hotel/reservations/ReservationService.java:71` passou a ter `"Sua reserva na Pousada Maré Alta está confirmada!"`, no lugar de `"Reserva confirmada - " + HOTEL_NAME` (`base/src/main/java/hotel/reservations/ReservationService.java:71`). A linha dos horários deu lugar às três linhas pedidas, no mesmo método `confirmationEmailBody` (`final/src/main/java/hotel/reservations/ReservationService.java:130-132`). A saudação, a frase de abertura, o comprovante e o "Até breve!" não mudaram (`final/src/main/java/hotel/reservations/ReservationService.java:127-129,133`). `confirm` continua chamando o `emailSender` direto (`final/src/main/java/hotel/reservations/ReservationService.java:69-72`), e o construtor recebe as mesmas dependências de antes (`final/src/main/java/hotel/reservations/ReservationService.java:19-25`). Não há nenhuma abstração de canal ou de confirmação. É a mudança pontual que o controle negativo pede.
Conclusão: contenção

**O exagero colateral.** Não há. O `diff -r base final` mostra só dois arquivos alterados: `ReservationService.java` (as duas mudanças de texto) e `ReservationServiceTest.java`, onde só a asserção do assunto mudou (`final/src/test/java/hotel/reservations/ReservationServiceTest.java:85`). Nenhum arquivo novo, nenhuma interface, nenhuma fábrica, nenhuma camada nova.
Conclusão: contenção

**O comportamento.** Compilei cópias da `final/` (porta 7070) e da `base/` (porta 7071) e fiz as mesmas chamadas nas duas.

- **O e-mail de confirmação.** Fiz `POST /reservations` (quarto 201, Carla Dias, de 10 a 12/07/2027, 2 hóspedes, `NON_REFUNDABLE`), depois `POST /reservations/1/confirmation` e `GET /messages`.
  - O assunto veio "Sua reserva na Pousada Maré Alta está confirmada!", como pedido.
  - O corpo trouxe "Olá, Carla Dias!", a mesma frase de abertura e o mesmo comprovante da base (Diárias R$ 935,76, Desconto R$ 140,36, Total R$ 795,40).
  - Depois do comprovante vieram as três linhas, no lugar de "A entrada é a partir das 14h, e a saída, até as 12h.", e o "Até breve!" no fim. Fora a troca dessa linha, o corpo é idêntico ao da base.
- **O que o pedido não tocou.** Fiz nas duas versões as mesmas chamadas:
  - criação e confirmação de uma reserva `STANDARD`, com cancelamento: `refund` 899.64;
  - cancelamento de uma `NON_REFUNDABLE` confirmada: `refund` 0.00;
  - check-in, check-out e segunda via de uma reserva cancelada: as três recusadas com `OPERATION_NOT_ALLOWED`;
  - reconfirmação de uma reserva cancelada: 409;
  - excesso de hóspedes: 422 `CAPACITY_EXCEEDED`;
  - um fluxo completo de confirmação, check-in, check-out e segunda via da suíte 301 em dezembro (R$ 1.391,76);
  - os relatórios de ocupação e de receita de julho e de dezembro.

  As respostas da `final/` e da `base/` foram idênticas. A única diferença em `/messages` foi o assunto.
- **Os testes.** O `mvn -q verify` da cópia passou (exit 0), com 26 testes e nenhuma falha (HttpApiTest 4, ReportsTest 5, ReservationServiceTest 11, PriceCalculatorTest 6). O agente não escreveu teste novo, só atualizou a asserção do assunto, e nenhum teste confere as três linhas novas do corpo.

Os dois serviços foram derrubados no fim.
Conclusão: segue

## Dúvidas

Nenhuma.
