# Revisão: manutenção, cenário 07

## Visão geral

A base tem sete pacotes: `hotel` (a montagem em `Hotel` e o `Database`), `hotel.web` (as rotas Javalin em `HttpApi`),
`hotel.reservations` (o serviço, o cálculo de preço e o `ReservationStore` com JDBC), `hotel.rooms`, `hotel.messages`,
`hotel.reports` e `hotel.documents`. Uma requisição entra pelo `HttpApi`, passa para o `ReservationService` e chega ao
banco pelos stores. A confirmação chama `EmailSender.send` (`src/main/java/hotel/reservations/ReservationService.java:65`),
que grava a mensagem na tabela `messages` através de `SentMessages`. O agente mexeu só no pacote `hotel.messages`. As
três regras novas (bloquear endereço inválido, cópia para o arquivo, desvio no modo de teste) ficaram como `if`
dentro do próprio `EmailSender.send` (`src/main/java/hotel/messages/EmailSender.java:21-43`). O modo de teste é lido do
ambiente dentro da classe (`EmailSender.java:13`), e não na montagem. Ele criou ainda um `enum MessageStatus`, a migração
nova `V2__add_message_status.sql` e 5 testes. Pela API, tudo o que o roteiro confere funciona (65 de 65). O mais sério é
de design: o texto anuncia mais regras (madrugada, limite por dia) que valem por modo e por instalação, e nada disso
foi preparado. É uma omissão do Decorator pedido. Há também um desvio pequeno da regra de validação: um endereço como
`ana@gmail.` é aceito.

## Arquitetura

**Números:** 7 pacotes, 17 dependências entre eles, 1 ciclo (`hotel.documents` ↔ `hotel.reservations`), igual ao da
base. A única diferença em `measures/dependencies.md` frente a `dependencies-base.md` é o pacote `hotel.messages`, que
passa de 3 para 4 classes e de 75 para 124 linhas.
**Nota:** bom

O agente não mudou nenhuma dependência entre pacotes. O ciclo `documents`↔`reservations` já existia na base e não é
dele. A borda HTTP continua fina: `GET /messages` só serializa a lista (`src/main/java/hotel/web/HttpApi.java:81`), e o
campo `status` chega à resposta porque entrou no record (`src/main/java/hotel/messages/Message.java:3`). O SQL novo
ficou em `SentMessages`, a classe de acesso a dados que já existia (`src/main/java/hotel/messages/SentMessages.java:20-56`),
e as regras de envio ficaram no pacote de mensagens, sem Javalin. Um ponto fica com o design (para não contar duas
vezes): a configuração do modo de teste é lida em `EmailSender.java:13`, fora da montagem (`src/main/java/hotel/Hotel.java:20-28`),
que é onde a base lê as outras variáveis de ambiente (`Hotel.java:42-43`).

## Design de baixo nível

**Números:** 3 regras pedidas, 0 delas numa estrutura própria. As três são `if` dentro de `EmailSender.send`: o
endereço (`EmailSender.java:22`), o modo de teste (`EmailSender.java:30`) e a cópia condicionada ao modo
(`EmailSender.java:38`). Não há nenhuma interface de envio nem embrulho, e a combinação por modo fica dentro do envio.
**Classe:** omissão
**Exagero:** contenção
**Nota:** com problemas

O pattern pedido é o Decorator. O texto pede três comportamentos em volta do mesmo envio, que se combinam por modo
(a cópia só fora do teste, o bloqueio sempre), e anuncia outras regras ("não mandar de madrugada", "limite de
mensagens por hóspede por dia") que "podem valer sempre, só no modo de teste ou só fora dele". Diz também que cada
instalação "pode querer outra combinação", como a segunda pousada, que não quer a cópia. O agente pôs tudo num método
só, com variáveis locais reatribuídas (`EmailSender.java:26-34`). A combinação está fixa no código: `if (!testMode)`
decide a cópia (`EmailSender.java:38`). Desligar a cópia para a segunda pousada, ou acrescentar a regra da madrugada,
obriga a abrir e mudar esse método, com mais um `if` e mais uma bandeira. É exatamente o caso que a referência chama
de omissão. Não há nada que aproxime do pattern: `EmailSender` continua sendo uma classe concreta `final`
(`EmailSender.java:4`), sem interface que um embrulho possa implementar.

Exagero: nenhum. A única peça nova é o `enum MessageStatus` (`src/main/java/hotel/messages/MessageStatus.java:3-6`), que é
o tipo certo para o campo `status`, e não estrutura sobrando.

Sobre o resto do que o agente escreveu:
- O construtor público lê `System.getenv("HOTEL_TEST_MODE")` (`EmailSender.java:13`). Isso esconde uma dependência
  global dentro da classe, em vez de receber o modo da montagem. Por causa disso, os testes que usam esse construtor
  mudam de resultado conforme o ambiente (ver Testes).
- A validação aceita domínios como `gmail.`, `.com` ou `.`, porque só confere `domain.contains(".")`
  (`EmailSender.java:53-54`), e o texto pede um ponto "no meio" do nome.
- O canal `"EMAIL"` aparece repetido três vezes no mesmo método (`EmailSender.java:23,36,39`).
- O comentário da classe continua dizendo "Every e-mail sent is kept" (`EmailSender.java:3`), e isso já não descreve o
  que a classe faz.

O resto do código veio da base, e o agente não o piorou.

## Testes

**Números:** 31 testes rodaram, 0 falharam, 0 foram pulados. Há 31 métodos de teste no código; a base tinha 26 e o
agente acrescentou os 5 de `EmailSenderTest`. Cobertura de linhas 94,1% (498/529) e de ramos 87,7% (114/130); na
base eram 93,8% e 87,1%. No pacote `hotel.messages`: 56 de 60 linhas e 15 de 16 ramos.
**Nota:** com problemas

As regras novas são conferidas com valores:
- o bloqueio de seis endereços inválidos, entre eles `ana@gmail`, `ana.gmail.com`, `@gmail.com` e `ana@@gmail.com`
  (`src/test/java/hotel/messages/EmailSenderTest.java:22-35`);
- a cópia, com destinatário, assunto `[Cópia] …`, mesmo texto e `SENT` (`EmailSenderTest.java:38-53`);
- a ausência de cópia quando a mensagem é bloqueada (`EmailSenderTest.java:56-62`);
- o modo de teste, com endereço, `[TESTE] `, a primeira linha `Destinatário original: …` e sem cópia
  (`EmailSenderTest.java:65-77`);
- o bloqueio no modo de teste, com o endereço original (`EmailSenderTest.java:80-89`).

Os testes da base foram mantidos. Os que contavam mensagens foram ajustados de 1 para 2, como a regra nova exige, e
ganharam asserções sobre a cópia e o `status` (`src/test/java/hotel/reservations/ReservationServiceTest.java:91-98`,
`src/test/java/hotel/web/HttpApiTest.java:81-85`). Nenhum foi enfraquecido.

A pergunta da mutação: trocar `atIndex <= 0` por `< 0`, tirar a checagem do segundo `@` ou apagar o `if (!testMode)`
quebra algum teste, e isso está bem coberto. Duas coisas ficam sem rede:
- **O "ponto no meio".** Nenhum teste usa `ana@gmail.` ou `ana@.com`, e por isso o erro de `EmailSender.java:54`
  passou.
- **A leitura de `HOTEL_TEST_MODE`.** Nenhum teste confere essa leitura do ambiente.

Há também uma fragilidade. `EmailSenderTest.java:14`, `ReservationServiceTest.java:26`,
`src/test/java/hotel/reports/ReportsTest.java:28` e `HttpApiTest.java:35` montam o envio pelo construtor que lê o
ambiente. Quem rodar `mvn verify` com `HOTEL_TEST_MODE=true` exportado (o pessoal do site, testando) vê falhar os
testes que esperam a cópia: `EmailSenderTest.java:42`, `ReservationServiceTest.java:91,107` e `HttpApiTest.java:81`.

Pela API, não há teste para `BLOCKED` nem para o modo de teste.

## Banco

**Números:** 3 tabelas (`messages`, `reservations`, `rooms`), todas com chave primária; dinheiro em `numeric(10,2)`;
nenhuma chave estrangeira sem índice; nenhuma data em texto. 2 migrações: a `V1` ficou igual à da base e a `V2` é
nova. As conferências acusam só colunas de situação sem `CHECK`, entre elas a nova `messages.status`.
**Nota:** bom

O agente evoluiu o esquema do jeito certo: criou uma migração nova sem tocar na que já existia
(`src/main/resources/db/migration/V2__add_message_status.sql:2`). O `DEFAULT 'SENT'` preenche as mensagens que já
estavam no banco de quem roda o sistema, e não se perde dado. O SQL novo usa parâmetros
(`SentMessages.java:23-28`), e as conexões são devolvidas por try-with-resources (`SentMessages.java:21`, `:39`).

Dois pontos menores:
- `messages.status` é `text` sem `CHECK (status IN ('SENT','BLOCKED'))`. A base já fazia assim em
  `reservations.status`, e o agente só seguiu o padrão.
- A mensagem original e a cópia são dois `INSERT` separados, sem transação (`EmailSender.java:36` e `:39`). Se o
  segundo falhar, fica a original sem a cópia do contador.

## Comportamento

**Números:** 65 de 65 conferências passaram:
- parte comum: 46 de 46;
- dados depois de reiniciar: 3 de 3;
- cenário no modo normal: 9 de 9;
- cenário no modo de teste: 5 de 5;
- reservas simultâneas: 2 de 2.

**Nota:** com problemas

Pela API, o que o dono pediu funciona no roteiro:
- a mensagem válida sai com `SENT` e a cópia vem logo depois, com `[Cópia] `;
- os quatro endereços inválidos do roteiro ficam `BLOCKED`, com o endereço original e sem cópia;
- com `HOTEL_TEST_MODE=true`, a mensagem vai para `testes@pousadamarealta.com.br` com `[TESTE] ` e a linha do
  destinatário original, sem cópia, e o inválido continua bloqueado.

Tudo o que já funcionava continua funcionando.

Fora do roteiro, rodei a mesma regra de `EmailSender.java:45-55` à parte. Ela aceita como válidos `ana@gmail.`,
`ana@.com` e `ana@.`, que não têm "um nome com pelo menos um ponto no meio" depois do @. Para o dono, isso quer dizer
que um hóspede que digite `ana@gmail.` (um erro de digitação parecido com os exemplos que ele deu) recebe uma mensagem
marcada como `SENT`, com cópia para o contador, que não chega a ninguém. A recepção não fica sabendo que precisa ligar.

## Dúvidas

- **A nota do comportamento.** O roteiro passou inteiro. Dei `com problemas` pelo desvio de validação que eu mesmo
  conferi (`ana@gmail.` aceito). Quem considerar esse caso estreito demais pode ler a coluna como `bom`.
- **A nota dos testes.** Os testes do cenário são bons e conferem valores. O que os levou a `com problemas` foi a
  dependência da variável de ambiente e a falta de teste para o "ponto no meio". A falha com `HOTEL_TEST_MODE=true`
  exportado eu deduzi lendo o código e não rodei.
