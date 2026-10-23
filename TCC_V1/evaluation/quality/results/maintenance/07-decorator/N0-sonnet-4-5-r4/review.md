# Revisão: manutenção, cenário 07

## Visão geral

A base é um serviço Javalin dividido em pacotes por assunto (`hotel.web` com as rotas, `hotel.reservations` com as
regras e o acesso ao banco das reservas, `hotel.messages` com o envio e a lista de mensagens, `hotel.reports`,
`hotel.rooms`, `hotel.documents`). Tudo é montado em `hotel.Hotel`, e os dados ficam no PostgreSQL, com migrações do
Flyway. Uma confirmação segue este caminho: `HttpApi` → `ReservationService.confirm` → `EmailSender.send` →
`SentMessages.add` (INSERT em `messages`). A mudança do agente é pequena e ficou só em `hotel.messages`: o
`EmailSender.send` ganhou três `if` (endereço inválido, modo de teste, cópia para o arquivo)
(`src/main/java/hotel/messages/EmailSender.java:21-43`), o `Message` ganhou um `status` em texto, e há uma migração
nova, `V2__add_message_status.sql`, e 7 testes novos. O sistema faz o que o dono pediu: as 65 conferências passaram e
os 33 testes também. O mais sério é o design. O texto avisa que vêm mais regras (madrugada, limite por dia), cada uma
valendo sempre, só no teste ou só fora dele, e que cada instalação vai querer outra combinação. Mesmo assim, as três
regras ficaram presas num único método, com o modo de teste lido de `System.getenv` dentro do construtor do
`EmailSender`. Hoje não dá para desligar a cópia na segunda pousada sem editar esse método.

## Arquitetura

**Números:** 7 pacotes, 17 dependências entre eles e 1 ciclo (`hotel.documents` ↔ `hotel.reservations`). A base tem
os mesmos pacotes, as mesmas dependências e o mesmo ciclo (`measures/dependencies-base.md`). Só mudou o tamanho de
`hotel.messages`, que foi de 75 para 126 linhas.
**Nota:** com problemas

O agente não mexeu nas camadas. A borda HTTP continua fina (`src/main/java/hotel/web/HttpApi.java:81` só devolve
`sentMessages.all()`), o SQL novo ficou em `SentMessages` (`src/main/java/hotel/messages/SentMessages.java:24-40`) e
o serviço de reservas não mudou. O ciclo documents↔reservations veio da base.

O defeito está na montagem. Na base, a configuração vinda do ambiente era lida num lugar só, `Hotel.main`
(`src/main/java/hotel/Hotel.java:42-43`). Agora o construtor público do `EmailSender` lê a `HOTEL_TEST_MODE` por
dentro (`src/main/java/hotel/messages/EmailSender.java:13`), e `Hotel` continua chamando
`new EmailSender(sentMessages)` (`src/main/java/hotel/Hotel.java:25`) sem saber que o modo existe. Por isso a
combinação das regras por instalação, que o texto pede, não pode ser decidida na montagem. Há ainda um efeito
concreto: os testes que usam esse construtor (`src/test/java/hotel/reservations/ReservationServiceTest.java:26`,
`src/test/java/hotel/web/HttpApiTest.java:35`) passam a depender do ambiente de quem roda o `mvn verify`. É um
defeito localizado, não uma mistura de camadas.

## Design de baixo nível

**Números:** há 3 regras pedidas (bloqueio, cópia, modo de teste), e as 3 estão fora de qualquer estrutura, como
`if` dentro de `EmailSender.send` (`EmailSender.java:22`, `:30`, `:38`). Não existe nenhuma interface de envio nem
nenhum embrulho. A combinação por modo (`if (!testMode)` para a cópia) também está dentro do envio.
**Classe:** omissão
**Exagero:** contenção
**Nota:** com problemas

O pattern pedido é o Decorator: cada regra seria um embrulho com a mesma interface do envio, e a montagem empilharia
os embrulhos conforme o modo e a instalação. O agente fez o caso descrito como omissão na referência. Os três `if`
ficaram dentro do envio (`src/main/java/hotel/messages/EmailSender.java:21-43`), e a combinação por modo foi escrita
à mão (a cópia só sai quando `!testMode`, em `EmailSender.java:38-40`). Nada aproxima a solução do pattern. A
próxima regra (madrugada, limite por dia) vai ser mais um `if` nesse mesmo método. Desligar a cópia na segunda
pousada também exige editar o método, porque não há onde configurar isso. Não há exagero: o agente não criou
nenhuma estrutura a mais.

O resto do código tem defeitos localizados:
- O status é texto solto (`"SENT"`/`"BLOCKED"`) em `src/main/java/hotel/messages/Message.java:3` e
  `EmailSender.java:23,36,39`, quando a própria base já usa `enum` para a situação da reserva (`ReservationStatus`).
- A sobrecarga antiga `add(channel, to, subject, body)` ficou sem nenhum uso
  (`src/main/java/hotel/messages/SentMessages.java:20-22`).
- A validação aceita domínios com o ponto na borda: `"ana@gmail."` e `"ana@.com"` passam, porque basta
  `domainPart.contains(".")` (`EmailSender.java:58`), e o texto pede "um ponto no meio". A conferência
  `localPart.isEmpty()` (`EmailSender.java:55`) repete o que `atIndex <= 0` (`:50`) já garante.
- O modo de teste é lido do ambiente dentro do construtor (`EmailSender.java:13`).
- O comentário da classe (`EmailSender.java:3`) e o README não mencionam o modo de teste, o bloqueio nem a cópia.

## Testes

**Números:** 33 testes rodaram, com 0 falhas, 0 erros e 0 pulados (a base tinha 26). São 33 métodos escritos, 7
deles novos, todos em `EmailSenderTest`. A cobertura é de 94,0% das linhas (498/530) e 86,6% dos ramos (116/134). Em
`hotel.messages`, são 56/61 linhas e 17/20 ramos.
**Nota:** com problemas

O que está bom:
- Os testes novos conferem as regras com valores. A cópia é conferida com destinatário, `"[Cópia] "` no assunto,
  mesmo texto e `SENT` (`src/test/java/hotel/messages/EmailSenderTest.java:64-81`). O modo de teste, com destino,
  `"[TESTE] "` e a primeira linha `"Destinatário original: ..."` (`:84-97`). Também há testes para a ausência da
  cópia no modo de teste (`:100-108`), o bloqueio no modo de teste com o endereço original (`:111-120`) e o bloqueio
  sem cópia (`:123-131`).
- Os testes da base que mudaram foram ajustados para a cópia e ficaram mais fortes, não mais fracos
  (`src/test/java/hotel/reservations/ReservationServiceTest.java:91-100`,
  `src/test/java/hotel/web/HttpApiTest.java:81-85`). Nenhum foi apagado.

Pela pergunta da mutação, ficam sem rede:
- **O "único @":** se alguém apagasse `atIndex != email.lastIndexOf('@')` (`EmailSender.java:50`), nenhum teste
  falharia, porque a lista de inválidos (`EmailSenderTest.java:31-36`) não tem `"ana@@gmail.com"`.
- **O "ponto no meio":** não há caso com ponto na borda do domínio, e o defeito da validação passa sem ser visto.
- **A variável `HOTEL_TEST_MODE`:** os testes usam só o construtor com `boolean` (`EmailSenderTest.java:29` e
  seguintes). Se alguém trocasse `"true"` por outra coisa em `EmailSender.java:13`, nada falharia.
- **Os inválidos sem cópia:** `allowsValidEmailAddresses` (`:46-61`) só conta mensagens e confere `SENT`, sem olhar
  os destinatários.

Além disso, `ReservationServiceTest` e `HttpApiTest` esperam a cópia para o arquivo, mas montam o `EmailSender` pelo
construtor que lê o ambiente. Com `HOTEL_TEST_MODE=true` exportado na máquina, o `mvn verify` falharia (essa é uma
conclusão da leitura do código, não uma execução).

## Banco

**Números:** 3 tabelas e 2 migrações: a `V1` ficou igual à da base e a `V2__add_message_status.sql` é nova. Nenhuma
tabela sem chave primária, dinheiro todo em `numeric(10,2)`, nenhuma chave estrangeira sem índice, nenhuma data em
texto. A conferência acusou `messages.status` em `text` sem CHECK (o mesmo vale para as colunas de situação que já
vinham da base). Não há SQL montado com dado de fora: os candidatos que o script listou são constantes de
`ReservationStore`, que vieram da base.
**Nota:** bom

O agente fez a mudança do jeito certo. Criou uma migração nova sem editar a da base, e ela dá
`NOT NULL DEFAULT 'SENT'` às mensagens que já existiam
(`src/main/resources/db/migration/V2__add_message_status.sql:2`), então um banco em uso sobe sem perder nada. O
INSERT e o SELECT novos usam parâmetros e try-with-resources
(`src/main/java/hotel/messages/SentMessages.java:24-40`, `:42-60`). Há dois pontos pequenos, que não pedem correção
antes de aceitar:
- Falta um `CHECK (status IN ('SENT','BLOCKED'))` na `V2`.
- A original e a cópia são dois INSERT separados, sem transação (`EmailSender.java:36` e `:39`). Se o segundo
  falhar, a original fica sem a cópia.

## Comportamento

**Números:** 65 de 65 conferências passaram. Foram 46/46 na parte comum, 3/3 depois de reiniciar, 9/9 no modo normal
do cenário, 5/5 no modo de teste (`HOTEL_TEST_MODE=true`) e 2/2 nas reservas simultâneas.
**Nota:** bom

Para o dono da pousada, tudo o que ele pediu funciona pela API:
- O e-mail digitado errado (`ana@gmail`, `ana.gmail.com`, `ana@@gmail.com`, `@gmail.com`) aparece como `BLOCKED`,
  com o endereço original e sem cópia.
- Toda mensagem enviada ganha, logo depois, a cópia para `arquivo@pousadamarealta.com.br`, com `[Cópia] ` no assunto
  e o mesmo texto.
- No modo de teste, nada sai para o hóspede: a mensagem vai para `testes@...`, com `[TESTE] ` no assunto e
  "Destinatário original: ..." na primeira linha, sem cópia.
- O que já funcionava continua funcionando, e os dados sobrevivem a um reinício.

Fora do roteiro, a leitura do código mostra que endereços como `ana@gmail.` e `ana@.com` seriam aceitos e enviados
(`src/main/java/hotel/messages/EmailSender.java:58`), contra o "ponto no meio" do texto. É um caso de borda, e
registrei esse defeito no design e nos testes.

## Dúvidas

- Não sei se o aceite de `ana@gmail.` e `ana@.com` deveria baixar a nota do comportamento. O texto diz "um ponto no
  meio", mas o roteiro não testa esse caso. Deixei a nota em `bom` e contei o defeito no design.
- Não rodei o `mvn verify` com `HOTEL_TEST_MODE=true` para provar que ele falha. A conclusão vem só da leitura de
  `EmailSender.java:13` e dos testes que usam esse construtor.
