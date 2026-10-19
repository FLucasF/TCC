# Cenário 03

## Visão geral

O agente construiu um sistema enxuto e correto, mas sem nenhum Observer. Os avisos à equipe foram escritos direto nos métodos `confirm` e `cancel` do `ReservationService`: o e-mail do hóspede, o da governança e o do financeiro saem um depois do outro, por chamadas fixas ao `EmailSender` (`src/main/java/hotel/reservations/ReservationService.java:72-84` e `:104-112`). Os endereços da equipe também estão no próprio serviço (`ReservationService.java:14-15`). O que está bom: todo o comportamento pedido funciona (valores, assuntos, a regra de só avisar o financeiro quando há reembolso, o silêncio no cancelamento de reserva pendente), e não há exagero estrutural. O problema mais sério é o desenho: para avisar o restaurante ou o transfer, que o texto já anuncia, seria preciso abrir e mudar o serviço de reservas.

## Como foi implementado, e por que o julgamento

**O projeto.** O código está dividido em pacotes por assunto:

- `hotel.Hotel` monta as peças e sobe o Javalin na porta de `PORT`, ou na 7070 se ela não vier (`src/main/java/hotel/Hotel.java:220-244`).
- `hotel.web.HttpApi` registra as rotas, lê e valida o corpo e traduz `HotelException` em status HTTP com `{error, message}` (`src/main/java/hotel/web/HttpApi.java:61-107`).
- `hotel.reservations` guarda a regra:
  - `ReservationService` cuida do ciclo de vida (criar, confirmar, cancelar, check-in, check-out, segunda via);
  - `PriceCalculator` calcula o preço, com a alta temporada, o desconto do não reembolsável e o arredondamento HALF_EVEN;
  - `Reservation` é a entidade mutável, com estado, valor pago e reembolso;
  - `ReservationStore` é um mapa em memória.
- `hotel.rooms.Rooms` traz os cinco quartos fixos no código.
- `hotel.documents` tem o comprovante (`ReservationReceipt`) e a formatação brasileira (`TextFormat`).
- `hotel.reports` tem os dois CSV.
- `hotel.messages` tem `EmailSender`, que grava em `SentMessages`, uma lista em memória.

O caminho de uma requisição é sempre o mesmo: a rota do `HttpApi` chama um método do `ReservationService`, que valida, lê ou grava no `ReservationStore` e devolve a `Reservation`, embrulhada em `ReservationResponse`. Os testes do agente são 26, todos passando: `PriceCalculatorTest` (6), `ReservationServiceTest` (11), `ReportsTest` (5) e `HttpApiTest` (4), este subindo o servidor numa porta livre.

**A parte que pede o pattern.** Não existe interface de ouvinte nem de evento, e nenhuma lista de interessados. O `ReservationService` recebe um único `EmailSender` no construtor (`ReservationService.java:22-28`).

- Na confirmação (`:65-86`), o serviço monta e envia ele mesmo três e-mails seguidos:
  - o do hóspede, com o corpo de `confirmationEmailBody` (`:149-155`);
  - o da governança, com quarto, datas e hóspedes (`:77-81`);
  - o do financeiro, com o valor pago (`:82-84`).
- No cancelamento (`:92-115`), um `if (wasConfirmed)` (`:104`) envia o aviso da governança (`:106-108`) e, dentro de outro `if (refund.signum() > 0)` (`:109`), o do financeiro (`:110-111`).

O `Hotel` não registra interessado nenhum: só passa `new EmailSender(sentMessages)` ao serviço (`Hotel.java:224-225`). Cada destino é uma chamada fixa dentro do serviço, e o serviço conhece todos eles.

**O raciocínio.** As instruções do pattern dizem que é omissão quando "a confirmação e o cancelamento mandam os e-mails um depois do outro". É exatamente o que está no código. Nada aproxima o desenho do Observer: não há anúncio de evento, não há ouvintes separados, não há registro na montagem, e o e-mail do hóspede também é feito à mão.

Exagero colateral não encontrei. As classes de apoio (`PriceCalculator`, `ReservationStore`, `EmailSender`) são concretas, cada uma com papel próprio. Não há interface com uma implementação só, nem fábrica, nem camada que só repassa.

Sobre o comportamento: todas as chamadas que testei conferiram com o enunciado. Uma ressalva fica para os testes do agente: eles só contam que saíram 3 mensagens na confirmação (`src/test/java/hotel/reservations/ReservationServiceTest.java:82`). Não conferem destinatário nem assunto da governança e do financeiro, e não testam os avisos de cancelamento.

## Item a item

**O pattern pedido.** Não há Observer. `ReservationService.confirm` envia os três e-mails um após o outro, com destinos e textos fixos (`src/main/java/hotel/reservations/ReservationService.java:72-84`). `cancel` faz o mesmo com `if`s (`:104-112`). Os endereços da equipe são constantes do serviço (`:14-15`), e a montagem não registra ouvintes (`src/main/java/hotel/Hotel.java:224-225`). Para incluir o restaurante ou o transfer seria preciso editar o serviço. É o caso que as instruções chamam de omissão.
Conclusão: omissão

**O exagero colateral.** Não há. Nenhuma interface é usada à toa e não há fábrica, builder nem camada de repasse. `EmailSender` (`src/main/java/hotel/messages/EmailSender.java:3-15`) e `PriceCalculator` são classes concretas com responsabilidade real.
Conclusão: contenção

**O comportamento.** Rodei o jar gerado (`target/hotel.jar`) numa cópia e chamei a API com `curl`.

Parte comum:
- **Preços:** STANDARD no 101 de 10/07 a 12/07/2027 deu total 599.76, como no exemplo do enunciado. NON_REFUNDABLE no 301 de 20/07 a 25/07 deu subtotal 3479.40, desconto 521.91 e total 2957.49.
- **Recusas:**
  - quarto já reservado: 409 ROOM_UNAVAILABLE;
  - 3 hóspedes no 101: 422 CAPACITY_EXCEEDED;
  - quarto 999: 404 ROOM_NOT_FOUND;
  - saída igual à entrada: 400 INVALID_REQUEST;
  - corpo que não é JSON: 400 INVALID_REQUEST;
  - confirmar duas vezes: 409 OPERATION_NOT_ALLOWED;
  - cancelar depois do check-out: 409;
  - segunda via de reserva cancelada: 409;
  - reserva 99: 404 RESERVATION_NOT_FOUND;
  - mês 2027-13: 400.
- **E-mail do hóspede:** saiu com o assunto "Reserva confirmada - Pousada Maré Alta" e o comprovante no formato pedido, inclusive a linha "Desconto: R$ 521,91" e o milhar em "R$ 3.479,40".
- **Check-out:** devolveu o comprovante em JSON.
- **Relatórios:**
  - ocupação de 2027-03-10: `201,DELUXE,1,...` e `total,1/5`;
  - ocupação de 2027-03-12, dia da saída: `total,0/5`;
  - financeiro de 2027-07: a reserva com check-out (599.76) e a não reembolsável cancelada (2957.49), `total,3557.25`;
  - Content-Type `text/csv; charset=utf-8`.

Parte do fim:
- **Confirmação:** cada uma gerou 3 mensagens:
  - para o hóspede;
  - para `governanca@pousadamarealta.com.br`, com o assunto "Reserva confirmada - quarto 201", as datas 10/03/2027 e 12/03/2027 e "Hóspedes: 2";
  - para `financeiro@pousadamarealta.com.br`, com o assunto "Pagamento recebido - reserva 1" e "R$ 779,80".
- **Cancelamento de reserva pendente (4):** nenhuma mensagem nova.
- **Cancelamento de STANDARD confirmada (5):** "Reserva cancelada - quarto 202", com 01/08/2027 a 03/08/2027, e "Reembolso - reserva 5", com R$ 779,80.
- **Cancelamento de NON_REFUNDABLE confirmada (3):** só "Reserva cancelada - quarto 301", sem aviso ao financeiro, porque o reembolso é 0.00.

No fim havia 15 mensagens, o total esperado. O `mvn verify` da cópia passou: 26 testes, 0 falhas, BUILD SUCCESS. O serviço foi derrubado depois dos testes.
Conclusão: segue

## Dúvidas

Nenhuma.
