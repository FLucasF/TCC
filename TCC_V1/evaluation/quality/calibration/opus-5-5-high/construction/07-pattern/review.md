# Revisão: construção, cenário 07

## Visão geral

O agente montou um projeto Maven com 7 pacotes por assunto: `hotel` (a montagem e o banco), `hotel.web` (a borda HTTP), `hotel.reservations` (as regras, o preço e o acesso às reservas), `hotel.rooms`, `hotel.messages`, `hotel.documents` (o comprovante) e `hotel.reports`. Uma requisição entra em `HttpApi` (`src/main/java/hotel/web/HttpApi.java:61-91`), que lê o corpo, chama `ReservationService` e devolve um `ReservationResponse`. O serviço valida, calcula o preço com `PriceCalculator` (`BigDecimal` com `HALF_EVEN`) e grava pelo `ReservationStore` (JDBC com `?`, pool Hikari). Os dados ficam no PostgreSQL, num esquema criado por duas migrações do Flyway, com uma restrição `EXCLUDE` que impede duas reservas do mesmo quarto na mesma noite. A parte do fim do texto foi resolvida com um Decorator: uma interface `MessageSender`, o envio real `EmailSender` e um embrulho por regra, empilhados em `Hotel.messageSender` conforme `HOTEL_TEST_MODE`. Todas as 65 conferências do roteiro passaram e os 29 testes passam. O mais sério é pequeno: um ciclo entre `hotel.reservations` e `hotel.documents`.

## Arquitetura

**Números:** 7 pacotes, 17 dependências entre eles, 1 ciclo (`hotel.documents` ↔ `hotel.reservations`). O Javalin aparece só em `hotel` e `hotel.web`. O JDBC aparece em `hotel.messages`, `hotel.reservations` e `hotel.rooms`, sempre em classes de acesso a dados. A maior classe é `HttpApi`, com 170 linhas.
**Nota:** com problemas

- **A borda HTTP é fina.** Cada rota lê a requisição, chama uma única operação do serviço e escreve a resposta (`src/main/java/hotel/web/HttpApi.java:62-90`). Os erros saem de um único `exception` handler, que traduz `ErrorCode` para o status HTTP (`HttpApi.java:93-107`). A validação dos campos obrigatórios fica na borda (`HttpApi.java:109-130`), e a validação de domínio (datas, hóspedes, capacidade) fica no serviço (`src/main/java/hotel/reservations/ReservationService.java:28-45`).
- **As regras não conhecem o Javalin nem o SQL.** `ReservationService` só fala com `Rooms`, `ReservationStore`, `PriceCalculator` e `MessageSender` (`ReservationService.java:14-25`).
- **O SQL está isolado** em `ReservationStore`, `Rooms` e `SentMessages`. O `ReservationStore` mora no mesmo pacote das regras, mas numa classe só dele. Não há SQL fora dessas três classes.
- **A montagem fica num lugar só,** em `src/main/java/hotel/Hotel.java:24-44`. Nenhuma classe cria as próprias dependências por dentro, e não há `static` mutável.
- **O defeito é o ciclo.** `ReservationService` importa e constrói `ReservationReceipt` (`ReservationService.java:3`, `:64`, `:109`, `:118`). Por sua vez, `ReservationReceipt` lê `Reservation` (`src/main/java/hotel/documents/ReservationReceipt.java:3`, `:24`). É um ciclo pequeno entre dois pacotes vizinhos. Ele resolveria tirando a montagem do comprovante do serviço, ou pondo o comprovante no pacote das reservas. Como o projeto foi feito do zero, o ciclo é do agente. Pela régua da skill, um ciclo entre dois pacotes basta para `com problemas`, mesmo com o resto bem separado.

## Design de baixo nível

**Números:** as 3 regras pedidas estão na estrutura, cada uma num embrulho próprio de `MessageSender`: bloqueio do endereço inválido, cópia para o arquivo e desvio para a caixa de testes. Nenhuma regra fica fora dela, e há 0 testes do modo dentro dos embrulhos. A combinação por modo é decidida em 1 lugar, `Hotel.java:41-43`.
**Classe:** acerto
**Exagero:** contenção
**Nota:** bom

**O pattern.**
- **A interface e o envio real.** `MessageSender` é a interface do envio (`src/main/java/hotel/messages/MessageSender.java:4-7`). `EmailSender` é o envio real e só guarda a mensagem como `SENT` (`src/main/java/hotel/messages/EmailSender.java:13-15`).
- **Um embrulho por regra, cada um chamando o próximo:**
  - `BlockInvalidAddress` testa o endereço e, se for inválido, registra a mensagem como `BLOCKED` sem seguir adiante (`src/main/java/hotel/messages/BlockInvalidAddress.java:20-25`);
  - `ArchiveCopy` manda a original e depois a cópia "[Cópia] " pelo mesmo `next` (`src/main/java/hotel/messages/ArchiveCopy.java:15-19`);
  - `RedirectToTestInbox` reescreve o destinatário, o assunto e a primeira linha do texto (`src/main/java/hotel/messages/RedirectToTestInbox.java:15-17`).
- **A combinação fica na montagem.** Fora do teste a pilha é `Block(ArchiveCopy(Email))`; no teste é `Block(Redirect(Email))` (`Hotel.java:39-44`). Nenhum embrulho e nem o envio de dentro pergunta pelo modo. Como o bloqueio é a camada de fora, a mensagem bloqueada fica com o endereço original também no teste, como o texto pede.
- **As regras anunciadas** (não mandar de madrugada, limite por dia, cópia desligada numa segunda pousada) entram como novos embrulhos e uma linha a mais na montagem, sem mexer nos que existem.
- **Um detalhe que afasta um pouco do ideal:** o bloqueio grava direto em `SentMessages` em vez de passar pelo envio (`BlockInvalidAddress.java:22`). Com isso, o literal `"EMAIL"` aparece em dois lugares (`BlockInvalidAddress.java:22` e `EmailSender.java:14`). É um atalho aceitável, porque a mensagem bloqueada não deve passar pelos outros embrulhos.

**O exagero.** Não há estrutura sem necessidade. A única interface (`MessageSender`) tem quatro implementações. `PriceCalculator`, os repositórios e o serviço são classes concretas, sem fábricas, builders ou camadas que só repassam.

**O resto do código.**
- **Os tipos estão certos.** Dinheiro é `BigDecimal`, e o arredondamento a cada passo sai de um único `cents` (`src/main/java/hotel/reservations/PriceCalculator.java:50-52`). As datas são `LocalDate` e `YearMonth`, e há `enum` para a situação da reserva, o plano, o tipo de quarto, o código de erro e o status da mensagem.
- **Os erros têm código próprio.** `HotelException` carrega o `ErrorCode`, e as falhas de SQL viram `IllegalStateException` com a causa preservada (por exemplo, `src/main/java/hotel/reservations/ReservationStore.java:59`).
- **Pontos menores, que não pedem mudança antes de aceitar:**
  - `Reservation.changeStatus` aceita qualquer transição (`src/main/java/hotel/reservations/Reservation.java:102-104`). As regras de transição ficam no serviço (`ReservationService.java:59`, `:78`, `:93`, `:104`), e não na própria reserva.
  - O `ReservationStore.read` remonta um `NewReservation` só para chamar o construtor (`ReservationStore.java:125-131`).
  - O `catch (Exception)` ao ler o corpo é largo, mas comentado e restrito a devolver 400 (`HttpApi.java:111-115`).

## Testes

**Números:** 29 testes rodaram, com 0 falhas, 0 erros e 0 pulados. São 29 métodos de teste no código. A cobertura é de 499 de 530 linhas (94,2%) e de 104 de 120 ramos (86,7%); `hotel.messages` tem 52 de 56 linhas e 4 de 4 ramos, e `hotel` tem 3 de 6 ramos. Há 5 classes de teste contra o banco de verdade (`DB_URL`) e 1 pela API HTTP, sem mocks.
**Nota:** bom

- **As regras da parte comum são conferidas com valores:**
  - **A temporada:** as bordas de 14 e 15 de dezembro, de 29 de fevereiro e 1º de março, e de 30 de junho, julho e 1º de agosto (`src/test/java/hotel/reservations/PriceCalculatorTest.java:31-54`).
  - **O "meio para o par":** 37,485 vira 37,48 (`PriceCalculatorTest.java:57-64`).
  - **As devoluções** por plano e reserva pendente (`src/test/java/hotel/reservations/ReservationServiceTest.java:185-198`).
  - **As transições recusadas** (`ReservationServiceTest.java:175-222`) e **os códigos de erro** (`ReservationServiceTest.java:129-138`; `src/test/java/hotel/web/HttpApiTest.java:178-192`).
  - **Os dois relatórios,** comparados como texto inteiro, inclusive a não reembolsável cancelada e a noite do check-out (`src/test/java/hotel/reports/ReportsTest.java:294-353`).
- **A parte nova tem testes com valores.** O endereço válido gera a original e a cópia, na ordem (`src/test/java/hotel/messages/SendingRulesTest.java:27-33`). Quatro endereços inválidos são bloqueados sem cópia (`SendingRulesTest.java:35-44`). No modo de teste, conferem-se o destinatário, o assunto e a primeira linha do texto (`SendingRulesTest.java:46-52`).

**A pergunta da mutação.** Trocar "[Cópia] " ou "[TESTE] ", tirar a cópia ou deixar passar "ana@gmail" faz um teste falhar. Ficam sem rede:
- **A pilha do modo de teste montada por `Hotel.messageSender`** (`Hotel.java:41-43`). Os testes montam as pilhas à mão, e `HttpApiTest` só sobe o modo normal. Uma mudança na leitura de `HOTEL_TEST_MODE`, ou tirar o bloqueio da pilha do teste, não seria pega.
- **O endereço inválido no modo de teste,** que deve ficar com o endereço original: não há teste.
- **Endereços com ponto na borda do domínio,** como `ana@gmail.com.` e `ana@.gmail.com`: não há teste. Conferi pela API e o código os bloqueia.
- **O texto exato do e-mail e o "1 diária":** só há `contains` (`ReservationServiceTest.java:171-172`).

São lacunas pequenas diante do conjunto. O texto não pedia testes, e o agente escreveu testes que conferem regras de verdade.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), criadas por 2 migrações do Flyway. Todas têm chave primária. As 6 colunas de dinheiro são `numeric(10,2)`, a chave estrangeira tem índice e nenhuma data é texto. As conferências acusaram só `messages.channel`, `reservations.rate_plan`, `reservations.status` e `rooms.type` em `text` sem CHECK. Há 0 transações explícitas e 0 `FOR UPDATE`, e o SQL montado com `+` só concatena constantes.
**Nota:** bom

- **O esquema evolui por migrações versionadas.** `src/main/resources/db/migration/V1__create_tables.sql` cria as tabelas. O `status` das mensagens veio numa migração nova, com `DEFAULT` e `CHECK` (`src/main/resources/db/migration/V2__message_status.sql:2-3`), sem editar a V1. Os quartos são semeados na própria migração (`V1__create_tables.sql:9-14`), o que combina com "quase nunca mudam".
- **A regra do quarto duplicado está no banco.** Uma `EXCLUDE USING gist` sobre `room_number` e `daterange(check_in, check_out)`, valendo só para as não canceladas (`V1__create_tables.sql:38-40`), funciona entre processos. O código traduz a violação `23P01` em `ROOM_UNAVAILABLE` (`src/main/java/hotel/reservations/ReservationStore.java:55-58`). O `synchronized` do serviço (`ReservationService.java:27`) é só uma camada a mais.
- **Mais restrições protegem regras:** `CHECK (check_out > check_in)`, `CHECK (guests >= 1)` e a chave estrangeira para `rooms` (`V1__create_tables.sql:21`, `:27`, `:36`).
- **O código que fala com o banco está certo.** Todo SQL usa `?`. As concatenações de `ReservationStore.java:98` e `:111` só juntam a constante `COLUMNS`. As conexões ficam em try-with-resources sobre o pool Hikari (`src/main/java/hotel/Database.java:23-28`).
- **Defeitos menores, que não derrubam a nota:**
  - Situação, plano e tipo de quarto ficam em `text` sem CHECK (`V1__create_tables.sql:4`, `:28`, `:33`).
  - Os relatórios trazem a tabela inteira de reservas para filtrar em Java e buscam o quarto linha a linha (`src/main/java/hotel/reports/OccupancyReport.java:25`, `:36`; `src/main/java/hotel/reports/RevenueReport.java:25`).
  - A confirmação grava a reserva e a mensagem em duas escritas separadas, sem transação (`ReservationService.java:62-68`). Se a gravação da mensagem falhar, a reserva fica confirmada sem o e-mail na lista.

## Comportamento

**Números:** 65 de 65 conferências passaram, 0 falharam:
- a parte comum do enunciado: 46 de 46;
- os dados depois de reiniciar: 3 de 3;
- o cenário 07 no modo normal: 9 de 9;
- o cenário 07 no modo de teste: 5 de 5;
- as reservas simultâneas: 2 de 2.

**Nota:** bom

Tudo o que o texto pede funciona pela API:
- os preços, com a temporada e o arredondamento;
- as recusas, com os códigos certos;
- a confirmação, com o e-mail e o comprovante;
- o check-in, o check-out e as devoluções;
- os dois relatórios em CSV;
- os dados que sobrevivem a um reinício;
- as dez rodadas de reservas simultâneas, cada uma com uma criada e a outra recusada com 409.

Na parte nova, o e-mail para endereço válido sai e é seguido da cópia "[Cópia] " para o arquivo. Os endereços inválidos ficam `BLOCKED`, com o endereço original e sem cópia. No modo de teste tudo vai para a caixa de testes, com "[TESTE] " e "Destinatário original: ...", sem cópia, e o bloqueio continua valendo.

Conferi à mão, num banco à parte:
- `ana@gmail.com.` e `ana@.gmail.com` também saem `BLOCKED`;
- o corpo do e-mail sai exatamente no formato do texto, com "1 diária" e a linha "Desconto: R$ 58,48" antes do total;
- uma data fora do formato (`2027-7-10`) responde 400 `INVALID_REQUEST`.

Para o dono da pousada, o sistema faz o que ele descreveu.

## Dúvidas

Nenhuma que mude as notas. Fica uma de régua: na Arquitetura, o único defeito é o ciclo pequeno entre `hotel.reservations` e `hotel.documents`, e a skill pede `com problemas` para isso. Um revisor que pesasse menos esse ciclo daria `bom`.
