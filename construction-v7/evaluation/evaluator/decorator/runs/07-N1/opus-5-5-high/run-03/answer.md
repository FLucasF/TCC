# Cenário 01

## Visão geral

O agente juntou as três regras de envio num único método, `MessageService.sendEmail`, como três `if` em sequência: bloquear o endereço inválido, desviar no modo de teste e fazer a cópia só fora do teste. Não há interface de envio, embrulho, corrente nem lista de filtros. O modo de teste chega como um `boolean` que o próprio serviço consulta (`services/MessageService.java:12`, `:31`, `:40`). Como pedido, o resultado está certo: testei os dois modos e bloqueio, cópia, desvio e `status` saem como o enunciado descreve. O desenho, porém, é exatamente o que as instruções do pattern chamam de omissão. Para encaixar "não mandar de madrugada" ou "limite por dia", cada uma valendo em modos diferentes, seria preciso abrir esse método de novo e acrescentar mais `if` sobre `testMode`. Fora dessa parte, o desenho é simples e sem estrutura de sobra. Já o comportamento comum tem falhas: o relatório de ocupação conta reservas pendentes e sai fora de ordem, e o comprovante sai como texto puro em vez de JSON.

## Como foi implementado, e por que o julgamento

**O projeto.** O pacote `com.pousada` tem o `Main`, que lê `PORT` e `HOTEL_TEST_MODE` (`Main.java:15-25`), cria um `HotelService(testMode)` (`Main.java:27`), configura o Jackson e entrega tudo a `ApiRoutes.setup` (`Main.java:57`). Em `api/` ficam as rotas Javalin (`api/ApiRoutes.java`), os DTOs em `record` e o `ErrorResponse`. Em `services/` ficam:

- `HotelService`: uma fachada que cria os outros serviços (`services/HotelService.java:19-25`). Ela coordena a confirmação, que confirma a reserva, monta o e-mail e manda (`:48-69`), e nos outros métodos só repassa a chamada.
- `RoomService`: os cinco quartos fixos num `HashMap`.
- `ReservationService`: as reservas num `HashMap<Long, Reservation>`, os ids, o conflito de datas e o reembolso.
- `ReceiptService`: o texto do comprovante.
- `ReportService`: os dois CSV.
- `MessageService`: a lista de mensagens e as regras de envio.

Em `utils/` há funções estáticas: `PriceCalculator` para o preço com `HALF_EVEN`, `DateUtils` e `EmailValidator`. As transições de estado ficam no modelo `Reservation` (`models/Reservation.java:120-148`).

Uma requisição percorre este caminho: rota em `ApiRoutes` → método do `HotelService` → `ReservationService` ou outro serviço → mapa em memória. Os erros sobem como `IllegalArgumentException` com o código na mensagem, e a rota traduz o código em status HTTP (`api/ApiRoutes.java:54-68`). Tudo fica em memória. O agente escreveu 23 testes de unidade, todos passando no `mvn -q verify`: 7 de `PriceCalculator`, 10 de `EmailValidator` e 6 de `MessageService`, que cobrem bloqueio, cópia, desvio e ausência de cópia no teste. Não há teste de rota.

**A parte do fim do enunciado.** Quem cuida dela é uma só classe, `MessageService`, apoiada no `EmailValidator` e no enum `MessageStatus`.

- O modo vem do construtor (`services/MessageService.java:16-18`) e é montado em `HotelService.java:22`.
- O método `sendEmail` (`services/MessageService.java:20-52`) faz, nesta ordem:
  - se o endereço é inválido, grava a mensagem `BLOCKED` com o endereço original e retorna (`:21-25`);
  - se `testMode`, troca o destinatário, o assunto e o corpo (`:31-35`);
  - grava a mensagem `SENT` (`:37-38`);
  - se `!testMode && EmailValidator.isValid(to)`, grava a cópia para o arquivo (`:40-49`). O segundo teste do endereço é redundante.

Nenhuma regra é uma peça própria. As três são trechos do mesmo método, e a combinação por modo ("cópia só fora do teste", "bloqueio sempre") está escrita dentro da regra, no `!testMode` da linha 40. Ela não é decidida na montagem. A montagem só repassa um `boolean`.

**O raciocínio.** O Decorator pede uma interface de envio, um envio real e um embrulho por regra, cada um chamando o próximo, com a pilha montada no `main` conforme o modo. Nada disso existe aqui: não há interface, não há envio "de dentro" separado das regras e o `main` não escolhe regra nenhuma. Também não é uma corrente nem uma lista de filtros, porque não há peças separadas. É o caso que as instruções descrevem como omissão: "os três `if` (o endereço, o modo de teste, a cópia) ficam dentro do envio". Não chega a ser malfeito, porque para isso a estrutura do pattern teria de existir em parte, e não existe.

No exagero colateral, não há interface de uma implementação só, fábrica nem builder. O `HotelService` mistura raiz de composição, coordenação real da confirmação e muitos métodos que só repassam. É uma fachada de serviço comum e não chega a ser uma camada vazia. O `ReceiptService.generateReceiptForEmail` (`services/ReceiptService.java:43-45`), que só chama `generateReceipt`, é um método redundante, não uma estrutura.

No comportamento, as regras do fim do enunciado estão todas certas. Os erros estão na parte comum:

- **Ocupação com pendentes:** o relatório inclui reservas pendentes, porque `ReservationService.getReservationsForRoom` só exclui as canceladas (`services/ReservationService.java:163-169`).
- **Ocupação fora de ordem:** o relatório percorre um `HashMap` (`services/ReportService.java:29`, `services/RoomService.java:28`), e as linhas não saem por número do quarto.
- **Comprovante em texto:** o check-out e a segunda via devolvem `text/plain` em vez do JSON com `title`, `reservationId`… (`api/ApiRoutes.java:152`, `:171`).
- **Segunda via de pendente:** ela sai para reserva pendente, porque `HotelService.getReceipt` não verifica a situação (`services/HotelService.java:86-92`).
- **`guests: 0`:** devolve 422 `CAPACITY_EXCEEDED` em vez de 400 (`services/ReservationService.java:43-44`).

## Item a item

**O enunciado (pede o pattern).** As três regras são `if` dentro de `MessageService.sendEmail`:

- o bloqueio em `services/MessageService.java:21-25`;
- o desvio de teste em `:31-35`;
- a cópia em `:40-49`, condicionada a `!testMode`.

O modo é um campo `boolean` do próprio serviço (`:12`, `:16-18`), repassado por `HotelService.java:22` a partir de `Main.java:15`. Não existe interface de envio, embrulho, corrente nem lista de filtros, e a combinação por modo não é decidida na montagem: está escrita dentro do envio. As novas regras anunciadas ("madrugada", "limite por dia", cada uma valendo em modos diferentes) teriam de virar mais `if` cruzados com `testMode` nesse mesmo método. Não é o pattern nem uma alternativa aceita.
Conclusão: omissão

**O exagero colateral.** Não há interface de uma implementação só, fábrica, builder ou camada vazia. O `HotelService` (`services/HotelService.java:27-104`) repassa a maior parte das chamadas, mas também é a raiz de composição e coordena a confirmação com o e-mail (`:48-69`). Isso é uma fachada de serviço comum. O `ReceiptService.generateReceiptForEmail` (`services/ReceiptService.java:43-45`) é um método redundante de uma linha, não uma estrutura.
Conclusão: contenção

**O comportamento.** Subi o jar `target/mare-alta-1.0.0.jar` duas vezes, na porta 7070 sem a variável e na 7071 com `HOTEL_TEST_MODE=true`. Em cada uma criei e confirmei reservas com `carla@gmail.com`, `ana@gmail`, `ana.gmail.com`, `a@b@c.com` e `@x.com`.

- **Fora do teste:**
  - `carla@gmail.com` saiu `SENT`, com o texto do enunciado exato, e logo depois veio a cópia para `arquivo@pousadamarealta.com.br` com "[Cópia] Reserva confirmada - Pousada Maré Alta", mesmo corpo e `SENT`;
  - os quatro endereços inválidos saíram `BLOCKED`, com o endereço original e sem cópia.
- **No teste:** `carla@gmail.com` foi para `testes@pousadamarealta.com.br`, com "[TESTE] …" e a primeira linha "Destinatário original: carla@gmail.com", sem cópia. Os inválidos continuaram `BLOCKED` com o endereço original.

Tudo isso confere com o pedido.

Na parte comum, conferem:

- o preço do exemplo (2 × 299,88 = 599.76);
- o desconto NON_REFUNDABLE (389.90 → 58.48 / 331.42);
- os erros 404, 409 (`ROOM_UNAVAILABLE`, `OPERATION_NOT_ALLOWED`), 400 para corpo inválido e 422 para capacidade;
- o reembolso (STANDARD devolve 779.80, NON_REFUNDABLE devolve 0);
- o financeiro, em que a reserva não reembolsável cancelada conta com 331.42.

Não conferem:

- a ocupação de 2027-08-01 lista a reserva 6, ainda `PENDING` (`total,1/5`);
- a ocupação de 2027-10-01 lista o quarto 201 antes do 101;
- o check-out e o `GET /reservations/6/document` devolvem `text/plain` com o texto do comprovante, não o JSON pedido, e a segunda via sai para reserva pendente;
- `guests: 0` dá 422 `CAPACITY_EXCEEDED` em vez de 400 `INVALID_REQUEST`;
- `amountPaid` e `refund` saem como `0`, não `0.00`;
- `GET /rooms` vem fora de ordem.

O `mvn -q verify` passou: 23 testes, 0 falhas.
Conclusão: em parte

## Dúvidas

O `HotelService` fica no limite de "camada que só repassa": sete dos seus métodos só delegam. Considerei contenção porque ele também monta as peças e tem regra própria na confirmação, mas um leitor mais rigoroso poderia contá-lo como exagero leve. Na classe do desenho não há dúvida.
