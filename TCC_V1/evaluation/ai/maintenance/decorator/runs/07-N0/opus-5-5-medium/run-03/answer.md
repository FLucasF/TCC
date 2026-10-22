# Cenário 07

## Visão geral

O agente transformou as regras de envio numa lista de filtros. Cada regra é um objeto próprio que implementa a interface `SendRule`. O `EmailSender` percorre a lista sem saber quais regras ela tem, e a classe `Hotel` decide quais regras entram e em qual modo cada uma vale, por uma especificação com escopo (`always`, `test`, `normal`). Não é um Decorator: não há um embrulho chamando o próximo. Mas é o que as instruções aceitam como alternativa: cada regra é uma peça própria, e a combinação por modo é decidida na montagem, não dentro das regras. Nenhuma regra e nenhum ponto do envio pergunta pelo modo de teste. Não encontrei um problema sério. A ressalva é que o envio tem quatro etapas fixas (bloquear, reescrever, registrar, copiar), e uma regra nova precisa caber em uma delas.

## Como foi implementado, e por que o julgamento

**O projeto.** É um serviço Javalin organizado em pacotes:
- `rooms`: os quartos.
- `reservations`: `ReservationService`, `PriceCalculator`, `ReservationStore` e os tipos de reserva.
- `reports`: os relatórios CSV.
- `documents`: o recibo.
- `messages`: o envio de e-mails.
- `web`: `HttpApi`, com as rotas.

Uma requisição chega numa rota da `HttpApi` e vai para o `ReservationService`. A confirmação da reserva (`final/src/main/java/hotel/reservations/ReservationService.java:69`) é o único lugar que chama `emailSender.send`. O `GET /messages` devolve `sentMessages.all()` em JSON (`final/src/main/java/hotel/web/HttpApi.java:81`). Os dados ficam em memória: `ReservationStore` guarda as reservas e `SentMessages` as mensagens.

A classe `Hotel` monta as peças e o `main` a sobe. O agente acrescentou um construtor `Hotel(Map env, Clock clock)`, que lê `HOTEL_TEST_MODE` e `HOTEL_MESSAGE_RULES` e monta o `EmailSender` com a lista de regras (`final/src/main/java/hotel/Hotel.java:31-37`).

Nos testes, o agente criou o `EmailSenderTest`. Ele cobre a validação de endereço, o bloqueio sem cópia, a cópia logo depois da original, o modo de teste sem cópia e com bloqueio, e o escopo das regras de madrugada e de limite por dia. O agente também ajustou o `HttpApiTest`: a confirmação agora gera duas mensagens, e a primeira tem `status` `SENT`.

**A parte do pattern.**
- `SendRule` (`final/src/main/java/hotel/messages/SendRule.java:7-27`) é uma interface com quatro ganchos, todos opcionais: `blockReason`, `rewrite`, `copiesOf` e `sent`.
- Cada regra é um objeto separado, criado em `SendRules`:
  - `validAddress()` só bloqueia (`final/src/main/java/hotel/messages/SendRules.java:44-51`).
  - `testRedirect()` só reescreve o destino, o assunto e o corpo (`SendRules.java:53-63`).
  - `archiveCopy()` só gera a cópia (`SendRules.java:65-73`).
  - `quietHours` e `dailyLimit` já vêm prontas (`SendRules.java:76-111`).
- O modo entra numa peça só: `scoped(rule, scope, testMode)` devolve a regra ou uma regra que não faz nada (`SendRules.java:113-115`), usando `Scope.applies` (`final/src/main/java/hotel/messages/Scope.java:9-11`).
- `parse` (`SendRules.java:123-147`) traduz a especificação na lista. O padrão `valid-address,test-redirect:test,archive:normal` (`SendRules.java:19`) reproduz exatamente as combinações pedidas. A outra pousada, que não quer a cópia, só muda a especificação.
- O `EmailSender.send` (`final/src/main/java/hotel/messages/EmailSender.java:25-45`) percorre a lista em quatro passadas: primeiro pergunta se alguma regra bloqueia, depois aplica as reescritas, registra a mensagem, avisa as regras de que ela foi enviada e registra as cópias. Ele não tem nenhum `if` sobre um caso concreto nem sobre o modo.

**O raciocínio.**
- O que afasta do pattern: não é um Decorator. As regras não têm a mesma interface do envio nem embrulham um "próximo".
- O que aproxima: o desenho é exatamente a "lista de filtros aplicada pelo envio" que as instruções aceitam.
  - Cada regra é uma peça própria.
  - Nenhuma regra conhece o modo. A cópia não verifica se está em teste: quem decide é `scoped` e o escopo `archive:normal`.
  - O envio de dentro não pergunta pelo modo.
  - A combinação é decidida na montagem (`Hotel.java:37`).
  - As três regras pedidas estão na estrutura, e nenhuma ficou como `if` solto.
  - O valor padrão de escopo do `test-redirect` fica em `parse` (`SendRules.java:135-138`), que é a camada de configuração, não a regra.
- Por isso a classe é `alternativa equivalente`.
- Exagero colateral: o mini-formato de especificação e as regras de madrugada e de limite vão além do mínimo. Mas estão dentro da parte que o pattern resolve, e o pedido anuncia as duas regras e as combinações por instalação. Fora dela não vi camada inútil. `Outgoing` e `MessageStatus` têm uso real.
- Comportamento: a leitura confere com as regras pedidas.

## Item a item

**O pattern pedido.** As regras viraram objetos `SendRule` independentes (`final/src/main/java/hotel/messages/SendRules.java:44-73`). O `EmailSender.send` percorre a lista sem saber qual regra é qual (`final/src/main/java/hotel/messages/EmailSender.java:27-43`). O modo é aplicado por fora das regras, em `scoped`/`Scope` (`SendRules.java:113-115`, `Scope.java:9-11`), e a combinação é escolhida na montagem (`final/src/main/java/hotel/Hotel.java:32-37`, com o padrão em `SendRules.java:19`). Não é Decorator, porque não há embrulhos encadeados. É uma lista de filtros com cada regra numa peça e a combinação na montagem, que é o que as instruções aceitam como equivalente. Não há pergunta pelo modo dentro de nenhuma regra nem no envio.
Conclusão: alternativa equivalente

**O exagero colateral.** Não há estrutura sem necessidade fora da parte do pattern. A especificação `HOTEL_MESSAGE_RULES` com seu parser (`SendRules.java:123-157`) e as regras `quietHours`/`dailyLimit` (`SendRules.java:76-111`) vão um pouco além do pedido imediato. Mas são a própria parte da variação, e o pedido anuncia essas regras e as combinações por instalação. Também há miudezas: o construtor sem regras (`EmailSender.java:16-18`) e o motivo de bloqueio, que é calculado e nunca é usado (`SendRule.java:10`). Nada disso é uma camada a mais.
Conclusão: contenção

**O comportamento.** A conferência foi feita só pela leitura do código. Não subi o serviço nem chamei a API.
- **Validação de endereço** (`SendRules.java:25-42`): exige um único `@` com algo antes e, no domínio, um ponto que não seja o primeiro nem o último caractere. `ana@gmail.com` é aceito; `ana@gmail`, `ana.gmail.com` e `a@.com` são recusados.
- **Mensagem bloqueada**: é registrada com o endereço, o assunto e o corpo originais e o status `BLOCKED`, e não gera cópia, porque o envio sai antes (`EmailSender.java:27-31`). O bloqueio também vale no modo de teste, já que `valid-address` não tem escopo e vale sempre.
- **Mensagem enviada**: é registrada como `SENT` e, logo depois, vem a cópia para `arquivo@pousadamarealta.com.br`, com o assunto `"[Cópia] "` mais o original e o mesmo corpo (`EmailSender.java:35-43`, `SendRules.java:68-71`).
- **Modo de teste**: com `HOTEL_TEST_MODE=true` (`Hotel.java:32`), o destino passa a ser `testes@pousadamarealta.com.br`, o assunto ganha `"[TESTE] "` e o corpo começa com `"Destinatário original: …\n"` (`SendRules.java:56-61`). Nesse modo não há cópia, porque o padrão é `archive:normal`.
- **Campo `status`**: está no `Message` (`final/src/main/java/hotel/messages/Message.java:3`) e sai em JSON como `SENT`/`BLOCKED`.
- **O resto do sistema**: o único remetente continua sendo a confirmação da reserva, e o resto não mudou.
Conclusão: segue

## Dúvidas

O `EmailSender` fixa quatro etapas para as regras: bloquear, reescrever, avisar o envio e copiar. Uma regra futura que não caiba nelas (por exemplo, adiar a mensagem para depois da madrugada em vez de bloqueá-la) obrigaria a mexer no próprio envio. Isso limita a extensão um pouco mais do que um Decorator limitaria. Mesmo assim, as regras pedidas e as anunciadas cabem nessas etapas, e o desenho continua dentro do que as instruções aceitam como equivalente.
