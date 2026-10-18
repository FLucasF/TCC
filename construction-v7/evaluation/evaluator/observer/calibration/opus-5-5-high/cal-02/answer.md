```markdown
# Cenário 01

## Visão geral

O agente entregou um sistema enxuto que funciona bem: todas as regras que testei conferem, inclusive os avisos da equipe. Mas a parte do fim do enunciado foi feita sem o Observer. O `ReservationService` conhece cada destinatário pelo endereço, tem o texto de cada e-mail escrito dentro dele e manda os e-mails um depois do outro, tanto na confirmação (`src/main/java/hotel/reservations/ReservationService.java:72-84`) quanto no cancelamento (`src/main/java/hotel/reservations/ReservationService.java:104-113`). Não existe interface de ouvinte, nem lista de interessados, nem registro na montagem do sistema. Para avisar o restaurante ou o transfer, que o enunciado já anuncia, seria preciso mexer no serviço de reservas. O que está bom é a contenção: fora dessa parte não há estrutura sobrando.

## Como foi implementado, e por que o julgamento

**O projeto.** Tudo fica no pacote `hotel`, dividido em subpacotes:
- `rooms`: o `Rooms` guarda os cinco quartos fixos no código (`src/main/java/hotel/rooms/Rooms.java:8-13`).
- `reservations`: o `Reservation` é a entidade com seu estado, o `ReservationStore` guarda as reservas num `LinkedHashMap` em memória, o `PriceCalculator` calcula o preço com alta temporada, desconto e arredondamento HALF_EVEN, e o `ReservationService` cuida do ciclo de vida da reserva. Os erros são o `HotelException` com o `ErrorCode`.
- `documents`: o `ReservationReceipt` monta o comprovante, em JSON e em texto, e o `TextFormat` formata dinheiro e datas.
- `messages`: o `SentMessages` é a lista de mensagens enviadas, e o `EmailSender` é um invólucro fino que grava nessa lista com o canal "EMAIL".
- `reports`: o `OccupancyReport` e o `RevenueReport` geram os CSVs.
- `web`: o `HttpApi` define as rotas Javalin, o mapeamento de erros e os DTOs.

Uma requisição segue este caminho: a rota do `HttpApi` (por exemplo `src/main/java/hotel/web/HttpApi.java:70-73`) lê o id ou o corpo e chama o `ReservationService`. O serviço valida, mexe na `Reservation`, usa o `ReservationStore` e o `EmailSender`, e a rota devolve um `ReservationResponse`. A classe `Hotel` monta as peças à mão e sobe o servidor na porta da variável `PORT`, ou na 7070 se ela não vier (`src/main/java/hotel/Hotel.java:20-28` e `:41-43`). O build gera `target/hotel.jar` com shade. O agente escreveu 26 testes, em `ReservationServiceTest`, `PriceCalculatorTest`, `ReportsTest` e `HttpApiTest`. Sobre os avisos da equipe, os testes só conferem que a confirmação gera 3 mensagens (`src/test/java/hotel/reservations/ReservationServiceTest.java:82`). Os e-mails da governança e do financeiro não têm nenhuma asserção.

**A parte do fim.** Não existe nenhuma classe própria para ela. Os endereços da governança e do financeiro são constantes do próprio serviço (`src/main/java/hotel/reservations/ReservationService.java:14-15`).
- Em `confirm`, o serviço manda o e-mail do hóspede (`:72-75`), depois monta e manda o da governança (`:77-81`) e por fim o do financeiro (`:82-84`), em três chamadas seguidas a `emailSender.send`.
- Em `cancel`, um `if (wasConfirmed)` manda o e-mail da governança (`:104-108`), e um `if (refund.signum() > 0)` decide se o financeiro também recebe (`:109-112`).

O `EmailSender` recebe só destinatário, assunto e texto (`src/main/java/hotel/messages/EmailSender.java:12-14`). Ele não sabe nada de reservas nem de interessados. O `Hotel` passa um único `EmailSender` ao serviço (`src/main/java/hotel/Hotel.java:24-25`) e não registra ouvinte nenhum.

**O julgamento.** As instruções do pattern pedem quatro coisas:
- que o serviço anuncie o evento sem saber quem é avisado;
- que cada interessado seja um ouvinte separado, com o hóspede incluído;
- que a lista de ouvintes venha de fora;
- que os ouvintes sejam registrados na montagem.

O projeto não tem nada disso. É exatamente o caso que as instruções chamam de omissão: "a confirmação e o cancelamento mandam os e-mails um depois do outro". O serviço sabe quem é avisado, sabe o que dizer a cada um e decide quando avisar. Também não é malfeito, porque não existe lista de ouvintes, nem parcial. A classe é **omissão**.

No exagero colateral, não encontrei interface com uma implementação só, nem fábrica, builder ou camada que só repassa (`grep` por `interface|abstract|Factory|Builder` em `src/main` não acha nada). O `EmailSender` é uma classe concreta e pequena, com um papel real: carimbar o canal. As classes de relatório e o `PriceCalculator` concentram regras de verdade. Por isso, contenção.

No comportamento, tudo o que testei confere com o enunciado, como mostra o item abaixo.

## Item a item

**O enunciado (pede o pattern).** A confirmação manda em sequência o e-mail do hóspede, o da governança e o do financeiro, escritos no próprio `ReservationService` (`src/main/java/hotel/reservations/ReservationService.java:72-84`). O cancelamento manda o da governança e, se houver reembolso, o do financeiro, dentro de `if`s no mesmo serviço (`src/main/java/hotel/reservations/ReservationService.java:104-113`). Os endereços são constantes do serviço (`:14-15`). Não existe interface de ouvinte nem lista de interessados, e o `Hotel` não registra nada (`src/main/java/hotel/Hotel.java:24-25`). Um novo interessado, como o restaurante ou o transfer, exige editar `confirm`/`cancel`. Isso é o contrário do Observer, que o enunciado pede ao anunciar que vai ter cada vez mais gente querendo ser avisada.
Conclusão: omissão

**O exagero colateral.** Não há. As classes são concretas e cada uma tem regra própria: `PriceCalculator`, `OccupancyReport`, `RevenueReport` e `ReservationReceipt`. O `EmailSender` (`src/main/java/hotel/messages/EmailSender.java:4-15`) é fino, mas cumpre um papel real: fixa o canal EMAIL e grava na lista. Não existe interface, fábrica, builder nem camada de repasse.
Conclusão: contenção

**O comportamento.** Fiz o build de uma cópia e subi o jar `target/hotel.jar` na porta 7070.
- **Criação.** O STANDARD do quarto 101 de 10/07/2027 a 12/07/2027 deu subtotal e total de 599.76, com 201 e status PENDING. Um NON_REFUNDABLE do quarto 202 por 3 noites deu 1169.70 − 175.46 = 994.24.
- **Recusas.** 3 hóspedes no quarto 102 deu 422 CAPACITY_EXCEEDED. Uma sobreposição no 101 deu 409 ROOM_UNAVAILABLE. O quarto 999 deu 404 ROOM_NOT_FOUND. Um corpo que não é JSON e o mês 2027-13 deram 400 INVALID_REQUEST. A reserva 99 deu 404 RESERVATION_NOT_FOUND. Cancelar depois do check-in deu 409 OPERATION_NOT_ALLOWED.
- **Confirmação.** Cada confirmação gerou 3 mensagens:
  - o e-mail do hóspede, com o assunto e o texto do enunciado e o comprovante certo, inclusive a linha "Desconto: R$ 175,46" e "R$ 1.169,70";
  - `governanca@…` com o assunto "Reserva confirmada - quarto 201", as datas 10/03/2027 e 12/03/2027 e "Hóspedes: 2";
  - `financeiro@…` com o assunto "Pagamento recebido - reserva 2" e "pagamento de R$ 779,80".
- **Cancelamento.**
  - Uma STANDARD confirmada (reserva 2) recebeu refund 779.80 e gerou "Reserva cancelada - quarto 201" com as datas e "Reembolso - reserva 2" com "R$ 779,80".
  - Uma NON_REFUNDABLE confirmada (reserva 3) recebeu refund 0.00 e avisou só a governança.
  - Uma pendente (reserva 4) foi cancelada sem mensagem nenhuma: o total ficou em 12 mensagens.
- **Comprovante e relatórios.** O check-out devolveu o comprovante com os campos pedidos. A segunda via de uma reserva cancelada deu 409. A ocupação de 2027-07-10 veio em `text/csv; charset=utf-8` com `total,1/5`. A receita de 2027-03 trouxe a NON_REFUNDABLE cancelada com 994.24 e deixou de fora a STANDARD reembolsada.
- **Testes do agente.** O `mvn verify` da cópia rodou 26 testes, sem falhas, e terminou com BUILD SUCCESS. O serviço foi derrubado no fim.
Conclusão: segue

## Dúvidas

nenhuma
```
