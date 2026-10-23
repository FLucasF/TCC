# Revisão: manutenção, cenário 07

## Visão geral

O agente mexeu só no pacote `hotel.messages` e na montagem (`Hotel`). O caminho continua o mesmo da base: a rota HTTP
(`hotel.web.HttpApi`) chama `ReservationService`, que chama `EmailSender.send`. A mudança está nesse ponto: o
`EmailSender` passa a mensagem por um `EmailProcessor`, que tem uma lista fixa de três regras (`EmailValidationRule`,
`TestModeRule`, `ArchiveCopyRule`). As regras trabalham sobre um `ProcessingResult`, que é mutável e guarda a mensagem
original e as extras. Depois, o `EmailSender` grava tudo em `messages`, que ganhou a coluna `status` numa migração nova,
a `V2`. As 65 conferências do roteiro passaram, os 45 testes passam e o esquema evoluiu do jeito certo. O ponto mais
sério está no design: quem decide que regras valem em cada modo são as próprias regras, que perguntam pelo
`testMode` por dentro. E a lista é montada dentro do `EmailProcessor`, não na montagem do sistema. A segunda pousada,
que não quer a cópia, teria de mudar código.

## Arquitetura

**Números:** 7 pacotes e 17 dependências entre eles, os mesmos números da base. Há um ciclo
(`hotel.documents` ↔ `hotel.reservations`), que já existia na base. `hotel.messages` passou de 3 para 12 classes e de
75 para 270 linhas, e continua sem depender de nenhum outro pacote do projeto.
**Nota:** bom

O agente não criou dependência nova nem ciclo novo (`measures/dependencies.md` e `measures/dependencies-base.md` têm
as mesmas 17 arestas). O ciclo `documents`/`reservations` veio da base.

- **O modo de teste:** é lido num lugar só, na montagem: `src/main/java/hotel/Hotel.java:28` lê `HOTEL_TEST_MODE` do
  ambiente, que `Hotel.java:53` repassa.
- **O acesso ao banco:** continua isolado em `SentMessages`.
- **A borda HTTP:** não mudou, continua com uma linha para `/messages` (`src/main/java/hotel/web/HttpApi.java:81`).
- **As regras novas:** não conhecem Javalin nem JDBC.

Há um único ponto de atenção, que é de montagem: a lista de regras é criada por dentro do `EmailProcessor`
(`src/main/java/hotel/messages/EmailProcessor.java:10-15`), e não em `Hotel`. Esse ponto pesa no design, não aqui.

## Design de baixo nível

**Números:** as 3 regras pedidas estão na estrutura, cada uma numa classe que implementa `EmailRule`. Mas 2 das 3
(`TestModeRule` e `ArchiveCopyRule`) perguntam pelo modo de teste por dentro. Nenhuma das três combinações por modo é
decidida na montagem.
**Classe:** malfeito
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** O que o agente fez é uma lista de filtros aplicada pelo envio, e a referência aceita isso como
alternativa equivalente ao Decorator, mas com uma condição: a combinação por modo tem de ser decidida na montagem,
não dentro das regras. Aqui é o contrário:

- `ArchiveCopyRule` recebe o `testMode` e se desliga sozinha (`src/main/java/hotel/messages/ArchiveCopyRule.java:13`).
  É exatamente o caso que a referência chama de malfeito: "a cópia que verifica se está em modo de teste".
- `TestModeRule` também recebe a flag e se desliga fora do modo de teste
  (`src/main/java/hotel/messages/TestModeRule.java:13`).
- A lista é fixa no construtor de `EmailProcessor` (`EmailProcessor.java:10-15`), que recebe só um `boolean`.
  `Hotel.java:32` não escolhe que regras entram nem em que ordem.

Por isso, desligar a cópia para a segunda pousada, ou ligar "não mandar de madrugada só no modo de teste", exige mudar
o `EmailProcessor` ou a regra, não a montagem.

As regras também dependem da ordem em que estão na lista. Cada uma confere se a anterior já bloqueou a mensagem
(`TestModeRule.java:17`, `ArchiveCopyRule.java:17`) em vez de a cadeia parar. Isso funciona, mas cada regra nova
precisa lembrar de fazer a mesma conferência.

Do lado do que aproxima: as três regras são peças próprias com a mesma interface (`EmailRule.java`). O `EmailSender`
não pergunta pelo modo (`EmailSender.java:17-27`), e nenhum `if` sobre o caso ficou espalhado pelos chamadores.

**O exagero:** contenção. As classes de apoio (`OutgoingMessage`, `ProcessingResult`, `MessageStatus`) servem à
estrutura. `EmailRule` tem três implementações. Não há fábrica, builder nem camada que só repassa.

**O resto do código:** os defeitos são poucos e localizados.

- `ProcessingResult` é mutável. Ele expõe a lista interna (`ProcessingResult.java:23-25`) e tem `setOriginal`
  (`:19`), e as regras se comunicam mexendo nesse estado compartilhado.
- `OutgoingMessage` e `Message` são dois records quase iguais.
- `EmailSender.java:20-24` repete a mesma chamada a `sentMessages.add` para a original e para cada extra.
- `send` passou a devolver `List<Message>`, mas ninguém usa o retorno (`ReservationService.java:65`).
- O validador é um método `static` solto (`EmailValidator.java:5`) chamado por uma regra que só o embrulha.
- O README não foi atualizado: não menciona `HOTEL_TEST_MODE` nem o campo `status`.

O código da base que o agente não tocou continua como estava.

## Testes

**Números:** 45 testes rodaram, com 0 falhas e 0 pulados, e há 45 métodos escritos. A base tinha 26, e os 19 novos
estão em `EmailRulesTest` (8), `EmailValidatorTest` (7) e `TestModeIntegrationTest` (4). A cobertura foi de 94,6% das
linhas e 88,7% dos ramos (na base, 93,8% e 87,1%). Em `hotel.messages`, a cobertura é de 115 de 120 linhas e 27 de 28
ramos.
**Nota:** bom

As três regras novas são conferidas com valores, em dois níveis: o serviço contra o banco e a API com o `Hotel` subido
nos dois modos.

- **A cópia:** os testes conferem o endereço, o assunto `"[Cópia] Assunto teste"`, o mesmo corpo, o status e a
  ordem logo depois da original (`src/test/java/hotel/messages/EmailRulesTest.java:64-79`;
  `TestModeIntegrationTest.java:56-72`).
- **A falta de cópia quando bloqueada:** conferida em `EmailRulesTest.java:81`.
- **O modo de teste:** os testes conferem o desvio, o prefixo `[TESTE] ` e a primeira linha `Destinatário original:`
  (`EmailRulesTest.java:92-104`, `TestModeIntegrationTest.java:75-91`). A ausência de cópia no modo de teste e o
  bloqueio com o endereço original também são conferidos (`EmailRulesTest.java:107`, `:118`;
  `TestModeIntegrationTest.java:111`).
- **O validador:** os testes cobrem os exemplos do texto e outros casos (`EmailValidatorTest.java:11-52`).

Pela pergunta da mutação: trocar o prefixo, o endereço do arquivo ou o de testes, ou apagar o `if` de bloqueio, faz
algum teste falhar.

Ficam sem rede dois pontos:

- **Os domínios com o ponto na ponta:** "ana@.com" e "ana@gmail." passariam no validador
  (`src/main/java/hotel/messages/EmailValidator.java:18`, que só usa `contains(".")`), e nenhum teste confere esses
  casos.
- **O corpo no modo de teste:** só se confere que ele "contém" o corpo original (`EmailRulesTest.java:102`), não que
  ele seja exatamente a linha nova seguida do corpo.

O agente mudou testes da base, mas não os enfraqueceu. As mudanças acompanham o pedido:

- `ReservationServiceTest` e `HttpApiTest` passaram a esperar 2 mensagens (a original e a cópia) em vez de 1.
- Elas também ganharam asserções de `status` e da cópia.

## Banco

**Números:** 3 tabelas, todas com chave primária. O dinheiro está todo em `numeric(10,2)`, nenhuma chave estrangeira
está sem índice e nenhuma data está em texto. As migrações são 2: a `V1` igual à da base e a `V2` nova. As colunas de
situação (`messages.status`, `messages.channel` e as da base) são `text` sem `CHECK`.
**Nota:** bom

A mudança no esquema é uma migração nova (`src/main/resources/db/migration/V2__add_message_status.sql:2`), com
`NOT NULL DEFAULT 'SENT'`. Ela não edita a `V1` e marca como `SENT` as mensagens que já existiam, o que está certo
para elas. O SQL novo usa parâmetros (`SentMessages.java`, o `INSERT` com 5 `?`), e as conexões seguem com
try-with-resources e o pool da base.

Dois detalhes:

- **Status sem `CHECK`:** `status` não tem `CHECK (status IN ('SENT','BLOCKED'))`. Segue o padrão da base, que
  também não tinha `CHECK` nas colunas de situação. Mas um valor inesperado quebraria o `MessageStatus.valueOf` na
  leitura (`SentMessages.java:51`).
- **A cópia sem transação:** a original e a cópia são gravadas em duas conexões separadas, sem transação
  (`EmailSender.java:20-24`). Uma falha entre as duas deixaria a original sem a cópia.

Nenhum dos dois perde dado em uso normal. A proteção contra reserva duplicada (o `EXCLUDE` com `gist`) veio da base e
continua lá.

## Comportamento

**Números:** 65 de 65 conferências passaram, nenhuma falhou:

- a parte comum: 46 de 46;
- os dados depois de reiniciar: 3 de 3;
- o cenário no modo normal: 9 de 9;
- o cenário no modo de teste: 5 de 5;
- as duas reservas ao mesmo tempo: 2 de 2.

**Nota:** bom

Para o dono da pousada, tudo o que já funcionava continua funcionando. No modo normal:

- O e-mail válido sai com `SENT` e gera a cópia `[Cópia] ` para o arquivo, logo depois da original.
- Os quatro endereços inválidos do roteiro ficam `BLOCKED`, com o endereço original e sem cópia: "ana@gmail",
  "ana.gmail.com", "ana@@gmail.com" e "@gmail.com".

Com `HOTEL_TEST_MODE=true`:

- Tudo vai para `testes@pousadamarealta.com.br`, com `[TESTE] ` no assunto e a linha `Destinatário original:`, sem
  cópia.
- O endereço inválido continua bloqueado, com o endereço original.

Fora do roteiro, pela leitura: o validador aceitaria "ana@gmail." e "ana@.com" (`EmailValidator.java:18`). O texto
pede "um nome com pelo menos um ponto no meio", então esses dois deveriam ser bloqueados. É um ponto pequeno, e o
roteiro não o mede.

## Dúvidas

- A classe do pattern ficou entre `malfeito` e `alternativa equivalente`. A estrutura (uma lista de regras, uma
  classe por regra) é a que a referência aceita, mas a combinação por modo está dentro de duas regras
  (`ArchiveCopyRule.java:13`, `TestModeRule.java:13`) e a lista é fixa no `EmailProcessor`. A referência dá
  exatamente isso como exemplo de malfeito, e foi o que decidiu a classe.
- O validador não bloqueia domínios com o ponto na ponta ("ana@gmail.", "ana@.com"). Não ficou decidido se isso deve
  pesar como desvio do texto ("ponto no meio") ou como leitura razoável. Não mudou a nota de comportamento, porque o
  roteiro não confere esses casos.
