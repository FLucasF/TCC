# Cenário 03

## Visão geral

O agente não mexeu no desenho. Pôs os avisos novos dentro do `ReservationService`, como envios seguidos ao `EmailSender`. Na confirmação, o e-mail da governança e o do financeiro saem logo depois do e-mail do hóspede. No cancelamento, há um `if (wasConfirmed)` que manda os dois e-mails direto, com os endereços e os textos fixos em constantes do próprio serviço. Não existe interface de ouvinte, nem lista de interessados, nem registro na montagem (`Hotel` não mudou). O comportamento está certo: valores, assuntos, datas, a regra de só avisar o financeiro quando há reembolso e o silêncio no cancelamento de reserva pendente conferem, e o e-mail do hóspede continua igual. O problema sério é de desenho. Hoje o serviço conhece cada interessado, e cada novo interessado (o restaurante, o transfer) vai exigir mexer de novo no `ReservationService`. É a omissão que o cenário descreve.

## Como foi implementado, e por que o julgamento

**O projeto.** Fora do que o agente mudou, a estrutura é a mesma da base:
- `hotel.web.HttpApi` registra as rotas Javalin e traduz `HotelException` em status HTTP.
- `hotel.reservations.ReservationService` guarda as regras do ciclo da reserva (criar, confirmar, cancelar, check-in, check-out, segunda via).
- `ReservationStore` guarda as reservas em memória.
- `PriceCalculator` calcula os preços.
- `hotel.documents` tem o comprovante e a formatação de valores e datas (`TextFormat`).
- `hotel.messages` tem o `EmailSender`, que não envia nada de verdade: só grava a mensagem em `SentMessages`, que a rota `/messages` expõe.
- `hotel.reports` tem os dois relatórios em CSV.

Uma requisição segue um caminho só. Por exemplo, `POST /reservations/{id}/confirmation` passa por `HttpApi` (final/src/main/java/hotel/web/HttpApi.java:70-71), chega a `ReservationService.confirm`, que muda o estado da `Reservation` e chama o `EmailSender`. A classe `Hotel` monta tudo à mão e sobe o servidor (final/src/main/java/hotel/Hotel.java:19-28), e ficou idêntica à da base.

O patch toca três arquivos:
- `ReservationService.java`, com a mudança de verdade.
- `ReservationServiceTest.java` e `HttpApiTest.java`, onde só trocou o número esperado de mensagens de 1 para 3 (final/src/test/java/hotel/reservations/ReservationServiceTest.java:82 e :96, final/src/test/java/hotel/web/HttpApiTest.java:78).

Nenhum teste novo confere o destinatário, o assunto ou o corpo dos e-mails da governança e do financeiro. O cancelamento e a regra do reembolso também ficaram sem teste.

**A parte que pede o pattern.**
- Os endereços viraram constantes do serviço: `HOUSEKEEPING_EMAIL` e `FINANCE_EMAIL` (final/src/main/java/hotel/reservations/ReservationService.java:14-15).
- Em `confirm`, depois do envio ao hóspede, que é o mesmo da base (final/src/main/java/hotel/reservations/ReservationService.java:72-75; na base, base/src/main/java/hotel/reservations/ReservationService.java:69-72):
  - o serviço monta e manda o e-mail da governança (final/src/main/java/hotel/reservations/ReservationService.java:76-81);
  - depois o do financeiro (final/src/main/java/hotel/reservations/ReservationService.java:82-84).
- Em `cancel`:
  - o serviço guarda se a reserva estava confirmada (final/src/main/java/hotel/reservations/ReservationService.java:98);
  - depois de cancelar, um `if (wasConfirmed)` manda o e-mail da governança (final/src/main/java/hotel/reservations/ReservationService.java:104-108);
  - um `if (refund.signum() > 0)` manda o do financeiro (final/src/main/java/hotel/reservations/ReservationService.java:109-112).

Ou seja, cada interessado é uma chamada a mais escrita dentro do serviço, e cada texto é montado ali mesmo. Não existe evento "reserva confirmada" ou "reserva cancelada", nem ouvintes.

**O raciocínio.** As instruções do cenário pedem que o serviço anuncie o fato e percorra uma lista de ouvintes recebida de fora. Cada interessado, inclusive o hóspede, deve ser uma implementação separada, registrada na montagem. Nada disso existe aqui:
- o `ReservationService` continua recebendo só o `EmailSender` (final/src/main/java/hotel/reservations/ReservationService.java:22-28);
- o `Hotel` não registra nada (final/src/main/java/hotel/Hotel.java:23-24);
- a confirmação e o cancelamento mandam os e-mails "um depois do outro", que é exatamente a definição de omissão nas instruções.

Não há estrutura parcial do pattern que justifique "malfeito": nenhuma interface, nenhuma lista, nenhum interessado virou ouvinte.

No exagero colateral, o agente não criou nada além do necessário. Não há classe, interface, fábrica ou camada nova, só constantes e chamadas.

No comportamento, os testes pela API conferiram todas as regras pedidas. Os testes existentes passam. A ressalva é que os testes do agente quase não cobrem o que é novo.

## Item a item

**O pattern pedido.** Não há Observer. O `ReservationService` chama o `EmailSender` três vezes seguidas na confirmação (final/src/main/java/hotel/reservations/ReservationService.java:72-84) e até duas no cancelamento, dentro de `if`s (final/src/main/java/hotel/reservations/ReservationService.java:104-112). Os endereços e os textos de cada interessado estão no próprio serviço (final/src/main/java/hotel/reservations/ReservationService.java:14-15, :77-84, :106-111). Não há interface de ouvinte, nem lista de ouvintes, nem registro na montagem: `Hotel` não mudou (final/src/main/java/hotel/Hotel.java:23-24). O serviço conhece todos os destinos, e para avisar o restaurante ou o transfer seria preciso editar `confirm` de novo. É o caso de omissão que as instruções descrevem: os e-mails saem um depois do outro.
Conclusão: omissão

**O exagero colateral.** Não há. O agente não criou classe, interface, fábrica, builder ou camada nova. A mudança se limita a duas constantes e às chamadas de envio em `ReservationService` (final/src/main/java/hotel/reservations/ReservationService.java:14-15, :76-84, :98-113).
Conclusão: contenção

**O comportamento.** Montei a base e o final (cópias em `/tmp`) e subi os dois serviços. Rodei a mesma sequência nos dois e comparei.

As reservas da sequência:
- #1: quarto 201, STANDARD, julho;
- #2: quarto 101, STANDARD, março;
- #3: quarto 102, NON_REFUNDABLE;
- #4: quarto 201, pendente.

O que veio no final:
- **Confirmação:** cada uma gerou três e-mails.
  - Hóspede: comparado com a base pelo `/messages`, idêntico, só o `id` sequencial da mensagem muda.
  - `governanca@pousadamarealta.com.br`, assunto "Reserva confirmada - quarto 201", com "Entrada: 10/07/2027 | Saída: 12/07/2027" e "Hóspedes: 2".
  - `financeiro@pousadamarealta.com.br`, assunto "Pagamento recebido - reserva 1", com "pagamento de R$ 935,76".
- **Cancelamento da #2 (confirmada, STANDARD):** governança com "Reserva cancelada - quarto 101" e "livre de 10/03/2027 a 13/03/2027", e financeiro com "Reembolso - reserva 2" e "Devolver R$ 749,70".
- **Cancelamento da #3 (confirmada, NON_REFUNDABLE, reembolso 0,00):** só a governança, sem o financeiro, como pedido.
- **Cancelamento da #4 (pendente):** nenhum e-mail.

Nada mais mudou em relação à base. Recancelar devolve `OPERATION_NOT_ALLOWED`. O check-in, o check-out, a segunda via e os relatórios de ocupação e de receita saíram iguais aos da base.

O `mvn verify` da cópia passou com 26 testes, 0 falhas e 0 erros, o mesmo número da base. O agente não escreveu nenhum teste novo, só trocou o total esperado de mensagens de 1 para 3 em três asserções. Os serviços foram derrubados no fim.
Conclusão: segue

## Dúvidas

Nenhuma.
