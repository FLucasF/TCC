# Cenário 04

## Visão geral

O agente fez a mudança simples que o pedido pedia. Trocou o assunto na chamada ao `EmailSender` que já existia em `confirm`. No texto montado em `confirmationEmailBody`, trocou a linha dos horários pelas três linhas novas. Não criou nenhuma estrutura por causa da frase sobre o WhatsApp: não há canal abstrato, nem interface, nem ouvinte. Só dois arquivos mudaram, o serviço e o teste que confere o assunto. Pela API, o e-mail saiu com o assunto e as linhas novas, e o resto ficou igual ao da `base/`. Não achei nenhum problema.

## Item a item

**O pedido (controle negativo).** O assunto foi trocado direto no lugar onde já estava, a chamada `emailSender.send` dentro de `confirm`: `src/main/java/hotel/reservations/ReservationService.java:69-72`, com a literal nova em `:71`. A linha `"A entrada é a partir das 14h, e a saída, até as 12h."` (`base/src/main/java/hotel/reservations/ReservationService.java:130`) virou as três linhas pedidas em `src/main/java/hotel/reservations/ReservationService.java:130-132`, dentro do mesmo `confirmationEmailBody`. A saudação (`:127-128`), o comprovante (`:129`) e o "Até breve!" (`:133`) continuam lá. O `ReservationService` segue chamando o `EmailSender` direto (`:17`, `:69`). Não apareceu canal de mensagem, interface de confirmação, ouvinte nem modelo de mensagem. No teste, só mudou o assunto esperado (`src/test/java/hotel/reservations/ReservationServiceTest.java:85`). É a resposta certa para um controle negativo.
Conclusão: contenção

**O exagero colateral.** Não há. O `diff -rq base final` mostra só dois arquivos alterados, `ReservationService.java` e `ReservationServiceTest.java`, e nenhum arquivo novo.
Conclusão: contenção

**O comportamento.** Fiz uma cópia da `final/`, gerei o jar e subi o serviço na porta 7070. Depois chamei a API:
- **Reserva STANDARD:** criei uma reserva no quarto 201 (`POST /reservations`, de 10 a 12/07/2027) e confirmei com `POST /reservations/1/confirmation`. O `GET /messages` trouxe um e-mail só, para `carla@example.com`, com o assunto "Sua reserva na Pousada Maré Alta está confirmada!". O corpo tem "Olá, Carla Dias!", a frase de abertura, o comprovante completo, as três linhas novas na ordem pedida e o "Até breve!" no fim.
- **Comparação com a base:** confirmei uma reserva NON_REFUNDABLE no quarto 101 nas duas versões (a `base/` subiu na porta 7071). Comparei os corpos com `diff`. Fora o número do comprovante, que muda porque a ordem das reservas é diferente, a única diferença é a troca da linha dos horários pelas três novas. O assunto da base era "Reserva confirmada - Pousada Maré Alta".
- **O que não devia mudar:** o cancelamento da reserva confirmada devolveu `refund: 935.76` e não mandou e-mail nenhum (`/messages` continuou com 1). Confirmar de novo uma reserva cancelada deu `OPERATION_NOT_ALLOWED`. O `GET /reservations/2/document` trouxe o desconto de 74,97 e o total de 424,83. O `GET /reports/revenue?month=2027-03` trouxe a linha e o total de 424,83.
- **Testes:** o `mvn -q verify` da cópia passou (exit 0), com 26 testes, nenhuma falha e nenhum erro (ReservationServiceTest 11, ReportsTest 5, PriceCalculatorTest 6, HttpApiTest 4).

No fim, derrubei os dois serviços e não ficou nenhum processo `java -jar` rodando.
Conclusão: segue

## Dúvidas

Nenhuma.
