# Cenário 07

## Visão geral

O agente fez as regras de envio como uma lista de filtros. Cada regra é uma classe própria que implementa `MessageRule`. A instalação monta a lista em um único lugar (`MessageRules`), e cada regra entra marcada com o modo em que vale: sempre, só no teste ou só fora dele. Ao ser criado, o `MessagePipeline` fica só com as regras do modo atual. Nenhuma regra pergunta pelo modo de teste, e o `Outbox` não tem nenhum `if` de regra de negócio. As três regras pedidas estão na estrutura. As duas anunciadas (madrugada e limite por dia) também já existem como peças, mas ainda não estão ligadas. Não é um Decorator no sentido estrito, porque as regras não embrulham o envio com a mesma interface. É a lista de filtros que as instruções aceitam de antemão. Nenhum problema sério apareceu. O ponto mais fraco é um risco latente: o `Outbox` grava as cópias pedidas mesmo quando a mensagem fica bloqueada, e isso só daria errado se uma regra que bloqueia fosse posta depois da cópia.

## Como foi implementado, e por que o julgamento

**O projeto.** O código fica em `br.com.pousadamarealta.reservas` e está dividido assim:
- `domain`: quarto, catálogo, reserva com a máquina de estados, preço (`Pricing`, `Money` com `HALF_EVEN`), plano de tarifa e códigos de erro.
- `service`: `ReservationService` com os casos de uso, mais o comprovante (`Receipt`, `ReceiptText`) e o e-mail de confirmação (`ConfirmationEmail`).
- `report`: os dois CSV.
- `messaging`: o envio e as regras.
- `web`: `App` com as rotas Javalin, `Config` com `PORT` e `HOTEL_TEST_MODE`, `Requests` com o parse e a validação do corpo, e `ReservationView`.

A requisição entra pela rota em `App`, passa por `ReservationService` e chega à `Reservation`, que guarda as transições e o valor pago. Os dados ficam em listas na memória, dentro do serviço e do `Outbox`. O `Main` lê o `Config` e chama `App.create(config).start(port)` (`src/main/java/br/com/pousadamarealta/reservas/Main.java:12-13`). Os testes do agente são três:
- `ApiTest`: ciclo completo, cancelamento não reembolsável, erros, e-mail inválido bloqueado na confirmação e lista de quartos.
- `MessagePipelineTest`: modo normal com cópia, bloqueio sem cópia, modo de teste com e sem bloqueio, e combinação por instalação.
- `PricingTest`: alta temporada, desconto e limites das temporadas.

**A parte que pede o pattern.** Funciona assim:
- `MessageRule` tem um único método, `apply(Draft)` (`src/main/java/br/com/pousadamarealta/reservas/messaging/MessageRule.java:4-7`).
- O `Draft` é a mensagem em preparo. As regras podem bloqueá-la, redirecioná-la ou pedir cópias (`src/main/java/br/com/pousadamarealta/reservas/messaging/Draft.java:23-36`).
- Cada regra é uma classe:
  - `ValidAddressRule` bloqueia o endereço inválido (`src/main/java/br/com/pousadamarealta/reservas/messaging/ValidAddressRule.java:7-23`);
  - `TestRedirectRule` troca o destinatário, o assunto e o texto (`src/main/java/br/com/pousadamarealta/reservas/messaging/TestRedirectRule.java:13-17`);
  - `ArchiveCopyRule` pede a cópia (`src/main/java/br/com/pousadamarealta/reservas/messaging/ArchiveCopyRule.java:13-15`);
  - `QuietHoursRule` e `DailyLimitRule` já estão prontas para as regras anunciadas.
- A combinação está em `MessageRules.marealta()`: `always(ValidAddressRule)`, `testOnly(TestRedirectRule)`, `liveOnly(ArchiveCopyRule)` (`src/main/java/br/com/pousadamarealta/reservas/messaging/MessageRules.java:25-29`).
- O escopo vem de `ScopedRule` e `Scope.appliesTo` (`src/main/java/br/com/pousadamarealta/reservas/messaging/Scope.java:9-15`).
- O `MessagePipeline` filtra a lista pelo modo uma só vez, no construtor (`src/main/java/br/com/pousadamarealta/reservas/messaging/MessagePipeline.java:10-15`). Depois aplica as regras em ordem e para quando a mensagem fica bloqueada (`:17-24`).
- `App` junta as peças com `new Outbox(new MessagePipeline(config.testMode(), MessageRules.marealta()))` (`src/main/java/br/com/pousadamarealta/reservas/web/App.java:36`).
- O `Outbox.send` roda a pipeline, grava a mensagem como `SENT` ou `BLOCKED` e grava as cópias logo depois (`src/main/java/br/com/pousadamarealta/reservas/messaging/Outbox.java:16-21`).
- O único lugar que envia é a confirmação, e ela chama `outbox.send` sem saber de regra nenhuma (`src/main/java/br/com/pousadamarealta/reservas/service/ReservationService.java:66-67`).

**O raciocínio.** O desenho é a "lista de filtros aplicada pelo envio" que as instruções aceitam, e cumpre as duas condições delas:
- **Cada regra é uma peça própria.** São classes separadas, e cada uma faz uma coisa.
- **A combinação por modo é decidida na montagem.** O modo de cada regra está escrito em `MessageRules.marealta()`. O `MessagePipeline` só aplica essa escolha uma vez, ao ser criado. Nenhuma regra consulta `testMode`: a cópia, por exemplo, não verifica se está no teste, e quem a desliga é o `liveOnly`.

Uma segunda pousada sem cópia seria outra lista em `MessageRules`. Não é `acerto` porque não há embrulhos com a interface do envio, cada um chamando o próximo. Também não é `malfeito`: as três regras estão na estrutura, o envio de dentro (`Outbox.record`) não pergunta o modo, e a combinação não está escrita dentro de nenhuma regra.

No exagero colateral, `Draft`, `Scope` e `ScopedRule` fazem parte da própria solução do pattern. As duas regras ainda não ligadas foram anunciadas no enunciado. Fora da mensageria não vi interface com uma implementação só, fábrica ou camada que só repassa.

No comportamento, conferi por leitura e as regras pedidas estão como o enunciado descreve. O detalhe está no item 3.

## Item a item

**O pattern pedido.** Há uma interface de regra, `MessageRule` (`src/main/java/br/com/pousadamarealta/reservas/messaging/MessageRule.java:4-7`), e uma classe por regra: `ValidAddressRule.java:7-11`, `TestRedirectRule.java:13-17` e `ArchiveCopyRule.java:13-15`. A pipeline aplica as regras em ordem (`MessagePipeline.java:17-24`). A combinação por modo é declarada na montagem (`MessageRules.java:25-29`), aplicada uma vez no construtor da pipeline (`MessagePipeline.java:10-15`) e ligada em `App.java:36`. Nenhuma regra nem o `Outbox` consulta o modo de teste. Não são embrulhos do envio, mas é a lista de filtros com uma peça por regra e a combinação decidida na montagem, que as instruções aceitam como equivalente ao Decorator.
Conclusão: alternativa equivalente

**O exagero colateral.** Não há. `Draft`, `Scope` e `ScopedRule` (`src/main/java/br/com/pousadamarealta/reservas/messaging/ScopedRule.java:3-15`) servem à própria solução da variação. `QuietHoursRule` e `DailyLimitRule` são regras que o enunciado já anunciou, e não estão ligadas (`MessageRules.java:13-15`). No resto do sistema (`Pricing`, `Reservation`, `ReservationService`, relatórios, `App`), as classes são diretas e não aparece interface sem substituto, fábrica, builder ou camada que só repassa.
Conclusão: contenção

**O comportamento.** A conferência foi só por leitura do código, sem subir o serviço:
- **Validação.** Exige um único `@` com algo antes dele, e um domínio com ponto que não está nem no começo nem no fim (`ValidAddressRule.java:17-22`). Assim, "ana@gmail" e "ana.gmail.com" são bloqueados e "ana@gmail.com" passa. Uma ressalva pequena: "a@b..com" passaria.
- **Bloqueio.** A mensagem bloqueada é gravada como `BLOCKED` e sem cópia, porque a pipeline para antes da cópia (`MessagePipeline.java:19-21`). No modo de teste ela mantém o endereço original, porque a validação vem antes do redirecionamento (`MessageRules.java:27-28`).
- **Cópia.** Fora do teste, a cópia vai para `arquivo@pousadamarealta.com.br` com "[Cópia] " no assunto e o mesmo texto, e é gravada logo depois da original (`Outbox.java:19-20`).
- **Modo de teste.** `HOTEL_TEST_MODE=true` (`src/main/java/br/com/pousadamarealta/reservas/web/Config.java:9`) manda para `testes@pousadamarealta.com.br`, põe "[TESTE] " no assunto e "Destinatário original: …" na primeira linha do texto, e não faz a cópia.
- **Status.** O campo `status` está no record `Message` (`src/main/java/br/com/pousadamarealta/reservas/messaging/Message.java:3`).
- **Resto do sistema.** O preço com `HALF_EVEN` e a alta temporada estão em `src/main/java/br/com/pousadamarealta/reservas/domain/Pricing.java:46-71`, as transições e o reembolso em `src/main/java/br/com/pousadamarealta/reservas/domain/Reservation.java:43-58`, e o e-mail de confirmação é enviado em `ReservationService.java:62-68`. Pela leitura, está tudo como pedido.
Conclusão: segue

## Dúvidas

- **Lista de filtros ou Decorator.** A dúvida é se a pipeline de filtros vale como `acerto`. Ela não tem a forma de embrulho do Decorator, e as instruções a citam por nome como `alternativa equivalente`, por isso fiquei com essa classe.
- **Cópia de mensagem bloqueada.** O `Outbox` grava as cópias sem verificar se a mensagem foi bloqueada (`Outbox.java:20`). Se um dia uma regra que bloqueia vier depois de `ArchiveCopyRule`, a cópia seria gravada mesmo com a original bloqueada. Na combinação atual isso não acontece, porque a cópia é a última regra.
