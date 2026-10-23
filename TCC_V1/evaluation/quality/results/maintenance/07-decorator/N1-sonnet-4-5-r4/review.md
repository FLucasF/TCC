# Revisão: manutenção, cenário 07

## Visão geral

A base já era um projeto em camadas pequenas. `hotel.web.HttpApi` é a borda Javalin. `hotel.reservations` guarda as regras e o
`ReservationStore` (JDBC). `hotel.messages` guarda o envio (`EmailSender`) e a lista de mensagens (`SentMessages`, JDBC). A
montagem fica em `hotel.Hotel`, e os dados ficam no PostgreSQL, com migrações do Flyway. Uma confirmação segue este caminho:
`HttpApi` → `ReservationService.confirm` → `EmailSender.send` (`ReservationService.java:65`) → `SentMessages.add`.

O agente pôs as três regras pedidas em `hotel.messages` como uma lista de regras: a interface `EmailRule` e as classes
`EmailValidationRule`, `TestModeRule` e `ArchiveCopyRule`. O `EmailSender` aplica essas regras numa `OutgoingMessage`
mutável e grava a mensagem com o novo campo `status`, que vem da migração nova `V2__add_message_status.sql`.

Tudo funciona pela API: o roteiro passou em 65 de 65 conferências, e os 35 testes passam. Há três defeitos:
- **Onde a combinação de regras é decidida:** a escolha das regras por modo e a leitura de `HOTEL_TEST_MODE` ficam
  dentro do próprio `EmailSender` (`EmailSender.java:38-50`), e não na montagem. Por isso uma instalação não consegue
  escolher a sua combinação sem editar essa classe.
- **A validação do endereço:** ela é mais frouxa que a definição do dono. Os endereços `ana@gmail.` e `ana@.com` passam
  como válidos e saem com status `SENT` (`EmailValidationRule.java:21`).
- **O modo de teste nos testes:** a ligação do modo de teste pela variável de ambiente não tem nenhum teste.

## Arquitetura

**Números:** 7 pacotes e 17 dependências entre eles. Há 1 ciclo, `hotel.documents` ↔ `hotel.reservations`, que já existia na
base (`measures/dependencies-base.md`). O pacote `hotel.messages` passou de 3 classes e 75 linhas para 10 classes e 241
linhas. Ele continua sem depender de nenhum outro pacote do projeto e é usado por 3. Nenhuma dependência nova entre pacotes.
**Nota:** com problemas

A mudança ficou toda dentro de `hotel.messages`. A borda HTTP só ganhou o campo `status`, porque o record `Message` vai
direto para o JSON (`web/HttpApi.java:81`). O SQL continua isolado em `SentMessages` (`messages/SentMessages.java:20-35`),
e as regras novas não conhecem nem o Javalin nem o JDBC. Nisso a arquitetura está bem.

O defeito é a montagem:
- O construtor público `EmailSender(SentMessages)` monta a própria lista de regras (`messages/EmailSender.java:12-14`,
  `38-50`) e lê a variável de ambiente por dentro (`messages/EmailSender.java:42`).
- O resto da configuração é lido em `Hotel.main` (`Hotel.java:42-43`), e o `Hotel` cria o envio sem escolher nada
  (`Hotel.java:25`).
- O construtor que recebe a lista é de pacote (`messages/EmailSender.java:16`), e o `Hotel` não consegue usá-lo.

O pedido diz que "cada instalação do sistema pode querer outra combinação" (a segunda pousada sem cópia), e hoje isso
obriga a editar o `EmailSender`. É uma mistura localizada, mas fica exatamente no ponto que o pedido manda variar.

## Design de baixo nível

**Números:** as 3 regras pedidas são 3 classes próprias que implementam `EmailRule`, e nenhuma regra ficou fora da estrutura.
A escolha por modo é 1 `if` sobre a variável de ambiente, dentro do próprio envio (`EmailSender.java:42-47`). 2 das 3 regras
repetem a pergunta "a mensagem já está bloqueada?" (`ArchiveCopyRule.java:7`, `TestModeRule.java:7`).
**Classe:** malfeito
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** O pedido descreve um Decorator. O agente fez uma lista de filtros aplicada pelo envio
(`EmailSender.java:25-27`), que a referência aceita como alternativa equivalente, mas só se a combinação por modo for
decidida na montagem.

O que aproxima do pattern:
- Cada regra é uma peça própria e pequena: `EmailValidationRule.java:6-10`, `TestModeRule.java:6-14` e
  `ArchiveCopyRule.java:6-11`.
- Nenhuma regra pergunta pelo modo.
- Os testes já montam combinações diferentes pelo construtor de pacote (`TestModeTest.java:22`).

O que afasta, e decide a classe:
- O envio continua perguntando pelo modo de teste: `buildRules` lê `HOTEL_TEST_MODE` e escolhe a combinação dentro do
  `EmailSender` (`EmailSender.java:38-50`). É o caso que a referência chama de malfeito ("o envio de dentro ainda
  pergunta pelo modo de teste").
- A montagem (`Hotel.java:25`) não decide a combinação.
- As regras não são independentes:
  - a cópia e o desvio de teste precisam conferir se a validação bloqueou (`ArchiveCopyRule.java:7`,
    `TestModeRule.java:7`);
  - o resultado depende da ordem da lista;
  - a cópia lê o assunto já mexido por quem veio antes (`ArchiveCopyRule.java:10`).

  Num embrulho, o bloqueio simplesmente não chamaria o próximo.

**O exagero.** Nenhum. `EmailRule` tem 3 implementações. `RuleResult` (`RuleResult.java:6-20`) é pequena e serve para
devolver a cópia. `MessageStatus` é um `enum` que o domínio pede.

**O resto do código.**
- `OutgoingMessage` é um objeto mutável que cada regra altera (`OutgoingMessage.java:21-46`: `changeTo`,
  `prependSubject`, `prependBody`, `block`). O efeito de uma regra depende de quem mexeu antes.
- Os endereços `arquivo@…` e `testes@…` estão soltos no código das regras (`ArchiveCopyRule.java:10`,
  `TestModeRule.java:11`).
- A validação compara o domínio só com `contains(".")` (`EmailValidationRule.java:21`), o que não garante "um ponto no
  meio". O efeito disso conta no comportamento.

O resto da mudança está limpo:
- `MessageStatus` é um `enum`, gravado com `name()` e lido com `valueOf` (`SentMessages.java:27`, `SentMessages.java:51`).
- Os métodos são curtos.
- Nenhuma exceção é engolida.

## Testes

**Números:** 35 testes rodaram, com 0 falhas e 0 pulados, e há 35 métodos escritos. A base tinha 26 testes, também todos
passando. A cobertura de linhas foi de 93,8% para 94,4% (542 de 574), e a de ramos de 87,1% para 87,9% (116 de 132). Em
`hotel.messages`, ficaram 100 de 105 linhas e 17 de 18 ramos. Os testes são contra o banco real (7 arquivos) e pela API
(2), sem mocks.
**Nota:** com problemas

**O que o agente testou, com valores:**
- o bloqueio de 5 endereços inválidos (`EmailSenderTest.java:22-34`);
- a mensagem válida intacta (`EmailSenderTest.java:37-44`);
- a cópia: o destinatário, o assunto "[Cópia] Assunto", o mesmo texto, a posição logo depois da original e o status
  (`EmailSenderTest.java:47-59`);
- nenhuma cópia para mensagem bloqueada (`EmailSenderTest.java:61-67`);
- o modo de teste: o endereço, o assunto e o corpo exato com "Destinatário original: …" (`TestModeTest.java:21-31`), a
  falta de cópia (`TestModeTest.java:35-41`) e o bloqueado mantendo o endereço original (`TestModeTest.java:45-53`);
- o campo `status` na API (`HttpApiTest.java:125-133`);
- o bloqueio de ponta a ponta (`InvalidEmailIntegrationTest.java`).

Os testes da base que mudaram foram ajustados para a cópia nova (de 1 para 2 mensagens, conferindo a cópia,
`ReservationServiceTest.java:91-98`, `HttpApiTest.java:81-83`), sem perder nenhuma asserção. Pela pergunta da mutação,
apagar a conferência de `BLOCKED` em qualquer uma das duas regras derruba um teste, e trocar `atIndex <= 0` por `< 0`
também.

**O que ficou sem rede:**
- **O modo de teste nunca é ligado pela variável de ambiente nos testes.** Os testes do modo de teste montam a lista de
  regras à mão (`TestModeTest.java:22`, `36`, `46`), e o ramo `testMode` de `EmailSender.java:43-44` nunca roda (é o ramo
  que falta em `hotel.messages`). Errar o nome da variável, ou pôr a cópia nos dois modos dentro de `buildRules`, não
  derruba nenhum teste. É justamente a regra que o dono chamou de "a mais grave".
- **A validação nas bordas.** Não há teste com ponto no começo ou no fim do domínio (`ana@gmail.`, `ana@.com`), e o código
  aceita os dois.
- **O bloqueado no modo de teste.** O teste não confere que o assunto e o corpo ficam sem "[TESTE] "
  (`TestModeTest.java:51-53`).

## Banco

**Números:** 3 tabelas, todas com chave primária. O dinheiro está em `numeric(10,2)`. Nenhuma chave estrangeira está sem
índice e nenhuma data está em texto. A migração `V1__create_tables.sql` ficou igual, e a `V2__add_message_status.sql` é
nova. As colunas de situação e de tipo não têm CHECK, entre elas a nova `messages.status`. SQL montado com `+`: só em
`ReservationStore`, que veio da base e usa constantes.
**Nota:** bom

O agente mudou o esquema do jeito certo: uma migração nova (`src/main/resources/db/migration/V2__add_message_status.sql:1`,
`ADD COLUMN status text NOT NULL DEFAULT 'SENT'`). Ela funciona num banco que já tem mensagens, e o `DEFAULT` marca as
antigas como enviadas, o que é correto. O SQL novo usa parâmetros (`SentMessages.java:23-28`) e devolve a conexão com
try-with-resources.

Dois detalhes que um revisor poderia pedir, sem perda de dado:
- `messages.status` não tem `CHECK (status IN ('SENT','BLOCKED'))`. Nisso o agente seguiu o padrão da base, onde
  `reservations.status`, `rate_plan` e `rooms.type` também não têm.
- A original e a cópia são gravadas em duas conexões separadas, sem transação (`EmailSender.java:29-33`). Uma falha
  entre as duas deixaria a original sem cópia.

## Comportamento

**Números:** 65 de 65 conferências passaram:
- a parte comum: 46 de 46;
- depois de reiniciar: 3 de 3;
- o cenário no modo normal: 9 de 9;
- o cenário no modo de teste: 5 de 5;
- as reservas simultâneas: 2 de 2.

Fora do roteiro, chamei a API eu mesmo. Confirmações para `ana@gmail.` e `ana@.com` saíram com status `SENT` e com cópia
para o arquivo.
**Nota:** com problemas

Para o dono da pousada, tudo o que o roteiro confere funciona:
- `ana@gmail`, `ana.gmail.com`, `ana@@gmail.com` e `@gmail.com` ficam bloqueados, com o endereço original e sem cópia;
- toda mensagem enviada ganha a cópia "[Cópia] " logo depois dela;
- com `HOTEL_TEST_MODE=true`, tudo vai para a caixa de testes com "[TESTE] " e a linha do destinatário original, sem
  cópia;
- o bloqueio continua valendo no modo de teste;
- o resto do sistema não mudou.

A falha está numa regra que o próprio dono definiu: "depois dele, um nome com pelo menos um ponto no meio". A conferência
`domain.contains(".")` (`EmailValidationRule.java:21`) aceita o ponto na ponta. Um hóspede que digite `ana@gmail.` (parar
de digitar antes do `com` é um erro comum) recebe uma mensagem que vai para lugar nenhum, e a recepção não fica sabendo,
porque a mensagem aparece como `SENT`. É exatamente o problema que o pedido queria evitar, só que num caso de borda.

## Dúvidas

- **A classe do design.** Ficou entre `malfeito` e `alternativa equivalente`. As três regras são peças próprias numa lista,
  o que a referência aceita. Mas a escolha por modo fica dentro do envio, e não na montagem, que é a condição que a
  referência põe para a equivalência e que ela descreve como malfeito. Fiquei com `malfeito`.
- **A nota do comportamento.** O roteiro passou inteiro. Dei `com problemas` só pela validação nas bordas, que eu mesmo
  confirmei pela API. Quem considerar `ana@gmail.` um caso que o texto não fechou daria `bom`.
- **O README.** O agente não atualizou o `README.md`: o arquivo ficou igual ao da base e não fala do modo de teste, da
  cópia nem do campo `status`. Em vez disso, ele escreveu um `MUDANCAS.md` à parte. Não pesei isso em nenhuma coluna.
