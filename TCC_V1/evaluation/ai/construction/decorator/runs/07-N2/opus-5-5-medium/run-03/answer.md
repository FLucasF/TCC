# Cenário 07

## Visão geral

O agente resolveu as regras de envio com um Decorator. Há uma interface de envio, `MessageSender`. O envio real é a lista de mensagens, `Outbox`. Cada regra é um embrulho próprio: `BlockInvalidAddress`, `ArchiveCopy` e `RedirectToTestAddress`. A combinação por modo fica numa única classe de montagem, `MessagingSetup`, que empilha os embrulhos conforme o escopo de cada regra (sempre, só no teste, só fora dele). Nem o domínio (`Hotel`) nem os embrulhos perguntam pelo modo de teste. Para atender ao pedido de que outra pousada dispense a cópia, ou de que entre uma regra nova, basta trocar uma linha na montagem. Não encontrei problema sério. O único ponto discutível é pequeno: o bloqueio grava a mensagem bloqueada direto na `Outbox`, sem passar pelo resto da pilha. Isso é justamente o efeito pedido (sem cópia e sem desvio), mas amarra esse embrulho à classe concreta da lista.

## Como foi implementado, e por que o julgamento

**O projeto.** Tudo fica num pacote só, `br.com.marealta`, com cerca de 23 classes curtas.
- `Main` lê `HOTEL_TEST_MODE` e `PORT`, monta as peças e registra as rotas Javalin (`Main.java:18-52`). Também traduz `DomainException` em `{error, message}` (`Main.java:45-46`).
- As rotas chamam `Hotel`, que guarda os quartos e as reservas em memória (um `LinkedHashMap`) e tem as operações e os dois relatórios CSV (`Hotel.java:15-124`).
- `Reservation` calcula noites, subtotal, desconto e total no construtor (`Reservation.java:233-252`) e guarda a máquina de estados (`confirm`, `doCheckIn`, `doCheckOut`, `cancel`, com `require`).
- `Pricing` cuida da alta temporada, `Money` do arredondamento HALF_EVEN e do formato R$, e `RatePlan` (um enum com métodos) do desconto e do reembolso.
- `Receipt` é o comprovante, e o texto dele sai de `toText`.
- `ReservationRequest.parse` valida o corpo e lança os 400. `ReservationView` dá o formato JSON da reserva.

O caminho de uma confirmação é este: rota `POST /reservations/{id}/confirmation` → `Hotel.confirm` → `Reservation.confirm` → `sender.send(new OutgoingMessage(...))` (`Hotel.java:61-67`). O jar é montado pelo maven-shade-plugin com `Main` como classe principal. Os testes são de dois tipos:
- `ApiTest` sobe o app numa porta livre e cobre preço de alta temporada, fluxo completo, erros e cancelamento não reembolsável.
- `MessagingSetupTest` testa a pilha nos dois modos e a validação de endereço.

**A parte do pattern.**
- `MessageSender` tem um método só, `send(OutgoingMessage)` (`MessageSender.java:4-6`).
- `Outbox` é o envio real, o fim da pilha: grava a mensagem como `SENT` (`Outbox.java:11-13`).
- `BlockInvalidAddress` testa o endereço. Se for válido, repassa ao próximo; se não, grava `BLOCKED` com o endereço original (`BlockInvalidAddress.java:14-20`, regra em `:23-29`).
- `ArchiveCopy` chama o próximo com a mensagem e depois com a cópia para o arquivo, com "[Cópia] " no assunto (`ArchiveCopy.java:14-17`).
- `RedirectToTestAddress` reescreve o destinatário, o assunto e a primeira linha do texto (`RedirectToTestAddress.java:14-18`).
- `MessagingSetup.build` declara as regras com seus escopos: bloqueio `ALWAYS`, cópia `OUTSIDE_TEST`, desvio `TEST_ONLY` (`MessagingSetup.java:28-31`). Depois embrulha a `Outbox` de dentro para fora só com as regras que valem no modo (`:33-39`).

No modo normal a pilha fica Bloqueio → Cópia → Outbox. No modo de teste fica Bloqueio → Desvio → Outbox. `Hotel` recebe só a interface (`Hotel.java:20-24`) e não sabe de modo nenhum.

**O raciocínio.** É exatamente o que as instruções chamam de acerto:
- uma interface de envio, o envio real e um embrulho por regra, cada um chamando o próximo;
- a combinação decidida na montagem;
- nenhum embrulho consulta o modo, e o envio de dentro também não;
- as três regras estão dentro da estrutura.

As regras anunciadas (madrugada, limite diário) cabem no mesmo molde: uma classe nova e uma linha `Rule` com o escopo. O que afasta um pouco do Decorator puro é o bloqueio depender de `Outbox.block` (`BlockInvalidAddress.java:6,18`) em vez de só de `MessageSender`. É uma dependência justificável, porque a mensagem bloqueada precisa aparecer na lista e não pode passar pelos embrulhos de dentro, e não muda a classe.

No exagero colateral não pesou nada: o `Scope` e o `Rule` servem à montagem do próprio pattern, e fora dele o código é direto, sem interfaces de uma implementação só, fábricas ou camadas de repasse.

No comportamento, a conferência foi feita só por leitura e as regras conferem.

## Item a item

**O pattern pedido.** O agente fez:
- a interface `MessageSender` (`MessageSender.java:4`) e o envio real `Outbox implements MessageSender` (`Outbox.java:7-13`);
- um embrulho por regra, cada um com `next`: `BlockInvalidAddress.java:4-20`, `ArchiveCopy.java:4-17` e `RedirectToTestAddress.java:4-18`;
- o empilhamento por modo na montagem (`MessagingSetup.java:27-40`), chamado em `Main.java:26` com o modo lido em `Main.java:19`.

Nenhum embrulho e nenhum chamador consulta o modo. `Hotel.confirm` só faz `sender.send(...)` (`Hotel.java:64-65`). A cópia não sabe do modo de teste: ela simplesmente não entra na pilha quando ele está ligado (`MessagingSetup.java:30,35`). É o Decorator pedido, aplicado às três regras.

Conclusão: acerto

**O exagero colateral.** Não há. `MessageSender` tem quatro implementações de verdade. O enum `Scope` e o record `Rule` (`MessagingSetup.java:14-22`) são a forma de dizer, na montagem, em que modo cada embrulho vale, e isso é parte do pattern. O resto (`Hotel`, `Reservation`, `Pricing`, `RatePlan`) é direto, sem fábricas, builders ou camadas que só repassam.

Conclusão: contenção

**O comportamento.** Conferi por leitura do código: não subi o serviço nem chamei a API, como a correção manda.

Regras das mensagens:
- A validação exige um único `@` com algo antes e, depois, um domínio com ponto que não fica nem no começo nem no fim (`BlockInvalidAddress.java:23-29`). Assim, "ana@gmail" e "ana.gmail.com" são recusados e "ana@gmail.com" passa.
- A mensagem bloqueada vai para a lista como `BLOCKED` com o endereço original, sem cópia. No modo de teste ela também fica bloqueada, porque o bloqueio é a camada mais externa nos dois modos (`MessagingSetup.java:29`).
- A cópia sai logo depois da original, para `arquivo@pousadamarealta.com.br`, com o mesmo texto e "[Cópia] " no assunto (`ArchiveCopy.java:15-16`, `MessagingSetup.java:11`).
- No modo de teste a mensagem vai para `testes@pousadamarealta.com.br`, com "[TESTE] " no assunto e "Destinatário original: …" como primeira linha, sem a cópia (`RedirectToTestAddress.java:15-17`).
- `status` aparece no JSON de `/messages` (`SentMessage.java:3`, `Main.java:41`). O modo é ligado por `HOTEL_TEST_MODE=true` (`Main.java:19`).

O resto do sistema:
- Preço: noite com 20% a mais na alta temporada (`Pricing.java:10-20`), 15% de desconto no não reembolsável e arredondamento HALF_EVEN a cada passo (`Reservation.java:244-251`, `Money.java:13-15`).
- O e-mail de confirmação segue o texto do enunciado (`Hotel.java:126-132`).
- Estados e reembolso: `Reservation.java:286-306` e `RatePlan.java:6-11`.
- Conflito de quarto ignorando as canceladas: `Hotel.java:36-40`.
- Segunda via só para reserva paga: `Reservation.java:308-311`.
- Relatórios CSV com as regras de ocupação e de dinheiro retido: `Hotel.java:91-118`.
- Códigos de erro: `DomainException.java` e `ReservationRequest.java`.
- Porta: `PORT`, ou 7070 se ela não vier (`Main.java:20-21`).

Não achei divergência.

Conclusão: segue

## Dúvidas

O bloqueio grava direto na `Outbox` (`BlockInvalidAddress.java:18`) e não através de uma operação da interface de envio. Por isso esse embrulho depende da classe concreta da lista. Como isso não põe o modo nem a combinação dentro de um embrulho, mantive `acerto`. O comportamento foi julgado só pela leitura, sem rodar nada.
