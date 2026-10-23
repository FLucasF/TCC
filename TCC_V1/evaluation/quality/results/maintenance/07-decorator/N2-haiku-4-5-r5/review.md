# Revisão: manutenção, cenário 07

## Visão geral

A base tem sete pacotes (`hotel` para a montagem, `hotel.web` para as rotas Javalin, `hotel.reservations` para as regras e o acesso às reservas, `hotel.rooms`, `hotel.documents`, `hotel.reports` e `hotel.messages`). Uma requisição entra em `HttpApi`, chama `ReservationService`, que grava por `ReservationStore` (JDBC sobre PostgreSQL, com o esquema criado por Flyway) e manda o e-mail por `EmailSender.send`, que guarda a mensagem em `SentMessages`. O agente pôs as três regras de e-mail no pacote `hotel.messages`: uma interface `EmailRule` com três implementações (`ValidationRule`, `TestModeRule`, `ArchiveCopyRule`), aplicadas em lista por um `EmailProcessor`. Quem chama esse processador é `SentMessages.add`, a classe de acesso ao banco, e não o envio. Acrescentou também a migração `V2` com a coluna `status`. O defeito mais sério é este: a cópia para o arquivo é passada entre as classes como um texto separado por `|` (`ArchiveCopyRule.java:20`) e depois cortada nesse caractere (`EmailProcessor.java:41-42`). Como o comprovante tem ` | ` no próprio texto (`documents/ReservationReceipt.java:46-47`), toda cópia que chega ao contador sai cortada, sem as diárias e sem o total. O roteiro acusa essa falha. Além disso, a combinação das regras conforme o modo ficou dentro das próprias regras, e não na montagem.

## Arquitetura

**Números:** 7 pacotes e 17 dependências entre eles, os mesmos números da base. Um ciclo, `hotel.documents` ↔ `hotel.reservations`, que já existia na base. `hotel.messages` passou de 3 para 12 classes (75 → 233 linhas) e continua sem depender de nenhum outro pacote do projeto. Fora isso, as dependências não mudaram.
**Nota:** com problemas

A montagem continua num lugar só. `Hotel.java:31` lê `HOTEL_TEST_MODE` do ambiente que recebe, `Hotel.java:45-51` monta as regras e `Hotel.java:66` passa `System.getenv()`. As regras novas não conhecem Javalin nem JDBC (`ValidationRule`, `TestModeRule`, `ArchiveCopyRule`, `EmailProcessor`), e a rota `/messages` continua fina (`web/HttpApi.java:81`).

O problema está no lugar onde as regras são aplicadas. O agente ligou o processador dentro de `SentMessages`, a classe de acesso a dados (`messages/SentMessages.java:17-32`). `add` decide o destino, o assunto, o status e as cópias, e só depois grava. O envio (`EmailSender.java:12-14`) continua só repassando. Resultado: o repositório da lista de mensagens passou a ser também o lugar da regra de envio, e quem quiser guardar uma mensagem sem passar pelas regras não tem como. É uma mistura localizada, numa classe só, mas é nova e foi feita pelo agente. Não surgiu nenhum ciclo novo, e nenhuma classe cresceu demais (a maior continua sendo `HttpApi`, com 170 linhas, que veio da base).

## Design de baixo nível

**Números:** 3 regras pedidas, as três em peças próprias (`EmailRule`). A combinação por modo está dentro de 2 das 3 regras (`TestModeRule.java:12`, `ArchiveCopyRule.java:12`), e a montagem empilha sempre as mesmas 3, seja qual for o modo (`Hotel.java:46-49`). Há 1 interface sem nenhuma implementação e sem nenhum uso (`EmailRuleFactory`).
**Classe:** malfeito
**Exagero:** exagero
**Nota:** ruim

**O pattern.** O que aproxima do pedido: há uma interface por regra (`EmailRule.java:3-5`), cada regra é uma classe própria, e o `EmailProcessor` aplica a lista em ordem (`EmailProcessor.java:20-35`). Isso cabe na "lista de filtros" que a referência aceita como alternativa. O que afasta: a referência exige que a combinação por modo seja decidida na montagem, "não dentro das regras", e aqui é o contrário. `Hotel.createEmailProcessor` sempre põe as três (`Hotel.java:45-51`). `TestModeRule` pergunta se está em modo de teste e, se não estiver, devolve a mensagem sem mudar (`TestModeRule.java:12-14`). `ArchiveCopyRule` pergunta se está em modo de teste para não fazer a cópia (`ArchiveCopyRule.java:12-14`). É o caso que a referência dá como exemplo de malfeito ("a cópia que verifica se está em modo de teste"). Com as regras anunciadas (a da madrugada e a do limite por dia, "sempre, só no teste ou só fora dele"), cada nova regra vai precisar receber o booleano e testá-lo por dentro. A segunda pousada, que não quer a cópia, também não tem como tirá-la sem mexer no código de `createEmailProcessor`. Além disso, as regras não embrulham o envio: são aplicadas pela classe de banco (`SentMessages.java:23`).

**O exagero.** `EmailRuleFactory.java:3-5` é uma interface de fábrica que nada implementa e nada usa. É estrutura morta.

**O resto do design (a parte que o agente escreveu):**
- A cópia passa de uma regra para o processador como um texto `"EMAIL|destino|assunto|corpo|original"` (`ArchiveCopyRule.java:19-21`). O processador o desmonta com `split("\\|", 5)` (`EmailProcessor.java:40-43`). Qualquer `|` no assunto ou no corpo corrompe a cópia, e o comprovante sempre tem dois `|` (`documents/ReservationReceipt.java:46-47`). Reproduzi o caso: o corpo da cópia termina logo em "Entrada: 10/03/2027 ", e perde a saída, as diárias e o total. Serializar em texto uma mensagem que nunca sai do processo, quando já existe o record `ProcessedEmail` (`EmailProcessor.java:45`), é a causa direta de um dado errado no arquivo do contador. Por isso a nota é `ruim`.
- O processador tem um efeito de ordem frágil. Se uma regra que bloqueia vier depois da cópia, a mensagem sai `BLOCKED`, mas as cópias que já foram juntadas continuam na lista e são gravadas como `SENT` (`EmailProcessor.java:22-27, 37`). Hoje não acontece porque a validação vem primeiro (`Hotel.java:47`), mas é exatamente o que vai acontecer com a regra da madrugada ou a do limite, se forem postas no fim.
- O status é texto solto (`"SENT"`, `"BLOCKED"` em `EmailProcessor.java:18, 23, 42` e em `Message.java:3`), em vez de um `enum`. `EmailRuleResult.blockedResult()` usa strings vazias como valor de "nada" (`EmailRuleResult.java:11-13`).
- `EmailValidator.java:4-11` está certo e é legível. Os nomes, em geral, dizem o que as coisas fazem.

## Testes

**Números:** final: 32 testes rodaram, 0 falhas, 0 erros, 0 pulados; 32 métodos escritos. Linhas: 548 de 579 (94,6%); ramos: 121 de 138 (87,7%); `hotel.messages` 97 de 101 linhas e 22 de 24 ramos. Base: 26 testes, todos passando, 93,8% das linhas e 87,1% dos ramos. O agente acrescentou 6 testes: 3 em `EmailProcessorTest`, 1 em `IntegrationTest` e 2 em `ReservationServiceTest`.
**Nota:** com problemas

Os 26 testes da base continuam passando. Os que mudaram foram ajustados por causa da cópia: `ReservationServiceTest.java:85-104` e `HttpApiTest.java:81-85` passaram de 1 para 2 mensagens e ganharam asserções sobre o destino, o assunto e o status da cópia. Nenhum teste foi apagado nem enfraquecido. Os novos testes conferem, com valores e contra o banco:
- o modo de teste: o destino, o assunto "[TESTE] ", a primeira linha "Destinatário original: …" e a ausência da cópia (`EmailProcessorTest.java:30-50`);
- o bloqueio também no modo de teste, com o endereço original (`EmailProcessorTest.java:77-95`);
- o bloqueio de `joao@gmail`, `bob@invalid` e `charlie.mail` sem cópia (`ReservationServiceTest.java:171-183`, `IntegrationTest.java:28-76`).

O que ficou sem rede:
- **O texto da cópia.** Nenhum teste compara o corpo da cópia com o da original, e por isso o corte no `|`, que o roteiro do comportamento acusa, passou com todos os testes verdes. As asserções sobre a cópia só olham o destino, o assunto e o status (`EmailProcessorTest.java:69-72`, `ReservationServiceTest.java:197-200`).
- **A pergunta da mutação no validador.** Se alguém trocasse `atIndex <= 0` por `atIndex < 0`, ou apagasse a conferência do `@` único (`EmailValidator.java:6`), nenhum teste falharia: não há caso com `@gmail.com` nem com dois `@`. Trocar `dotIndex > atIndex + 1` por `>=` (`EmailValidator.java:10`) também passaria.
- **A leitura da variável `HOTEL_TEST_MODE`** (`Hotel.java:31`). Os testes montam o processador com `createEmailProcessor(true)` direto, e nenhum sobe o sistema com a variável ligada.

Um detalhe menor: `EmailProcessorTest` não testa o processador sozinho. Passa pelo serviço e pelo banco, igual a `IntegrationTest`.

## Banco

**Números:** 3 tabelas, todas com chave primária. Dinheiro em `numeric(10,2)`. Nenhuma chave estrangeira sem índice e nenhuma data em texto. O `EXCLUDE` contra duas reservas na mesma noite veio da base. 2 migrações: `V1` igual à da base e `V2__add_status_to_messages.sql` nova. `messages.status` é `text` sem CHECK. 0 transações explícitas.
**Nota:** bom

O agente mudou o banco do jeito certo: uma migração nova (`V2__add_status_to_messages.sql:1`) acrescenta `status text NOT NULL DEFAULT 'SENT'`, sem editar a `V1`. O `DEFAULT` dá um valor coerente às mensagens que já existiam num banco em uso. O SQL novo usa parâmetros (`SentMessages.java:37-44`) e devolve as conexões com try-with-resources. Duas observações, nenhuma das quais quebra dados:
- `status` não tem `CHECK (status IN ('SENT','BLOCKED'))`. As outras colunas de situação da base também não têm, então o agente seguiu a convenção da base.
- A original e a cópia são gravadas em duas conexões, sem transação (`SentMessages.java:25-29`). Se a segunda inserção falhar, a original fica sem cópia. A base já gravava a reserva e o e-mail fora de uma transação (`reservations/ReservationService.java:63-68`).

O corte do texto da cópia acontece antes do banco e pesa no design, não aqui.

## Comportamento

**Números:** 64 de 65 conferências passaram. Parte comum: 46 de 46. Dados depois de reiniciar: 3 de 3. Cenário no modo normal: 8 de 9. Cenário no modo de teste: 5 de 5. Reservas simultâneas: 2 de 2.
**Nota:** com problemas

Tudo o que já funcionava continua funcionando. O bloqueio de `ana@gmail`, `ana.gmail.com`, `ana@@gmail.com` e `@gmail.com` está certo (uma mensagem só, `BLOCKED`, com o endereço original, sem cópia). O modo de teste também está certo: o endereço de testes, "[TESTE] ", a primeira linha com o destinatário original, sem a cópia, e o inválido continua bloqueado.

A única falha é "a cópia tem o mesmo texto" (esperado `true`, veio `false`), e não é um ponto que o texto deixava em aberto: o pedido diz "com o mesmo texto". A causa está em `EmailProcessor.java:41-42`. O corpo da cópia é cortado no primeiro `|`, e o comprovante sempre tem um (`documents/ReservationReceipt.java:46`). Para o dono da pousada, isso quer dizer que todas as cópias que vão para arquivo@pousadamarealta.com.br chegam pela metade, com a data de entrada mas sem a saída, as diárias, o desconto e o total. O contador recebe uma cópia de cada mensagem, mas sem os valores que ele precisa.

## Dúvidas

- A classe do design ficou entre `malfeito` e `alternativa equivalente`. A estrutura de lista de regras é aceita pela referência, mas a combinação por modo está dentro de `TestModeRule` e de `ArchiveCopyRule`, que é o exemplo de malfeito da própria referência. Fiquei com `malfeito`.
- A nota `ruim` no design pesa principalmente a serialização da cópia em texto com `|`, que corrompe todas as cópias. Se esse defeito pesar só no comportamento (onde já aparece), o resto do design (o status em texto, a interface morta, a ordem frágil no processador) justificaria `com problemas`.
