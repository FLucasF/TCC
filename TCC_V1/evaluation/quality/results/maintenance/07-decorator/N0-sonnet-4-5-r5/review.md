# Revisão: manutenção, cenário 07

## Visão geral

A base é um serviço Javalin em 7 pacotes. `hotel.web.HttpApi` recebe a requisição e chama `hotel.reservations.ReservationService`, que guarda os dados pelo `ReservationStore` (JDBC, PostgreSQL com Flyway) e envia o e-mail pelo `hotel.messages.EmailSender`, que grava a mensagem em `SentMessages`. A montagem fica em `hotel.Hotel`. O agente mexeu pouco: pôs as três regras novas (bloquear endereço inválido, cópia para o arquivo, desvio no modo de teste) como `if`s dentro de `EmailSender.send` (`src/main/java/hotel/messages/EmailSender.java:21-43`), lê `HOTEL_TEST_MODE` dentro do próprio `EmailSender` (`EmailSender.java:13`), acrescentou o campo `status` ao `Message` e criou a migração `V2__add_message_status.sql`. Tudo o que o roteiro confere funciona (65 de 65). O que há de mais sério está no design: o pedido descreve uma variação que vai crescer (madrugada, limite por dia, regras que valem sempre, só no teste ou só fora dele, combinações diferentes por instalação), e nada disso cabe no código entregue sem mexer de novo no `send`. Além disso, a validação aceita `ana@.com` e `ana@gmail.` (testei pela API), porque só confere se há um ponto em algum lugar depois do @.

## Arquitetura

**Números:** 7 pacotes, 17 dependências entre eles e 1 ciclo (`hotel.documents` ↔ `hotel.reservations`), iguais aos da base (`measures/dependencies-base.md`). Só muda o tamanho de `hotel.messages`, de 75 para 122 linhas. `EmailSender` tem 60 linhas.
**Nota:** bom

A mudança não criou dependência nova nem ciclo novo. O ciclo `documents`/`reservations` já existia na base. As regras novas ficaram no pacote `hotel.messages`, que não conhece o Javalin. O SQL continua isolado em `SentMessages` (`src/main/java/hotel/messages/SentMessages.java:20-35`), e a borda HTTP não mudou (`src/main/java/hotel/web/HttpApi.java:81` só devolve `sentMessages.all()`). Tem uma ressalva, que conto no design: a configuração do modo fica fora da montagem. O construtor público do `EmailSender` lê a variável de ambiente (`EmailSender.java:12-14`), enquanto o resto da configuração é lido em `Hotel.main` (`src/main/java/hotel/Hotel.java:42-43`), e `Hotel.java:25` nem sabe que existe um modo de teste.

## Design de baixo nível

**Números:** 0 de 3 regras numa estrutura própria; as 3 estão em `if`s dentro de `EmailSender.send` (`EmailSender.java:22`, `:30`, `:38`). Nenhuma interface de envio, nenhum embrulho. A combinação por modo é decidida dentro do envio, não em `Hotel`.
**Classe:** omissão
**Exagero:** contenção
**Nota:** com problemas

A referência chama de omissão o caso em que os três `if` (o endereço, o modo de teste, a cópia) ficam dentro do envio, e é exatamente o que está aqui:
- `if (!isValidEmail(to))` em `EmailSender.java:22`;
- `if (testMode)`, que reescreve destinatário, assunto e texto, em `EmailSender.java:30-34`;
- `if (!testMode)`, que faz a cópia, em `EmailSender.java:38-40`.

A regra "a cópia só fora do teste" está escrita na própria cópia, e a gravação de verdade (`sentMessages.add`) fica misturada com as regras. A próxima regra que o dono já anunciou (madrugada, limite por dia, cada uma valendo sempre, só no teste ou só fora dele) e a segunda pousada sem cópia obrigam a abrir esse método de novo e a juntar mais `if (testMode)`. Nenhuma instalação consegue escolher a combinação na montagem (`Hotel.java:25`).

Não há exagero: o agente não criou interface, fábrica nem camada a mais.

Sobre o resto do código, a nota é `com problemas` por defeitos localizados:
- o `status` virou texto solto, `"SENT"`/`"BLOCKED"` repetidos em `EmailSender.java:23`, `:36` e `:39` e `String status` no `Message` (`src/main/java/hotel/messages/Message.java:3`), em vez de um `enum`. A base já guardava `channel` como texto, mas um campo novo com dois valores fechados pedia um tipo;
- o construtor público lê `System.getenv` (`EmailSender.java:13`), o que esconde a configuração e torna o comportamento de toda classe que usa esse construtor dependente do ambiente (veja Testes);
- `isValidEmail` (`EmailSender.java:45-59`) é legível, mas `domain.contains(".")` (`:58`) não implementa "um ponto no meio";
- as duas gravações (original e cópia) não andam juntas (`EmailSender.java:36` e `:39`, sem transação), mas isso pesa pouco numa lista de mensagens.

## Testes

**Números:** 38 testes rodaram, 0 falhas, 0 erros, 0 pulados; 38 métodos escritos (a base tinha 26, todos mantidos). Cobertura total: linhas 94,1% (498/529), ramos 87,9% (116/132); na base, 93,8% e 87,1%. Em `hotel.messages`: linhas 56/60, ramos 17/18.
**Nota:** com problemas

O agente testou a parte nova com valores, em três níveis:
- unidade contra o banco: `src/test/java/hotel/messages/EmailSenderTest.java:22-37` confere a cópia (destinatário, `[Cópia] `, mesmo texto, `SENT`), e `:40-87` cobre os bloqueios sem @, com dois @, sem ponto, começando com @ e vazio;
- modo de teste: `src/test/java/hotel/messages/EmailSenderTestModeTest.java:23-54` confere o desvio, `[TESTE] `, a primeira linha, o bloqueio com o endereço original e a ausência de cópia;
- pela API: `src/test/java/hotel/messages/MessageApiTest.java:63-88`.

Os testes da base que mudaram foram ajustados para a cópia nova (`ReservationServiceTest.java:91-100`, `HttpApiTest.java:81-83`), sem enfraquecer o que já conferiam.

Fazendo a pergunta da mutação: apagar o `if (!testMode)` ou o `atIndex == 0`, ou trocar o `contains(".")` por `true`, derruba algum teste. Mas faltam casos:
- nenhum teste usa um ponto na ponta do domínio (`ana@.com`, `ana@gmail.`), e é justamente aí que está o defeito de comportamento;
- nada confere que `HOTEL_TEST_MODE=true` liga o modo, porque o construtor que lê a variável (`EmailSender.java:12-14`) só é usado com ela ausente;
- os testes dependem do ambiente: `EmailSenderTest.java:14`, `ReservationServiceTest.java:26`, `ReportsTest.java:28`, `HttpApiTest.java:35` e `MessageApiTest.java:40` montam o envio pelo construtor que lê a variável. Por isso, rodar `mvn verify` numa máquina com `HOTEL_TEST_MODE=true` (justamente o ambiente do pessoal do site) faria falhar os testes que esperam a cópia e o destinatário real. Isso é dedução pela leitura; não rodei.

## Banco

**Números:** 3 tabelas, todas com chave primária; dinheiro em `numeric(10,2)`; nenhuma chave estrangeira sem índice; nenhuma data em texto. A migração da base `V1__create_tables.sql` está igual, e `V2__add_message_status.sql` é nova. A conferência acusou `messages.status` em `text` sem CHECK, assim como as outras colunas de situação que já vinham da base.
**Nota:** bom

O agente fez a mudança do jeito certo para quem já tem dados: uma migração nova que acrescenta a coluna com `NOT NULL DEFAULT 'SENT'` (`src/main/resources/db/migration/V2__add_message_status.sql:2`), o que dá às mensagens antigas um valor coerente (todas foram enviadas). Não editou a V1. O SQL novo usa parâmetros (`src/main/java/hotel/messages/SentMessages.java:23-29`) e devolve a conexão com try-with-resources. A falta de CHECK em `status` segue a convenção da base (nenhuma coluna de situação tem CHECK). O EXCLUDE contra reservas sobrepostas veio da base e não foi mexido.

## Comportamento

**Números:** 65 de 65 conferências passaram: parte comum 46/46, depois de reiniciar 3/3, cenário no modo normal 9/9, cenário no modo de teste 5/5, reservas simultâneas 2/2.
**Nota:** com problemas

Tudo o que o roteiro confere funciona: o bloqueio de `ana@gmail`, `ana.gmail.com`, `ana@@gmail.com` e `@gmail.com`, a cópia logo depois da original, o modo de teste com `[TESTE] ` e a primeira linha, e o campo `status`. Fora do roteiro, chamei a API com o serviço de pé e confirmei reservas de três hóspedes:
- `ana@.com` e `ana@gmail.` saíram como `SENT`, cada uma com a sua cópia para o arquivo, embora o dono tenha pedido "um nome com pelo menos um ponto no meio". Para a pousada, um e-mail digitado errado desse jeito continua indo para lugar nenhum: a recepção não fica sabendo para ligar, e o contador recebe cópia de uma mensagem que ninguém leu;
- `ana@gmail..com` também saiu como `SENT`, mas esse caso o texto deixa em aberto.

## Dúvidas

- A nota de Testes considera que `HOTEL_TEST_MODE=true` no ambiente quebra os testes do `mvn verify`; deduzi isso pela leitura de `EmailSender.java:13` e dos construtores usados nos testes, sem rodar.
- `ana@gmail..com` aceito: o texto pede "pelo menos um ponto no meio" e não diz nada sobre dois pontos seguidos, então não contei esse caso como falha.
