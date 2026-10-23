# Revisão: manutenção, cenário 07

## Visão geral

A base é um serviço Javalin em pacotes por assunto (`hotel.web` com as rotas, `hotel.reservations` com o serviço e o
acesso às reservas, `hotel.messages` com o envio e a lista de mensagens, `hotel.rooms`, `hotel.reports`,
`hotel.documents`), montado em `hotel.Hotel`, com os dados no PostgreSQL criados por migrações do Flyway. Uma
confirmação entra por `HttpApi`, passa por `ReservationService.confirm`, que grava a reserva e chama
`EmailSender.send`. O agente mexeu só no envio. Criou a interface `MessageProcessor`, três regras
(`EmailValidator`, `TestModeRedirector`, `ArchiveCopier`), uma mensagem mutável `OutgoingMessage`, o enum
`MessageStatus` e a migração `V2`. O `EmailSender` agora passa a mensagem por uma lista de processadores montada em
`Hotel.java:31-34` e grava a original e as cópias. O roteiro passa inteiro (65 de 65) e os 36 testes passam. O
problema mais sério está no design: as duas regras que dependem do modo perguntam pelo modo por dentro
(`ArchiveCopier.java:15`, `TestModeRedirector.java:15`), e a montagem empilha sempre as mesmas três peças, então a
combinação por modo não se decide na montagem. Fora do roteiro, o validador aceita endereços como `ana@gmail.` e
`ana@.com`, o que vai contra o "ponto no meio" do pedido.

## Arquitetura

**Números:** 7 pacotes, 17 dependências entre eles, 1 ciclo (`hotel.documents` ↔ `hotel.reservations`). A base tem os
mesmos números (`measures/dependencies-base.md`). `hotel.messages` passou de 3 para 9 classes e de 75 para 263 linhas
e continua sem depender de nenhum outro pacote do projeto.
**Nota:** bom

O agente não criou dependência nova nem ciclo novo; o único ciclo já existia na base. As regras novas ficam em
`hotel.messages`, sem Javalin e sem JDBC: `EmailValidator.java`, `TestModeRedirector.java` e `ArchiveCopier.java` só
conhecem `OutgoingMessage`. O SQL continua isolado em `SentMessages.java:20-56`. A montagem continua num lugar só:
os processadores são criados em `Hotel.java:31-34`, e o modo de teste é lido da variável de ambiente só no `main`
(`Hotel.java:59`) e passado pelo construtor (`Hotel.java:26`). A rota `/messages` continua fina
(`HttpApi.java:81`). O campo `status` chega ao JSON porque foi acrescentado ao record (`Message.java:3`), sem mexer
na borda HTTP. Nenhuma classe cresceu demais: a maior do pacote novo é `SentMessages`, com 72 linhas.

## Design de baixo nível

**Números:** 3 regras pedidas, as 3 numa peça própria (`MessageProcessor` com 3 implementações), aplicadas em lista
por `EmailSender.java:23-25`. Das 2 regras que dependem do modo, as 2 perguntam pelo modo por dentro. A montagem
decide 0 combinações: empilha sempre as mesmas três peças.
**Classe:** malfeito
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** O formato é o de uma lista de filtros aplicada pelo envio, que a referência aceita como alternativa
equivalente: cada regra é uma peça própria (`EmailValidator.java:4`, `TestModeRedirector.java:4`,
`ArchiveCopier.java:4`) e o envio só percorre a lista (`EmailSender.java:22-26`), sem `if` por regra. Mas a
referência pede que "a combinação por modo seja decidida na montagem, não dentro das regras", e aqui acontece o
contrário. `Hotel.java:32-34` põe sempre as três regras e passa o `testMode` para duas delas. Quem decide é cada
regra: `ArchiveCopier.java:15` (`if (testMode || ...) return;`, a cópia que verifica se está em modo de teste,
exatamente o exemplo de malfeito da referência) e `TestModeRedirector.java:15` (`if (!testMode || ...) return;`).

Por isso, as regras anunciadas no pedido ("pode valer sempre, só no modo de teste ou só fora dele", e a segunda
pousada sem a cópia) vão exigir mais um `boolean` dentro de cada regra nova, em vez de uma escolha na montagem.
Além disso, as regras dependem da ordem e se consultam pelo status. `TestModeRedirector` e `ArchiveCopier` só
funcionam porque `EmailValidator` roda antes e marca `BLOCKED` (`TestModeRedirector.java:15`,
`ArchiveCopier.java:15`); o "bloqueada não segue" não é garantido pelo envio, cada regra repete a pergunta. Por
fim, as mensagens extras (a cópia, `ArchiveCopier.java:18-23`) não passam pelas regras: uma regra futura como o
"limite por dia" ou o "não mandar de madrugada" não vai valer para elas sem mudar `SentMessages.add`.

**O exagero.** Não há. `MessageProcessor` tem três implementações reais, e `OutgoingMessage` é necessária para as
regras mudarem a mensagem antes de gravar. O construtor `EmailSender(SentMessages)` (`EmailSender.java:17-19`) e o
`Hotel(DataSource)` (`Hotel.java:43-45`) só servem para os testes da base continuarem compilando; são pequenos.

**O resto do código.**
- `OutgoingMessage` é um objeto mutável com setters públicos para destinatário, assunto e texto
  (`OutgoingMessage.java:50-60`). Ele também devolve a própria lista interna (`additionalMessages()`,
  `OutgoingMessage.java:46-48`), que qualquer um pode alterar.
- `SentMessages.add` grava a original e as cópias em conexões separadas (`SentMessages.java:20-26`); isso pesa
  na coluna do banco.
- O validador é uma sequência clara de retornos (`EmailValidator.java:13-29`), mas não confere o "ponto no meio"
  (ver Comportamento).
- Status em `enum` (`MessageStatus.java`), endereços em constantes (`ArchiveCopier.java:6`,
  `TestModeRedirector.java:6`), sem duplicação de texto.

## Testes

**Números:** 36 testes rodaram, 0 falhas, 0 erros, 0 pulados; 36 métodos escritos (a base tinha 26, todos
mantidos). Cobertura de linhas 558/592 (94,3%; base 93,8%) e de ramos 122/140 (87,1%; base 87,1%). Em
`hotel.messages`: 110/116 linhas e 23/26 ramos.
**Nota:** bom

O agente escreveu 10 testes para o que mudou, em dois níveis:
- **Unidade contra o banco, em `MessageProcessingTest`:** bloqueio de `invalid`, `missing@domain` e `double@@at.com`
  com o status conferido (`MessageProcessingTest.java:22-36`); o desvio no modo de teste com o endereço, o
  "[TESTE] " e a primeira linha "Destinatário original: …" (`:39-52`); a mensagem bloqueada que não é desviada e
  mantém o endereço (`:55-67`); a cópia com endereço, assunto "[Cópia] ", mesmo texto, status e posição logo depois
  da original (`:70-87`); nenhuma cópia no modo de teste (`:90-99`) nem para a mensagem bloqueada (`:102-112`); e a
  combinação das três regras nos dois modos (`:115-140`).
- **Pela API:** `TestModeIntegrationTest.java:59-85` sobe o `Hotel` em modo de teste e confere o desvio e o bloqueio.
  O teste da base em `HttpApiTest.java:81-86` foi ajustado de 1 para 2 mensagens, como o pedido manda, e ficou mais
  forte (status e cópia conferidos). Nada foi apagado nem desligado.

As lacunas são pequenas:
- Nenhum teste usa um endereço sem nada antes do @ (`@gmail.com`). Quem apagasse `atIndex <= 0` em
  `EmailValidator.java:18` não quebraria nenhum teste.
- Nenhum teste usa os casos do "ponto no meio" (`ana@gmail.`, `ana@.com`), que o código erra.
- A leitura de `HOTEL_TEST_MODE` em `Hotel.java:59` não é testada (o `main` não é coberto: 31/37 linhas em `hotel`).

## Banco

**Números:** 3 tabelas, todas com chave primária; o dinheiro em `numeric(10,2)`; nenhuma chave estrangeira sem
índice; nenhuma data em texto. As conferências acusaram as colunas de situação sem CHECK, entre elas a nova
`messages.status` (as outras já vinham da base) e nenhuma transação explícita. Migrações: `V1` igual à base, `V2`
nova.
**Nota:** bom

O esquema evolui do jeito certo. O agente não editou a `V1` e acrescentou uma migração
(`src/main/resources/db/migration/V2__add_status_to_messages.sql:1`) com `NOT NULL DEFAULT 'SENT'`, o que dá um
status válido às mensagens que já existem num banco em uso. O SQL novo usa parâmetros e try-with-resources
(`SentMessages.java:29-36`). Dois pontos menores:
- `messages.status` é `text` sem `CHECK (status IN ('SENT','BLOCKED'))`. Isso segue o estilo da base, que também não
  restringe `reservations.status`, e a leitura com `MessageStatus.valueOf` (`SentMessages.java:65`) falharia com um
  valor estranho.
- A original e a cópia são gravadas em duas conexões, sem transação (`SentMessages.java:20-26`). Uma falha no
  segundo `INSERT` deixa a original sem a cópia para o contador. A confirmação da base já gravava a reserva e a
  mensagem separadas (`ReservationService.java:63-68`), então o padrão veio da base; o agente só o estendeu.

## Comportamento

**Números:** 65 de 65 conferências passaram. Parte comum 46/46, dados depois de reiniciar 3/3, cenário no modo
normal 9/9, cenário no modo de teste 5/5, reservas simultâneas 2/2.
**Nota:** com problemas

O roteiro inteiro passa:
- **Modo normal:** a mensagem válida sai `SENT` com a cópia logo depois, com "[Cópia] ", o mesmo texto e `SENT`;
  `ana@gmail`, `ana.gmail.com`, `ana@@gmail.com` e `@gmail.com` ficam `BLOCKED`, com o endereço original e sem
  cópia.
- **Com `HOTEL_TEST_MODE=true`:** a mensagem vai para a caixa de testes com "[TESTE] " e a linha do destinatário
  original, sem cópia, e o endereço inválido continua bloqueado.
- **O resto:** a parte comum não quebrou.

Fora do roteiro, chamei o validador da `final/` com outros endereços digitados errado. `ana@gmail.`, `ana@.com`,
`ana@gmail..com` e `ana @gmail.com` saem como `SENT`. A regra do dono é "depois dele, um nome com pelo menos um ponto
no meio", e o código só confere se há um ponto em qualquer lugar (`EmailValidator.java:24-28`). Para o dono, isso
quer dizer que um hóspede que digite `ana@gmail.` (o ponto no fim, um erro comum) recebe a mensagem como enviada: ela
vai para lugar nenhum e a recepção não fica sabendo que precisa ligar. É justamente o problema que o pedido quer
resolver. É uma regra que o texto fechou, não um ponto em aberto. Por isso a nota não é `bom`.

## Dúvidas

- **Classe do design:** `malfeito` e `alternativa equivalente`. O formato de lista de filtros é aceito pela
  referência, mas o modo de teste é decidido dentro de duas regras (`ArchiveCopier.java:15`,
  `TestModeRedirector.java:15`) e não na montagem. A referência dá esse caso como exemplo de malfeito, e foi o que
  pesou.
- **Peso da falha fora do roteiro:** a falha do "ponto no meio" não aparece no roteiro (65/65). A nota
  `com problemas` no Comportamento vem só da leitura do texto e de uma chamada direta ao validador. Quem considerar
  "no meio" vago o bastante para aceitar `ana@gmail.` daria `bom`.
