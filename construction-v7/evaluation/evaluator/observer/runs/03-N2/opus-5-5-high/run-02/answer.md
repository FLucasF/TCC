# Cenário 01

## Visão geral

O agente montou os avisos da confirmação e do cancelamento com um Observer completo. Há uma interface `ReservationListener` e três ouvintes independentes, um para o hóspede, um para a governança e um para o financeiro. Os três são registrados na montagem do sistema, e o `ReservationService` só percorre a lista, sem saber quem é avisado nem o que cada um recebe. O e-mail do hóspede também virou ouvinte, e o cancelamento passa pela mesma estrutura que a confirmação. O resto do sistema é enxuto: enums com dados, records para o JSON e nenhuma camada sobrando. Nos testes com `curl`, todas as regras da parte do fim e as da parte comum que conferi saíram certas. Não achei problema sério. O único ponto que se pode discutir é pequeno: quem decide que o cancelamento de uma reserva pendente não gera evento é o serviço, e não cada ouvinte.

## Como foi implementado, e por que o julgamento

**O projeto.** Fica tudo num pacote só, `com.marealta`, e cada classe tem um papel:

- **Partida e montagem.** `Main` lê a variável `PORT` e usa 7070 se ela não vier (`src/main/java/com/marealta/Main.java:5-6`). `App` monta as peças e declara as rotas Javalin. No construtor, ele cria o `ReservationService` com os cinco quartos e registra os ouvintes (`App.java:25-35`). As rotas ficam em `App.java:37-58`.
- **Dados e preço.** `RoomType` é um enum com capacidade e diária (`RoomType.java:5-8`). `RatePlan` é um enum com a taxa de desconto e a informação de se o plano devolve o pagamento (`RatePlan.java:6-24`). `Pricing` calcula a alta temporada e a soma das noites (`Pricing.java:13-29`), e `Money` arredonda com HALF_EVEN e formata R$ e datas (`Money.java:17-31`).
- **Reserva.** `Reservation` guarda os valores e cuida das transições de estado com `require` (`Reservation.java:41-62`).
- **Saída.** `Receipt` é o comprovante e gera o texto dele (`Receipt.java:15-28`). `Reports` gera os dois CSVs (`Reports.java:14-45`). `ReservationView` e `RoomView` são records que dão forma ao JSON.
- **Erros.** `ApiException` traz fábricas estáticas para cada código de erro (`ApiException.java:16-21`).

O caminho de uma requisição é este: a rota em `App` valida o corpo (`App.java:60-84`), chama o `ReservationService`, e o serviço busca a reserva e chama a transição na `Reservation`. Os dados ficam na memória, numa lista e num `TreeMap` dentro do serviço (`ReservationService.java:10-11`). Os e-mails vão para o `Outbox`, uma lista sincronizada (`Outbox.java:7-16`) que `GET /messages` devolve. O jar é gerado pelo shade plugin, com `Main` no manifesto (`pom.xml:63-64`). Há um arquivo de testes, `AppTest`, com 4 testes que sobem o servidor numa porta livre e cobrem o preço, os avisos da confirmação, as regras do cancelamento, os erros e os relatórios (`src/test/java/com/marealta/AppTest.java:37-87`).

**A parte do fim do enunciado.**

- **A interface.** `ReservationListener` tem dois métodos `default` vazios, `onConfirmed` e `onPaidReservationCancelled` (`ReservationListener.java:7-13`).
- **Um ouvinte por interessado:**
  - `GuestNotifier` monta o e-mail do hóspede com o comprovante (`GuestNotifier.java:10-17`).
  - `HousekeepingNotifier` guarda o endereço da governança e trata os dois eventos (`HousekeepingNotifier.java:5`, `:11-24`).
  - `FinanceNotifier` guarda o endereço do financeiro e trata os dois eventos. A regra de não avisar quando não há nada a devolver mora dentro dele (`FinanceNotifier.java:18`).
- **O serviço.** O `ReservationService` recebe os ouvintes por `addListener` (`ReservationService.java:18-20`). Na confirmação, ele faz `listeners.forEach(l -> l.onConfirmed(r))` (`ReservationService.java:53`). No cancelamento, só emite o evento se a reserva já estava paga (`ReservationService.java:59-61`).
- **A montagem.** Os três ouvintes são registrados em `App.java:31-33`. Avisar o restaurante ou o transfer é criar uma classe nova e registrá-la ali, sem mexer no serviço.

**O raciocínio.** As instruções do Observer pedem três coisas, e o projeto cumpre as três:

1. Uma interface de ouvinte com uma implementação por interessado.
2. O serviço percorrendo uma lista que vem de fora.
3. O hóspede também como ouvinte.

Nenhum dos sinais de malfeito aparece:

- O serviço não monta texto nenhum.
- O cancelamento não manda e-mail à mão.
- O hóspede não tem chamada direta.
- Nenhum ouvinte pergunta qual é o caso concreto.

O filtro do "estava paga" no serviço (`ReservationService.java:59-61`) define *quando* o evento acontece, e não *quem* recebe. O próprio nome do evento, `onPaidReservationCancelled`, deixa isso explícito, então não afasta o desenho do pattern.

Quanto ao exagero colateral, não encontrei nada:

- Não há interfaces com uma implementação só, fábricas, builders nem camadas que só repassam.
- As views são records que dão forma ao JSON.
- As fábricas estáticas de `ApiException` são só construtores com nome, não uma estrutura a mais.

No comportamento, tudo o que conferi bateu com o enunciado, e a suíte do agente passa.

## Item a item

**O enunciado (pede o pattern).** A interface `ReservationListener` (`ReservationListener.java:7-13`) tem três implementações separadas: `GuestNotifier` (`GuestNotifier.java:4-17`), `HousekeepingNotifier` (`HousekeepingNotifier.java:4-24`) e `FinanceNotifier` (`FinanceNotifier.java:4-21`). O serviço guarda a lista de ouvintes, que recebe por `addListener` (`ReservationService.java:12`, `:18-20`). Ele anuncia a confirmação (`ReservationService.java:53`) e o cancelamento de uma reserva paga (`ReservationService.java:61`) sem conhecer endereço, assunto nem texto. O registro fica na montagem do sistema (`App.java:31-33`). O hóspede também é ouvinte, e a regra "só avisa o financeiro se houver reembolso" mora no próprio ouvinte (`FinanceNotifier.java:18`). É o Observer que o enunciado precisava, aplicado aos três interessados e aos dois eventos.
Conclusão: acerto

**O exagero colateral.** Não há. Os tipos e planos são enums com dados (`RoomType.java:5-19`, `RatePlan.java:6-24`). `Pricing` e `Money` são utilitários estáticos pequenos. As views são records de saída (`ReservationView.java:6-15`, `RoomView.java:5-9`). Não há interfaces de uma implementação só, repositórios que só repassam nem fábricas.
Conclusão: contenção

**O comportamento.** Testei numa cópia do projeto, com o jar `target/reservas-1.0.0.jar` rodando na porta 7070.

*Avisos da parte do fim:*

- **Confirmação da reserva 2** (quarto 201, STANDARD, de 10/03 a 12/03/2027, 2 hóspedes). Saíram três e-mails:
  - para o hóspede, o assunto "Reserva confirmada - Pousada Maré Alta" e o texto exato do enunciado, com o comprovante (Diárias e Total R$ 779,80);
  - para `governanca@pousadamarealta.com.br`, "Reserva confirmada - quarto 201", com Entrada 10/03/2027, Saída 12/03/2027 e Hóspedes: 2;
  - para `financeiro@pousadamarealta.com.br`, "Pagamento recebido - reserva 2", com R$ 779,80.
- **Cancelamento da reserva 2, confirmada e STANDARD.** Veio `refund: 779.80` e saíram "Reserva cancelada - quarto 201", com as datas liberadas, e "Reembolso - reserva 2", com R$ 779,80.
- **Reserva 3, NON_REFUNDABLE de uma noite.**
  - Valores: subtotal 389.90, desconto 58.48 (o meio-para-o-par sobre 58,485) e total 331.42.
  - O comprovante tem "1 diária" e a linha "Desconto".
  - Ao cancelar depois de confirmada, veio `refund: 0.00`, a governança foi avisada e o financeiro não.
- **Reserva 4, pendente cancelada.** Nenhuma mensagem nova.
- **Reserva 5, suíte de 20/12 a 23/12/2027.** Total 2087.64, e o reembolso foi formatado como R$ 2.087,64.
- **Check-in e check-out.** Não geraram mensagens.

*Parte comum:*

- **Quartos e preços.** `GET /rooms` devolve os cinco quartos com `nightlyRate` de 2 casas. O STANDARD de 10/07 a 12/07/2027 deu 599.76.
- **Conflito e erros.** Depois do cancelamento, o quarto 201 voltou a aceitar reserva para as mesmas datas, e uma reserva sobreposta a essa deu 409 ROOM_UNAVAILABLE. Os outros erros vieram certos: 404 ROOM_NOT_FOUND, 422 CAPACITY_EXCEEDED, 404 RESERVATION_NOT_FOUND e 400 INVALID_REQUEST para 0 hóspede, saída antes da entrada, data mal formada, corpo que não é JSON e campo faltando.
- **Transições proibidas.** Todas deram 409 OPERATION_NOT_ALLOWED: confirmar duas vezes, fazer check-out antes do check-in, cancelar depois do check-out e pedir a segunda via de reserva pendente.
- **Comprovante.** O check-out e a segunda via devolveram o comprovante com todos os campos.
- **Ocupação.** `text/csv; charset=utf-8`. Em 2027-07-10 veio `101,…` e `total,1/5`; em 2027-07-12, `total,0/5`.
- **Receita de 2027-07.**
  - Incluiu a reserva 7, NON_REFUNDABLE cancelada, com 795.40, antes da reserva 1, CHECKED_OUT, com 599.76.
  - Fechou com `total,1395.16`.
  - O mês `2027-13` deu 400.

*Testes do agente:* o `mvn verify` da cópia terminou com BUILD SUCCESS, 4 testes, 0 falhas e 0 erros. No fim, derrubei o serviço e a porta 7070 ficou fechada.
Conclusão: segue

## Dúvidas

Nenhuma que mude a classe. O único ponto discutível é o filtro de quando há evento de cancelamento. O serviço só emite `onPaidReservationCancelled` se a reserva já estava paga (`ReservationService.java:59-61`), em vez de anunciar todo cancelamento e deixar cada ouvinte decidir. Isso é a definição do evento, não conhecimento sobre quem é avisado. Também não há montagem de e-mail no serviço, então não chega a ser malfeito.
