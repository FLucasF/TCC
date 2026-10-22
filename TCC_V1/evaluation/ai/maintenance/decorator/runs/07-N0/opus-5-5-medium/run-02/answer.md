# Cenário 07

## Visão geral

O agente pôs as três regras em classes próprias (`EmailValidationRule`, `TestModeRedirectRule`, `ArchiveCopyRule`), todas com a interface `MessageRule`. O `EmailSender` aplica essas regras em lista a uma mensagem mutável (`ProcessedMessage`). Separar cada regra numa peça é o lado bom do desenho. O problema mais sério é a combinação por modo. A montagem do sistema não decide nada: o `Hotel` só passa um booleano `testMode`. Quem escolhe as regras é o construtor do `EmailSender`, que cria sempre as três numa lista fixa (`src/main/java/hotel/messages/EmailSender.java:14-17`). E as próprias regras perguntam pelo modo: a cópia para o arquivo verifica se o sistema está em teste (`src/main/java/hotel/messages/ArchiveCopyRule.java:13`), que é o exemplo de `malfeito` dado pelas instruções. Pelas regras pedidas, conferidas por leitura, o comportamento está quase todo certo. A única falha é que a validação aceita um ponto na ponta do domínio.

## Como foi implementado, e por que o julgamento

**O projeto.** O projeto segue a organização da base, com os pacotes `hotel.rooms`, `hotel.reservations`, `hotel.messages`, `hotel.reports`, `hotel.documents` e `hotel.web`.
- **Montagem e subida:** a classe `Hotel` monta o sistema (`src/main/java/hotel/Hotel.java:19-29`). Ela cria `Rooms`, `ReservationStore` e `SentMessages`, que guardam tudo em memória, e o `ReservationService`, que recebe um `EmailSender`. Depois cria o `HttpApi` do Javalin.
- **Caminho de uma requisição:** a rota de confirmação do `HttpApi` chama o `ReservationService`. Este chama `emailSender.send(...)` (`src/main/java/hotel/reservations/ReservationService.java:69`), e o envio grava em `SentMessages`. O `GET /messages` devolve `sentMessages.all()` em JSON (`src/main/java/hotel/web/HttpApi.java:81`).
- **O que o agente mudou fora das mensagens:** o `Hotel` passou a ler `HOTEL_TEST_MODE` e a passar o booleano ao `EmailSender` (`src/main/java/hotel/Hotel.java:23-25`). O `Message` ganhou o campo `status` (`src/main/java/hotel/messages/Message.java:3`), e o enum é `MessageStatus` (SENT/BLOCKED).
- **Testes:** o agente escreveu o `EmailRulesTest`, com sete casos: endereço inválido bloqueado nos dois modos, endereço válido com cópia, redirecionamento no modo de teste e ausência de cópia no teste e na mensagem bloqueada. Também ajustou `ReservationServiceTest`, `ReportsTest` e `HttpApiTest`, que agora esperam a cópia e o `status`.

**A parte que pede o pattern.**
- A interface é `MessageRule` (`void apply(ProcessedMessage)`, `src/main/java/hotel/messages/MessageRule.java:3-4`). Ela não tem a mesma interface do envio. Uma regra não chama a próxima: ela altera um objeto mutável, e um laço no `EmailSender` aplica as regras uma depois da outra (`src/main/java/hotel/messages/EmailSender.java:23-25`).
- Depois do laço, o próprio `EmailSender` grava a mensagem principal e as derivadas (`EmailSender.java:27-44`). A cópia não é um envio de verdade: é uma `ProcessedMessage` posta na lista `derived` (`ArchiveCopyRule.java:17-22`).
- **Onde mora cada regra:**
  - validação: `EmailValidationRule.java:6-10`, que marca a mensagem como BLOCKED;
  - redirecionamento: `TestModeRedirectRule.java:12-20`;
  - cópia: `ArchiveCopyRule.java:12-22`.
- Duas regras recebem `testMode` no construtor e decidem sozinhas se agem (`TestModeRedirectRule.java:13`, `ArchiveCopyRule.java:13`). Além disso, cada regra precisa perguntar se a mensagem já foi bloqueada (`status() == BLOCKED`), porque a lista nunca é interrompida.

**O raciocínio do julgamento.**
- **O que aproxima do pattern:** é uma lista de filtros aplicada pelo envio, cada regra numa peça própria. As instruções aceitam isso como alternativa equivalente, desde que a combinação por modo seja decidida na montagem.
- **O que afasta:** essa condição não se cumpre, por dois motivos.
  - A lista é fixa dentro do `EmailSender` (`EmailSender.java:14-17`), e o `Hotel` só entrega um booleano. Para atender a segunda pousada, que não quer a cópia, ou para pôr a regra da madrugada "só no teste", seria preciso mexer no `EmailSender` ou criar mais um booleano dentro de uma regra.
  - A combinação por modo está escrita dentro das regras: a cópia testa `testMode` (`ArchiveCopyRule.java:13`), e o redirecionamento também (`TestModeRedirectRule.java:13`).

  Então a estrutura existe, mas convive com o jeito sem ela, e a classe é `malfeito`.
- **Exagero colateral:** não há. O `ProcessedMessage` e o `MessageStatus` servem à própria parte que o pattern resolve, e nada foi criado fora dela.
- **Comportamento:** quase tudo confere por leitura. A falha é na validação do domínio, explicada no item.

## Item a item

**O pattern pedido.** O agente fez uma lista de regras (`MessageRule`), cada uma numa classe, aplicada em laço pelo `EmailSender` (`src/main/java/hotel/messages/EmailSender.java:23-25`). Seria uma alternativa aceita se a combinação ficasse na montagem, mas ela não fica lá:
- o `EmailSender` cria sempre as três regras (`EmailSender.java:14-17`);
- o `Hotel` só passa `testMode` (`src/main/java/hotel/Hotel.java:23-25`);
- a regra da cópia verifica o modo de teste (`src/main/java/hotel/messages/ArchiveCopyRule.java:13`), e o redirecionamento também (`src/main/java/hotel/messages/TestModeRedirectRule.java:13`).

As instruções dão exatamente esse caso, a cópia que verifica o modo, como `malfeito`. Também não dá para tirar a cópia numa instalação sem mudar código.
Conclusão: malfeito

**O exagero colateral.** Não há. As classes novas (`MessageRule`, `ProcessedMessage`, `MessageStatus` e as três regras) servem à variação pedida. Nada de fábrica, builder ou camada de repasse fora dela.
Conclusão: contenção

**O comportamento.** A conferência foi feita só por leitura do código: não subi o serviço nem chamei a API.
- **Endereço inválido:** fica BLOCKED com o endereço original e sem cópia (`EmailValidationRule.java:7-8`; o redirecionamento e a cópia ignoram BLOCKED em `TestModeRedirectRule.java:13` e `ArchiveCopyRule.java:13`). Isso vale também no modo de teste.
- **Cópia:** vai para `arquivo@pousadamarealta.com.br` com "[Cópia] " no assunto e é gravada logo depois da original (`ArchiveCopyRule.java:17-21`, `EmailSender.java:35-43`).
- **Modo de teste:** a mensagem vai para `testes@...`, com "[TESTE] " no assunto e a primeira linha "Destinatário original: …", sem cópia (`TestModeRedirectRule.java:17-19`). O modo é ligado por `HOTEL_TEST_MODE=true` (`Hotel.java:23`).
- **Campo `status`:** aparece no JSON do `GET /messages` (`Message.java:3`, `HttpApi.java:81`).
- **Falha:** a validação só exige que o domínio contenha um ponto (`EmailValidationRule.java:28`). Por isso aceita `ana@gmail.` e `ana@.com`, e o pedido diz "um nome com pelo menos um ponto no meio". Os casos do pedido (`ana@gmail`, `ana.gmail.com`) são bloqueados corretamente.
- **Resto do sistema:** continua igual. A única mudança fora das mensagens é a cópia a mais nas listas.

Conclusão: em parte

## Dúvidas

Fiquei entre `malfeito` e `omissão`. Cada regra é uma peça própria, mas o modo é verificado dentro de duas delas. Escolhi `malfeito` porque a estrutura existe e cobre as três regras. Também é discutível se o ponto na ponta do domínio basta para rebaixar o comportamento para `em parte`. Os casos principais conferem; a falha está só nas bordas da regra que o dono escreveu.
