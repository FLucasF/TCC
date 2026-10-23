# Revisão: manutenção, cenário 07

## Visão geral

A base é um serviço Javalin em sete pacotes: `hotel.web` (rotas, `HttpApi`), `hotel.reservations` (regras e acesso
às reservas), `hotel.rooms`, `hotel.reports`, `hotel.documents`, `hotel.messages` (o envio e a lista de mensagens) e
`hotel` (a montagem em `Hotel` e o `Database` com Flyway e pool). Uma confirmação entra por `HttpApi`, passa por
`ReservationService`, que chama `EmailSender.send`, e a mensagem é gravada na tabela `messages` por `SentMessages`.
O agente resolveu o pedido dentro de `hotel.messages`: uma interface `MessageRule` com três regras (validação do
endereço, desvio no modo de teste, cópia para o arquivo), um `MessageContext` mutável que as regras alteram, uma lista
montada por `MessageRules.build(testMode)` e aplicada em laço por `EmailSender` (`final/src/main/java/hotel/messages/EmailSender.java:19-30`),
o campo `status` com a migração nova `V2__add_message_status.sql` e a variável `HOTEL_TEST_MODE` lida em
`Hotel.java:45`. Tudo funciona pela API (65 de 65 conferências) e os 34 testes passam. O que há de mais sério é de
design: a combinação por modo ficou dentro das regras (cada uma pergunta `testMode`), a montagem sempre liga as três
regras (a segunda pousada, que não quer a cópia, não tem como desligá-la sem mexer no código), e a validação aceita
endereços como `ana@.` e `ana@gmail.`, contra a definição do dono ("um ponto no meio").

## Arquitetura

**Números:** 7 pacotes e 17 dependências, iguais à base; 1 ciclo (`hotel.documents` ↔ `hotel.reservations`), que já existia na base (`measures/dependencies-base.md`). `hotel.messages` passou de 3 para 11 classes (75 → 248 linhas) e continua sem depender de nenhum outro pacote do projeto.
**Nota:** bom

A mudança ficou contida no pacote certo: as regras novas moram em `hotel.messages` e não conhecem Javalin nem JDBC
(`ArchiveCopyRule.java`, `EmailValidationRule.java`, `TestModeRedirectRule.java`); o único SQL tocado está em
`SentMessages.java:23` e `SentMessages.java:41`, a classe de acesso a dados que já existia. A borda HTTP não mudou: a
rota continua só serializando a lista (`HttpApi.java:81`). A leitura da variável de ambiente fica no `main`
(`Hotel.java:45`) e a montagem continua em um lugar só (`Hotel.java:21-30`), recebendo `testMode` como parâmetro,
o que deixa os testes construírem o hotel nos dois modos. Nenhuma dependência nova entre pacotes, nenhum ciclo novo; o
ciclo `documents`↔`reservations` vem da base. O único reparo é de visibilidade, e pesa no design: `MessageRules.build`
(`MessageRules.java:9`) e o construtor público de `EmailSender` (`EmailSender.java:11`) expõem
`List<MessageRule>`, sendo `MessageRule` um tipo do pacote (`MessageRule.java:4`).

## Design de baixo nível

**Números:** as 3 regras pedidas estão na estrutura (3 classes `MessageRule`, aplicadas em laço em `EmailSender.java:19-21`); nenhuma regra ficou fora. Mas 2 das 3 regras decidem o modo por dentro (`ArchiveCopyRule.java:15`, `TestModeRedirectRule.java:15`), e a montagem sempre liga as 3 (`MessageRules.java:11-13`).
**Classe:** malfeito
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** O que aproxima: é uma lista de filtros aplicada pelo envio, com cada regra numa peça própria
(`EmailValidationRule`, `TestModeRedirectRule`, `ArchiveCopyRule`), a mesma interface (`MessageRule.java:6`), e o envio
de dentro (`EmailSender.send`) não pergunta pelo modo — a forma que a referência aceita como alternativa equivalente.
O que afasta: a condição para isso é que "a combinação por modo seja decidida na montagem, não dentro das regras", e
aqui é o contrário. `MessageRules.build` (`MessageRules.java:9-15`) põe sempre as três regras e passa `testMode` para
duas delas; a cópia pergunta `if (context.isBlocked() || testMode) return;` (`ArchiveCopyRule.java:15`) e o desvio
pergunta `if (!testMode || ...) return;` (`TestModeRedirectRule.java:15`). É exatamente o caso que a referência
classifica como malfeito ("a cópia que verifica se está em modo de teste"). A consequência prática é a que o dono
anunciou: para a segunda pousada sem cópia, ou para uma regra de madrugada "só fora do teste", é preciso mudar o
código de cada regra ou acrescentar mais `boolean` no `build`, em vez de só escolher que peças empilhar. Além disso, as
regras dependem da ordem e do estado compartilhado (`isBlocked()` lido por duas regras, `MessageContext.java:35`), e o
contexto mutável (`MessageContext.java:9-10`, `39-45`) faz as vezes do encadeamento.

**O exagero.** Contenção: `MessageContext`, `OutgoingMessage` e `MessageRules` fazem parte da solução do pattern;
fora dela não há interface, fábrica ou camada sem uso.

**O resto do código.** Defeitos localizados:
- a validação não segue a definição pedida: `EmailValidationRule.java:25-26` só confere se há um ponto em qualquer
  lugar depois do @, então `ana@.`, `ana@.com`, `ana@gmail.` e `ana @gmail.com` passam como válidos (conferido
  reproduzindo o método com esses valores); o dono pediu "um nome com pelo menos um ponto no meio";
- a API pública expõe um tipo do pacote: `public static List<MessageRule> build` (`MessageRules.java:9`) e
  `public EmailSender(SentMessages, List<MessageRule>)` (`EmailSender.java:11`), com `MessageRule` sem `public`
  (`MessageRule.java:4`); quem está fora do pacote só consegue repassar o resultado de `build`, nunca montar outra
  combinação;
- a mensagem e a cópia são gravadas em dois `INSERT` separados, sem transação (`EmailSender.java:24-30`): se o segundo
  falhar, a original fica sem a cópia que o contador pediu;
- os endereços fixos (`ArchiveCopyRule.java:6`, `TestModeRedirectRule.java:6`) são aceitáveis para o tamanho do
  sistema. O `enum MessageStatus` e o mapeamento em `SentMessages.java:28` e `:51` estão corretos.

## Testes

**Números:** 34 testes rodaram, 0 falhas, 0 erros, 0 pulados; 34 métodos escritos (base: 26). Cobertura de linhas 94,1% (542/576; base 93,8%) e de ramos 88,0% (125/142; base 87,1%); `hotel.messages` 99/105 linhas e 26/28 ramos.
**Nota:** com problemas

O agente acrescentou 7 testes contra o banco em `MessageRulesTest` e 1 pela API (`HttpApiTest.java:110`), e ajustou os
testes da base que passaram a ver a cópia (`ReservationServiceTest.java:92-100`, `:109`; `HttpApiTest.java:81-83`),
reforçando-os com o endereço e o assunto da cópia — nenhum foi enfraquecido. As regras novas são conferidas com
valores: a cópia com o assunto `[Cópia] ` e o mesmo texto (`MessageRulesTest.java:96-113`), o desvio com o endereço, o
assunto `[TESTE] ` e a primeira linha exata (`MessageRulesTest.java:57-70`), o bloqueio no modo de teste com o endereço
original (`MessageRulesTest.java:82-93`) e a ausência de cópia para bloqueada (`MessageRulesTest.java:116-124`).

O que fica sem rede: a validação só é exercitada com `ana@gmail`, `ana.gmail.com`, `@gmail.com`, `invalid` e
`invalid-email` (`MessageRulesTest.java:32-35`, `:85`, `:119`). Nenhum teste tem dois @, então apagar a contagem de
`EmailValidationRule.java:17-19` não derruba nada; nenhum teste tem ponto no início ou no fim do domínio, e é
justamente aí que a regra está errada (`ana@.` passa). A leitura de `HOTEL_TEST_MODE` (`Hotel.java:45`) também não é
testada; o modo de teste só é testado montando o `EmailSender` à mão. `testModeDoesNotCreateArchiveCopy`
(`MessageRulesTest.java:73-79`) repete o que `testModeRedirectsMessages` já confere (`:64`).

## Banco

**Números:** 3 tabelas, todas com chave primária; dinheiro em `numeric(10,2)`; nenhuma chave estrangeira sem índice, nenhuma data em texto; Flyway com 2 migrações, a `V1` igual à base e a `V2__add_message_status.sql` nova. A conferência acusa colunas de situação em `text` sem CHECK, entre elas a nova `messages.status`.
**Nota:** bom

O agente evoluiu o esquema do jeito certo: uma migração nova, sem tocar na que já existia
(`src/main/resources/db/migration/V2__add_message_status.sql:2`), com `NOT NULL DEFAULT 'SENT'`, o que preenche as
mensagens já gravadas num banco em uso. O SQL alterado continua com parâmetros (`SentMessages.java:23-28`) e
try-with-resources. `messages.status` sem CHECK segue o padrão que a base já tinha para `reservations.status`,
`rate_plan` e `rooms.type`; um CHECK (`status IN ('SENT','BLOCKED')`) seria melhor, mas o valor só é escrito a partir do
enum (`SentMessages.java:28`). A falta de transação entre a original e a cópia está contada no design. A
proteção contra reserva duplicada (o `EXCLUDE USING gist`) vem da base e não mudou.

## Comportamento

**Números:** 65 de 65 conferências passaram: parte comum 46/46, dados depois de reiniciar 3/3, cenário no modo normal 9/9, cenário no modo de teste 5/5, reservas simultâneas 2/2.
**Nota:** bom

Pela API, o sistema faz o que o dono pediu nos casos do roteiro: a mensagem válida sai `SENT` com a cópia logo
depois, com `[Cópia] ` e o mesmo texto; `ana@gmail`, `ana.gmail.com`, `ana@@gmail.com` e `@gmail.com` ficam
`BLOCKED`, sem cópia e com o endereço original; com `HOTEL_TEST_MODE=true` tudo vai para a caixa de testes com
`[TESTE] ` e a linha "Destinatário original: …", sem cópia, e o inválido continua bloqueado. Nada do que já
funcionava quebrou. Fora do roteiro, a validação deixa passar endereços como `ana@.` ou `ana@gmail.`
(`EmailValidationRule.java:25-26`); para o dono, isso quer dizer que esses erros de digitação não aparecem como
bloqueados para a recepção ligar. O defeito está contado no design e nos testes, sem mudar esta nota.

## Dúvidas

- A classe do pattern ficou entre `alternativa equivalente` e `malfeito`: a estrutura é uma lista de filtros que a
  referência aceita, com as três regras nela, mas a decisão por modo está dentro de duas regras e não na montagem, que
  é o critério que a referência usa para o malfeito.
- A leitura de "um nome com pelo menos um ponto no meio" como "o ponto não pode ser o primeiro nem o último caractere
  do domínio" é minha; o roteiro não confere esse ponto.
