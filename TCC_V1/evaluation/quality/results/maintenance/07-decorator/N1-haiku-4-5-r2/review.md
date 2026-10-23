# Revisão: manutenção, cenário 07

## Visão geral

A base é um serviço Javalin em sete pacotes (`hotel` monta tudo, `hotel.web` tem as rotas, `hotel.reservations` as
regras e o acesso às reservas, `hotel.messages` a lista de mensagens, mais `rooms`, `documents` e `reports`). Os dados
ficam no PostgreSQL, com migrações do Flyway. Uma confirmação percorre este caminho: `HttpApi` →
`ReservationService.confirm` → `EmailSender.send` → `SentMessages.add`, que grava na tabela `messages`. O agente mudou
pouco código e tudo no mesmo lugar. As três regras novas (bloquear endereço inválido, desviar no modo de teste, cópia
para o arquivo) entraram como `if` dentro de `SentMessages.add` (`src/main/java/hotel/messages/SentMessages.java:29-51`),
a classe que grava no banco, e o modo de teste é lido do ambiente dentro dela (`SentMessages.java:21`). Ele também
criou um `EmailValidator`, a migração `V2` com a coluna `status`, e 8 testes novos. Pela API, tudo o que o dono pediu
funciona: as 65 conferências passaram. O mais sério é o design. O texto avisa que vêm mais regras (madrugada, limite
por dia, cada uma valendo sempre, só no teste ou só fora dele) e combinações diferentes por instalação (a segunda
pousada sem a cópia). Nada disso tem onde encaixar: a combinação por modo está fixa em código, e para desligar a cópia
seria preciso mexer na classe.

## Arquitetura

**Números:** 7 pacotes, 17 dependências entre eles, 1 ciclo (`hotel.documents` ↔ `hotel.reservations`). A base tem
os mesmos 7 pacotes, as mesmas 17 dependências e o mesmo ciclo. O que mudou foi `hotel.messages`, de 3 classes e 75
linhas para 4 classes e 162 linhas. `SentMessages` foi de 56 para 121 linhas.
**Nota:** com problemas

O agente não criou dependência nova nem ciclo novo; o ciclo `documents` ↔ `reservations` já vinha da base. A borda
HTTP continua fina: `GET /messages` só devolve `sentMessages.all()` (`src/main/java/hotel/web/HttpApi.java:81`).

O defeito é de lugar. As regras de envio moram na classe de acesso a dados. `SentMessages` agora valida o endereço
(`SentMessages.java:30`), decide o desvio do modo de teste (`SentMessages.java:38-42`) e decide a cópia
(`SentMessages.java:46-48`), além de montar o SQL de três inserts (`SentMessages.java:56`, `:73`, `:90`). Enquanto
isso, `EmailSender`, que é o envio de fato, segue só repassando (`src/main/java/hotel/messages/EmailSender.java:12-14`).
A configuração também saiu da montagem. `Hotel` cria `new SentMessages(dataSource)` (`src/main/java/hotel/Hotel.java:23`),
e é o construtor de `SentMessages` que lê `System.getenv("HOTEL_TEST_MODE")` (`SentMessages.java:21`). Quem monta o
sistema não escolhe o modo nem as regras. É uma mistura localizada, mas está justamente no ponto que o próximo pedido
vai mexer.

## Design de baixo nível

**Números:** das 3 regras de envio pedidas, 0 estão numa estrutura própria e 3 estão em `if` dentro de
`SentMessages.add` (`SentMessages.java:30`, `:38`, `:46`). A combinação por modo (cópia só fora do teste) está escrita
dentro do envio (`SentMessages.java:46`) e não na montagem. A cópia para o arquivo não pode ser desligada por
instalação.
**Classe:** omissão
**Exagero:** contenção
**Nota:** com problemas

O pattern pedido é o Decorator. Ele cobre três regras em volta do mesmo envio, que se combinam conforme o modo, e as
regras que ainda vão chegar. A referência chama de omissão o caso em que "os três `if` (o endereço, o modo de teste, a
cópia) ficam dentro do envio", e é o que está no código. `SentMessages.add` (`SentMessages.java:29-51`) testa
`EmailValidator.isValid(to)` (`:30`), depois `if (testMode)` para trocar destinatário, assunto e texto (`:38-42`),
depois `if (!testMode)` para a cópia (`:46-48`). Não existe interface de envio, não existe embrulho por regra, e a
classe `Hotel` não decide nada (`Hotel.java:23-25`). A regra da madrugada ou o limite por dia viraria outro `if` com
outra condição de modo dentro do mesmo método. Atender a segunda pousada, que não quer a cópia, pediria uma nova flag
nesta classe. O único passo na direção certa é a validação em classe separada (`src/main/java/hotel/messages/EmailValidator.java:5-21`),
mas ela é um utilitário estático chamado de dentro do envio, não uma peça que se põe ou se tira.

Exagero não há. O agente não criou nenhuma interface, fábrica ou camada a mais.

O resto do código novo tem defeitos localizados:
- **Duplicação:** `addSent` e `addBlocked` (`SentMessages.java:53-68` e `:70-85`) são o mesmo método copiado, e só o
  literal `'SENT'`/`'BLOCKED'` muda. `addArchiveCopy` (`:87-99`) repete o mesmo insert pela terceira vez.
- **Tipo solto:** a situação da mensagem é texto (`src/main/java/hotel/messages/Message.java:3`, `String status`, e os
  literais em `SentMessages.java:63` e `:80`), quando o próprio projeto usa `enum` para isso (`ReservationStatus`).
- **Parâmetro morto:** `addArchiveCopy` recebe `Message original` (`SentMessages.java:87`) e não usa.
- **Classe utilitária:** `EmailValidator` só tem um método estático e não tem construtor privado
  (`EmailValidator.java:3`).

O código que veio da base continua como estava.

## Testes

**Números:** 34 testes rodaram, 0 falhas, 0 erros, 0 pulados; 34 métodos escritos (a base tinha 26). Cobertura total
de 93,5% das linhas (516/552) e 89,0% dos ramos (121/136); na base eram 93,8% e 87,1%. Em `hotel.messages`, 74 de 83
linhas e 22 de 22 ramos.
**Nota:** com problemas

O agente acrescentou `EmailValidatorTest` (2 testes) e `SentMessagesTest` (6 testes, contra o banco). Também ajustou
dois testes da base para esperar a cópia: `src/test/java/hotel/reservations/ReservationServiceTest.java:91-101`, que
ficou mais forte (agora confere o destinatário, o assunto e o status da cópia), e `src/test/java/hotel/web/HttpApiTest.java:81-83`.
Nenhum teste da base foi apagado ou enfraquecido.

As regras novas são conferidas com valores:
- o bloqueio de "ana@gmail", "ana.gmail.com", "@gmail.com" e "ana@@gmail.com" (`src/test/java/hotel/messages/EmailValidatorTest.java:18-28`);
- o status `BLOCKED` com o endereço original e sem cópia (`src/test/java/hotel/messages/SentMessagesTest.java:49-73`);
- o destinatário e o assunto "[Cópia] " (`SentMessagesTest.java:34-46`);
- o desvio do modo de teste, com o endereço, "[TESTE] " e a primeira linha (`SentMessagesTest.java:76-87`);
- a ausência de cópia e o bloqueio no modo de teste (`SentMessagesTest.java:90-107`).

Pela pergunta da mutação, ficam sem rede:
- **O texto da cópia:** nenhum teste confere que o corpo da cópia é igual ao original (`SentMessagesTest.java:34-46`
  não olha `body()` da cópia). Se a cópia saísse com outro texto, nenhum teste falharia.
- **O "ponto no meio" do domínio:** o validador aceita "ana@gmail." e "ana@.com" (`EmailValidator.java:20` só pede um
  ponto em qualquer lugar do domínio), e nenhum teste usa esses casos.
- **O campo `status` na API:** `HttpApiTest` não lê o campo `status` de `GET /messages`.
- **A variável `HOTEL_TEST_MODE`:** a leitura (`SentMessages.java:21`) não é exercitada; os testes passam o booleano
  direto.

## Banco

**Números:** 3 tabelas, todas com chave primária; 2 migrações aplicadas (`V1` igual à da base, `V2__add_message_status.sql`
nova); dinheiro em `numeric(10,2)`; nenhuma chave estrangeira sem índice; nenhuma data como texto. A coluna nova
`messages.status` é `text` sem CHECK.
**Nota:** com problemas

A mudança no esquema foi feita do jeito certo. É uma migração nova
(`src/main/resources/db/migration/V2__add_message_status.sql:1`), com `NOT NULL DEFAULT 'SENT'`, e por isso as
mensagens que já existem num banco em uso ficam válidas. `V1` não foi tocada. O SQL novo usa parâmetros
(`SentMessages.java:56`, `:73`, `:90`).

Há dois defeitos do agente:
- **`status` sem CHECK:** a coluna aceita qualquer texto, quando só pode ser `SENT` ou `BLOCKED`
  (`V2__add_message_status.sql:1`). As outras colunas de tipo sem CHECK já vinham da base.
- **Original e cópia sem transação:** as duas são gravadas em duas conexões, cada uma com o seu autocommit
  (`SentMessages.java:44` e `:47`, abrindo conexão em `:54` e `:88`). Se a segunda gravação falhar, a original fica
  sem cópia e a exceção chega a `ReservationService.confirm` depois de `store.update` (`src/main/java/hotel/reservations/ReservationService.java:62-68`).
  A reserva fica confirmada e a resposta é erro. Quem garante que a cópia vem "logo depois" da original no `ORDER BY id`
  é só o `synchronized` de `confirm` (`ReservationService.java:57`), que vale para um processo.

## Comportamento

**Números:** 65 de 65 conferências passaram:
- a parte comum: 46 de 46;
- depois de reiniciar: 3 de 3;
- o cenário no modo normal: 9 de 9;
- o cenário no modo de teste: 5 de 5;
- as reservas simultâneas: 2 de 2.
**Nota:** bom

Para o dono da pousada, as três regras funcionam como ele pediu. Mensagem para endereço inválido não sai e fica na
lista como `BLOCKED`, com o endereço original e sem cópia, também no modo de teste. Toda mensagem enviada gera, logo
depois dela, a cópia para `arquivo@pousadamarealta.com.br` com "[Cópia] " no assunto e o mesmo texto. Com
`HOTEL_TEST_MODE=true`, tudo vai para `testes@pousadamarealta.com.br` com "[TESTE] " e a linha "Destinatário original: …",
sem cópia. O que já funcionava continua funcionando, e os dados resistem a reiniciar.

Fora do roteiro, um ponto da regra do endereço: o validador aceita "ana@gmail." e "ana@.com"
(`EmailValidator.java:20`), e o texto pede "um nome com pelo menos um ponto no meio". São erros de digitação raros, e
mensagens para esses endereços sairiam em vez de serem bloqueadas.

## Dúvidas

- **A classe do pattern:** entre `omissão` e `malfeito`, fiquei com `omissão`, porque não há estrutura nenhuma: nem
  interface de envio, nem embrulhos. A separação do `EmailValidator` não chega a ser uma estrutura que convive com o
  jeito sem ela.
- **O "ponto no meio":** o roteiro não testa "ana@gmail." nem "ana@.com". Por isso esse desvio não pesou na nota do
  comportamento.
