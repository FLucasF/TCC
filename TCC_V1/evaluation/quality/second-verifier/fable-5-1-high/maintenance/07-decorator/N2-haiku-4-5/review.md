# Revisão: manutenção, cenário 07

## Visão geral

O projeto segue a base: `hotel.web` (o Javalin, `HttpApi.java`) lê a requisição e chama `hotel.reservations`
(`ReservationService.java`), que usa `hotel.rooms`, `hotel.documents` e `hotel.messages`; os dados ficam num
PostgreSQL com HikariCP e migrações do Flyway (`Database.java:28`), e a montagem é `Hotel.java:20-29`. A mudança do
agente ficou toda dentro de `hotel.messages` (3 → 9 classes) mais uma migração `V2` e dez testes. O envio
(`EmailSender.send`) passou a rodar uma lista de regras (`EmailRule`: validação do endereço e modo de teste), mas a
cópia para o arquivo é um `if` dentro do próprio envio, que também consulta a variável de ambiente, e a lista é montada
dentro do `EmailSender`, não em `Hotel`. Pela API tudo o que o pedido descreve funciona (65 de 65 conferências); o
que há de mais sério é o design da variação, que não ficou pronto para as próximas regras, e a ausência de qualquer
teste do modo de teste. O README não foi atualizado (não fala de `status`, cópia nem `HOTEL_TEST_MODE`).

## Arquitetura

**Números:** 7 pacotes, 17 dependências entre eles, 1 ciclo (`hotel.documents` ↔ `hotel.reservations`), igual à
base (a única diferença em `measures/dependencies-base.md` é `hotel.messages` passar de 3 classes/75 linhas para
9/220). `hotel.messages` continua sem depender de ninguém e usado por 3 pacotes.
**Nota:** com problemas

As camadas ficaram como estavam: a rota `/messages` só repassa a lista (`src/main/java/hotel/web/HttpApi.java:81`), o
SQL das mensagens fica em `SentMessages` (`src/main/java/hotel/messages/SentMessages.java:20-36`) e as regras novas não
conhecem Javalin nem JDBC. O ciclo `documents`/`reservations` já existia na base e o agente não mexeu nele. O que
pesa é a montagem: o `EmailSender` cria a sua própria lista de regras no construtor
(`src/main/java/hotel/messages/EmailSender.java:14-16`), e tanto ele (`EmailSender.java:30`) quanto `TestModeRule`
(`src/main/java/hotel/messages/TestModeRule.java:7`) leem `System.getenv` por dentro, um global que a base só lia no
`main` (`src/main/java/hotel/Hotel.java:42-43`, que passa o ambiente para `Database` como `Map`). A classe de montagem
`Hotel.java:25` ficou intocada, então a combinação de regras por instalação (que o pedido anuncia) não tem onde ser
decidida sem editar a classe de envio.

## Design de baixo nível

**Números:** três comportamentos pedidos; 2 na estrutura (`ValidateEmailRule`, `TestModeRule`) e 1 fora dela (a
cópia para o arquivo, um `if` em `EmailSender.java:30-34`); a variável `HOTEL_TEST_MODE` lida em 2 lugares; a lista de
regras montada em 1 lugar, dentro do próprio `EmailSender`; interface `sealed` que admite exatamente 2 regras.
**Classe:** malfeito
**Exagero:** contenção
**Nota:** com problemas

O pattern pedido é o Decorator (ou, como a referência aceita, uma corrente/lista de regras, cada uma peça própria e a
combinação por modo decidida na montagem). O que aproxima: existe uma interface de regra
(`src/main/java/hotel/messages/EmailRule.java:3-4`) com uma classe por regra (`ValidateEmailRule.java:6-11`,
`TestModeRule.java:6-16`), e o envio percorre a lista (`EmailSender.java:22-28`). O que afasta, e sustenta o
`malfeito`: (1) a cópia para o arquivo não é uma regra, é um `if` escrito à mão no envio (`EmailSender.java:30-34`), e
é esse `if` que decide a combinação por modo (`!testMode`), exatamente o caso que a referência cita; (2) a
`TestModeRule` decide sozinha se vale, lendo o ambiente por dentro (`TestModeRule.java:7-8`), em vez de a montagem
empilhá-la só no modo de teste; (3) a lista é fixa dentro do `EmailSender` (`EmailSender.java:14-16`), e `Hotel.java:25`
não sabe de regra nenhuma, então "a segunda pousada não quer a cópia" exige editar o envio; (4) a interface é
`sealed ... permits ValidateEmailRule, TestModeRule` (`EmailRule.java:3`): a regra da madrugada ou do limite por dia
não compila sem editar a interface, o oposto do que o pedido anuncia. Não há exagero: nada fora da parte das regras
ganhou interface, fábrica ou camada a mais.

No resto do código novo: `EmailToSend` é mutável, com setters (`src/main/java/hotel/messages/EmailToSend.java:18-35`),
e a `TestModeRule` altera o objeto no lugar e ainda o devolve dentro de um `ProcessingResult`
(`TestModeRule.java:12-15`, `EmailSender.java:27`), dois caminhos para o mesmo estado; `ProcessingResult.blocked()`
carrega `email == null` (`src/main/java/hotel/messages/ProcessingResult.java:21`), um `null` que só não quebra porque
o `EmailSender` testa `isBlocked()` antes; o `status` é texto solto em vez de um `enum` de dois valores
(`src/main/java/hotel/messages/Message.java:3`, `SentMessages.java:20`, `EmailSender.java:25,29,33`); a leitura de
`HOTEL_TEST_MODE` está duplicada (`EmailSender.java:30`, `TestModeRule.java:7`); os endereços fixos ficam como
literais no meio do código (`EmailSender.java:32`, `TestModeRule.java:12`). O código da base que o agente tocou
(`SentMessages.java`, `Message.java`) manteve o estilo: record, `PreparedStatement`, try-with-resources. Defeitos
localizados no pacote novo; o resto do projeto (`BigDecimal` no preço, `LocalDate`, enums, records) veio da base.

## Testes

**Números:** 36 testes rodaram, 0 falharam, 0 pulados (a base tinha 26: o agente acrescentou 10 e não apagou nenhum);
36 métodos de teste; cobertura 92,3% de linhas (524/568) e 85,0% de ramos (119/140), contra 93,8%/87,1% na base. Em
`hotel.messages`: 82 de 99 linhas e 20 de 26 ramos.
**Nota:** com problemas

O que está conferido com valores: endereço inválido bloqueado, uma mensagem só, com o endereço original
(`src/test/java/hotel/messages/EmailSenderTest.java:43-50`, `53-59`, `62-68`;
`src/test/java/hotel/reservations/ReservationServiceTest.java:166-178`; pela API em
`src/test/java/hotel/web/HttpApiTest.java:95-104`); a cópia para o arquivo logo depois da original, com
"[Cópia] " e `SENT` (`EmailSenderTest.java:32-40`, `ReservationServiceTest.java:181-192`, `HttpApiTest.java:80-85`).
Os testes da base foram mantidos e adaptados (a contagem de 1 para 2 mensagens, `ReservationServiceTest.java:91,107`,
`HttpApiTest.java:81`), sem enfraquecer: as asserções sobre a mensagem original continuam.

O que ficou sem rede: o modo de teste inteiro. Não há teste que suba com `HOTEL_TEST_MODE=true`, e o único teste da
`TestModeRule` (`src/test/java/hotel/messages/TestModeEmailSenderTest.java:12-20`) chama-se
`testModeRuleRedirectsEmail` mas afirma o contrário, que o endereço **não** mudou (linha 19), porque a variável não
está ligada. Pela pergunta da mutação: trocar "[TESTE] " por "[TEST] ", apagar a linha "Destinatário original: "
(`TestModeRule.java:13-14`) ou remover o `if (!testMode)` que suprime a cópia (`EmailSender.java:31`), e nenhum teste
falha; é o reflexo de a regra ler `System.getenv` por dentro, o que impede injetar o modo no teste. Também não há teste
de unidade para "@@" (dois arrobas) nem para "@gmail.com" (nada antes do @): trocar `atCount != 1` por `atCount < 1`
em `src/main/java/hotel/messages/EmailValidator.java:17` passa pelos testes (o roteiro de comportamento é que pegou
esses casos). Os 26 ramos cobertos de `hotel.messages` deixam de fora justamente `TestModeRule.java:11-15`. Detalhe:
`assertTrue` importado e não usado em `EmailSenderTest.java:10`.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), 2 migrações do Flyway (a `V1` da base igual, a `V2`
nova). As conferências não acusaram nada: toda tabela com chave primária, dinheiro em `numeric(10,2)`, nenhuma chave
estrangeira sem índice, nenhuma data em texto; `messages.status` é `text` sem `CHECK`, como todas as colunas de
situação da base.
**Nota:** bom

A coluna nova entrou por uma migração própria
(`src/main/resources/db/migration/V2__add_status_to_messages.sql:1`: `ALTER TABLE messages ADD COLUMN status text NOT
NULL DEFAULT 'SENT'`), sem tocar na `V1`, e o `DEFAULT 'SENT'` dá um valor certo às mensagens que um banco já
existente guardava (todas foram enviadas). O código continua com SQL parametrizado e try-with-resources
(`src/main/java/hotel/messages/SentMessages.java:21-32`, `39-42`); o serviço subiu num banco vazio e criou o esquema. A
cópia e a original são duas inserções sem transação (`EmailSender.java:29-33`): se a segunda falhar fica a original
sem cópia, o que é menor para uma lista de mensagens. A ordem "logo depois" vem da identidade da tabela
(`V1__create_tables.sql:45`) e do `ORDER BY id` (`SentMessages.java:41`). Um `CHECK (status IN ('SENT','BLOCKED'))`
seria o esperado de um revisor exigente, mas a base também não tem `CHECK` em `channel`, `rate_plan` nem `status` das
reservas; o agente seguiu o estilo que havia. A proteção da reserva dupla é a restrição `EXCLUDE` da base.

## Comportamento

**Números:** 65 de 65 conferências passaram: a parte comum 46/46, os dados depois de reiniciar 3/3, o cenário no modo
normal 9/9, o cenário no modo de teste 5/5, as duas reservas ao mesmo tempo 2/2.
**Nota:** bom

Nada falhou. Para o dono da pousada: o endereço válido recebe a mensagem com `status` `SENT` e a cópia para
`arquivo@pousadamarealta.com.br` vem logo depois, com "[Cópia] " e o mesmo texto; os quatro endereços errados do
roteiro ("ana@gmail", "ana.gmail.com", "ana@@gmail.com", "@gmail.com") ficam como uma mensagem só, `BLOCKED`, com o
endereço original e sem cópia; com `HOTEL_TEST_MODE=true` a mensagem vai para `testes@pousadamarealta.com.br`, com
"[TESTE] " no assunto e "Destinatário original: ana@example.com" na primeira linha, sem cópia, e o endereço inválido
continua bloqueado com o endereço original. Tudo o que já funcionava (preço, temporada, arredondamento, recusas,
cancelamentos, relatórios, reserva dupla) continua funcionando.

## Dúvidas

Entre `malfeito` e `alternativa equivalente`: a lista de regras é a forma que a referência aceita, mas a cópia ficou
fora dela e a combinação por modo está dentro do envio e dentro da regra, não na montagem; a evidência sustenta melhor
`malfeito`. Na arquitetura, pesou-se a montagem fora do lugar e o `System.getenv` nas regras como mistura localizada
(`com problemas`); alguém poderia considerá-la pequena demais para tirar o `bom`, já que as camadas HTTP/regras/dados
ficaram intactas.
