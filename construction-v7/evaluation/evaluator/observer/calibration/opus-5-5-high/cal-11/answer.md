# Cenário 03

## Visão geral

O sistema funciona e faz tudo o que o enunciado pede. A parte do fim, porém, não usa o Observer. O agente pôs os avisos à equipe dentro do próprio `ReservationService`, que conhece os endereços da governança e do financeiro, monta cada e-mail e manda um depois do outro, tanto na confirmação quanto no cancelamento. Não há interface de ouvinte, nem lista de interessados recebida na montagem. Para avisar o restaurante ou o transfer, que o enunciado já anuncia, alguém teria de abrir e mudar o serviço de reservas. Esse é o problema mais sério. Fora isso, o desenho é enxuto: não há estrutura sobrando, e o comportamento testado confere.

## Item a item

**O enunciado (pede o pattern).** O enunciado traz mais dois interessados na confirmação e no cancelamento e avisa que vão aparecer outros (o restaurante, o transfer). O agente resolveu tudo à mão dentro do serviço:
- os destinos são constantes do próprio serviço (`src/main/java/hotel/reservations/ReservationService.java:14-15`);
- a confirmação manda três e-mails em sequência pelo `EmailSender`: o do hóspede (`ReservationService.java:72-75`), o da governança, com o texto montado ali mesmo (`ReservationService.java:77-81`), e o do financeiro (`ReservationService.java:82-84`);
- o cancelamento usa um `if (wasConfirmed)` e manda à mão o e-mail da governança e, se houver reembolso, o do financeiro (`ReservationService.java:104-113`).

O serviço depende só de `EmailSender` (`ReservationService.java:20,22-28`), uma classe concreta que só grava a mensagem (`src/main/java/hotel/messages/EmailSender.java:9-11`). Ele não anuncia nenhum evento. A montagem em `src/main/java/hotel/Hotel.java:224-225` não registra ouvinte nenhum, e não existe nenhuma interface no código (o `grep` por `interface` em `src/main/java` não acha nada). É exatamente o caso que as instruções chamam de omissão: os e-mails mandados um depois do outro na confirmação e no cancelamento.
Conclusão: omissão

**O exagero colateral.** Não há:
- não existe nenhuma interface, classe abstrata, fábrica ou builder próprio (os únicos `Builder` são o `StringBuilder` do Java e o `JsonMapper.builder()` do Jackson);
- `ReservationStore` (`src/main/java/hotel/reservations/ReservationStore.java:69-90`) e `Rooms` (`src/main/java/hotel/rooms/Rooms.java:243-258`) são classes concretas simples;
- os planos de tarifa ficam em `switch` diretos sobre o enum (`src/main/java/hotel/reservations/PriceCalculator.java:26-29`, `ReservationService.java:99-102`);
- `EmailSender` é uma classe pequena, não uma abstração com várias implementações;
- a montagem é direta, em `Hotel.java:220-229`.
Conclusão: contenção

**O comportamento.** Subi o `target/hotel.jar` de uma cópia, na porta 7070, e testei pela API.

Parte do fim do enunciado:
- **Confirmação da reserva 1 (quarto 201, 10/03 a 12/03/2027, 2 hóspedes):** saíram três e-mails.
  - O do hóspede, para `carla@x.com`, com o assunto "Reserva confirmada - Pousada Maré Alta", a saudação, o comprovante, as duas linhas finais e o "Até breve!", como no enunciado.
  - O da governança, para `governanca@pousadamarealta.com.br`, com o assunto "Reserva confirmada - quarto 201", as datas 10/03/2027 e 12/03/2027 e "Hóspedes: 2".
  - O do financeiro, para `financeiro@pousadamarealta.com.br`, com o assunto "Pagamento recebido - reserva 1" e "pagamento de R$ 779,80".
- **Cancelamento da reserva 2 (confirmada, NON_REFUNDABLE):** só a governança foi avisada ("Reserva cancelada - quarto 101", livre de 10/07/2027 a 12/07/2027). Não saiu e-mail para o financeiro, porque o reembolso é 0,00.
- **Cancelamento da reserva 3 (confirmada, STANDARD):** foram avisados a governança e o financeiro ("Reembolso - reserva 3", "Devolver R$ 599,76").
- **Cancelamento da reserva 4 (pendente):** nenhuma mensagem saiu.

Parte comum:
- **Preços:** STANDARD de 10/07 a 12/07/2027 deu 599.76; NON_REFUNDABLE deu desconto de 89.96 e total de 509.80; uma noite de suíte em julho deu 695.88.
- **Recusas:**
  - quarto já ocupado: 409 ROOM_UNAVAILABLE;
  - hóspedes acima da capacidade: 422 CAPACITY_EXCEEDED;
  - quarto inexistente: 404 ROOM_NOT_FOUND;
  - corpo que não é JSON: 400 INVALID_REQUEST;
  - mês "2027-7": 400 INVALID_REQUEST;
  - reserva inexistente: 404 RESERVATION_NOT_FOUND;
  - confirmar duas vezes: 409 OPERATION_NOT_ALLOWED;
  - cancelar depois do check-in: 409 OPERATION_NOT_ALLOWED;
  - segunda via de uma reserva cancelada: 409 OPERATION_NOT_ALLOWED.
- **Comprovante:** o check-out devolveu o comprovante com todos os campos.
- **Relatórios:** o de ocupação veio em `text/csv; charset=utf-8`, com `total,1/5`. O financeiro de 2027-07 trouxe só a reserva não reembolsável cancelada (509.80). A reserva STANDARD reembolsada e a pendente cancelada ficaram de fora, como o enunciado pede.

O `mvn verify` da cópia passou: 26 testes, sem falhas nem erros. No fim, derrubei o serviço.
Conclusão: segue

## Dúvidas

Nenhuma.
