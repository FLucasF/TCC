# Revisão: manutenção, cenário 03

## Visão geral

A base é um serviço Javalin em pacotes por assunto: `hotel.web` (as rotas, `HttpApi`), `hotel.reservations` (as regras
em `ReservationService` e o acesso ao banco em `ReservationStore`), `hotel.rooms`, `hotel.messages` (o `EmailSender`,
que grava os e-mails na tabela `messages`), `hotel.documents` (o comprovante e a formatação), `hotel.reports`, e a
montagem em `hotel.Hotel`. Uma requisição vai de `HttpApi` para `ReservationService`, que usa `ReservationStore` e
`Rooms` (JDBC com pool, esquema pelo Flyway). O agente criou o pacote `hotel.notifications`, com uma interface de
ouvinte (`DepartmentNotifications`), um ouvinte para a governança e outro para o financeiro, e uma classe que percorre
a lista (`ReservationNotifications`). A lista é montada em `Hotel.java:29-30`, e o serviço a chama na confirmação
(`ReservationService.java:77`) e no cancelamento de uma reserva confirmada (`ReservationService.java:98-100`). O
comportamento está todo certo (66 de 66 conferências) e os testes passam (33). O que há de mais sério está no design:
o e-mail do hóspede continua sendo uma chamada direta no serviço (`ReservationService.java:73-76`), fora da lista de
ouvintes. Na arquitetura, o novo pacote formou um ciclo com `hotel.reservations`.

## Arquitetura

**Números:** 8 pacotes (7 na base), 23 dependências entre eles (17 na base). Os ciclos: 3 no final contra 1 na base. O
ciclo `documents ↔ reservations` já vinha da base. São novos `reservations ↔ notifications` e o de três pacotes
`documents → reservations → notifications → documents`.
**Nota:** com problemas

As camadas da base continuam no lugar. O pacote novo não usa Javalin nem JDBC (tabela de bibliotecas em
`measures/dependencies.md`), e as peças são ligadas num lugar só, a classe de montagem (`Hotel.java:28-32`). O serviço
recebe a lista de fora e não cria nada por dentro (`ReservationService.java:26,31`). O defeito é o ciclo novo.
`ReservationService.java:5` importa `hotel.notifications.ReservationNotifications`, e o pacote `notifications` importa
`hotel.reservations.Reservation` (`DepartmentNotifications.java:3`, `FinanceNotifications.java:6`,
`HousekeepingNotifications.java:6`). A interface do ouvinte ficou no pacote de quem ouve, e não no pacote de quem
anuncia. Se a interface (e a lista) morassem em `hotel.reservations`, a dependência teria um sentido só
(`notifications → reservations`). Além disso, `notifications → documents` (`FinanceNotifications.java:3`, pelo
`TextFormat`) junta o ciclo novo ao ciclo que já existia. As classes continuam pequenas: `ReservationService` passou de
138 para 151 linhas.

## Design de baixo nível

**Números:** 3 interessados na confirmação (hóspede, governança, financeiro): 2 na estrutura e 1 fora (o hóspede, com
chamada direta). Os 2 interessados no cancelamento estão os dois na estrutura. Há 1 interface de ouvinte, 2
implementações e 1 classe que percorre a lista.
**Classe:** malfeito
**Exagero:** contenção
**Nota:** bom

O que aproxima do Observer: há uma interface de ouvinte com os dois eventos (`DepartmentNotifications.java:10-16`) e uma
implementação por interessado. Cada uma monta o seu próprio e-mail (`HousekeepingNotifications.java:21-38`,
`FinanceNotifications.java:22-39`), e a regra "só avisa o financeiro se houver o que devolver" mora no ouvinte, não
no serviço (`FinanceNotifications.java:33`). O serviço percorre uma lista que recebe de fora
(`ReservationNotifications.java:19-29`), registrada na montagem (`Hotel.java:29-30`), e não conhece a governança nem o
financeiro. Para pôr o restaurante ou o transfer, basta uma classe nova e uma linha na montagem.

O que afasta: o hóspede, que era o único interessado da base, não virou ouvinte. `ReservationService.confirm` continua
montando e mandando o e-mail dele à mão (`ReservationService.java:72-76`, com o texto em
`ReservationService.java:138-144`) e só depois avisa a lista (`ReservationService.java:77`). Por isso o serviço ainda
depende de `EmailSender` (`ReservationService.java:18,25`) e convive com os dois jeitos de avisar. A referência do
cenário dá esse caso como malfeito: "o hóspede tem chamada direta e só a governança e o financeiro viraram ouvintes".
O nome da interface (`DepartmentNotifications`) reforça a separação: ela foi pensada para departamentos, não para
todos os interessados.

Exagero: não há. `ReservationNotifications` só repassa para a lista, mas é a parte do pattern que guarda e percorre
os ouvintes. Não há fábrica, builder nem interface com uma implementação só fora do pattern.

O resto do código: a mudança é pequena e limpa. `BigDecimal` e `LocalDate` vêm do domínio, e a formatação reaproveita
o `TextFormat` da base (`FinanceNotifications.java:3`, `HousekeepingNotifications.java:3`) sem copiar regra. Há três
pontos menores:

- um ouvinte que lançar exceção interrompe os seguintes. Isso acontece depois do `store.update`
  (`ReservationService.java:70,77`), quando a reserva já foi gravada;
- o `refund != null` em `FinanceNotifications.java:33` é defensivo: o cancelamento sempre grava um valor;
- o comentário de `EmailSender` ("Sends e-mails to guests", da base) ficou desatualizado.

Nenhum desses pontos chega a pedir mudança antes de aceitar.

## Testes

**Números:** final: 33 testes rodaram, 0 falhas, 0 erros, 0 pulados; 33 métodos escritos. Linhas: 94,4% (525 de
556). Ramos: 87,5% (112 de 128). `hotel.notifications`: 45 de 45 linhas e 7 de 8 ramos. Base: 26 testes, 93,8% das
linhas e 87,1% dos ramos.
**Nota:** bom

O agente manteve os 26 testes da base passando e acrescentou 7. Ajustou os que contavam mensagens (de 1 para 3 em
`ReservationServiceTest.java:100` e `HttpApiTest.java:81`) e acrescentou asserções sobre a governança e o financeiro;
não apagou nem enfraqueceu nenhum. A parte nova é conferida com valores:

- na confirmação, assunto, datas, hóspedes e valor pago (`NotificationsTest.java:49-69`,
  `ReservationServiceTest.java:93-116`, com "Valor pago: R$ 212,42");
- no cancelamento de uma confirmada, assunto, as duas datas e "R$ 499,80" (`ReservationServiceTest.java:148-168`);
- a NON_REFUNDABLE cancelada não avisa o financeiro (`ReservationServiceTest.java:171-182`);
- a pendente cancelada não avisa ninguém (`ReservationServiceTest.java:185-192`).

A pergunta da mutação: trocar `> 0` por `>= 0` em `FinanceNotifications.java:33` derruba o teste da NON_REFUNDABLE.
Apagar o `if (wasConfirmed)` (`ReservationService.java:98`) derruba o da pendente. Trocar a entrada pela saída no
e-mail da governança derruba as asserções de "Entrada:"/"Saída:".

Ficam três fraquezas menores:

- `NotificationsTest` repete quase os mesmos quatro casos de `ReservationServiceTest`;
- a montagem da lista de ouvintes foi copiada em quatro classes de teste (por exemplo, `NotificationsTest.java:31-32`
  e `ReportsTest.java:32-33`);
- os testes conferem a ordem das mensagens (`sent.get(1)`, `sent.get(2)`), e o pedido diz que a ordem não importa.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. Dinheiro em `numeric(10,2)`,
nenhuma chave estrangeira sem índice e nenhuma data em texto. O `EXCLUDE USING gist` impede duas reservas do mesmo
quarto na mesma noite. 1 migração do Flyway (`V1__create_tables.sql`), igual à da base. As colunas de situação e tipo
estão sem CHECK, o que vem da base.
**Nota:** bom

O agente não mexeu no esquema nem em `ReservationStore`. Não havia por quê: os avisos são gravados como as mensagens
que a base já guardava, por `EmailSender` e `SentMessages`, na tabela `messages`. A migração da base ficou intacta. O
SQL montado com `+` (`ReservationStore.java:33,83,98,111`) só junta constantes e vem da base. Um ponto menor da
mudança: a confirmação agora faz quatro escritas seguidas sem transação (o `store.update` em
`ReservationService.java:70` e três `INSERT` em `messages`). Uma falha no meio deixa a reserva confirmada com só parte
dos avisos gravados. Na base, a mesma falta de transação já existia com uma escrita a menos.

## Comportamento

**Números:** 66 de 66 conferências passaram, 0 falharam. Parte comum: 46 de 46. Dados depois de reiniciar: 3 de 3.
Cenário 03: 15 de 15. Duas reservas ao mesmo tempo: 2 de 2.
**Nota:** bom

Tudo o que o dono pediu funciona pela API:

- o hóspede continua recebendo o mesmo e-mail;
- na confirmação, a governança recebe "Reserva confirmada - quarto 201" com 10/03/2027, 12/03/2027 e 2 hóspedes, e o
  financeiro recebe "Pagamento recebido - reserva 1" com R$ 779,80;
- no cancelamento da confirmada, saem "Reserva cancelada - quarto 201" com as datas e "Reembolso - reserva 1" com
  R$ 779,80;
- a NON_REFUNDABLE cancelada avisa só a governança, e a pendente cancelada não manda nada.

O resto do sistema (preços, recusas, check-in e check-out, relatórios) continua igual, e os dados sobrevivem a um
reinício.

## Dúvidas

O ciclo na arquitetura é o preço de um Observer cuja interface ficou no pacote dos ouvintes. Ele é pequeno e entre
pacotes vizinhos, e outro revisor poderia pesá-lo menos. Dei `com problemas` porque a skill conta um ciclo novo entre
dois pacotes como problema e porque ele se resolveria só mudando a interface de pacote.
