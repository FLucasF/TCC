# Revisão: construção, cenário 07

## Visão geral

O projeto é um serviço Javalin em 10 pacotes: `controller` (rotas e DTOs), `service` (reservas, quartos, relatórios),
`util` (preço e comprovante), `messaging` e `messaging.rules` (o envio de e-mails e as regras dele), `repository`
(JDBC puro), `config` (o pool HikariCP num `static` e a criação do esquema) e `model`. Uma requisição entra em
`ApiController`, que lê o corpo e chama `ReservationService`. O serviço calcula o preço com `PricingCalculator`
(`BigDecimal`, HALF_EVEN) e grava pelo `ReservationRepository`. Ao confirmar, ele monta o e-mail e chama
`MessagingService.sendEmail`, que passa a mensagem por uma lista de três regras e grava o resultado na tabela
`messages`. Os dados ficam no PostgreSQL, num esquema criado com `CREATE TABLE IF NOT EXISTS` no código. O que há de
mais sério:
- nada impede duas reservas do mesmo quarto feitas ao mesmo tempo, e nas 10 rodadas do roteiro as duas foram criadas
  nas 10;
- os testes dependem de Docker (Testcontainers) e ignoram o `DB_URL`, então aqui nenhum rodou e o `mvn verify` não
  passa;
- o dinheiro sai no JSON como `double` (`0.0`, `249.9`);
- a combinação das regras de e-mail por modo ficou escrita dentro das próprias regras.

## Arquitetura

**Números:** 10 pacotes, 22 dependências entre eles, 1 ciclo (`messaging` ↔ `messaging.rules`). O Javalin aparece só em
`com.pousadamarealta` e `controller`, e o JDBC só em `config` e `repository` (`measures/dependencies.md`). As maiores
classes têm 233 linhas (`ReservationRepository`), 222 (`ApiController`) e 192 (`ReservationService`).
**Nota:** com problemas

As camadas existem e as dependências vão no sentido certo: as rotas chamam os serviços, e os serviços chamam os
repositórios. Os serviços não conhecem o Javalin nem o SQL, e o SQL fica todo em `repository/`. A montagem fica no
`main` (`Application.java:34-56`). Há, porém, defeitos localizados:

- **O ciclo entre `messaging` e `messaging.rules`.** As regras implementam `messaging.MessageRule`
  (`messaging/rules/ArchiveCopyRule.java:3-7`). Ao mesmo tempo, `MessagingService` importa as regras concretas e dá
  `new` nelas no próprio construtor (`messaging/MessagingService.java:3-5,24-26`). A escolha de quais regras valem
  deveria estar na montagem; como está dentro do serviço, nasce o ciclo.
- **O acesso ao banco por um `static` global.** O pool é um campo estático, `Database.dataSource`
  (`config/Database.java:12`), e todos os repositórios chamam `Database.getConnection()` direto
  (`repository/ReservationRepository.java:25`, `repository/MessageRepository.java:20`,
  `repository/RoomRepository.java:21`), sem receber o `DataSource`. A troca do banco nos testes acontece reinicializando
  esse estático (`ReservationSystemIT.java:40`).
- **Uma regra de negócio dentro do handler.** Quando a segunda via existe, quem decide é a rota, com
  `reservation.getAmountPaid().doubleValue() == 0` (`controller/ApiController.java:184`). Enquanto isso, o serviço tem um
  `getReceipt` com a mesma regra escrita de outro jeito, e ninguém o chama (`service/ReservationService.java:140-149`).
  O check-out e a segunda via também vão buscar o quarto na própria rota (`controller/ApiController.java:176,188`).

## Design de baixo nível

**Números:** 3 regras de envio, todas na estrutura (`EmailValidationRule`, `TestModeRedirectRule`, `ArchiveCopyRule`,
aplicadas em laço em `MessagingService.java:33-35`). Nenhuma das 3 é escolhida na montagem: a lista é fixa no
construtor do serviço (`MessagingService.java:24-26`). Duas delas perguntam pelo modo de teste por dentro
(`ArchiveCopyRule.java:12`, `TestModeRedirectRule.java:12`).
**Classe:** malfeito
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** Uma lista de filtros aplicada pelo envio é a alternativa que a referência aceita, desde que cada regra
seja uma peça própria e a combinação por modo seja decidida na montagem. A primeira metade foi cumprida: cada regra é
uma classe com `apply(OutboundMessage, RuleContext)` (`messaging/MessageRule.java:3-5`).

A segunda metade, não:

- **O modo está dentro das regras.** A cópia para o arquivo verifica se está em modo de teste
  (`rules/ArchiveCopyRule.java:12`, `context.isTestMode()`), e o desvio para testes também
  (`rules/TestModeRedirectRule.java:12`). A referência cita exatamente esse caso, "a cópia que verifica se está em modo
  de teste", como malfeito.
- **A combinação não sai do código.** A lista é montada com `new` dentro de `MessagingService` (`:24-26`), e o serviço
  ainda guarda o `testMode` (`:17,31`) para repassá-lo às regras. Para a "segunda pousada" que não quer a cópia, ou para
  uma regra de madrugada que valha "só fora do teste", seria preciso editar a classe da regra ou o serviço; a montagem
  (`Application.java:39`) só passa um booleano.
- **As regras dependem da ordem da lista.** Cada uma confere `message.isBlocked()` por conta própria
  (`TestModeRedirectRule.java:12`, `ArchiveCopyRule.java:12`). Com isso, a validação precisa vir antes das outras na
  lista fixa.
- **As mensagens extras saem sempre como `SENT`.** O serviço grava cada uma com `MessageStatus.SENT` fixo
  (`MessagingService.java:47-55`), sem passar pelas regras.

**O exagero.** Não há estrutura sem uso: a interface `MessageRule` tem três implementações, e não há fábricas nem
builders. O mais perto disso é o campo `RuleContext.originalRecipient` (`messaging/RuleContext.java:9,28-34`), que só
serve para a regra de teste reler um valor que ela mesma acabou de guardar. É pouco para chamar de exagero.

**O resto do código:**

- **Dinheiro em `double` nos DTOs.** O cálculo é em `BigDecimal` e certo (`util/PricingCalculator.java:51-72,96-97`),
  mas na saída vira `double`: `controller/dto/ReservationResponse.java:23-27,42-47`,
  `controller/dto/ReceiptResponse.java:22-24,37-39` e `controller/dto/RoomResponse.java:11,18`. Por isso o JSON traz
  `0.0` e `249.9` em vez de duas casas.
- **Código morto.** `ReservationService.getReceipt` não é chamado por ninguém (`service/ReservationService.java:140-149`).
  O `ObjectMapper` criado em `ApiController` nunca é usado (`controller/ApiController.java:25,35-36`).
- **A regra da segunda via, duplicada e errada.** Ela aparece em dois lugares e nos dois está errada:
  `ApiController.java:184` testa `amountPaid == 0` com `double`, e `ReservationService.java:143` testa só `PENDING`.
  Nenhuma das duas recusa a reserva cancelada (veja o comportamento).
- **Um `catch` que só complica.** `catch (Exception e)` faz um `instanceof` e relança a mesma exceção
  (`controller/ApiController.java:114-119`). Enquanto isso, o enum de plano inválido no corpo não é tratado e acaba em
  500.
- **A validação de e-mail fraca.** A regra só confere `domain.contains(".")` (`rules/EmailValidationRule.java:33`), o
  que aceita `ana@gmail.` e `ana@.com`. O texto pede um ponto "no meio".
- **Cópias de SQL.** A mesma lista de 16 colunas está repetida em quatro `SELECT`s
  (`repository/ReservationRepository.java:91-93,117-119,150-152,185-187`).
- **Exceções aninhadas nos serviços.** As exceções de domínio são classes internas dos serviços
  (`service/ReservationService.java:163-191`), e o controller depende delas pelo nome qualificado.

## Testes

**Números:** 18 métodos de teste em 2 classes (`ReservationSystemIT` com 15, `TestModeIT` com 3). Rodaram 3 execuções
de classe, com 0 falhas, 3 erros e 0 pulados. Cobertura de linhas 0 de 903 (0,0%) e de ramos 0 de 170 (0,0%)
(`measures/tests.md`). O `BUILD SUCCESS` do log só aparece porque o script roda com `-Dmaven.test.failure.ignore=true`.
**Nota:** ruim

Os três erros são iguais: "Could not find a valid Docker environment" (`measures/tests-maven.log`). Os dois arquivos
sobem um PostgreSQL próprio por Testcontainers (`ReservationSystemIT.java:30-34`, `TestModeIT.java:30-34`) e ignoram o
`DB_URL` que o texto diz que o sistema recebe. Sem Docker, nenhum teste roda e o `mvn verify` que o texto exige falha.
Há outros problemas de construção:

- **`TestModeIT` roda duas vezes.** O nome bate com o padrão `Test*` do surefire e com `*IT` do failsafe
  (`measures/tests.md`, a tabela por classe).
- **Portas fixas.** Os testes ocupam 7070 e 7071 (`ReservationSystemIT.java:65`, `TestModeIT.java:65`).
- **Testes em ordem e dependentes uns dos outros.** Eles usam `@Order` e ids fixos que valem só nessa sequência
  (`ReservationSystemIT.java:133`, `:274`, que supõe que a reserva é a nº 4).

Pela leitura, os testes conferem valores onde chegam:

- o preço da alta temporada, 599.76 (`ReservationSystemIT.java:216`);
- o desconto do não reembolsável, 74.97 e 424.83 (`:244-245`);
- as devoluções (`:375,415`) e os códigos 409 e 422 (`:308-309,334-335`);
- a cópia com "[Cópia] " (`:149-150`);
- o desvio de teste com "[TESTE] " e "Destinatário original" (`TestModeIT.java:561-563`).

Ficam sem rede:

- o texto exato do comprovante e do e-mail (só o assunto é conferido);
- a validação de e-mail além de `invalid.email` e `no-at-sign` (`ReservationSystemIT.java:255`, `TestModeIT.java:586`).
  Se alguém trocasse `domain.contains(".")` por `true`, nenhum teste falharia, e `ana@gmail` (o exemplo do próprio
  texto) não é testado;
- que a cópia não sai para a mensagem bloqueada (o teste da linha 283 olha só o status da original);
- o 400 para corpo inválido, para mês ou data fora do formato e para o arredondamento meio para o par.

Não há nenhum teste de unidade das regras de envio.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. O dinheiro está em
`numeric(10,2)` nas 5 colunas, e as datas em `date`. Conferências que acusaram:

- a chave estrangeira `reservations(room_number) -> rooms` sem índice;
- nenhum CHECK nas 5 colunas de situação e tipo;
- nenhuma restrição UNIQUE ou EXCLUDE;
- 0 transações explícitas e 0 `FOR UPDATE`;
- o esquema criado por `CREATE TABLE` no Java, com 0 migrações (`measures/database.md`).

**Nota:** com problemas

Os tipos estão certos e o SQL é todo parametrizado com `?`; o script não achou concatenação. As conexões são
devolvidas com try-with-resources, e há um pool (`config/Database.java:15-22`, e por exemplo
`repository/ReservationRepository.java:25-26`). Os dados sobrevivem ao reinício (3 de 3 no roteiro). Os defeitos:

- **O esquema não evolui.** Ele nasce de `CREATE TABLE IF NOT EXISTS` no código (`config/Database.java:37-81`), sem
  migração versionada, e não saberia acrescentar uma coluna a uma tabela que já existe.
- **Nada no banco protege a regra principal.** Um quarto não pode ter duas reservas na mesma noite, mas não há
  restrição de exclusão nem transação com trava: a conferência de conflito (`repository/ReservationRepository.java:115-146`)
  e o `INSERT` (`:17-61`) rodam em conexões separadas, em autocommit (`config/Database.java:20`). O efeito aparece no
  roteiro de concorrência, contado no comportamento.
- **Faltam o índice e as conferências.** Não há índice para `room_number`, a coluna mais consultada
  (`config/Database.java:62`), nem CHECK para status, plano e quantidade de hóspedes (`:53-59`).
- **Os quartos estão pela metade no banco.** A capacidade e a diária vivem só no enum (`model/RoomType.java:6-8`), e a
  tabela `rooms` guarda só o número e o tipo (`config/Database.java:37-42`).

## Comportamento

**Números:** 60 de 65 conferências passaram (`measures/behavior.md`):
- parte comum: 42 de 46;
- dados depois de reiniciar: 3 de 3;
- cenário, no modo normal: 9 de 9;
- cenário, no modo de teste: 5 de 5;
- reservas simultâneas: 1 de 2.

**Nota:** com problemas

O que o cenário pede funciona pela API, nos dois modos:
- a mensagem válida sai com `SENT` e a cópia vem logo depois;
- `ana@gmail`, `ana.gmail.com`, `ana@@gmail.com` e `@gmail.com` ficam `BLOCKED`, sem cópia;
- no modo de teste, a mensagem vai para `testes@` com "[TESTE] " e a primeira linha certa, sem cópia, e o bloqueio
  continua.

As falhas que pesam:

- **Duas reservas ao mesmo tempo.** Em 10 de 10 rodadas, os dois pedidos simultâneos para o mesmo quarto e as mesmas
  noites foram criados. Para o dono, quando o site recebe dois pedidos no mesmo instante, o quarto fica vendido duas
  vezes.
- **O dinheiro sem duas casas.** O JSON traz `"discount":0.0`, `"amountPaid":0.0` e `"nightlyRate":389.9`. O texto pede
  número com 2 casas, então o site que contava com `599.76` também recebe `249.9`. É o que derruba a conferência da
  diária do 201.
- **Corpo que não é JSON responde 500**, não 400 INVALID_REQUEST. Na mão, um `ratePlan` desconhecido (`"FOO"`) também
  dá 500 (`measures/behavior-service.log` e o meu teste: `InvalidFormatException` sem tratamento).
- **"O 201 comporta 3"** falhou porque o `number` do quarto sai como texto (`"201"`), e o roteiro procura número.
  Testei na mão e o valor está certo (`capacity: 3`). O texto não fixa o tipo do número do quarto: escolha razoável, que
  não pesa.

Fora do roteiro, testei na mão:

- **A segunda via sai para reserva cancelada** (200 para uma STANDARD confirmada e cancelada, e para uma NON_REFUNDABLE
  confirmada e cancelada), quando o texto diz que ela só existe para reserva confirmada, com check-in ou com check-out
  (`controller/ApiController.java:184`).
- **`ana@gmail.` é aceito como válido**: a mensagem saiu `SENT` e com cópia para o arquivo
  (`rules/EmailValidationRule.java:33`).

## Dúvidas

- **A classe do pattern** pode ser lida como alternativa equivalente: a estrutura de regras é uma lista de filtros com
  cada regra numa peça própria. Escolhi malfeito porque a combinação por modo está dentro das regras
  (`ArchiveCopyRule.java:12`, `TestModeRedirectRule.java:12`) e a lista é fixa no serviço, que é o caso que a
  referência dá como malfeito.
- **A nota dos testes** pode ser `ruim` ou `com problemas`, porque a falha vem do ambiente (não há Docker aqui). Os
  testes podem passar numa máquina com Docker, onde provavelmente seriam `com problemas`. Mas o texto dá o banco por
  `DB_URL` e pede que o `mvn verify` passe, e isso não acontece sem Docker; neste ambiente, o que se mediu foi 0% de
  cobertura.
- **O número do quarto como texto** é uma escolha que o texto deixava aberta.
