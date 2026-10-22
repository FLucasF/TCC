# Revisão: manutenção, cenário 03

## Visão geral

A base é um serviço Javalin em pacotes por assunto: `hotel.web` (rotas, `HttpApi`), `hotel.reservations` (regras em
`ReservationService`, acesso ao banco em `ReservationStore`), `hotel.rooms`, `hotel.messages` (o `EmailSender` grava
cada e-mail na tabela `messages`), `hotel.documents` e `hotel.reports`. A montagem fica em `hotel.Hotel`. Uma
requisição de confirmação vai de `HttpApi` para `ReservationService.confirm`, que grava pelo `ReservationStore` e
manda os e-mails. O agente criou o pacote `hotel.reservations.notifications`, com uma interface
`ReservationEventNotifier` (dois métodos: confirmação e cancelamento), uma implementação para a governança e outra
para o financeiro, e um builder `ReservationNotifiers` usado em `Hotel.java:26-29`. O serviço recebe a lista e a
percorre (`ReservationService.java:84`, `:109`). O roteiro de comportamento passa inteiro (66 de 66) e os testes
passam (30 de 30). O mais sério é de design: o e-mail do hóspede continua sendo uma chamada direta dentro do serviço
(`ReservationService.java:80-83`, com o texto em `:150`). Só os dois interessados novos viraram ouvintes, e a
referência do pattern chama isso de malfeito. Há também um ciclo novo entre `hotel.reservations` e o subpacote que o
agente criou.

## Arquitetura

**Números:** 8 pacotes (a base tinha 7), 23 dependências entre eles (a base tinha 17), 2 ciclos (a base tinha 1).
O ciclo `hotel.documents` ↔ `hotel.reservations` vem da base. O ciclo `hotel.reservations` ↔
`hotel.reservations.notifications` é novo.
**Nota:** com problemas

- O ciclo novo vem de onde a interface ficou. `ReservationService` importa `ReservationEventNotifier` do subpacote
  (`src/main/java/hotel/reservations/ReservationService.java:5`). A interface, por sua vez, usa `Reservation` e
  `Room` (`src/main/java/hotel/reservations/notifications/ReservationEventNotifier.java:3-4`), e os ouvintes usam
  `Reservation` e `ReservationStatus`. Se a interface ficasse em `hotel.reservations`, a dependência iria num sentido
  só: os ouvintes dependeriam do serviço, e não o contrário. É um ciclo pequeno entre dois pacotes vizinhos, mas foi
  o agente que o criou.
- O resto está bem e não piorou. A montagem continua num lugar só (`src/main/java/hotel/Hotel.java:21-34`): os
  ouvintes e os endereços são ligados ali, e o serviço não cria nada por dentro. A regra nova não conhece o Javalin
  nem o JDBC: a tabela de bibliotecas mostra `hotel.reservations.notifications` sem nenhuma. Os ouvintes só falam com
  o `EmailSender`. `ReservationService` cresceu de 138 para 163 linhas, um tamanho ainda razoável.
- Os defeitos que a base já tinha (o ciclo `documents`↔`reservations`, `HttpApi` com 170 linhas) não são do agente.

## Design de baixo nível

**Números:** há 3 interessados na confirmação (hóspede, governança e financeiro). Desses, 2 estão na estrutura e 1
ficou fora: o e-mail do hóspede é chamado direto em `ReservationService.java:80-83`. No cancelamento, os 2
interessados estão na estrutura. As implementações de `ReservationEventNotifier` são 2.
**Classe:** malfeito
**Exagero:** exagero
**Nota:** com problemas

**O pattern.** O que aproxima do Observer:
- há uma interface de ouvinte, `notifications/ReservationEventNotifier.java:6-10`, com uma implementação por
  interessado (`HousekeepingNotifier`, `FinanceNotifier`);
- o serviço recebe a lista de fora (`ReservationService.java:28-38`) e a percorre na confirmação (`:84-86`) e no
  cancelamento (`:107-112`), sem conhecer os destinos nem montar os textos da governança e do financeiro;
- o registro é feito na montagem (`Hotel.java:26-29`).

O que afasta: o hóspede, que já existia na base, não virou ouvinte. `confirm` continua mandando o e-mail dele à mão
(`ReservationService.java:80-83`), e o texto desse e-mail continua no serviço (`confirmationEmailBody`,
`ReservationService.java:150-156`). A referência põe este caso, com todas as letras, em malfeito ("o hóspede tem
chamada direta e só a governança e o financeiro viraram ouvintes"). Além disso, a regra "só avisa quem estava
confirmada" fica no serviço (`ReservationService.java:100`, `:107`), mas os ouvintes também conferem a situação
(`notifications/HousekeepingNotifier.java:30`, `notifications/FinanceNotifier.java:27`). É a mesma decisão escrita em
dois lugares.

**O exagero.** O builder `ReservationNotifiers` (`notifications/ReservationNotifiers.java:7-29`) não acrescenta nada
ao pattern:
- ele só embrulha um `ArrayList`, e existe porque as implementações ficaram package-private
  (`HousekeepingNotifier.java:9`, `FinanceNotifier.java:9`);
- cada interessado precisa de um método `addX` próprio (`ReservationNotifiers.java:16`, `:21`), então o restaurante
  e o transfer vão obrigar a mexer no builder, além de criar a classe nova;
- um `List.of(new HousekeepingNotifier(...), new FinanceNotifier(...))` em `Hotel` faria o mesmo.

**O resto do código:**
- o parâmetro `Room room` dos dois métodos da interface não é usado por nenhuma implementação
  (`FinanceNotifier.java:19`, `:26`; `HousekeepingNotifier.java:19`, `:29`), e mesmo assim o serviço busca o quarto no
  banco no cancelamento só para passá-lo (`ReservationService.java:108`);
- a conferência `reservation.refund() == null` (`FinanceNotifier.java:27`) é defensiva sem necessidade, porque o
  serviço só avisa depois de `cancel(refund)`;
- os tipos estão certos: o dinheiro sai de `BigDecimal` por `TextFormat.money` e as datas de `LocalDate` por
  `TextFormat.date`, reusando o formato do comprovante;
- o construtor de quatro argumentos que o agente manteve (`ReservationService.java:23-26`) é usado só por
  `src/test/java/hotel/reports/ReportsTest.java:27`. Pode ficar, mas ninguém do código de produção o usa.

São defeitos localizados, que um revisor pediria para limpar.

## Testes

**Números:** 30 testes rodaram, 0 falhas, 0 erros, 0 pulados, 30 métodos escritos (a base tinha 26, todos mantidos).
A cobertura de linhas é de 94,3% (531 de 563; a base tinha 93,8%) e a de ramos, de 86,4% (114 de 132; a base tinha
87,1%). No pacote novo, `hotel.reservations.notifications` tem 42 de 43 linhas cobertas e 5 de 8 ramos.
**Nota:** bom

O agente testou a parte nova com valores, contra o banco de verdade:
- **Confirmação:** destinatários, assuntos com o número do quarto e da reserva, entrada 10/03/2027, saída
  11/03/2027, "Hóspedes: 1" e "R$ 249,90" (`src/test/java/hotel/reservations/ReservationServiceTest.java:174-191`).
- **Cancelamento STANDARD confirmado:** o aviso à governança com "10/03/2027 a 11/03/2027" e o reembolso de
  R$ 249,90 (`:194-208`).
- **NON_REFUNDABLE cancelada:** só a governança é avisada, 4 mensagens (`:211-221`).
- **Pendente cancelada:** nenhuma mensagem (`:224-231`).
- **Testes da base:** os que contavam 1 mensagem foram ajustados para 3, sem perder as outras asserções
  (`:101-105`, `:115`), e o teste pela API confere os três destinatários
  (`src/test/java/hotel/web/HttpApiTest.java:81-84`).

A pergunta da mutação:
- se alguém apagasse o `if (wasConfirmed)`, o teste da pendente falharia;
- se trocasse `signum() == 0` por `!= 0`, o teste da NON_REFUNDABLE falharia;
- se trocasse `amountPaid` por `total` no aviso do financeiro, nenhum teste falharia, porque os dois valores são
  iguais depois da confirmação (isso não chega a ser um defeito).

Pontos fracos:
- o número de hóspedes só é conferido com o valor 1;
- os testes dependem da ordem dos e-mails (`sent.get(1)`, `sent.get(2)`), que o dono disse não importar. Mudar a
  ordem de registro quebraria testes sem quebrar nenhuma regra.

Nada disso pesa o bastante para baixar a nota.

## Banco

**Números:** 3 tabelas, todas com chave primária. O dinheiro está todo em `numeric(10,2)`, não há chave estrangeira
sem índice nem datas em texto, e há `EXCLUDE` contra reservas sobrepostas. A única migração,
`V1__create_tables.sql`, ficou igual à da base. Status e plano estão em `text` sem `CHECK`, o que vem da base.
**Nota:** bom

O agente não mexeu no esquema, e nem precisava: os avisos novos usam a tabela `messages`, pelo mesmo `EmailSender`
(`src/main/java/hotel/messages/SentMessages.java:39-53`, SQL com parâmetros). O que ainda dá para notar vem da base:
- os `CHECK` que faltam nas colunas de situação;
- nenhuma transação junta a gravação da reserva e a dos e-mails (`ReservationService.java:77` grava, e só depois os
  avisos são gravados em conexões separadas).

O agente repetiu esse mesmo arranjo nos avisos novos, sem piorá-lo.

## Comportamento

**Números:** 66 de 66 conferências passaram:
- a parte comum: 46 de 46;
- depois de reiniciar: 3 de 3;
- o cenário 03: 15 de 15;
- reservas simultâneas: 2 de 2.

**Nota:** bom

Para o dono da pousada, tudo o que ele pediu funciona pela API:
- na confirmação, o hóspede continua recebendo o mesmo e-mail;
- a governança recebe "Reserva confirmada - quarto 201", com entrada, saída e o número de hóspedes;
- o financeiro recebe "Pagamento recebido - reserva 1" com R$ 779,80;
- no cancelamento de uma reserva confirmada, a governança recebe as datas liberadas e o financeiro recebe o
  reembolso, só quando há o que devolver;
- uma reserva pendente cancelada não avisa ninguém.

Nada da parte comum quebrou, e os dados continuam lá depois de reiniciar o serviço.

## Dúvidas

- **O exagero:** o builder `ReservationNotifiers` pode ser lido como o mecanismo de registro do próprio pattern, e
  então não seria exagero. Marquei exagero porque ele não faz nada além de embrulhar uma lista, e porque cada
  interessado novo obriga a acrescentar um método nele.
- **Os ouvintes futuros:** o restaurante e o transfer só serão avisados mudando `Hotel` (e o builder) e
  reempacotando o sistema. O texto diz que quem é avisado "vai mudar com o tempo"; a referência aceita o registro na
  montagem, então não pesei isso.
