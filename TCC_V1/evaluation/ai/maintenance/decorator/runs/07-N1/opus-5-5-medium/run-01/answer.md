# Cenário 07

## Visão geral

O agente pôs as três regras do e-mail (bloqueio de endereço inválido, desvio para a caixa de testes e cópia para o arquivo) como `if`s em sequência dentro de `SentMessages.add`, o método que todo envio usa para guardar a mensagem. A combinação por modo também ficou ali dentro, num `if (!config.isTestMode() && ...)`. Não existe interface de envio, não há embrulhos e a montagem em `Hotel` não decide nada sobre as regras. As regras anunciadas (madrugada, limite por dia, outra combinação na segunda pousada) só podem entrar como mais um `if` nesse método. O validador de endereço e o campo `status` estão certos. O problema mais sério de comportamento é a cópia: ela depende de uma variável de ambiente nova, `HOTEL_ARCHIVE_EMAIL`, que o pedido não menciona. No sistema como ele sobe, sem essa variável, nenhuma cópia é gerada e o endereço `arquivo@pousadamarealta.com.br` não aparece em nenhum lugar do código.

## Como foi implementado, e por que o julgamento

**O projeto.** É o mesmo sistema da base, com estes pacotes:

- `hotel.rooms`: os quartos.
- `hotel.reservations`: o serviço de reservas, o cálculo de preço e o armazenamento em memória (`ReservationStore`).
- `hotel.documents`: o recibo.
- `hotel.reports`: os relatórios de ocupação e de receita.
- `hotel.messages`: o envio de e-mail.
- `hotel.web`: a API Javalin (`HttpApi`).

A classe `Hotel` monta as peças à mão e sobe o servidor na porta de `PORT` (`final/src/main/java/hotel/Hotel.java:20-29` e `:41-44`). Uma requisição chega numa rota do `HttpApi` e vai para o `ReservationService`. Quando há uma confirmação, por exemplo, o serviço chama `EmailSender.send` (`final/src/main/java/hotel/messages/EmailSender.java:12-14`), que só repassa para `SentMessages.add`. Os dados ficam em listas em memória. O `GET /messages` serializa `sentMessages.all()` (`final/src/main/java/hotel/web/HttpApi.java:81`).

As peças novas são três:

- `EmailConfig` (`final/src/main/java/hotel/messages/EmailConfig.java:3-29`), que guarda o modo de teste e um endereço de arquivo lido do ambiente.
- `EmailValidator` (`final/src/main/java/hotel/messages/EmailValidator.java:3-23`).
- `MessageStatus`, com `SENT` e `BLOCKED`, que virou um campo novo do record `Message`.

Os testes novos são `EmailValidationTest`, `TestModeIntegrationTest`, `ReservationServiceWithArchiveTest`, `EmailIntegrationTest` e um caso a mais em `HttpApiTest`. Quase todos montam `SentMessages` com um `EmailConfig` feito à mão.

**A parte que pede o pattern.** O envio continua sendo uma classe concreta só, `EmailSender`, sem interface. Todas as regras moram em `SentMessages.add` (`final/src/main/java/hotel/messages/SentMessages.java:15-42`):

- **Bloqueio:** um `if` sobre `EmailValidator.isValid(to)` desvia para `addBlocked` (`:16-18`).
- **Modo de teste:** um `if (config.isTestMode())` troca o destinatário, o assunto e o corpo (`:24-28`).
- **Cópia:** um `if (!config.isTestMode() && config.archiveEmail() != null)` acrescenta a cópia (`:34-39`).

Nenhuma regra é uma peça própria. A ordem e a combinação por modo estão escritas dentro do método, e o próprio guardador da lista pergunta pelo modo. `Hotel` só lê a configuração e a entrega a `SentMessages` (`final/src/main/java/hotel/Hotel.java:22-23`), sem empilhar nada.

**O raciocínio.** As instruções do pattern dizem que é omissão quando "os três `if` (o endereço, o modo de teste, a cópia) ficam dentro do envio". É exatamente o caso, porque `SentMessages.add` é o ponto por onde passa todo envio. Nada aproxima o desenho do Decorator, nem de uma corrente ou lista de filtros: não há interface comum, não há uma peça por regra e não há combinação decidida na montagem. Não chega a ser malfeito porque não existe nenhuma estrutura do pattern convivendo com o jeito antigo. O que existe é só o jeito sem pattern.

No exagero colateral, `EmailConfig` é um objeto de dados simples que de fato carrega a configuração. `EmailValidator` é uma função estática pequena. Nenhuma das duas é estrutura sem necessidade.

No comportamento, pesou a cópia para o arquivo. O pedido manda gerá-la sempre fora do teste, para um endereço fixo. O código só a gera quando `HOTEL_ARCHIVE_EMAIL` está definida (`final/src/main/java/hotel/messages/EmailConfig.java:26` e `final/src/main/java/hotel/messages/SentMessages.java:34`).

## Item a item

**O pattern pedido.** Não há Decorator nem uma estrutura equivalente. Os três comportamentos são `if`s seguidos em `SentMessages.add`: o bloqueio em `final/src/main/java/hotel/messages/SentMessages.java:16-18`, o modo de teste em `:24-28` e a cópia em `:34-39`. A combinação por modo está escrita dentro do próprio método (`:34`, `!config.isTestMode()`). `EmailSender` continua concreto, sem interface (`final/src/main/java/hotel/messages/EmailSender.java:4-14`). A montagem não escolhe regras (`final/src/main/java/hotel/Hotel.java:22-25`). Uma regra nova, como a da madrugada ou o limite por dia, ou uma instalação sem a cópia, exigiria mexer nesse método. Pelas instruções, isso é omissão.
Conclusão: omissão

**O exagero colateral.** Não há. `EmailConfig` (`final/src/main/java/hotel/messages/EmailConfig.java:3-29`) é um portador de configuração usado de verdade, e `EmailValidator` (`final/src/main/java/hotel/messages/EmailValidator.java:3-23`) é um utilitário estático. Não há interfaces com uma implementação só, fábricas, builders nem camadas de repasse. O agente também apagou dois comentários de Javadoc da base (em `Hotel` e em `SentMessages`), o que é só ruído e não é estrutura.
Conclusão: contenção

**O comportamento.** A conferência foi feita só pela leitura do código, sem subir o serviço.

O que segue o pedido:

- **Validador:** exige um único @, algo antes dele, algo depois dele, e um ponto no domínio que não fique na ponta (`final/src/main/java/hotel/messages/EmailValidator.java:8-22`). Isso confere com o pedido: "ana@gmail" e "ana.gmail.com" são recusados.
- **Bloqueio:** a mensagem bloqueada sai com o endereço original e `BLOCKED`, sem cópia, também no modo de teste, porque a validação vem antes de tudo (`final/src/main/java/hotel/messages/SentMessages.java:16-18` e `:44-49`).
- **Modo de teste:** com `HOTEL_TEST_MODE=true`, a mensagem vai para `testes@pousadamarealta.com.br`, com "[TESTE] " no assunto e "Destinatário original: …" como primeira linha, e sem cópia (`:24-28` e `:34`).
- **Status:** o campo `status` aparece no JSON do `GET /messages` porque foi acrescentado ao record `Message`.

O que não segue:

- **A cópia não sai na configuração padrão.** Ela só é feita quando a variável `HOTEL_ARCHIVE_EMAIL` está definida (`final/src/main/java/hotel/messages/EmailConfig.java:26` e `final/src/main/java/hotel/messages/SentMessages.java:34`). Sem ela, que é o caso em que o pedido manda copiar "toda mensagem enviada", nenhuma cópia é gerada. O endereço `arquivo@pousadamarealta.com.br` não existe no código de produção. O próprio teste `EmailIntegrationTest`, que sobe o `Hotel` real, confirma uma confirmação válida com uma mensagem só na lista (`final/src/test/java/hotel/web/EmailIntegrationTest.java:58-66`).

O resto do sistema continua funcionando. Os testes antigos foram ajustados só para o construtor novo de `SentMessages`.

Como a regra da cópia, uma das três pedidas, não acontece no sistema como ele sobe, o comportamento segue só em parte.
Conclusão: em parte

## Dúvidas

O agente pode ter entendido o endereço configurável como um jeito de atender "a segunda pousada não quer a cópia". Mesmo assim, a ausência da variável desliga a cópia na instalação principal, contra o texto do pedido. Por isso fiquei com "em parte", e não com "segue". Entre "em parte" e "não segue", escolhi "em parte" porque as outras regras e o campo `status` conferem.
