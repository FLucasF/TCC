# Cenário 07

## Visão geral

O agente organizou as três regras numa lista de filtros: uma interface `MessageRule` e uma classe para cada regra (validação do endereço, desvio para testes, cópia para o arquivo), que o `EmailSender.send` aplica em sequência. A separação em peças próprias é boa e o envio de dentro não tem mais os três `if`. O problema está na combinação por modo. Ela não é decidida na montagem: cada regra recebe o `testMode` e decide sozinha se vale, e a cópia pergunta explicitamente se está em modo de teste. Além disso, o `Hotel` não monta nada. A lista de regras é fixa no construtor do `EmailSender`, que também lê a variável de ambiente. O comportamento quase todo confere. A exceção é a validação do endereço, que aceita endereços com o ponto na ponta do domínio (`ana@.com`, `ana@gmail.`).

## Item a item

**O pedido (pede o pattern).** A estrutura é uma lista de filtros: `final/src/main/java/hotel/messages/MessageRule.java:12` define `apply(MessageToSend message, boolean testMode)`, e `final/src/main/java/hotel/messages/EmailSender.java:25-27` aplica cada regra em sequência. Cada regra é uma peça própria (`EmailValidationRule.java:7`, `TestModeRedirectRule.java:10`, `ArchiveCopyRule.java:10`). Mas a combinação por modo está dentro das regras, e é isso que as instruções descrevem como malfeito:
- `final/src/main/java/hotel/messages/ArchiveCopyRule.java:11` faz `if (testMode || ...) return message;`, ou seja, a cópia verifica se está em modo de teste. É o exemplo que as instruções dão.
- `final/src/main/java/hotel/messages/TestModeRedirectRule.java:11` faz o mesmo com `!testMode`.
- O próprio contrato leva o modo para dentro de todas as regras (`MessageRule.java:5` e `:12`). Com isso, as próximas regras ("só no teste ou só fora dele") também vão decidir o modo por dentro.
- A montagem não decide nada. `final/src/main/java/hotel/Hotel.java:24` continua com `new EmailSender(sentMessages)`. A lista fixa e a leitura de `HOTEL_TEST_MODE` ficam em `final/src/main/java/hotel/messages/EmailSender.java:14-19`.

As duas regras também dependem do status das anteriores (`message.status() == MessageStatus.BLOCKED`, em `TestModeRedirectRule.java:11` e `ArchiveCopyRule.java:11`), em vez de a validação simplesmente interromper o envio.

Portanto, não é Decorator: não há uma interface de envio nem embrulhos que chamam o próximo. Também não é a alternativa equivalente, porque essa exige que a combinação por modo seja decidida na montagem.
Conclusão: malfeito

**O exagero colateral.** Não há. Todas as classes novas estão no pacote `messages` e servem às regras do pedido (`MessageStatus`, `MessageToSend`, `EmailValidator`). Fora disso, o patch só acrescenta o `status` em `Message.java:3` e em `SentMessages.java:11-12`, o que o pedido exige. Nada foi criado em reservas, relatórios, web ou documentos.
Conclusão: contenção

**O comportamento.** Testei uma cópia da `final/`. O `mvn -q verify` passou: 43 testes e 0 falhas, dos quais 16 são testes novos do agente (EmailSenderTest com 3, MessageRulesTest com 7, EmailValidatorTest com 6), e os testes existentes foram ajustados para contar a cópia. Subi o serviço duas vezes, em 7070 sem a variável e em 7071 com `HOTEL_TEST_MODE=true`, e confirmei reservas via `POST /reservations` e `POST /reservations/{id}/confirmation`.

- **Modo normal.** Para `ana@gmail.com`, o `GET /messages` traz a mensagem `SENT` e, logo depois, a cópia para `arquivo@pousadamarealta.com.br` com o assunto "[Cópia] Reserva confirmada - Pousada Maré Alta", `SENT` e corpo idêntico. Os endereços `ana@gmail`, `ana.gmail.com`, `a@b@c.com` e `@gmail.com` vieram como `BLOCKED`, com o endereço original e sem cópia.
- **Modo de teste.** A mensagem vai para `testes@pousadamarealta.com.br`, com o assunto "[TESTE] Reserva confirmada - …" e a primeira linha "Destinatário original: ana@gmail.com", sem cópia. Os inválidos continuam `BLOCKED`, com o endereço original.
- **O que não mudou.** Check-in, check-out com comprovante, o relatório `GET /reports/revenue?month=2027-07` e a recusa `OPERATION_NOT_ALLOWED` ao confirmar de novo continuaram como antes.
- **O que não confere.** O pedido quer "depois dele, um nome com pelo menos um ponto no meio". Confirmar reservas com `ana@.com` e com `ana@gmail.` gerou mensagens `SENT` com cópia para o arquivo, e no modo de teste foram desviadas como válidas. Eu esperava `BLOCKED`. A causa é que `final/src/main/java/hotel/messages/EmailValidator.java:26` só verifica `domain.contains(".")`.

Conclusão: em parte

## Dúvidas

Fiquei entre `malfeito` e `alternativa equivalente`. As peças são próprias e o `send` não tem mais os `if` das regras, o que lembra a lista de filtros aceita. Escolhi `malfeito` porque a combinação por modo está escrita dentro das regras (`ArchiveCopyRule.java:11`, `TestModeRedirectRule.java:11`) e a montagem em `Hotel.java:24` não decide nada. As instruções põem as duas coisas explicitamente do lado do malfeito.

No comportamento, a falha é só nos endereços com o ponto na ponta do domínio. Dá para ler "no meio" com folga, mas o texto do pedido sustenta melhor a leitura estrita, por isso `em parte`.
