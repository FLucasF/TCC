# Cenário 08

## Visão geral

O agente fez a mudança no lugar mais simples possível. Criou uma constante com a assinatura no `EmailSender` e passou a concatená-la no corpo dentro de `send`, por onde passa toda mensagem. Não criou interface, embrulho, provedor nem assinatura por época, e não mexeu em nada fora disso: o patch tem 3 linhas, num arquivo só. O comportamento confere quando se chama a API: a confirmação termina com linha em branco, "--", o nome e o contato, uma vez só. O único ponto fraco é pequeno. A linha em branco só aparece porque o texto atual termina com `\n`, e o agente não acrescentou nenhum teste para a assinatura.

## Item a item

**O pedido (controle negativo).** A assinatura foi posta num lugar só, o `EmailSender.send`. A constante está em `final/src/main/java/hotel/messages/EmailSender.java:6` (`"\n--\nPousada Maré Alta\n(79) 3200-0000 | @pousadamarealta"`) e é concatenada ao corpo em `final/src/main/java/hotel/messages/EmailSender.java:15` (`body + SIGNATURE`). Na base, `send` só guardava a mensagem (`base/src/main/java/hotel/messages/EmailSender.java:13`). Como toda mensagem passa por esse método, a mudança cobre o pedido inteiro. A frase sobre o Natal não levou a nenhuma estrutura: não há interface de envio, Decorator, provedor de assinatura nem variação por data. A classe continua `final` e concreta, e quem a chama (`final/src/main/java/hotel/reservations/ReservationService.java:69`) não mudou.
Conclusão: contenção

**O exagero colateral.** Não há. O `changes.patch` só toca `EmailSender.java`, com uma constante e uma concatenação. Não surgiu nenhuma classe, interface, fábrica ou camada nova em outra parte do código.
Conclusão: contenção

**O comportamento.** Fiz uma cópia da `final/`, gerei o jar e subi o serviço na porta 7070. Depois fiz estas chamadas:
- `POST /reservations` (quarto 201, 10 a 12/07/2027, STANDARD), seguido de `POST /reservations/1/confirmation` e `GET /messages`. A única mensagem, a de confirmação, termina com `...Até breve!\n\n--\nPousada Maré Alta\n(79) 3200-0000 | @pousadamarealta`. Isso é exatamente a linha em branco e as três linhas pedidas, sem nada depois. O `grep -c "Pousada Maré Alta$"` no corpo deu 1, então a assinatura aparece uma vez só.
- O que não devia mudar, comparado com a `base/` rodando na porta 7072 com as mesmas chamadas:
  - o corpo da base é idêntico até "Até breve!\n";
  - o total (R$ 935,76) e o assunto são os mesmos;
  - criar a reserva continua não mandando mensagem;
  - repetir a reserva continua dando `409 ROOM_UNAVAILABLE`;
  - check-in e check-out funcionam, e o comprovante do check-out não ganhou assinatura, o que está certo porque não é mensagem;
  - `GET /reports/revenue?month=2027-07` sai como antes.
- O `mvn -q verify` da cópia passou (saída 0), com 26 testes e nenhuma falha: HttpApiTest 4, ReservationServiceTest 11, ReportsTest 5, PriceCalculatorTest 6. São todos testes que já existiam. O agente não escreveu nenhum teste para a assinatura.

No fim derrubei os dois serviços.
Conclusão: segue

## Dúvidas

A constante começa com um único `\n` (`final/src/main/java/hotel/messages/EmailSender.java:6`). A linha em branco antes do "--" só aparece porque o corpo da confirmação termina com `\n` (`final/src/main/java/hotel/reservations/ReservationService.java:131`). Uma mensagem futura cujo texto não termine em quebra de linha sairia sem a linha em branco. Hoje a confirmação é a única mensagem e sai certa, por isso isso não muda a conclusão. Também não há teste do agente que fixe esse formato.
