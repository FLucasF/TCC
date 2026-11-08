# Revisão: manutenção, cenário 07

## Visão geral

O projeto mantém a forma da base: sete pacotes, com a borda HTTP em `hotel.web` (`HttpApi`), as regras em
`hotel.reservations` (`ReservationService`), o acesso ao banco nas classes `Rooms`, `ReservationStore` e
`SentMessages`, o esquema em migrações do Flyway e a montagem em `hotel.Hotel`. Uma confirmação de reserva entra por
`POST /reservations/{id}/confirmation` (`final/src/main/java/hotel/web/HttpApi.java:68`), passa por
`ReservationService.confirm` (`ReservationService.java:57-70`), que grava a reserva e chama `EmailSender.send`; a
mudança do agente ficou toda em `hotel.messages`: `EmailSender` virou uma casca que monta uma pilha de `MessageSender`
(`EmailSender.java:9-16`), com o envio real em `BasicMessageSender` e um embrulho por regra
(`EmailValidationDecorator`, `ArchiveCopyDecorator`, `TestModeDecorator`), e a tabela `messages` ganhou a coluna
`status` numa migração nova (V2). Os 65 pontos do roteiro passam, os 33 testes passam, e nada da base foi apagado. O
que há a apontar é pequeno e localizado: a pilha é montada dentro de `EmailSender` (e não em `Hotel`) a partir de um
único booleano, o validador grava no banco por conta própria, a conferência do "ponto no meio" do domínio é mais
frouxa que a regra do dono, e o README não foi atualizado com o campo `status` nem com `HOTEL_TEST_MODE`.

## Arquitetura

**Números:** 7 pacotes, 17 dependências entre eles, 1 ciclo (`hotel.documents` <-> `hotel.reservations`); os mesmos 7
pacotes, as mesmas 17 dependências e o mesmo ciclo em `measures/dependencies-base.md`. O pacote `hotel.messages` foi
de 3 para 9 classes (75 para 188 linhas); os outros não mudaram.
**Nota:** bom

O agente não criou dependência nova entre pacotes nem ciclo novo: o ciclo entre `hotel.documents` e
`hotel.reservations` já estava na base (`measures/dependencies-base.md`, seção "Os ciclos"). As camadas continuam
separadas: nenhuma das classes novas conhece o Javalin, e o único JDBC do pacote de mensagens continua em
`SentMessages` (`final/src/main/java/hotel/messages/SentMessages.java:21-35`); os decoradores só chamam o próximo
(`ArchiveCopyDecorator.java:16-18`, `TestModeDecorator.java:18`). A borda HTTP não mudou: `GET /messages` continua
só devolvendo `sentMessages.all()` (`HttpApi.java:79`), e o campo `status` sai pelo record `Message`
(`Message.java:3`). A escolha do modo fica na montagem, `Hotel.java:21`, e desce por um parâmetro do construtor
(`Hotel.java:26`). Dois reparos miúdos, que não mudam a nota: `Hotel` lê `System.getenv` dentro do construtor
(`Hotel.java:21`), enquanto o `main` passa o ambiente explicitamente para `Database` (`Hotel.java:43-44`), de modo que
quem constrói `Hotel` nos testes (`src/test/java/hotel/web/HttpApiTest.java:35`) depende do ambiente da máquina; e a
pilha de envio é montada em `EmailSender` (`EmailSender.java:9-16`), e não em `Hotel`, que é a classe de montagem (isso
pesa no design, abaixo).

## Design de baixo nível

**Números:** 3 regras pedidas, 3 embrulhos (`EmailValidationDecorator`, `ArchiveCopyDecorator`, `TestModeDecorator`),
todos sobre a mesma interface `MessageSender`, mais o envio real `BasicMessageSender`; 0 regras fora da estrutura;
0 `if` sobre o modo de teste dentro dos embrulhos; 1 `if` sobre o modo, na montagem (`EmailSender.java:10-14`).
**Classe:** acerto
**Exagero:** contenção
**Nota:** com problemas

O pattern pedido está lá e aplicado do jeito que a referência descreve. A interface de envio é `MessageSender`
(`final/src/main/java/hotel/messages/MessageSender.java:4-6`); o envio de dentro, `BasicMessageSender`, só grava a
mensagem como `SENT` (`BasicMessageSender.java:13-15`) e não sabe de modo nenhum. Cada regra é um embrulho com a
mesma interface, que chama o próximo: o bloqueio de endereço inválido (`EmailValidationDecorator.java:15-20`), a
cópia para o arquivo, mandada pelo próximo só quando a original saiu `SENT` (`ArchiveCopyDecorator.java:16-19`), e o
desvio para a caixa de testes com "[TESTE] " e a linha "Destinatário original: " (`TestModeDecorator.java:16-18`). A
combinação por modo é decidida fora das regras: `EmailSender` empilha `TestMode` ou `ArchiveCopy` sobre o envio real
conforme o booleano, e a validação por cima dos dois, para valer sempre (`EmailSender.java:9-15`). As regras
anunciadas (madrugada, limite por dia) entrariam como mais um embrulho cada, encaixado no mesmo `if`. Não há exagero:
a interface tem quatro implementações de verdade, não há fábrica nem builder, e `MessageStatus` é o enum certo para
o campo novo (`MessageStatus.java:3-6`).

O que pesa na nota são defeitos pequenos, mas que um revisor pediria para mudar:

- A montagem da pilha ficou dentro de `EmailSender`, que virou uma casca que só repassa (`EmailSender.java:19-21`) e
  esconde a pilha de `Hotel`; `ReservationService` continua dependendo da classe concreta
  (`ReservationService.java:17-20`) e não de `MessageSender`. A segunda pousada, que "não quer a cópia para o
  arquivo", teria de editar `hotel.messages` em vez da classe de montagem, e a combinação é um booleano único
  (`EmailSender.java:8`), não uma pilha que a montagem escolhe.
- `EmailValidationDecorator` grava a mensagem bloqueada direto em `SentMessages` (`EmailValidationDecorator.java:7`,
  `:17`), um segundo caminho para o banco ao lado de `BasicMessageSender`, com o literal `"EMAIL"` repetido nos dois
  (`BasicMessageSender.java:14`, `EmailValidationDecorator.java:17`). O resultado é o pedido (a bloqueada fica com o
  endereço original e não passa pelo desvio de teste), mas o embrulho deixou de ser só um embrulho.
- A conferência do domínio é `domain.contains(".")` (`EmailValidationDecorator.java:31`): aceita `ana@gmail.` e
  `ana@.com`, quando o dono pediu "um nome com pelo menos um ponto no meio". Os quatro inválidos do roteiro são
  recusados, mas a regra não é exatamente a escrita.
- `Hotel` lê o ambiente no construtor (`Hotel.java:21`) em vez de receber o modo, destoando do `main`, que passa
  `System.getenv()` para `Database` (`Hotel.java:43-44`).

O resto do código não mudou e continua limpo: `BigDecimal` para dinheiro e `LocalDate` para datas na base, métodos
curtos, exceções com causa (`SentMessages.java:33-35`). O README não foi atualizado com o campo `status`, a cópia nem
`HOTEL_TEST_MODE` (`final/README.md:39`, `:55`), e o pedido dizia que o README explica como o sistema é.

## Testes

**Números:** 33 testes rodaram, 0 falharam, 0 pulados; 33 métodos de teste escritos (a base tinha 26, todos
mantidos); cobertura de 94,1% das linhas (512 de 544) e 85,4% dos ramos (111 de 130); a base, 93,8% e 87,1%. O pacote
`hotel.messages` cobre 69 de 74 linhas e 12 de 16 ramos.
**Nota:** bom

O agente testou o que mudou, com valores. `EmailSenderTest` (`final/src/test/java/hotel/messages/EmailSenderTest.java`)
confere, contra o banco, os quatro endereços inválidos bloqueados e gravados (`:22-36`), o válido que sai `SENT` com
o endereço original (`:40-46`), a cópia logo depois da original com `arquivo@pousadamarealta.com.br`, "[Cópia] " no
assunto, o mesmo texto e `SENT` (`:50-63`), a bloqueada sem cópia (`:67-73`), o modo de teste com o endereço de
testes, "[TESTE] " e a primeira linha "Destinatário original: ana@gmail.com" (`:78-87`), sem cópia (`:92-96`), e a
bloqueada no modo de teste com o endereço original (`:102-109`). Os testes da base foram adaptados sem enfraquecer:
`ReservationServiceTest` passou a exigir 2 mensagens e confere o destinatário e o assunto da cópia (`:91-98`), e
`HttpApiTest` confere a cópia pela API (`:81-83`). A pergunta da mutação: trocar "[Cópia] " ou "[TESTE] ", apagar a
linha do destinatário original ou inverter a ordem da pilha derrubaria um teste; apagar `atIndex !=
email.lastIndexOf('@')` (`EmailValidationDecorator.java:27`) não derrubaria nenhum, porque os inválidos dos testes não
incluem um endereço com dois `@` (`EmailSenderTest.java:24-27`; o roteiro do comportamento cobre esse caso, os testes
do projeto não); e trocar `contains(".")` por algo que aceite ponto na ponta também passaria. Fica sem rede também o
campo `status` no JSON de `GET /messages`, que `HttpApiTest` não lê (`:81-83`). Observação: `HttpApiTest` constrói
`Hotel` (`:35`), que lê `HOTEL_TEST_MODE` do ambiente (`Hotel.java:21`); com a variável ligada na máquina, o teste
da linha 81 falharia.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), 2 migrações do Flyway (V1 da base, igual; V2 nova);
nenhuma tabela sem chave primária, dinheiro todo em `numeric(10,2)`, nenhuma chave estrangeira sem índice, nenhuma
data em texto; as colunas de situação (`messages.status`, `messages.channel`, `reservations.status`,
`reservations.rate_plan`, `rooms.type`) sem `CHECK`; 0 transações explícitas.
**Nota:** bom

O esquema evoluiu do jeito certo: a V1 da base ficou intacta (`measures/database.md`, "igual"), e a coluna nova veio
numa migração própria, `ALTER TABLE messages ADD COLUMN status text NOT NULL DEFAULT 'SENT'`
(`final/src/main/resources/db/migration/V2__add_message_status.sql:2`), de modo que as mensagens que uma instalação já
tinha ganham `SENT` sem perder nada. O código continua com SQL parametrizado (`SentMessages.java:23-28`, `:41`),
try-with-resources e o pool da base (`Database.java:23-28`); o `+` nos candidatos do script é só a constante
`COLUMNS` de `ReservationStore`. O enum é gravado pelo nome e lido com `valueOf` (`SentMessages.java:28`, `:51`),
consistente com a base. Sem `CHECK` em `messages.status`, o banco aceitaria outro texto, mas isso segue o padrão da
base, que não tem `CHECK` em nenhuma coluna de situação. A original e a cópia são dois `INSERT` fora de transação
(`ArchiveCopyDecorator.java:16-18`); se o segundo falhasse, ficaria a original sem cópia, o que é o mesmo risco que
a base já tem entre gravar a reserva e gravar o e-mail (`ReservationService.java:63-68`). A duplicação de reserva
continua protegida pela restrição `EXCLUDE` da base (`V1__create_tables.sql:38-40`).

## Comportamento

**Números:** 65 de 65 conferências passaram: a parte comum 46 de 46, os dados depois de reiniciar 3 de 3, o cenário
no modo normal 9 de 9, o cenário no modo de teste 5 de 5, as duas reservas ao mesmo tempo 2 de 2.
**Nota:** bom

Nada falhou. No modo normal, a mensagem do hóspede sai `SENT`, a cópia vem logo depois com "[Cópia] " e o mesmo
texto, e os quatro inválidos do roteiro (`ana@gmail`, `ana.gmail.com`, `ana@@gmail.com`, `@gmail.com`) ficam como uma
mensagem só, `BLOCKED`, com o endereço original e sem cópia. Com `HOTEL_TEST_MODE=true`, vai uma mensagem só para
`testes@pousadamarealta.com.br`, com "[TESTE] " e a primeira linha "Destinatário original: ana@example.com", e o
inválido continua bloqueado com o endereço original. A parte comum ficou como estava e os dados sobrevivem ao
reinício. Fora do roteiro, uma ressalva já contada no design: `ana@gmail.` ou `ana@.com` passariam como válidos
(`EmailValidationDecorator.java:31`), e para o dono isso é um e-mail que "vai para lugar nenhum" sem aparecer
bloqueado; é um caso de borda que o roteiro não exercita e não muda a nota.

## Dúvidas

- A referência pede a combinação "decidida na montagem (o `main` ou a classe que monta as peças)". Aqui ela está
  num `if` do construtor de `EmailSender` (`EmailSender.java:10-14`), que `Hotel` alimenta com o booleano. Tratei
  `EmailSender` como a montagem da pilha e dei `acerto`; se a leitura for que a montagem tinha de estar em `Hotel`, o
  lugar errado desconta, mas não chega a `malfeito`, porque nenhuma regra pergunta o modo.
- "Pelo menos um ponto no meio" pode ser lido como "pelo menos um ponto" ou como "um ponto que não está na ponta";
  julguei pela leitura mais estrita, e o roteiro não decide.
