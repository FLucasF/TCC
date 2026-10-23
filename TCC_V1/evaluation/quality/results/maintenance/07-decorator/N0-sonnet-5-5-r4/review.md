# Revisão: manutenção, cenário 07

## Visão geral

A base é um serviço Javalin em sete pacotes: `hotel.web` (as rotas, `HttpApi`), `hotel.reservations` (as regras e o
acesso às reservas), `hotel.rooms`, `hotel.messages`, `hotel.reports`, `hotel.documents` e `hotel`, onde fica a
montagem (`Hotel`). Uma requisição entra em `HttpApi`, chama `ReservationService`, que usa `ReservationStore`/`Rooms`
(JDBC, PostgreSQL com migrações do Flyway). A confirmação manda o e-mail por `EmailSender.send`
(`final/src/main/java/hotel/reservations/ReservationService.java:65`). Toda a mudança ficou em `hotel.messages`. Há uma
lista de regras de envio (`SendingRule`, com três implementações). Cada regra vale num escopo (`RuleScope`), e a
combinação é escolhida a partir do ambiente em `SendingRules.from`, chamado pela montagem em `Hotel.java:32`. Há também
uma coluna `status` nova, criada por uma migração `V2` aditiva, e 6 testes novos. Os 65 checks do roteiro passaram,
os 32 testes passam e a estrutura de pacotes ficou igual à da base. Não há nada sério. Os defeitos são pequenos: o
motivo do bloqueio é calculado e jogado fora, a mensagem e a cópia são gravadas fora de uma transação, e o README não
foi atualizado (ele não fala do modo de teste, do `status` nem de `HOTEL_ARCHIVE_COPY`).

## Arquitetura

**Números:** 7 pacotes e 17 dependências, nos dois lados (base e final, `measures/dependencies.md` e
`measures/dependencies-base.md`). Há 1 ciclo, `hotel.documents` ↔ `hotel.reservations`, que já existia na base.
`hotel.messages` passou de 3 classes e 75 linhas para 12 classes e 264 linhas, e continua sem depender de nenhum outro
pacote do projeto. A maior classe nova é `SendingRules` (109 linhas).
**Nota:** bom

O agente não criou nenhuma dependência nova nem ciclo novo. O ciclo `documents`/`reservations` veio da base. As regras
de envio ficaram no pacote das mensagens, sem Javalin e sem SQL (`SendingRules.java:1-109`, `SendingRule.java`). O SQL
continua isolado em `SentMessages` (`SentMessages.java:27-48`). A borda HTTP não mudou: `/messages` só serializa a
lista (`HttpApi.java:81`), e o campo `status` vem do record (`Message.java:3`). A montagem continua num lugar só.
`Hotel.java:27-32` recebe o ambiente e monta o `EmailSender` com as regras, e o `main` passa `System.getenv()`
(`Hotel.java:51`). O código não lê variável de ambiente em nenhum outro lugar, e não há `static` global.
`ReservationService` não ficou sabendo de nada novo: ele continua chamando `emailSender.send(...)`
(`ReservationService.java:65`).

## Design de baixo nível

**Números:** as 3 regras pedidas estão na estrutura, cada uma numa classe própria: `ValidAddressRule`
(`SendingRules.java:114`), `TestRedirectRule` (`:133`) e `ArchiveCopyRule` (`:143`). Nenhuma fica fora dela, e há 0
`if` de modo dentro do envio ou das regras. A combinação por modo está num lugar só (`SendingRules.java:73-81`).
**Classe:** alternativa equivalente
**Exagero:** contenção
**Nota:** bom

**O pattern.** Não é um Decorator: nenhuma regra embrulha o envio nem chama o próximo. É o que a referência aceita como
alternativa: uma lista de filtros que o envio aplica, em que cada regra é uma peça própria e a combinação por modo é
decidida fora das regras. Isto aproxima a solução do pedido:

- a interface `SendingRule` (`SendingRule.java:10-26`) tem três ganchos com padrão neutro: bloquear, reescrever e
  pedir cópias;
- cada regra implementa só o gancho dela, e nenhuma pergunta pelo modo de teste: o desvio (`SendingRules.java:135-139`)
  não sabe se está em teste, e a cópia (`:145-147`) também não;
- o modo entra só pelo `RuleScope` (`RuleScope.java:9-15`), associado a cada regra em `SendingRules.from`
  (`SendingRules.java:75-78`). Assim a combinação "bloqueio sempre, desvio só em teste, cópia só fora" fica escrita
  num lugar só, e a segunda pousada desliga a cópia pelo ambiente (`:77`);
- o `EmailSender` não conhece nenhuma regra (`EmailSender.java:27-39`), só a ordem das fases: bloquear sobre o
  original, depois reescrever, depois gerar as cópias da mensagem que saiu.

As regras anunciadas cabem sem mudar o envio: "não mandar de madrugada" e "limite por dia" seriam outro `blocks`
somado com um `RuleScope`. Duas coisas afastam a solução do Decorator puro:

- a ordem das fases está fixa no `EmailSender`, e não na pilha;
- a combinação mora numa fábrica estática do pacote das mensagens, chamada pela montagem, e não na própria `Hotel`.

**O exagero.** Não achei estrutura sem necessidade. O record `Draft` é a mensagem que passa pelas regras. O enum
`RuleScope` atende ao que o texto pede ("pode valer sempre, só no modo de teste ou só fora dele"). A interface tem três
implementações.

**O resto do código.** Os tipos estão certos: `MessageStatus` é um enum (`MessageStatus.java:3-6`), e não texto solto.
A validação do endereço é curta e segue a definição do dono (`SendingRules.java:120-130`). Há só detalhes:

- `blockReason` devolve um motivo, e `EmailSender.java:29-31` o descarta: nem guarda nem registra. Ou o motivo vai
  para algum lugar, ou o método deveria ser um `boolean`;
- `active()` refiltra a lista três vezes a cada envio (`SendingRules.java:84, 95, 103, 109-111`), o que é inofensivo
  com três regras;
- o construtor de conveniência `EmailSender(SentMessages)` (`EmailSender.java:17-19`) liga em silêncio as regras de
  produção, inclusive a cópia. Ele só é usado nos testes;
- o README não foi atualizado com `HOTEL_TEST_MODE`, `HOTEL_ARCHIVE_COPY` e o campo `status`.

Nada disso pede mudança antes de aceitar.

## Testes

**Números:** 32 testes rodaram, com 0 falhas e 0 pulados. São 32 métodos escritos, contra 26 na base, e os 6 novos estão
em `SendingRulesTest`. A cobertura de linhas foi de 93,8% para 94,5% (547/579), e a de ramos de 87,1% para 89,1%
(131/147). Em `hotel.messages`, os testes cobrem 102 de 107 linhas e 32 de 33 ramos (`measures/tests.md`).
**Nota:** bom

As regras novas estão conferidas com valores, contra o banco de verdade:

- a validação: 2 endereços válidos e 10 inválidos, entre eles os dois do texto (`SendingRulesTest.java:28-35`);
- o bloqueio sem cópia, com o endereço original (`:38-46`);
- a cópia logo depois da original, com `[Cópia] `, o mesmo texto e `SENT` (`:49-60`);
- o modo de teste: endereço de testes, `[TESTE] `, a primeira linha `Destinatário original: ` e nenhuma cópia (`:63-72`);
- o bloqueio no modo de teste, com o original (`:75-84`);
- a instalação que desliga a cópia (`:87-90`).

A pergunta da mutação: trocar o escopo da cópia ou do desvio, mudar os prefixos ou mudar o `at <= 0` derrubaria um
teste.

Dos testes da base, dois mudaram de 1 para 2 mensagens (`ReservationServiceTest.java:91, 105`) e um passou a esperar 2
(`HttpApiTest.java:81`). É a mudança pedida, e não uma asserção enfraquecida. `HttpApiTest.java:82` ainda passou a
conferir o `status`.

Faltam três coisas pequenas:

- nenhum teste pela API mostra uma mensagem `BLOCKED` ou o modo de teste ligado pelo ambiente: `HttpApiTest.java:35`
  usa o construtor sem ambiente;
- `ReservationServiceTest.java:91` conta 2 mensagens mas não confere que a segunda é a cópia;
- o caso mínimo válido (`a@b.c`) não está testado.

## Banco

**Números:** 3 tabelas, todas com chave primária. O dinheiro está em `numeric(10,2)` e as datas em `date`. Nenhuma
chave estrangeira está sem índice. As migrações são `V1` (igual à da base) e `V2__message_status.sql` (nova). A
conferência das colunas de situação acusou `messages.status: text, sem CHECK`, como as outras colunas de situação, que
já eram assim na base (`measures/database.md`).
**Nota:** bom

O agente evoluiu o esquema do jeito certo: com uma migração nova e aditiva, sem tocar na `V1`
(`V2__message_status.sql:2`, `ADD COLUMN status text NOT NULL DEFAULT 'SENT'`). As mensagens antigas ficam como
enviadas, e quem já roda o sistema sobe sem erro. O SQL novo usa parâmetros (`SentMessages.java:29-35`) e devolve as
conexões com try-with-resources. Há dois detalhes:

- `messages.status` não tem `CHECK (status IN ('SENT','BLOCKED'))`. Isso segue a convenção da base, que também não tem
  CHECK em `reservations.status`;
- a original e a cópia são gravadas em dois `INSERT` com conexões separadas, sem transação (`EmailSender.java:34-37`).
  Se o segundo falhar, a original fica sem cópia. A ordem "logo depois" é garantida por `synchronized`
  (`EmailSender.java:27`), o que só vale dentro de um processo. A falta de transação entre a reserva confirmada e o
  e-mail (`ReservationService.java:63-68`) veio da base.

A reserva duplicada continua protegida pelo `EXCLUDE` da base.

## Comportamento

**Números:** 65 de 65 checks passaram (`measures/behavior.md`):

| parte | passaram |
|---|---|
| parte comum | 46 de 46 |
| depois de reiniciar | 3 de 3 |
| cenário, modo normal | 9 de 9 |
| cenário, modo de teste | 5 de 5 |
| reservas simultâneas | 2 de 2 |

**Nota:** bom

Para o dono da pousada, as três regras funcionam como ele pediu:

- o endereço digitado errado ("ana@gmail", "ana.gmail.com", "ana@@gmail.com", "@gmail.com") não sai. Ele aparece em
  `/messages` como `BLOCKED`, com o endereço que o hóspede deu, e sem cópia;
- cada e-mail enviado ganha logo depois a cópia para `arquivo@pousadamarealta.com.br`, com "[Cópia] " no assunto e o
  mesmo texto;
- com `HOTEL_TEST_MODE=true`, nada vai para o hóspede: tudo vai para a caixa de testes, com "[TESTE] " no assunto, a
  linha do destinatário original e nenhuma cópia, e o endereço inválido continua bloqueado.

Tudo o que já funcionava continua funcionando, e os dados continuam lá depois de reiniciar.

## Dúvidas

- **Classe:** `alternativa equivalente` ou `acerto`? A estrutura é uma lista de filtros com fases, e não embrulhos que
  se chamam. A combinação por modo está em `SendingRules.from` (`SendingRules.java:73-81`), uma fábrica no pacote das
  mensagens chamada pela montagem, e não na própria `Hotel`. Como as regras não perguntam pelo modo e o envio não
  conhece as regras, fiquei com `alternativa equivalente`.
- **`HOTEL_ARCHIVE_COPY`:** a variável foi uma decisão do agente para "a segunda pousada não quer a cópia", que o texto
  deixou em aberto. O roteiro não a testa. O teste `SendingRulesTest.java:87-90` mostra que ela funciona.
