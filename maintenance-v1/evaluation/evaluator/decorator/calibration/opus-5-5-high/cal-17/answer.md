# Cenário 07

## Visão geral

O agente resolveu o pedido com um Decorator limpo. Criou a interface `MessageSender`, fez o `EmailSender` real implementá-la e pôs cada regra num embrulho próprio: o bloqueio de endereço, a cópia para o arquivo e o desvio para a caixa de testes. Cada embrulho chama o próximo, e a pilha é montada na classe `Hotel` conforme o `HOTEL_TEST_MODE`. Nenhum embrulho pergunta pelo modo, e o envio de dentro não sabe de regra nenhuma. O único ponto discutível é pequeno: o embrulho de bloqueio grava a mensagem `BLOCKED` direto na `SentMessages`, sem passar pelo envio. Isso tem sentido, porque a mensagem bloqueada não é enviada, e não mistura a combinação por modo dentro das regras. Os testes pela API conferem com o pedido nos dois modos.

## Item a item

**O pedido (pede o pattern).**
- **A interface.** A interface de envio está em `src/main/java/hotel/messages/MessageSender.java:6`, e o envio real a implementa em `src/main/java/hotel/messages/EmailSender.java:13-14`, que agora só grava a mensagem com `SENT`.
- **Os três embrulhos.** Cada regra é um embrulho com a mesma interface e um `next`:
  - `BlockInvalidAddress` grava `BLOCKED` se o endereço não casar com o padrão e, senão, delega (`src/main/java/hotel/messages/BlockInvalidAddress.java:20-24`);
  - `ArchiveCopy` delega a original e depois manda a cópia com "[Cópia] " (`src/main/java/hotel/messages/ArchiveCopy.java:15-18`);
  - `RedirectToTestInbox` troca o destinatário, o assunto e a primeira linha e delega (`src/main/java/hotel/messages/RedirectToTestInbox.java:15-16`).
- **A combinação por modo.** Ela é decidida só na montagem, em `src/main/java/hotel/Hotel.java:38-43`: o bloqueio fica sempre por fora, e por dentro vai o desvio no teste ou a cópia fora dele.
- **Quem envia.** O `ReservationService` passa a depender da interface (`src/main/java/hotel/reservations/ReservationService.java:17,20,69`) e não conhece as regras. O único ponto de envio do sistema passa pela pilha.
- **As regras anunciadas.** As regras futuras (madrugada, limite por dia) entram como novos embrulhos, empilhados em `Hotel` conforme o modo.

Isso é exatamente o Decorator que as instruções descrevem como acerto.
Conclusão: acerto

**O exagero colateral.** Não há. As peças novas (`MessageSender`, `MessageStatus` e os três embrulhos) estão todas dentro do que o pedido tocava. O campo `status` em `Message` (`src/main/java/hotel/messages/Message.java:3-4`) foi pedido explicitamente. A troca do tipo do parâmetro do `ReservationService` para a interface é consequência direta do pattern. Nenhuma outra parte do código ganhou estrutura.
Conclusão: contenção

**O comportamento.** Subi uma cópia da `final/` duas vezes: sem a variável, na porta 7070, e com `HOTEL_TEST_MODE=true`, na 7071. Em cada uma, criei e confirmei (`POST /reservations` e `POST /reservations/{id}/confirmation`) reservas com estes e-mails: `ana@gmail.com`, `ana@gmail`, `ana.gmail.com`, `a@@b.com`, `@x.com`, `ana@.com`, `ana@gmail.` e `ana@gmail..com`.
- **Modo normal.** O `GET /messages` trouxe a confirmação para `ana@gmail.com` com `status: SENT` e, logo depois, a cópia para `arquivo@pousadamarealta.com.br` com o assunto "[Cópia] Reserva confirmada - Pousada Maré Alta", corpo idêntico (`.[0].body == .[1].body` → `true`) e `SENT`. Os outros sete endereços vieram `BLOCKED`, com o endereço original e sem cópia.
- **Modo de teste.** A mensagem válida foi para `testes@pousadamarealta.com.br`, com o assunto "[TESTE] Reserva confirmada…", primeira linha "Destinatário original: ana@gmail.com", `SENT` e sem cópia. Os inválidos vieram `BLOCKED` com o endereço original, sem o prefixo de teste.
- **O que não mudou.** O preço na alta temporada continua igual: 2 noites no quarto 101 em janeiro dão 599,76. O check-in e o check-out com o comprovante funcionam, o relatório financeiro de 2027-01 saiu certo, e o `POST /reservations` com `{}` continua devolvendo `INVALID_REQUEST`.
- **Os testes.** O `mvn -q verify` da cópia passou: 29 testes, 0 falhas. Desses, 3 são do agente em `SendingRulesTest`, e um teste do `HttpApiTest` foi ajustado de 1 para 2 mensagens por causa da cópia.

Os serviços foram derrubados no fim.
Conclusão: segue

## Dúvidas

Só uma, pequena. O `BlockInvalidAddress` recebe a `SentMessages` e grava a mensagem bloqueada direto nela (`src/main/java/hotel/messages/BlockInvalidAddress.java:22`), em vez de delegar a um colaborador. É uma pequena dependência fora da interface de envio, mas a regra continua sendo uma peça própria e não decide nada sobre o modo, então não muda a classe.
