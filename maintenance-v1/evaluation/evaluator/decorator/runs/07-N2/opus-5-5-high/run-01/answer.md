# Cenário 07

## Visão geral

O agente pôs as três regras novas como `if`s dentro de `EmailSender.send`: a validação do endereço, o desvio do modo de teste e a cópia para o arquivo. O modo de teste chega ao envio como um `boolean` no construtor. Não existe interface de envio, nenhum embrulho e nenhuma corrente de regras, e a classe `Hotel` não monta nada conforme o modo. O comportamento ficou quase todo certo e a validação foi isolada num `EmailValidator`, o que está bom. O problema mais sério está no desenho. As próximas regras já anunciadas (madrugada, limite por dia), cada uma valendo sempre, só no teste ou só fora dele, vão virar mais `if (testMode)` dentro do mesmo método. Há também um erro de comportamento: endereços com o ponto na borda do domínio (`ana@gmail.`, `ana@.com`) passam como válidos.

## Item a item

**O pedido (pede o pattern).** As três regras e a combinação delas por modo estão escritas à mão no mesmo método. O bloqueio está em `final/src/main/java/hotel/messages/EmailSender.java:19-21`. O desvio para a caixa de testes fica sob `if (testMode)` em `final/src/main/java/hotel/messages/EmailSender.java:27-31`. A cópia para o arquivo fica sob `if (!testMode)` em `final/src/main/java/hotel/messages/EmailSender.java:35-37`. O modo entra como campo do envio (`final/src/main/java/hotel/messages/EmailSender.java:7`, `:13-16`). Na montagem, `final/src/main/java/hotel/Hotel.java:23-25` só lê a variável de ambiente e passa o `boolean` para o `EmailSender`; nada é empilhado conforme o modo. `EmailSender` continua sendo uma classe concreta `final`, sem interface, e `ReservationService` depende dela diretamente (`final/src/main/java/hotel/reservations/ReservationService.java:17`). As instruções descrevem esse caso como omissão: os três `if` dentro do `send`, sem embrulhos, sem corrente e sem combinação decidida na montagem. Tirar o `EmailValidator` (`final/src/main/java/hotel/messages/EmailValidator.java:3-17`) para fora é só extrair uma função. Não é uma peça que embrulha o envio.
Conclusão: omissão

**O exagero colateral.** Não há. As peças novas são `EmailValidator`, uma função estática que o pedido exige, e `MessageStatus` (`final/src/main/java/hotel/messages/MessageStatus.java:3-6`), o enum do campo `status` que o pedido também exige. A mudança em `Message`/`SentMessages` só acrescenta o campo (`final/src/main/java/hotel/messages/Message.java:3`, `final/src/main/java/hotel/messages/SentMessages.java:11-12`). Nenhuma interface, fábrica ou camada foi criada fora do que o pedido tocava.
Conclusão: contenção

**O comportamento.** Testei numa cópia da `final/`. O `mvn -q package -DskipTests` funcionou e o `mvn -q verify` passou: 34 testes, 0 falhas. Desses, 10 são testes novos do agente (5 em `EmailSenderTest`, 2 em `EmailValidatorTest` e 1 em `HttpApiTest`), e o agente ajustou testes que já existiam para contar a cópia. Subi o serviço duas vezes, na porta 7070 sem a variável e na 7071 com `HOTEL_TEST_MODE=true`. Em cada um criei reservas com `POST /reservations` e as confirmei com `POST /reservations/{id}/confirmation`.

O que confere com o pedido:
- **Modo normal, endereço válido.** `carla@example.com` gerou a mensagem 1 (`SENT`) e logo depois a 2, para `arquivo@pousadamarealta.com.br`, com assunto `[Cópia] Reserva confirmada - Pousada Maré Alta`, o mesmo corpo (comparação com `jq` deu `true`) e `status` `SENT`.
- **Endereços inválidos.** `ana@gmail`, `ana.gmail.com` e `a@@b.com` aparecem como `BLOCKED`, com o endereço original e sem cópia, nos dois modos.
- **Modo de teste, endereço válido.** A mensagem foi para `testes@pousadamarealta.com.br`, com assunto `[TESTE] …` e primeira linha `Destinatário original: carla@example.com`, sem cópia.

O que não confere:
- **Ponto na borda do domínio.** Esperava que `ana@gmail.` e `ana@.com` fossem `BLOCKED`, porque o pedido quer "um nome com pelo menos um ponto no meio". Os dois saíram como `SENT`: no modo normal geraram as mensagens 6 e 8, cada uma com cópia para o arquivo (7 e 9); no modo de teste foram desviados para a caixa de testes. A causa é que a regra só confere `domain.contains(".")` (`final/src/main/java/hotel/messages/EmailValidator.java:15-16`).

O que não mudou e continua igual:
- Confirmar duas vezes a mesma reserva ainda devolve 409, sem mensagem nova.
- `GET /reservations/1`, `GET /rooms` e `GET /reports/revenue?month=2027-07` responderam normalmente.

No fim, derrubei os dois serviços.
Conclusão: em parte

## Dúvidas

Só uma, sobre a regra de validação: "um nome com pelo menos um ponto no meio" pode ser lido de mais de um jeito. Li "no meio" como "não na ponta", e por isso conto `ana@gmail.` e `ana@.com` como erro. Com uma leitura mais frouxa ("tem um ponto"), o comportamento seria `segue`. A classe do desenho não depende dessa dúvida.
