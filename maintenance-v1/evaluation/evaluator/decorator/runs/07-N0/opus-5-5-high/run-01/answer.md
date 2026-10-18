# Cenário 07

## Visão geral

O agente colocou as três regras dentro de `EmailSender.send`, uma depois da outra: o bloqueio do endereço, o desvio do modo de teste e a cópia para o arquivo. O modo de teste é lido da variável de ambiente no construtor do próprio envio. Não há interface de envio, nenhum embrulho e nenhuma montagem por modo em `Hotel`. É a omissão que as instruções descrevem: as regras de madrugada e de limite diário, já anunciadas, vão precisar de mais `if` no mesmo método. O que está bom: o campo `status` na lista, a cópia logo depois da original e o desvio no modo de teste funcionam, e o `mvn verify` passa. O problema mais sério de comportamento está no validador: ele aceita endereços com o ponto na beira do domínio, como `ana@.com` e `ana@gmail.`, e essas mensagens saem.

## Item a item

**O pedido (pede o pattern).** As três regras são três trechos seguidos do mesmo método. O bloqueio do endereço inválido está em `final/src/main/java/hotel/messages/EmailSender.java:19-21`. O desvio do modo de teste está em `final/src/main/java/hotel/messages/EmailSender.java:24-28`. A cópia para o arquivo está em `final/src/main/java/hotel/messages/EmailSender.java:31-35`. O modo é lido dentro do envio, com `System.getenv("HOTEL_TEST_MODE")`, em `final/src/main/java/hotel/messages/EmailSender.java:14`. A combinação por modo ("cópia só fora do teste") também fica escrita ali: o `return` antecipado do modo de teste em `:27` é o que impede a cópia. A montagem do sistema não mudou: `final/src/main/java/hotel/Hotel.java:24` continua criando um único `new EmailSender(sentMessages)`, sem empilhar nada. `EmailSender` continua sendo uma classe concreta `final` (`:4`), e `ReservationService` depende dela diretamente (`final/src/main/java/hotel/reservations/ReservationService.java:17`), então não há como embrulhar. A única peça separada é o `EmailValidator` (`final/src/main/java/hotel/messages/EmailValidator.java:10`), uma função estática chamada de dentro do `send`. Ela não é um embrulho nem um filtro que a montagem compõe. É o caso descrito como omissão: "os três `if` (o endereço, o modo de teste, a cópia) ficam dentro do `send`".
Conclusão: omissão

**O exagero colateral.** Nenhuma estrutura nova fora do que o pedido tocava. As peças novas são o `MessageStatus` (`final/src/main/java/hotel/messages/MessageStatus.java:3`), pedido explicitamente, e o `EmailValidator`, que é uma função utilitária da própria regra pedida. O agente também criou `TEST_MODE_DEMO.md`, que é só documentação.
Conclusão: contenção

**O comportamento.** O `mvn -q verify` da cópia passou, com 35 testes e nenhuma falha. São 6 classes, e os testes novos do agente estão em `EmailSenderTest` (2), `EmailValidatorTest` (6) e no caso novo de `ReservationServiceTest`; os testes antigos foram ajustados para contar a cópia. Subi o serviço duas vezes, na porta 7070 sem a variável e na 7071 com `HOTEL_TEST_MODE=true`. Em cada uma, criei reservas pelo `POST /reservations`, confirmei com `POST /reservations/{id}/confirmation` e conferi o `GET /messages`.

O que conferiu:
- **Modo normal, endereço válido.** `carla@example.com` gerou a mensagem `SENT` para o hóspede e, logo depois, a cópia `SENT` para `arquivo@pousadamarealta.com.br`. O assunto da cópia é "[Cópia] Reserva confirmada - Pousada Maré Alta", e o corpo é idêntico ao da original (`.[0].body == .[1].body` deu `true`).
- **Modo normal, endereço inválido.** `ana@gmail` e `ana.gmail.com` ficaram `BLOCKED`, com o endereço original e sem cópia.
- **Modo de teste.** A mensagem foi para `testes@pousadamarealta.com.br`, com "[TESTE] " no assunto, primeira linha "Destinatário original: carla@example.com" e sem cópia. Os endereços inválidos continuaram `BLOCKED`, com o endereço original.
- **O que não mudou.** Check-in, check-out com o comprovante, cancelamento `NON_REFUNDABLE` com reembolso 0,00, a recusa `OPERATION_NOT_ALLOWED` ao reconfirmar e os relatórios de receita e ocupação continuam iguais.

O que não conferiu:
- **O validador.** O pedido exige, depois do @, "um nome com pelo menos um ponto no meio". O código só verifica `domain.contains(".")` (`final/src/main/java/hotel/messages/EmailValidator.java:30`). Criei e confirmei uma reserva com `ana@.com`: esperava `BLOCKED`, e veio `SENT`, com cópia no modo normal e desviada para a caixa de testes no modo de teste. Com `ana@gmail.` aconteceu o mesmo: veio `SENT` com cópia (mensagens 7 e 8).

As outras regras conferem; o validador é a regra que não segue o pedido.
Conclusão: em parte

## Dúvidas

Sobre o desenho, nenhuma: não existe estrutura de embrulho nem de filtros que pudesse ser lida como `malfeito` ou `alternativa equivalente`. Sobre o comportamento, há uma leitura possível: "um ponto no meio" pode ser entendido de forma frouxa, como "tem um ponto". Considerei que "no meio" exclui o ponto no começo ou no fim do domínio, que é a leitura natural do exemplo `ana@gmail.com`. Por isso a conclusão do comportamento é `em parte`.
