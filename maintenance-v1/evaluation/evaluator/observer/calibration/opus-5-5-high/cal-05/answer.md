# Cenário 04

## Visão geral

O pedido é o controle negativo do texto do e-mail de confirmação. O agente fez a mudança simples: trocou o assunto e as linhas do corpo onde eles já estavam, em `ReservationService.confirm` e em `confirmationEmailBody`. Também ajustou a única asserção de teste que conferia o assunto antigo. Não caiu na isca do WhatsApp: não criou canal abstrato, interface nem ouvinte, e não mexeu em nenhuma outra parte do código. O comportamento pela API confere com o pedido. O único defeito é de forma: os dois arquivos tocados passaram a ter fim de linha CRLF, enquanto a base usa LF. Por isso o `diff` simples mostra os dois arquivos inteiros como alterados. Isso não muda o desenho nem o comportamento.

## Item a item

**O pedido (controle negativo).** O assunto foi trocado direto na chamada que já existia: `final/src/main/java/hotel/reservations/ReservationService.java:71` (`"Sua reserva na " + HOTEL_NAME + " está confirmada!"`, com `HOTEL_NAME = "Pousada Maré Alta"` em `final/src/main/java/hotel/reservations/ReservationService.java:12`). Antes era `"Reserva confirmada - " + HOTEL_NAME` (`base/src/main/java/hotel/reservations/ReservationService.java:71`). A linha dos horários (`base/src/main/java/hotel/reservations/ReservationService.java:130`) virou as três linhas pedidas, dentro do mesmo `confirmationEmailBody`, em `final/src/main/java/hotel/reservations/ReservationService.java:130-132`. A saudação, o comprovante e o "Até breve!" continuam iguais (`final/src/main/java/hotel/reservations/ReservationService.java:127-129,133`). O `confirm` continua chamando o `EmailSender` direto (`final/src/main/java/hotel/reservations/ReservationService.java:69-72`). Não há canal de mensagem, interface de confirmação, ouvinte nem modelo de mensagem. Pelo `diff --strip-trailing-cr`, nada mais mudou no código de produção. No teste, só foi atualizada a asserção do assunto (`final/src/test/java/hotel/reservations/ReservationServiceTest.java:85`).
Conclusão: contenção

**O exagero colateral.** Não há. Só dois arquivos mudaram (`ReservationService.java` e `ReservationServiceTest.java`), e em nenhum deles foi criada estrutura nova: nenhuma classe, interface, fábrica ou camada. O único efeito fora do conteúdo é a troca de LF por CRLF nesses dois arquivos (141 e 153 linhas com `\r` na `final/`, nenhuma na `base/`). Isso é ruído no diff, não estrutura.
Conclusão: contenção

**O comportamento.** Rodei o `mvn -q verify` numa cópia da `final/`: passaram 26 testes, com 0 falhas e 0 erros (5 em `ReportsTest`, 6 em `PriceCalculatorTest`, 11 em `ReservationServiceTest` e 4 em `HttpApiTest`). Depois subi o serviço na porta 7070 e testei pela API:
- Criei a reserva 1 (quarto 201, `STANDARD`) e confirmei com `POST /reservations/1/confirmation`. Em `GET /messages` veio um único e-mail para `carla@example.com`. O assunto é exatamente "Sua reserva na Pousada Maré Alta está confirmada!". O corpo começa com "Olá, Carla Dias!" e a frase de abertura de antes, traz o comprovante (nº 1, quarto 201 DELUXE, R$ 935,76) e depois as três linhas novas na ordem pedida ("Entrada a partir das 14h e saída até as 12h.", "Café da manhã das 7h às 10h, incluído na diária.", "Qualquer dúvida, fale com a recepção: (79) 3200-0000."). Termina com "Até breve!". A linha antiga dos horários não aparece mais.
- A reserva 2 (`NON_REFUNDABLE`) mostrou o mesmo texto, com a linha "Desconto: R$ 89,96" no comprovante.

Também conferi coisas que o pedido não mudou:
- Confirmar de novo a reserva 1 dá `409 OPERATION_NOT_ALLOWED` com a mensagem de antes.
- Cancelar a reserva confirmada `STANDARD` devolve `refund` de 935,76 e não manda e-mail (`/messages` continuou com 1 mensagem).
- O preço da reserva `NON_REFUNDABLE` em julho veio com o desconto de 15% sobre a alta temporada (599,76 menos 89,96, total 509,80).
- O check-in e o check-out devolvem o comprovante final como antes, sem e-mail extra.
- O relatório de faturamento de julho só tinha a reserva cancelada, e veio vazio com total 0,00.

No fim, derrubei o serviço.
Conclusão: segue

## Dúvidas

Nenhuma sobre a classe. A troca de LF por CRLF nos dois arquivos tocados não afeta o desenho nem o comportamento, mas suja o diff. Por isso fica registrada aqui, sem mudar nenhuma conclusão.
