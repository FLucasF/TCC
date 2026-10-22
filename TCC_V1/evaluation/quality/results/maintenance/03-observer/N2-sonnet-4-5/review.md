# Revisão: manutenção, cenário 03

## Visão geral

A base é um serviço Javalin em sete pacotes: `hotel.web` (a borda HTTP, `HttpApi`), `hotel.reservations` (as regras,
`ReservationService`, e o acesso ao banco, `ReservationStore`), `hotel.rooms`, `hotel.messages` (o `EmailSender`, que
grava cada e-mail na tabela `messages`), `hotel.documents` (o comprovante e a formatação), `hotel.reports` e `hotel`
(a montagem, `Hotel`). Uma requisição entra em `HttpApi`, chama o `ReservationService`, que usa `ReservationStore` e
`Rooms` (JDBC com pool) sobre o PostgreSQL, com o esquema criado pelo Flyway. A mudança do agente é pequena e
localizada: uma interface `ReservationListener`, dois ouvintes (`HousekeepingNotifier`, `FinanceNotifier`) recebidos
pelo `ReservationService` numa lista montada em `Hotel`, e um teste novo (`NotificationTest`). Tudo funciona: 66 de 66
conferências do roteiro e 30 de 30 testes. O que há de mais sério é de design: o e-mail do hóspede continua sendo
mandado à mão dentro do serviço, fora da lista de ouvintes (`final/src/main/java/hotel/reservations/ReservationService.java:72-79`),
e a regra "reserva pendente cancelada não avisa ninguém" foi repetida dentro de cada ouvinte em vez de ficar no
serviço. O README não foi atualizado (a seção "Como funciona" não fala dos avisos à equipe).

## Arquitetura

**Números:** 7 pacotes e 17 dependências entre eles, iguais aos da base (`measures/dependencies.md` e
`measures/dependencies-base.md`); 1 ciclo (`hotel.documents` ↔ `hotel.reservations`), que já existia na base.
`hotel.reservations` passou de 13 para 16 classes (508 → 615 linhas); a maior classe continua `HttpApi` (170 linhas).
**Nota:** bom

O agente não criou dependência nova nem ciclo novo: os ouvintes ficam em `hotel.reservations` e usam só
`hotel.messages.EmailSender` e `hotel.documents.TextFormat`, dependências que o pacote já tinha
(`HousekeepingNotifier.java:3-4`, `FinanceNotifier.java:3-4`). O ciclo `documents` ↔ `reservations` vem da base
(`ReservationReceipt` lê a reserva e o serviço monta o comprovante); a mudança só soma mais um uso de
`reservations → documents` pelo `TextFormat`, sem piorá-lo. A montagem continua num lugar só: os ouvintes são criados
e registrados em `src/main/java/hotel/Hotel.java:27-33`, e o serviço recebe a lista de fora e a copia
(`ReservationService.java:26,31`), sem `new` de ouvinte por dentro. A borda HTTP não foi tocada (o pacote `hotel.web`
segue sem JDBC, pela tabela de bibliotecas), e as regras continuam sem Javalin. O `ReservationService` cresceu de 138
para 153 linhas, ainda num tamanho razoável.

## Design de baixo nível

**Números:** 3 interessados no pedido (hóspede, governança, financeiro); 2 viraram ouvintes
(`HousekeepingNotifier`, `FinanceNotifier`) e 1 ficou fora da estrutura (o hóspede, chamado direto em
`ReservationService.java:73-76`, com o texto em `ReservationService.java:140-146`). Os dois eventos (confirmação e
cancelamento) passam pela lista de ouvintes.
**Classe:** malfeito
**Exagero:** contenção
**Nota:** com problemas

O pattern: o que aproxima do Observer é real. Há uma interface de ouvinte com os dois eventos
(`ReservationListener.java:5-10`), uma implementação por interessado novo, a lista recebida de fora e registrada na
montagem (`Hotel.java:33`), e o serviço percorre a lista nos dois eventos (`ReservationService.java:77-79` e
`100-102`) sem conhecer os endereços nem montar os textos da governança e do financeiro. Para pôr o restaurante ou o
transfer, basta uma classe nova e uma linha em `Hotel`. O que afasta: o hóspede, o interessado que já existia, ficou
com a chamada direta de antes (`ReservationService.java:72-76`), e o serviço ainda guarda o `EmailSender`
(`ReservationService.java:18`) e o texto do e-mail dele (`ReservationService.java:140-146`). A referência do cenário
põe esse caso, "o hóspede tem chamada direta e só a governança e o financeiro viraram ouvintes", em malfeito: a
estrutura convive com o jeito antigo.

Outro ponto da estrutura: o evento de cancelamento leva um `boolean wasConfirmed`
(`ReservationListener.java:9`, `ReservationService.java:93,101`), e cada ouvinte repete o mesmo teste para ignorar a
reserva pendente (`HousekeepingNotifier.java:30-32`, `FinanceNotifier.java:30-32`). A regra "a reserva que nem chegou
a ser confirmada pode ser cancelada sem avisar ninguém" vale para todos os interessados, mas foi copiada em cada um;
o próximo ouvinte (o restaurante, o transfer) precisa lembrar dela, e quem usa a estrutura continua perguntando qual
é o caso. O serviço poderia simplesmente não anunciar esse cancelamento, ou anunciar um evento próprio.

O exagero: contenção. Uma interface com duas implementações registradas, sem fábrica, sem barramento de eventos, sem
classe de evento genérica; nada além do que o pedido justifica.

O resto do código que o agente escreveu: os tipos estão certos (dinheiro em `BigDecimal` formatado por
`TextFormat.money`, datas em `LocalDate` por `TextFormat.date`; `FinanceNotifier.java:33-35` usa `signum()` para o
"nada a devolver"), os nomes dizem o que fazem e não há estado mutável novo. Os defeitos são pequenos e localizados:
o teste `wasConfirmed` duplicado acima; as linhas de entrada e saída repetidas entre os dois e-mails da governança
(`HousekeepingNotifier.java:22-23` e `36-37`); e os endereços fixos como constantes em cada classe
(`HousekeepingNotifier.java:9`, `FinanceNotifier.java:10`), aceitável neste tamanho. O resto do projeto vem da base e
não foi piorado.

## Testes

**Números:** final: 30 testes rodaram, 0 falhas, 0 erros, 0 pulados; 30 métodos escritos. Linhas 522 de 553 (94,4%),
ramos 113 de 128 (88,3%); `hotel.reservations` com 258 de 271 linhas e 61 de 64 ramos. Base: 26 testes, linhas 93,8%,
ramos 87,1% (`measures/tests.md`, `measures/tests-base.md`).
**Nota:** bom

O agente manteve os 26 testes da base passando e não enfraqueceu nenhum: `ReservationServiceTest` e `ReportsTest` só
ganharam o novo parâmetro do construtor (`List.of()`), e `HttpApiTest.java:81-84` ficou mais forte (passa de 1 para 3
mensagens e confere os três destinatários, pelo `Hotel` de verdade, o que também prova a montagem). Os 4 testes novos
de `src/test/java/hotel/reservations/NotificationTest.java` conferem a parte nova com valores, contra o banco: na
confirmação, o destinatário, o assunto e o corpo inteiro da governança (datas 15/03/2027 e 17/03/2027, 2 hóspedes) e
do financeiro (R$ 779,80) (`NotificationTest.java:39-65`); no cancelamento STANDARD confirmado, os dois avisos com
corpo e reembolso (`:67-91`); no NON_REFUNDABLE, só a governança (4 mensagens, `:93-106`); na pendente, nenhuma
mensagem (`:108-117`). A pergunta da mutação: apagar o `if (!wasConfirmed)` da governança derruba `:108-117`; apagar o
`signum() == 0` do financeiro derruba `:93-106`; trocar o assunto ou o valor derruba `:39-65`. Ficam sem rede só
pontos sem efeito visível: o `wasConfirmed` do financeiro (`FinanceNotifier.java:30`) é redundante, porque a pendente
tem reembolso zero. Fraquezas menores: os testes dependem da ordem das mensagens (`sent.get(1)`, `sent.get(2)`), que o
pedido diz não importar, o que os torna frágeis a uma mudança de ordem dos ouvintes; o caso NON_REFUNDABLE só confere
o assunto da governança (`NotificationTest.java:104-105`); e o cancelamento pela API não tem teste de avisos (só pelo
serviço).

## Banco

**Números:** 3 tabelas (`messages`, `reservations`, `rooms`), todas com chave primária; dinheiro em `numeric(10,2)`;
nenhuma chave estrangeira sem índice; nenhuma data em texto; a migração da base `V1__create_tables.sql` igual; 1
migração aplicada pelo Flyway; o `EXCLUDE USING gist` protege a reserva dupla (`measures/database.md`). As colunas
`status`, `rate_plan`, `type` e `channel` sem `CHECK` vêm da base.
**Nota:** bom

O pedido não exigia mudar o esquema, e o agente não mexeu nele: os avisos usam a tabela `messages` que já existia, por
`EmailSender`/`SentMessages` com SQL parametrizado (`src/main/java/hotel/messages/SentMessages.java:153-157`). Os SQL
montados com `+` em `ReservationStore.java:33,83,98,111` juntam só constantes (base). Uma observação, sem pesar na nota:
não há transação (0 `setAutoCommit(false)`), então a confirmação grava a reserva (`ReservationService.java:70`) e
depois, em conexões separadas, até três mensagens (`:73-79`); se uma delas falhar, a reserva fica confirmada com parte
dos avisos gravada. Esse jeito vem da base (o e-mail do hóspede já era gravado depois do `update`); o agente só somou
mais escritas no mesmo padrão, inclusive no cancelamento (`:99-102`).

## Comportamento

**Números:** 66 de 66 conferências passaram, 0 falharam: parte comum 46 de 46; dados depois de reiniciar 3 de 3;
cenário 03 15 de 15; duas reservas ao mesmo tempo 2 de 2 (`measures/behavior.md`).
**Nota:** bom

Tudo o que o dono pediu acontece pela API: na confirmação, o hóspede continua recebendo o mesmo e-mail, a governança
recebe "Reserva confirmada - quarto 201" com 10/03/2027, 12/03/2027 e 2 hóspedes, e o financeiro recebe "Pagamento
recebido - reserva 1" com R$ 779,80; no cancelamento de uma confirmada, a governança recebe "Reserva cancelada -
quarto 201" com as datas e o financeiro "Reembolso - reserva 1" com R$ 779,80; no NON_REFUNDABLE o financeiro não é
avisado; e a pendente cancelada não manda nada. O que já funcionava continua funcionando (preços, recusas, check-in e
check-out, relatórios), os dados sobrevivem ao reinício e a reserva dupla simultânea é recusada nas 10 rodadas.

## Dúvidas

Na classe do pattern, entre `malfeito` e `acerto`: a governança e o financeiro estão inteiros na estrutura, nos dois
eventos, e a referência do cenário nomeia exatamente o caso do hóspede com chamada direta como malfeito; por isso a
classe ficou em malfeito, embora o resto da estrutura esteja certo. Na nota do design, entre `bom` e `com problemas`:
os defeitos são poucos e pequenos (a regra da pendente repetida em cada ouvinte, o texto das datas repetido); dei
`com problemas` porque a repetição da regra é justamente o que o próximo ouvinte vai ter de copiar.
