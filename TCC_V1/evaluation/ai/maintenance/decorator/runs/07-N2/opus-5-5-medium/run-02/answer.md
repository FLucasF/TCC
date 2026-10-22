# Cenário 07

## Visão geral

O agente pôs as três regras em peças próprias: `EmailValidator`, `TestModeHandler` e `ArchiveCopyHandler`, todas implementando a interface `EmailHandler`. O `EmailSender` percorre essas peças em lista, como uma corrente. Até aí, o desenho está perto da alternativa equivalente que as instruções aceitam. O problema mais sério é onde fica a combinação por modo. Ela não é decidida na montagem do sistema: o `Hotel` só passa dois booleanos ao `EmailSender`, e é o próprio envio que recebe `testMode` e monta a lista com um `if (!testMode)`. Além disso, as regras carregam o modo dentro delas. O `TestModeHandler` entra na lista sempre e verifica a própria flag; o `ArchiveCopyHandler` tem uma flag `enabled`. Para criar uma nova regra ou uma nova combinação por instalação, é preciso mudar o construtor e o método privado do `EmailSender`, e o `EMAIL_HANDLERS.md` do próprio agente diz isso. O comportamento confere nos casos principais. A exceção é a validação do domínio, que aceita endereço com o ponto na ponta, como `ana@gmail.`.

## Como foi implementado, e por que o julgamento

**O projeto.** É um serviço Javalin com estes pacotes:
- `hotel.rooms`: os quartos.
- `hotel.reservations`: o `ReservationService`, com as regras de reserva, o preço e o armazenamento em memória (`ReservationStore`).
- `hotel.documents`: o recibo.
- `hotel.reports`: os relatórios de ocupação e de receita.
- `hotel.messages`: o envio de e-mail e a lista `SentMessages`.
- `hotel.web`: o `HttpApi`, com as rotas.

A classe `Hotel` monta tudo (`final/src/main/java/hotel/Hotel.java:19-29`) e sobe na porta definida por `PORT` (`Hotel.java:42`). Os dados ficam em memória.

O único envio de e-mail acontece na confirmação da reserva. O caminho é a rota `POST /reservations/{id}/confirmation` (`final/src/main/java/hotel/web/HttpApi.java:70-71`), depois `ReservationService.confirm` e por fim `emailSender.send(...)` (`final/src/main/java/hotel/reservations/ReservationService.java:69-72`). O `GET /messages` serializa `sentMessages.all()` (`HttpApi.java:81`). O registro `Message` ganhou o campo `status`, do tipo enum `MessageStatus` (`final/src/main/java/hotel/messages/Message.java:3`, `MessageStatus.java:3-6`).

Os testes do agente:
- `EmailSenderTest`: nove casos de validação, cópia e modo de teste.
- `EmailHandlersIntegrationTest`: cinco combinações.
- Ajustes no `HttpApiTest`, que agora espera duas mensagens, com a cópia, e o `status`.
- Ajustes no `ReservationServiceTest` e no `ReportsTest`, por causa do novo construtor.

**A parte que pede o pattern.**
- A interface é `EmailHandler.handle(EmailContext)`, que devolve `boolean` para dizer se a corrente continua (`EmailHandler.java:7-14`).
- O `EmailContext` é mutável: tem `to`, `subject`, `body`, `status` e uma lista de mensagens extras (`EmailContext.java:12-27`).
- O `EmailValidator` marca `BLOCKED` e interrompe a corrente (`EmailValidator.java:13-19`).
- O `TestModeHandler` reescreve o destinatário, o assunto e o corpo, mas só se a flag `testMode` dele estiver ligada (`TestModeHandler.java:18-26`).
- O `ArchiveCopyHandler` acrescenta a cópia se `enabled` estiver ligado e se a mensagem estiver `SENT` (`ArchiveCopyHandler.java:18-22`).
- O `EmailSender.send` percorre a lista e, no fim, grava a mensagem principal e as extras (`EmailSender.java:17-33`).
- A lista é montada por `buildHandlerChain(testMode, archiveCopyEnabled)`, dentro do `EmailSender`. O validador e o handler de teste entram sempre, e a cópia entra `if (!testMode)` (`EmailSender.java:35-43`).
- O `Hotel` lê `HOTEL_TEST_MODE` e chama `new EmailSender(sentMessages, testMode, true)` (`Hotel.java:23-25`).

**O raciocínio.** O que aproxima o desenho do pattern: cada regra é uma classe própria com a mesma interface, a corrente pode ser interrompida e a cópia fica separada da validação. Isso corresponde à "corrente de regras" que as instruções aceitam.

O que afasta: as instruções exigem que a combinação por modo "seja decidida na montagem, não dentro das regras". Aqui a combinação não está no `Hotel`. Ela está no próprio envio, que recebe o modo de teste e decide quais peças entram (`EmailSender.java:12-14`, `39-41`). Ela está também numa das regras: o `TestModeHandler` sempre entra na lista e pergunta pelo modo dentro dele (`TestModeHandler.java:19`). A cópia tem o mesmo tipo de interruptor interno (`ArchiveCopyHandler.java:19`).

Isso é o que as instruções descrevem como malfeito: o envio ainda pergunta pelo modo de teste, e a combinação por modo está escrita dentro dos componentes. Uma instalação que queira "madrugada só no teste" ou "sem cópia" não consegue isso compondo peças. Ela precisa de um novo parâmetro no construtor do `EmailSender` e de um novo `if` em `buildHandlerChain`. O próprio `EMAIL_HANDLERS.md` do agente propõe `new EmailSender(sentMessages, testMode, archiveEnabled, blockNightTime, maxMessagesPerDay)`.

Exagero colateral: não há estrutura sobrando fora da parte do pattern. O `EmailContext` e o `MessageRecord` servem à corrente.

Comportamento, conferido por leitura: está quase todo certo. O desvio é o domínio validado só com `contains(".")` (`EmailValidator.java:28`).

## Item a item

**O pattern pedido.** As três regras viraram peças de uma corrente (`EmailValidator.java:10`, `TestModeHandler.java:8`, `ArchiveCopyHandler.java:8`), aplicadas em laço pelo envio (`EmailSender.java:20-24`). Mas a combinação por modo está dentro do envio: o `EmailSender` recebe `testMode` e decide com `if (!testMode)` se a cópia entra (`EmailSender.java:12`, `35-43`). A montagem só passa booleanos (`Hotel.java:23-25`). E as próprias regras carregam o interruptor de modo: o `TestModeHandler` está sempre na lista e verifica `testMode` (`TestModeHandler.java:19`), e o `ArchiveCopyHandler` verifica `enabled` (`ArchiveCopyHandler.java:19`). A estrutura existe, mas convive com a decisão por modo dentro do envio e das regras, que é o caso de malfeito previsto nas instruções.
Conclusão: malfeito

**O exagero colateral.** Não há. As classes novas (`EmailHandler`, `EmailContext` com `MessageRecord`, `MessageStatus` e os três handlers) servem à variação. O `EMAIL_HANDLERS.md` é documentação, não estrutura.
Conclusão: contenção

**O comportamento.** A conferência foi feita por leitura do código: não subi o serviço nem rodei os testes.
- **Sem `HOTEL_TEST_MODE`:** a confirmação gera a mensagem original `SENT` e, logo depois, a cópia para `arquivo@pousadamarealta.com.br`, com o assunto `"[Cópia] " + assunto` e o mesmo corpo (`ArchiveCopyHandler.java:20`, `EmailSender.java:26-30`). É o esperado.
- **Endereço inválido:** a corrente para no validador e a mensagem é gravada `BLOCKED`, com o endereço original e sem cópia (`EmailValidator.java:14-17`). É o esperado.
- **Com `HOTEL_TEST_MODE=true`:** a mensagem vai para `testes@pousadamarealta.com.br`, com `"[TESTE] "` no assunto e a primeira linha `"Destinatário original: …"`, sem a cópia (`TestModeHandler.java:22-25`, `EmailSender.java:39`). É o esperado.
- **Inválido no modo de teste:** o validador vem antes do redirecionamento, então a mensagem bloqueada mantém o endereço original. É o esperado.
- **`status` no `GET /messages`:** o campo vem do enum no record (`Message.java:3`).
- **O desvio:** a regra pede depois do @ "um nome com pelo menos um ponto no meio", mas o código só exige `domainPart.contains(".")` (`EmailValidator.java:28`). Com isso, `ana@gmail.` e `ana@.com` passam como válidos.
- **O resto do sistema:** só mudou o construtor do `EmailSender`, que foi ajustado nos testes que já existiam.
Conclusão: em parte

## Dúvidas

1. **A classe do pattern.** Hesitei entre `malfeito` e `alternativa equivalente`. A corrente com uma peça por regra está lá. O que decidiu foi a combinação por modo ficar no `EmailSender` e no `TestModeHandler`, e não no `Hotel`. Pesou também que o `EmailSender` recebe e consulta `testMode`, que as instruções citam explicitamente como malfeito.
2. **O comportamento.** O desvio da validação é um caso de borda, com o ponto no início ou no fim do domínio. Escolhi `em parte` porque a regra pedida fala em "ponto no meio", mas todos os casos principais conferem.
