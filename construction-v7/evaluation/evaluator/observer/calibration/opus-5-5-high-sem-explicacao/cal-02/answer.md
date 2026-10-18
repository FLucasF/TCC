# Cenário 01

## Visão geral

O agente fez a parte comum com cuidado, mas não usou o pattern na parte do fim. Não há ouvinte nem evento. O `ReservationService` conhece os três destinatários: guarda os endereços da governança e do financeiro em constantes, monta todos os e-mails e manda um depois do outro, tanto na confirmação quanto no cancelamento. Avisar o restaurante ou o transfer vai exigir mudar o serviço. O comportamento está todo certo e o resto do sistema também. Não há estrutura desnecessária fora dessa parte. O problema mais sério é exatamente a falta do pattern que o enunciado pedia.

## Item a item

**O enunciado (pede o pattern).** O serviço recebe um único `EmailSender` concreto (`src/main/java/hotel/reservations/ReservationService.java:20`, `:22-27`) e guarda os endereços da equipe em constantes próprias (`ReservationService.java:14-15`). Em `confirm`, ele manda o e-mail do hóspede (`ReservationService.java:72-75`), depois monta e manda o da governança (`:77-81`) e o do financeiro (`:82-84`), um em seguida do outro. Em `cancel`, decide ele mesmo quem avisar, com `if (wasConfirmed)` e `if (refund.signum() > 0)`, e monta os e-mails da governança e do financeiro à mão (`ReservationService.java:104-113`). No projeto não existe nenhuma interface (`grep interface` em `src/main` não acha nada). A montagem em `src/main/java/hotel/Hotel.java:23-24` só passa o `EmailSender` e não registra nenhum interessado. Isso é o caso que as instruções chamam de omissão: a confirmação e o cancelamento mandam os e-mails um depois do outro, e o serviço conhece todos os destinos. Para incluir o restaurante ou o transfer, seria preciso editar `ReservationService`.
Conclusão: omissão

**O exagero colateral.** Não há. As classes são concretas e cada uma tem um papel: `Rooms`, `ReservationStore`, `PriceCalculator`, os dois relatórios e `HttpApi`. O plano de tarifa é resolvido com um `switch` simples (`src/main/java/hotel/reservations/PriceCalculator.java:26-29`). Não há fábrica, builder nem interface com uma implementação só. O único candidato é o `EmailSender` (`src/main/java/hotel/messages/EmailSender.java:4-15`), que repassa para `SentMessages.add` (`src/main/java/hotel/messages/SentMessages.java:11`). Mas ele fixa o canal `EMAIL` e representa o envio que o enunciado descreve, então não o conto como uma camada que só repassa.
Conclusão: contenção

**O comportamento.** Testei numa cópia em `/tmp`. O `mvn -q package -DskipTests` gerou `target/hotel.jar`, que subiu na porta 7070. Os resultados:

- **Preço e criação:**
  - `GET /rooms` devolveu os cinco quartos com as diárias certas.
  - O STANDARD do quarto 101, de 10/07 a 12/07/2027, deu total 599.76.
  - O NON_REFUNDABLE do quarto 202, de 10/07 a 13/07, deu subtotal 1403.64, desconto 210.55 e total 1193.09. Os três valores conferem com o arredondamento meio para o par.
- **Recusas:**
  - quarto ocupado: 409 ROOM_UNAVAILABLE;
  - 3 pessoas no 101: 422 CAPACITY_EXCEEDED;
  - quarto 999: 404 ROOM_NOT_FOUND;
  - corpo que não é JSON: 400 INVALID_REQUEST;
  - confirmar duas vezes: 409 OPERATION_NOT_ALLOWED;
  - data fora do formato no relatório: 400.
- **Confirmação:** cada uma das três confirmações gerou três mensagens.
  - O hóspede recebeu "Reserva confirmada - Pousada Maré Alta", com o texto e o comprovante iguais ao enunciado, inclusive a linha "Desconto" e "R$ 1.403,64".
  - A governança recebeu `governanca@pousadamarealta.com.br`, "Reserva confirmada - quarto 201", com as datas e "Hóspedes: 2".
  - O financeiro recebeu `financeiro@pousadamarealta.com.br`, "Pagamento recebido - reserva 1", com "R$ 779,80".
- **Cancelamento:**
  - Confirmada no plano STANDARD (reserva 2): a governança recebeu "Reserva cancelada - quarto 101" com as datas, e o financeiro "Reembolso - reserva 2" com "R$ 599,76".
  - Confirmada no NON_REFUNDABLE (reserva 3): só a governança foi avisada, porque não havia nada a devolver.
  - Pendente (reserva 4): nenhuma mensagem.
- **Comprovante e relatórios:**
  - O check-out e a segunda via devolveram o comprovante com os campos pedidos.
  - O relatório de receita de 2027-07 veio em `text/csv; charset=utf-8`. Ele lista só a reserva 3, a não reembolsável cancelada que continua contando, com `total,1193.09`.
  - O relatório de ocupação de 2027-03-11 deu `total,1/5`.

O `mvn verify` da cópia passou: 26 testes, 0 falhas, BUILD SUCCESS. Derrubei o serviço no fim.
Conclusão: segue

## Dúvidas

Nenhuma sobre a classe: não existe nenhuma estrutura de ouvinte, então não há como ser malfeito. A única questão menor é o `EmailSender` (`src/main/java/hotel/messages/EmailSender.java:12`), que é quase só um repasse para `SentMessages`. Como ele fixa o canal e é o ponto natural de envio, não o contei como exagero.
