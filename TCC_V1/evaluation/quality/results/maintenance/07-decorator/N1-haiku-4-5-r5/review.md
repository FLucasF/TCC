# Revisão: manutenção, cenário 07

## Visão geral

A base já era um sistema em pacotes por assunto (`hotel.web` com as rotas Javalin, `hotel.reservations` com o serviço e o acesso ao banco, `hotel.messages` com o envio, `hotel.rooms`, `hotel.reports`, `hotel.documents`), montado na classe `Hotel`, com os dados no PostgreSQL por migrações do Flyway. Uma confirmação entra por `HttpApi`, passa por `ReservationService`, que chama `EmailSender.send`, que grava em `SentMessages`. O agente mexeu só no pacote de mensagens: criou `EmailValidator` e `MessageRules` (as três regras novas), deu a `SentMessages` uma `MessageRules` que ela consulta antes de gravar (e grava a cópia), acrescentou `status` ao `Message` e a migração `V2__add_message_status.sql`, e leu `HOTEL_TEST_MODE` em `Hotel`. Pela API, tudo funciona (65 de 65 conferências). O mais sério é o design: o texto pede regras que se combinam por modo e por instalação, e que vão aumentar. O agente pôs as três regras num único método, com `if`s, e a combinação por modo ficou escrita dentro dele (`MessageRules.java:23-41`). Além disso, quem chama essas regras é a classe que grava no banco (`SentMessages.java:23`).

## Arquitetura

**Números:** 7 pacotes e 17 dependências entre eles, iguais aos da base. Um ciclo (`hotel.documents` ↔ `hotel.reservations`), que já existia na base. `hotel.messages` passou de 3 para 7 classes (contando os dois records internos) e de 75 para 178 linhas, e continua sem depender de nenhum outro pacote do projeto.
**Nota:** com problemas

A mudança não criou dependências nem ciclos novos. A leitura do ambiente ficou na montagem: `Hotel.java:22-26` recebe o `Map` do ambiente e cria a `MessageRules`, e `Hotel.java:51` passa `System.getenv()`. O defeito é localizado e está no lugar das regras. `SentMessages` é a classe de acesso ao banco (o SQL de `SentMessages.java:36-52`), e agora também decide o que se envia: chama `rules.processMessage` (`SentMessages.java:23`) e resolve se grava uma segunda mensagem (`SentMessages.java:28-31`). O lugar natural dessas regras seria o envio (`EmailSender`, que ficou intocado e só repassa: `EmailSender.java:12-14`). Com isso, o acesso aos dados passou a conter a regra de negócio. O ciclo documents↔reservations vem da base e não conta contra o agente.

## Design de baixo nível

**Números:** 3 regras pedidas (bloqueio, cópia, modo de teste), nenhuma numa peça própria. As três estão em `if`s dentro de `MessageRules.processMessage` (`MessageRules.java:23-41`). As regras anunciadas (madrugada, limite diário, combinação por instalação) não têm onde entrar sem mexer nesse método.
**Classe:** omissão
**Exagero:** contenção
**Nota:** com problemas

O pattern pedido é o Decorator: cada regra embrulha o envio, e a montagem empilha os embrulhos conforme o modo. Na `final`, não há interface de envio nem embrulhos. O bloqueio está em `MessageRules.java:24-26`, o desvio para testes em `MessageRules.java:32-36` e a cópia em `MessageRules.java:37-39` e `43-48`. A regra "no modo de teste não há cópia" está escrita no `if/else` do próprio método (`MessageRules.java:32-40`), e não na montagem. A cópia não é um envio que passa pelas outras regras: ela é devolvida como dado (`ArchiveCopy`) e gravada à parte por `SentMessages.java:28-31`. Isso é exatamente o caso "os três `if` dentro do envio" que a referência chama de omissão. O pedido também diz que a segunda pousada não quer a cópia. Para isso existe o campo `enableArchiveCopy`, mas ele é fixado em `true` no construtor (`MessageRules.java:14`), então não há como desligá-lo sem mudar o código. Os endereços também estão fixos no construtor (`MessageRules.java:15-16`).

Contenção: o agente não criou interfaces, fábricas nem camadas que só repassam. Os dois records (`MessageToSend` e `ArchiveCopy`, `MessageRules.java:50-53`) são pequenos e servem à solução escolhida.

O resto do código novo:
- a situação da mensagem é `String` solta (`"BLOCKED"` e `"SENT"` em `MessageRules.java:25,36,39`, `SentMessages.java:30` e `Message.java:3`) quando caberia um `enum`;
- `archiveCopy` usa `null` como "sem cópia" (`MessageRules.java:25,36,47`, conferido em `SentMessages.java:28`);
- `EmailValidator.isValid` (`EmailValidator.java:5-30`) é estático e correto para a regra do texto, e é razoavelmente simples;
- `Hotel(DataSource)` (`Hotel.java:34-36`) lê `System.getenv()` por dentro, um atalho que os testes usam (`HttpApiTest.java:35`).

São defeitos localizados. O que vinha da base continua como estava.

## Testes

**Números:** 47 testes rodaram, 0 falhas, 0 erros, 0 pulados (a base tinha 26). 47 métodos escritos. Cobertura de linhas 93,9% (524/558) e de ramos 88,9% (128/144); na base eram 93,8% e 87,1%. No pacote `hotel.messages`: 79 de 86 linhas e 29 de 30 ramos.
**Nota:** bom

O agente escreveu 21 testes em três níveis.
- Unidade: `EmailValidatorTest` cobre os casos do texto, "ana@gmail" (`EmailValidatorTest.java:41`), "ana.gmail.com", dois @ e sem parte local. `MessageRulesTest` confere com valores o assunto "[Cópia] ", o endereço do arquivo e o mesmo texto (`MessageRulesTest.java:31-33`). Também confere o "[TESTE] " e a primeira linha "Destinatário original: …" exata, e o bloqueio também no modo de teste.
- Banco: `SentMessagesIntegrationTest` confere a ordem (a cópia logo depois da original) e o status gravado.
- API: `InvalidEmailHttpApiTest` e `TestModeHttpApiTest`.

Pela pergunta da mutação, os principais erros seriam pegos: tirar o prefixo "[Cópia] ", trocar a ordem do "Destinatário original", gerar a cópia no modo de teste ou deixar de bloquear no modo de teste fariam algum teste falhar. Os testes da base que mudaram foram ajustados ao novo comportamento (duas mensagens em vez de uma), não enfraquecidos: `ReservationServiceTest.java:95-107` e `HttpApiTest` passaram a conferir também a cópia e o status.

Ressalvas pequenas:
- `ReservationServiceTest.java:175` se chama `invalidEmailIsBlocked`, mas testa um endereço válido (o nome engana);
- `ReservationServiceTest.java:198` supõe que a reserva tem id 1;
- os testes que montam `new Hotel(database.dataSource())` dependem de `HOTEL_TEST_MODE` não estar no ambiente de quem roda o `mvn verify`.

## Banco

**Números:** 3 tabelas, todas com chave primária. Dinheiro em `numeric(10,2)` e nenhuma data em texto. As conferências acusaram só colunas de situação sem `CHECK` (`messages.status` é nova; as outras vêm da base). Nenhuma transação explícita. 2 migrações: V1 igual à da base e V2 nova.
**Nota:** com problemas

A evolução do esquema foi feita do jeito certo. O agente não editou a V1 e acrescentou `V2__add_message_status.sql:2` (`ADD COLUMN status text NOT NULL DEFAULT 'SENT'`), que preserva as mensagens já gravadas num banco em uso. O SQL novo usa parâmetros (`SentMessages.java:39-44`) e try-with-resources.

Há dois defeitos:
- `messages.status` aceita qualquer texto, sem `CHECK (status IN ('SENT','BLOCKED'))`.
- A original e a cópia são duas escritas em conexões separadas, sem transação (`SentMessages.java:25-31`, cada uma em `insertMessage`, `SentMessages.java:37`). Uma falha entre as duas deixa a mensagem gravada sem a cópia que o contador espera.

A proteção contra reserva dupla (o `EXCLUDE` da V1) vem da base e continua lá.

## Comportamento

**Números:** 65 de 65 conferências passaram, nenhuma falhou:
- parte comum: 46/46;
- depois de reiniciar: 3/3;
- cenário no modo normal: 9/9;
- cenário no modo de teste: 5/5;
- reservas simultâneas: 2/2.

**Nota:** bom

Para o dono da pousada, as três regras funcionam do jeito que ele descreveu:
- endereço inválido ("ana@gmail", "ana.gmail.com", "ana@@gmail.com", "@gmail.com") aparece como BLOCKED, com o endereço original e sem cópia;
- toda mensagem enviada gera, logo em seguida, a cópia para o arquivo, com "[Cópia] " e o mesmo texto;
- com `HOTEL_TEST_MODE=true`, nada sai para o hóspede: vai para testes@, com "[TESTE] " e a linha "Destinatário original", sem cópia, e o bloqueio continua.

O resto do sistema continua igual, e os dados persistem. O que não se vê pela API: a segunda pousada não consegue desligar a cópia sem mudar o código (`MessageRules.java:14`), e as regras futuras exigirão mexer no mesmo método. Isso já foi contado no design.

## Dúvidas

- Na arquitetura, fiquei entre `com problemas` e `bom`. A estrutura de pacotes não mudou e não há ciclo novo, mas pôr a decisão de envio dentro da classe de acesso ao banco (`SentMessages.java:23-31`) é uma mistura que um revisor pediria para desfazer. Fiquei com `com problemas`.
- No banco, a falta de transação entre a original e a cópia só aparece numa falha entre as duas gravações. Pesei como defeito a corrigir, não como risco sério.
