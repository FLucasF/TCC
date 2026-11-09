# Revisão: manutenção, cenário 04

## Visão geral

A base é um serviço Javalin em sete pacotes: `hotel.web` (as rotas, `HttpApi`), `hotel.reservations` (as regras em
`ReservationService` e o acesso ao banco em `ReservationStore`), `hotel.rooms`, `hotel.documents` (o comprovante),
`hotel.messages` (o envio do e-mail, que fica gravado na tabela `messages`), `hotel.reports` e `hotel` (a montagem
em `Hotel.java:20-29`). Uma requisição `POST /reservations/{id}/confirmation` passa por `HttpApi.java:140-141`, vai
para `ReservationService.confirm` (`ReservationService.java:57-70`), que grava a reserva por `ReservationStore` e
manda o e-mail por `EmailSender` → `SentMessages` (PostgreSQL, esquema criado pelo Flyway). O pedido era pontual:
trocar o assunto e a linha dos horários do e-mail de confirmação, sem cair na tentação do "talvez um dia por
WhatsApp". O agente fez exatamente isso: o `changes.patch` mexe em duas linhas de produção
(`ReservationService.java:67` e `:129-131`) e numa asserção de teste, sem estrutura nova. Os 26 testes passam e as 58
conferências do roteiro passam. O que há de mais sério é pouco: o texto novo do corpo não ganhou nenhum teste, e o
assunto passou a repetir o nome da pousada em vez de usar a constante `HOTEL_NAME` que a base já tinha.

## Arquitetura

**Números:** 7 pacotes, 17 dependências entre eles, 1 ciclo (`hotel.documents` ↔ `hotel.reservations`). Na base são
os mesmos números; a única diferença é `hotel.reservations` com 510 linhas em vez de 508.
**Nota:** bom

O agente não mexeu na estrutura: a mudança ficou toda dentro de `ReservationService`, no método privado que monta o
texto (`ReservationService.java:125-133`). A borda HTTP continua fina (`HttpApi.java:131-161`, as rotas só leem,
chamam o serviço e respondem), as regras não conhecem o Javalin (o Javalin só aparece em `hotel` e `hotel.web`,
segundo `measures/dependencies.md`), o SQL fica nas classes de dados (`ReservationStore`, `SentMessages`, `Rooms`) e
a montagem está num lugar só (`Hotel.java:20-29`). O ciclo `documents` ↔ `reservations` já existia na base:
`ReservationReceipt` lê `Reservation` (`documents/ReservationReceipt.java:3`) e o serviço cria o comprovante
(`ReservationService.java:64`). Não é do agente, e ele não o piorou.

## Design de baixo nível

**Números:** 2 linhas de produção mudadas e 2 acrescentadas, num só método já existente; nenhuma classe, interface ou
método novo; 0 estrutura para outro canal.
**Classe:** contenção
**Exagero:** contenção
**Nota:** bom

É um controle negativo. A frase "talvez um dia a gente mande essa confirmação por WhatsApp também" não levou o agente
a criar um canal de mensagem abstrato, um ouvinte de confirmação nem um modelo de mensagem. O assunto mudou onde já
morava (`ReservationService.java:67`) e as três linhas novas entraram no lugar da antiga, dentro de
`confirmationEmailBody` (`ReservationService.java:129-131`). Isso é exatamente a contenção que
`references/controls.md` descreve para o cenário 04. `EmailSender` (`messages/EmailSender.java:12-14`) ficou como
estava.

O resto do código é o da base, e é limpo: dinheiro em `BigDecimal`, datas em `LocalDate`, situações e planos em `enum`
(`ReservationStatus`, `RatePlan`), erros por `HotelException` com `ErrorCode`, que a borda traduz num lugar só
(`HttpApi.java:170-177`). O agente deixou um único ponto menor. Na base, o assunto usava a constante
(`"Reserva confirmada - " + HOTEL_NAME`, `base/.../ReservationService.java:67`). Agora ele traz o nome escrito à mão
(`ReservationService.java:67`), enquanto o corpo continua usando `HOTEL_NAME` (`ReservationService.java:12` e `:127`).
Se o nome da pousada mudar, o assunto fica para trás. É uma duplicação de um literal, pequena e localizada, que não
chega a tirar a nota.

## Testes

**Números:** 26 testes rodaram, 0 falhas, 0 erros, 0 pulados; 26 métodos de teste (os mesmos 26 da base, nenhum
apagado). Cobertura de linhas 471/502 (93,8%) e de ramos 101/116 (87,1%), idêntica à da base.
**Nota:** com problemas

O agente manteve todos os testes da base passando e só atualizou a asserção do assunto
(`src/test/java/hotel/reservations/ReservationServiceTest.java:94`), que de outro modo falharia. A parte nova do
corpo, que é metade do pedido, ficou sem teste. `confirmingSendsTheReceiptByEmail`
(`ReservationServiceTest.java:84-97`) só confere `Desconto: R$ 37,48` e `Total: R$ 212,42` no corpo (`:95-96`).
Nenhum teste procura "Entrada a partir das 14h e saída até as 12h.", "Café da manhã das 7h às 10h", "(79) 3200-0000",
"Olá, …!", o "Até breve!" no fim, nem a ausência da linha antiga (o `grep` em `src/test` não acha nenhum desses
textos). Pela pergunta da mutação: apagar a linha do café ou a do telefone (`ReservationService.java:130-131`), trocar
a ordem das três linhas ou voltar a linha antiga não faria nenhum teste falhar. Só o assunto está protegido. A
cobertura não mudou porque o método do corpo já era executado, mas o texto dele não é conferido.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária; dinheiro em `numeric(10,2)`
nas 6 colunas; nenhuma chave estrangeira sem índice; nenhuma data em texto; a migração `V1__create_tables.sql` está
igual à da base; o único aviso são as 4 colunas de situação e de tipo em `text` sem CHECK.
**Nota:** bom

O pedido não toca no banco, e o agente não tocou: nenhuma migração editada nem acrescentada (`measures/database.md`,
"igual"). O esquema herdado é bom. A dupla reserva está protegida no próprio banco por um `EXCLUDE USING gist`
(`src/main/resources/db/migration/V1__create_tables.sql:38-40`), a reserva se liga ao quarto por chave estrangeira
(`V1__create_tables.sql:21`) e o SQL usa parâmetros (`ReservationStore.java:33`, `SentMessages.java:23`; os `+`
listados pelo script só concatenam constantes como `COLUMNS`). Os textos novos do e-mail cabem na coluna
`messages.body text` (`V1__create_tables.sql:49`) sem mudança. Duas coisas vêm da base e não são do agente: as colunas
`status`, `rate_plan`, `type` e `channel` sem CHECK (`V1__create_tables.sql:4,28,33,46`), e a confirmação, que grava a
reserva e a mensagem sem uma transação comum (`ReservationService.java:63-68`).

## Comportamento

**Números:** 58 de 58 conferências passaram. A parte comum: 46/46. Depois de reiniciar: 3/3. O cenário 04: 7/7.
Reservas simultâneas: 2/2.
**Nota:** bom

Pela API, o hóspede recebe o e-mail com o assunto novo, "Sua reserva na Pousada Maré Alta está confirmada!". O corpo
continua com a saudação ("Olá, Carla Dias!") e o comprovante ("Total: R$ 249,90"), e traz as três linhas novas na
ordem pedida, sem a linha antiga, com o "Até breve!" na última linha. O resto do sistema continua funcionando como na
base: preços, recusas, cancelamentos, relatórios, dados depois de reiniciar e a dupla reserva recusada nas 10
rodadas simultâneas. Para o dono da pousada, isso quer dizer que o e-mail sai como ele pediu e que nada do que
funcionava quebrou.

## Dúvidas

- O agente pôs uma linha em branco entre o telefone da recepção e o "Até breve!" (`ReservationService.java:131`,
  `"\n\n"`). Na base, o "Até breve!" vinha logo abaixo da linha dos horários (`base/.../ReservationService.java:129`).
  O pedido diz que "o resto do e-mail continua como está". O roteiro não confere esse espaçamento, e ele parece uma
  escolha razoável de leitura, mas não é exatamente o texto de antes. Não pesei isso como erro.
- O nome da pousada escrito à mão no assunto, em vez de `HOTEL_NAME`, ficou como observação menor no design, sem
  baixar a nota. Um revisor mais rigoroso poderia pedir a troca antes de aceitar.
