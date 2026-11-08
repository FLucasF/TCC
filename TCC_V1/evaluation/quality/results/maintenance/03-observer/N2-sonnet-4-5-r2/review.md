# Revisão: manutenção, cenário 03

## Visão geral

O sistema da base é um serviço Javalin em sete pacotes: `hotel.web` (as rotas, `HttpApi`) chama `hotel.reservations`
(`ReservationService`, com as regras, e `ReservationStore`, com o SQL), que usa `hotel.rooms`, `hotel.documents` (o
comprovante e a formatação) e `hotel.messages` (o `EmailSender`, que grava os e-mails na tabela `messages`). A montagem
fica em `hotel.Hotel`, e os dados ficam no PostgreSQL, com o esquema criado por uma migração do Flyway. O agente
acrescentou uma interface `ReservationListener` com duas implementações, `HousekeepingNotifier` e `FinanceNotifier`.
O `ReservationService` recebe a lista de ouvintes pelo construtor e a percorre na confirmação e no cancelamento
(`final/src/main/java/hotel/reservations/ReservationService.java:77-79` e `:100-105`), e a lista é montada em
`Hotel.java:30-31`. O comportamento está todo certo: 66 de 66 conferências passaram. O defeito mais sério está no
design. O Observer ficou pela metade: o e-mail do hóspede continua uma chamada direta no serviço
(`ReservationService.java:73-76`), e o próprio serviço decide quem é avisado no cancelamento, porque só chama os
ouvintes quando a reserva estava paga e ainda passa a eles um `wasPaid` que nesse ponto é sempre `true`.

## Arquitetura

**Números:** 7 pacotes, 17 dependências entre eles, 1 ciclo (`hotel.documents` ↔ `hotel.reservations`). Os números
batem com a base (`measures/dependencies-base.md`: os mesmos 7 pacotes, as mesmas 17 dependências e o mesmo ciclo).
O que mudou foram 3 classes a mais em `hotel.reservations` (13 → 16, 508 → 618 linhas) e o `ReservationService`, que
foi de 138 para 156 linhas.
**Nota:** bom

O agente não criou dependência nem ciclo. Os ouvintes usam `hotel.documents.TextFormat`
(`FinanceNotifier.java:3`, `HousekeepingNotifier.java:3`) e `hotel.messages.EmailSender`, duas direções que
`hotel.reservations` já tinha na base: `ReservationService.java:3-4` já importava `ReservationReceipt` e
`EmailSender`. O ciclo `documents` ↔ `reservations` vem da base. As regras continuam sem Javalin e sem SQL, e os
ouvintes são criados e registrados num lugar só, a montagem (`Hotel.java:24-31`), e não dentro do serviço. Há dois
detalhes menores. Os avisos à equipe moram em `hotel.reservations`, e um pacote próprio deixaria o de reservas menos
carregado à medida que entrarem o restaurante e o transfer. E `Hotel.java:30-31` usa nomes completos
(`java.util.List`, `hotel.reservations.HousekeepingNotifier`) em vez de imports. Nenhum dos dois pesa na nota.

## Design de baixo nível

**Números:** dos 5 avisos do cenário, os 4 novos (governança e financeiro, na confirmação e no cancelamento) estão nos
ouvintes. O que já existia, o e-mail do hóspede, ficou fora da estrutura, numa chamada direta do serviço. No
cancelamento, um dos filtros mora no serviço (`if (wasPaid)`), e não nos ouvintes.
**Classe:** malfeito
**Exagero:** contenção
**Nota:** com problemas

O que aproxima do Observer: há uma interface de ouvinte com dois eventos (`ReservationListener.java`) e uma
implementação por interessado. Cada ouvinte monta o próprio e-mail, com destino, assunto e texto
(`HousekeepingNotifier.java:19-41`, `FinanceNotifier.java:19-38`). O serviço recebe a lista de fora
(`ReservationService.java:26,31`) e só a percorre (`:77-79`, `:102-104`), sem conhecer a governança nem o financeiro.
Quem é avisado se muda na montagem (`Hotel.java:30-31`), e é isso que o pedido quer para o restaurante e o transfer.

O que afasta, e leva a `malfeito` pela referência:
- **O hóspede não virou ouvinte.** O serviço ainda monta e manda o e-mail dele
  (`ReservationService.java:71-76`, com o texto em `confirmationEmailBody`, `:143-149`). Para isso guarda um
  `EmailSender` só para esse fim (`:18,30`). A referência diz que o e-mail do hóspede também é um ouvinte e que "o
  hóspede com chamada direta e só a governança e o financeiro como ouvintes" é malfeito.
- **O serviço ainda decide quem é avisado no cancelamento.** Ele só chama os ouvintes quando a reserva estava
  confirmada (`ReservationService.java:100`) e passa a eles `wasPaid` (`:103`), que nesse ponto é sempre `true`.
  Com isso, a guarda `if (!wasPaid)` dos dois ouvintes é código morto (`HousekeepingNotifier.java:31`,
  `FinanceNotifier.java:30`), e um ouvinte futuro que quisesse saber de qualquer cancelamento, inclusive de reserva
  pendente, não saberia, porque o evento nem é publicado. O parâmetro `boolean wasPaid` da interface
  (`ReservationListener.java:10`) obriga todo ouvinte futuro a receber uma flag inútil.

Exagero: não há. A interface tem duas implementações e existe para que outras entrem, e não há fábrica, builder ou
camada que só repassa.

O resto do código, quanto ao que o agente escreveu: as classes novas são curtas e claras, usam `BigDecimal` com
`compareTo` para o reembolso (`FinanceNotifier.java:30`) e reaproveitam a formatação do comprovante
(`TextFormat.money`/`date`), em vez de copiá-la. Os defeitos que um revisor pediria para corrigir são o parâmetro
morto da interface e as guardas mortas citadas acima, e os nomes completos na montagem (`Hotel.java:30-31`). Há também
um ponto que fica em aberto: um ouvinte que lance exceção depois do `store.update` (`ReservationService.java:70`,
`:99`) faz a chamada responder erro com a reserva já gravada como confirmada ou cancelada. O agente não tratou isso,
mas a base já tinha o mesmo risco com o e-mail do hóspede. O agente também não atualizou o README: os avisos à equipe
não aparecem na seção "Como funciona".

## Testes

**Números:** 30 testes rodaram, 0 falharam, 0 erros e 0 pulados; há 30 métodos de teste no código. A base tinha 26,
todos passando. A cobertura de linhas foi de 93,8% para 94,2% (520 de 552) e a de ramos de 87,1% para 86,9% (113 de
130). Em `hotel.reservations`: 256 de 270 linhas e 61 de 66 ramos.
**Nota:** com problemas

O agente testou as quatro regras novas, contra o banco, com o serviço montado com os dois ouvintes
(`ReservationServiceTest.java:31-36`):
- a confirmação avisa a governança e o financeiro, com destino e assunto exatos (`:174-191`);
- o cancelamento de uma reserva confirmada avisa os dois (`:194-210`);
- uma reserva pendente cancelada não avisa ninguém (`:213-220`);
- uma NON_REFUNDABLE cancelada não avisa o financeiro (`:223-233`).

O único teste da base que mudou foi a contagem de mensagens no `HttpApiTest`, que passou de 1 para 3 (a mudança está
em `changes.patch`). A mudança é legítima, e nenhum teste da base foi apagado. As asserções do conteúdo, porém, são
fracas:
- o valor pago e o valor a devolver só são conferidos com `contains("R$ ")` (`:190`, `:209`). Se o financeiro
  recebesse `amountPaid` no lugar de `refund`, ou um valor errado, nenhum teste falharia;
- o número de hóspedes é conferido com `contains("2")` (`:187`), que passa sozinho por causa do "2027" das datas e do
  "201" do quarto. Trocar `reservation.guests()` por qualquer outro número não derruba o teste;
- o teste da NON_REFUNDABLE só confere o tamanho da lista e o destino da 4ª mensagem (`:231-232`). Não confere o
  assunto nem as datas desse aviso.

Os testes também dependem da ordem dos e-mails (`sent.get(1)`, `sent.get(2)`, ...), que o pedido diz não importar.
Isso não é erro, mas os deixa frágeis a uma mudança na ordem dos ouvintes. Nenhum teste confere que o e-mail do
hóspede continua igual depois da mudança: `:94-106` confere o texto do hóspede só no serviço sem ouvintes.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. O dinheiro está todo em
`numeric(10,2)`, as datas em `date`, e a chave estrangeira tem índice. A migração `V1__create_tables.sql` está "igual"
à da base, e não há migração nova. As conferências só acusaram as colunas de situação e de tipo em `text` sem CHECK
(`reservations.status`, `rate_plan`, `rooms.type`, `messages.channel`), o que vem da base. O `EXCLUDE USING gist`
contra duas reservas na mesma noite continua lá.
**Nota:** bom

O pedido não precisava mudar o esquema, e o agente não mexeu nele nem no SQL: os avisos à equipe são gravados na
tabela `messages` pelo mesmo `EmailSender` da base. O SQL montado com `+` que o script lista
(`ReservationStore.java:33,83,98,111`) só concatena constantes (`COLUMNS`) e vem da base. Há um único detalhe novo,
que não pesa: a confirmação agora grava até quatro linhas (a reserva e três mensagens) sem transação
(`ReservationService.java:70-79`; o script conta 0 `setAutoCommit(false)`). A base já gravava duas sem transação.

## Comportamento

**Números:** 66 de 66 conferências passaram, com 0 falhas: a parte comum, 46 de 46; os dados depois de reiniciar, 3 de
3; o cenário 03, 15 de 15; as duas reservas ao mesmo tempo, 2 de 2.
**Nota:** bom

Para o dono da pousada, tudo o que ele pediu funciona pela API:
- o hóspede continua recebendo o mesmo e-mail;
- na confirmação, a governança recebe "Reserva confirmada - quarto 201", com as duas datas e os 2 hóspedes, e o
  financeiro recebe "Pagamento recebido - reserva 1", com R$ 779,80;
- no cancelamento de uma reserva confirmada, a governança recebe as datas que ficaram livres, e o financeiro recebe
  "Reembolso - reserva 1", com R$ 779,80;
- a NON_REFUNDABLE cancelada avisa só a governança, e a reserva pendente cancelada não avisa ninguém.

Nada da parte comum quebrou.

## Dúvidas

- A nota de design: o principal defeito novo fora da classe do pattern é o parâmetro `wasPaid` morto na interface,
  que também é parte do pattern. Separando com rigor o que é do pattern e o que é do resto, a nota poderia ser `bom`.
  Dei `com problemas` porque um revisor exigente pediria para tirar o parâmetro e as guardas mortas antes de aceitar.
