# Cenário 03

## Visão geral

O pedido descreve uma lista de interessados que vai crescer (governança e financeiro agora, restaurante e transfer depois), e o desenho certo para isso é o Observer. O agente não usou o pattern. Ele pôs os novos e-mails um depois do outro dentro de `ReservationService.confirm` e `ReservationService.cancel`, com os endereços, os assuntos e os textos escritos no próprio serviço, que é o único arquivo de produção que mudou. Não há interface de ouvinte, o e-mail do hóspede continua com chamada direta e a classe `Hotel` não registra ninguém. O comportamento está certo em todos os casos que testei, e os 28 testes passam. O problema mais sério é o desenho: avisar o restaurante ou o transfer vai exigir mais um `emailSender.send` dentro de `confirm`.

## Item a item

**O pedido (pede o pattern).** Em `final/src/main/java/hotel/reservations/ReservationService.java:71-74`, `confirm` continua mandando o e-mail do hóspede direto, como na base. Logo abaixo vêm, à mão, o e-mail da governança (`final/src/main/java/hotel/reservations/ReservationService.java:76-80`) e o do financeiro (`final/src/main/java/hotel/reservations/ReservationService.java:82-85`), com os endereços escritos no código. Em `cancel`, um `if (wasConfirmed)` manda o e-mail da governança (`final/src/main/java/hotel/reservations/ReservationService.java:107-112`), e um `if (refund.signum() > 0)` manda o do financeiro (`final/src/main/java/hotel/reservations/ReservationService.java:114-119`). Os quatro textos novos também ficaram no serviço, como métodos privados (`final/src/main/java/hotel/reservations/ReservationService.java:165-184`). O construtor só recebe o `EmailSender` (`final/src/main/java/hotel/reservations/ReservationService.java:21-27`), não uma lista de ouvintes, e `final/src/main/java/hotel/Hotel.java` ficou igual ao da base. Ou seja, o serviço conhece todos os destinatários e manda os e-mails ele mesmo. É o caso que as instruções chamam de omissão.
Conclusão: omissão

**O exagero colateral.** Não há. Nenhuma classe, interface ou camada nova foi criada, e o único arquivo de produção alterado é o `ReservationService.java`. Há só uma sobra: o `import java.time.LocalDate` não é usado (`final/src/main/java/hotel/reservations/ReservationService.java:9`).
Conclusão: contenção

**O comportamento.** Fiz o teste numa cópia da `final/`. O `mvn -q verify` passou, com 28 testes e 0 falhas (11 em `ReservationServiceTest`, 6 em `PriceCalculatorTest`, 5 em `ReportsTest` e 6 em `HttpApiTest`). Subi o serviço na porta 7070 e chamei a API com `curl`:
- **Confirmação de uma reserva STANDARD.** Criei a reserva 1 (quarto 201, 10/07 a 12/07/2027, 2 hóspedes) e chamei `POST /reservations/1/confirmation`. Saíram três e-mails:
  - o do hóspede, igual ao da base, com o assunto "Reserva confirmada - Pousada Maré Alta", o comprovante e a linha dos horários;
  - o da governança, para `governanca@pousadamarealta.com.br`, com o assunto "Reserva confirmada - quarto 201" e o texto "Entrada: 10/07/2027 / Saída: 12/07/2027 / Hóspedes: 2";
  - o do financeiro, para `financeiro@pousadamarealta.com.br`, com o assunto "Pagamento recebido - reserva 1" e "Valor: R$ 935,76".
- **Cancelamento da reserva 1.** `POST /reservations/1/cancellation` mandou dois e-mails: "Reserva cancelada - quarto 201", com as datas, e "Reembolso - reserva 1", com "Valor: R$ 935,76". O reembolso registrado também foi 935,76.
- **Reserva NON_REFUNDABLE.** Na reserva 2 (quarto 102), a confirmação mandou os três e-mails. O cancelamento devolveu 0,00 e mandou só o e-mail da governança, sem o do financeiro, como o pedido diz.
- **Reserva pendente.** Cancelar a reserva 3, que estava pendente, não mandou nenhum e-mail: o total de mensagens ficou em 9.
- **O que o pedido não mudou continua igual.**
  - Cancelar depois do check-in ainda dá `OPERATION_NOT_ALLOWED`, e cancelar duas vezes também.
  - O check-out devolve o comprovante.
  - Os relatórios `/reports/revenue?month=2027-03` e `/reports/occupancy?date=2027-08-01` saem no formato de antes.

No fim, derrubei o serviço: a porta 7070 não responde mais.
Conclusão: segue

## Dúvidas

Nenhuma.
