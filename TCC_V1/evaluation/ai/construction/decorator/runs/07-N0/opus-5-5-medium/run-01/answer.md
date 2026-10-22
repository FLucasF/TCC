# Cenário 07

## Visão geral

O agente montou as regras de envio como uma lista de regras (`Rule`) que o `MessageDispatcher` percorre antes de gravar a mensagem na lista. Cada regra é uma peça separada: validação do endereço, horário de silêncio, limite diário, desvio para a caixa de testes e cópia para o arquivo. Cada uma vem com um `Scope` (`ALWAYS`, `TEST_ONLY`, `LIVE_ONLY`, `OFF`) que diz em que modo ela vale. As regras em si não sabem nada do modo de teste. A montagem decide a combinação, e cada instalação pode configurá-la por variável de ambiente. Não é um Decorator, porque as regras não embrulham o envio nem têm a interface dele. É uma lista de filtros com a combinação decidida na montagem, e isso as instruções aceitam como alternativa equivalente. O ponto mais fraco é que a montagem fica no construtor do próprio `MessageDispatcher`, e não no `main` ou no `App`. Além disso, o dispatcher guarda o `testMode` e aplica o `Scope` a cada envio. O comportamento pedido confere na leitura.

## Como foi implementado, e por que o julgamento

**O projeto.** Tudo está num pacote só, `br.com.pousadamarealta`.
- `Main` lê a configuração do ambiente com `AppConfig.fromEnv` e a porta (`Main.java:9-12`), e chama `App.create`.
- `App.create` é quem monta o sistema: cria o `MessageLog`, o `MessageDispatcher` e o `ReservationService`, e registra as rotas Javalin e os tratadores de erro (`App.java:43-75`). As views JSON também ficam em `App` (`ReservationView`, `DocumentView`), junto com a leitura e validação do corpo (`App.java:123-174`).
- `ReservationService` guarda as reservas numa `ArrayList` em memória e cuida das transições de estado, da disponibilidade e da capacidade. Na confirmação, chama `dispatcher.sendEmail(...)` (`ReservationService.java:54`).
- O preço sai de `Pricing` e `RatePlan`, o arredondamento e a formatação em reais de `Money`, o comprovante de `Receipt` e os CSVs de `Reports`.
- O caminho de uma requisição é: rota em `App`, depois `ReservationService`, depois `Reservation`/`Pricing`, e na confirmação `MessageDispatcher`, que grava no `MessageLog`.
- Os dados ficam só na memória.
- Os testes estão em `AppTest`, com cerca de oito casos: fluxo e alta temporada, erros, reembolso, envio normal com cópia, endereço bloqueado, modo de teste, madrugada/limite configuráveis e ocupação.

**A parte que pede o pattern.**
- `MessageDispatcher` define uma `Draft`, que é a mensagem em processamento: guarda os campos originais e os atuais, o `blocked` e a lista `copies` (`MessageDispatcher.java:18-36`). Define também a interface `Rule { void apply(Draft) }` (`:38-40`) e o par `Step(Scope, Rule)` (`:42`).
- O construtor monta a lista (`:52-56`):
  - validação do endereço com `Scope.ALWAYS`;
  - `QuietHours` com o escopo vindo de `config.quietHours()`;
  - `DailyLimit` com o escopo de `config.dailyLimit()`;
  - `redirectToTest` com `Scope.TEST_ONLY`;
  - `addArchiveCopy` com o escopo de `config.archiveCopy()`, que por padrão é `LIVE_ONLY` (`AppConfig.java:21`).
- `sendEmail` percorre os passos e aplica cada regra se a mensagem ainda não foi bloqueada e se `step.scope().appliesTo(testMode)` (`:61-65`). Depois grava a mensagem como `BLOCKED`, com os dados originais (`:66-69`), ou como `SENT` seguida das cópias (`:71-74`).
- O `Scope` resolve o modo num lugar só (`Scope.java:7-14`), e o `AppConfig.fromEnv` permite escolher o escopo de cada regra configurável por instalação (`AppConfig.java:50-62`). Assim, a segunda pousada pode desligar a cópia com `HOTEL_RULE_ARCHIVE_COPY=OFF`.

**O raciocínio.**

O que aproxima o desenho do esperado:
- Cada uma das três regras pedidas, e as duas anunciadas, é uma peça própria (`:52-56`, `:86-137`).
- Nenhuma regra pergunta pelo modo de teste. `addArchiveCopy` (`:92-94`) e `redirectToTest` (`:86-90`) são incondicionais, e quem decide se valem é o `Scope` atribuído na montagem.
- Uma regra nova entra com uma linha na lista e um escopo, sem mexer nas outras.

Por que é alternativa equivalente, e não acerto:
- Não há uma interface de envio que o envio real e os embrulhos implementem, com um chamando o outro. É uma lista aplicada por um laço (`:61-65`).

O que afasta do ideal, sem chegar a malfeito:
- A lista é montada dentro do construtor do `MessageDispatcher` e não em `App.create`.
- A escolha por modo é feita a cada envio pelo laço (`:62`), com o `testMode` guardado no dispatcher (`:45, :50`).

Mesmo assim, o `Scope` de cada passo é fixado na montagem e o laço é genérico. O envio de dentro não tem um `if (testMode)` próprio para uma regra específica. Por isso não se encaixa no critério de malfeito.

**Exagero colateral.** `Scope`, `AppConfig` com os `with...` e a `Draft` servem à configuração das regras, que o enunciado pede. Não há interface com uma só implementação, fábrica nem camada que só repassa.

**Comportamento.** Conferido só pela leitura. Valores, recusas, formatos da mensagem, `status`, ordem da cópia e o modo de teste conferem com o pedido.

## Item a item

**O pattern pedido.** As regras são objetos `Rule` numa lista ordenada de `Step(Scope, Rule)` montada no construtor do dispatcher (`MessageDispatcher.java:52-56`) e aplicada por um laço genérico (`MessageDispatcher.java:61-65`). Nenhuma regra conhece o modo: a cópia não verifica o modo de teste (`MessageDispatcher.java:92-94`) e o desvio também não (`MessageDispatcher.java:86-90`). A combinação vem do `Scope` dado na montagem, configurável por instalação (`AppConfig.java:21`, `AppConfig.java:50-62`). Não é Decorator, porque não há embrulho com a interface do envio. É a lista de filtros com peças próprias e combinação decidida na montagem, que as instruções aceitam.
Conclusão: alternativa equivalente

**O exagero colateral.** Não há. `Scope` (`Scope.java:4-15`) e `AppConfig` (`AppConfig.java:11-75`) atendem ao pedido de cada regra valer "sempre, só no teste ou só fora dele" e de cada instalação ter sua combinação. `Draft` (`MessageDispatcher.java:18-36`) guarda os dados originais que a mensagem bloqueada precisa. Não há interfaces de uma implementação só, fábricas nem camadas de repasse.
Conclusão: contenção

**O comportamento.** A conferência foi feita por leitura do código; não subi o serviço nem chamei a API.
- **Endereço válido** (`MessageDispatcher.java:78-84`): exige um único `@` com algo antes e um domínio com ponto que não esteja em nenhuma das pontas. `ana@gmail` e `ana.gmail.com` são bloqueados.
- **Bloqueio**: a validação é o primeiro passo e vale sempre (`MessageDispatcher.java:52`). A mensagem bloqueada é gravada como `BLOCKED` com o destinatário, o assunto e o texto originais, sem cópia (`MessageDispatcher.java:66-69`), também no modo de teste.
- **Cópia**: vai para `arquivo@pousadamarealta.com.br` com "[Cópia] " no assunto e o mesmo texto (`MessageDispatcher.java:93`). É gravada logo depois da original (`MessageDispatcher.java:71-74`) e por padrão só fora do teste (`AppConfig.java:21`).
- **Modo de teste**: ligado por `HOTEL_TEST_MODE=true` (`AppConfig.java:54`). Desvia para `testes@pousadamarealta.com.br`, com "[TESTE] " no assunto e a primeira linha "Destinatário original: …" (`MessageDispatcher.java:86-90`).
- **Campo `status`**: está no `Message` (`Message.java:3-6`).
- **E-mail de confirmação**: o assunto e o texto conferem (`ReservationService.java:54`, `ReservationService.java:103-109`).
- **Resto do sistema**:
  - preço com alta temporada e meio-para-o-par (`Pricing.java:23-39`, `Money.java:16-18`);
  - estados e reembolso (`ReservationService.java:50-76`);
  - códigos de erro (`App.java:123-174`, `ReservationService.java:20-36`);
  - relatórios (`Reports.java:12-42`).
  
  Tudo isso confere com o enunciado.
Conclusão: segue

## Dúvidas

A dúvida principal é entre `alternativa equivalente` e `malfeito`. A lista é montada no construtor do `MessageDispatcher` (`MessageDispatcher.java:48-57`) e não no `main`/`App`. O dispatcher guarda o `testMode` (`MessageDispatcher.java:50`) e filtra os passos pelo escopo a cada envio (`MessageDispatcher.java:62`). Pode-se ler isso como "o envio ainda pergunta pelo modo", mas a pergunta é genérica, via `Scope`, e não um `if` de alguma regra específica. Os escopos vêm da configuração montada fora. Por isso a evidência sustenta melhor a alternativa equivalente. Outro detalhe menor: `a@b..c` passa na validação, um caso de borda que o enunciado não define.
