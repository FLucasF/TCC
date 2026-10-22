# Cenário 07

## Visão geral

O agente tirou as três regras de dentro do envio e fez de cada uma uma classe própria (`EmailValidationRule`, `TestModeRule`, `ArchiveCopyRule`), todas atrás da interface `EmailRule`, e o `EmailSender` aplica essa lista de regras. Até aí parece a "lista de filtros" que as instruções aceitam. O problema mais sério é que não se escolhe na montagem qual regra vale em cada modo. A lista é sempre a mesma e fica escrita dentro do construtor do `EmailSender`. O que muda é que o modo de teste entra como `boolean` no envio e é repassado às regras, e cada regra decide sozinha se age. A cópia para o arquivo, por exemplo, verifica se está em modo de teste. As instruções do pattern dão exatamente esse caso como `malfeito`. Pela leitura do código, o comportamento pedido está quase todo certo. O desvio é pequeno: a validação aceita domínios com o ponto na ponta, como `ana@gmail.`.

## Como foi implementado, e por que o julgamento

**O projeto.** É um serviço Javalin. A classe `Hotel` (`final/src/main/java/hotel/Hotel.java:19-29`) monta as peças: `Rooms`, `ReservationStore`, `SentMessages`, `ReservationService` (que recebe `PriceCalculator` e `EmailSender`), os relatórios e a `HttpApi`, que expõe as rotas. Os dados ficam em memória. Uma requisição como `POST /reservations/{id}/confirmation` vai da `HttpApi` para `ReservationService`, que chama `emailSender.send(...)` (`final/src/main/java/hotel/reservations/ReservationService.java:69`). O `GET /messages` devolve `sentMessages.all()` em JSON (`final/src/main/java/hotel/web/HttpApi.java:81`). O agente fez o seguinte:

- Criou `MessageStatus` (`SENT`/`BLOCKED`) e o pôs no record `Message`.
- Mudou `SentMessages.add` para receber o status (`final/src/main/java/hotel/messages/SentMessages.java:11-12`).
- Escreveu dois testes novos, `EmailRulesTest` (regras isoladas, nos dois modos) e `TestModeIntegrationTest` (pelo `ReservationService`).
- Ajustou os testes antigos, que agora esperam a cópia.

Fora do código, acrescentou um `MUDANCAS.md` e um `exemplo-uso.sh`.

**A parte do pattern.** Na base, `EmailSender.send` só gravava a mensagem. Agora `send` cria um `OutboundMessage`, que é mutável (destinatário, assunto, corpo, status e uma lista de cópias). Depois passa esse objeto por cada regra da lista (`final/src/main/java/hotel/messages/EmailSender.java:21-24`). Por fim grava a principal e, logo depois, as cópias (`EmailSender.java:25-28`). As regras são estas:

- **`EmailValidationRule`** marca a mensagem como bloqueada se o endereço for inválido (`final/src/main/java/hotel/messages/EmailValidationRule.java:8-10`).
- **`TestModeRule`** guarda o `testMode` e só age se ele for verdadeiro (`final/src/main/java/hotel/messages/TestModeRule.java:15`). Quando age, troca o destinatário, prefixa "[TESTE] " e acrescenta a linha "Destinatário original: …" (`TestModeRule.java:18-21`).
- **`ArchiveCopyRule`** também guarda o `testMode` e não faz nada se ele for verdadeiro (`final/src/main/java/hotel/messages/ArchiveCopyRule.java:15`). Fora do teste, acrescenta a cópia (`ArchiveCopyRule.java:18-23`).

A lista é fixa, dentro do construtor do `EmailSender` (`EmailSender.java:13-17`), e o construtor recebe o modo como `boolean` (`EmailSender.java:11`). A `Hotel` só lê a variável de ambiente e repassa esse `boolean` (`Hotel.java:23,25`).

**O raciocínio.** O que aproxima o desenho do pattern:

- cada regra é uma peça própria, atrás de uma interface comum;
- o envio não tem mais os três `if`;
- uma regra nova (madrugada, limite por dia) entraria como mais uma classe.

O que o afasta:

- **A combinação por modo está dentro das regras.** `ArchiveCopyRule.java:15` e `TestModeRule.java:15` perguntam pelo modo, em vez de a montagem incluir ou não a peça.
- **O envio ainda depende do modo.** O `EmailSender` recebe o modo (`EmailSender.java:11`).
- **A montagem do sistema não escolhe nada.** A `Hotel` não decide quais regras entram. Uma instalação "sem cópia", que o pedido cita, exigiria mexer no `EmailSender` ou na regra.
- **As regras dependem da ordem.** Cada regra consulta o resultado da anterior: `TestModeRule` e `ArchiveCopyRule` testam `status() == BLOCKED`.

As instruções só aceitam a lista de filtros como alternativa equivalente se a combinação for decidida na montagem. Aqui ela não é. E dizem que é `malfeito` "a cópia que verifica se está em modo de teste". Por isso, `malfeito`.

No exagero colateral, nada pesou. `EmailRule` tem três implementações reais, e `OutboundMessage` faz parte da solução das regras. O `MUDANCAS.md` e o script são documentação, não estrutura.

No comportamento, pesou só o detalhe da validação, descrito abaixo.

## Item a item

**O pattern pedido.** As três regras viraram classes `EmailRule` (`final/src/main/java/hotel/messages/EmailRule.java:4-6`), aplicadas em laço pelo envio (`final/src/main/java/hotel/messages/EmailSender.java:22-24`). Mas a lista é fixa dentro do `EmailSender` (`EmailSender.java:13-17`), e o modo de teste entra no envio (`EmailSender.java:11`) e dentro das regras. A cópia verifica `testMode` (`ArchiveCopyRule.java:15`) e o redirecionamento também (`TestModeRule.java:15`). A montagem (`final/src/main/java/hotel/Hotel.java:23-25`) só repassa o `boolean` e não empilha nem escolhe peças. A estrutura existe, mas a combinação por modo ficou escrita nas regras, que é o caso de `malfeito` previsto nas instruções.
Conclusão: malfeito

**O exagero colateral.** Não há. `EmailRule` tem três implementações em uso, `OutboundMessage` e `MessageStatus` servem à solução, e não foi criada fábrica, builder nem camada de repasse. Os arquivos `MUDANCAS.md` e `exemplo-uso.sh` são documentação, não estrutura.
Conclusão: contenção

**O comportamento.** A conferência foi só por leitura do código: não subi o serviço nem chamei a API. Conferi o seguinte:

- **Endereço inválido.** É bloqueado com status `BLOCKED`, sem cópia, nos dois modos, e mantém o endereço original. O redirecionamento e a cópia pulam mensagens bloqueadas (`TestModeRule.java:15`, `ArchiveCopyRule.java:15`).
- **Fora do teste.** Sai a mensagem e logo depois a cópia para `arquivo@pousadamarealta.com.br`, com "[Cópia] " no assunto e o mesmo corpo (`ArchiveCopyRule.java:18-23`, `EmailSender.java:25-28`).
- **Com `HOTEL_TEST_MODE=true`.** A mensagem vai para `testes@…`, com "[TESTE] " no assunto e "Destinatário original: …" como primeira linha, sem cópia (`TestModeRule.java:18-21`, `Hotel.java:23`).
- **Campo `status`.** Está no record `Message`, e o `GET /messages` o serializa.
- **O resto do sistema.** O único chamador do envio continua igual (`ReservationService.java:69`).

O desvio: a validação só exige que o domínio contenha um ponto (`EmailValidationRule.java:21-22`). Assim, `ana@gmail.` e `ana@.com` passam como válidos, e o pedido diz "um ponto no meio". É um caso de borda; os exemplos do pedido (`ana@gmail`, `ana.gmail.com`) são bloqueados.
Conclusão: em parte

## Dúvidas

- **Lista de filtros ou malfeito.** Pesei se a lista de filtros não seria `alternativa equivalente`, porque cada regra é uma peça própria. Fiquei com `malfeito` porque a combinação por modo está dentro das regras e do envio, e não na montagem, que é o caso que as instruções citam.
- **O comportamento.** Pesei se o desvio da validação, um caso de borda, deveria rebaixar o comportamento para `em parte` ou ficar só como nota com `segue`. Fiquei com `em parte` porque a regra do endereço foi pedida de forma explícita.
