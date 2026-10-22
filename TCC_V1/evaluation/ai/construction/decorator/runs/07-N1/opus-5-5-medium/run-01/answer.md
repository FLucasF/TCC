# Cenário 07

## Visão geral

O agente montou as regras de envio como uma lista de filtros. Cada regra é uma classe própria que implementa `MessageRule`: a validação do endereço, a cópia para o arquivo e o desvio do modo de teste. As regras agem sobre um contexto mutável da mensagem. Um `MessageProcessor` aplica a lista em ordem, e quem decide a combinação por modo é o `App.main` (`App.java:249-260`): a validação sempre, e depois o desvio no modo de teste ou a cópia fora dele. Nenhuma regra e nenhum envio pergunta pelo modo. Isso é a forma de "lista de filtros aplicada pelo envio" que as instruções aceitam como equivalente. O problema mais sério está no comportamento, não no desenho. A validação do endereço só confere se existe um ponto em algum lugar depois do @, então aceita `ana@gmail.` e `ana@.com`. Há também falhas menores fora das mensagens, como corpo que não é JSON resultar provavelmente em 500 e valores zero saírem sem as duas casas.

## Como foi implementado, e por que o julgamento

**O projeto.** Tudo fica num único módulo Maven, no pacote `com.pousadamarealta`:
- `App` monta as peças e declara todas as rotas Javalin em `main`, com o tratamento de erro em `try/catch` em cada rota.
- `domain` tem `Room`, `RoomType` (capacidade e diária no enum), `Reservation` (estado e transições `canConfirm`/`canCancel`/…), `Message`, `MessageStatus` e `MessageChannel`.
- `repository` tem repositórios em memória: `RoomRepository` com os cinco quartos fixos, `ReservationRepository` com as consultas de disponibilidade e de relatório, e `MessageRepository`, que é a lista de mensagens.
- `service` tem `PricingService` (alta temporada, desconto e arredondamento `HALF_EVEN`), `ReservationService` (regras de reserva, texto do comprovante e e-mail de confirmação), `ReportService` (os dois CSV) e `MessageService` (o envio).

O caminho de uma requisição é: rota em `App` → `ReservationService` → repositórios. Na confirmação, `ReservationService.sendConfirmationEmail` (`service/ReservationService.java:159-174`) chama `MessageService.send`. Os dados ficam só em memória. O sistema sobe por `App.main`, que lê `PORT` e `HOTEL_TEST_MODE` (`App.java:239-247`). Os testes do agente são unitários de preço e de cada regra de mensagem, um teste de integração do `MessageProcessor` nas duas combinações e um `AppIT` com três cenários HTTP.

**A parte que pede o pattern.**
- `MessageRule` é a interface de cada regra (`messaging/MessageRule.java:3-5`).
- `MessageRuleContext` carrega o destinatário original e o atual, o assunto, o texto, o `status` e uma lista de mensagens adicionais (`messaging/MessageRuleContext.java:9-26,72-74`).
- `EmailValidationRule` marca `BLOCKED` quando o endereço é inválido (`messaging/rules/EmailValidationRule.java:10-14`).
- `ArchiveCopyRule` acrescenta uma mensagem adicional para o arquivo se o status for `SENT` (`messaging/rules/ArchiveCopyRule.java:11-15`).
- `TestModeRedirectRule` reescreve destinatário, assunto e texto se o status for `SENT` (`messaging/rules/TestModeRedirectRule.java:11-16`).
- `MessageProcessor` percorre a lista (`messaging/MessageProcessor.java:12-17`).
- `MessageService.send` cria o contexto, passa pelo processador e grava a mensagem principal e depois as adicionais (`service/MessageService.java:20-45`). Ele não tem nenhum `if` de regra ou de modo.
- A escolha por modo está só em `App.createMessageProcessor` (`App.java:249-260`).

**O raciocínio do julgamento.** Não é um Decorator: as regras não embrulham o envio nem chamam o próximo. Mas cada regra é uma peça própria, a combinação por modo é decidida na montagem e nenhuma regra conhece o modo. A cópia não pergunta se está em teste, e o desvio é simplesmente adicionado à lista no modo de teste. Isso é a "lista de filtros aplicada pelo envio" que as instruções aceitam de antemão. As regras novas anunciadas (madrugada, limite por dia) cabem como novas `MessageRule` que marcam `BLOCKED`, e a segunda pousada sem cópia é só não incluir `ArchiveCopyRule` na lista. O que afasta um pouco do ideal é o acoplamento pela ordem e pelo estado compartilhado: cópia e desvio dependem de a validação ter rodado antes e de lerem `status`. Mas essa ordem também é decidida na montagem. No exagero colateral não pesa nada: `MessageProcessor` e o contexto são a própria estrutura da alternativa, e fora dela não há interfaces de uma implementação só, fábricas ou camadas de repasse. No comportamento pesam a validação frouxa do endereço e alguns desvios do contrato HTTP fora da parte das mensagens.

## Item a item

**O pattern pedido.** As três regras são classes separadas que implementam `MessageRule` (`messaging/rules/EmailValidationRule.java:7`, `messaging/rules/ArchiveCopyRule.java:7`, `messaging/rules/TestModeRedirectRule.java:7`). Elas são aplicadas em sequência por `MessageProcessor` (`messaging/MessageProcessor.java:13-15`), e a combinação por modo é montada em `App.java:249-260`. O envio (`service/MessageService.java:20-45`) não pergunta pelo modo nem pelo caso, e nenhuma regra verifica o modo de teste. Não são embrulhos com a mesma interface do envio, então não é o Decorator em si. É a lista de filtros com combinação na montagem, que as instruções aceitam como equivalente.
Conclusão: alternativa equivalente

**O exagero colateral.** Não há. `MessageProcessor` e `MessageRuleContext` fazem parte da solução da variação. Fora dela, os repositórios e serviços são classes concretas sem interfaces sobrando, e não há fábricas, builders ou camadas que só repassam.
Conclusão: contenção

**O comportamento.** A conferência foi feita por leitura do código: não subi o serviço nem chamei a API.

O que confere:
- Endereço inválido fica `BLOCKED` com o endereço original, porque a validação não altera `to` e o desvio só age em `SENT` (`messaging/rules/TestModeRedirectRule.java:12`). Mensagem bloqueada não gera cópia (`messaging/rules/ArchiveCopyRule.java:12`).
- Fora do teste, a cópia vai para `arquivo@pousadamarealta.com.br` com "[Cópia] " e o mesmo texto, gravada logo depois da original (`service/MessageService.java:32-44`).
- Com `HOTEL_TEST_MODE=true`, o destinatário vira `testes@…`, o assunto ganha "[TESTE] ", o texto começa com "Destinatário original: …" e não há cópia (`messaging/rules/TestModeRedirectRule.java:13-15`, `App.java:253-257`).
- `GET /messages` traz `status` (`App.java:200`).
- O e-mail de confirmação e o comprovante seguem o texto pedido (`service/ReservationService.java:159-219`).
- Preços, transições de estado, reembolso e os relatórios conferem com as regras.

O que não confere:
- A validação aceita qualquer ponto depois do @ (`messaging/rules/EmailValidationRule.java:31-32`). Assim, `ana@gmail.` e `ana@.com` passam como válidos, quando o pedido era um ponto "no meio" do nome.
- Corpo que não é JSON não é tratado: `ctx.bodyAsClass` lança uma exceção do Jackson, que só seria pega pelo `catch` de `IllegalArgumentException` se o Javalin a convertesse (`App.java:69,104`). Provavelmente vira 500 em vez de 400 `INVALID_REQUEST`.
- `discount`, `amountPaid` e `refund` com `BigDecimal.ZERO` saem como `0`, não `0.00` (`service/PricingService.java:280`, `domain/Reservation.java:157`, `service/ReservationService.java:109`).
- `GET /rooms` sai na ordem de um `HashMap` (`repository/RoomRepository.java:21`).
- O `formatMoney` depende do locale padrão da JVM (`service/ReservationService.java:230`).

Conclusão: em parte

## Dúvidas

- Se a ordem das regras e o estado compartilhado contam contra a equivalência. Cópia e desvio só funcionam se a validação vier antes, e as cópias herdam o `status` do contexto da mensagem principal (`service/MessageService.java:41`). Uma regra futura que bloqueie depois da cópia deixaria uma cópia marcada `BLOCKED`. Como a ordem é decidida na montagem e nenhuma regra conhece o modo, fiquei com `alternativa equivalente`, não com `malfeito`.
- Se o corpo que não é JSON dá mesmo 500 depende de como o Javalin 6.7.0 propaga a exceção do Jackson. Pela leitura, nenhum `catch` em `App.java` a captura diretamente.
