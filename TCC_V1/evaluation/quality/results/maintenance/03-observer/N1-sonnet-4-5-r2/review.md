# Revisão: manutenção, cenário 03

## Visão geral

A base é um serviço Javalin em pacotes por assunto: `hotel.web` (rotas e JSON), `hotel.reservations` (o serviço com as
regras, o cálculo de preço e o `ReservationStore`, que faz o JDBC), `hotel.rooms`, `hotel.messages` (o `EmailSender`,
que grava cada e-mail na tabela `messages`), `hotel.documents` (comprovante e formatação) e `hotel.reports`. A classe
`Hotel` monta as peças. Uma requisição passa por `HttpApi` → `ReservationService` → `ReservationStore`/`SentMessages`
→ PostgreSQL, com o esquema criado pelo Flyway. O agente criou um pacote `hotel.notifications` com a interface
`ReservationListener` e dois ouvintes (`HousekeepingNotifier`, `FinanceNotifier`), registrados em `Hotel.java:29-33`.
O serviço chama a lista de ouvintes na confirmação e no cancelamento (`ReservationService.java:78-80` e `:100-102`).
O comportamento pedido está todo certo: 66 de 66 conferências passaram e os 30 testes passam. Os problemas são de
estrutura. O e-mail do hóspede continua sendo uma chamada direta no serviço (`ReservationService.java:74-77`) e não
virou um ouvinte. A interface ficou num pacote que depende de `hotel.reservations`, o que criou um ciclo novo entre os
dois pacotes. E a regra "só avisa se a reserva estava confirmada" ficou repetida dentro de cada ouvinte, deduzida de
`amountPaid` e de `refund`, em vez de o serviço anunciar só o cancelamento de uma reserva confirmada.

## Arquitetura

**Números:** 8 pacotes (a base tinha 7) e 23 dependências entre eles (a base tinha 17). Na final há três ciclos:
`documents ↔ reservations`, que já vinha da base; `reservations ↔ notifications`, que é novo; e um de três pacotes,
`documents → reservations → notifications → documents`, também novo. O pacote `hotel.notifications` não usa Javalin
nem JDBC.
**Nota:** com problemas

A borda HTTP não mudou e continua fina; a montagem continua num lugar só (`Hotel.java:24-36`), onde os ouvintes são
criados e passados ao serviço. Isso está certo. O defeito novo é o ciclo. `ReservationService` importa
`hotel.notifications.ReservationListener` (`ReservationService.java:5`), e os ouvintes desse pacote importam
`hotel.reservations.Reservation` e `ReservationStatus` (`HousekeepingNotifier.java:5-6`, `FinanceNotifier.java:5`).
Bastaria pôr a interface do ouvinte em `hotel.reservations`, ao lado do serviço que a anuncia, para as dependências
irem num sentido só (notifications → reservations). Os ouvintes também passaram a usar `hotel.documents.TextFormat`
(`FinanceNotifier.java:3`), o que fechou o ciclo de três pacotes por cima do ciclo antigo da base. Pelos critérios da
skill, é um ciclo novo entre dois pacotes vizinhos: um defeito localizado, não uma quebra das camadas.

## Design de baixo nível

**Números:** há três interessados na confirmação: o hóspede, a governança e o financeiro. Dois deles estão na
estrutura de ouvintes, e o hóspede ficou fora (chamada direta). No cancelamento, os dois interessados estão na
estrutura. A interface `ReservationListener` tem 2 implementações.
**Classe:** malfeito
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** Há uma interface de ouvinte com dois eventos (`ReservationListener.java:7-12`) e uma implementação por
interessado novo. A lista vem de fora, pelo construtor (`ReservationService.java:22-33`), e é montada em
`Hotel.java:29-31`. O serviço não conhece a governança nem o financeiro, e os textos desses e-mails moram nos
ouvintes. Isso aproxima o código do Observer: o restaurante ou o transfer entrariam como mais uma classe e uma linha
na montagem. Mas o e-mail do hóspede, que já existia, ficou como antes: o serviço ainda monta o texto e chama o
`EmailSender` direto (`ReservationService.java:73-77` e `confirmationEmailBody` em `:140-146`), e só depois percorre a
lista (`:78-80`). A referência do cenário diz que esse caso, "o hóspede tem chamada direta e só a governança e o
financeiro viraram ouvintes", é **malfeito**. Por causa disso, o serviço ainda depende do `EmailSender`
(`ReservationService.java:19`, `:26`).

**O exagero.** Não há estrutura sobrando. A interface tem duas implementações reais, e não há fábrica, builder nem
camada que só repasse.

**O resto do design.**
- O evento de cancelamento é disparado para toda reserva cancelada, inclusive a pendente (`ReservationService.java:100-102`).
  Cada ouvinte precisa então descobrir sozinho se a reserva estava confirmada:
  - a governança olha `amountPaid().signum() == 0` (`HousekeepingNotifier.java:35`). É uma dedução indireta: ela
    funciona só porque `cancel` não zera o valor pago (`Reservation.java:111-114`);
  - o financeiro só olha se o reembolso é zero (`FinanceNotifier.java:31-33`). Para uma pendente, isso dá certo por
    acaso: a devolução de uma reserva STANDARD é igual ao valor pago, que é zero (`ReservationService.java:95`).

  Cada ouvinte novo (o restaurante, o transfer) vai ter de repetir esse filtro. O serviço deveria anunciar só "uma
  reserva confirmada foi cancelada", porque o que distingue os casos é o estado de antes, que só ele conhece.
- A checagem `reservation.status() != ReservationStatus.CANCELLED` em `HousekeepingNotifier.java:32-34` é código
  morto: o evento só é disparado depois de `reservation.cancel(...)`, que sempre deixa a reserva como CANCELLED.
- `roomOf(reservation)`, uma consulta ao banco, é chamado dentro do laço, uma vez por ouvinte
  (`ReservationService.java:101`). Na confirmação, o agente fez certo e buscou o quarto uma vez só (`:72`).
- Pouco a apontar fora disso. Dinheiro em `BigDecimal` e datas em `LocalDate`; a formatação reaproveita
  `TextFormat.money` e `TextFormat.date` (`FinanceNotifier.java:24`, `HousekeepingNotifier.java:24-25`), sem
  duplicar. Os endereços ficam em constantes por ouvinte (`HousekeepingNotifier.java:11`, `FinanceNotifier.java:11`).

## Testes

**Números:** 30 testes rodaram, com 0 falhas, 0 erros e 0 pulados (a base tinha 26). O código tem 30 métodos de
teste, 4 deles novos. Cobertura de linhas de 94,2% (520 de 552) e de ramos de 86,7% (111 de 128); a base tinha 93,8%
e 87,1%. No pacote novo `hotel.notifications`, 39 de 40 linhas e 6 de 8 ramos estão cobertos.
**Nota:** bom

Os quatro testes novos conferem a parte nova com valores, contra o banco:
- confirmação (`ReservationServiceTest.java:171`): assunto "Reserva confirmada - quarto 201", "Entrada: 10/03/2027",
  "Saída: 13/03/2027", "Hóspedes: 2", "Pagamento recebido - reserva N" e "R$ 1.169,70";
- cancelamento de uma STANDARD confirmada, com governança e financeiro (`:191`);
- cancelamento de uma NON_REFUNDABLE, só com a governança (`:211`);
- cancelamento de uma pendente, sem mensagem nenhuma (`:226`).

A pergunta da mutação:
- se alguém apagasse o filtro de `HousekeepingNotifier.java:35` ou o de `FinanceNotifier.java:31`, o teste da
  pendente (`:226`) falharia;
- se trocasse o assunto ou o destinatário, os testes de `:171` e `:191` falhariam.

Ficam sem rede dois pontos:
- no e-mail de reembolso, trocar `refund` por `amountPaid` não seria pego, porque no único caso testado os dois
  valem R$ 1.169,70 (`:206`);
- a checagem morta de `HousekeepingNotifier.java:32` não tem teste, por isso a cobertura de ramos não chega a 100%.

Os testes da base foram ajustados ao novo número de mensagens, sem perder o que conferiam. Por exemplo, o e-mail do
hóspede continua com o corpo conferido (`ReservationServiceTest.java:98-103`). Já `HttpApiTest.java:81-89` passou a
procurar o hóspede na lista em vez de olhar a primeira mensagem, o que é coerente com "tanto faz a ordem". Pela API,
os e-mails novos só são contados (3), sem conferir assunto nem texto; a conferência deles fica nos testes do serviço.

## Banco

**Números:** 3 tabelas, todas com chave primária. Dinheiro em `numeric(10,2)`, datas em `date`, a chave estrangeira
tem índice, e um `EXCLUDE USING gist` impede reservas que se sobrepõem. A única migração (`V1__create_tables.sql`) é
igual à da base. As conferências só acusaram as colunas de situação e de tipo em `text` sem CHECK
(`reservations.status`, `rate_plan`, `rooms.type`, `messages.channel`), que já vinham da base.
**Nota:** bom

O pedido não exigia mudar o esquema, e o agente não mexeu: as mensagens novas usam a tabela `messages` que já existia,
pelo `EmailSender` (`EmailSender.java:12-14`). O SQL continua com parâmetros, e os pontos montados com `+` em
`ReservationStore.java:33,83,98,111` juntam só constantes. Uma ressalva que vem da base: a confirmação grava a reserva
(`ReservationService.java:71`) e depois as mensagens, sem transação. O agente acrescentou mais duas ou três escritas
nesse mesmo trecho (`:78-80`, `:100-102`). Se uma delas falhar, a reserva fica confirmada ou cancelada com parte dos
avisos gravados, mas nenhum dado de reserva se perde.

## Comportamento

**Números:** 66 de 66 conferências passaram:
- parte comum: 46 de 46;
- depois de reiniciar: 3 de 3;
- cenário 03: 15 de 15;
- reservas simultâneas: 2 de 2.
**Nota:** bom

Tudo o que o dono pediu funciona pela API. Na confirmação:
- o hóspede continua recebendo o e-mail de antes;
- a governança recebe "Reserva confirmada - quarto 201", com as datas de entrada e saída e o número de hóspedes;
- o financeiro recebe "Pagamento recebido - reserva 1", com R$ 779,80.

No cancelamento de uma reserva confirmada:
- a governança recebe o aviso, com as datas que ficaram livres;
- o financeiro recebe "Reembolso - reserva 1", com o valor a devolver;
- numa NON_REFUNDABLE, só a governança é avisada.

Uma reserva pendente cancelada não avisa ninguém. A parte que já existia continua igual: preço, recusas, relatórios,
os dados depois de reiniciar e as reservas simultâneas.

## Dúvidas

1. **A classe do pattern (`malfeito` ou `acerto` incompleto).** A estrutura está certa para os dois interessados
   novos e para os dois eventos. O único caso fora dela é o hóspede (`ReservationService.java:73-77`). A referência
   do cenário classifica esse caso como malfeito, e foi o que dei.
2. **A nota da arquitetura (`com problemas` ou `bom`).** O ciclo novo vem só da interface estar no pacote errado. É
   uma correção de uma linha de `package`, mas o critério da skill pede `com problemas` para um ciclo novo.
