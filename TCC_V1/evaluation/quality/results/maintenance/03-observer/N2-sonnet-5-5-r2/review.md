# Revisão: manutenção, cenário 03

## Visão geral

A base já era organizada em pacotes por assunto: `hotel.web` (a borda Javalin, `HttpApi`), `hotel.reservations` (as
regras em `ReservationService` e o acesso ao banco em `ReservationStore`), `hotel.rooms`, `hotel.messages` (os e-mails
"enviados" gravados na tabela `messages`), `hotel.documents` (o comprovante), `hotel.reports` e a montagem em
`hotel.Hotel`. Uma requisição entra pelo `HttpApi`, chama o `ReservationService`, que lê e grava pelo
`ReservationStore` e pelo `Rooms` num PostgreSQL com o esquema criado pelo Flyway. O agente fez o que o pedido
precisava com o Observer: criou a interface `ReservationListener` (`src/main/java/hotel/reservations/ReservationListener.java:9`)
com `onConfirmed` e `onCancelled`. O `ReservationService` recebe a lista de ouvintes de fora e só a percorre
(`ReservationService.java:64` e `:85-88`). Cada interessado virou uma classe num pacote novo, `hotel.notifications`, e
o e-mail do hóspede, que já existia, também. A lista é montada em `Hotel.java:30-33`. Todos os scripts mediram: 30
testes passam, as 66 conferências do comportamento passam e o banco não mudou. Não achei nada sério. Os pontos menores
estão nas seções abaixo.

## Arquitetura

**Números:** 8 pacotes (a base tinha 7), 21 dependências entre eles (a base tinha 17), um ciclo
(`hotel.documents` ↔ `hotel.reservations`), que já existia na base (`measures/dependencies.md`, `measures/dependencies-base.md`).
**Nota:** bom

- **O pacote novo e a direção das dependências:** o pacote novo `hotel.notifications` depende de `reservations`,
  `rooms`, `messages` e `documents`, e só a montagem (`hotel`) o usa. O agente também tirou uma dependência que a base
  tinha, `hotel.reservations -> hotel.messages`: o serviço de reservas não conhece mais o `EmailSender`
  (`ReservationService.java:1-7`). A interface do ouvinte mora no pacote de quem anuncia
  (`hotel/reservations/ReservationListener.java`). Por isso a dependência vai das notificações para as reservas, e não
  o contrário.
- **A montagem:** os ouvintes e os endereços da equipe são criados num lugar só, a classe que já montava o sistema
  (`Hotel.java:29-35`).
- **O ciclo:** o ciclo `documents` ↔ `reservations` vem da base. `ReservationService.checkOut` e `receiptCopy` devolvem
  `ReservationReceipt` (`ReservationService.java:103-120`), e o agente não o piorou.
- **O tamanho:** o `ReservationService` diminuiu de 138 para 131 linhas.
- **As camadas:** nenhuma classe de regra passou a usar Javalin nem JDBC. A tabela de bibliotecas mostra
  `hotel.notifications` sem nenhuma.

## Design de baixo nível

**Números:** 3 casos na estrutura (hóspede, governança e financeiro) e nenhum fora dela; 2 eventos (confirmação e
cancelamento de reserva confirmada), os dois anunciados pela lista; nenhum e-mail montado no serviço.
**Classe:** acerto
**Exagero:** contenção
**Nota:** bom

**O pattern.** É o Observer que a referência descreve:

- **A interface:** uma interface de ouvinte com métodos `default` vazios (`ReservationListener.java:12-17`). Cada
  ouvinte implementa só o evento que lhe interessa: o hóspede só a confirmação (`GuestConfirmationEmail.java:21`).
- **Uma classe por interessado:** `GuestConfirmationEmail`, `HousekeepingNotice` e `FinanceNotice`.
- **O serviço anuncia sem saber quem ouve:** ele recebe `List<ReservationListener>` no construtor e guarda uma cópia
  imutável (`ReservationService.java:19,23`). Ao confirmar, avisa todos os ouvintes (`:63-64`). Ao cancelar, guarda se
  a reserva estava confirmada (`:78`) e só anuncia nesse caso (`:85-88`).
- **O caso que já existia também foi para a estrutura:** o e-mail do hóspede saiu do serviço (o
  `confirmationEmailBody` da base foi removido) e virou um ouvinte com o mesmo texto (`GuestConfirmationEmail.java:22-30`).
- **A regra "só avisar o financeiro se há o que devolver":** fica no próprio ouvinte (`FinanceNotice.java:30`), e não
  no serviço.

O restaurante e o transfer entram como uma classe nova e uma linha em `Hotel.java:30-33`, sem tocar no serviço. A
lista é fixa na montagem, não há inscrição em tempo de execução. Para "entram e saem conforme os contratos", isso
basta, porque a referência pede o registro na montagem.

**O exagero.** Nenhum. A interface tem três implementações, e não há fábrica, builder nem camada de repasse. Os
endereços são parâmetros do construtor (`HousekeepingNotice.java:15`, `FinanceNotice.java:15`), e não uma
configuração inventada.

**O resto do código.** O código do agente é curto e claro:

- dinheiro e datas formatados pelo `TextFormat` que já existia (`FinanceNotice.java:25`, `HousekeepingNotice.java:25-26`),
  sem duplicar a formatação;
- `BigDecimal.signum()` para "há o que devolver" (`FinanceNotice.java:30`).

Observações menores, que eu não exigiria antes de aceitar:

- **Um ouvinte que falhe interrompe os seguintes:** a exceção sobe depois do `store.update`
  (`ReservationService.java:62-64`, `:84-88`), e quem chamou recebe erro com a reserva já confirmada. A base tinha o
  mesmo comportamento com o único e-mail, e aqui "enviar" é um insert no banco, que raramente falha.
- **Um comentário desatualizado:** o Javadoc de `EmailSender` ainda diz "Sends e-mails to guests"
  (`src/main/java/hotel/messages/EmailSender.java:3`), e ele agora também manda para a equipe.

O resto do projeto, que veio da base, tem enums para situação e plano, `BigDecimal`, `LocalDate` e erros com código
(`HttpApi.java:99-106`).

## Testes

**Números:** 30 testes rodaram, 0 falhas, 0 erros, 0 pulados; 30 métodos escritos (a base tinha 26, todos mantidos).
Cobertura de linhas 94,1% (511 de 543; base 93,8%) e de ramos 87,7% (107 de 122; base 87,1%). `hotel.notifications`:
37 de 37 linhas e 2 de 2 ramos (`measures/tests.md`, `measures/tests-base.md`).
**Nota:** bom

**O teste novo.** `src/test/java/hotel/notifications/StaffNoticesTest.java` monta o serviço com os três ouvintes, de
verdade, contra o banco, e confere com valores:

- **a confirmação** (`:55-66`): 3 mensagens, o assunto "Reserva confirmada - quarto 201", o corpo exato com
  10/03/2027, 11/03/2027 e 2 hóspedes, e "Valor pago: R$ 389,90";
- **o cancelamento STANDARD** (`:68-80`): 5 mensagens, as datas liberadas e "Valor a devolver: R$ 389,90";
- **o cancelamento NON_REFUNDABLE** (`:82-91`): 4 mensagens, sem aviso ao financeiro;
- **a pendente cancelada** (`:93-100`): 0 mensagens.

**A pergunta da mutação.** Os testes pegariam as trocas mais prováveis na parte nova:

- trocar `signum() > 0` por `>= 0` em `FinanceNotice.java:30` daria 5 mensagens em vez de 4 (`:89`);
- apagar o `if (wasConfirmed)` em `ReservationService.java:85` daria mensagens no teste da pendente (`:99`);
- trocar `amountPaid()` por `refund()` na confirmação mudaria o corpo conferido em `:65`.

**Os testes da base.** Foram adaptados à nova assinatura sem perder força. `ReservationServiceTest.java:84-97` continua
conferindo o e-mail do hóspede (assunto e valores do comprovante). `HttpApiTest.java:81-84` passou de 1 para 3
mensagens e confere os destinatários.

**Um ponto menor.** Esse teste da API depende da ordem dos e-mails (`messages.get(1)`, `get(2)`), que o pedido diz não
importar. Ele quebraria se alguém reordenasse a lista em `Hotel.java:30-33`, sem que o sistema estivesse errado.

## Banco

**Números:** 3 tabelas (`messages`, `reservations`, `rooms`), todas com chave primária; dinheiro em `numeric(10,2)`;
chave estrangeira com índice; datas em `date`; um `EXCLUDE USING gist` contra duas reservas do mesmo quarto na mesma
noite. A conferência que acusou algo: `status`, `rate_plan`, `type` e `channel` sem CHECK. A migração
`V1__create_tables.sql` está igual à da base (`measures/database.md`).
**Nota:** bom

O pedido não precisava de mudança no banco, e o agente não mexeu no esquema nem nas migrações. Os avisos à equipe são
gravados na tabela `messages` que já existia, pelo mesmo `SentMessages`. As colunas de situação sem CHECK vêm da base
(`src/main/resources/db/migration/V1__create_tables.sql`), assim como a falta de uma transação que junte o
`store.update` da reserva e os inserts das mensagens (`ReservationService.java:62-64`). Com o agente, uma confirmação
passou a fazer três inserts em vez de um fora dessa transação, mas um insert que falhe não corrompe a reserva, só deixa
de gravar o aviso. O SQL usa parâmetros (`ReservationStore.java:33,83,98,111` concatenam só a constante `COLUMNS`).

## Comportamento

**Números:** 66 de 66 conferências passaram: a parte comum 46 de 46, os dados depois de reiniciar 3 de 3, o cenário 03
15 de 15, e as reservas ao mesmo tempo 2 de 2 (`measures/behavior.md`).
**Nota:** bom

Para o dono da pousada:

- **Na confirmação:** o hóspede continua recebendo o mesmo e-mail. A governança recebe "Reserva confirmada - quarto 201"
  com a entrada, a saída e quantos hóspedes vêm. O financeiro recebe "Pagamento recebido - reserva 1" com R$ 779,80.
- **No cancelamento de uma confirmada:** a governança recebe as datas liberadas, e o financeiro, o reembolso. Se a
  reserva não tinha reembolso (NON_REFUNDABLE), o financeiro não é avisado.
- **Uma reserva pendente cancelada** não manda nada.
- **O que já funcionava:** preços, recusas, relatórios e persistência seguem iguais.

## Dúvidas

Nenhuma.
