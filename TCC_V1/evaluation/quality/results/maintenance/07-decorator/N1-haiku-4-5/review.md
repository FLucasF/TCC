# Revisão: manutenção, cenário 07

## Visão geral

A base é um serviço Javalin em sete pacotes: `hotel.web` (rotas), `hotel.reservations` (regras e acesso às reservas),
`hotel.rooms`, `hotel.reports`, `hotel.documents`, `hotel.messages` (o envio de e-mail, que só grava a mensagem na
tabela `messages`) e `hotel` (a montagem, com as migrações do Flyway em PostgreSQL). Uma confirmação entra por
`HttpApi`, passa por `ReservationService`, que grava a reserva por `ReservationStore` e chama `EmailSender.send`, e
este grava em `SentMessages`. O agente mexeu só no envio: criou `EmailValidator`, `HotelEnvironment` (lê
`HOTEL_TEST_MODE`) e `MessageStatus`, pôs a coluna `status` numa migração nova (`V2__add_message_status.sql`) e escreveu
as três regras como `if`s dentro de `EmailSender.send` (`src/main/java/hotel/messages/EmailSender.java:17-32`). O
sistema faz o que o dono pediu (65 de 65 conferências do roteiro passam, 49 testes passam). O mais sério é o design:
o texto anuncia mais regras (madrugada, limite por dia), cada uma valendo sempre, só no teste ou só fora dele, e
combinações diferentes por instalação, e nada disso dá para fazer sem reescrever o `send`. O pattern ficou de fora.
O README, que segundo o dono "explica como o sistema é", ficou igual ao da base e não fala do modo de teste nem das
regras novas.

## Arquitetura

**Números:** 7 pacotes e 17 dependências entre eles, iguais na base e no final (`measures/dependencies.md` e
`measures/dependencies-base.md`); 1 ciclo (`hotel.documents` ↔ `hotel.reservations`), que já existia na base;
`hotel.messages` passou de 3 para 6 classes (75 para 140 linhas) e continua sem depender de nenhum pacote do projeto.
**Nota:** bom

A mudança ficou toda no pacote do envio e na montagem. A borda HTTP não mudou: `/messages` continua só serializando a
lista (`src/main/java/hotel/web/HttpApi.java:81`), e o campo `status` aparece porque o registro `Message` ganhou o
campo (`src/main/java/hotel/messages/Message.java:3`). O SQL novo ficou em `SentMessages`
(`src/main/java/hotel/messages/SentMessages.java:20-35`), a classe de acesso a dados que já existia. A configuração
do modo de teste é lida uma vez e entregue na montagem (`src/main/java/hotel/Hotel.java:21-31`): o construtor novo
recebe o mapa de ambiente, e é isso que deixa os testes ligarem o modo de teste sem variável de ambiente
(`src/test/java/hotel/web/HttpApiTestModeTest.java:36`). Nenhuma dependência nova, nenhum ciclo novo; o ciclo
`documents`↔`reservations` veio da base. Um detalhe pequeno: `HotelEnvironment` é configuração do sistema e ficou em
`hotel.messages` (`src/main/java/hotel/messages/HotelEnvironment.java:1`). Hoje só o envio usa essa configuração, então
não chega a ser um defeito.

## Design de baixo nível

**Números:** 3 regras pedidas (bloqueio, cópia, desvio para testes): nenhuma numa estrutura própria, as 3 em `if`s
dentro de `EmailSender.send` (`EmailSender.java:18`, `:23`, `:27-29`); nenhuma interface de envio; a combinação por
modo também está escrita dentro do `send` (`EmailSender.java:23-30`), e não na montagem.
**Classe:** omissão
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** O pedido descreve comportamentos que embrulham o mesmo envio e se combinam conforme o modo e a
instalação ("cada regra dessas pode valer sempre, só no modo de teste ou só fora dele"; "a segunda pousada [...] não
quer a cópia"). A referência trata exatamente o que o agente fez como omissão: os três `if` dentro do envio.
`EmailSender.send` valida o endereço (`EmailSender.java:18-20`), pergunta se está em modo de teste
(`EmailSender.java:23`) e grava a cópia só no `else` (`EmailSender.java:27-29`). A regra "a cópia só fora do teste"
não foi decidida na montagem: está no código do envio. `Hotel` só passa o `HotelEnvironment` (`Hotel.java:29-31`) e
não escolhe nenhuma regra. Para a segunda pousada (sem cópia), seria preciso mais um `if` com mais uma configuração.
Cada regra anunciada (madrugada, limite por dia, cada uma com três modos) somaria mais ramos ao mesmo método. Não há
interface de envio, nem embrulho, nem lista de filtros. Pelo menos o validador é uma peça separada
(`EmailValidator.java:7-23`), e isso facilitaria transformá-lo num embrulho depois.

**O exagero.** Nenhum. `HotelEnvironment` (`HotelEnvironment.java:5-15`) é um objeto pequeno que tem uso (isola a
leitura do ambiente e deixa testar), e `EmailValidator` é uma função estática simples. Não há interface com uma
implementação só, nem fábrica, nem camada de repasse.

**O resto do código.** O `send` é curto e se lê bem, mas tem defeitos localizados:
- O agente criou o `enum MessageStatus` (`MessageStatus.java:3-5`), mas o descartou logo em seguida. O registro
  guarda o status como `String` (`Message.java:3`), `SentMessages.add` recebe `String status`
  (`SentMessages.java:20`), e o envio converte com `.name()` em cada chamada (`EmailSender.java:19`, `:26`, `:28`,
  `:29`). É texto solto, justamente onde o tipo já existia.
- Os endereços de arquivo e de testes e os prefixos "[Cópia] ", "[TESTE] " e "Destinatário original: " são
  constantes e literais presos ao envio (`EmailSender.java:6-7`, `:24-25`, `:29`). A segunda pousada, que tem outro
  endereço, não tem onde configurar isso.
- `Hotel.java:25` usa `java.util.Map` com o nome completo em vez de importar a classe. É só cosmético.
- O validador aceita espaço e pontos seguidos ("ana silva@gmail..com" passa por `EmailValidator.java:17-22`). O
  critério do dono não fecha esse caso, então aqui fica só a nota.

## Testes

**Números:** 49 testes rodaram, 0 falhas, 0 erros, 0 pulados; 49 métodos escritos (na base eram 26, todos mantidos).
Cobertura de linhas 501/532 (94,2%; na base 93,8%) e de ramos 119/134 (88,8%; na base 87,1%); `hotel.messages` com
56/60 linhas e 20/20 ramos (`measures/tests.md`, `measures/tests-base.md`).
**Nota:** com problemas

A parte nova está testada com valores, em três níveis:
- o validador, com os casos do dono e os de borda (`src/test/java/hotel/messages/EmailValidatorTest.java:11-59`);
- o envio contra o banco: o bloqueio sem cópia, a cópia logo depois da original com "[Cópia] ", o desvio no modo de
  teste com "[TESTE] " e a primeira linha "Destinatário original: ...\n", e o bloqueio no modo de teste com o
  endereço original (`src/test/java/hotel/messages/EmailSenderTest.java:31-102`);
- a API, nos dois modos (`src/test/java/hotel/web/HttpApiTest.java:94-112`,
  `src/test/java/hotel/web/HttpApiTestModeTest.java:60-93`).

Os testes da base foram ajustados de 1 para 2 mensagens, como o pedido exige, e não ficaram mais fracos: o ajuste
confere também o destinatário e o assunto da cópia
(`src/test/java/hotel/reservations/ReservationServiceTest.java:93-100`). Pela pergunta da mutação, apagar a cópia,
trocar a ordem, gerar cópia no modo de teste ou trocar `atIndex < 1` por `< 0` derrubam algum teste.

Os problemas:
- **"Com o mesmo texto" da cópia não é conferido em lugar nenhum.** Os testes da cópia olham só o destinatário, o
  assunto e o status (`EmailSenderTest.java:60-62`, `CompleteEmailFlowTest.java:35-38`,
  `ReservationServiceTest.java:99-100`). Se a cópia saísse com o texto vazio, nenhum teste falharia.
- **`CompleteEmailFlowTest` repete quase todos os casos de `EmailSenderTest`** (válido com cópia, inválido, teste
  válido, teste inválido: `CompleteEmailFlowTest.java:23-81` contra `EmailSenderTest.java:51-102`). São cinco testes
  a mais que não acrescentam nada.
- `assertFalse` é importado e não é usado (`EmailSenderTest.java:4`).

## Banco

**Números:** 3 tabelas, todas com chave primária; dinheiro em `numeric(10,2)`; nenhuma chave estrangeira sem índice;
nenhuma data em texto; Flyway com 2 migrações, `V1` igual à da base e `V2__add_message_status.sql` nova; a conferência
acusa `messages.status` em `text` sem CHECK, assim como as outras colunas de situação, que já vinham da base
(`measures/database.md`).
**Nota:** bom

O agente evoluiu o esquema do jeito certo: uma migração nova, sem tocar a da base
(`src/main/resources/db/migration/V2__add_message_status.sql:1`). A coluna é `NOT NULL DEFAULT 'SENT'`, então as
mensagens que já existiam num banco em uso ficam com um status coerente. O SQL novo usa parâmetros
(`SentMessages.java:23-28`), com a conexão do pool devolvida por try-with-resources (`SentMessages.java:21`). Duas
observações pequenas:
- `status` não tem `CHECK (status IN ('SENT','BLOCKED'))`. A base também não restringe nenhuma das suas colunas de
  situação, e o agente seguiu esse estilo.
- A original e a cópia são dois `INSERT` em conexões separadas, sem transação (`EmailSender.java:28-29`). Se o
  segundo falhar, a original fica sem cópia. Num registro de mensagens, o risco é baixo.

## Comportamento

**Números:** 65 de 65 conferências passaram (`measures/behavior.md`):
- a parte comum: 46/46;
- os dados depois de reiniciar: 3/3;
- o cenário no modo normal: 9/9;
- o cenário no modo de teste: 5/5;
- as reservas simultâneas: 2/2.

**Nota:** bom

O dono recebe o que pediu:
- os endereços "ana@gmail", "ana.gmail.com", "ana@@gmail.com" e "@gmail.com" ficam na lista como `BLOCKED`, com o
  endereço original e sem cópia;
- toda mensagem enviada gera, logo depois dela, a cópia para `arquivo@pousadamarealta.com.br`, com "[Cópia] " e o
  mesmo texto;
- com `HOTEL_TEST_MODE=true`, tudo vai para `testes@pousadamarealta.com.br`, com "[TESTE] " e a linha "Destinatário
  original: ...", sem cópia, e o endereço inválido continua bloqueado com o endereço original;
- o campo `status` aparece no `GET /messages`.

O que já funcionava continua funcionando: os preços, as recusas, os cancelamentos, os relatórios, os dados depois de
reiniciar e nenhuma reserva dupla em 10 rodadas simultâneas.

## Dúvidas

- Na nota do design, o peso do `status` como `String` (com o `enum` criado e descartado) e das constantes presas ao
  envio ficou entre `bom` e `com problemas`. Escolhi `com problemas` porque o pedido fala de instalações com
  combinações diferentes, e o código não tem onde configurá-las.
- Nos testes, a falta de conferência do texto da cópia e a classe repetida também ficaram no limite entre `bom` e
  `com problemas`.
