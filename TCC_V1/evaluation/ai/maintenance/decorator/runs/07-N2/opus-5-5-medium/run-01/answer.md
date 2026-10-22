# Cenário 07

## Visão geral

O agente usou um Decorator de manual. A classe `EmailSender` virou interface. O envio de verdade passou para `RecordingEmailSender`, e cada regra tem o seu embrulho: `ValidatingEmailSender`, `TestModeEmailSender` e `ArchivingEmailSender`. Cada embrulho tem um `next` e chama o próximo. A combinação por modo fica num lugar só, em `EmailSenders.assemble`, chamado pela montagem `Hotel`. Nenhum embrulho pergunta pelo modo, e o envio de dentro também não. Pela leitura, as regras pedidas estão certas. Não encontrei problema sério. O ponto mais discutível é que o bloqueio grava a mensagem `BLOCKED` direto em `SentMessages`, sem passar pela corrente. Isso dá certo porque o embrulho de validação fica por fora do embrulho do modo de teste.

## Como foi implementado, e por que o julgamento

**O projeto.** O projeto continua dividido em `hotel.rooms` (quartos), `hotel.reservations` (`ReservationService`, `ReservationStore`, preço), `hotel.documents` (recibo), `hotel.reports` (ocupação e receita), `hotel.messages` (envio e lista de mensagens) e `hotel.web` (`HttpApi`, com as rotas Javalin). Uma requisição entra pela rota do `HttpApi` e vai para o `ReservationService`. Ele aplica a regra e grava no `ReservationStore`, que fica em memória. Na confirmação, o serviço manda o recibo com `emailSender.send(...)` (`src/main/java/hotel/reservations/ReservationService.java:68`), que é o único lugar que envia. O `GET /messages` serializa `sentMessages.all()` (`src/main/java/hotel/web/HttpApi.java:81`). A classe `Hotel` monta tudo: o construtor sem argumentos lê `HOTEL_TEST_MODE` e uma variável nova, `HOTEL_ARCHIVE_COPIES`, que por padrão é `true` (`src/main/java/hotel/Hotel.java:19-21`, `34-37`). Depois disso, ela passa ao serviço o envio já empilhado (`src/main/java/hotel/Hotel.java:28`). O agente escreveu o `EmailSendersTest`, com a validação, o modo normal com cópia, o bloqueio sem cópia, a instalação sem arquivo, o redirecionamento de teste e o bloqueio no teste com o endereço original. Também ajustou o `HttpApiTest` para esperar duas mensagens: a original com `status` `SENT` e a cópia para o arquivo.

**A parte do pattern.**
- `EmailSender` é a interface, com um único método `send` (`src/main/java/hotel/messages/EmailSender.java:4-6`).
- `RecordingEmailSender` é o envio real: só grava a mensagem com `SENT` (`src/main/java/hotel/messages/RecordingEmailSender.java:13-15`).
- `ValidatingEmailSender` verifica o endereço. Se ele for inválido, grava `BLOCKED` com o endereço, o assunto e o texto originais e não chama o próximo. Se for válido, repassa (`src/main/java/hotel/messages/ValidatingEmailSender.java:15-20`). A regra do endereço fica em `isValid` (`:23-34`).
- `TestModeEmailSender` troca o destinatário, o assunto e a primeira linha do texto antes de chamar o próximo, sem fazer nenhuma pergunta (`src/main/java/hotel/messages/TestModeEmailSender.java:15-17`).
- `ArchivingEmailSender` envia a mensagem. Se o status voltar `SENT`, envia logo em seguida a cópia `[Cópia] ` pelo mesmo `next` (`src/main/java/hotel/messages/ArchivingEmailSender.java:15-21`).
- Quem decide a pilha é `EmailSenders.assemble` (`src/main/java/hotel/messages/EmailSenders.java:9-19`). A base é o `RecordingEmailSender`. No modo de teste, ele é embrulhado pelo `TestMode`. A `Validating` vai sempre por fora do que houver. A `Archiving` só entra se `archiveCopies && !testMode`.

**O julgamento.** Isso é o que as instruções chamam de acerto: há uma interface de envio, o envio real e um embrulho por regra, cada um chamando o próximo. A combinação ("cópia só fora do teste", "bloqueio sempre") fica na montagem (`EmailSenders.java:11-17`), e nenhum embrulho a decide. Uma regra nova, como a de madrugada ou a do limite por dia, seria só mais um embrulho e mais uma linha no `assemble`. A segunda pousada sem cópia já é atendida pela flag `HOTEL_ARCHIVE_COPIES`. Encontrei dois detalhes que afastam o código um pouco do ideal, mas nenhum chega a malfeito:
- O `ValidatingEmailSender` recebe `SentMessages` e grava o bloqueio ele mesmo (`ValidatingEmailSender.java:17`), em vez de delegar o registro. Isso é aceitável, porque a mensagem bloqueada, por definição, não passa pelos embrulhos de dentro.
- O `assemble` fica no pacote `messages`, e não dentro de `Hotel`. Mas o `Hotel` o chama, e ele faz exatamente o papel da montagem.

Também não vejo exagero colateral. `EmailSenders` é o próprio ponto de montagem do pattern, e não uma fábrica sem necessidade. O `MessageStatus` foi pedido.

Quanto ao comportamento, conferi tudo por leitura. A validação segue a regra pedida. O bloqueio acontece antes do desvio do modo de teste e guarda o endereço original. A cópia sai logo depois da mensagem original e não existe no modo de teste. O campo `status` é serializado no `GET /messages`, já que é um componente do record `Message` (`src/main/java/hotel/messages/Message.java:3`).

## Item a item

**O pattern pedido.** `EmailSender` é a interface (`src/main/java/hotel/messages/EmailSender.java:4`), e `RecordingEmailSender` é o envio real (`src/main/java/hotel/messages/RecordingEmailSender.java:14`). As três regras são embrulhos com `next` (`ValidatingEmailSender.java:15-20`, `TestModeEmailSender.java:15-17`, `ArchivingEmailSender.java:15-21`), e a pilha muda conforme o modo e a instalação em `EmailSenders.assemble` (`src/main/java/hotel/messages/EmailSenders.java:9-19`), chamado pela montagem em `src/main/java/hotel/Hotel.java:28`. Nenhum embrulho e nenhum envio consulta o modo de teste. O único lugar que envia, `ReservationService.java:68`, usa só a interface. É o Decorator que o texto pedia, e cobre os três casos.
Conclusão: acerto

**O exagero colateral.** Não há. `EmailSenders` é o ponto de montagem da combinação, não uma camada que só repassa. O segundo construtor de `Hotel` (`Hotel.java:23`) e a flag `HOTEL_ARCHIVE_COPIES` (`Hotel.java:20`) atendem ao pedido de cada instalação escolher a sua combinação. Não há interfaces de implementação única fora do pattern, nem builders.
Conclusão: contenção

**O comportamento.** A conferência foi só por leitura, sem subir o serviço, como mandam as instruções.
- **Validação.** `isValid` exige um único `@` com algo antes (`ValidatingEmailSender.java:28-30`) e um domínio que tenha ponto e não comece nem termine com ponto (`:31-33`). Com isso, "ana@gmail" e "ana.gmail.com" são recusados e "ana@gmail.com" passa. Esperava isso, e é o que o código faz. Há uma pequena folga: "ana@gmail..com" e endereços com espaço passam.
- **Bloqueio.** A mensagem fica como `BLOCKED`, com o endereço e o assunto originais, e não gera cópia, porque `Archiving` só copia quando o status é `SENT` (`ArchivingEmailSender.java:17`). No modo de teste, o bloqueio também vale e o endereço original fica, porque `Validating` está por fora do `TestMode` (`EmailSenders.java:11-14`).
- **Cópia.** Vai para `arquivo@pousadamarealta.com.br`, com `"[Cópia] "` no assunto e o mesmo texto, logo depois da original (`ArchivingEmailSender.java:16-18`).
- **Modo de teste.** Tudo vai para `testes@pousadamarealta.com.br`, com `"[TESTE] "` no assunto e `"Destinatário original: …\n"` na primeira linha (`TestModeEmailSender.java:16`), sem cópia (`EmailSenders.java:15`). O modo é ligado por `HOTEL_TEST_MODE=true` (`Hotel.java:20`).
- **Campo `status`.** Está no record (`Message.java:3`).
- **O resto do sistema.** Continua igual. Só a forma de construir o envio mudou nos testes antigos.
Conclusão: segue

## Dúvidas

Uma só, sem efeito na classe. O `ValidatingEmailSender` grava a mensagem bloqueada direto em `SentMessages` (`ValidatingEmailSender.java:17`), em vez de deixar o registro com o envio de dentro. Isso prende esse embrulho à lista de mensagens, mas não mistura a combinação por modo com as regras. Por isso mantive acerto.
