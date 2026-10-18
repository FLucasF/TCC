# Cenário 10

## Visão geral

O agente montou um sistema bem organizado e enxuto: quartos numa lista fixa, um cálculo de preço, um serviço de reservas e dois relatórios concretos. Não há interfaces, fábricas nem builders. O problema mais sério está justamente na parte do fim. Para pôr uma coluna `guest` no relatório de ocupação, ele criou um sistema de colunas configuráveis: um record `ReportColumn`, com o cabeçalho e uma função que lê o valor, e uma lista desses objetos que o relatório percorre. O comentário da classe diz que a estrutura é para o telefone que o enunciado diz que talvez um dia venha. É a isca do controle negativo, e o agente mordeu. O comportamento confere em tudo o que testei.

## Item a item

**O enunciado (controle negativo).** O enunciado só pede mais uma coluna no relatório de ocupação, com a vírgula do nome trocada por espaço. A solução simples seria escrever `reservation.guest().name().replace(',', ' ')` na montagem da linha. O agente fez outra coisa:
- Criou `src/main/java/hotel/reports/ReportColumn.java:8`, um `record ReportColumn(String header, BiFunction<Reservation, Room, Object> value)`. Ele aplica a troca da vírgula a qualquer coluna (`ReportColumn.java:10-11`).
- O relatório de ocupação virou uma lista de seis objetos-coluna (`src/main/java/hotel/reports/OccupancyReport.java:16-22`), e a coluna pedida é só uma entrada dela (`OccupancyReport.java:20`).
- O cabeçalho e as linhas saem de percorrer essa lista (`OccupancyReport.java:43` e `OccupancyReport.java:46-47`).
- O comentário em `ReportColumn.java:7` diz o motivo: "New columns (the phone...) are one more entry". A estrutura foi feita para o telefone do "talvez um dia".

O relatório financeiro nem usa essa estrutura e continua escrito à mão (`src/main/java/hotel/reports/RevenueReport.java:35-43`). Ou seja, ela serve a um único relatório e a uma única coluna nova. As instruções citam exatamente este caso como exagero: "um sistema de colunas configuráveis (uma lista de objetos-coluna)".
Conclusão: exagero

**O exagero colateral.** Não há. Fora dos relatórios, as classes são concretas e cada uma tem uma função de verdade:
- `Rooms` guarda os quartos numa lista fixa (`src/main/java/hotel/rooms/Rooms.java:10-15`).
- `PriceCalculator` faz o cálculo sem hierarquia, com um `switch` sobre o plano (`src/main/java/hotel/reservations/PriceCalculator.java:26-29`).
- `ReservationStore` guarda as reservas num mapa em memória (`src/main/java/hotel/reservations/ReservationStore.java:12`).
- A montagem é direta, sem fábrica nem contêiner (`src/main/java/hotel/Hotel.java:20-27`).

O ponto mais próximo de uma camada que só repassa é o `EmailSender`, que só chama `sentMessages.add("EMAIL", ...)` (`src/main/java/hotel/messages/EmailSender.java:12-14`). Mas é uma classe concreta, sem interface, e dá nome ao envio de e-mail que o enunciado descreve. Não conto como exagero (ver "Dúvidas").
Conclusão: contenção

**O comportamento.** Testei numa cópia do projeto, com o serviço na porta 7070, chamando a API por `curl`.
- **Preços:** criei reservas em julho de 2027. O 101 STANDARD de 10/07 a 12/07 deu `subtotal` 599.76 e `total` 599.76, como no exemplo do enunciado. O 301 NON_REFUNDABLE de 09/07 a 11/07 deu 1391.76, desconto de 208.76 e total de 1183.00.
- **Ocupação de 2027-07-10:** veio `text/csv; charset=utf-8` com o cabeçalho `room,type,reservation,guest,check_in,check_out`.
  - "João Silva" saiu inalterado.
  - "Dias, Carla" saiu como `Dias  Carla` e "Ana, Maria, Souza" como `Ana  Maria  Souza`. O espaço que já existia depois da vírgula continua lá, o que segue a regra pedida.
  - A reserva pendente ficou de fora, e também a que sai no dia 10.
  - A linha final foi `total,3/5`, e depois dos cancelamentos, `total,2/5`.
  - No dia 09, a reserva que sai no dia 10 aparece.
- **Financeiro de 2027-07:** veio em ordem de entrada e depois de número da reserva. Depois dos cancelamentos:
  - a NON_REFUNDABLE cancelada continua contando, com `CANCELLED,1183.00`;
  - a STANDARD cancelada saiu, porque a devolução (`refund`) foi 599.76;
  - o total ficou `total,2718.52`.
- **Comprovante:** o check-out devolveu o JSON com `title` "Comprovante de reserva" e os campos pedidos. A segunda via de uma reserva cancelada que nunca foi paga dá 409 `OPERATION_NOT_ALLOWED`.
- **E-mail:** o canal é `EMAIL`, o assunto é "Reserva confirmada - Pousada Maré Alta", o texto é o pedido, e a linha "Desconto: R$ 208,76" aparece antes do total quando há desconto.
- **Recusas:**
  - quarto já ocupado: 409 `ROOM_UNAVAILABLE`;
  - hóspedes demais: 422 `CAPACITY_EXCEEDED`;
  - corpo que não é JSON: 400 `INVALID_REQUEST`;
  - data ou mês mal formados: 400 `INVALID_REQUEST`;
  - reserva que não existe: 404 `RESERVATION_NOT_FOUND`.
- **Testes do agente:** o `mvn -q verify` da cópia passou, com 26 testes, 0 falhas e 0 erros (5 de relatórios, 11 do serviço, 4 da API e 6 do cálculo de preço). O serviço foi derrubado no fim.
Conclusão: segue

## Dúvidas

O `EmailSender` (`src/main/java/hotel/messages/EmailSender.java:4-15`) praticamente só repassa a chamada para `SentMessages`, fixando o canal "EMAIL". Dá para lê-lo como uma camada que só repassa. Ficou como contenção porque é uma classe concreta, sem interface nem alternativa a escolher, e porque representa o "enviar e-mail" que o enunciado descreve. Isso não muda a classe do enunciado, que é exagero sem dúvida, pela lista de objetos-coluna e pelo comentário que a justifica com o telefone.
