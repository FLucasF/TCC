# Revisão: manutenção, cenário 07

## Visão geral

O sistema da base tem 7 pacotes: `hotel` com a montagem (`Hotel`) e o `Database` com Flyway e Hikari, `hotel.web`
com as rotas do Javalin, `hotel.reservations` com as regras e o acesso ao banco das reservas, `hotel.rooms`,
`hotel.documents`, `hotel.reports` e `hotel.messages`. Uma requisição entra pelo `HttpApi`, passa pelo
`ReservationService`, que grava com o `ReservationStore`. Ao confirmar, o serviço chama `EmailSender.send`, que guarda
a mensagem pelo `SentMessages` na tabela `messages` do PostgreSQL. A mudança do agente é pequena: um campo `status` no
`Message`, a migração `V2__add_message_status.sql` e as três regras novas escritas como `if` dentro de
`SentMessages.add` (`final/src/main/java/hotel/messages/SentMessages.java:29-51`), que é a classe de acesso a dados.
Ele também testou o que mudou e ajustou os testes da base. Tudo funciona e o roteiro passa inteiro. O mais sério é
que o pedido descrevia uma variação que vai crescer: regras de envio que se combinam por modo e por instalação, com
mais duas já anunciadas. O agente não criou estrutura para isso: as regras e a combinação por modo ficaram fixas
num método da camada de dados, e o modo de teste é lido do ambiente dentro dessa mesma classe. Há ainda um desvio
de comportamento: a validação aceita `ana@gmail.` e `ana@.com`.

## Arquitetura

**Números:** 7 pacotes, 17 dependências entre eles, 1 ciclo (`hotel.documents` ↔ `hotel.reservations`), iguais aos
da base (`measures/dependencies.md` e `measures/dependencies-base.md`). A única diferença é o tamanho de
`hotel.messages`, que foi de 75 para 148 linhas, e o de `SentMessages`, que foi de 56 para 129.
**Nota:** com problemas

O grafo de pacotes não mudou, e o ciclo `documents`/`reservations` já vinha da base. O problema está dentro de
`hotel.messages`. `SentMessages` era só o acesso à tabela `messages`. Agora ela também guarda as regras de negócio do
envio: a validação do endereço (`SentMessages.java:87-107`), o desvio para a caixa de testes (`SentMessages.java:38-42`)
e a cópia para o arquivo (`SentMessages.java:46-48`). Elas estão misturadas com o SQL da mesma classe
(`SentMessages.java:53-85`). O `EmailSender` (`EmailSender.java:12-14`), que seria o lugar natural das regras de envio,
continua só repassando. A montagem também ficou dividida: o modo de teste é lido com `System.getenv("HOTEL_TEST_MODE")`
dentro do construtor da classe de dados (`SentMessages.java:20-22`), e não em `Hotel` (`Hotel.java:23`), que é onde a
base monta as peças. A segunda pousada, que não quer a cópia, não tem como desligá-la na montagem. É uma mistura
localizada, sem ciclo novo.

## Design de baixo nível

**Números:** 3 regras pedidas (bloqueio, cópia, modo de teste), nenhuma numa estrutura. As 3 são `if` dentro de
`SentMessages.add` (`SentMessages.java:30`, `:38`, `:46`), e a combinação por modo está escrita no próprio `if` da
cópia (`if (!testMode)`, `:46`). Nenhuma interface de envio e nenhum embrulho.
**Classe:** omissão
**Exagero:** contenção
**Nota:** com problemas

O pedido descreve o Decorator: três comportamentos em volta do mesmo envio, que se combinam por modo; mais duas
regras anunciadas (madrugada e limite por dia), cada uma valendo sempre, só no teste ou só fora dele; e instalações
com combinações diferentes. O agente não tirou nenhuma regra do método. O bloqueio é um `if` com retorno antecipado
(`SentMessages.java:30-32`), o modo de teste reescreve três variáveis locais (`:34-42`) e a cópia é um `if (!testMode)`
(`:46-48`). A combinação por modo está escrita na própria regra, e não na montagem. Para acrescentar a regra da
madrugada, ou para desligar a cópia na segunda pousada, alguém vai ter de editar esse método. Pela referência, isso
é exatamente a **omissão** ("os três `if` ... ficam dentro do envio"). Não há estrutura sobrando: nenhuma interface,
fábrica ou camada a mais. Daí a **contenção**.

Sobre o resto do código que o agente escreveu:
- `addSent` e `addBlocked` (`SentMessages.java:53-68` e `:70-85`) são cópias de 16 linhas que diferem só no literal
  `'SENT'`/`'BLOCKED'`. Um parâmetro de situação bastaria.
- A situação da mensagem é texto solto (`Message.java:3`, `String status`, e os literais em `SentMessages.java:56`,
  `:63`, `:73` e `:80`), quando a base já usa `enum` para a situação da reserva (`ReservationStatus`).
- O construtor público lê a variável de ambiente (`SentMessages.java:20-22`). A configuração fica escondida numa
  classe de dados, e quem usa o construtor público não tem como escolher o modo.
- A validação percorre a string à mão (`SentMessages.java:91-99`) e confere só `domain.contains(".")` (`:106`), sem
  exigir o ponto "no meio".

São defeitos localizados num arquivo, e o resto vem da base sem mudança.

## Testes

**Números:** 31 testes rodaram, 0 falharam, 0 pulados, 31 métodos escritos (na base: 26, todos passando).
Cobertura de linhas 93,8% (510/544) e de ramos 87,3% (117/134); no pacote `hotel.messages`, 68/75 linhas e 18/20
ramos (na base, 29/33 e 2/2) (`measures/tests.md`, `measures/tests-base.md`).
**Nota:** com problemas

O agente escreveu 5 testes novos contra o banco (`SentMessagesTest.java`) e ajustou os da base. As contagens de
mensagens passaram de 1 para 2, e o teste agora confere a cópia (`ReservationServiceTest.java:91-101`,
`HttpApiTest.java:81-83`). Os testes ficaram mais fortes, nada foi apagado nem enfraquecido. As regras novas são
conferidas com valores:
- o bloqueio, com 5 endereços inválidos, todos `BLOCKED` e sem cópia (`SentMessagesTest.java:21-34`);
- a cópia, com o destinatário, "[Cópia] " no assunto e a situação (`:53-65`);
- o modo de teste, com o endereço de testes, "[TESTE] " e nenhuma cópia (`:68-79`);
- o bloqueio no modo de teste, com o endereço original (`:82-90`).

Pela pergunta da mutação, há regras sem rede:
- O texto da cópia nunca é conferido (`:53-65` não olha o `body`). Se a cópia levasse o `finalBody`, ou um texto vazio,
  nenhum teste falharia.
- "Destinatário original" é conferido com `contains` (`:76`). Pôr a linha no fim do texto, e não como primeira linha,
  passaria.
- Não há caso para o ponto na borda do domínio (`ana@gmail.`, `ana@.com`). O defeito da validação passa sem que
  nenhum teste o acuse.
- A leitura de `HOTEL_TEST_MODE` (`SentMessages.java:21`) não é testada; os testes usam o construtor de pacote.

## Banco

**Números:** 3 tabelas, todas com chave primária; dinheiro em `numeric(10,2)`; nenhuma chave estrangeira sem índice;
nenhuma data em texto. Há 2 migrações: a V1 está igual à da base e a V2 é nova. A conferência acusou colunas de
situação sem `CHECK`: `messages.status` (nova, do agente), além de `messages.channel`, `reservations.status`,
`reservations.rate_plan` e `rooms.type`, que já vinham da base (`measures/database.md`).
**Nota:** com problemas

A evolução está certa. A V1 não foi tocada, e a coluna nova veio numa migração nova
(`src/main/resources/db/migration/V2__add_message_status.sql:1`), `NOT NULL` com `DEFAULT 'SENT'`, o que preserva as
mensagens antigas de quem já roda o sistema. O SQL novo usa parâmetros (`SentMessages.java:56-60`), e as conexões são
devolvidas com try-with-resources. Ficam dois defeitos do agente:
- A original e a cópia são gravadas em duas conexões separadas, sem transação (`SentMessages.java:44` e `:47`, cada
  uma abrindo conexão em `:54`). Se a segunda gravação falhar, a original fica sem a cópia que o contador quer. Com
  dois envios ao mesmo tempo, os `id` podem se intercalar (original A, original B, cópia A), e a cópia deixa de vir
  "logo depois da original" na lista, que é ordenada por `id` (`SentMessages.java:112`).
- `messages.status` aceita qualquer texto, sem um `CHECK (status IN ('SENT','BLOCKED'))`. O padrão sem `CHECK` nas
  outras colunas de situação vem da base.

## Comportamento

**Números:** 65 de 65 conferências passaram, 0 falharam. Parte comum: 46/46; dados depois de reiniciar: 3/3; cenário
no modo normal: 9/9; no modo de teste: 5/5; reservas simultâneas: 2/2 (`measures/behavior.md`).
**Nota:** com problemas

Tudo o que o roteiro confere funciona. Os casos são `ana@gmail`, `ana.gmail.com`, `ana@@gmail.com` e `@gmail.com`
bloqueados sem cópia, a cópia logo depois com "[Cópia] " e o mesmo texto, e o modo de teste com o endereço de testes,
"[TESTE] ", a primeira linha "Destinatário original:", sem cópia e com o bloqueio mantido. A parte comum também
continua igual.

Fora do roteiro, chamei a API com o serviço de pé, num banco vazio, e confirmei e-mails de reservas com três
endereços. `ana@gmail.`, `ana@.com` e `ana@gmail..com` saíram como `SENT`, e cada um ainda gerou a cópia para o
arquivo. O dono definiu que o endereço é válido quando, depois do @, há "um nome com pelo menos um ponto no meio", e
a regra só confere se existe um ponto (`SentMessages.java:106`). Para o dono da pousada, quer dizer que um hóspede que
digite `ana@gmail.` continua não recebendo nada, e a recepção não fica sabendo que precisa ligar. Esse é o primeiro
dos três problemas que ele queria resolver, numa variação de digitação bem comum. O resto do cenário está certo.

## Dúvidas

- A nota do comportamento depende de pesar `ana@gmail.` e `ana@.com` aceitos contra os 65/65 do roteiro. Contei como
  falha de parte do cenário, porque o texto diz "no meio" com todas as letras. Mas `ana@gmail..com` é um caso mais
  discutível, já que o texto não fala de pontos seguidos.
- A nota do banco pesa um risco que só aparece em envios simultâneos ou em falha no meio da gravação. Nenhuma
  conferência do roteiro mostrou esse risco.
