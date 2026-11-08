# Revisão: manutenção, cenário 03

## Visão geral

A base é um serviço Javalin em oito pacotes depois da mudança: `hotel.web` (as rotas, `HttpApi`), `hotel.reservations` (as regras em `ReservationService` e o acesso ao banco em `ReservationStore`), `hotel.rooms`, `hotel.messages` (o `EmailSender`, que grava cada e-mail na tabela `messages`), `hotel.documents`, `hotel.reports`, e a montagem em `hotel.Hotel`. Uma requisição entra por `HttpApi`, chama `ReservationService`, que usa `ReservationStore`/`Rooms` (JDBC sobre PostgreSQL, esquema pela migração Flyway `V1__create_tables.sql`). O agente criou a interface `ReservationListener` (`reservations/ReservationListener.java:7`), dois ouvintes num pacote novo `hotel.notifications` (`GovernanceNotifier`, `FinanceNotifier`), registrados em `Hotel.java:30-32`, e o serviço percorre a lista na confirmação e no cancelamento (`ReservationService.java:81`, `:102`). O roteiro de comportamento passa inteiro (66 de 66) e os 30 testes passam. O mais sério é de design: o e-mail do hóspede continua como chamada direta dentro do serviço (`ReservationService.java:76-80`), fora da lista de ouvintes. Os testes novos também têm algumas asserções fracas.

## Arquitetura

**Números:** 8 pacotes (base: 7), 21 dependências entre pacotes (base: 17), 1 ciclo (`hotel.documents` ↔ `hotel.reservations`), que já existia na base, igual. O pacote novo `hotel.notifications` depende de `documents`, `messages` e `reservations`, e só `hotel` (a montagem) depende dele. Nenhum pacote novo usa Javalin nem JDBC.
**Nota:** bom

A mudança respeita as camadas. A interface do ouvinte fica no pacote das regras (`reservations/ReservationListener.java:7`), e os ouvintes concretos ficam fora dele (`notifications/GovernanceNotifier.java:9`, `notifications/FinanceNotifier.java:9`). Assim, `hotel.reservations` não passa a depender de `hotel.notifications` e nenhum ciclo novo aparece (`measures/dependencies.md`). Quem é avisado é decidido num lugar só, a montagem (`Hotel.java:24-38`), e o serviço recebe a lista pronta pelo construtor (`ReservationService.java:26-32`). A borda HTTP não mudou. O ciclo `documents` ↔ `reservations` vem da base (`ReservationService.java:3` importa `ReservationReceipt`, e `documents/ReservationReceipt.java:3` importa `Reservation`), e o agente não o piorou. A maior classe continua sendo `HttpApi` (170 linhas). `ReservationService` cresceu de 138 para 153 linhas.

## Design de baixo nível

**Números:** 3 interessados pedidos ou já existentes (hóspede, governança, financeiro). 2 estão na estrutura de ouvintes (governança e financeiro) e 1 ficou fora dela (o e-mail do hóspede, chamado direto no serviço). Os 2 eventos (confirmação e cancelamento) passam pela lista.
**Classe:** malfeito
**Exagero:** contenção
**Nota:** bom

**O que aproxima do Observer:**
- Há uma interface de ouvinte com dois eventos, com implementações vazias por padrão (`ReservationListener.java:10`, `:17`).
- Cada interessado novo é uma classe própria, que monta o próprio texto (`GovernanceNotifier.java:20-39`, `FinanceNotifier.java:20-36`).
- O serviço recebe a lista de fora (`ReservationService.java:32`) e só a percorre (`:81`, `:102`), sem conhecer governança nem financeiro.
- O registro fica na montagem (`Hotel.java:30-32`). Assim, o restaurante e o transfer entram com uma classe nova e uma linha a mais na lista.

**O que afasta:** o hóspede, o interessado que já existia, não virou ouvinte. `ReservationService.confirm` continua montando o comprovante e mandando o e-mail ele mesmo (`ReservationService.java:76-80`, com o texto em `confirmationEmailBody`, `:140-146`). O serviço ainda guarda o `EmailSender` (`:18`) só para isso. A referência do cenário diz com todas as letras que "o hóspede tem chamada direta e só a governança e o financeiro viraram ouvintes" é malfeito.

**O exagero:** não há estrutura sem necessidade. A interface tem duas implementações reais e é o ponto do pattern. Não há fábrica nem registro dinâmico. O construtor antigo, que agora delega com `List.of()` (`ReservationService.java:21-24`), só mantém os testes da base compilando, e não é camada nova.

**O resto do código:** é limpo. Os valores e as datas usam o `TextFormat` que já existia (`GovernanceNotifier.java:24-25`, `FinanceNotifier.java:23`), sem duplicar a formatação. A `Reservation` passada aos ouvintes não pode ser alterada por eles, porque os métodos que mudam o estado são de pacote (`Reservation.java:102-114`). A lista é copiada de forma imutável (`ReservationService.java:32`). Há dois detalhes pequenos, que não mudam a nota:
- O argumento booleano `wasConfirmed` (`ReservationListener.java:17`) obriga cada ouvinte a repetir a mesma guarda (`GovernanceNotifier.java:31`, `FinanceNotifier.java:29`).
- O `forEach` não isola um ouvinte que falhe (`ReservationService.java:81`). Se um deles lançar uma exceção, os seguintes não são avisados e a chamada responde erro, com a reserva já gravada como confirmada (`:75`). O mesmo já acontecia na base com o e-mail do hóspede.

## Testes

**Números:** 30 testes rodaram, 0 falhas, 0 erros, 0 pulados. 30 métodos escritos (base: 26; os 4 novos estão em `StaffNotificationTest`). Cobertura de linhas 93,9% (510/543; base 93,8%) e de ramos 87,9% (109/124; base 87,1%). `hotel.notifications`: 31 de 31 linhas e 6 de 6 ramos.
**Nota:** com problemas

**O que está bom:**
- Os 26 testes da base continuam passando.
- O único teste da base que mudou foi o de `HttpApiTest.java:81`, que agora espera 3 mensagens em vez de 1. É o ajuste certo para o novo comportamento, e a linha 82 continua conferindo que o hóspede recebe o e-mail.
- Os testes novos cobrem as quatro regras do pedido contra o banco de verdade: a confirmação avisa os dois (`StaffNotificationTest.java:49-63`), o cancelamento de uma confirmada avisa os dois (`:66-78`), sem devolução o financeiro não é avisado (`:81-89`) e a pendente cancelada não avisa ninguém (`:92-97`).
- Os assuntos são conferidos com o texto exato.

**As asserções fracas:**
- O número de hóspedes é conferido com `body().contains("2")` (`:58`). As datas já têm o "2" (`10/03/2027`), então apagar a linha "Hóspedes" do e-mail da governança não faria nenhum teste falhar.
- Os valores em reais não são fixos. O teste compara com `TextFormat.money(confirmed.amountPaid())` e `TextFormat.money(cancelled.refund())`, calculados pelo próprio código (`:61-62`, `:76-77`). Trocar `refund()` por `amountPaid()` no `FinanceNotifier` também passaria, porque no plano STANDARD os dois valores são iguais.
- No cancelamento, o teste não confere o destinatário de cada mensagem (só o assunto, por posição, `:73-75`) nem a data de saída (`:74`, só a de entrada).
- O teste do cancelamento depende da ordem dos e-mails (`all.get(3)`, `all.get(4)`), que o pedido diz não importar.

## Banco

**Números:** 3 tabelas (`messages`, `reservations`, `rooms`), todas com chave primária. Dinheiro em `numeric(10,2)`, datas em `date`, a chave estrangeira com índice. A única migração (`V1__create_tables.sql`) está igual à da base. As conferências só acusam as colunas de situação/tipo em `text` sem CHECK (`reservations.status`, `reservations.rate_plan`, `rooms.type`, `messages.channel`), o que vem da base. 0 transações explícitas.
**Nota:** bom

O agente não mexeu no esquema nem no SQL. Os avisos novos usam o `EmailSender` → `SentMessages.add`, que já existia e usa parâmetros (`messages/SentMessages.java:174-189`), e não precisavam de tabela nova. O que vem da base e o agente não piorou:
- A duplicação de reserva é impedida por um `EXCLUDE USING gist` no banco.
- O SQL montado com `+` só junta constantes (`ReservationStore.java:33`, `:83`, `:98`, `:111`).
- Não há transação entre gravar a reserva (`ReservationService.java:75`, `:101`) e gravar as mensagens. Agora há mais mensagens por operação, mas é o mesmo arranjo da base para o e-mail do hóspede.

## Comportamento

**Números:** 66 de 66 conferências passaram, 0 falharam:
- a parte comum: 46 de 46;
- os dados depois de reiniciar: 3 de 3;
- o cenário 03: 15 de 15;
- reservas ao mesmo tempo: 2 de 2.
**Nota:** bom

Para o dono da pousada, quando a reserva é confirmada:
- o hóspede continua recebendo o mesmo e-mail com o comprovante;
- a governança recebe "Reserva confirmada - quarto 201", com a entrada, a saída e quantos hóspedes vêm;
- o financeiro recebe "Pagamento recebido - reserva 1", com o valor em R$ 779,80.

Quando uma reserva confirmada é cancelada, a governança recebe as datas liberadas e o financeiro recebe "Reembolso", com o valor. Se não há nada a devolver (NON_REFUNDABLE), só a governança é avisada. Uma reserva pendente cancelada não avisa ninguém. Tudo o que já funcionava continua funcionando, inclusive depois de reiniciar o serviço e com dois pedidos ao mesmo tempo.

## Dúvidas

- O pedido diz "tanto faz a ordem em que os e-mails saem". O agente manda o do hóspede primeiro, por chamada direta. Isso não muda o comportamento, mas é o motivo do `malfeito`: se o pedido não tivesse deixado claro que o hóspede é mais um interessado, alguém poderia defender que o e-mail ao hóspede é parte da confirmação e não um aviso. A referência do cenário fecha essa questão a favor de `malfeito`.
- Na nota de testes, `com problemas` contra `bom` depende de quanto pesam as asserções fracas (`StaffNotificationTest.java:58`, `:61-62`, `:76-77`). As regras estão todas exercitadas, mas algumas mutações simples passariam sem nenhum teste falhar.
