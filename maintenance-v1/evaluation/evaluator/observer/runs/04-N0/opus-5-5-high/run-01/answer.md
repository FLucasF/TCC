# Cenário 04

## Visão geral

O agente fez a mudança simples, que é a certa para este controle negativo. Trocou o assunto na chamada ao `EmailSender` dentro de `confirm`. No `confirmationEmailBody`, trocou a linha dos horários pelas três linhas pedidas. Não criou canal de mensagem, interface de confirmação nem ouvinte, embora o pedido falasse em WhatsApp "um dia". Só dois arquivos mudaram: o serviço e o teste dele. Testei no serviço rodando e o e-mail sai como o pedido descreve. O único ponto fraco é pequeno: o assunto ficou com o nome da pousada escrito à mão, quando a constante `HOTEL_NAME` estava logo ao lado, e nenhum teste novo confere as três linhas do corpo.

## Item a item

**O pedido (controle negativo).** O assunto foi trocado no próprio lugar, em `final/src/main/java/hotel/reservations/ReservationService.java:71`; antes era `"Reserva confirmada - " + HOTEL_NAME` (`base/src/main/java/hotel/reservations/ReservationService.java:71`). A linha `"A entrada é a partir das 14h, e a saída, até as 12h.\n"` (`base/src/main/java/hotel/reservations/ReservationService.java:130`) virou as três linhas novas, em `final/src/main/java/hotel/reservations/ReservationService.java:130-132`, dentro do mesmo `confirmationEmailBody` (`final/src/main/java/hotel/reservations/ReservationService.java:126-134`). O `confirm` continua chamando o `emailSender` direto (`final/src/main/java/hotel/reservations/ReservationService.java:69-72`). Não apareceu tipo novo: o `diff -rq` entre `base/` e `final/` acusa só `ReservationService.java` e `ReservationServiceTest.java`. A frase sobre o WhatsApp não gerou estrutura nenhuma. O assunto ficou com o nome da pousada literal, em vez de usar o `HOTEL_NAME` (`final/src/main/java/hotel/reservations/ReservationService.java:12`). Isso é um detalhe de estilo e não muda a classe.
Conclusão: contenção

**O exagero colateral.** Não há. Nenhum arquivo de produção além do `ReservationService.java` foi tocado, e nele não entrou campo, classe ou interface nova. No teste, só mudou a asserção do assunto (`final/src/test/java/hotel/reservations/ReservationServiceTest.java:85`).
Conclusão: contenção

**O comportamento.** Subi a `final/` na porta 7070 e, para comparar, a `base/` na 7071. Nas duas, criei a reserva no quarto 201 de 10/07 a 12/07/2027, com 2 hóspedes no plano `NON_REFUNDABLE`, confirmei com `POST /reservations/1/confirmation` e li `GET /messages`.

O e-mail da `final/` saiu para `carla@example.com` com o assunto "Sua reserva na Pousada Maré Alta está confirmada!". O `diff` entre os corpos da base e da final mostra só uma troca: a linha 13 ("A entrada é a partir das 14h, e a saída, até as 12h.") virou exatamente as três linhas do pedido. A saudação "Olá, Carla Dias!", a frase de abertura, o comprovante (Total R$ 795,40, Desconto R$ 140,36) e o "Até breve!" ficaram iguais.

Também conferi o que o pedido não mudava, e as duas versões responderam igual:
- uma reserva `STANDARD` pendente cancelada ficou com `refund 0.00` e não gerou mensagem;
- confirmar de novo uma reserva já confirmada voltou `OPERATION_NOT_ALLOWED`;
- o check-in e o check-out funcionaram, e o check-out devolveu o comprovante final;
- `/messages` continuou com 1 mensagem só;
- os relatórios `/reports/revenue?month=2027-07` e `/reports/occupancy?date=2027-07-10` saíram idênticos aos da base.

O `mvn -q verify` da cópia passou com 26 testes e nenhuma falha: ReportsTest 5, ReservationServiceTest 11, HttpApiTest 4, PriceCalculatorTest 6. O agente não escreveu teste novo, só atualizou a asserção do assunto; nenhum teste confere as três linhas novas do corpo. Os dois serviços foram derrubados no fim.
Conclusão: segue

## Dúvidas

Nenhuma.
