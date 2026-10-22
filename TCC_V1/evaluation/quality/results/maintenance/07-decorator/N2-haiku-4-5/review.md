# Revisão: manutenção, cenário 07

## Visão geral

A base é um serviço Javalin em pacotes por assunto: `hotel.web` (as rotas, `HttpApi`), `hotel.reservations` (a
regra, `ReservationService`, e o acesso ao banco, `ReservationStore`), `hotel.rooms`, `hotel.reports`,
`hotel.documents` e `hotel.messages`. A montagem fica em `hotel.Hotel` e os dados no PostgreSQL, com migrações do
Flyway. Uma confirmação entra por `HttpApi`, passa por `ReservationService.confirm`, que chama `EmailSender.send`
(`reservations/ReservationService.java:65`), e o e-mail fica na tabela `messages` por `SentMessages`. O agente mexeu
só em `hotel.messages`. Criou uma lista de regras (`EmailRule`, com `ValidateEmailRule` e `TestModeRule`) que o
`EmailSender` percorre, acrescentou o campo `status` em `Message` e uma migração nova, `V2`. O comportamento
está todo certo (65 de 65 conferências). O problema mais sério é o design: a estrutura de regras não resolve o que
o dono pediu para o futuro. A cópia para o arquivo ficou fora dela, num `if` dentro do envio, e cada regra lê o
modo de teste por conta própria. Além disso, a lista de regras é montada dentro do `EmailSender`, então cada
instalação não tem como escolher a sua combinação sem mudar o código.

## Arquitetura

**Números:** 7 pacotes, 17 dependências entre eles e 1 ciclo (`hotel.documents` ↔ `hotel.reservations`). Os três
números são iguais aos da base (`measures/dependencies-base.md`). Só mudou o tamanho de `hotel.messages`, que foi
de 3 classes e 75 linhas para 9 classes e 220 linhas.
**Nota:** com problemas

A divisão em camadas continua a da base. As rotas não mudaram. `hotel.messages` não depende de nenhum pacote do
projeto, e o JDBC continua só em `SentMessages`. O ciclo `documents` ↔ `reservations` já existia na base e não
é do agente.

O defeito do agente está na montagem e na configuração:

- `EmailSender` cria as próprias regras por dentro (`messages/EmailSender.java:14-16`). A montagem
  (`Hotel.java:25`) continua fazendo só `new EmailSender(sentMessages)`. Por isso, "cada instalação pode querer
  outra combinação" não tem onde ser decidido sem editar a classe de envio.
- A variável de ambiente `HOTEL_TEST_MODE` é lida dentro do domínio, em dois lugares
  (`messages/TestModeRule.java:7` e `messages/EmailSender.java:30`), e não uma vez só, no `main`
  (`Hotel.java:41-44`), que é onde a base já lê `PORT` e as variáveis do banco.
- `SentMessages.add` era visível só dentro do pacote na base (`base/.../SentMessages.java:20`) e virou `public`
  (`messages/SentMessages.java:20`). Assim, qualquer pacote pode gravar uma mensagem "enviada" sem passar pelas
  regras.

São defeitos localizados, mas são os pontos que o próximo pedido (madrugada, limite por dia, a segunda pousada)
vai tocar.

## Design de baixo nível

**Números:** dos 3 comportamentos pedidos, 2 estão na estrutura (a validação e o modo de teste) e 1 está fora (a
cópia para o arquivo, num `if` em `EmailSender.java:30-34`). O modo é consultado em 2 lugares
(`TestModeRule.java:7-8` e `EmailSender.java:30-31`). A combinação por modo é decidida em 0 lugares da montagem.
**Classe:** malfeito
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** O agente fez uma lista de filtros (`EmailRule`, `messages/EmailRule.java:3`) que o envio percorre
(`EmailSender.java:22-28`). A referência aceita esse formato como alternativa, mas só se cada regra for uma peça
própria e a combinação por modo for decidida na montagem. Aqui não é assim:

- A cópia para o arquivo não é uma regra. Ela é um `if (!testMode)` dentro do próprio envio
  (`EmailSender.java:30-34`), ou seja, o envio de dentro ainda pergunta pelo modo de teste.
- A regra do modo de teste decide sozinha se vale, lendo a variável (`TestModeRule.java:7-10`). A combinação por
  modo ficou escrita dentro do embrulho, e não na montagem.
- A lista é fixa no construtor (`EmailSender.java:14-16`). `EmailRule` é `sealed ... permits ValidateEmailRule,
  TestModeRule` (`EmailRule.java:3`), o que fecha a estrutura justamente para as regras anunciadas (madrugada,
  limite por dia): cada uma vai exigir editar a interface.
- O bloqueio também está em parte fora da regra. Quem grava a mensagem `BLOCKED` com o endereço original é o laço
  do envio (`EmailSender.java:24-25`), e não a regra.

Pelos critérios da referência, isso é malfeito: a estrutura existe, mas convive com o `if` do modo e com a cópia
feita à mão.

**O exagero.** Não há estrutura sem necessidade fora do pattern. `EmailValidator` (`EmailValidator.java:3`) é uma
classe de função estática pequena e usada uma vez, mas é razoável. `ProcessingResult` só existe para servir à lista
de regras.

**O resto do código.**

- `EmailToSend` é mutável, com setters (`EmailToSend.java:18`, `:26`, `:34`), e `TestModeRule` altera o objeto que
  recebeu (`TestModeRule.java:12-14`), em vez de devolver um novo. Com regras em sequência, isso é estado
  compartilhado sem controle. Um `record` resolveria.
- `ProcessingResult` é uma classe à mão que um `record` substituiria. Quando bloqueia, ela carrega `email = null`
  (`ProcessingResult.java:21`).
- A situação da mensagem é texto solto: `"SENT"` e `"BLOCKED"` em `EmailSender.java:25`, `:29` e `:33`, e
  `String status` em `Message.java:3`. Um `enum` seria o tipo certo.
- Os endereços `arquivo@...` e `testes@...` e o nome da variável de ambiente estão espalhados como literais
  (`EmailSender.java:30-32` e `TestModeRule.java:7`, `:12`), e a leitura do modo está duplicada.
- A validação aceita domínios como `gmail.` ou `.com` (`EmailValidator.java:20-21`, `domain.contains(".")`), embora
  o texto peça um ponto "no meio" do nome.

O resto da base não foi tocado.

## Testes

**Números:** 36 testes rodaram, com 0 falhas, 0 erros e 0 pulados, e há 36 métodos escritos. A base tinha 26
(todos continuam passando). A cobertura de linhas foi de 93,8% para 92,3% (524 de 568) e a de ramos de 87,1% para
85,0% (119 de 140). Em `hotel.messages`, a cobertura é de 82 de 99 linhas e 20 de 26 ramos (`measures/tests.md`).
**Nota:** com problemas

**O que está testado.**

- O bloqueio de endereço inválido é testado com valores: a mensagem fica `BLOCKED`, com o endereço original e sem
  cópia. Os casos sem domínio e sem `@` estão em `src/test/java/hotel/messages/EmailSenderTest.java:43`, `:53` e
  `:62`, e o mesmo bloqueio é conferido pelo serviço em `reservations/ReservationServiceTest.java:166` e pela API em
  `web/HttpApiTest.java:95`.
- A cópia para o arquivo é testada no destinatário, no `status` e no prefixo "[Cópia] "
  (`ReservationServiceTest.java:181`, `EmailSenderTest.java:31`).
- Os testes da base que mudaram foram ajustados para a cópia nova (de 1 para 2 mensagens em
  `ReservationServiceTest.java:91` e `:107`, e em `HttpApiTest.java:81`), sem perder as asserções que já
  existiam.

**O que ficou sem teste.**

- O modo de teste, a regra mais grave do pedido, não tem nenhum teste no modo de teste.
  `TestModeEmailSenderTest.testModeRuleRedirectsEmail` (`messages/TestModeEmailSenderTest.java:12`) roda sem a
  variável e confere que o endereço *não* muda (`:19`): o nome diz o contrário do que o teste confere. Como a regra
  lê `System.getenv` por dentro, nenhum teste consegue ligar o modo, e a falta de teste vem do próprio design.
- Se alguém apagasse o "[TESTE] ", a linha "Destinatário original" ou o `if (!testMode)` da cópia, nenhum teste
  falharia.
- O texto da cópia (o mesmo corpo) também não é conferido.
- Faltam os casos de borda da validação: `ana@@gmail.com`, `@gmail.com` e o ponto no começo ou no fim do domínio.

## Banco

**Números:** 3 tabelas, todas com chave primária. Todo o dinheiro está em `numeric(10,2)`, não há chave
estrangeira sem índice nem data guardada como texto. A migração da base, `V1`, está igual, e há uma migração nova,
`V2`. A única coisa que as conferências acusam é `messages.status` em `text` sem `CHECK`, como as outras colunas de
situação da base (`measures/database.md`).
**Nota:** bom

- O agente evoluiu o esquema do jeito certo. Ele não editou a `V1` e criou
  `src/main/resources/db/migration/V2__add_status_to_messages.sql:1`, com `NOT NULL DEFAULT 'SENT'`, o que dá uma
  situação coerente às mensagens que já existem num banco em uso.
- O SQL novo usa parâmetros (`messages/SentMessages.java:24-28`), e as conexões continuam em try-with-resources.
- A falta de `CHECK` em `status` segue o padrão que a base já tinha (`rate_plan`, `status` da reserva) e é um
  detalhe menor.
- A original e a cópia são gravadas em dois `INSERT` separados (`EmailSender.java:29` e `:32`), sem transação.
  Uma falha entre os dois deixaria a original sem cópia, mas o risco é pequeno.
- A proteção contra reserva duplicada (o `EXCLUDE` em `V1__create_tables.sql:38-40`) vem da base.

## Comportamento

**Números:** 65 de 65 conferências passaram (`measures/behavior.md`):

- a parte comum: 46 de 46;
- depois de reiniciar: 3 de 3;
- o cenário no modo normal: 9 de 9;
- o cenário no modo de teste: 5 de 5;
- as reservas simultâneas: 2 de 2.

**Nota:** bom

Para o dono da pousada, tudo o que o texto descreve funciona pela API:

- O endereço inválido fica `BLOCKED`, com o endereço original e sem cópia. Isso vale para `ana@gmail`,
  `ana.gmail.com`, `ana@@gmail.com` e `@gmail.com`, e também no modo de teste.
- A cópia para o arquivo vem logo depois da original, com "[Cópia] " no assunto e o mesmo texto.
- Com `HOTEL_TEST_MODE=true`, tudo vai para `testes@pousadamarealta.com.br`, com "[TESTE] " no assunto e a linha
  "Destinatário original", e sem a cópia.
- O que já existia continua igual, e os dados sobrevivem a uma reinicialização.

Fora do roteiro, pela leitura de `EmailValidator.java:20-21`: um endereço como `ana@gmail.` ou `ana@.com` seria
aceito e enviado, embora o texto peça um ponto "no meio" do nome. Isso não apareceu nas chamadas do roteiro.

## Dúvidas

- A classe do pattern ficou entre "malfeito" e "omissão". Há uma estrutura de regras de verdade, com duas peças
  próprias, o que tira o caso da omissão. Mas a terceira regra (a cópia) e a combinação por modo ficaram fora
  dela, o que é o malfeito descrito na referência.
- Não verifiquei pela API se `ana@gmail.` ou `ana@.com` são aceitos. A conclusão vem só da leitura do código.
